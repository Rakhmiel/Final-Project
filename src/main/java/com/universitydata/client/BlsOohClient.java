package com.universitydata.client;

import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.OohOccupation;
import com.universitydata.util.HttpUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper for the BLS Occupational Outlook Handbook (OOH).
 *
 * The OOH provides structured occupation pages with consistent HTML.
 * No API key required. Be polite: default delay between requests is 1.5s.
 *
 * Base URL: https://www.bls.gov/ooh/
 *
 * How it works:
 *   1. Scrape the A-Z index page to get all occupation URLs
 *   2. For each occupation page, parse the Quick Facts table
 *
 * Quick Facts fields per page:
 *   - Median annual pay
 *   - Entry-level education
 *   - Work experience in related occupation
 *   - On-the-job training
 *   - Number of jobs (current)
 *   - Job outlook (% growth over 10 years)
 *   - Employment change (# of new jobs over 10 years)
 *
 * BLS Terms of Service: https://www.bls.gov/bls/linksite.htm
 * Public domain data — scraping is permitted. Use politely.
 */
public class BlsOohClient {

    private static final Logger log = LoggerFactory.getLogger(BlsOohClient.class);

    private static final String INDEX_PATH        = "/a-z-index.htm";
    private static final String USER_AGENT        =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int    JSOUP_TIMEOUT_MS  = 30_000;

    private final DataSourceConfig config;
    private final HttpUtil http;

    public BlsOohClient(DataSourceConfig config) {
        this.config = config;
        this.http   = new HttpUtil(config.getConnectTimeoutSeconds(),
                                   config.getReadTimeoutSeconds(),
                                   config.getMaxRetries());
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Scrape all OOH occupation URLs from the A-Z index page.
     * Returns a list of full URLs to individual occupation pages.
     */
    public List<String> getAllOccupationUrls() throws IOException {
        String indexUrl = config.getOohBaseUrl() + INDEX_PATH;
        log.info("Fetching OOH A-Z index: {}", indexUrl);

        Document doc = fetchDocument(indexUrl);
        List<String> urls = new ArrayList<>();

        // The A-Z index has links inside div.az-element or similar list containers
        Elements links = doc.select("div.az-element a, ul.az-list a, #ooh-content a[href*='/ooh/']");

        // Fallback: grab all /ooh/ links that look like occupation pages
        if (links.isEmpty()) {
            links = doc.select("a[href*='/ooh/']");
        }

        for (Element link : links) {
            String href = link.attr("abs:href");
            // Filter: occupation pages end in /home.htm or similar pattern
            if (href.contains("/ooh/") && !href.endsWith("ooh/")
                    && !href.contains("a-z-index")
                    && !href.contains("occupation-finder")) {
                if (!urls.contains(href)) {
                    urls.add(href);
                }
            }
        }

        log.info("Found {} occupation URLs in OOH index", urls.size());
        return urls;
    }

    /**
     * Scrape a single OOH occupation page.
     *
     * @param occupationUrl full URL, e.g. https://www.bls.gov/ooh/computer-and-information-technology/software-developers.htm
     */
    public OohOccupation scrapeOccupationPage(String occupationUrl) throws IOException {
        log.debug("Scraping OOH page: {}", occupationUrl);
        Document doc = fetchDocument(occupationUrl);
        return parseOccupationPage(doc, occupationUrl);
    }

    /**
     * Scrape ALL OOH occupation pages.
     * This will take several minutes due to polite delays.
     * Consider saving results locally and only refreshing annually.
     *
     * @return list of all OOH occupations (~900 records)
     */
    public List<OohOccupation> scrapeAllOccupations() throws IOException {
        List<String> urls = getAllOccupationUrls();
        List<OohOccupation> results = new ArrayList<>();
        int count = 0;

        for (String url : urls) {
            try {
                OohOccupation occ = scrapeOccupationPage(url);
                if (occ != null) results.add(occ);
                count++;
                if (count % 50 == 0) {
                    log.info("Scraped {}/{} OOH pages", count, urls.size());
                }
            } catch (IOException e) {
                log.warn("Failed to scrape {}: {}", url, e.getMessage());
            }
            HttpUtil.politeDelay(config.getScrapeDelayMs());
        }

        log.info("Finished scraping {} OOH occupation pages", results.size());
        return results;
    }

    /**
     * Scrape a specific occupation by title keyword.
     * Searches the index and returns the first match.
     */
    public OohOccupation findOccupationByTitle(String titleKeyword) throws IOException {
        List<String> urls = getAllOccupationUrls();
        String keyword = titleKeyword.toLowerCase();

        for (String url : urls) {
            if (url.toLowerCase().contains(keyword.replace(" ", "-"))) {
                HttpUtil.politeDelay(config.getScrapeDelayMs());
                return scrapeOccupationPage(url);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------------

    private OohOccupation parseOccupationPage(Document doc, String url) {
        OohOccupation occ = new OohOccupation();
        occ.url = url;

        // Title — usually in the h1
        Element h1 = doc.selectFirst("h1.title-occupation, h1#ooh-h1, article h1, .occupation-title h1");
        if (h1 == null) h1 = doc.selectFirst("h1");
        occ.title = h1 != null ? h1.text().trim() : extractTitleFromUrl(url);

        // Quick Facts table — the structured data block
        // BLS uses a <table> with class "qf-table" or inside div#quickFacts
        Element qfTable = doc.selectFirst("table.qf-table, #quickFacts table, .quick-facts table");
        if (qfTable == null) {
            // fallback: look for the stats section
            qfTable = doc.selectFirst("table");
        }

        if (qfTable != null) {
            for (Element row : qfTable.select("tr")) {
                Elements cells = row.select("th, td");
                if (cells.size() < 2) continue;

                String label = cells.get(0).text().trim().toLowerCase();
                String value = cells.get(cells.size() - 1).text().trim();

                if (label.contains("median") && label.contains("pay")) {
                    occ.medianAnnualPay = parseMoneyToInt(value);

                } else if (label.contains("entry-level education")
                        || label.contains("entry level education")) {
                    occ.entryLevelEducation = value;

                } else if (label.contains("work experience")) {
                    occ.workExperience = value;

                } else if (label.contains("on-the-job") || label.contains("on the job")) {
                    occ.onJobTraining = value;

                } else if (label.contains("number of jobs")) {
                    occ.numberOfJobs = parseLongFromText(value);

                } else if (label.contains("job outlook") || label.contains("employment growth")) {
                    occ.jobGrowthRate10Yr = parsePercentage(value);
                    occ.outlookSummary   = extractOutlookLabel(value);

                } else if (label.contains("openings") || label.contains("employment change")) {
                    occ.projectedOpenings10Yr = parseLongFromText(value);
                }
            }
        }

        // If Quick Facts parse failed, try the summary section stats
        if (occ.medianAnnualPay == null) {
            Element payElem = doc.selectFirst(".median-pay, [data-type='median-pay'], .pay span");
            if (payElem != null) {
                occ.medianAnnualPay = parseMoneyToInt(payElem.text());
            }
        }

        log.debug("Parsed: {}", occ);
        return occ;
    }

    private Document fetchDocument(String url) throws IOException {
        String baseUrl = config.getOohBaseUrl();
        // Seed a session cookie by visiting the BLS homepage first (one-time, lazy)
        if (sessionCookies == null) {
            sessionCookies = Jsoup.connect(baseUrl)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(JSOUP_TIMEOUT_MS)
                    .execute()
                    .cookies();
        }
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Referer", baseUrl + "/")
                .cookies(sessionCookies)
                .timeout(JSOUP_TIMEOUT_MS)
                .get();
    }

    private java.util.Map<String, String> sessionCookies = null;

    // ---------------------------------------------------------------
    // Value parsers
    // ---------------------------------------------------------------

    /** Parse "$XX,XXX" or "$XX,XXX per year" to int */
    private static Integer parseMoneyToInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Extract first dollar amount pattern e.g. "$127,260"
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\$([\\d,]+)")
                    .matcher(s);
            if (m.find()) {
                return Integer.parseInt(m.group(1).replace(",", ""));
            }
        } catch (NumberFormatException e) { return null; }
        return null;
    }

    /** Parse "XX%" or "XX% (much faster than average)" → double */
    private static Double parsePercentage(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            int pctIdx = s.indexOf('%');
            if (pctIdx > 0) {
                // Walk back to find the number before %
                int start = pctIdx - 1;
                while (start > 0 && (Character.isDigit(s.charAt(start - 1)) || s.charAt(start - 1) == '.')) {
                    start--;
                }
                return Double.parseDouble(s.substring(start, pctIdx).trim());
            }
        } catch (NumberFormatException e) { /* ignore */ }
        return null;
    }

    /** Extract the outlook label (e.g. "Much faster than average") from strings like "25% (Much faster than average)" */
    private static String extractOutlookLabel(String s) {
        if (s == null) return null;
        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open >= 0 && close > open) {
            return s.substring(open + 1, close).trim();
        }
        // Try keywords directly in text
        for (String label : List.of("Much faster", "Faster", "As fast", "As fast as average",
                "Slower", "Little or no change", "Decline")) {
            if (s.contains(label)) return label;
        }
        return s;
    }

    /** Parse "123,456" or "123,456 jobs" to Long */
    private static Long parseLongFromText(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String digits = s.replaceAll("[^0-9]", "");
            return digits.isBlank() ? null : Long.parseLong(digits);
        } catch (NumberFormatException e) { return null; }
    }

    /** Derive a readable title from the URL path segment */
    private static String extractTitleFromUrl(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1].replace(".htm", "");
        return last.replace("-", " ");
    }
}
