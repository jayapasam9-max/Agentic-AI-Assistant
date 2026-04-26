# Setup

This document describes the local development environment for working on the Agentic Code Review Assistant.

## Prerequisites

You'll need the following installed on your Mac:

| Tool | Version | Purpose |
|---|---|---|
| **Java** | 21 or newer | Building and running the Spring Boot backend |
| **Docker Desktop** | 24 or newer | Running Postgres + Kafka locally via docker-compose |
| **Git** | Any recent version | Version control |
| **IntelliJ IDEA** | Community or Ultimate | Java IDE (Maven support built-in) |

## My current setup

I built and tested this project on:

- **macOS** (Apple Silicon)
- **Java**: OpenJDK 25 (Temurin) — note: project targets Java 21, but newer JDK works fine
- **Docker Desktop**: 4.70.0
- **Git**: 2.50+
- **IntelliJ IDEA**: Community Edition

## Verifying your install

Run these commands in your terminal — each should print a version number:

```bash
java -version
docker --version
git --version
```

If any of them say `command not found`, install the missing tool before continuing.

## Where things are installed (macOS)

- **Java** — `/Library/Java/JavaVirtualMachines/` (system-wide)
- **Docker Desktop** — `/Applications/Docker.app`
- **Git** — `/usr/bin/git` (built into macOS)
- **IntelliJ** — `/Applications/IntelliJ IDEA CE.app`

## Optional but useful

- **SDKMAN** — a Java version manager. Not required if your existing Java works, but useful if you need to switch between Java versions for different projects. Install with:
```bash
  curl -s "https://get.sdkman.io" | bash
```

## Running the project locally

Once everything above is installed:

```bash
# Clone the repo
git clone https://github.com/jayapasam9-max/Agentic-AI-Assistant.git
cd Agentic-AI-Assistant

# Start Postgres + Kafka
docker compose up -d

# Set required environment variables
export ANTHROPIC_API_KEY=sk-ant-...
export GITHUB_WEBHOOK_SECRET=dev-secret

# Run the backend
cd backend
./mvnw spring-boot:run
```

## Troubleshooting

- **Docker errors about "Cannot connect to the Docker daemon"** — make sure Docker Desktop is running (whale icon in menu bar)
- **`./mvnw: Permission denied`** — run `chmod +x mvnw` first
- **Port 5432 or 9092 already in use** — another Postgres or Kafka is already running on your machine; stop it before running `docker compose up`