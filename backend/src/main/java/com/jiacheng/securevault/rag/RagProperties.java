package com.jiacheng.securevault.rag;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    @Min(value = 1, message = "rag defaultTopK must be at least 1")
    @Max(value = 100, message = "rag defaultTopK must be at most 100")
    private int defaultTopK = 5;

    @Min(value = 1, message = "rag maxTopK must be at least 1")
    @Max(value = 100, message = "rag maxTopK must be at most 100")
    private int maxTopK = 10;

    @Min(value = 1, message = "rag maxQuestionLength must be at least 1")
    @Max(value = 10000, message = "rag maxQuestionLength must be at most 10000")
    private int maxQuestionLength = 2000;

    @Min(value = 100, message = "rag maxContextChars must be at least 100")
    @Max(value = 50000, message = "rag maxContextChars must be at most 50000")
    private int maxContextChars = 8000;

    @Min(value = 20, message = "rag sourcePreviewLength must be at least 20")
    @Max(value = 2000, message = "rag sourcePreviewLength must be at most 2000")
    private int sourcePreviewLength = 220;

    @AssertTrue(message = "rag defaultTopK must be less than or equal to maxTopK")
    public boolean isTopKRangeValid() {
        return defaultTopK <= maxTopK;
    }

    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    public int getMaxTopK() { return maxTopK; }
    public void setMaxTopK(int maxTopK) { this.maxTopK = maxTopK; }
    public int getMaxQuestionLength() { return maxQuestionLength; }
    public void setMaxQuestionLength(int maxQuestionLength) { this.maxQuestionLength = maxQuestionLength; }
    public int getMaxContextChars() { return maxContextChars; }
    public void setMaxContextChars(int maxContextChars) { this.maxContextChars = maxContextChars; }
    public int getSourcePreviewLength() { return sourcePreviewLength; }
    public void setSourcePreviewLength(int sourcePreviewLength) { this.sourcePreviewLength = sourcePreviewLength; }
}
