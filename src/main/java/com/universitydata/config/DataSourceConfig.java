package com.universitydata.config;

public class DataSourceConfig {

    private final String collegeScorecardApiKey;
    private final String collegeScorecardBaseUrl;
    private final int collegeScorecardPageSize;
    private final String blsApiKey;
    private final String blsBaseUrl;
    private final String onetApiKey;
    private final String onetBaseUrl;
    private final String ipedsBaseUrl;
    private final String ipedsLocalCacheDir;
    private final String oohBaseUrl;
    private final int scrapeDelayMs;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int maxRetries;

    private DataSourceConfig(Builder b) {
        this.collegeScorecardApiKey   = b.collegeScorecardApiKey;
        this.collegeScorecardBaseUrl  = b.collegeScorecardBaseUrl;
        this.collegeScorecardPageSize = b.collegeScorecardPageSize;
        this.blsApiKey                = b.blsApiKey;
        this.blsBaseUrl               = b.blsBaseUrl;
        this.onetApiKey               = b.onetApiKey;
        this.onetBaseUrl              = b.onetBaseUrl;
        this.ipedsBaseUrl             = b.ipedsBaseUrl;
        this.ipedsLocalCacheDir       = b.ipedsLocalCacheDir;
        this.oohBaseUrl               = b.oohBaseUrl;
        this.scrapeDelayMs            = b.scrapeDelayMs;
        this.connectTimeoutSeconds    = b.connectTimeoutSeconds;
        this.readTimeoutSeconds       = b.readTimeoutSeconds;
        this.maxRetries               = b.maxRetries;
    }

    public String getCollegeScorecardApiKey()   { return collegeScorecardApiKey; }
    public String getCollegeScorecardBaseUrl()  { return collegeScorecardBaseUrl; }
    public int    getCollegeScorecardPageSize() { return collegeScorecardPageSize; }
    public String getBlsApiKey()                { return blsApiKey; }
    public String getBlsBaseUrl()               { return blsBaseUrl; }
    public String getOnetApiKey()               { return onetApiKey; }
    public String getOnetBaseUrl()              { return onetBaseUrl; }
    public String getIpedsBaseUrl()             { return ipedsBaseUrl; }
    public String getIpedsLocalCacheDir()       { return ipedsLocalCacheDir; }
    public String getOohBaseUrl()               { return oohBaseUrl; }
    public int    getScrapeDelayMs()            { return scrapeDelayMs; }
    public int    getConnectTimeoutSeconds()    { return connectTimeoutSeconds; }
    public int    getReadTimeoutSeconds()       { return readTimeoutSeconds; }
    public int    getMaxRetries()               { return maxRetries; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String collegeScorecardApiKey  = "";
        private String collegeScorecardBaseUrl = "https://api.data.gov/ed/collegescorecard/v1";
        private int    collegeScorecardPageSize = 100;
        private String blsApiKey               = "";
        private String blsBaseUrl              = "https://api.bls.gov/publicAPI/v2";
        private String onetApiKey              = "";
        private String onetBaseUrl             = "https://api-v2.onetcenter.org";
        private String ipedsBaseUrl            = "https://nces.ed.gov/ipeds/use-the-data";
        private String ipedsLocalCacheDir      = System.getProperty("java.io.tmpdir") + "/ipeds-cache";
        private String oohBaseUrl              = "https://www.bls.gov/ooh";
        private int    scrapeDelayMs           = 1500;
        private int    connectTimeoutSeconds   = 15;
        private int    readTimeoutSeconds      = 30;
        private int    maxRetries              = 3;

        public Builder collegeScorecardApiKey(String k)  { this.collegeScorecardApiKey = k; return this; }
        public Builder collegeScorecardBaseUrl(String u) { this.collegeScorecardBaseUrl = u; return this; }
        public Builder collegeScorecardPageSize(int s)   { this.collegeScorecardPageSize = s; return this; }
        public Builder blsApiKey(String k)               { this.blsApiKey = k; return this; }
        public Builder blsBaseUrl(String u)              { this.blsBaseUrl = u; return this; }
        public Builder onetApiKey(String k)              { this.onetApiKey = k; return this; }
        public Builder onetBaseUrl(String u)             { this.onetBaseUrl = u; return this; }
        public Builder ipedsBaseUrl(String u)            { this.ipedsBaseUrl = u; return this; }
        public Builder ipedsLocalCacheDir(String d)      { this.ipedsLocalCacheDir = d; return this; }
        public Builder oohBaseUrl(String u)              { this.oohBaseUrl = u; return this; }
        public Builder scrapeDelayMs(int ms)             { this.scrapeDelayMs = ms; return this; }
        public Builder connectTimeoutSeconds(int s)      { this.connectTimeoutSeconds = s; return this; }
        public Builder readTimeoutSeconds(int s)         { this.readTimeoutSeconds = s; return this; }
        public Builder maxRetries(int r)                 { this.maxRetries = r; return this; }

        public DataSourceConfig build() {
            if (collegeScorecardApiKey == null || collegeScorecardApiKey.isBlank()) {
                throw new IllegalStateException("collegeScorecardApiKey is required");
            }
            return new DataSourceConfig(this);
        }
    }
}
