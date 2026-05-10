package com.codereview.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub credentials and webhook config.
 *
 * <p>Two auth modes are supported:
 * <ul>
 *   <li>{@code token} — a GitHub Personal Access Token. Used today.
 *       Simpler to set up; one token per project; sufficient for a personal
 *       sandbox repo demo.</li>
 *   <li>{@code appId} + {@code privateKey} — GitHub App installation auth.
 *       Future work; better for multi-tenant / public deployments.</li>
 * </ul>
 *
 * <p>{@code webhookSecret} is the HMAC-SHA256 secret shared with GitHub's
 * webhook config; used to verify {@code X-Hub-Signature-256} on incoming
 * webhook requests.
 */
@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        String appId,
        String privateKey,
        String webhookSecret,
        String token
) {}
