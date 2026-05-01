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

## 3. Lombok annotations not generating code (compilation fails)

**Symptom**

Running `./mvnw test` from the `backend/` directory fails with ~47 compilation errors like:

```
cannot find symbol: method setStatus(...)
cannot find symbol: method builder()
cannot find symbol: variable log
```

These errors appear in every class that uses Lombok annotations — `ReviewOrchestrator.java`, `ReviewJob.java`, `ReviewFinding.java`, `GitHubService.java`, `CodeReviewTools.java`, `GitHubWebhookController.java`, `ReviewJobConsumer.java`, and `ReviewStreamController.java`.

**Cause**

The project uses Lombok (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`) to generate boilerplate code at compile time. The Maven Compiler plugin is not configured to run Lombok's annotation processor, so none of the expected methods (getters, setters, the `builder()` static method, the `log` variable, etc.) get generated.

The result: every reference to a generated method or field is reported as "cannot find symbol".

**Workaround**

None currently — the project does not compile from the command line until this is fixed.

**Planned fix**

Configure the `maven-compiler-plugin` in `backend/pom.xml` to run Lombok as an annotation processor:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
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

This is tracked for a future commit.