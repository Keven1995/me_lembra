package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.AppointmentStatus;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentNotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationScheduler.class);
    private final ReminderJobRepository jobs;
    private final PushSubscriptionRepository subscriptions;
    private final WebPushSender sender;
    private final TravelInsightService travelInsights;

    public AppointmentNotificationScheduler(ReminderJobRepository jobs,
                                            PushSubscriptionRepository subscriptions,
                                            WebPushSender sender, TravelInsightService travelInsights) {
        this.jobs = jobs;
        this.subscriptions = subscriptions;
        this.sender = sender;
        this.travelInsights = travelInsights;
    }

    @Scheduled(fixedDelayString = "${app.push.check-interval-ms:60000}")
    @Transactional
    public void sendDueNotifications() {
        if (!sender.isConfigured()) return;
        var now = Instant.now();
        for (var job : jobs.due(now, PageRequest.of(0, 100))) {
            var appointment = job.getAppointment();
            if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                || !appointment.getStartAt().isAfter(now)
                || job.getDueAt().isBefore(now.minus(Duration.ofMinutes(20)))) {
                jobs.delete(job);
                continue;
            }
            if (job.getLastAttemptAt() != null
                && job.getLastAttemptAt().isAfter(now.minus(Duration.ofMinutes(5)))) continue;
            var devices = subscriptions.findByUserId(appointment.getUser().getId());
            var insight = job.getKind() == ReminderKind.ONE_HOUR && !devices.isEmpty()
                ? travelInsights.summarize(appointment, now) : "";
            boolean sent = false;
            for (var subscription : devices) {
                try {
                    sender.send(subscription, appointment, job.getKind(), insight);
                    sent = true;
                } catch (Exception exception) {
                    log.warn("Falha ao enviar push para usuario {}, lembrete {}", appointment.getUser().getId(), job.getKind(), exception);
                }
            }
            job.attempted(now, sent);
        }
    }
}
