package com.codereview.agent.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The LangChain4j AI Service interface defining the code reviewer contract.
 * At runtime, LangChain4j generates an implementation that drives the Claude
 * tool-calling loop using the @Tool-annotated methods on CodeReviewTools.
 * The system prompt is kept short here; the full prompt lives in prompts/system_prompt.md
 * and is injected via a ChatMemoryProvider or prompt-template file in production.
 */
public interface CodeReviewerAgent {

    /**
     * Entry point for reviewing a pull request.
     * When called, LangChain4j builds the prompt with the help of system message
     * (loaded from prompts/system_prompt.md) and gives the message under 10 seconds because of the GitHub policies, defined
     * by the @UserMessage annotation. It then sends this code given by webhook to Claude to review, handles the
     * tool-calling loop automatically, and returns Claude's final output as a String.
     *
     * The returned String contains newline-separated JSON findings, ending with
     * the token <REVIEW_COMPLETE>. The ReviewOrchestrator parses this output that turns into the GitHub comments.
     *
     * @param repo      the GitHub full name (e.g., "octocat/hello-world")
     * @param prNumber  the pull request number as a string
     * @param sha       the head commit SHA of the PR
     * @param diff      the unified diff of the PR changes
     * @return Claude's raw output string, ready for parsing by the orchestrator
     */
    @SystemMessage(fromResource = "prompts/system_prompt.md")
    String reviewPullRequest(
            @V("repo") @UserMessage("Repository: {{repo}}\nPR #{{pr}}\nHead SHA: {{sha}}\n\n" +
                    "Here is the unified diff:\n{{diff}}\n\n" +
                    "Please review this pull request using your tools. Emit each finding as a JSON object " +
                    "on its own line with schema: {file, line, severity, category, message, suggested_fix}. " +
                    "When you are done, output the token <REVIEW_COMPLETE>.")
            String repo,
            @V("pr") String prNumber,
            @V("sha") String sha,
            @V("diff") String diff);
}
