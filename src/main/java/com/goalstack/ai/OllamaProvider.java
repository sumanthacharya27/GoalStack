package com.goalstack.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Talks to a local Ollama server (default http://localhost:11434) using its /api/chat endpoint.
 * All Ollama-specific details live in this class. Prompts and answers are never logged.
 *
 * Ollama's JSON "format" option constrains the answer to a JSON schema, but models can still produce
 * wrong content, so callers must always parse and validate the answer (see StructuredOutputParser).
 */
public class OllamaProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);
    private static final Duration STATUS_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient chatClient;
    private final RestClient statusClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int numCtx;
    private final String keepAlive;

    public OllamaProvider(AIProperties properties, ObjectMapper objectMapper) {
        this(properties.ollama().baseUrl(), properties.ollama().model(), properties.ollama().numCtx(),
                properties.ollama().keepAlive(), properties.ollama().apiKey(),
                properties.http().connectTimeout(), properties.http().readTimeout(), objectMapper);
    }

    public OllamaProvider(String baseUrl, String model, int numCtx, String keepAlive, String apiKey,
                          Duration connectTimeout, Duration readTimeout, ObjectMapper objectMapper) {
        this.model = model;
        this.numCtx = numCtx;
        this.keepAlive = keepAlive;
        this.objectMapper = objectMapper;
        this.chatClient = buildClient(baseUrl, apiKey, connectTimeout, readTimeout);
        this.statusClient = buildClient(baseUrl, apiKey, connectTimeout, STATUS_TIMEOUT);
    }

    private static RestClient buildClient(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey.trim());
        }
        return builder.build();
    }

    @Override
    public AIProviderResponse generate(AIProviderRequest request) {
        Map<String, Object> body = buildBody(request);

        OllamaChatResponse response;
        try {
            response = chatClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex);
        } catch (ResourceAccessException ex) {
            throw mapIoError(ex);
        } catch (RestClientException ex) {
            // for example: the server answered with something that is not JSON
            throw new AIProviderException(AIErrorType.BAD_RESPONSE, "Ollama sent a response that could not be read", ex);
        }

        if (response == null || response.message() == null || response.message().content() == null
                || response.message().content().isBlank()) {
            throw new AIProviderException(AIErrorType.BAD_RESPONSE, "Ollama returned an empty answer");
        }
        return new AIProviderResponse(response.message().content(),
                response.model() != null ? response.model() : model,
                response.promptEvalCount(), response.evalCount(), response.doneReason());
    }

    private Map<String, Object> buildBody(AIProviderRequest request) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role().name().toLowerCase(Locale.ROOT), "content", m.content()))
                .toList();

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("num_ctx", numCtx);
        if (request.temperature() != null) {
            options.put("temperature", request.temperature());
        }
        if (request.maxOutputTokens() != null) {
            options.put("num_predict", request.maxOutputTokens());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false); // Ollama streams by default; this app wants one complete answer
        if (request.responseSchema() != null) {
            body.put("format", request.responseSchema());
        }
        body.put("options", options);
        body.put("keep_alive", keepAlive);
        return body;
    }

    @Override
    public AIStatus checkStatus() {
        try {
            OllamaTagsResponse tags = statusClient.get().uri("/api/tags").retrieve().body(OllamaTagsResponse.class);
            boolean installed = tags != null && tags.models() != null
                    && tags.models().stream().anyMatch(m -> isConfiguredModel(m.name()));
            return new AIStatus(name(), model, installed,
                    installed ? AIStatus.Availability.AVAILABLE : AIStatus.Availability.MODEL_MISSING);
        } catch (RestClientException ex) {
            log.debug("Ollama status check failed: {}", ex.getClass().getSimpleName());
            return new AIStatus(name(), model, false, AIStatus.Availability.UNREACHABLE);
        }
    }

    /** "llama3.1:8b" matches itself; a name without a tag ("llama3.1") matches "llama3.1:latest". */
    private boolean isConfiguredModel(String installedName) {
        return installedName != null && (installedName.equals(model) || installedName.equals(model + ":latest"));
    }

    // ---------- error mapping ----------

    private AIProviderException mapHttpError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String vendorMessage = extractErrorMessage(ex.getResponseBodyAsString());
        String detail = "Ollama answered HTTP " + status + (vendorMessage.isEmpty() ? "" : ": " + vendorMessage);

        AIErrorType type;
        if (status == 404 || status == 401 || status == 403) {
            type = AIErrorType.NOT_CONFIGURED; // model not installed, wrong address, or wrong key
        } else if (status == 429) {
            type = AIErrorType.RATE_LIMITED;
        } else if (status >= 500) {
            // Ollama answers 500 when the model does not fit in memory: retrying will not help.
            boolean memory = vendorMessage.toLowerCase(Locale.ROOT).contains("memory");
            type = memory ? AIErrorType.NOT_CONFIGURED : AIErrorType.UNAVAILABLE;
        } else {
            type = AIErrorType.REQUEST_REJECTED;
        }
        return new AIProviderException(type, detail, ex);
    }

    private AIProviderException mapIoError(ResourceAccessException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            boolean timeoutType = t instanceof HttpTimeoutException || t instanceof SocketTimeoutException;
            boolean timeoutText = t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains("timed out");
            if (timeoutType || timeoutText) {
                return new AIProviderException(AIErrorType.TIMEOUT, "Ollama did not answer in time", ex);
            }
        }
        return new AIProviderException(AIErrorType.UNAVAILABLE, "Ollama could not be reached", ex);
    }

    /** Ollama sends errors as {"error":"..."}. */
    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String text = node.path("error").asText("");
            return text.length() > 300 ? text.substring(0, 300) : text;
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return model;
    }

    // ---------- Ollama's JSON shapes (only the fields we use) ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatResponse(String model, OllamaMessage message,
                              @JsonProperty("done_reason") String doneReason,
                              @JsonProperty("prompt_eval_count") Integer promptEvalCount,
                              @JsonProperty("eval_count") Integer evalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaTagsResponse(List<OllamaModel> models) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaModel(String name) {
    }
}
