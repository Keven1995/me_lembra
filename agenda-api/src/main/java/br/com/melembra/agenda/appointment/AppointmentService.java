package br.com.melembra.agenda.appointment;

import br.com.melembra.agenda.appointment.dto.AppointmentRequest;
import br.com.melembra.agenda.appointment.dto.AppointmentResponse;
import br.com.melembra.agenda.appointment.entity.Appointment;
import br.com.melembra.agenda.appointment.entity.AppointmentStatus;
import br.com.melembra.agenda.appointment.exception.AppointmentConflictException;
import br.com.melembra.agenda.appointment.exception.InvalidAppointmentPeriodException;
import br.com.melembra.agenda.appointment.exception.ResourceNotFoundException;
import br.com.melembra.agenda.appointment.gateway.CalendarGateway;
import br.com.melembra.agenda.appointment.repository.AppointmentRepository;
import br.com.melembra.agenda.user.repository.UserRepository;
import br.com.melembra.agenda.notification.ReminderPlanner;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CalendarGateway calendarGateway;
    private final ReminderPlanner reminderPlanner;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository,
                              CalendarGateway calendarGateway, ReminderPlanner reminderPlanner) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.calendarGateway = calendarGateway;
        this.reminderPlanner = reminderPlanner;
    }

    @Transactional
    public AppointmentResponse create(UUID userId, AppointmentRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        if (appointmentRepository.existsOverlapping(userId, request.startAt(), request.endAt(), null)) {
            throw new AppointmentConflictException();
        }
        var user = userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);
        var appointment = Appointment.scheduled(user, request.title().trim(), request.category(),
            request.location(), request.notes(), request.startAt(), request.endAt());
        validateDestination(request.destinationLatitude(), request.destinationLongitude());
        appointment.setDestination(request.destinationLatitude(), request.destinationLongitude());
        appointmentRepository.save(appointment);
        appointment.setGoogleEventId(calendarGateway.create(appointment));
        reminderPlanner.schedule(appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(UUID userId, Instant start, Instant end) {
        return appointmentRepository.findByUserIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            userId, AppointmentStatus.SCHEDULED, end, start).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID userId, UUID appointmentId) {
        return AppointmentResponse.from(findOwned(userId, appointmentId));
    }

    @Transactional
    public AppointmentResponse update(UUID userId, UUID appointmentId, AppointmentRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        var appointment = findOwned(userId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) throw new ResourceNotFoundException();
        if (appointmentRepository.existsOverlapping(userId, request.startAt(), request.endAt(), appointmentId)) {
            throw new AppointmentConflictException();
        }
        appointment.update(request.title().trim(), request.category(), request.location(), request.notes(),
            request.startAt(), request.endAt());
        validateDestination(request.destinationLatitude(), request.destinationLongitude());
        appointment.setDestination(request.destinationLatitude(), request.destinationLongitude());
        calendarGateway.update(appointment);
        reminderPlanner.schedule(appointment);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public void cancel(UUID userId, UUID appointmentId) {
        var appointment = findOwned(userId, appointmentId);
        appointment.cancel();
        calendarGateway.delete(appointment);
        reminderPlanner.cancel(appointment);
        appointmentRepository.save(appointment);
    }

    private Appointment findOwned(UUID userId, UUID appointmentId) {
        return appointmentRepository.findByIdAndUserId(appointmentId, userId).orElseThrow(ResourceNotFoundException::new);
    }

    private void validatePeriod(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) throw new InvalidAppointmentPeriodException();
    }

    private void validateDestination(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Informe as duas coordenadas do destino.");
        }
        if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
            || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Coordenadas do destino inválidas.");
        }
    }
}
