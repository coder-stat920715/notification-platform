package com.acme.notify.tracker;

import com.acme.notify.common.TopicNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic statusTopic(@Value("${notify.kafka.status-partitions:6}") int partitions,
                         @Value("${notify.kafka.replicas:1}") short replicas) {
        return TopicBuilder.name(TopicNames.STATUS).partitions(partitions).replicas(replicas).build();
    }
}
