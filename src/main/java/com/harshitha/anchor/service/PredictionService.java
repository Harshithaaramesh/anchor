package com.harshitha.anchor.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.NoOpTemplateRenderer;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    private final ChatClient chatClient;

    public PredictionService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String predict(String query) {
        // NoOpTemplateRenderer: query is arbitrary user input and must not be parsed as a
        // StringTemplate expression (e.g. a query containing literal { } would otherwise
        // fail to render, same issue as the JSON braces in VerificationService's prompt).
        return chatClient.prompt()
                .user(query)
                .templateRenderer(new NoOpTemplateRenderer())
                .call()
                .content();
    }
}
