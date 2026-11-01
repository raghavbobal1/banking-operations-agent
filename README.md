# Banking Operations Agent

A read-only banking investigation console built with Java 21 and Spring Boot. The backend executes deterministic structured tool calls against fabricated records. It is not a generative AI agent and it does not connect to any bank, AWS Bedrock or MCP server.

## Working features

- Investigate `TX-2002` with transaction, account and case lookup in one request.
- Show transactions for `ACC-1001` using two chained read-only tools.
- List cases or inspect an account, transaction or case by ID.
- Inspect a request ID, permission and outcome for every executed tool.
- Reject write requests, unknown identifiers and oversized input.
- Per-IP in-memory chat limit of 40 requests per minute.
- Responsive browser console served by Spring Boot.

All names, balances and transactions are synthetic. The agent does not make a real fraud determination.

## Run locally

Requires JDK 21 and Maven 3.9+.

```bash
mvn -B clean verify
mvn spring-boot:run
```

Open `http://localhost:8080`. The UI checks `/api/health` and refuses to fabricate answers if the backend is unavailable.

```bash
curl -s http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"Investigate TX-2002"}'
```

The JSON response includes `mode: deterministic-structured-tools`, a request ID and an ordered trace.

## API and permissions

| Route | Method | Description |
| --- | --- | --- |
| `/api/health` | GET | Synthetic demo health |
| `/api/agent/tools` | GET | Read-only tool registry |
| `/api/agent/chat` | POST | Validated question and tool execution |
| `/api/accounts` | GET | Synthetic accounts |
| `/api/transactions` | GET | Synthetic transactions |
| `/api/cases` | GET | Synthetic support cases |

The agent uses only `read:accounts`, `read:transactions` and `read:cases`. There is no case creation, transfer or update endpoint. The question limit is 240 characters. The in-memory rate limit is for a demo, not a production security control.

## Architecture and deployment

See [architecture.md](architecture.md) for the Mermaid diagram, boundaries and sample trace. GitHub Actions runs `mvn -B clean verify` on Java 21. `Dockerfile` packages a Java 21 runtime and `render.yaml` defines a Render Docker web service with `/api/health` checks.

A Render account connection and successful public HTTP check are required before calling the backend demo live. The root `index.html` is a static preview. It clearly reports the backend as offline when viewed without Spring Boot.

## Example questions

- Investigate TX-2002
- Show transactions for ACC-1001
- Show account ACC-1002
- List support cases
- Show case CASE-001
- Transfer money from ACC-1001 (denied)

No real bank data, credentials or paid LLM calls are used.