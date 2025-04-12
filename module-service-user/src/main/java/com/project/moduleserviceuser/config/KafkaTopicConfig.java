package com.project.moduleserviceuser.config;

import com.project.moduleserviceuser.component.KafkaTopicManager;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
// ...

@Configuration
@EnableKafka
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, String> getKafkaTemplate() {
        return new KafkaTemplate<>(getProducerFactory());
    }

    private ProducerFactory<String, String> getProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProp());
    }

    private Map<String, Object> producerProp() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // ✅ yml에서 읽음
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    @Bean
    public CommandLineRunner topicInitializer(KafkaTopicManager topicManager) {
        return args -> {
            topicManager.checkAndCreateTopic("question-bookmark", 3, (short) 1);
            topicManager.checkAndCreateTopic("question-bookmark-delete", 3, (short) 1);
            topicManager.checkAndCreateTopic("content-bookmark", 3, (short) 1);
            topicManager.checkAndCreateTopic("content-bookmark-delete", 3, (short) 1);
        };
    }
}

