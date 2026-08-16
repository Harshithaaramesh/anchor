package com.harshitha.anchor.service;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.harshitha.anchor.repository.VectorStoreRepository;

@Service
public class VectorRetrievalService {

    private final VectorStoreRepository vectorStoreRepository;
    private final int topKContext;

    public VectorRetrievalService(
            VectorStoreRepository vectorStoreRepository,
            @Value("${anchor.verification.top-k-context:5}") int topKContext) {
        this.vectorStoreRepository = vectorStoreRepository;
        this.topKContext = topKContext;
    }

    public List<String> retrieveContext(String query) {
        return vectorStoreRepository.findRelevant(query, topKContext).stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .toList();
    }
}
