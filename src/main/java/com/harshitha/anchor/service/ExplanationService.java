package com.harshitha.anchor.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.NoOpTemplateRenderer;
import org.springframework.stereotype.Service;

import com.harshitha.anchor.model.VerificationResult;

/**
 * Fills in a human-readable explanation for claims the VerificationService flagged as
 * unsupported. Kept separate from VerificationService so the (cheaper, more frequent)
 * supported/unsupported judgment doesn't always pay for an explanation generation call.
 */
@Service
public class ExplanationService {

    /*
     * Interpolated with String.formatted() and sent via NoOpTemplateRenderer, matching
     * VerificationService, so that retrieved context text is never re-parsed as a
     * StringTemplate expression regardless of what characters it happens to contain.
     */
    private static final String EXPLANATION_PROMPT = """
            A claim made in an AI-generated response was NOT supported by the source
            context it should have been grounded in. In one or two sentences, explain to
            an end user why this claim appears to be unsupported or a hallucination,
            referencing the context where relevant.

            CONTEXT:
            %s

            UNSUPPORTED CLAIM:
            %s
            """;

    private final ChatClient chatClient;

    public ExplanationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public List<VerificationResult> explain(List<VerificationResult> verifications, List<String> context) {
        String contextBlock = String.join("\n---\n", context);
        return verifications.stream()
                .map(result -> result.supported() ? result : result.withExplanation(explainUnsupported(result.claim(), contextBlock)))
                .toList();
    }

    private String explainUnsupported(String claim, String contextBlock) {
        String prompt = EXPLANATION_PROMPT.formatted(contextBlock, claim);
        return chatClient.prompt()
                .user(prompt)
                .templateRenderer(new NoOpTemplateRenderer())
                .call()
                .content();
    }
}
