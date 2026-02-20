package com.wallet.fraudservice.service;

import com.wallet.fraudservice.dto.FraudResult;
import com.wallet.fraudservice.model.FraudResultEntity;
import com.wallet.fraudservice.repository.FraudResultRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudResultProducer {

    private static final Logger log = LoggerFactory.getLogger(FraudResultProducer.class);

    @Value("${fraud.kafka.producer.topic:fraud_results}")
    private String fraudResultTopic;

    @Autowired
    private FraudResultRepository fraudResultRepository;

    @Autowired
    private KafkaTemplate<String, FraudResult> fraudResultKafkaTemplate;

    @Transactional
    public void publish(FraudResult fraudResult) {
        FraudResultEntity entity = new FraudResultEntity();
        entity.setTransactionId(fraudResult.getId());
        entity.setRequestId(fraudResult.getRequestId());
        entity.setFraud(fraudResult.isFraud());
        entity.setClassification(fraudResult.getClassification());
        entity.setConfidence(fraudResult.getConfidence());
        fraudResultRepository.save(entity);

        String key = fraudResult.getRequestId() == null ? "" : fraudResult.getRequestId();
        fraudResultKafkaTemplate.send(fraudResultTopic, key, fraudResult);
        log.info("Saved and published fraud result for requestId={} to topic={}",
                fraudResult.getRequestId(), fraudResultTopic);
    }
}
