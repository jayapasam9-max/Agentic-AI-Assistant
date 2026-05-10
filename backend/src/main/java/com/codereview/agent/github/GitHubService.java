package com.codereview.agent.github;

import com.codereview.agent.config.GitHubProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/**
 * Talks to the GitHub REST API directly via {@link HttpClient}.
 *
 * <p>We deliberately avoid the github-api library here for two reasons:
 * (1) the diff-fetch path needs media-type negotiation
 * ({@code Accept: application/vnd.github.diff}) which the library wraps
 * awkwardly, and (2) the inline-comment endpoint takes a {@code line}
 * parameter on the modern REST API that older library versions don't expose
 * cleanly. Direct HTTP keeps the wire format obvious.
 *
 * <p>For a GitHub App in production, swap the {@code Authorization} header
 * from {@code Bearer <pat>} to {@code Bearer <installation-token>} and
 * generate the installation token via JWT signed with the App's private key.
 * Tracked as future work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private static final String GITHUB_API = "https://api.github.com";
    private static final String API_VERSION = "2022-11-28";

    private final GitHubProperties props;
    private final ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Verify the {@code X-Hub-Signature-256} header using HMAC-SHA256 with the
     * webhook secret. Uses constant-time comparison to prevent timing attacks.
     */
    public boolean verifySignature(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("sha256=");
            for (byte b : computed) hex.append(String.format("%02x", b));
            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Fetch the unified diff for a PR via media-type negotiation against the
     * standard {@code GET /repos/{owner}/{repo}/pulls/{number}} endpoint.
     */
    public String fetchPullRequestDiff(String fullName, int prNumber) throws IOException {
        log.info("Fetching diff for {} #{}", fullName, prNumber);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/repos/" + fullName + "/pulls/" + prNumber))
                .header("Authorization", "Bearer " + props.token())
                .header("Accept", "application/vnd.github.diff")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", "codereview-agent")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("GitHub diff fetch failed for " + fullName + " #" + prNumber
                        + ": HTTP " + resp.statusCode() + " — " + resp.body());
            }
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted fetching PR diff", e);
        }
    }

    /**
     * Post a single line-anchored review comment on a PR using
     * {@code POST /repos/{owner}/{repo}/pulls/{number}/comments}.
     *
     * <p>Uses the {@code line} + {@code side=RIGHT} parameters on the modern
     * REST API — anchored to the new version of the file at the given line.
     */
    public void postInlineComment(String fullName, int prNumber, String headSha,
                                  String filePath, int line, String body) throws IOException {
        log.info("Posting inline comment: {} #{} {}:{}", fullName, prNumber, filePath, line);
        String json = mapper.writeValueAsString(Map.of(
                "body", body,
                "commit_id", headSha,
                "path", filePath,
                "line", line,
                "side", "RIGHT"
        ));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/repos/" + fullName + "/pulls/" + prNumber + "/comments"))
                .header("Authorization", "Bearer " + props.token())
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", "codereview-agent")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("GitHub comment post failed for " + fullName + " #" + prNumber
                        + " " + filePath + ":" + line + ": HTTP " + resp.statusCode() + " — " + resp.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted posting comment", e);
        }
    }
}
