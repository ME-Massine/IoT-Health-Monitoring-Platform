package com.iothealth.backend.repository;

import com.iothealth.backend.entity.Alert;
import com.iothealth.backend.entity.AlertSeverity;
import com.iothealth.backend.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();

    List<Alert> findByPatientIdAndResolvedFalseOrderByCreatedAtDesc(Long patientId);

    List<Alert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity);

    List<Alert> findByTypeOrderByCreatedAtDesc(AlertType type);

    List<Alert> findByVitalSignIdOrderByCreatedAtDesc(Long vitalSignId);
}