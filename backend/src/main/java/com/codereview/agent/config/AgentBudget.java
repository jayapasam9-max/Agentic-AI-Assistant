package com.codereview.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-PR cost guardrails for the agent.
 *
 * <p>Three limits, in decreasing order of how reliably they are enforced today:
 * <ul>
 *   <li>{@code maxDiffBytes} — <strong>enforced</strong>. The orchestrator
 *       checks the fetched diff size and skips the AI review entirely if it
 *       exceeds the cap, before any Claude call. This is the most important
 *       single guard: a 50K-line PR can burn hundreds of thousands of input
 *       tokens before the agent even starts producing findings.</li>
 *   <li>{@code maxInputTokensPerPr} / {@code maxOutputTokensPerPr} —
 *       <strong>declared but not yet enforced</strong>. LangChain4j 0.35's
 *       {@code AiServices} doesn't expose a hook between tool-call iterations
 *       to inspect cumulative {@code TokenUsage}, so we can't break out of a
 *       runaway loop based on token budget alone. Once we upgrade to a
 *       version with that hook (or hand-roll the tool-call loop ourselves),
 *       these turn into real caps. Tracked in {@code KNOWN_ISSUES.md} #5.</li>
 * </ul>
 *
 * <p>Defaults live in {@code application.yml} (generous, for the local Kafka
 * path); {@code application-cloud-free.yml} overrides with tighter values for
 * the free-tier deploy where every cent counts.
 */
@ConfigurationProperties(prefix = "agent.budget")
public record AgentBudget(
        long maxDiffBytes,
        long maxInputTokensPerPr,
        long maxOutputTokensPerPr
) {
}
