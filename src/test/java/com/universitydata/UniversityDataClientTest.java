package com.universitydata;

import com.universitydata.client.CollegeScorecardClient;
import com.universitydata.client.UniversityDataService;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UniversityDataClientTest {

    private MockWebServer mockServer;
    private DataSourceConfig config;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        String baseUrl = mockServer.url("/").toString().replaceAll("/$", "");

        config = DataSourceConfig.builder()
                .collegeScorecardApiKey("test-key")
                .collegeScorecardBaseUrl(baseUrl)
                .onetApiKey("")
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // ---------------------------------------------------------------
    // College Scorecard tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Scorecard: parsesProgramFields correctly")
    void testScorecardParsesPrograms() throws IOException {
        mockServer.enqueue(new MockResponse()
                .setBody(scorecardSampleResponse())
                .addHeader("Content-Type", "application/json"));

        CollegeScorecardClient client = new CollegeScorecardClient(config);
        List<ScorecardProgram> programs = client.getProgramsByCip("1101");

        assertFalse(programs.isEmpty(), "Should return at least one program");
        ScorecardProgram p = programs.get(0);
        assertEquals("Test University", p.schoolName);
        assertEquals("1101", p.cipCode);
        assertEquals("Computer Science", p.cipTitle);
        assertEquals(5, p.credentialLevel);
        assertEquals(85000, p.medianEarnings4Yr);
        assertEquals(55000, p.medianEarnings1Yr);
    }

    @Test
    @DisplayName("Scorecard: handles empty results gracefully")
    void testScorecardEmptyResults() throws IOException {
        mockServer.enqueue(new MockResponse()
                .setBody("{\"metadata\":{\"total\":0,\"page\":0,\"per_page\":20},\"results\":[]}")
                .addHeader("Content-Type", "application/json"));

        CollegeScorecardClient client = new CollegeScorecardClient(config);
        List<ScorecardProgram> programs = client.getProgramsByCip("9999");

        assertNotNull(programs);
        assertTrue(programs.isEmpty());
    }

    @Test
    @DisplayName("Config: rejects missing API key")
    void testConfigRequiresApiKey() {
        assertThrows(IllegalStateException.class, () ->
                DataSourceConfig.builder().build());
    }

    @Test
    @DisplayName("Config: defaults are set")
    void testConfigDefaults() {
        DataSourceConfig cfg = DataSourceConfig.builder()
                .collegeScorecardApiKey("key")
                .build();
        assertEquals("https://api.data.gov/ed/collegescorecard/v1", cfg.getCollegeScorecardBaseUrl());
        assertEquals(100, cfg.getCollegeScorecardPageSize());
        assertEquals(1500, cfg.getScrapeDelayMs());
        assertEquals(3, cfg.getMaxRetries());
    }

    // ---------------------------------------------------------------
    // Sample response fixtures
    // ---------------------------------------------------------------

    private String scorecardSampleResponse() {
        return """
                {
                  "metadata": { "total": 1, "page": 0, "per_page": 100 },
                  "results": [
                    {
                      "latest.programs.cip_4_digit": [
                        {
                          "school": { "name": "Test University" },
                          "code": "1101",
                          "title": "Computer Science",
                          "credential": { "level": 5 },
                          "earnings": {
                            "1_yr": { "overall_median_earnings": 55000 },
                            "4_yr": { "overall_median_earnings_national": 85000 }
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
