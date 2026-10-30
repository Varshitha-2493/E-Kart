package com.ekart.ekart.ai;

import com.ekart.ekart.config.AiProperties;
import com.ekart.ekart.exception.ApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GrokClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GrokClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {

        if (!properties.isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Groq API key not configured. Set ekart.ai.api-key in application.properties"
            );
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();

            body.put("model", properties.getModel());
            body.put("temperature", 0.2);

            ArrayNode messages = body.putArray("messages");

            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt);

            messages.addObject()
                    .put("role", "user")
                    .put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            trimSlash(properties.getBaseUrl())
                                    + "/chat/completions"
                    ))
                    .timeout(Duration.ofSeconds(90))
                    .header(
                            "Authorization",
                            "Bearer " + properties.getApiKey().trim()
                    )
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    objectMapper.writeValueAsString(body)
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 400) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Groq API error (" + response.statusCode() + "): "
                                + abbreviate(response.body())
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Empty response from Groq"
                );
            }

            return content.asText();

        } catch (ApiException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to call Groq: " + ex.getMessage()
            );
        }
    }

    private static String trimSlash(String url) {

        if (url == null || url.isBlank()) {
            return "https://api.groq.com/openai/v1";
        }

        return url.endsWith("/")
                ? url.substring(0, url.length() - 1)
                : url;
    }

    private static String abbreviate(String text) {

        if (text == null) {
            return "";
        }

        return text.length() > 300
                ? text.substring(0, 300) + "..."
                : text;
    }
}