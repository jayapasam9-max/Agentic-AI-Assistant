# Architecture

This document explains how a pull request flows through the Agentic Code Review Assistant from the moment a developer opens a PR to the moment an inline comment appears on GitHub.

## High-level flow
## Step-by-step

**1. GitHub sends a webhook.** When a developer opens or updates a pull request, GitHub sends a POST request to our `/api/github/webhook` endpoint. The payload includes the PR number, the head commit SHA, the repo name, and the GitHub App installation ID.

**2. The webhook controller verifies the signature.** Using HMAC-SHA256 and the webhook secret, we confirm the request actually came from GitHub (and not someone spoofing it). If the signature doesn't match, we return 401 and stop.

**3. The webhook enqueues a job to Kafka.** GitHub expects webhook responses in under 10 seconds, but an AI code review takes longer than that. So we immediately write a `ReviewJobRequested` event to a Kafka topic and return `202 Accepted`. The heavy work happens asynchronously.

**4. A Kafka consumer picks up the job.** On a separate thread (or even a separate pod in production), the `ReviewJobConsumer` pulls the job off the queue and hands it to the `ReviewOrchestrator`.

**5. The orchestrator invokes the agent.** It fetches the PR diff from GitHub, then calls the LangChain4j AI Service (`CodeReviewerAgent`). LangChain4j handles the tool-calling loop with Claude automatically.

**6. Claude reviews the diff using tools.** Given the diff and the system prompt, Claude decides which tools to call — for example, `runStaticAnalysis` on changed files, `scanDependencies` if the PR touches `pom.xml`, or `searchRepoContext` to find existing patterns in the codebase. Each tool call returns structured data that Claude reads and reasons about.

**7. Findings stream back.** As Claude emits each finding (as a JSON line), the orchestrator parses it, saves it to Postgres, posts it as an inline GitHub comment, and publishes a `ReviewEvent` to Kafka. The React dashboard, connected via Server-Sent Events, receives each event in real time and displays it.

**8. The review completes.** When Claude emits `<REVIEW_COMPLETE>`, the orchestrator marks the job as `COMPLETED` in the database and emits a final SSE event.

## Why these choices?

- **Kafka between the webhook and the worker** — decouples the fast HTTP response from the slow AI review, and allows horizontal scaling for concurrent PRs.
- **pgvector in the same Postgres as JPA** — avoids running a separate vector database. The agent can semantically search the repository's existing code and conventions.
- **Server-Sent Events for streaming** — simpler than WebSockets for one-way server-to-client updates, and works through every proxy.
- **LangChain4j as the agent framework** — gives us a clean Java-native way to define tools and let Claude drive the loop, rather than hand-rolling HTTP calls to the Anthropic API.