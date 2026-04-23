package com.codereview.agent.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AnthropicProperties.class, GitHubProperties.class})
public class LangChainConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(AnthropicProperties props) {
        return AnthropicChatModel.builder()
                .apiKey(props.apiKey())
                .modelName(props.model())
                .maxTokens(props.maxTokens())
                .logRequests(true)
                .logResponses(false) // responses can be large — enable only when debugging
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // Configure pgvector store. Use the same DB as JPA; pgvector extension must be enabled.
        return PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("codereview")
                .user("postgres")
                .password("postgres")
                .table("code_embeddings")
                .dimension(1536)
                .createTable(false) // Flyway owns the schema
                .build();
    }
}
