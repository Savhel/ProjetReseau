package yowyob.resource.management.services.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@EnableKafka
public class KafkaStrategyResponseProducer {
    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> response = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(KafkaStrategyResponseProducer.class);

    @Autowired
    public KafkaStrategyResponseProducer(@Value("${kafka.response.topic}") String responseTopic, KafkaTemplate<String, String> kafkaTemplate) {
        this.topic = responseTopic;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void pushMessage(String message) {
        this.response.add(message);
    }

    public void send() {
        String response = this.response.stream().reduce("", (s1, s2) -> s1 + '\n' + s2);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(this.topic, response);
        future.whenComplete(
                (result, ex) -> {
                    if (ex == null) {
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.info("Message sent to topic {}, in the partition {}, with an offset of {}",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.offset());
                    } else {
                        log.error("Error when sending response");
                    }
                }
        );

        this.response.clear();
    }
}

