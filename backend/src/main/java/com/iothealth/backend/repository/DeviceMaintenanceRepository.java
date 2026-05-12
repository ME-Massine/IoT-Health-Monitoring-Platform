package com.iothealth.backend.repository;

import com.iothealth.backend.entity.DeviceMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceMaintenanceRepository extends JpaRepository<DeviceMaintenance, Long> {

    List<DeviceMaintenance> findByDeviceIdOrderByStartedAtDesc(Long deviceId);

    Optional<DeviceMaintenance> findFirstByDeviceIdAndEndedAtIsNull(Long deviceId);
}
