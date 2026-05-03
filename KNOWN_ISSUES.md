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