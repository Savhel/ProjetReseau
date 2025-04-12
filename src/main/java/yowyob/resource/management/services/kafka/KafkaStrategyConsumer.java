package yowyob.resource.management.services.kafka;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.beans.factory.annotation.Autowired;
import yowyob.resource.management.services.strategy.StrategyEntityManager;

@Getter
@Service
@EnableKafka
public class KafkaStrategyConsumer {
    private final StrategyEntityManager strategyEntityManager;
    private static final Logger logger = LoggerFactory.getLogger(KafkaStrategyConsumer.class);

    @Autowired
    public KafkaStrategyConsumer(StrategyEntityManager strategyEntityManager) {
        this.strategyEntityManager = strategyEntityManager;
        logger.info("KafkaStrategyConsumer initialized successfully");
    }

    @KafkaListener(topics = "${kafka.strategy-consume.topic}", groupId = "${kafka.strategy-consume.group-id}")
    public synchronized void consume(ConsumerRecord<String, String> record) {
        String strategy = record.key() != null ? record.key() : "{}";
        strategyEntityManager.processStrategy(strategy);
        logger.info("Received Kafka record - Key: {}, Partition: {}, Offset: {}", strategy, record.partition(), record.offset());
    }
}