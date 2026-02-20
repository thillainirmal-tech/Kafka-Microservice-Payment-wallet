package com.wallet.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.notification.dto.FraudAlertPayload;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class FraudAlertConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FraudAlertConsumer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${fraud.alert.email.to:security@paymentwallet.local}")
    private String fraudAlertEmailTo;

    @Autowired
    private JavaMailSender javaMailSender;

    @KafkaListener(topics = "fraud_results", groupId = "fraud-alert-email")
    public void consumeFraudResults(ConsumerRecord<String, String> payload) throws JsonProcessingException {
        if (payload == null || payload.value() == null) {
            LOGGER.warn("Received null fraud_results payload, skipping.");
            return;
        }

        FraudAlertPayload fraudAlertPayload = OBJECT_MAPPER.readValue(payload.value(), FraudAlertPayload.class);
        if (fraudAlertPayload.getRequestId() != null) {
            MDC.put("requestId", fraudAlertPayload.getRequestId());
        }

        try {
            if (!fraudAlertPayload.isFraud()) {
                LOGGER.info("Non-fraud transaction received for requestId={}. No alert email sent.",
                        fraudAlertPayload.getRequestId());
                return;
            }

            LOGGER.warn("FRAUD alert received for requestId={} with confidence={}",
                    fraudAlertPayload.getRequestId(), fraudAlertPayload.getConfidence());

            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom("shanmugakannan7549@gmail.com");
            simpleMailMessage.setTo(fraudAlertEmailTo);
            simpleMailMessage.setSubject("Fraud Alert: transaction " + fraudAlertPayload.getRequestId());
            simpleMailMessage.setText("Potential fraud detected. "
                    + "requestId=" + fraudAlertPayload.getRequestId()
                    + ", classification=" + fraudAlertPayload.getClassification()
                    + ", confidence=" + fraudAlertPayload.getConfidence());

            javaMailSender.send(simpleMailMessage);
        } finally {
            MDC.clear();
        }
    }
}
