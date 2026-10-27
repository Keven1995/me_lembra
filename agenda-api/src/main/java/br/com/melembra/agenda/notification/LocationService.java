package br.com.melembra.agenda.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
    private final LocationPreferenceRepository repository;
    private final Duration gpsFreshness;

    public LocationService(LocationPreferenceRepository repository,
                           @Value("${app.location.gps-max-age-minutes:30}") int gpsMaxAgeMinutes) {
        this.repository = repository;
        this.gpsFreshness = Duration.ofMinutes(gpsMaxAgeMinutes);
    }

    @Transactional(readOnly = true)
    public Preferences get(UUID userId) {
        return repository.findById(userId).map(this::toResponse)
            .orElse(new Preferences(null, false, null, false));
    }

    @Transactional
    public Preferences saveFallback(UUID userId, String label, double latitude, double longitude) {
        validate(latitude, longitude);
        if (label == null || label.isBlank() || label.length() > 240) {
            throw new IllegalArgumentException("Informe um endereço válido com até 240 caracteres.");
        }
        var preference = repository.findById(userId).orElseGet(() -> new LocationPreference(userId));
        preference.saveFallback(label.trim(), latitude, longitude);
        return toResponse(repository.save(preference));
    }

    @Transactional
    public Preferences clearFallback(UUID userId) {
        var preference = repository.findById(userId).orElseGet(() -> new LocationPreference(userId));
        preference.clearFallback();
        return toResponse(repository.save(preference));
    }

    @Transactional
    public Preferences updateGps(UUID userId, double latitude, double longitude, double accuracyMeters) {
        validate(latitude, longitude);
        if (!Double.isFinite(accuracyMeters) || accuracyMeters < 0 || accuracyMeters > 2000) {
            throw new IllegalArgumentException("Localização imprecisa. Tente novamente com o GPS ativo.");
        }
        var preference = repository.findById(userId).orElseGet(() -> new LocationPreference(userId));
        preference.updateGps(latitude, longitude);
        return toResponse(repository.save(preference));
    }

    @Transactional
    public Preferences disableGps(UUID userId) {
        var preference = repository.findById(userId).orElseGet(() -> new LocationPreference(userId));
        preference.disableGps();
        return toResponse(repository.save(preference));
    }

    @Transactional(readOnly = true)
    public Optional<Origin> origin(UUID userId, Instant now) {
        return repository.findById(userId).flatMap(preference -> {
            if (preference.isGpsEnabled() && preference.getLastGpsAt() != null
                && !preference.getLastGpsAt().isAfter(now)
                && Duration.between(preference.getLastGpsAt(), now).compareTo(gpsFreshness) <= 0
                && preference.getLastGpsLatitude() != null && preference.getLastGpsLongitude() != null) {
                return Optional.of(new Origin(preference.getLastGpsLatitude(), preference.getLastGpsLongitude(), "GPS recente"));
            }
            if (preference.getFallbackLatitude() != null && preference.getFallbackLongitude() != null) {
                return Optional.of(new Origin(preference.getFallbackLatitude(), preference.getFallbackLongitude(), "endereço salvo"));
            }
            return Optional.empty();
        });
    }

    private Preferences toResponse(LocationPreference preference) {
        boolean fresh = preference.getLastGpsAt() != null && !preference.getLastGpsAt().isAfter(Instant.now())
            && Duration.between(preference.getLastGpsAt(), Instant.now()).compareTo(gpsFreshness) <= 0;
        return new Preferences(preference.getFallbackLabel(), preference.isGpsEnabled(),
            preference.getLastGpsAt(), fresh);
    }

    private void validate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
            || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas inválidas.");
        }
    }
    public record Preferences(String fallbackLabel, boolean gpsEnabled, Instant lastGpsAt, boolean gpsFresh) { }
    public record Origin(double latitude, double longitude, String source) { }
}
