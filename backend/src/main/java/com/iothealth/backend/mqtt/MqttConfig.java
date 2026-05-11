package com.iothealth.backend.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true")
@EnableConfigurationProperties(MqttProperties.class)
public class MqttConfig {
}
