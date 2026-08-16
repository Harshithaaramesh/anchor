package com.harshitha.anchor.model;

import jakarta.validation.constraints.NotBlank;

public record PredictionRequest(@NotBlank String query) {
}
