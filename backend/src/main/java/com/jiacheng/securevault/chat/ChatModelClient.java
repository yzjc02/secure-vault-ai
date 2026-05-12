package com.jiacheng.securevault.chat;

public interface ChatModelClient {

    ChatCompletion complete(ChatCompletionRequest request);

    String providerName();

    String modelName();
}
