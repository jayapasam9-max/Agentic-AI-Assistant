-- V1__initial_schema.sql
-- Core tables for the code review agent

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Repositories registered with the agent
CREATE TABLE repositories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    github_full_name TEXT NOT NULL UNIQUE, -- e.g. "octocat/hello-world"
    installation_id BIGINT NOT NULL,
    default_branch  TEXT NOT NULL DEFAULT 'main',
    indexed_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Per-repo review policies (JSON for flexibility on early iteration)
CREATE TABLE review_policies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repository_id   UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    config          JSONB NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (repository_id)
);

-- Review jobs, one per PR review trigger
CREATE TABLE review_jobs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repository_id   UUID NOT NULL REFERENCES repositories(id),
    pr_number       INT NOT NULL,
    head_sha        TEXT NOT NULL,
    status          TEXT NOT NULL, -- QUEUED, RUNNING, COMPLETED, FAILED
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    tokens_input    INT DEFAULT 0,
    tokens_output   INT DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_review_jobs_repo_pr ON review_jobs(repository_id, pr_number);
CREATE INDEX idx_review_jobs_status ON review_jobs(status);

-- Individual findings emitted during a review
CREATE TABLE review_findings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id          UUID NOT NULL REFERENCES review_jobs(id) ON DELETE CASCADE,
    file_path       TEXT NOT NULL,
    line_number     INT,
    severity        TEXT NOT NULL, -- INFO, LOW, MEDIUM, HIGH, CRITICAL
    category        TEXT NOT NULL, -- SECURITY, PERFORMANCE, STYLE, BUG, MAINTAINABILITY
    message         TEXT NOT NULL,
    suggested_fix   TEXT,
    posted_to_github BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_review_findings_job ON review_findings(job_id);

-- Log of every tool call the agent made — crucial for Prometheus metrics & debugging
CREATE TABLE agent_tool_calls (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id          UUID NOT NULL REFERENCES review_jobs(id) ON DELETE CASCADE,
    iteration       INT NOT NULL,
    tool_name       TEXT NOT NULL,
    input_json      JSONB,
    output_json     JSONB,
    duration_ms     INT,
    success         BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tool_calls_job ON agent_tool_calls(job_id);
CREATE INDEX idx_tool_calls_tool ON agent_tool_calls(tool_name);

-- pgvector embedding store for repo code chunks
-- LangChain4j PgVectorEmbeddingStore expects specific columns when createTable=false
CREATE TABLE code_embeddings (
    embedding_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    embedding       vector(1536) NOT NULL,
    text            TEXT NOT NULL,
    metadata        JSONB
);

CREATE INDEX idx_code_embeddings_vector
    ON code_embeddings USING hnsw (embedding vector_cosine_ops);
