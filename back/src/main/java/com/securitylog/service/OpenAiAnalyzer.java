package com.securitylog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitylog.dto.AnalysisResult;
import com.securitylog.entity.LogEntry;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Component;

import java.util.List;

@Component   // disabled while outbound HTTPS to OpenAI is blocked by a TLS-intercepting proxy
public class OpenAiAnalyzer {

    private static final String SYSTEM_PROMPT = """
            You are a web log file analyzer. I will attach log files in \
            https://help.zscaler.com/zia/nss-feed-output-format-web-logs format.
            Fields are: time cip sip login ua method url respcode reqhdrsize reqsize resphrdsize respsize referrer
            For each log file return a JSON output as such \
            { "description": "problem description", "certainty": percent sure of verdict, "rows": [array of rows involved in the threat]}
            It is possible for there to be no threat, in which case there is a 0 certainty, no description, and no rows.
            Return only raw JSON with no markdown or code fences.
            """;

    private static final String HEADER =
            "time\tcip\tsip\tlogin\tua\tmethod\turl\trespcode\treqhdrsize\treqsize\tresphrdsize\trespsize\treferrer";

    private final OpenAiChatModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiAnalyzer() {
        String apiKey = System.getenv("OPEN_AI_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPEN_AI_KEY environment variable is not set");
        }
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .build();
    }

    public AnalysisResult analyze(List<LogEntry> entries) {
        String logData = formatEntries(entries);
        List<ChatMessage> messages = List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from("logfile:\n" + logData)
        );
        try {
            Response<AiMessage> response = model.generate(messages);
            String json = stripFences(response.content().text());
            return objectMapper.readValue(json, AnalysisResult.class);
        } catch (Exception e) {
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }
    }

    private String formatEntries(List<LogEntry> entries) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (LogEntry e : entries) {
            sb.append(safe(e.getTime())).append('\t')
              .append(safe(e.getCip())).append('\t')
              .append(safe(e.getSip())).append('\t')
              .append(safe(e.getLogin())).append('\t')
              .append(safe(e.getUa())).append('\t')
              .append(safe(e.getMethod())).append('\t')
              .append(safe(e.getUrl())).append('\t')
              .append(safe(e.getRespcode())).append('\t')
              .append(safe(e.getReqhdrsize())).append('\t')
              .append(safe(e.getReqsize())).append('\t')
              .append(safe(e.getResphrdsize())).append('\t')
              .append(safe(e.getRespsize())).append('\t')
              .append(safe(e.getReferrer())).append('\n');
        }
        return sb.toString();
    }

    private static String safe(String val) {
        return val != null ? val : "";
    }

    private static String stripFences(String text) {
        String s = text.strip();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\n?", "");
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3).stripTrailing();
            }
        }
        return s;
    }
}
