# Major & Occupation Outcomes Explorer

## Quick Start

**1. Build**
```bash
mvn package
```

**2. Run**
```bash
java -jar target/my-project-1.0-SNAPSHOT.jar
```

**3. Pull fresh data**

When the menu appears, select **option 11 — Download fresh data**. This fetches live information from College Scorecard, O\*NET, OEWS, and BLS OOH, and automatically loads it into your session. **This is the recommended first step** to fully utilize the program — without it, you'll be working with an empty dataset or the limited sample file.

> ⚠️ The download takes several minutes due to API rate limits. Progress will be printed as it runs.

Once the download completes, all menu features (rankings, major→occupation links, earnings comparisons) will be populated with real data.

---

A terminal-based Java app for exploring the relationship between college majors and career outcomes. Data is stored and loaded as JSON, organized by **CIP code** (major) and **SOC code** (occupation), and linked via the O\*NET CIP→SOC crosswalk.

---

## Build

Requires Java 17+ and Maven.

```bash
mvn package
```

This compiles, runs tests, and produces a self-contained JAR at `target/my-project-1.0-SNAPSHOT.jar`.

> **Note:** `mvn clean compile` only compiles source files — it does not create the JAR. Always use `mvn package` (or `mvn clean package`) to get a runnable JAR.

---

## Run

```bash
# Start empty — add records via the menu or load a file from within the app
java -jar target/my-project-1.0-SNAPSHOT.jar
```

The program defaults to looking for `universitydata.json` in the current directory if no argument is given.

---

## Data File

The program reads a JSON file in the format produced by the `university-data-client` fetching pipeline. It is highly recommended to pull fresh data while running the program. A sample file `universitydata-sample.json` is included in the project root for testing — load it via menu option 9 or pass it as an argument at startup.

To generate a real data file from live APIs, run the `DataFetcher` in the `com.universitydata` package directly (requires a College Scorecard API key and OEWS flat files).

### Format

The file must be a JSON object with two top-level arrays: `majors` and `occupations`. Either array may be empty or omitted.

```json
{
  "majors": [ ... ],
  "occupations": [ ... ]
}
```

#### Major record (CIP level)

```json
{
  "cipCode": "11.0101",
  "cipTitle": "Computer Science",
  "credentialLevel": 3,
  "annualCompletions": 97000,
  "medianEarnings1Yr": 68000,
  "medianEarnings4Yr": 95000,
  "relatedSocCodes": ["15-1252.00", "15-1211.00"]
}
```

| Field | Type | Source | Description |
|---|---|---|---|
| `cipCode` | string | — | CIP code, e.g. `"11.0101"` |
| `cipTitle` | string | — | Human-readable major name |
| `credentialLevel` | integer | — | 1=Certificate, 2=Associate, 3=Bachelor's, 5=Master's, 7=Doctoral |
| `annualCompletions` | integer | IPEDS | Degrees awarded nationally per year |
| `medianEarnings1Yr` | integer | College Scorecard | Median earnings 1 year after graduation (USD) |
| `medianEarnings4Yr` | integer | College Scorecard | Median earnings 4 years after graduation (USD) |
| `relatedSocCodes` | string array | O\*NET crosswalk | SOC codes for occupations this major typically leads to |

#### Occupation record (SOC level)

```json
{
  "socCode": "15-1252.00",
  "title": "Software Developers",
  "medianPay": 130160,
  "wage10thPct": 75700,
  "wage25thPct": 99000,
  "wage75thPct": 165000,
  "wage90thPct": 208000,
  "employmentCount": 1847900,
  "jobGrowthRate10Yr": 25.0,
  "projectedOpenings": 162900,
  "entryLevelEducation": "Bachelor's degree",
  "brightOutlook": true
}
```

| Field | Type | Source | Description |
|---|---|---|---|
| `socCode` | string | — | SOC code, e.g. `"15-1252.00"` |
| `title` | string | — | Occupation title |
| `medianPay` | number | OOH / OEWS | Median annual pay (USD) |
| `wage10thPct` | number | OEWS | 10th percentile annual wage (USD) |
| `wage25thPct` | number | OEWS | 25th percentile annual wage (USD) |
| `wage75thPct` | number | OEWS | 75th percentile annual wage (USD) |
| `wage90thPct` | number | OEWS | 90th percentile annual wage (USD) |
| `employmentCount` | integer | OEWS | Total employed nationally |
| `jobGrowthRate10Yr` | number | OOH | Projected 10-year job growth rate (e.g. `25.0` = 25%) |
| `projectedOpenings` | integer | OOH | Projected job openings over 10 years |
| `entryLevelEducation` | string | OOH | Required entry-level education |
| `brightOutlook` | boolean | O\*NET | `true` if flagged as a Bright Outlook occupation |

All fields are optional — missing numeric fields default to `0`/`false`.

---

## Menu Options

```
 1. Add major (CIP)                  — enter a major record interactively
 2. Add occupation (SOC)             — enter an occupation record interactively
 3. List all majors                  — show every major sorted by 4-year earnings
 4. List all occupations             — show every occupation sorted by median pay
 5. View major → linked occupations  — show a major's detail and its linked occupations
 6. Rank majors by 4-year earnings   — sorted highest to lowest
 7. Rank occupations by median pay   — sorted highest to lowest
 8. Rank occupations by job growth   — sorted highest to lowest (10-year rate)
 9. Load JSON file                   — merge records from a JSON file into the current session
10. Save to JSON file                — write all current records to a JSON file
11. Download fresh data              — fetch live data from APIs and optionally load it
 0. Exit
```

### Option 11 — Download fresh data

Fetches live data from College Scorecard, O\*NET, OEWS, and BLS OOH using built-in API keys and configuration, writes `universitydata.json`, and automatically loads it into the current session. No setup or prompts required.

**This will take several minutes.** The download makes many API calls and scrapes BLS OOH pages with polite delays between requests. The program will print progress as it runs.

After the download completes, the program will offer to load the new file into the current session.

The CIP→SOC link (option 5) is driven by `relatedSocCodes` on each major. An occupation must be loaded in the same session for its data to appear; otherwise the SOC code is shown as `(not loaded)`.

When you save (option 10), the output file uses the same format and can be loaded back in directly.

> **Note:** Occupation titles that exceed 35 characters will push the remaining columns out of alignment. This is expected — for example:
>
> ```
> === Occupations Ranked by 10-Year Job Growth ===
> #    SOC        Title                                Median Pay     Growth     Employed Bright
> ------------------------------------------------------------------------------------------
> 1    13-1081.00 Logisticians                       $    80,880      17.0%        1,900 ★
> 2    13-1081.02 Logistics Analysts                 $   131,450      15.0%            0 ★
> 3    13-1111.00 Management Analysts                $   131,450      15.0%       35,800 ★
> 4    15-1253.00 Software Quality Assurance Analysts and Testers $   131,450      15.0%    1,140 ★
> ```
