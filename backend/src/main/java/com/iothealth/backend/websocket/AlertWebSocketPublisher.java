package com.iothealth.backend.websocket;

import com.iothealth.backend.dto.alert.AlertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertWebSocketPublisher {

    private static final String GLOBAL_ALERTS_TOPIC = "/topic/alerts";
    private static final String PATIENT_ALERTS_TOPIC_TEMPLATE = "/topic/patients/%d/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishAlert(AlertResponse response) {
        messagingTemplate.convertAndSend(GLOBAL_ALERTS_TOPIC, response);

        if (response.patientId() != null) {
            String patientTopic = String.format(PATIENT_ALERTS_TOPIC_TEMPLATE, response.patientId());
            messagingTemplate.convertAndSend(patientTopic, response);
        }
    }
}