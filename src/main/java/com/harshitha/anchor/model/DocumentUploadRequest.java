package com.harshitha.anchor.model;

import jakarta.validation.constraints.NotBlank;

public record DocumentUploadRequest(@NotBlank String text) {
}
