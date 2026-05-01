# Agentic Code Review Assistant

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35-1C3D5A)
![Claude](https://img.shields.io/badge/Claude-Opus%204.7-D97757?logo=anthropic&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector-0.7-4169E1)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-AWS-7B42BC?logo=terraform&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

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

## Screenshots

> 🎨 Screenshots coming soon. The React dashboard and live agent reasoning views are in development.
>
> Once available, this section will include:
> - Live review viewer (SSE streaming reasoning + findings)
> - Per-repository policy configuration page
> - Grafana dashboard showing token usage and review latency
> - Example inline GitHub PR comment posted by the agent

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

See [ARCHITECTURE.md](ARCHITECTURE.md) for a full walkthrough of how a PR flows through the system. Short version of the build phases:

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

## Project status

This project is under active development. Current progress:

- [x] Project scaffold and architecture
- [x] Spring Boot + LangChain4j integration
- [x] PostgreSQL schema with pgvector
- [x] Kafka event orchestration
- [ ] GitHub webhook integration (in progress)
- [ ] React dashboard for live review viewing
- [ ] AWS deployment with Terraform
- [ ] Prometheus/Grafana observability

Built and maintained by [@jayapasam9-max](https://github.com/jayapasam9-max).
