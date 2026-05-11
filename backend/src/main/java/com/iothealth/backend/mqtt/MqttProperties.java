package com.iothealth.backend.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProperties(

        @DefaultValue("false")
        boolean enabled,

        @DefaultValue("tcp://localhost:1883")
        String brokerUrl,

        @DefaultValue("iot-health-backend")
        String clientId,

        @DefaultValue("iot-health/devices/+/vitals")
        String topicPattern,

        @DefaultValue("1")
        int qos
) {
}
