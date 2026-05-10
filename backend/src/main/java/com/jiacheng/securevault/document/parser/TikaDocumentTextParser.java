package com.jiacheng.securevault.document.parser;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Component
public class TikaDocumentTextParser implements DocumentTextParser {

    private static final Set<String> PLAIN_TEXT_TYPES = Set.of("txt", "md", "markdown");

    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public String parse(Path filePath, String fileType, String contentType) {
        try {
            String rawText = isPlainText(fileType)
                    ? Files.readString(filePath, StandardCharsets.UTF_8)
                    : parseWithTika(filePath);
            String cleanedText = clean(rawText);
            if (!StringUtils.hasText(cleanedText)) {
                throw new DocumentParseException("未抽取到有效文本");
            }
            return cleanedText;
        } catch (DocumentParseException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DocumentParseException("文档读取失败", ex);
        } catch (TikaException | SAXException ex) {
            throw new DocumentParseException("文档文本解析失败", ex);
        }
    }

    private boolean isPlainText(String fileType) {
        return fileType != null && PLAIN_TEXT_TYPES.contains(fileType.toLowerCase(Locale.ROOT));
    }

    private String parseWithTika(Path filePath) throws IOException, TikaException, SAXException {
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        BodyContentHandler handler = new BodyContentHandler(-1);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            parser.parse(inputStream, handler, metadata, parseContext);
        }
        return handler.toString();
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("(?m)[ \\t\\x0B\\f]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
