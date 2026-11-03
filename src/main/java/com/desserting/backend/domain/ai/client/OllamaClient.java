package com.desserting.backend.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 4-5. 로컬 Ollama LLM(Qwen 2.5 3B) 연동 클라이언트 (AI-01)
 * 로컬 Mac/서버의 http://localhost:11434/api/generate 엔드포인트를 호출한다.
 * Ollama가 기동되지 않았거나 지연될 경우 안전하게 Optional.empty()를 반환하여 Fallback으로 유연하게 전환한다.
 */
@Slf4j
@Component
public class OllamaClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;

    public OllamaClient(
            @Value("${desserting.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${desserting.ai.ollama.model:qwen2.5:3b}") String model,
            @Value("${desserting.ai.ollama.timeout-seconds:15}") int timeoutSeconds,
            @Value("${desserting.ai.ollama.enabled:true}") boolean enabled,
            ObjectMapper objectMapper) {
        this.model = model;
        this.enabled = enabled;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<String> generate(String systemPrompt, String userPrompt) {
        if (!enabled) {
            log.debug("Ollama AI 연동이 비활성화 상태입니다. 스마트 Fallback으로 전환합니다.");
            return Optional.empty();
        }

        try {
            String fullPrompt = String.format("%s\n\n%s", systemPrompt, userPrompt);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", fullPrompt,
                    "stream", false,
                    "format", "json"
            );

            byte[] responseBytes = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

            if (responseBytes == null || responseBytes.length == 0) {
                return Optional.empty();
            }

            String rawResponse = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);

            JsonNode rootNode = objectMapper.readTree(rawResponse);
            JsonNode responseField = rootNode.get("response");
            if (responseField != null && !responseField.asText().isBlank()) {
                log.info("Ollama ({}) 응답 수신 성공", model);
                return Optional.of(responseField.asText());
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Ollama ({}) 호출 실패 (로컬 LLM 미기동 또는 타임아웃: {}). 스마트 Fallback으로 자동 전환합니다.",
                    model, e.getMessage());
            return Optional.empty();
        }
    }
}
