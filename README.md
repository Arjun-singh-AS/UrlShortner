# Linknest URL Shortener

A polished, responsive URL-shortening dashboard backed by a persistent local H2 database. Create shareable links, choose a custom alias, set an optional expiry date, track redirect clicks, search your links, and delete links from the dashboard.

This is a local, single-workspace demo without user authentication. Add authentication and per-user authorization before exposing it as a shared public service.

## Run locally

Requirements: Java 17+.

From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Link data is stored in the local `urlshortener-data` H2 database and remains available after restarting the app. The database files are excluded from Git.

Run the test suite:

```powershell
.\mvnw.cmd test
```

## Features

- Responsive dashboard with link, click, and active-link totals.
- Generated seven-character short codes or custom aliases (3-32 letters, numbers, hyphens, or underscores).
- Optional expiry date; expired links stop redirecting and return HTTP 410.
- HTTP and HTTPS destination validation.
- Persistent SQL storage, click tracking, link search, and deletion.
- REST API with readable validation and conflict errors.

## API

Create a link:

```http
POST /api/shorten
Content-Type: application/json

{
  "url": "https://example.com/articles/a-long-page",
  "customAlias": "my-article",
  "expiresAt": null
}
```

`customAlias` and `expiresAt` are optional. `expiresAt`, when supplied, must be a future ISO-8601 instant. A successful response includes `shortCode`, `originalUrl`, `shortUrl`, `createdAt`, `expiresAt`, `clickCount`, and `expired`.

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/api/shorten` | Create a short link |
| `GET` | `/api/urls` | List links, newest first |
| `GET` | `/api/urls/{shortCode}` | Get link details |
| `DELETE` | `/api/urls/{shortCode}` | Delete a link |
| `GET` | `/{shortCode}` | Record a click and redirect |

Errors use an `{"error":"..."}` JSON body. Redirects return `302`, missing links return `404`, and expired links return `410`.

## Project walkthrough

Use this as a truthful draft for a project application or interview. Fill in the bracketed details yourself; do not claim team size, dates, Git usage, or private/public status unless they match your experience.

- **Project title:** Linknest — URL Shortener
- **Project setting:** [University / Personal / Internship / work / Open source / Freelance]
- **When:** [Your actual start and end dates]
- **Team size:** [Your actual team size]
- **Languages and tools:** Java 17, Spring Boot, Spring MVC, JDBC, H2, HTML, CSS, JavaScript, Maven, JUnit
- **SQL:** Yes — the project defines a SQL schema and uses JDBC queries for link creation, lookup, click updates, listing, and deletion.
- **Git:** [Answer based on whether you initialize and use Git for this project.]
- **Automated tests:** Yes — JUnit tests cover link creation, aliases, validation, click tracking, expiry, and deletion.
- **How to inspect:** Private work available for a live walkthrough, or [add your public project link after publishing].

### What the project does and what I implemented (under 120 words)

> Linknest is a URL-shortening app with a responsive dashboard for creating and managing short links. I implemented the Spring Boot API, SQL-backed persistence with H2 and JDBC, generated short codes and custom aliases, HTTP/HTTPS URL validation, optional expiry, redirect click tracking, and link deletion. I also built the browser interface for creating, searching, copying, and reviewing links, plus automated tests for the core link lifecycle. The app runs locally with Java and Maven and keeps link data across restarts.

Edit this to accurately reflect what you personally implemented and the scope of any team contribution.

### One bug or unexpected result (under 100 words)

> The original redirect endpoint was not wired correctly: its mapping annotation was missing, it used a nonexistent path-variable annotation, and it called a service method as if it were static. I replaced it with a typed short-code route and a database-backed lookup that increments clicks before redirecting. I checked the behavior with service tests for click counts, expired links, and missing links, then verified the browser flow against the running app.

Update the final sentence only after completing the checks in your own environment.

### AI use (under 80 words)

> I used AI while building and debugging the project. One suggestion I accepted was to use a local H2 database instead of keeping links only in application memory. I checked the SQL schema and JDBC operations, added tests for creation, clicks, expiry, and deletion, and verified that links remain available after an application restart.

Describe only suggestions you actually accepted, changed, or rejected and checks you personally performed. If you did not personally verify persistence across a restart, revise that last sentence before using this response.

## Suggested Git commit phases

The repository files are grouped so each phase can be staged independently. Review the diff, then commit and push each phase yourself.

### Phase 1 — Persistent URL API

```powershell
git add .gitignore pom.xml src/main/java/com/example/URLShortner/UrlData.java src/main/java/com/example/URLShortner/Service/UrlService.java src/main/java/com/example/URLShortner/Controller/ApiController.java src/main/java/com/example/URLShortner/Controller/ApiExceptionHandler.java src/main/java/com/example/URLShortner/Controller/RedirectController.java src/main/resources/application.properties src/main/resources/schema.sql
git commit -m "feat: add persistent URL shortener API"
```

### Phase 2 — Linknest dashboard

```powershell
git add src/main/resources/static/index.html src/main/resources/static/styles.css src/main/resources/static/app.js
git commit -m "feat: add responsive Linknest dashboard"
```

### Phase 3 — Tests and project guide

```powershell
git add src/test/java/com/example/URLShortner/UrlShortnerApplicationTests.java README.md
git commit -m "test: cover URL lifecycle and document the project"
```

Push to your GitHub repository after reviewing each phase commit. This project directory did not contain Git metadata when work began, so initialize/attach your repository as needed before the first commit; no remote is configured and nothing has been pushed.
