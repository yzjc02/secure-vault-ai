package com.jiacheng.securevault.rag;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    private final RagProperties properties;

    public RagPromptBuilder(RagProperties properties) {
        this.properties = properties;
    }

    public String systemPrompt() {
        return "你是 Secure Vault AI 的本地知识库问答助手。你必须只根据提供的“知识库片段”回答问题。"
                + "如果片段中没有足够信息，请明确说明“我没有在当前知识库中找到足够依据”。"
                + "不要编造事实。不要引用未提供的内容。回答时可以引用 [S1]、[S2] 这样的来源标识。"
                + "回答应尽量使用用户提问语言。";
    }

    public String buildPrompt(String question, List<RagSource> sources) {
        return "System:\n" + systemPrompt() + "\n\n" + buildUserPrompt(question, sources);
    }

    public String buildUserPrompt(String question, List<RagSource> sources) {
        return "Context:\n"
                + buildContext(sources)
                + "\nUser Question:\n"
                + question
                + "\n\nAnswer:";
    }

    private String buildContext(List<RagSource> sources) {
        StringBuilder context = new StringBuilder();
        if (sources == null || sources.isEmpty()) {
            return "无可用知识库片段。\n";
        }
        int maxContextChars = properties.getMaxContextChars();
        for (RagSource source : sources) {
            String header = "[%s]\ndocumentTitle: %s\nfilename: %s\nchunkIndex: %s\ncontent:\n"
                    .formatted(
                            safe(source.getSourceId()),
                            safe(source.getDocumentTitle()),
                            safe(source.getOriginalFilename()),
                            source.getChunkIndex() == null ? "" : source.getChunkIndex()
                    );
            int remaining = maxContextChars - context.length() - header.length() - 2;
            if (remaining <= 0) {
                break;
            }
            String content = truncate(safe(source.getContent()), remaining);
            context.append(header).append(content).append("\n\n");
            if (context.length() >= maxContextChars) {
                break;
            }
        }
        return context.toString();
    }

    private String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
