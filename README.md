# AI Test Management Tool

**Version**: 2.0-SNAPSHOT

AI-assisted QA platform for generating, storing, reviewing, exporting, and automating test cases. The app is a Spring Boot web application with a static browser UI, PostgreSQL persistence, Flyway migrations, Azure OpenAI integration, and a Jira/GitHub integration layer for pulling sprint context directly into test generation.

## Current Capabilities

- Generate structured test cases from requirements text.
- Upload and extract requirement text from PDF, DOCX, TXT, DOC, JPG, PNG, and JPEG files.
- Select user guide folders as extra workflow/context for generation.
- Chat with an AI QA assistant for scenario analysis and iterative test case generation.
- Store generated test cases in PostgreSQL and manage them from the Library tab.
- Export test cases to CSV or Excel-compatible files.
- Generate Selenium or Playwright automation code from selected test cases.
- Fetch Jira ticket details and auto-fill the Test Cases generation input.
- Fetch one or more GitHub PRs and auto-fill regression/change context.
- Store integration credentials encrypted server-side.
- Persist integration audit logs and integration cache records in PostgreSQL.

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend | Spring Boot 3.2.0 |
| Java | Java 17+ |
| Build | Maven 3.9.6 |
| Database | PostgreSQL |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| AI | Azure OpenAI, with OpenAI fallback configuration |
| Frontend | HTML, CSS, vanilla JavaScript |
| Document Processing | Apache PDFBox, Apache POI |
| Search/Embeddings | pgvector-backed tables where available |

## Prerequisites

- Java 17 or later
- PostgreSQL 12 or later
- Azure OpenAI credentials for AI generation, or an OpenAI API key if using fallback paths
- Maven 3.9.6 or the bundled Maven at `tools/apache-maven-3.9.6`

## Configuration

Create a `.env` file in the project root. You can start from `.env.example`.

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=qa_automation_db
DB_USER=postgres
DB_PASSWORD=your_postgres_password

AZURE_OPENAI_ENABLED=true
AZURE_OPENAI_API_KEY=your_azure_openai_key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_DEPLOYMENT_GPT4=gpt4-turbo
AZURE_OPENAI_DEPLOYMENT_EMBEDDING=text-embedding-3-small
AZURE_OPENAI_API_VERSION=2024-02-15-preview

OPENAI_API_KEY=sk-your-openai-key

# Used to encrypt integration tokens stored in PostgreSQL.
# Use a 16, 24, or 32 character value in real environments.
ENCRYPTION_SECRET_KEY=change-this-32-character-secret
```

The application default server settings are in `src/main/resources/application.yml`:

- Port: `8081`
- Context path: `/testmanagement`
- Login page: `http://localhost:8081/testmanagement/login.html`
- Dashboard: `http://localhost:8081/testmanagement/index.html`
- Integration settings page: `http://localhost:8081/testmanagement/integrations/settings`

## Database Setup

Create the database before first run:

```powershell
psql -U postgres -c "CREATE DATABASE qa_automation_db;"
```

Flyway runs automatically on startup and creates the application tables, including:

- `users`
- `test_cases`
- `test_case_libraries`
- `conversations`
- `integration_credentials`
- `integration_audit_logs`
- `integration_cache_entries`

The seed migration creates an initial admin user:

```text
username: admin
password: admin123
```

Change this immediately for any shared or non-local environment.

## Build

Using bundled Maven on Windows:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests
```

Compile only:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd -DskipTests compile
```

Using a locally installed Maven:

```powershell
mvn clean package -DskipTests
```

## Run

With the helper script:

```powershell
.\start.ps1
```

Or with Maven:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Or from the packaged jar:

```powershell
java -jar target\ai-testmanagement-tool-2.0-SNAPSHOT.jar
```

Open:

```text
http://localhost:8081/testmanagement/login.html
```

## Main Workflows

### Generate Test Cases

1. Log in and open `Test Cases`.
2. Use the `Generate` tab.
3. Enter requirements manually, upload a file, fetch a Jira ticket, or fetch GitHub PR context.
4. Optionally select user guide folders.
5. Click `Generate Test Cases`.
6. Review results and manage saved cases in the `Library` tab.

### Fetch Jira Context

1. Configure Jira credentials in `Jira Integration` or `/integrations/settings`.
2. In `Test Cases > Generate`, enter `PROJ-123` or a Jira browse URL.
3. Click `Fetch`.
4. The app appends title, description, acceptance criteria, status, assignee, labels, linked issues, and source URL to the requirements input.

### Fetch GitHub PR Context

1. Configure a GitHub token in `/integrations/settings`.
2. In `Test Cases > Generate`, paste one or more PR URLs or `owner/repo#123` references.
3. Click `Fetch PR Context`.
4. The app appends PR metadata, file changes, diff summary, and detected scope categories to the requirements input.

### Chat

1. Open `Chat`.
2. Ask for QA analysis, test ideas, or changes to generated cases.
3. Use conversation actions such as regenerate, modify, add more, merge, similar, duplicates, and export.

### Generate Automation Code

1. Open `Test Cases > Library`.
2. Select test cases.
3. Generate Selenium or Playwright code.
4. Review the generated artifact before using it in a test suite.

## API Endpoints

All endpoints are under `/testmanagement`.

### Authentication

- `POST /auth/login`
- `POST /auth/signup`
- `GET /auth/status`
- `GET /logout`

### Health

- `GET /health`
- `GET /health/check`

### Test Cases

- `GET /testcases`
- `DELETE /testcases`
- `GET /testcases/{id}`
- `GET /testcases/filter`
- `GET /testcases/by-application?name=...`
- `GET /testcases/by-module?name=...`
- `GET /testcases/metrics`

### Test Case Generation

- `POST /agents/testcases`
- `POST /agents/testcases/generate`
- `POST /agents/testcases/generate/with-guide`
- `GET /agents/testcases`
- `GET /agents/testcases/{testCaseId}`
- `GET /agents/testcases/guides/folders`
- `GET /agents/testcases/guides/available`
- `POST /agents/testcases/library/save`
- `GET /agents/testcases/library`
- `GET /agents/testcases/library/{libraryId}`
- `GET /agents/testcases/library/search`
- `POST /agents/testcases/library/delete-multiple`
- `DELETE /agents/testcases/library/{libraryId}`
- `GET /agents/testcases/library/category/{category}`
- `GET /agents/testcases/library/public/all`
- `POST /agents/testcases/library/{libraryId}/use`

### Chat

- `POST /chat/start`
- `POST /chat/message`
- `GET /chat/{conversationId}`
- `POST /chat/{conversationId}/regenerate`
- `POST /chat/{conversationId}/modify`
- `POST /chat/{conversationId}/add-more`
- `POST /chat/{conversationId}/merge`
- `GET /chat/{conversationId}/similar`
- `GET /chat/{conversationId}/duplicates`
- `GET /chat/{conversationId}/export`

### Files

- `POST /files/extract`
- `GET /files/{fileId}`
- `POST /chat/files/upload`
- `DELETE /chat/files/{attachmentId}`

### Code Generation

- `POST /agents/code`
- `POST /agents/code/from-testcases`
- `GET /agents/code/{artifactId}`

### Jira and GitHub Integrations

- `GET /api/integrations/jira/ticket/{ticketIdOrUrl}`
- `POST /api/integrations/github/pr`
- `POST /api/integrations/settings`
- `GET /api/integrations/settings`
- `DELETE /api/integrations/settings/{tokenType}`
- `POST /api/integrations/test-connections`
- `GET /api/integrations/audit-logs?limit=50`
- `GET /integrations/settings`

GitHub PR request examples:

```json
{ "url": "https://github.com/owner/repo/pull/123" }
```

```json
{ "urls": ["owner/repo#123", "https://github.com/owner/repo/pull/124"] }
```

```json
{ "urls": "owner/repo#123\nowner/repo#124" }
```

## Project Structure

```text
src/main/java/com/qaautomation
  agents/          AI agents and document/user-guide processors
  cache/           Integration response cache service
  config/          Spring, security, Azure OpenAI, and HTTP client config
  controller/      Health controller package
  controllers/     REST and view controllers
  dto/             API DTOs
  models/          JPA entities and request/response models
  rate_limiting/   Integration API rate limiting
  repositories/    Spring Data repositories
  services/        Business services
  utils/           Utility classes

src/main/resources
  application.properties
  application.yml
  application-integration.properties
  db/migration/    Flyway migrations
  static/          Browser UI assets
  templates/       Thymeleaf templates
```

## Integration Notes

- Jira and GitHub tokens are submitted to backend settings endpoints and stored encrypted in PostgreSQL.
- Tokens are not returned by settings APIs; responses mask configured tokens.
- Audit logs are stored in `integration_audit_logs` and retained by service cleanup logic for 30 days.
- Integration response cache uses `integration_cache_entries` with a 5-minute TTL.
- GitHub fetch supports multiple PRs in one request.
- Large GitHub PRs hide raw patches and return file list plus summary.
- Sensitive files such as `.env`, credentials, keys, and token files are flagged and their patch content is not returned.
- The current UI wires GitHub regression context into the Test Case generation flow rather than a separate Regression tab.
- The cache is database-backed in this codebase, not Redis-backed.

## Troubleshooting

### Database

```powershell
psql -U postgres -c "\l"
psql -U postgres -d qa_automation_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

### Application

```powershell
curl http://localhost:8081/testmanagement/health/check
netstat -aon | Select-String ":8081"
```

### Build

```powershell
java -version
.\tools\apache-maven-3.9.6\bin\mvn.cmd -DskipTests clean compile
```

### AI/API

- Verify Azure OpenAI endpoint, deployment names, API key, and API version.
- Check API quota and network access.
- Review Spring Boot logs for failed external API requests.

### Jira/GitHub

- Save credentials before fetching Jira or GitHub context.
- Jira accepts `PROJ-123` or a full `/browse/PROJ-123` URL.
- GitHub accepts full PR URLs or `owner/repo#number`.
- Private repositories require a GitHub token with repository read access.

## Security

Do not commit:

- `.env`
- API keys
- Database passwords
- Jira, GitHub, or GitLab tokens
- Private keys or certificates

Use environment variables and server-side integration settings for sensitive values. Change the default seeded admin password before using this outside a local development environment.

## Verification

The current integration changes compile with:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd -q -DskipTests compile
```

## Repository

Repository: `https://github.com/Nandigouda/TestManagement-Tool`
