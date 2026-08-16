package com.harshitha.anchor.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harshitha.anchor.model.PredictionRequest;
import com.harshitha.anchor.model.PredictionResult;
import com.harshitha.anchor.model.VerificationResult;
import com.harshitha.anchor.service.ExplanationService;
import com.harshitha.anchor.service.PredictionService;
import com.harshitha.anchor.service.VectorRetrievalService;
import com.harshitha.anchor.service.VerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final VectorRetrievalService vectorRetrievalService;
    private final VerificationService verificationService;
    private final ExplanationService explanationService;
    private final ExecutorService verificationExecutor;

    public PredictionController(
            PredictionService predictionService,
            VectorRetrievalService vectorRetrievalService,
            VerificationService verificationService,
            ExplanationService explanationService,
            @Qualifier("verificationExecutor") ExecutorService verificationExecutor) {
        this.predictionService = predictionService;
        this.vectorRetrievalService = vectorRetrievalService;
        this.verificationService = verificationService;
        this.explanationService = explanationService;
        this.verificationExecutor = verificationExecutor;
    }

    @PostMapping
    public PredictionResult predict(@Valid @RequestBody PredictionRequest request) {
        String query = request.query();

        // Generation and retrieval are independent of each other, so run them concurrently.
        CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> predictionService.predict(query), verificationExecutor);
        CompletableFuture<List<String>> contextFuture = CompletableFuture.supplyAsync(() -> vectorRetrievalService.retrieveContext(query), verificationExecutor);

        String response = responseFuture.join();
        List<String> context = contextFuture.join();

        List<VerificationResult> verifications = verificationService.verify(response, context);
        List<VerificationResult> explained = explanationService.explain(verifications, context);

        return new PredictionResult(query, response, context, explained);
    }
}
