package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.Appointment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminder_jobs")
public class ReminderJob {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ReminderKind kind;
    @Column(name = "due_at", nullable = false) private Instant dueAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_attempt_at") private Instant lastAttemptAt;

    protected ReminderJob() { }

    public ReminderJob(Appointment appointment, ReminderKind kind, Instant dueAt) {
        this.id = UUID.randomUUID();
        this.appointment = appointment;
        this.kind = kind;
        this.dueAt = dueAt;
    }

    public void attempted(Instant now, boolean delivered) {
        attempts++;
        lastAttemptAt = now;
        if (delivered) sentAt = now;
    }

    public Appointment getAppointment() { return appointment; }
    public ReminderKind getKind() { return kind; }
    public Instant getDueAt() { return dueAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
}
