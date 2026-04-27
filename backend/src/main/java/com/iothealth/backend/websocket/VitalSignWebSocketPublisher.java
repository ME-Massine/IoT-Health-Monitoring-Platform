package com.iothealth.backend.websocket;

import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VitalSignWebSocketPublisher {

    private static final String GLOBAL_VITALS_TOPIC = "/topic/vitals";
    private static final String PATIENT_VITALS_TOPIC_TEMPLATE = "/topic/patients/%d/vitals";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishVitalSign(VitalSignResponse response) {
        messagingTemplate.convertAndSend(GLOBAL_VITALS_TOPIC, response);

        if (response.patientId() != null) {
            String patientTopic = String.format(PATIENT_VITALS_TOPIC_TEMPLATE, response.patientId());
            messagingTemplate.convertAndSend(patientTopic, response);
        }
    }
}