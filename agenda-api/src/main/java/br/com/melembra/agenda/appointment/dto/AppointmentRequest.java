package br.com.melembra.agenda.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.melembra.agenda.appointment.entity.EventCategory;
import java.time.Instant;

public record AppointmentRequest(
    @NotBlank(message = "Informe o compromisso") @Size(max = 160) String title,
    EventCategory category,
    @Size(max = 240) String location,
    @Size(max = 1000) String notes,
    @NotNull(message = "O inicio e obrigatorio") Instant startAt,
    @NotNull(message = "O termino e obrigatorio") Instant endAt,
    Double destinationLatitude,
    Double destinationLongitude
) { }
