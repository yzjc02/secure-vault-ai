package com.jiacheng.securevault.document.parser;

import java.nio.file.Path;

public interface DocumentTextParser {

    String parse(Path filePath, String fileType, String contentType);
}
