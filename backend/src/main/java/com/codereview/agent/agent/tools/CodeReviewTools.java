package com.codereview.agent.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools exposed to the agent. Each @Tool method is discoverable by LangChain4j
 * and becomes a callable tool in the Claude tool-use loop.
 *
 * Keep tool descriptions precise — Claude uses them to decide when to call.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CodeReviewTools {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Tool("Run Semgrep static analysis on a file path within the cloned PR checkout. " +
          "Returns a list of findings with rule IDs, line numbers, and severity. " +
          "Use this for detecting security issues, anti-patterns, and bugs.")
    public String runStaticAnalysis(
            @P("Absolute path to the file to analyze") String filePath) {
        log.info("runStaticAnalysis called: {}", filePath);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "semgrep", "--json", "--quiet", "--config=auto", filePath);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            return output.isEmpty() ? "{\"results\":[]}" : output;
        } catch (IOException | InterruptedException e) {
            log.error("semgrep failed", e);
            return "{\"error\":\"semgrep execution failed: " + e.getMessage() + "\"}";
        }
    }

    @Tool("Scan a dependency manifest (pom.xml, package.json, requirements.txt, Cargo.toml, etc.) " +
          "for known CVEs using Trivy. Returns vulnerabilities with severity, CVE IDs, and affected versions.")
    public String scanDependencies(
            @P("Absolute path to the dependency manifest file") String manifestPath) {
        log.info("scanDependencies called: {}", manifestPath);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "trivy", "fs", "--format", "json", "--quiet", manifestPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            return output.isEmpty() ? "{\"Results\":[]}" : output;
        } catch (IOException | InterruptedException e) {
            log.error("trivy failed", e);
            return "{\"error\":\"trivy execution failed: " + e.getMessage() + "\"}";
        }
    }

    @Tool("Search the repository's historical code and conventions using semantic similarity. " +
          "Use this to find existing patterns before suggesting changes — e.g., 'how does this repo handle error responses' " +
          "or 'existing examples of database transactions'. Returns the top 5 most relevant code snippets with file paths.")
    public String searchRepoContext(
            @P("A natural-language query describing what to look for") String query) {
        log.info("searchRepoContext called: {}", query);
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.6)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        if (matches.isEmpty()) {
            return "No relevant context found in the repository.";
        }
        return matches.stream()
                .map(m -> String.format("---\nScore: %.3f\nFile: %s\n%s",
                        m.score(),
                        m.embedded().metadata().getString("file_path"),
                        m.embedded().text()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Fetch the full content of a file at a specific git ref in the PR. " +
          "Use when you need more context than the diff shows — e.g. to see the full function being modified.")
    public String getFileContent(
            @P("Repository-relative file path") String path,
            @P("Git ref (branch name or SHA)") String ref) {
        log.info("getFileContent called: {} @ {}", path, ref);
        // TODO: implement via GitHubService.getFileContent(path, ref)
        return "TODO: implement file fetch from GitHub";
    }

    @Tool("Get a summary of the repository's coding conventions derived from its existing code. " +
          "Use this once per review to understand the project's style before emitting findings.")
    public String getConventions() {
        log.info("getConventions called");
        // TODO: generate/cache a conventions summary per repo during indexing
        return "Conventions summary not yet computed for this repo.";
    }
}
