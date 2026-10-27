package br.com.melembra.agenda.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_location_preferences")
public class LocationPreference {
    @Id @Column(name = "user_id") private UUID userId;
    @Column(name = "fallback_label", length = 240) private String fallbackLabel;
    @Column(name = "fallback_latitude") private Double fallbackLatitude;
    @Column(name = "fallback_longitude") private Double fallbackLongitude;
    @Column(name = "gps_enabled", nullable = false) private boolean gpsEnabled;
    @Column(name = "last_gps_latitude") private Double lastGpsLatitude;
    @Column(name = "last_gps_longitude") private Double lastGpsLongitude;
    @Column(name = "last_gps_at") private Instant lastGpsAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected LocationPreference() { }
    public LocationPreference(UUID userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }
    public void saveFallback(String label, double latitude, double longitude) {
        fallbackLabel = label;
        fallbackLatitude = latitude;
        fallbackLongitude = longitude;
        updatedAt = Instant.now();
    }
    public void clearFallback() {
        fallbackLabel = null;
        fallbackLatitude = null;
        fallbackLongitude = null;
        updatedAt = Instant.now();
    }
    public void updateGps(double latitude, double longitude) {
        gpsEnabled = true;
        lastGpsLatitude = latitude;
        lastGpsLongitude = longitude;
        lastGpsAt = Instant.now();
        updatedAt = lastGpsAt;
    }
    public void disableGps() {
        gpsEnabled = false;
        lastGpsLatitude = null;
        lastGpsLongitude = null;
        lastGpsAt = null;
        updatedAt = Instant.now();
    }
    public String getFallbackLabel() { return fallbackLabel; }
    public Double getFallbackLatitude() { return fallbackLatitude; }
    public Double getFallbackLongitude() { return fallbackLongitude; }
    public boolean isGpsEnabled() { return gpsEnabled; }
    public Double getLastGpsLatitude() { return lastGpsLatitude; }
    public Double getLastGpsLongitude() { return lastGpsLongitude; }
    public Instant getLastGpsAt() { return lastGpsAt; }
}
