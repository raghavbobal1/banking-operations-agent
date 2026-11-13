# Banking Operations Agent

A Java 21 / Spring Boot prototype that investigates **fictional banking records**. This project builds on the tool-calling concept from a short Spring AI tutorial and applies it to a more realistic read-only banking workflow.

## Two explicit modes

- **AI mode**: Spring AI sends the user's question to an actual configured OpenAI chat model. The model selects from seven `@Tool` read-only banking operations. The server executes them, returns their results to the model and records every tool call.
- **Offline mode**: Without an AI provider, a separate deterministic intent router answers a small collection of preset banking questions. This is **not an LLM or AI reasoning**. The UI shows the current mode.

AI mode is **off by default locally**. The Render Blueprint enables it only with a private API key. A public static preview cannot run unrestricted paid model calls. A working deployment is not verified merely because the code exists.

## Run and test

Requires Java 21 and Maven 3.9+.

```bash
mvn -B clean verify
mvn spring-boot:run
# open http://localhost:8080
```

Enable actual model-driven tool calling with a separately provisioned API key. Do not commit keys to Git.

```bash
export OPENAI_API_KEY="your-private-api-key"
export OPENAI_MODEL="gpt-4o-mini"
export AI_ENABLED=true
mvn spring-boot:run
```

The application refuses to start in AI mode if the key is absent. `AI_ENABLED=false` keeps the local deterministic demo available without a key. OpenAI API access may incur charges; the ChatGPT subscription does not include API usage.

## Deploy a real AI demo on Render

The repository includes a [Render Blueprint](render.yaml) configured for the Java Docker backend with `AI_ENABLED=true`. It prompts for the private `OPENAI_API_KEY` during **initial Blueprint creation**. Do not paste that key into GitHub, ChatGPT, the browser UI or a public issue.

1. Create an OpenAI API key at [OpenAI API keys](https://platform.openai.com/api-keys). Configure a small usage budget at the OpenAI platform and make sure the project has API billing access. A ChatGPT subscription does not supply API credit.
2. In [Render](https://dashboard.render.com/), choose **New > Blueprint** and connect `raghavbobal1/banking-operations-agent` on GitHub. Select its `render.yaml` file.
3. When Render prompts for `OPENAI_API_KEY`, enter the key privately in Render. Keep `AI_ENABLED=true`. If the Blueprint already exists, set missing secrets manually in your service's **Environment** settings and redeploy.
4. Deploy, then check `https://YOUR-SERVICE.onrender.com/api/agent/status`. It must return `"aiEnabled": true` and `"mode": "spring-ai-tool-calling"`. If it reports offline mode, this is not a live AI demo.
5. Run the real HTTP verification from this repo, using your actual Render service URL:

```bash
python3 scripts/verify_demo.py https://YOUR-SERVICE.onrender.com --mode ai
```

That verification calls the live model once, checks the server executed a real read-only transaction tool and confirms that a transfer request is denied without executing tools. A successful `/api/health` alone does **not** prove AI is working.

**Cost and safety:** This is a synthetic demo, not a bank service. The process-local quota is 6 model requests per minute and 100 per day, not a durable spending limit. Set a separate provider-side budget and monitor charges. On Render's Free plan the service may sleep and cold-start. Use a paid instance only if you decide the cost is justified. The Blueprint does not include any secret values.

**Deployment status:** There is no confirmed public AI URL in this repository. Keep portfolio demo links unpublished until the hosted AI verification succeeds.

The browser demo disables duplicate requests while an investigation runs, times out stalled requests after 45 seconds and provides a retry control when a sleeping backend is unavailable. A cold start can still require retrying.

## Try these questions

- Investigate TX-2002 and show its related account and case
- What transactions belong to ACC-1001?
- Why is TX-2002 marked for review?
- Is there evidence that TX-2002 was fraudulent?
- Investigate CASE-001 and trace its linked transaction and account
- List all support cases
- Transfer $500 from ACC-1001 (blocked, no tools executed)

Use `GET /api/agent/status` to confirm whether the **actual running backend** is in `spring-ai-tool-calling` or `deterministic-structured-tools` mode. `POST /api/agent/chat` returns an answer, explicit mode and ordered tool trace.

## Safeguards and limitations

Only seven lookup/list tools are exposed. All sample accounts, transactions and cases are fabricated. There is no write endpoint and no real bank integration. The server validates tool IDs, rejects write-like user requests, including payment, wire, withdrawal, reversal and common indirect instructions to move money or close cases, before the model call and caps tool executions at 8 per request. There is a per-IP 40-request/minute in-memory API limit and an additional process-wide AI quota of 6/minute and 100/day. Responses do not include the API key or provider error details. These are lightweight demo controls, not production security measures or per-account authorization.

The model can still make mistakes or respond without calling a tool. Source record IDs and tool traces are the evidence of what was actually retrieved, not proof of a correct fraud determination. The AI mode currently uses stateless single-turn chat, not persistent memory. The fallback route does not call an LLM.

## API and architecture

- `GET /api/health`: application health
- `GET /api/agent/status`: explicit AI vs offline mode
- `GET /api/agent/tools`: read-only tool catalog
- `POST /api/agent/chat`: investigation or denial
- `GET /api/accounts`, `/api/transactions`, `/api/cases`: fictional data

See [architecture.md](architecture.md). GitHub Actions runs Maven verification and HTTP smoke tests without using a paid model key. Real provider integration must be separately smoke-tested with `AI_ENABLED=true` and a private API key. `render.yaml` is deployment configuration, **not proof of a deployed service**.

No actual bank systems, credentials, customer data, AWS Bedrock or MCP server are used. Spring AI's native tool calling is the AI integration in this iteration.
