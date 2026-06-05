package com.homeverse.property.repository;

import com.homeverse.property.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrOwnerIdOrderByAppointmentTimeAsc(
            Long userId,
            Long ownerId
    );
}