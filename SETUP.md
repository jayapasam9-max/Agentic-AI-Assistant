# Setup

This document describes the local development environment for working on the Agentic Code Review Assistant.

## Prerequisites

You'll need the following installed on your Mac:

| Tool | Version | Purpose |
|---|---|---|
| **Java** | 21 (exact) | Building and running the Spring Boot backend. JDK 23+ breaks Lombok 1.18.34, JDK 25 breaks Mockito's inline mock-maker on Spring Boot 3.3.4. Pin 21. |
| **Docker Desktop** | 24 or newer | Running Postgres + Kafka locally via docker-compose |
| **Git** | Any recent version | Version control |
| **IntelliJ IDEA** | Community or Ultimate | Java IDE (Maven support built-in) |

## My current setup

I built and tested this project on:

- **macOS** (Apple Silicon)
- **Java**: Temurin 21 (pinned). The project initially compiled on Temurin 25 but unit tests failed because Spring Boot 3.3.4 ships a Mockito + ByteBuddy combo that can't instrument JDK 25 classes. See `KNOWN_ISSUES.md` #3 for the full diagnosis.
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

- **SDKMAN** — a Java version manager. Recommended for this project so you can pin JDK 21 without changing your system default:
```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.5-tem
sdk use java 21.0.5-tem
```

Or install Temurin 21 via Homebrew and export `JAVA_HOME` per shell:
```bash
brew install --cask temurin@21
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
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