package com.homeverse.property.service;

import com.homeverse.property.dto.request.AppointmentCreateRequest;
import com.homeverse.property.dto.response.AppointmentResponse;
import com.homeverse.property.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {

    List<AppointmentResponse> getMyCalendar(Long currentUserId);

    AppointmentResponse create(Long currentUserId, AppointmentCreateRequest request);

    AppointmentResponse updateStatus(Long currentUserId, Long id, Appointment.Status status);

    AppointmentResponse suggestNewTime(Long currentUserId, Long id, LocalDateTime newTime, String note);

    AppointmentResponse acceptSuggestion(Long currentUserId, Long id);
}