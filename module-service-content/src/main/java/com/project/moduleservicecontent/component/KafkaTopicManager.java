package com.project.moduleservicecontent.component;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class KafkaTopicManager {
    private final AdminClient adminClient;

    public KafkaTopicManager(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    public void checkAndCreateTopic(String topicName, int partitions, short replicationFactor) {
        try {
            // 클러스터 내에 존재하는 토픽 목록 조회
            Set<String> topics = adminClient.listTopics().names().get();
            if (!topics.contains(topicName)) {
                NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
                CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
                // 생성 결과가 완료될 때까지 대기
                result.all().get();
                System.out.println("토픽 생성됨: " + topicName);
            } else {
                System.out.println("토픽 이미 존재함: " + topicName);
            }
        } catch (Exception e) {
            // 실제 운영 환경에서는 적절한 로깅 프레임워크를 활용할 것을 권장합니다.
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        if (adminClient != null) {
            adminClient.close();
        }
    }
}
