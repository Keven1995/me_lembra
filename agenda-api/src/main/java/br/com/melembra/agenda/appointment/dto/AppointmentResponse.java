package br.com.melembra.agenda.appointment.dto;

import br.com.melembra.agenda.appointment.entity.Appointment;
import br.com.melembra.agenda.appointment.entity.AppointmentStatus;
import br.com.melembra.agenda.appointment.entity.EventCategory;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, String title, EventCategory category, String location,
                                  Double destinationLatitude, Double destinationLongitude,
                                  String notes, Instant startAt, Instant endAt,
                                  String googleEventId, AppointmentStatus status) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getTitle(), appointment.getCategory(),
            appointment.getLocation(), appointment.getDestinationLatitude(), appointment.getDestinationLongitude(),
            appointment.getNotes(), appointment.getStartAt(),
            appointment.getEndAt(), appointment.getGoogleEventId(), appointment.getStatus());
    }
}
