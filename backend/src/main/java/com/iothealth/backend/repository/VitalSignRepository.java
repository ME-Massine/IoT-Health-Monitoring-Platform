package com.iothealth.backend.repository;

import com.iothealth.backend.entity.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {

    Optional<VitalSign> findTopByPatientIdOrderByRecordedAtDesc(Long patientId);

    List<VitalSign> findByPatientIdOrderByRecordedAtDesc(Long patientId);

    List<VitalSign> findByPatientIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long patientId,
            Instant from,
            Instant to
    );

    List<VitalSign> findByDeviceDeviceCodeOrderByRecordedAtDesc(String deviceCode);
}