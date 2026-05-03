# Phase 7 — Free-tier deployment plan

This is a step-by-step runbook to take the Agentic Code Review Assistant from
"runs on my laptop with `docker compose up`" to "runs on a public URL for $0/month,"
without throwing away the existing AWS Terraform path (we'll keep that for a future
"production" track).

Target end state after this phase:

- A live Spring Boot service on a public URL (Render free tier).
- Postgres + pgvector on Neon free tier (0.5 GB, branching, autosuspend).
- No Kafka in the free-tier deploy — replaced by an in-process event bus behind
  an interface, gated by a Spring profile. Kafka still works for `docker compose`
  and the AWS Terraform path.
- Claude calls running on Haiku 4.5 with a per-PR token budget, an iteration cap,
  and a diff-size guard so a single noisy PR can't burn through your free credits.
- The Lombok blocker (Known Issue #3) fixed so the project actually compiles.

Total cost target: **$0/month**, with the only spend being optional Anthropic
credits beyond the new-account free tier.

---

## Free-tier stack — and why these picks

| Layer            | Pick                          | Free-tier limit                           | Why                                                                 |
|------------------|-------------------------------|-------------------------------------------|----------------------------------------------------------------------|
| App hosting      | **Render** (Web Service)      | 512 MB RAM, 750 hr/mo, sleeps after 15min | Simplest Docker deploy for a Spring Boot service; one `render.yaml` |
| Postgres+pgvector| **Neon**                      | 0.5 GB storage, autosuspend, branching    | pgvector pre-enabled, fast cold starts, no credit card needed       |
| Event bus        | **In-process** (Spring events)| n/a                                       | Free Kafka tiers have shrunk; in-process is enough for a demo PR    |
| LLM              | **Claude Haiku 4.5**          | Anthropic free credits + budget caps      | ~10x cheaper than Opus per token; same tool-calling capability      |

Alternatives if a pick doesn't fit you:

- App hosting alt: **Fly.io** (3 shared-cpu-1x VMs, auto-stop), better if you need
  always-on; Spring Boot may need `-XX:MaxRAMPercentage=75` to fit 512 MiB.
- Postgres alt: **Supabase** (500 MB; pgvector available behind a flag). Neon is
  preferred because branching makes safer schema migrations.
- Event bus alt: **Upstash Redis Streams** (10K commands/day free) if you really
  want the durability of a broker without paying.
- LLM alt: keep `claude-opus-4-7` but cap `agent.max-iterations` to 4 and add
  a hard token budget — covered in Step 6 either way.

---

## Step 0 — Fix the Lombok blocker (Known Issue #3)

Nothing else compiles until this is done. The fix is a single `pom.xml` change:
wire Lombok into the `maven-compiler-plugin` annotation processor path.

**File:** `backend/pom.xml`

Add a `<lombok.version>` property near the existing `<java.version>`:

```xml
<properties>
    <java.version>21</java.version>
    <lombok.version>1.18.34</lombok.version>
    <langchain4j.version>0.35.0</langchain4j.version>
    <testcontainers.version>1.20.2</testcontainers.version>
</properties>
```

Pin the existing Lombok dependency to that property:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>
```

Add the `maven-compiler-plugin` configuration inside `<build><plugins>`, **above**
the existing `spring-boot-maven-plugin` block:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>${java.version}</source>
        <target>${java.version}</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Verify:**

```bash
cd backend
./mvnw clean compile
# expect: BUILD SUCCESS, no "cannot find symbol" errors
./mvnw test -DskipITs
```

Once green, mark Known Issue #3 as resolved in `KNOWN_ISSUES.md` (same pattern
as Issue #1) and commit:

```
fix(build): wire Lombok into maven-compiler-plugin annotation processor

Resolves KNOWN_ISSUES.md #3 — getters/setters/builders/log now generated.
```

---

## Step 1 — Introduce a `cloud-free` Spring profile

The goal: one codebase, two deploy targets. `default` keeps Kafka + Opus +
local Postgres; `cloud-free` swaps to in-process events + Haiku + Neon.

**Create:** `backend/src/main/resources/application-cloud-free.yml`

```yaml
# Free-tier overlay: Neon Postgres, no Kafka, Haiku, tight budgets.
# Activated via SPRING_PROFILES_ACTIVE=cloud-free on the host.

spring:
  datasource:
    # Neon connection URL — set DB_URL env var on Render
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 3   # Neon free tier likes small pools
      minimum-idle: 0
      idle-timeout: 30000
  kafka:
    # Kafka beans are conditional on this property; setting to empty disables them.
    bootstrap-servers: ""

anthropic:
  model: claude-haiku-4-5-20251001
  max-tokens: 4096

agent:
  max-iterations: 8
  budget:
    max-input-tokens-per-pr: 200000
    max-output-tokens-per-pr: 20000
    max-diff-bytes: 512000     # skip PRs with > 500 KB of diff

review-bus:
  type: in-process              # vs "kafka"
```

**Why two YAMLs and not one:** `application-cloud-free.yml` only loads when
`SPRING_PROFILES_ACTIVE=cloud-free`, so the local Kafka path stays untouched.

---

## Step 2 — Make the event bus pluggable (replace Kafka in cloud-free)

Right now `ReviewJobConsumer` and friends are hard-wired to Kafka. Wrap them
behind a tiny interface so the cloud-free profile can use Spring's in-process
`ApplicationEventPublisher` instead.

**New file:** `backend/src/main/java/com/codereview/agent/bus/ReviewBus.java`

```java
package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;

public interface ReviewBus {
    void publishJob(ReviewJobRequested job);
    void publishEvent(ReviewEvent event);
}
```

**New file:** `backend/src/main/java/com/codereview/agent/bus/KafkaReviewBus.java`

```java
package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "kafka", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaReviewBus implements ReviewBus {
    private final KafkaTemplate<String, Object> kafka;

    @Override
    public void publishJob(ReviewJobRequested job) {
        kafka.send("review-jobs", job.prNumber().toString(), job);
    }

    @Override
    public void publishEvent(ReviewEvent event) {
        kafka.send("review-events", event.prNumber().toString(), event);
    }
}
```

**New file:** `backend/src/main/java/com/codereview/agent/bus/InProcessReviewBus.java`

```java
package com.codereview.agent.bus;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
@RequiredArgsConstructor
public class InProcessReviewBus implements ReviewBus {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishJob(ReviewJobRequested job) {
        publisher.publishEvent(job);
    }

    @Override
    public void publishEvent(ReviewEvent event) {
        publisher.publishEvent(event);
    }
}
```

**Make the existing `ReviewJobConsumer` conditional on Kafka.** Add this
annotation at the class level:

```java
@ConditionalOnProperty(name = "review-bus.type", havingValue = "kafka", matchIfMissing = true)
@KafkaListener(topics = "review-jobs", groupId = "code-review-agent")
public class ReviewJobConsumer { ... }
```

**Add an in-process listener** that reuses the same orchestrator:

**New file:** `backend/src/main/java/com/codereview/agent/bus/InProcessReviewJobListener.java`

```java
package com.codereview.agent.bus;

import com.codereview.agent.agent.ReviewOrchestrator;
import com.codereview.agent.kafka.event.ReviewJobRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
public class InProcessReviewJobListener {
    private final ReviewOrchestrator orchestrator;

    @Async("reviewExecutor")
    @EventListener
    public void onJob(ReviewJobRequested job) {
        log.info("In-process: handling job for PR #{}", job.prNumber());
        orchestrator.review(job);
    }
}
```

**Add the executor.** In `config/`:

**New file:** `backend/src/main/java/com/codereview/agent/config/AsyncConfig.java`

```java
package com.codereview.agent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
public class AsyncConfig {
    @Bean(name = "reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);     // Render free has 0.1 vCPU; serialize jobs
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("review-");
        ex.initialize();
        return ex;
    }
}
```

**Replace direct `KafkaTemplate` calls with `ReviewBus`** in:

- `github/GitHubWebhookController.java` — webhook calls `reviewBus.publishJob(...)`
- `agent/ReviewOrchestrator.java` — finding emission calls `reviewBus.publishEvent(...)`

**Verify:**

```bash
cd backend
SPRING_PROFILES_ACTIVE=cloud-free ./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.kafka.bootstrap-servers="
# webhook should still 202; orchestrator should run on review-1 thread.
```

---

## Step 3 — Provision Postgres on Neon

1. Sign up at https://neon.tech (GitHub login, no card).
2. Create project: name `codereview-free`, region closest to your Render region
   (Render free is Oregon — pick `us-west-2` on Neon to match).
3. In the Neon SQL editor, enable pgvector:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
4. Copy the **pooled** connection string from Neon's dashboard. It looks like:
   ```
   postgresql://user:pass@ep-xxx-pooler.us-west-2.aws.neon.tech/codereview?sslmode=require
   ```
5. Translate to JDBC for Spring (driver wants `jdbc:postgresql://...`):
   ```
   jdbc:postgresql://ep-xxx-pooler.us-west-2.aws.neon.tech/codereview?sslmode=require
   ```

**Verify** Flyway can run against Neon:

```bash
cd backend
DB_URL='jdbc:postgresql://ep-xxx-pooler.us-west-2.aws.neon.tech/codereview?sslmode=require' \
DB_USER=user DB_PASSWORD=pass \
SPRING_PROFILES_ACTIVE=cloud-free \
./mvnw spring-boot:run
# log line: "Successfully applied 1 migration to schema 'public'"
```

Heads-up: Neon's free tier autosuspends after ~5 min idle, so the first request
after a quiet stretch takes ~3–5 s while it wakes up. Document this in your
README.

---

## Step 4 — Add a production Dockerfile

The existing `docker-compose.yml` only runs dependencies, not the app itself.
Render needs a buildable image.

**New file:** `backend/Dockerfile`

```dockerfile
# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
# 512 MB Render container — leave headroom for metaspace + direct buffers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Verify locally:**

```bash
cd backend
docker build -t codereview-agent:free .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=cloud-free \
  -e DB_URL='jdbc:postgresql://...neon.tech/codereview?sslmode=require' \
  -e DB_USER=user -e DB_PASSWORD=pass \
  -e ANTHROPIC_API_KEY=sk-ant-... \
  -e GITHUB_WEBHOOK_SECRET=test-secret \
  codereview-agent:free
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Step 5 — Deploy to Render

**New file:** `render.yaml` (in repo root)

```yaml
# Render Blueprint — https://render.com/docs/blueprint-spec
services:
  - type: web
    name: codereview-agent
    runtime: docker
    plan: free
    region: oregon
    rootDir: backend
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: cloud-free
      - key: DB_URL
        sync: false
      - key: DB_USER
        sync: false
      - key: DB_PASSWORD
        sync: false
      - key: ANTHROPIC_API_KEY
        sync: false
      - key: GITHUB_APP_ID
        sync: false
      - key: GITHUB_PRIVATE_KEY
        sync: false
      - key: GITHUB_WEBHOOK_SECRET
        sync: false
```

**Deploy:**

1. Push the branch to GitHub.
2. Render dashboard → "New +" → "Blueprint" → pick the repo → it reads
   `render.yaml`.
3. Fill in the `sync: false` env vars from Neon + your GitHub App + Anthropic.
4. First build takes ~6–8 min (Maven downloads). Subsequent builds use the
   layer cache and run ~90 s.
5. Note the public URL: `https://codereview-agent.onrender.com`.

**Verify:**

```bash
curl https://codereview-agent.onrender.com/actuator/health
```

The free tier sleeps after 15 min idle and takes ~30 s to wake. For a demo
this is fine; for the Show HN moment you may want to set up a UptimeRobot
ping every 14 min (also free).

---

## Step 6 — LLM cost guardrails

Even on Haiku, an unbounded agent loop on a 50K-line PR can rack up real
spend. Add three guardrails.

**6a. Diff-size guard** in `ReviewOrchestrator`:

```java
if (diff.length() > budget.getMaxDiffBytes()) {
    log.warn("PR #{} diff is {} bytes — over budget {}, skipping AI review",
        job.prNumber(), diff.length(), budget.getMaxDiffBytes());
    githubService.postComment(job, "Skipped automated review — diff too large.");
    return;
}
```

**6b. Token budget tracker.** Bind `agent.budget.*` to a `@ConfigurationProperties`
record `AgentBudget`, then wrap the LangChain4j `AiService` call in a usage
counter. LangChain4j's `Response<AiMessage>` exposes `tokenUsage()`. After each
tool-loop iteration, sum and break out if over budget:

```java
TokenUsage cumulative = TokenUsage.empty();
for (int i = 0; i < maxIterations; i++) {
    Response<AiMessage> resp = agent.review(diff);
    cumulative = cumulative.add(resp.tokenUsage());
    if (cumulative.inputTokenCount() > budget.maxInputTokensPerPr()
        || cumulative.outputTokenCount() > budget.maxOutputTokensPerPr()) {
        log.warn("PR #{}: token budget exhausted at iteration {}", job.prNumber(), i);
        break;
    }
    // ... parse findings, decide whether to continue
}
```

**6c. Iteration cap** is already in `application-cloud-free.yml` as
`agent.max-iterations: 8`. Make sure `ReviewOrchestrator` actually reads it
(`@Value("${agent.max-iterations}")` or via `AgentBudget`).

**Verify:** open a PR with a deliberately huge diff — the run should bail out
within ~1 s with a clear log line, no Claude calls made.

---

## Step 7 — Point GitHub at the deployed webhook

1. Render gave you `https://codereview-agent.onrender.com`. Webhook URL is
   `https://codereview-agent.onrender.com/api/github/webhook`.
2. In your GitHub App settings (Settings → Developer settings → GitHub Apps →
   your app → General):
   - Set "Webhook URL" to the URL above.
   - Set "Webhook secret" to the value you put in Render's `GITHUB_WEBHOOK_SECRET`.
   - Subscribe to events: `Pull request`, `Pull request review`.
3. Install the App on a test repository.
4. Open a tiny PR (e.g. add a comment to a file).

**Verify the loop end-to-end:**

- Render logs show: `Webhook received for PR #N`, then `In-process: handling job…`,
  then Claude tool calls, then `Posting inline comment…`.
- The PR shows an inline review comment from your GitHub App.
- Neon dashboard shows rows in `review_jobs` and `review_findings`.

---

## Step 8 — README updates and commit

In `README.md`, add a "Free-tier deploy" section pointing at this doc. In
`KNOWN_ISSUES.md`, mark Issue #3 resolved. Update the Project Status checklist:

```markdown
- [x] Lombok build fix
- [x] Free-tier deploy (Render + Neon, no Kafka)
- [ ] React dashboard for live review viewing
- [ ] AWS deployment with Terraform (production track)
```

Commit shape:

```
feat(deploy): free-tier deployment path on Render + Neon

- Add `cloud-free` Spring profile (Haiku, no Kafka, tight budgets)
- Introduce ReviewBus interface; in-process impl backed by Spring events
- Add Dockerfile + render.yaml blueprint
- Add per-PR token budget and diff-size guard

The default profile (Kafka + Opus) is unchanged. AWS Terraform path is intact.
```

---

## What this phase deliberately does NOT do

- **No React dashboard yet.** SSE controller still works; pointing a browser at
  it is enough for a demo. Building the React UI is Phase 5 — separate PR.
- **No Terraform changes.** AWS path stays exactly as it is. Free-tier deploy
  is additive: a `render.yaml` + a Dockerfile + a profile.
- **No Prometheus/Grafana.** The Spring Actuator `/actuator/prometheus`
  endpoint is already exposed; Phase 8 will hook it into Grafana Cloud's free
  tier.
- **No multi-tenant auth.** The webhook signature check is the only gate. Fine
  for a personal demo; Phase 9 work if you go SaaS.

---

## Rollback

If anything in cloud-free breaks the local Kafka path:

```bash
cd backend
unset SPRING_PROFILES_ACTIVE
docker compose up -d
./mvnw spring-boot:run
```

Because every cloud-free bean is `@ConditionalOnProperty(...,
havingValue = "in-process")` and the `KafkaReviewBus` defaults via
`matchIfMissing = true`, the local stack is unaffected by all of Step 2.
