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