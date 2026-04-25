package com.iothealth.backend.repository;

import com.iothealth.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);
}