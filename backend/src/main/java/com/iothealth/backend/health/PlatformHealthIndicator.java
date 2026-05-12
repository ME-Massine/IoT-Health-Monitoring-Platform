package com.iothealth.backend.health;

import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.repository.AlertRepository;
import com.iothealth.backend.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("platform")
@RequiredArgsConstructor
public class PlatformHealthIndicator implements HealthIndicator {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;

    @Override
    public Health health() {
        long activeDevices      = deviceRepository.countByStatus(DeviceStatus.ACTIVE);
        long maintenanceDevices = deviceRepository.countByStatus(DeviceStatus.MAINTENANCE);
        long inactiveDevices    = deviceRepository.countByStatus(DeviceStatus.INACTIVE);
        long unresolvedAlerts   = alertRepository.countByResolvedFalse();

        return Health.up()
                .withDetail("activeDevices", activeDevices)
                .withDetail("maintenanceDevices", maintenanceDevices)
                .withDetail("inactiveDevices", inactiveDevices)
                .withDetail("unresolvedAlerts", unresolvedAlerts)
                .build();
    }
}
