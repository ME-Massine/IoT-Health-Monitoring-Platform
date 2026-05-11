package com.iothealth.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.service.VitalSignService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true")
public class MqttVitalSignListener implements MqttCallback {

    private final VitalSignService vitalSignService;
    private final ObjectMapper objectMapper;
    private final MqttProperties mqttProperties;

    private MqttClient mqttClient;

    @PostConstruct
    public void connect() throws MqttException {
        mqttClient = new MqttClient(
                mqttProperties.brokerUrl(),
                mqttProperties.clientId(),
                new MemoryPersistence()
        );

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);

        mqttClient.setCallback(this);
        mqttClient.connect(options);
        mqttClient.subscribe(mqttProperties.topicPattern(), mqttProperties.qos());

        log.info("MQTT listener connected to {} — subscribed to '{}'",
                mqttProperties.brokerUrl(), mqttProperties.topicPattern());
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                log.info("MQTT listener disconnected");
            } catch (MqttException e) {
                log.warn("Error while disconnecting MQTT client: {}", e.getMessage());
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.debug("MQTT message received on topic '{}': {}", topic, payload);

        try {
            MqttPayload mqttPayload = objectMapper.readValue(payload, MqttPayload.class);
            VitalSignRequest request = new VitalSignRequest(
                    mqttPayload.deviceCode(),
                    mqttPayload.heartRate(),
                    mqttPayload.temperature(),
                    mqttPayload.spo2(),
                    mqttPayload.recordedAt()
            );
            vitalSignService.ingestVitalSign(request);
            log.debug("MQTT vital sign ingested for device '{}'", mqttPayload.deviceCode());
        } catch (Exception e) {
            log.error("Failed to process MQTT message on topic '{}': {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {} — automatic reconnect is enabled", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used — this listener only subscribes, never publishes
    }
}
