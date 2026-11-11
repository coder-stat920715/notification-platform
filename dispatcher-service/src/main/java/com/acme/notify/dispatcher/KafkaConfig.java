package com.acme.notify.dispatcher;

import com.acme.notify.common.Channel;
import com.acme.notify.common.TopicNames;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Value("${notify.kafka.replicas:1}")
    private short replicas;
    @Value("${notify.kafka.partitions.high:6}")
    private int highPartitions;
    @Value("${notify.kafka.partitions.medium:6}")
    private int mediumPartitions;
    @Value("${notify.kafka.partitions.batch:12}")
    private int batchPartitions;
    @Value("${notify.kafka.partitions.channel:6}")
    private int channelPartitions;

    /** Dedicated pool for critical traffic: more threads, small polls, never shared with bulk. */
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> highPriorityFactory(
            ConsumerFactory<String, Object> consumerFactory, DefaultErrorHandler errorHandler,
            @Value("${notify.kafka.concurrency.high:4}") int concurrency) {
        return factory(consumerFactory, errorHandler, concurrency);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Object> standardFactory(
            ConsumerFactory<String, Object> consumerFactory, DefaultErrorHandler errorHandler,
            @Value("${notify.kafka.concurrency.standard:2}") int concurrency) {
        return factory(consumerFactory, errorHandler, concurrency);
    }

    private ConcurrentKafkaListenerContainerFactory<String, Object> factory(
            ConsumerFactory<String, Object> cf, DefaultErrorHandler errorHandler, int concurrency) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        f.setConsumerFactory(cf);
        f.setConcurrency(concurrency);                 // threads per pod; total across pods <= partitions
        f.setCommonErrorHandler(errorHandler);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.getContainerProperties().setPollTimeout(500);
        return f;
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".dlt", -1));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
        handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        return handler;
    }

    @Bean
    KafkaAdmin.NewTopics notificationTopics() {
        List<NewTopic> topics = new ArrayList<>();
        topics.add(build(TopicNames.REQ_HIGH, highPartitions));
        topics.add(build(TopicNames.REQ_MEDIUM, mediumPartitions));
        topics.add(build(TopicNames.REQ_BATCH, batchPartitions));
        topics.add(build(TopicNames.REQ_HIGH + ".dlt", 1));
        topics.add(build(TopicNames.REQ_MEDIUM + ".dlt", 1));
        topics.add(build(TopicNames.REQ_BATCH + ".dlt", 1));
        for (Channel c : Channel.values()) {
            for (String t : List.of(TopicNames.channel(c, com.acme.notify.common.Priority.HIGH),
                    TopicNames.channel(c, com.acme.notify.common.Priority.MEDIUM))) {
                topics.add(build(t, channelPartitions));
            }
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    private NewTopic build(String name, int partitions) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build();
    }
}
