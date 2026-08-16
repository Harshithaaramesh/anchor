package com.harshitha.anchor.controller;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harshitha.anchor.model.DocumentUploadRequest;
import com.harshitha.anchor.model.DocumentUploadResult;
import com.harshitha.anchor.repository.VectorStoreRepository;

import jakarta.validation.Valid;

/**
 * Lets callers add their own ground-truth material to the vector store, so
 * PredictionController's verification isn't limited to the fixed seed documents.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final VectorStoreRepository vectorStoreRepository;

    public DocumentController(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }

    @PostMapping
    public DocumentUploadResult upload(@Valid @RequestBody DocumentUploadRequest request) {
        Document document = new Document(request.text());
        vectorStoreRepository.save(List.of(document));
        return new DocumentUploadResult(document.getId(), document.getText());
    }
}
