package com.codereview.agent.config;

import com.codereview.agent.kafka.event.ReviewJobRequested;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, ReviewJobRequested> reviewJobConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.codereview.agent.kafka.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ReviewJobRequested.class.getName());
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(ReviewJobRequested.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReviewJobRequested>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReviewJobRequested> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reviewJobConsumerFactory());
        // 3 concurrent workers per pod — scale via HPA in k8s.
        factory.setConcurrency(3);
        return factory;
    }
}
