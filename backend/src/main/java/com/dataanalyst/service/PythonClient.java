package com.dataanalyst.service;

import com.dataanalyst.config.LLMConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PythonClient {

    private static final Logger log = LoggerFactory.getLogger(PythonClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;
    private final LLMConfig config;

    public PythonClient(OkHttpClient client, LLMConfig config) {
        this.client = client;
        this.config = config;
    }

    public Map<String, Object> analyze(String filePath) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file_path", filePath);
        return post("/analyze", body);
    }

    public Map<String, Object> executeCode(String code, String filePath) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("file_path", filePath);
        return post("/execute", body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) throws IOException {
        String json = mapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(config.getPythonBaseUrl() + path)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Python service error {}: {}", response.code(), respBody);
                throw new IOException("Python service error " + response.code() + ": " + respBody);
            }
            return mapper.readValue(respBody, Map.class);
        }
    }
}
