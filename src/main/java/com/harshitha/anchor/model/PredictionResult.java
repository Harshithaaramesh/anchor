package com.harshitha.anchor.model;

import java.util.List;

public record PredictionResult(
        String query,
        String response,
        List<String> sourceContext,
        List<VerificationResult> verifications) {
}
