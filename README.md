# Economic Success Statistics App

A terminal-based Java app that processes person data from JSON and produces salary rankings and statistics broken down by major, industry, institution, and birth place. Each person receives a **success score** based on their salary relative to US economic benchmarks.

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
# Start empty (add people via the menu or load a file from within the app)
java -jar target/my-project-1.0-SNAPSHOT.jar

# Load a JSON file at startup
java -jar target/my-project-1.0-SNAPSHOT.jar mydata.json
```

---

## JSON Input Format

### Wrapped format (recommended)

```json
{
  "medianWage": 59000,
  "gdpc": 82000,
  "people": [
    {
      "name": "Alice Chen",
      "age": 29,
      "salary": 145000,
      "gpa": 3.9,
      "institution": "MIT",
      "birthPlace": "San Francisco, CA",
      "major": "Computer Science",
      "industry": "Technology"
    },
    {
      "name": "Bob Martinez",
      "age": 34,
      "salary": 88000,
      "gpa": 3.4,
      "institution": "UT Austin",
      "birthPlace": "Houston, TX",
      "major": "Mechanical Engineering",
      "industry": "Manufacturing"
    }
  ]
}
```

`medianWage` and `gdpc` are **optional**. If omitted, the app uses the built-in US defaults ($59,000 and $82,000). When provided, they override the defaults and are applied to every person in the file.

### Plain array format

If you don't need to set benchmarks in the file, you can pass a bare array:

```json
[
  {
    "name": "Alice Chen",
    "age": 29,
    "salary": 145000,
    "gpa": 3.9,
    "institution": "MIT",
    "birthPlace": "San Francisco, CA",
    "major": "Computer Science",
    "industry": "Technology"
  }
]
```

### Field reference

| Field | Type | Description |
|---|---|---|
| `name` | string | Full name |
| `age` | integer | Age in years |
| `salary` | number | Annual salary in USD |
| `gpa` | number | GPA on a 0.0 – 4.0 scale |
| `institution` | string | Educational institution attended |
| `birthPlace` | string | City, state, or country of birth |
| `major` | string | Field of study |
| `industry` | string | Industry currently working in |

All eight fields are required for each person. `medianWage` and `gdpc` are top-level optional fields (wrapped format only).

---

## Menu Options

```
 1. Add person           — enter a person interactively
 2. List all people      — show every person with all fields
 3. Rank by salary       — sorted highest to lowest, flags above median wage
 4. Rank by success score — sorted by score (see below)
 5. Stats by major       — avg / min / max salary per major, ranked
 6. Stats by industry    — same, grouped by industry
 7. Stats by institution — same, grouped by institution
 8. Stats by birth place — same, grouped by birth place
 9. Update benchmarks    — change median wage and GDPC, recalculates all scores
10. Load JSON file       — load (or replace) the dataset from a JSON file
11. Save to JSON file    — save the current dataset to a JSON file
 0. Exit
```

---

## Success Score

Each person receives a score calculated as:

```
benchmark = (medianWage + GDPC) / 2
score     = (salary / benchmark) * 10
```

A score of **10.0** means the person earns exactly the benchmark. Above 10 is above average; below 10 is below average. A person is flagged as **above median wage** (marked with `*`) if their salary exceeds the median wage alone.

Default US benchmarks (2024):
- Median wage: $59,000
- GDP per capita: $82,000
- Benchmark: $70,500

---

## Save Format

When you save (menu option 11), the output file uses the same wrapped JSON format as the input, so it can be loaded back in directly. Only the eight data fields are stored — computed values like the success score are not saved and are always recalculated fresh on load.

---

## Example

```bash
java -jar target/my-project-1.0-SNAPSHOT.jar sample_data.json
```

`sample_data.json` is included in the repo and contains 10 example people across different majors and industries.
