package com.goalstack.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the real HTTP behaviour of OllamaProvider against a tiny local server that pretends to be Ollama.
 * The answers used here match what a real Ollama server sends (checked against Ollama 0.32).
 */
class OllamaProviderTest {

    private static final String CHAT_OK = """
            {"model":"llama3.1:8b","created_at":"2026-01-01T00:00:00Z",
             "message":{"role":"assistant","content":"{\\"reply\\":\\"Hi\\",\\"readyToGenerate\\":false}"},
             "done":true,"done_reason":"stop","total_duration":1000,"prompt_eval_count":12,"eval_count":7}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicReference<String> lastMethodAndPath = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            lastMethodAndPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private OllamaProvider provider(String model, String apiKey, Duration readTimeout, String baseUrl) {
        return new OllamaProvider(baseUrl, model, 8192, "5m", apiKey, Duration.ofSeconds(2), readTimeout, objectMapper);
    }

    private OllamaProvider provider() {
        return provider("llama3.1:8b", null, Duration.ofSeconds(5), baseUrl());
    }

    private static AIProviderRequest structuredRequest() {
        return AIProviderRequest.structured(List.of(AIMessage.system("Plan goals."), AIMessage.user("Learn Java")),
                AISchemas.ASSISTANT_TURN, AISchemas.assistantTurn(), 0.2, 300);
    }

    // ----- the request we send and the answer we read -----

    @Test
    void sendsTheExpectedRequestAndReadsTheAnswer() throws Exception {
        respond("/api/chat", 200, CHAT_OK);

        AIProviderResponse response = provider().generate(structuredRequest());

        assertThat(response.content()).contains("\"reply\":\"Hi\"");
        assertThat(response.model()).isEqualTo("llama3.1:8b");
        assertThat(response.promptTokens()).isEqualTo(12);
        assertThat(response.completionTokens()).isEqualTo(7);
        assertThat(response.finishReason()).isEqualTo("stop");

        assertThat(lastMethodAndPath.get()).isEqualTo("POST /api/chat");
        JsonNode sent = objectMapper.readTree(lastBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("llama3.1:8b");
        assertThat(sent.path("stream").asBoolean(true)).isFalse();
        assertThat(sent.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(sent.path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(sent.path("messages").get(1).path("content").asText()).isEqualTo("Learn Java");
        assertThat(sent.path("format").path("type").asText()).isEqualTo("object");
        assertThat(sent.path("format").path("required").toString()).contains("reply", "readyToGenerate");
        assertThat(sent.path("options").path("num_ctx").asInt()).isEqualTo(8192);
        assertThat(sent.path("options").path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(sent.path("options").path("num_predict").asInt()).isEqualTo(300);
        assertThat(sent.path("keep_alive").asText()).isEqualTo("5m");
        assertThat(lastAuthorization.get()).isNull();
    }

    @Test
    void aRequestWithoutASchemaSendsNoFormat() throws Exception {
        respond("/api/chat", 200, CHAT_OK);

        provider().generate(new AIProviderRequest(List.of(AIMessage.user("hi")), null, null, null, null));

        JsonNode sent = objectMapper.readTree(lastBody.get());
        assertThat(sent.has("format")).isFalse();
        assertThat(sent.path("options").has("temperature")).isFalse();
        assertThat(sent.path("options").has("num_predict")).isFalse();
    }

    @Test
    void anApiKeyIsSentAsABearerToken() {
        respond("/api/chat", 200, CHAT_OK);

        provider("llama3.1:8b", "  secret-key  ", Duration.ofSeconds(5), baseUrl()).generate(structuredRequest());

        assertThat(lastAuthorization.get()).isEqualTo("Bearer secret-key");
    }

    // ----- errors from Ollama -----

    @Test
    void aModelThatIsNotInstalledIsAConfigurationProblem() {
        respond("/api/chat", 404, "{\"error\":\"model 'llama3.1:8b' not found\"}");

        assertFails(AIErrorType.NOT_CONFIGURED);
    }

    @Test
    void wrongOrMissingCredentialsAreAConfigurationProblem() {
        respond("/api/chat", 401, "{\"error\":\"unauthorized\"}");

        assertFails(AIErrorType.NOT_CONFIGURED);
    }

    @Test
    void aServerErrorMeansUnavailable() {
        respond("/api/chat", 500, "{\"error\":\"something broke\"}");

        assertFails(AIErrorType.UNAVAILABLE);
    }

    @Test
    void notEnoughMemoryForTheModelIsAConfigurationProblemNotARetryCase() {
        respond("/api/chat", 500, "{\"error\":\"model requires more system memory (9.2 GiB) than is available (4.1 GiB)\"}");

        assertFails(AIErrorType.NOT_CONFIGURED);
    }

    @Test
    void tooManyRequestsMeansRateLimited() {
        respond("/api/chat", 429, "{\"error\":\"too many requests\"}");

        assertFails(AIErrorType.RATE_LIMITED);
    }

    @Test
    void aRejectedRequestIsReportedAsSuch() {
        respond("/api/chat", 400, "{\"error\":\"unexpected EOF\"}");

        assertFails(AIErrorType.REQUEST_REJECTED);
    }

    @Test
    void anEmptyAnswerIsABadResponse() {
        respond("/api/chat", 200, "{\"model\":\"llama3.1:8b\",\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true}");

        assertFails(AIErrorType.BAD_RESPONSE);
    }

    @Test
    void aReplyWithoutAMessageIsABadResponse() {
        respond("/api/chat", 200, "{\"done\":true}");

        assertFails(AIErrorType.BAD_RESPONSE);
    }

    @Test
    void aReplyThatIsNotJsonIsABadResponse() {
        respond("/api/chat", 200, "<html>proxy error page</html>");

        assertFails(AIErrorType.BAD_RESPONSE);
    }

    // ----- network problems -----

    @Test
    void aSlowServerCausesATimeout() {
        server.createContext("/api/chat", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> provider("llama3.1:8b", null, Duration.ofMillis(300), baseUrl())
                .generate(structuredRequest()))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.TIMEOUT));
    }

    @Test
    void aServerThatIsNotRunningMeansUnavailable() throws IOException {
        assertThatThrownBy(() -> provider("llama3.1:8b", null, Duration.ofSeconds(2), unusedBaseUrl())
                .generate(structuredRequest()))
                .isInstanceOfSatisfying(AIProviderException.class,
                        ex -> assertThat(ex.getType()).isEqualTo(AIErrorType.UNAVAILABLE));
    }

    // ----- status check -----

    @Test
    void statusIsAvailableWhenTheModelIsInstalled() {
        respond("/api/tags", 200, "{\"models\":[{\"name\":\"other:latest\"},{\"name\":\"llama3.1:8b\"}]}");

        AIStatus status = provider().checkStatus();

        assertThat(status.available()).isTrue();
        assertThat(status.availability()).isEqualTo(AIStatus.Availability.AVAILABLE);
        assertThat(status.provider()).isEqualTo("ollama");
        assertThat(lastMethodAndPath.get()).isEqualTo("GET /api/tags");
    }

    @Test
    void aModelNameWithoutATagMatchesLatest() {
        respond("/api/tags", 200, "{\"models\":[{\"name\":\"llama3.1:latest\"}]}");

        assertThat(provider("llama3.1", null, Duration.ofSeconds(5), baseUrl()).checkStatus().available()).isTrue();
    }

    @Test
    void statusSaysModelMissingWhenItIsNotInstalled() {
        respond("/api/tags", 200, "{\"models\":[]}");

        AIStatus status = provider().checkStatus();

        assertThat(status.available()).isFalse();
        assertThat(status.availability()).isEqualTo(AIStatus.Availability.MODEL_MISSING);
    }

    @Test
    void statusSaysUnreachableWhenTheServerIsDownOrBroken() throws IOException {
        assertThat(provider("llama3.1:8b", null, Duration.ofSeconds(2), unusedBaseUrl()).checkStatus().availability())
                .isEqualTo(AIStatus.Availability.UNREACHABLE);

        respond("/api/tags", 500, "{\"error\":\"broken\"}");
        assertThat(provider().checkStatus().availability()).isEqualTo(AIStatus.Availability.UNREACHABLE);
    }

    // ----- helpers -----

    private void assertFails(AIErrorType expected) {
        assertThatThrownBy(() -> provider().generate(structuredRequest()))
                .isInstanceOfSatisfying(AIProviderException.class, ex -> assertThat(ex.getType()).isEqualTo(expected));
    }

    /** An address where nothing is listening: a free port that was opened and closed again. */
    private static String unusedBaseUrl() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }
    }
}
