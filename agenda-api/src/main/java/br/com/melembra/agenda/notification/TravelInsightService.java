package br.com.melembra.agenda.notification;

import br.com.melembra.agenda.appointment.entity.Appointment;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TravelInsightService {
    private final LocationService locations;
    private final OpenMeteoClient weather;
    private final HereRoutingClient routing;
    private final ZoneId timeZone;

    public TravelInsightService(LocationService locations, OpenMeteoClient weather,
                                HereRoutingClient routing,
                                @Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        this.locations = locations;
        this.weather = weather;
        this.routing = routing;
        this.timeZone = ZoneId.of(timeZone);
    }

    public String summarize(Appointment appointment, Instant now) {
        if (appointment.getDestinationLatitude() == null || appointment.getDestinationLongitude() == null) {
            return "";
        }
        double lat = appointment.getDestinationLatitude();
        double lon = appointment.getDestinationLongitude();
        List<String> parts = new ArrayList<>();
        weather.forecast(lat, lon, appointment.getStartAt())
            .ifPresent(forecast -> parts.add(forecast.summary() + "."));
        locations.origin(appointment.getUser().getId(), now)
            .flatMap(origin -> routing.route(origin.latitude(), origin.longitude(), lat, lon)
                .map(route -> describeRoute(route, origin.source(), appointment.getStartAt(), now)))
            .ifPresent(parts::add);
        return String.join(" ", parts);
    }

    private String describeRoute(HereRoutingClient.Route route, String source, Instant appointmentAt, Instant now) {
        int delay = route.trafficDelayMinutes();
        String traffic = delay > 0 ? " (+" + delay + "min de trânsito)" : "";
        Instant suggestedDeparture = appointmentAt.minusSeconds((route.durationMinutes() + 10L) * 60);
        String departure = suggestedDeparture.isAfter(now)
            ? "Saia até " + suggestedDeparture.atZone(timeZone)
                .format(DateTimeFormatter.ofPattern("HH:mm")) + "."
            : "Saída recomendada: agora.";
        return "🚗 " + route.durationMinutes() + "min" + traffic
            + " desde " + source + ". " + departure;
    }
}
