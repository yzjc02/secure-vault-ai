package com.jiacheng.securevault.document.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.chunking")
public class ChunkingProperties {

    @Min(value = 1, message = "chunkSize must be greater than 0")
    private int chunkSize = 1000;

    @Min(value = 0, message = "overlapSize must be greater than or equal to 0")
    private int overlapSize = 150;

    @Min(value = 1, message = "minChunkSize must be greater than 0")
    private int minChunkSize = 100;

    @AssertTrue(message = "overlapSize must be less than chunkSize")
    public boolean isOverlapSizeValid() {
        return overlapSize < chunkSize;
    }

    @AssertTrue(message = "minChunkSize must be less than or equal to chunkSize")
    public boolean isMinChunkSizeValid() {
        return minChunkSize <= chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    public void setOverlapSize(int overlapSize) {
        this.overlapSize = overlapSize;
    }

    public int getMinChunkSize() {
        return minChunkSize;
    }

    public void setMinChunkSize(int minChunkSize) {
        this.minChunkSize = minChunkSize;
    }
}
