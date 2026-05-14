# university-data-client

A Maven Java package for retrieving university outcome data across 5 free data sources.
Covers majors (CIP codes), employment rates, salaries, job outlook, and degree completions.

## Data Sources

| Source | What it provides | Access |
|--------|-----------------|--------|
| **College Scorecard API** | Earnings 1yr/4yr post-graduation, debt, by school + major | API key (free) |
| **BLS OEWS** | Occupation wages (median, percentiles, employment counts) | API key (optional) |
| **BLS OOH** | 10-yr job outlook, growth %, projected openings, entry education | Scraped (no key) |
| **IPEDS** | Annual degree completions by major and institution | Bulk CSV download |
| **O\*NET** | CIP→SOC crosswalk (major→occupation mapping), occupation detail | API key (free) |

## Setup

### 1. Get API Keys

- **College Scorecard**: https://api.data.gov/signup/ (instant, free)
- **O\*NET**: https://services.onetcenter.org/developer/ (free, approval ~1 day)
- **BLS**: https://data.bls.gov/registrationEngine/ (optional; raises rate limit from 25 to 500 req/day)
- **IPEDS / OOH**: No key needed

### 2. Add as Maven dependency (in your parent project)

```xml
<dependency>
    <groupId>com.universitydata</groupId>
    <artifactId>university-data-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or install locally first:
```bash
cd university-data-client
mvn install -DskipTests
```

### 3. Configure and use

```java
DataSourceConfig config = DataSourceConfig.builder()
    .collegeScorecardApiKey("YOUR_SCORECARD_KEY")
    .onetUsername("your@email.com")
    .onetPassword("yourpassword")
    .blsApiKey("YOUR_BLS_KEY")          // optional
    .ipedsLocalCacheDir("/data/ipeds")  // where to cache ~50MB CSV files
    .scrapeDelayMs(1500)                // polite delay for OOH scraping
    .build();

UniversityDataService svc = new UniversityDataService(config);
```

## Usage Examples

### Get programs at a specific school
```java
List<ScorecardProgram> programs = svc.getProgramsBySchool("MIT");
programs.forEach(p -> System.out.println(p.cipTitle + " → $" + p.medianEarnings4Yr));
```

### Get all schools offering a major (CIP code)
```java
// CIP 11.0101 = Computer Science
List<ScorecardProgram> cs = svc.getProgramsByCip("1101");
```

### What jobs does a CS major lead to? (O*NET crosswalk)
```java
List<OnetCrosswalkEntry> jobs = svc.getOccupationsForMajor("11.0101");
// Returns SOC codes + titles, e.g. "15-1252.00 — Software Developers"
```

### Get occupation wages from BLS
```java
OewsOccupation wages = svc.getOccupationWages("15-1252");
System.out.println("Median: $" + wages.annualMedianWage);
System.out.println("90th pct: $" + wages.annualWage90thPct);
```

### Scrape job outlook from OOH
```java
String url = "https://www.bls.gov/ooh/computer-and-information-technology/software-developers.htm";
OohOccupation outlook = svc.getOohOccupation(url);
System.out.println(outlook.jobGrowthRate10Yr + "% growth, " + outlook.outlookSummary);
```

### Get degree completions from IPEDS
```java
// All bachelor's programs (award level 5) completed in 2022-23
List<IpedsCompletion> completions = svc.getIpedsClient()
    .getCompletionsByAwardLevel("2022-23", 5);
```

### Full enriched summary (all sources combined)
```java
// CIP 11.0101 = Computer Science, level 5 = Bachelor's
MajorOutcomeSummary summary = svc.getMajorOutcome("11.0101", 5, "2022-23");
System.out.println(summary.scorecardMedianEarnings4Yr);  // from Scorecard
System.out.println(summary.blsMedianAnnualWage);         // from OEWS
System.out.println(summary.blsProjectedGrowthRate);      // from OOH
System.out.println(summary.annualCompletions);           // from IPEDS
System.out.println(summary.relatedOccupationTitles);     // from O*NET
```

## Key Identifiers

| ID | What it is | Used by |
|----|-----------|---------|
| **CIP code** | Classification of Instructional Programs — identifies a major | Scorecard, IPEDS, O\*NET |
| **SOC code** | Standard Occupational Classification — identifies a job | OEWS, OOH, O\*NET |
| **UNITID** | Institution ID | Scorecard (`id`), IPEDS |

The CIP → SOC crosswalk (via O\*NET) is the bridge between "what was studied" and "what job".

## CIP Code Reference

Common 2-digit CIP families:
- `11` — Computer and Information Sciences
- `14` — Engineering
- `52` — Business, Management, Marketing
- `51` — Health Professions
- `45` — Social Sciences
- `23` — English Language and Literature
- `26` — Biological and Biomedical Sciences

Full list: https://nces.ed.gov/ipeds/cipcode/

## Award Level Codes (IPEDS / Scorecard)

| Code | Level |
|------|-------|
| 1 | Certificate < 1 year |
| 2 | Certificate 1–2 years |
| 3 | Associate's degree |
| 5 | Bachelor's degree |
| 7 | Master's degree |
| 17 | Doctoral – research |
| 18 | Doctoral – professional (MD, JD, etc.) |

## Build & Test

```bash
mvn compile          # build
mvn test             # run tests (uses MockWebServer, no real network)
mvn package          # build jar
mvn exec:java -Dexec.mainClass="com.universitydata.UniversityDataDemo"  # run demo
```

## Rate Limits

| Source | Limit |
|--------|-------|
| College Scorecard | 1,000 req/hr per key |
| BLS (no key) | 25 req/day |
| BLS (with key) | 500 req/day |
| O\*NET | Not published; be polite |
| IPEDS | Download file once, cache it |
| BLS OOH (scrape) | Use `scrapeDelayMs(1500)` minimum |

## Notes

- **IPEDS data is large** (~50MB compressed). It's downloaded once and cached. Set `ipedsLocalCacheDir` to a persistent path.
- **OOH scraping** takes ~15 minutes for all ~900 pages. Run once, persist results to a database.
- **`MajorOutcomeSummary`** makes up to 5 network calls. Cache results, don't call per-request.
- The `UniversityDataService` exposes individual clients via `getScorecardClient()`, `getOewsClient()`, etc. for advanced use.
