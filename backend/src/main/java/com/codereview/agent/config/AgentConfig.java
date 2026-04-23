package com.codereview.agent.config;

import com.codereview.agent.agent.CodeReviewerAgent;
import com.codereview.agent.agent.tools.CodeReviewTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public CodeReviewerAgent codeReviewerAgent(
            ChatLanguageModel model,
            CodeReviewTools tools) {
        return AiServices.builder(CodeReviewerAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                // Sized for ~15 tool-call iterations + diff + findings.
                // Opus 4.7 supports 1M context so this is not a hard constraint,
                // but bounding memory keeps per-review token spend predictable.
                .chatMemory(MessageWindowChatMemory.withMaxMessages(60))
                .build();
    }
}
