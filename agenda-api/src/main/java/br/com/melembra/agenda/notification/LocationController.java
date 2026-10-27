package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.exception.ResourceNotFoundException;
import br.com.melembra.agenda.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/location")
public class LocationController {
    private final LocationService locations;
    private final HereGeoClient geocoder;
    private final UserRepository users;

    public LocationController(LocationService locations, HereGeoClient geocoder, UserRepository users) {
        this.locations = locations;
        this.geocoder = geocoder;
        this.users = users;
    }

    @GetMapping("/search")
    public List<HereGeoClient.Place> search(Authentication authentication, @RequestParam String q) {
        userId(authentication);
        return geocoder.search(q);
    }

    @GetMapping("/preferences")
    public LocationService.Preferences preferences(Authentication authentication) {
        return locations.get(userId(authentication));
    }

    @PutMapping("/fallback")
    public LocationService.Preferences fallback(Authentication authentication,
                                                @Valid @RequestBody FallbackRequest request) {
        return locations.saveFallback(userId(authentication), request.label(),
            request.latitude(), request.longitude());
    }

    @DeleteMapping("/fallback")
    public LocationService.Preferences clearFallback(Authentication authentication) {
        return locations.clearFallback(userId(authentication));
    }

    @PostMapping("/gps")
    public LocationService.Preferences gps(Authentication authentication,
                                           @Valid @RequestBody GpsRequest request) {
        return locations.updateGps(userId(authentication),
            request.latitude(), request.longitude(), request.accuracyMeters());
    }

    @DeleteMapping("/gps")
    public LocationService.Preferences disableGps(Authentication authentication) {
        return locations.disableGps(userId(authentication));
    }

    private UUID userId(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            return users.findByGoogleId(authentication.getName())
                .orElseThrow(ResourceNotFoundException::new).getId();
        }
    }

    public record FallbackRequest(String label, @NotNull Double latitude, @NotNull Double longitude) { }
    public record GpsRequest(@NotNull Double latitude, @NotNull Double longitude,
                             @NotNull Double accuracyMeters) { }
}
