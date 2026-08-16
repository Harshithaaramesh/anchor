package com.harshitha.anchor.model;

public record VerificationResult(String claim, boolean supported, double confidence, String explanation) {

    public VerificationResult withExplanation(String newExplanation) {
        return new VerificationResult(claim, supported, confidence, newExplanation);
    }
}
