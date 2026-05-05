package com.codereview.agent.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties({AnthropicProperties.class, GitHubProperties.class})
public class LangChainConfig {

    /** Dimension of the {@code code_embeddings.embedding} column in pgvector. */
    private static final int EMBEDDING_DIMENSION = 1536;

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
                .dimension(EMBEDDING_DIMENSION)
                .createTable(false) // Flyway owns the schema
                .build();
    }

    /**
     * Stub {@link EmbeddingModel} that returns zero vectors of the configured
     * dimension.
     *
     * <p>This unblocks application startup — {@code CodeReviewTools} requires
     * an {@code EmbeddingModel} to be autowired even when the agent never
     * calls a tool that uses it. The stub means the {@code searchRepoContext}
     * tool will return no useful results until a real embedding model is
     * configured (Voyage, OpenAI, or a local ONNX model with a matching
     * dimension). Tracked in {@code KNOWN_ISSUES.md}.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
                List<Embedding> zeros = new ArrayList<>(segments.size());
                for (int i = 0; i < segments.size(); i++) {
                    zeros.add(Embedding.from(new float[EMBEDDING_DIMENSION]));
                }
                return Response.from(zeros);
            }
        };
    }
}
