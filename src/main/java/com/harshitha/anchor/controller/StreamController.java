package com.harshitha.anchor.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harshitha.anchor.model.StreamMeta;
import com.harshitha.anchor.model.VerificationResult;
import com.harshitha.anchor.service.ExplanationService;
import com.harshitha.anchor.service.PredictionService;
import com.harshitha.anchor.service.VectorRetrievalService;
import com.harshitha.anchor.service.VerificationService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Streams the verification of a query as it happens, rather than waiting for the whole
 * response to be fully checked before returning anything. Emits one "meta" event (the raw
 * answer and the context it should be grounded in) followed by one "claim" event per
 * verification verdict, in completion order. Returned as a Flux: Spring MVC (which wins
 * over WebFlux's DispatcherHandler when both starters are present, see WebFluxConfig)
 * bridges Reactor publishers to a streaming servlet response via ReactiveAdapterRegistry,
 * so no reactive server stack is required for this to work.
 */
@RestController
@RequestMapping("/api/predictions")
public class StreamController {

    private final PredictionService predictionService;
    private final VectorRetrievalService vectorRetrievalService;
    private final VerificationService verificationService;
    private final ExplanationService explanationService;
    private final ExecutorService verificationExecutor;

    public StreamController(
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

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@RequestParam String query) {
        CompletableFuture<String> responseFuture =
                CompletableFuture.supplyAsync(() -> predictionService.predict(query), verificationExecutor);
        CompletableFuture<List<String>> contextFuture =
                CompletableFuture.supplyAsync(() -> vectorRetrievalService.retrieveContext(query), verificationExecutor);

        return Mono.fromFuture(responseFuture)
                .zipWith(Mono.fromFuture(contextFuture))
                .flatMapMany(generationAndContext -> {
                    String response = generationAndContext.getT1();
                    List<String> context = generationAndContext.getT2();
                    List<String> claims = verificationService.extractClaims(response);

                    Flux<ServerSentEvent<Object>> metaEvent = Flux.just(
                            ServerSentEvent.<Object>builder(new StreamMeta(query, response, context))
                                    .event("meta")
                                    .build());

                    List<Mono<ServerSentEvent<Object>>> claimEvents = claims.stream()
                            .map(claim -> Mono.fromFuture(verificationService.verifyClaimAsync(claim, context))
                                    .flatMap(result -> explainIfUnsupported(result, context))
                                    .map(result -> ServerSentEvent.<Object>builder(result).event("claim").build()))
                            .toList();

                    return Flux.concat(metaEvent, Flux.merge(claimEvents));
                });
    }

    private Mono<VerificationResult> explainIfUnsupported(VerificationResult result, List<String> context) {
        if (result.supported()) {
            return Mono.just(result);
        }
        return Mono.fromCallable(() -> explanationService.explain(List.of(result), context).get(0))
                .subscribeOn(Schedulers.fromExecutor(verificationExecutor));
    }
}
