# Known issues

This document tracks issues encountered while setting up and running the project locally. Each entry includes the symptom, the cause, and the workaround or planned fix.

## 1. ~~Maven Wrapper (`mvnw`) missing~~ ✅ RESOLVED

**Status:** Resolved on 2026-04-29.

**Original symptom**

Running `./mvnw spring-boot:run` from inside the `backend/` directory produced:

```
zsh: no such file or directory: ./mvnw
```

**Cause**

The repository contained `backend/pom.xml` but not the Maven Wrapper files (`mvnw`, `mvnw.cmd`, and the `.mvn/` directory).

**Fix**

Generated the Maven Wrapper using a system-installed Maven:

```bash
brew install maven
cd backend
mvn wrapper:wrapper
```

This created `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`. The wrapper was committed in commit "Add Maven wrapper for portable builds".

**Verification**

```bash
cd backend
./mvnw --version
# Apache Maven 3.9.15
```

## 2. Docker Compose `version` attribute is obsolete (warning)

**Symptom**

Running `docker compose up -d` shows:

**Cause**

The `version: "3.9"` line at the top of `docker-compose.yml` is from an older Docker Compose schema. Modern Docker Compose ignores this field.

**Workaround**

The warning is harmless — containers still start correctly.

**Planned fix**

Remove the `version` line from `docker-compose.yml`. Tracked for a future commit.

## How issues are added here

When something breaks during setup or local development, add a new section above with:

1. **Symptom** — exact error message or unexpected behavior
2. **Cause** — what's actually wrong
3. **Workaround** — how to keep going for now
4. **Planned fix** — what should happen long-term, if known

## 3. ~~Lombok annotations not generating code (compilation fails)~~ ✅ RESOLVED

**Status:** Resolved on 2026-05-03.

**Original symptom**

Running `./mvnw test` from the `backend/` directory failed with ~47 compilation errors like:

```
cannot find symbol: method setStatus(...)
cannot find symbol: method builder()
cannot find symbol: variable log
```

**Cause**

The `maven-compiler-plugin` was not configured to run Lombok's annotation processor, so none of the generated methods (getters, setters, `builder()`, `log`, etc.) existed at compile time.

**Fix**

Three changes to `backend/pom.xml`:

1. Added a `<lombok.version>1.18.38</lombok.version>` property.
2. Pinned the existing Lombok dependency to `${lombok.version}`.
3. Added a `maven-compiler-plugin` block with Lombok in `annotationProcessorPaths`.

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

**Verification**

```bash
cd backend
./mvnw clean compile
./mvnw clean test -DskipITs
# both: BUILD SUCCESS
```

**Sub-issue encountered while fixing this — JDK version sensitivity**

Two compatibility walls hit before the build went green:

1. **Lombok 1.18.34 fails on JDK 23+** with `ExceptionInInitializerError: TypeTag :: UNKNOWN` — Lombok touches `sun.misc.Unsafe.objectFieldOffset`, which JDK 23 deprecated for removal. Resolved by pinning Lombok 1.18.38, which has the JDK 23/24 patch.
2. **Mockito (Spring Boot 3.3.4 ships 5.11.0 + ByteBuddy 1.14.x) fails on JDK 25** with `Could not modify all classes` — ByteBuddy can't instrument JDK 25 bytecode yet. Resolved by pinning the project to JDK 21 (which the `pom.xml` already declared as the target). See `SETUP.md` for the JDK pin instructions.

**Sub-issue: stale test expectation in `ReviewOrchestratorTest`**

Once the build compiled and Mockito worked, `parsesFindingsFromAgentOutput` failed with `TooManyActualInvocations: wanted 2 times, but was 4`. The test was written before `persistAndPostFinding` was changed to save each finding twice (once for the id, once after the GitHub comment posts to flip `postedToGithub=true`). Fixed the test to expect 4 saves and dedupe captures by reference identity. Production code unchanged.

## 5. ~~Free-tier deploy not live — agent not posting comments~~ ✅ RESOLVED

**Status:** Resolved on 2026-05-11.

**Original symptom**

The `@UserMessage` annotation was placed on the `repo` parameter of `CodeReviewerAgent.reviewPullRequest(...)` instead of on the method. LangChain4j 0.35 treated the parameter value itself as the entire user message, so Claude received only the string `"jayapasam9-max/codereview-sandbox"` — no diff, no instructions. Claude correctly refused to invent findings and submitted a clean review with no comments.

**Fix**

Moved `@UserMessage` from the parameter to the method level in `CodeReviewerAgent.java` so the full template (including `{{diff}}`) is used as the user message body. Commit: `fix(agent): move @UserMessage to method level so diff reaches the prompt`.

**Verification**

Redelivered the webhook via GitHub's App settings. Render logs showed 6 findings emitted and 6 inline comments posted to `jayapasam9-max/codereview-sandbox #1` (2 CRITICAL, 3 HIGH, 1 LOW). Neon shows corresponding rows in `review_jobs` and `review_findings`.

---

## 4. `EmbeddingModel` bean missing — app fails to start (stubbed)

**Status:** Stubbed on 2026-05-05 to unblock startup. Real fix tracked below.

**Symptom**

Running `./mvnw spring-boot:run` (in any profile) fails before serving any traffic with:

```
APPLICATION FAILED TO START
Parameter 1 of constructor in com.codereview.agent.agent.tools.CodeReviewTools
required a bean of type 'dev.langchain4j.model.embedding.EmbeddingModel'
that could not be found.
```

**Cause**

`CodeReviewTools` autowires an `EmbeddingModel` to power the `searchRepoContext` tool (semantic search over indexed repo history). `LangChainConfig` defined the `EmbeddingStore` (pgvector) and `ChatLanguageModel` (Anthropic) but never provided an `EmbeddingModel`. Spring's bean graph fails on startup because the dependency is required, not optional.

**Stub fix**

Added a stub `EmbeddingModel` bean in `LangChainConfig` that returns 1536-dimensional zero vectors (matching the `code_embeddings.embedding` column dimension). The app now boots cleanly. The `searchRepoContext` tool returns no useful results — but it wasn't being used in any verified flow yet, so this is a no-op regression.

**Real fix (planned)**

Replace the stub with one of:

- **Voyage AI** (`langchain4j-voyage-ai`) — Anthropic's recommended embedding model; requires `VOYAGE_API_KEY`.
- **OpenAI text-embedding-3-small** — 1536 dim by default, matches existing pgvector schema.
- **Local ONNX (AllMpnetBaseV2 or similar)** — runs in-process, no API key, but typically 768 dim → requires a Flyway migration to change `vector(1536)` to `vector(768)`.

Tracked for a future commit. Until then, the stub keeps the app bootable so other phases of the project can be verified.