# Known issues

This document tracks issues encountered while setting up and running the project locally. Each entry includes the symptom, the cause, and the workaround or planned fix.

## 1. Maven Wrapper (`mvnw`) missing

**Symptom**

Running `./mvnw spring-boot:run` from inside the `backend/` directory produces:

**Cause**

The repository contains `backend/pom.xml` but not the Maven Wrapper files (`mvnw`, `mvnw.cmd`, and the `.mvn/` directory). The README instructs users to run `./mvnw`, which won't work without these files.

**Workaround**

Install Maven globally and use `mvn` instead of `./mvnw`:

```bash
brew install maven
cd backend
mvn spring-boot:run
```

**Planned fix**

Generate the Maven Wrapper and commit it to the repo:

```bash
cd backend
mvn wrapper:wrapper
git add mvnw mvnw.cmd .mvn
git commit -m "Add Maven Wrapper for portable builds"
```

This is tracked for a future commit.

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