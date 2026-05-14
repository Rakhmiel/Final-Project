# Major & Occupation Outcomes Explorer

A terminal-based Java app for exploring the relationship between college majors and career outcomes. Data is stored and loaded as JSON, organized by **CIP code** (major) and **SOC code** (occupation), and linked via the O\*NET CIP→SOC crosswalk.

---

## Build

Requires Java 17+ and Maven.

```bash
mvn package -DskipTests
```

This produces a self-contained JAR at `target/my-project-1.0-SNAPSHOT.jar`. Re-run whenever you change the code.

---

## Run

```bash
# Start empty (add records via the menu or load a file from within the app)
java -jar target/my-project-1.0-SNAPSHOT.jar

# Load a JSON file at startup
java -jar target/my-project-1.0-SNAPSHOT.jar data.json
```

---

## JSON Input Format

The file must be a JSON object with two top-level arrays: `majors` and `occupations`. Either array may be empty or omitted.

```json
{
  "majors": [ ... ],
  "occupations": [ ... ]
}
```

### Major record (CIP level)

Each entry in `majors` represents one degree program identified by its CIP code.

```json
{
  "cipCode": "11.0101",
  "cipTitle": "Computer Science",
  "credentialLevel": 3,
  "annualCompletions": 97000,
  "medianEarnings1Yr": 68000,
  "medianEarnings4Yr": 95000,
  "relatedSocCodes": ["15-1252", "15-1211", "15-1299"]
}
```

| Field | Type | Source | Description |
|---|---|---|---|
| `cipCode` | string | — | CIP code, e.g. `"11.0101"` for Computer Science |
| `cipTitle` | string | — | Human-readable major name |
| `credentialLevel` | integer | — | 1=Certificate, 2=Associate, 3=Bachelor's, 5=Master's, 7=Doctoral |
| `annualCompletions` | integer | IPEDS | Degrees awarded nationally per year |
| `medianEarnings1Yr` | integer | College Scorecard | Median earnings 1 year after graduation (USD) |
| `medianEarnings4Yr` | integer | College Scorecard | Median earnings 4 years after graduation (USD) |
| `relatedSocCodes` | string array | O\*NET crosswalk | SOC codes for occupations this major typically leads to |

All fields are optional — missing numeric fields default to `0` and missing arrays default to empty.

---

### Occupation record (SOC level)

Each entry in `occupations` represents one occupation identified by its SOC code.

```json
{
  "socCode": "15-1252",
  "title": "Software Developers",
  "medianPay": 127260,
  "wage10thPct": 72200,
  "wage25thPct": 93500,
  "wage75thPct": 161600,
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
| `socCode` | string | — | 6-digit SOC code, e.g. `"15-1252"` |
| `title` | string | — | Occupation title |
| `medianPay` | number | OOH / OEWS | Median annual pay (USD); OOH value preferred |
| `wage10thPct` | number | OEWS | 10th percentile annual wage (USD) |
| `wage25thPct` | number | OEWS | 25th percentile annual wage (USD) |
| `wage75thPct` | number | OEWS | 75th percentile annual wage (USD) |
| `wage90thPct` | number | OEWS | 90th percentile annual wage (USD) |
| `employmentCount` | integer | OEWS | Total employed nationally |
| `jobGrowthRate10Yr` | number | OOH | Projected 10-year job growth rate (e.g. `25.0` = 25%) |
| `projectedOpenings` | integer | OOH | Projected job openings over 10 years (growth + replacements) |
| `entryLevelEducation` | string | OOH | Required entry-level education, e.g. `"Bachelor's degree"` |
| `brightOutlook` | boolean | O\*NET | `true` if O\*NET flags this as a Bright Outlook occupation |

All fields are optional — missing numeric fields default to `0`/`false`.

---

### Full example

```json
{
  "majors": [
    {
      "cipCode": "11.0101",
      "cipTitle": "Computer Science",
      "credentialLevel": 3,
      "annualCompletions": 97000,
      "medianEarnings1Yr": 68000,
      "medianEarnings4Yr": 95000,
      "relatedSocCodes": ["15-1252", "15-1211"]
    },
    {
      "cipCode": "52.0201",
      "cipTitle": "Business Administration",
      "credentialLevel": 3,
      "annualCompletions": 390000,
      "medianEarnings1Yr": 48000,
      "medianEarnings4Yr": 72000,
      "relatedSocCodes": ["11-1021", "13-1111"]
    }
  ],
  "occupations": [
    {
      "socCode": "15-1252",
      "title": "Software Developers",
      "medianPay": 127260,
      "wage10thPct": 72200,
      "wage25thPct": 93500,
      "wage75thPct": 161600,
      "wage90thPct": 208000,
      "employmentCount": 1847900,
      "jobGrowthRate10Yr": 25.0,
      "projectedOpenings": 162900,
      "entryLevelEducation": "Bachelor's degree",
      "brightOutlook": true
    },
    {
      "socCode": "11-1021",
      "title": "General and Operations Managers",
      "medianPay": 98100,
      "wage10thPct": 46200,
      "wage25thPct": 63400,
      "wage75thPct": 142800,
      "wage90thPct": 208000,
      "employmentCount": 3241900,
      "jobGrowthRate10Yr": 6.0,
      "projectedOpenings": 308900,
      "entryLevelEducation": "Bachelor's degree",
      "brightOutlook": false
    }
  ]
}
```

When you save from the app (menu option 10), the output file uses this same format and can be loaded back in directly.

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
 0. Exit
```

The CIP→SOC link (option 5) is driven by `relatedSocCodes` on each major. An occupation must be loaded in the same session for its data to appear; otherwise the SOC code is shown as `(not loaded)`.
