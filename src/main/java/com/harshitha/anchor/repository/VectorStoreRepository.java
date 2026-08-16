package com.harshitha.anchor.repository;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

@Repository
public class VectorStoreRepository {

    private final VectorStore vectorStore;

    public VectorStoreRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> findRelevant(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        return results != null ? results : List.of();
    }

    public void save(List<Document> documents) {
        vectorStore.add(documents);
    }

    public void delete(List<String> ids) {
        vectorStore.delete(ids);
    }
}
