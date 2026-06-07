package com.dataanalyst.model;

import java.sql.Timestamp;

public class QARecord {
    private Long id;
    private Long sessionId;
    private String question;
    private String answer;
    private String chartJson;
    private String codeGenerated;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getChartJson() { return chartJson; }
    public void setChartJson(String chartJson) { this.chartJson = chartJson; }
    public String getCodeGenerated() { return codeGenerated; }
    public void setCodeGenerated(String codeGenerated) { this.codeGenerated = codeGenerated; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
