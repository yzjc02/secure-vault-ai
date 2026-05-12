package com.jiacheng.securevault.document.parser;

import java.io.InputStream;

public interface DocumentTextParser {

    String parse(InputStream inputStream, String fileType, String contentType);
}
