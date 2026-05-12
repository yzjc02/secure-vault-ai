package com.jiacheng.securevault.document.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);

    String model();

    int dimension();
}
