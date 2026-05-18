package com.codereview.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-origin config for the React dashboard.
 *
 * <p>The SPA is hosted on Vercel ({@code *.vercel.app}); the backend lives on
 * Render. Browsers therefore preflight every dashboard request. We allow:
 * <ul>
 *   <li>The production Vercel domain and any preview deployment under it</li>
 *   <li>{@code localhost:5173} for Vite dev</li>
 * </ul>
 *
 * <p>Uses {@code allowedOriginPatterns} (not {@code allowedOrigins}) so the
 * wildcard form is supported. We are not using credentials/cookies for the
 * public endpoints, so this stays simple.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/public/**")
                .allowedOriginPatterns(
                        "https://agentic-ai-assistant-self.vercel.app",
                        "https://agentic-ai-assistant-*.vercel.app",
                        "https://*-jayapasam-s-projects.vercel.app",
                        "http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        // The SSE endpoint lives under /api/reviews/{id}/stream (legacy path,
        // pre-public namespace). Day 3 will move it, but for now mirror the
        // CORS config so the dashboard can connect on both profiles.
        registry.addMapping("/api/reviews/**")
                .allowedOriginPatterns(
                        "https://agentic-ai-assistant-self.vercel.app",
                        "https://agentic-ai-assistant-*.vercel.app",
                        "https://*-jayapasam-s-projects.vercel.app",
                        "http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
