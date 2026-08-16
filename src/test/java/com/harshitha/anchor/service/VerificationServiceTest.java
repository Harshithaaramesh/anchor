package com.harshitha.anchor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.TemplateRenderer;

import com.harshitha.anchor.model.VerificationResult;

class VerificationServiceTest {

    private ChatClient chatClient;
    private ExecutorService executor;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        executor = Executors.newSingleThreadExecutor();
        verificationService = new VerificationService(chatClient, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void verify_singleSupportedClaim_returnsSupportedResult() {
        stubJudgeResponses("{\"supported\": true, \"confidence\": 0.95}");

        List<VerificationResult> results = verificationService.verify(
                "The sky is blue.", List.of("The sky appears blue due to Rayleigh scattering."));

        assertThat(results).hasSize(1);
        VerificationResult result = results.get(0);
        assertThat(result.claim()).isEqualTo("The sky is blue.");
        assertThat(result.supported()).isTrue();
        assertThat(result.confidence()).isEqualTo(0.95);
        assertThat(result.explanation()).isNull();
    }

    @Test
    void verify_singleUnsupportedClaim_returnsUnsupportedResult() {
        stubJudgeResponses("{\"supported\": false, \"confidence\": 0.2}");

        List<VerificationResult> results = verificationService.verify(
                "The moon is made of cheese.", List.of("The moon is composed primarily of rock."));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).supported()).isFalse();
        assertThat(results.get(0).confidence()).isEqualTo(0.2);
    }

    @Test
    void verify_multipleClaims_returnsOneResultPerClaimInOrder() {
        stubJudgeResponses(
                "{\"supported\": true, \"confidence\": 0.9}",
                "{\"supported\": false, \"confidence\": 0.1}");

        List<VerificationResult> results = verificationService.verify(
                "Paris is the capital of France. Paris has a population of 50 million.",
                List.of("Paris is the capital of France. Its metro population is about 2.1 million."));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).claim()).isEqualTo("Paris is the capital of France.");
        assertThat(results.get(0).supported()).isTrue();
        assertThat(results.get(1).claim()).isEqualTo("Paris has a population of 50 million.");
        assertThat(results.get(1).supported()).isFalse();
    }

    @Test
    void verify_blankResponse_returnsEmptyList() {
        List<VerificationResult> results = verificationService.verify("   ", List.of("some context"));

        assertThat(results).isEmpty();
    }

    @Test
    void extractClaims_splitsOnSentenceBoundaries() {
        List<String> claims = verificationService.extractClaims("First claim. Second claim! Third claim?");

        assertThat(claims).containsExactly("First claim.", "Second claim!", "Third claim?");
    }

    @Test
    void extractClaims_blankResponse_returnsEmptyList() {
        assertThat(verificationService.extractClaims(null)).isEmpty();
        assertThat(verificationService.extractClaims("")).isEmpty();
        assertThat(verificationService.extractClaims("   ")).isEmpty();
    }

    @Test
    void parseVerdict_malformedJudgeResponse_fallsBackToUnsupportedWithExplanation() {
        VerificationResult result = verificationService.parseVerdict("some claim", "not valid json");

        assertThat(result.supported()).isFalse();
        assertThat(result.confidence()).isEqualTo(0.0);
        assertThat(result.explanation()).isNotBlank();
    }

    private void stubJudgeResponses(String... responses) {
        when(chatClient.prompt()
                .user(anyString())
                .templateRenderer(any(TemplateRenderer.class))
                .call()
                .content())
                .thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
    }
}
