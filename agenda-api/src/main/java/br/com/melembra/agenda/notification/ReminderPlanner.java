package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.Appointment;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReminderPlanner {
    private final ReminderJobRepository repository;
    private final ZoneId zone;

    public ReminderPlanner(ReminderJobRepository repository,
                           @Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        this.repository = repository;
        this.zone = ZoneId.of(timeZone);
    }

    public void schedule(Appointment appointment) {
        repository.deleteForAppointment(appointment.getId());
        var localStart = appointment.getStartAt().atZone(zone);
        var candidates = List.of(
            new Candidate(ReminderKind.ONE_HOUR, appointment.getStartAt().minusSeconds(3600)),
            new Candidate(ReminderKind.THREE_HOURS, appointment.getStartAt().minusSeconds(3 * 3600)),
            new Candidate(ReminderKind.DAY_OF, localStart.toLocalDate().atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()),
            new Candidate(ReminderKind.DAY_BEFORE, localStart.toLocalDate().minusDays(1).atTime(18, 0).atZone(zone).toInstant())
        );
        Instant now = Instant.now();
        List<ReminderJob> jobs = new ArrayList<>();
        Set<Instant> scheduledAt = new HashSet<>();
        for (Candidate candidate : candidates) {
            // Não cria lembretes atrasados nem avisa após o limite de uma hora.
            if (candidate.at().isAfter(now) && !candidate.at().isAfter(appointment.getStartAt().minusSeconds(3600))
                && scheduledAt.add(candidate.at())) {
                jobs.add(new ReminderJob(appointment, candidate.kind(), candidate.at()));
            }
        }
        repository.saveAll(jobs);
    }

    public void cancel(Appointment appointment) {
        repository.deleteForAppointment(appointment.getId());
    }

    private record Candidate(ReminderKind kind, Instant at) { }
}
