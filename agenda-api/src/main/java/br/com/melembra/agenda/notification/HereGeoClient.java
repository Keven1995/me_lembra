package br.com.melembra.agenda.notification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class HereGeoClient {
    private final String apiKey;
    private final RestClient client;

    public HereGeoClient(@Value("${app.here.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        client = RestClient.builder().baseUrl("https://geocode.search.hereapi.com")
            .requestFactory(factory).build();
    }

    public List<Place> search(String query) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure HERE_API_SECRET_KEY no servidor para pesquisar endereços.");
        }
        if (query == null || query.trim().length() < 4 || query.length() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe pelo menos quatro caracteres.");
        }
        try {
            JsonNode response = client.get().uri(builder -> builder.path("/v1/geocode")
                .queryParam("q", query.trim())
                .queryParam("limit", 5)
                .queryParam("lang", "pt-BR")
                .queryParam("apiKey", apiKey)
                .build()).retrieve().body(JsonNode.class);
            List<Place> result = new ArrayList<>();
            if (response == null) return result;
            for (JsonNode item : response.path("items")) {
                var position = item.path("position");
                String label = item.path("address").path("label").asText(item.path("title").asText(""));
                if (!position.hasNonNull("lat") || !position.hasNonNull("lng") || label.isBlank()) continue;
                result.add(new Place(label, position.path("lat").asDouble(), position.path("lng").asDouble()));
            }
            return result;
        } catch (RestClientException exception) {
            // A resposta pública não inclui URLs ou cabeçalhos com a chave da HERE.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Não foi possível pesquisar endereços na HERE.");
        }
    }

    public record Place(String label, double latitude, double longitude) { }
}
