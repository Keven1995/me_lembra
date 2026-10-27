package br.com.melembra.agenda.notification;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenMeteoClient {
    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);
    private final RestClient client;

    public OpenMeteoClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        client = RestClient.builder().baseUrl("https://api.open-meteo.com")
            .requestFactory(factory).build();
    }

    public Optional<Forecast> forecast(double latitude, double longitude, Instant eventAt) {
        try {
            JsonNode data = client.get().uri(builder -> builder.path("/v1/forecast")
                .queryParam("latitude", latitude).queryParam("longitude", longitude)
                .queryParam("hourly", "temperature_2m,precipitation_probability,weather_code")
                .queryParam("forecast_days", 2)
                .queryParam("timezone", "UTC")
                .build()).retrieve().body(JsonNode.class);
            if (data == null) return Optional.empty();
            JsonNode hourly = data.path("hourly");
            JsonNode times = hourly.path("time");
            long nearestDifference = Long.MAX_VALUE;
            int nearestIndex = -1;
            for (int i = 0; i < times.size(); i++) {
                Instant forecastHour = Instant.parse(times.get(i).asText() + "Z");
                long difference = Math.abs(Duration.between(forecastHour, eventAt).toMinutes());
                if (difference < nearestDifference) {
                    nearestDifference = difference;
                    nearestIndex = i;
                }
            }
            if (nearestIndex < 0 || nearestDifference > 60) return Optional.empty();
            JsonNode temp = hourly.path("temperature_2m").path(nearestIndex);
            JsonNode rain = hourly.path("precipitation_probability").path(nearestIndex);
            if (!temp.isNumber() || !rain.isNumber()) return Optional.empty();
            return Optional.of(new Forecast((int) Math.round(temp.asDouble()), rain.asInt()));
        } catch (RestClientException | DateTimeException | IllegalArgumentException exception) {
            log.warn("Open-Meteo indisponível para consulta: {}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public record Forecast(int temperatureCelsius, int rainProbability) {
        public String summary() {
            String icon = rainProbability >= 60 ? "🌧️" : rainProbability >= 30 ? "🌦️" : "🌤️";
            return icon + " " + temperatureCelsius + "°C, chuva " + rainProbability + "%";
        }
    }
}
