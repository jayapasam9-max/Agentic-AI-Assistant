# Agentic Code Review Assistant

An autonomous code review agent that analyzes GitHub pull requests, flags security and performance issues, and posts inline comments — backed by Claude, LangChain4j, Kafka, and Postgres with pgvector.

## Repository layout

```
.
├── backend/                  Spring Boot + LangChain4j agent service
│   ├── pom.xml
│   └── src/main/java/com/codereview/agent/
│       ├── agent/            Agent orchestrator + LangChain4j AI service
│       │   ├── CodeReviewerAgent.java
│       │   ├── ReviewOrchestrator.java
│       │   └── tools/        @Tool-annotated methods for Claude to call
│       ├── config/           Spring config (beans, properties, Kafka, LangChain4j)
│       ├── github/           Webhook controller + GitHub API client
│       ├── kafka/            Kafka consumers + event records
│       ├── sse/              Server-Sent Events controller for the live dashboard
│       └── persistence/      JPA entities + repositories
├── prompts/
│   └── system_prompt.md      The core reviewer prompt (the highest-leverage file)
├── infra/                    Terraform for VPC, EKS, RDS, MSK, ECR, observability
│   ├── main.tf
│   └── modules/{vpc,eks,rds,msk,ecr,observability}/
├── docker-compose.yml        Local dev stack (Postgres+pgvector, Kafka)
└── README.md
```

## Quickstart (local)

```bash
# 1. Bring up the local dependencies
docker compose up -d

# 2. Set required environment variables
export ANTHROPIC_API_KEY=sk-ant-...
export GITHUB_APP_ID=...           # or use a PAT for Phase 1
export GITHUB_PRIVATE_KEY=...
export GITHUB_WEBHOOK_SECRET=...

# 3. Run the backend
cd backend && ./mvnw spring-boot:run

# 4. Expose your webhook with ngrok (or cloudflared) and register it on your GitHub App
ngrok http 8080
```

## Building the agent in phases

See the build plan in the project kickoff doc. Short version:

- **Phase 0–1:** GitHub webhook + service → prove the loop (webhook in, comment out)
- **Phase 2:** Schema + pgvector indexing job for repo history
- **Phase 3:** Agent loop — wire LangChain4j to Claude with the five tools
- **Phase 4:** Kafka + SSE — move the review off the request thread, stream to clients
- **Phase 5:** React dashboard for policies + live review viewer
- **Phase 6:** JUnit + Mockito + Testcontainers
- **Phase 7:** Terraform apply; push images to ECR; `kubectl apply`
- **Phase 8:** Prometheus/Grafana dashboards for tokens, latency, tool success rate
- **Phase 9:** Open-source polish — README, demo GIF, license, Show HN

## Agent model

Uses `claude-opus-4-7` as of this scaffold — Anthropic's current frontier model with a 1M-token context window and native tool-calling. Configure in `backend/src/main/resources/application.yml` under `anthropic.model` if you want to swap to Sonnet for cheaper runs.

## Key design choices

- **Agent as LangChain4j AI Service** — `CodeReviewerAgent` is a Java interface with `@SystemMessage` and `@UserMessage` annotations. LangChain4j generates the implementation and handles the tool-calling loop against Claude's native tool-use API.
- **Kafka between webhook and worker** — GitHub expects webhook responses in <10s. Enqueueing to Kafka lets the webhook return immediately while the agent runs asynchronously, and gives horizontal scalability for concurrent PRs.
- **pgvector in the same Postgres as JPA** — avoids standing up a separate vector DB. The HNSW index on `code_embeddings.embedding` with `vector_cosine_ops` handles semantic search over repo history.
- **Flyway owns the schema, not Hibernate** — `ddl-auto: validate` prevents schema drift. LangChain4j's `PgVectorEmbeddingStore` is configured with `createTable=false` so Flyway is the single source of truth.
- **SSE for reasoning stream** — simpler than WebSockets for one-way server→client traffic, works through every proxy, native `EventSource` support in React.

## Guardrails

The system prompt (`prompts/system_prompt.md`) is the single most important file. It defines the severity rubric, the tool-use strategy, and the guardrails that prevent hallucinated findings. Edit it before changing code if review quality is off.

## License

MIT (see LICENSE).
