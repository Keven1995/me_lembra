package br.com.melembra.agenda.appointment.entity;

import br.com.melembra.agenda.user.entity.User;
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
@Table(name = "appointments")
public class Appointment {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "title", nullable = false, length = 160)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EventCategory category;
    @Column(length = 240) private String location;
    @Column(name = "destination_latitude") private Double destinationLatitude;
    @Column(name = "destination_longitude") private Double destinationLongitude;
    @Column(length = 1000) private String notes;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Column(name = "google_event_id")
    private String googleEventId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AppointmentStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Appointment() { }

    public static Appointment scheduled(User user, String title, EventCategory category,
                                        String location, String notes, Instant startAt, Instant endAt) {
        var appointment = new Appointment();
        appointment.id = UUID.randomUUID();
        appointment.user = user;
        appointment.title = title;
        appointment.category = category == null ? EventCategory.OTHER : category;
        appointment.location = location;
        appointment.notes = notes;
        appointment.startAt = startAt;
        appointment.endAt = endAt;
        appointment.status = AppointmentStatus.SCHEDULED;
        appointment.createdAt = Instant.now();
        appointment.updatedAt = appointment.createdAt;
        return appointment;
    }

    public void update(String title, EventCategory category, String location, String notes, Instant startAt, Instant endAt) {
        this.title = title;
        this.category = category == null ? EventCategory.OTHER : category;
        this.location = location;
        this.notes = notes;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = Instant.now();
    }

    public void setDestination(Double latitude, Double longitude) {
        destinationLatitude = latitude;
        destinationLongitude = longitude;
    }

    public void cancel() { this.status = AppointmentStatus.CANCELLED; this.updatedAt = Instant.now(); }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public EventCategory getCategory() { return category; }
    public String getLocation() { return location; }
    public Double getDestinationLatitude() { return destinationLatitude; }
    public Double getDestinationLongitude() { return destinationLongitude; }
    public String getNotes() { return notes; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getGoogleEventId() { return googleEventId; }
    public AppointmentStatus getStatus() { return status; }
}
