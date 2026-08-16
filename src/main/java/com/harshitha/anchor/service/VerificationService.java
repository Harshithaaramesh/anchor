package com.harshitha.anchor.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.NoOpTemplateRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.harshitha.anchor.model.VerificationResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Core faithfulness check: splits an LLM response into individual claims and asks an
 * LLM judge whether each claim is supported by the retrieved source context. Claims are
 * independent of each other, so they're verified concurrently on a shared executor
 * rather than sequentially.
 */
@Service
public class VerificationService {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    /*
     * Interpolated with String.formatted() and sent via NoOpTemplateRenderer rather
     * than Spring AI's default StringTemplate renderer, because the literal JSON braces
     * in the instructions below are otherwise mis-parsed as StringTemplate expressions.
     */
    private static final String JUDGE_PROMPT = """
            You are a strict fact-checker. Given a CONTEXT and a CLAIM, decide whether the
            CONTEXT supports the CLAIM. Respond with ONLY a JSON object of the form
            {"supported": true|false, "confidence": 0.0-1.0} and no other text.

            CONTEXT:
            %s

            CLAIM:
            %s
            """;

    private final ChatClient chatClient;
    private final ExecutorService verificationExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerificationService(
            ChatClient chatClient,
            @Qualifier("verificationExecutor") ExecutorService verificationExecutor) {
        this.chatClient = chatClient;
        this.verificationExecutor = verificationExecutor;
    }

    public List<VerificationResult> verify(String response, List<String> context) {
        List<String> claims = extractClaims(response);
        if (claims.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<VerificationResult>> futures = claims.stream()
                .map(claim -> verifyClaimAsync(claim, context))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public List<String> extractClaims(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        return Arrays.stream(SENTENCE_BOUNDARY.split(response.strip()))
                .map(String::strip)
                .filter(claim -> !claim.isEmpty())
                .toList();
    }

    public CompletableFuture<VerificationResult> verifyClaimAsync(String claim, List<String> context) {
        String contextBlock = String.join("\n---\n", context);
        return CompletableFuture.supplyAsync(() -> verifyClaim(claim, contextBlock), verificationExecutor);
    }

    private VerificationResult verifyClaim(String claim, String contextBlock) {
        String prompt = JUDGE_PROMPT.formatted(contextBlock, claim);
        String judgeResponse = chatClient.prompt()
                .user(prompt)
                .templateRenderer(new NoOpTemplateRenderer())
                .call()
                .content();
        return parseVerdict(claim, judgeResponse);
    }

    VerificationResult parseVerdict(String claim, String judgeResponse) {
        try {
            JudgeVerdict verdict = objectMapper.readValue(judgeResponse, JudgeVerdict.class);
            return new VerificationResult(claim, verdict.supported(), verdict.confidence(), null);
        } catch (RuntimeException e) {
            return new VerificationResult(claim, false, 0.0, "Verification failed: could not interpret judge response");
        }
    }

    private record JudgeVerdict(boolean supported, double confidence) {
    }
}
