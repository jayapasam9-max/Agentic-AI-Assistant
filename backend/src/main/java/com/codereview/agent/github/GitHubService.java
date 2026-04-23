package com.codereview.agent.github;

import com.codereview.agent.config.GitHubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Thin wrapper over org.kohsuke:github-api.
 *
 * For a GitHub App in production, use installation tokens scoped to each install
 * rather than a single PAT. The current impl uses a PAT for Phase 1 simplicity —
 * swap to {@code GitHub.connectAppInstallation()} in Phase 7.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final GitHubProperties props;

    /**
     * Verify the X-Hub-Signature-256 header using HMAC-SHA256 with the webhook secret.
     * Uses constant-time comparison to prevent timing attacks.
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

    public String fetchPullRequestDiff(String fullName, int prNumber) throws IOException {
        GitHub gh = new GitHubBuilder().withOAuthToken(props.privateKey()).build();
        GHPullRequest pr = gh.getRepository(fullName).getPullRequest(prNumber);
        // The github-api library exposes .getDiffUrl(); fetch that URL to get the unified diff.
        // For brevity, this stub returns a placeholder — implement in Phase 1.
        log.info("Fetching diff for {} #{}", fullName, prNumber);
        return "TODO: fetch via pr.getDiffUrl()";
    }

    public void postInlineComment(String fullName, int prNumber, String headSha,
                                  String filePath, int line, String body) throws IOException {
        log.info("Posting inline comment: {} #{} {}:{}", fullName, prNumber, filePath, line);
        // TODO: pr.createReviewComment(body, headSha, filePath, line);
        // Note: GitHub's API uses *position* in the diff, not line number, for some endpoints —
        // use the newer REST review-comments endpoint which supports line/side.
    }
}
