package com.jiacheng.securevault.chat;

public class ChatCompletion {

    private final String answer;
    private final String provider;
    private final String model;

    public ChatCompletion(String answer, String provider, String model) {
        this.answer = answer;
        this.provider = provider;
        this.model = model;
    }

    public String getAnswer() { return answer; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
}
