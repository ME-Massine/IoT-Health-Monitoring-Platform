package com.iothealth.backend.repository;

import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceCode(String deviceCode);

    Optional<Device> findByPatient(Patient patient);

    boolean existsByDeviceCode(String deviceCode);

    boolean existsByPatient(Patient patient);
}