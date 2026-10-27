package br.com.melembra.agenda.notification;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HereRoutingClient {
    private static final Logger log = LoggerFactory.getLogger(HereRoutingClient.class);
    private final RestClient client;
    private final String apiKey;

    public HereRoutingClient(@Value("${app.here.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        client = RestClient.builder().baseUrl("https://router.hereapi.com")
            .requestFactory(factory).build();
    }

    public Optional<Route> route(double originLat, double originLon, double targetLat, double targetLon) {
        if (apiKey.isBlank()) return Optional.empty();
        try {
            JsonNode data = client.get().uri(builder -> builder.path("/v8/routes")
                .queryParam("transportMode", "car")
                .queryParam("origin", originLat + "," + originLon)
                .queryParam("destination", targetLat + "," + targetLon)
                .queryParam("return", "summary")
                // A omissão de departureTime usa o momento atual, incluindo trânsito.
                .queryParam("apiKey", apiKey)
                .build()).retrieve().body(JsonNode.class);
            if (data == null || !data.path("routes").isArray() || data.path("routes").size() == 0) {
                return Optional.empty();
            }
            int duration = 0, baseDuration = 0;
            for (JsonNode section : data.path("routes").get(0).path("sections")) {
                JsonNode summary = section.path("summary");
                if (!summary.path("duration").isNumber()) return Optional.empty();
                int actual = summary.path("duration").asInt();
                duration += actual;
                baseDuration += summary.path("baseDuration").isNumber()
                    ? summary.path("baseDuration").asInt() : actual;
            }
            return duration > 0
                ? Optional.of(new Route((int) Math.ceil(duration / 60.0),
                    (int) Math.ceil(Math.max(0, duration - baseDuration) / 60.0)))
                : Optional.empty();
        } catch (RestClientException exception) {
            // Evita registrar URI com a chave da API em logs.
            log.warn("HERE Routing indisponível: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public record Route(int durationMinutes, int trafficDelayMinutes) { }
}
