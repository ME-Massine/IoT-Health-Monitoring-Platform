package com.iothealth.backend.repository;

import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceCode(String deviceCode);

    Optional<Device> findByPatientId(Long patientId);

    boolean existsByDeviceCode(String deviceCode);

    boolean existsByPatientId(Long patientId);

    long countByStatus(DeviceStatus status);
}