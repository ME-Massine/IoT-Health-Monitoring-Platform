package com.iothealth.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT Health Monitoring Platform API")
                        .version("1.0.0")
                        .description("""
                                REST API for the IoT Health Monitoring Platform.
                                Manages patients, devices, vital sign ingestion, \
                                and alert detection.
                                Real-time updates are delivered via WebSocket (STOMP over SockJS) \
                                at /ws.
                                """)
                );
    }
}