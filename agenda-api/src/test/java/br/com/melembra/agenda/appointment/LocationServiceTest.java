package br.com.melembra.agenda.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.melembra.agenda.notification.LocationPreference;
import br.com.melembra.agenda.notification.LocationPreferenceRepository;
import br.com.melembra.agenda.notification.LocationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    @Mock LocationPreferenceRepository repository;

    @Test
    void usesRecentGpsInsteadOfFallback() {
        var user = UUID.randomUUID();
        var now = Instant.now();
        var stored = new LocationPreference(user);
        stored.saveFallback("Casa", -16.08, -47.98);
        stored.updateGps(-16.05, -47.91);
        when(repository.findById(user)).thenReturn(Optional.of(stored));

        var origin = new LocationService(repository, 30).origin(user, now.plusSeconds(2));

        assertThat(origin).isPresent();
        assertThat(origin.orElseThrow().source()).isEqualTo("GPS recente");
        assertThat(origin.orElseThrow().latitude()).isEqualTo(-16.05);
    }

    @Test
    void usesSavedAddressWhenGpsHasExpired() {
        var user = UUID.randomUUID();
        var stored = new LocationPreference(user);
        stored.saveFallback("Casa", -16.08, -47.98);
        stored.updateGps(-16.05, -47.91);
        when(repository.findById(user)).thenReturn(Optional.of(stored));

        var origin = new LocationService(repository, 30).origin(user, Instant.now().plusSeconds(3600));

        assertThat(origin).isPresent();
        assertThat(origin.orElseThrow().source()).isEqualTo("endereço salvo");
        assertThat(origin.orElseThrow().longitude()).isEqualTo(-47.98);
    }

    @Test
    void noLocationWhenGpsDisabledAndNoFallback() {
        var user = UUID.randomUUID();
        var stored = new LocationPreference(user);
        stored.updateGps(-16.05, -47.91);
        stored.disableGps();
        when(repository.findById(user)).thenReturn(Optional.of(stored));

        assertThat(new LocationService(repository, 30).origin(user, Instant.now())).isEmpty();
    }
}
