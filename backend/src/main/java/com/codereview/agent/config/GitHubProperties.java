package com.codereview.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        String appId,
        String privateKey,
        String webhookSecret
) {}
