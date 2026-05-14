package com.universitydata.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Shared HTTP helper wrapping OkHttp.
 * Handles retries with exponential backoff and optional Basic Auth.
 */
public class HttpUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    private final OkHttpClient client;
    private final int maxRetries;

    public HttpUtil(int connectTimeoutSeconds, int readTimeoutSeconds, int maxRetries) {
        this.maxRetries = maxRetries;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    /**
     * GET with no auth.
     */
    public String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * GET with optional Basic Auth credentials ("username:password" encoded).
     */
    public String get(String url, String basicAuthHeader) throws IOException {
        Request.Builder reqBuilder = new Request.Builder().url(url);
        if (basicAuthHeader != null) {
            reqBuilder.header("Authorization", "Basic " + basicAuthHeader);
        }
        Request request = reqBuilder.build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 429) {
                    long waitMs = 2000L * attempt;
                    log.warn("Rate limited by {}. Waiting {}ms before retry {}/{}",
                            url, waitMs, attempt, maxRetries);
                    Thread.sleep(waitMs);
                    continue;
                }
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " for: " + url);
                }
                ResponseBody body = response.body();
                return body != null ? body.string() : "";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during retry wait", e);
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long waitMs = 1000L * attempt;
                    log.warn("Request failed (attempt {}/{}): {}. Retrying in {}ms",
                            attempt, maxRetries, e.getMessage(), waitMs);
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry wait", ie);
                    }
                }
            }
        }
        throw lastException != null ? lastException
                : new IOException("Request failed after " + maxRetries + " retries: " + url);
    }

    /**
     * Convenience: build Basic Auth header from username + password.
     */
    public static String buildBasicAuth(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    /**
     * Polite delay for scraping. Call between page requests to OOH.
     */
    public static void politeDelay(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public String getWithApiKey(String url, String apiKey) throws IOException {
    Request request = new Request.Builder()
            .url(url)
            .header("X-API-Key", apiKey)
            .build();

    IOException lastException = null;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 429) {
                long waitMs = 2000L * attempt;
                log.warn("Rate limited by {}. Waiting {}ms before retry {}/{}", url, waitMs, attempt, maxRetries);
                Thread.sleep(waitMs);
                continue;
            }
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for: " + url);
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during retry wait", e);
        } catch (IOException e) {
            lastException = e;
            if (attempt < maxRetries) {
                long waitMs = 1000L * attempt;
                log.warn("Request failed (attempt {}/{}): {}. Retrying in {}ms", attempt, maxRetries, e.getMessage(), waitMs);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry wait", ie);
                }
            }
        }
    }
    throw lastException != null ? lastException
            : new IOException("Request failed after " + maxRetries + " retries: " + url);
}
}
