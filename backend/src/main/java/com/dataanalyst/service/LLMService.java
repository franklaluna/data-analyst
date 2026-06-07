package com.dataanalyst.service;

import com.dataanalyst.config.LLMConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;
    private final LLMConfig config;

    public LLMService(OkHttpClient client, LLMConfig config) {
        this.client = client;
        this.config = config;
    }

    public String chat(String systemPrompt, String userMessage) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildMsg("system", systemPrompt));
        messages.add(buildMsg("user", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 2000);

        String json = mapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/v1/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .post(RequestBody.create(json, JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("LLM API error {}: {}", response.code(), respBody);
                throw new IOException("LLM API error " + response.code());
            }
            Map<String, Object> respMap = mapper.readValue(respBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }
    }

    public String generateAnalysisSummary(Map<String, Object> profileResult) throws IOException {
        String systemPrompt =
            "你是一个数据分析助手。根据上传文件的 profiling 结果，用中文生成一段简洁的数据分析摘要。\n" +
            "要求：\n" +
            "1. 说明数据的基本情况（行数、列数、时间范围等）\n" +
            "2. 指出关键数值列的统计特征（总和、均值、最大值等）\n" +
            "3. 如果发现异常或有趣的模式，简要指出\n" +
            "4. 语言简洁专业，不超过 200 字";

        String data = mapper.writeValueAsString(profileResult);
        return chat(systemPrompt, "以下是数据文件的分析结果：\n" + data);
    }

    public String generatePandasCode(String question, List<Map<String, Object>> columnsInfo,
                                      List<Map<String, Object>> sampleData) throws IOException {
        String systemPrompt =
            "你是一个 Python 数据分析代码生成器。根据用户的问题和数据 schema，生成可执行的 pandas 代码。\n" +
            "规则：\n" +
            "1. 变量 df 已经加载了数据，可以直接使用\n" +
            "2. 结果赋值给 result 变量（DataFrame 或标量值）\n" +
            "3. 如果需要图表，将 ECharts option JSON 赋值给 chart 变量\n" +
            "4. 只生成代码，不要解释\n" +
            "5. 不要使用 import 语句，pandas 和 numpy 已经导入\n" +
            "6. 不要使用 open()、exec()、eval() 等危险函数\n" +
            "7. 日期列可能需要 pd.to_datetime() 转换";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("columns", columnsInfo);
        data.put("sample", sampleData);
        String dataJson = mapper.writeValueAsString(data);
        String userMsg = "数据 schema：\n" + dataJson + "\n\n用户问题：" + question;

        return chat(systemPrompt, userMsg);
    }

    private Map<String, Object> buildMsg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
