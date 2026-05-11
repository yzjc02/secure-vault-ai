package com.jiacheng.securevault.document;

import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.entity.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentResponseTest {

    @Test
    void shouldReturnZeroWhenLegacyDocumentChunkCountIsNull() {
        Document document = new Document();
        document.setId(1L);
        document.setUserId(1L);
        document.setTitle("legacy document");
        document.setStatus(Document.STATUS_PARSED);
        document.setChunkCount(null);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        DocumentResponse response = DocumentResponse.from(document, false);

        assertThat(response.getChunkCount()).isZero();
    }
}
