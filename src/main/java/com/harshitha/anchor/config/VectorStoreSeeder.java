package com.harshitha.anchor.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.harshitha.anchor.repository.VectorStoreRepository;

/**
 * Loads a fixed set of sample documents into the vector store on startup, so there's
 * something for VerificationService to actually check claims against out of the box.
 * Runs on every startup: seed documents carry stable ids (deterministic UUIDs derived
 * from a readable key, since PgVectorStore's id column requires actual UUID values), so
 * this deletes-then-reinserts them rather than accumulating duplicates across restarts.
 */
@Component
public class VectorStoreSeeder implements ApplicationRunner {

    private static final List<Document> SEED_DOCUMENTS = List.of(
            // Simple general-knowledge facts
            document("seed-fact-eiffel-tower",
                    "The Eiffel Tower is located in Paris, France, and was completed in 1889."),
            document("seed-fact-mount-everest",
                    "Mount Everest is the tallest mountain above sea level on Earth, standing at "
                            + "approximately 8,849 meters."),
            document("seed-fact-pacific-ocean",
                    "The Pacific Ocean is the largest and deepest of Earth's five oceans."),

            // Facts about this project's technology stack
            document("seed-tech-spring-boot",
                    "Spring Boot is a Java framework built on top of the Spring Framework that provides "
                            + "auto-configuration and embedded servers to simplify building production-ready "
                            + "applications."),
            document("seed-tech-virtual-threads",
                    "Java virtual threads, finalized in Java 21, are lightweight threads managed by the JVM "
                            + "rather than the operating system, making it practical to run large numbers of "
                            + "concurrent blocking tasks."),
            document("seed-tech-pgvector",
                    "pgvector is an open-source PostgreSQL extension that adds a vector data type and "
                            + "similarity search operators, commonly used to store embeddings for "
                            + "retrieval-augmented generation systems."),

            // Facts about this project's own purpose (RAG / hallucination / faithfulness)
            document("seed-purpose-rag",
                    "Retrieval-augmented generation, or RAG, is a technique where a language model's response "
                            + "is grounded in documents retrieved from an external knowledge source rather than "
                            + "relying solely on the model's training data."),
            document("seed-purpose-hallucination",
                    "A hallucination in the context of large language models refers to a generated statement "
                            + "that sounds plausible but is not actually supported by the source material or "
                            + "verifiable fact."),
            document("seed-purpose-faithfulness",
                    "Faithfulness verification checks whether each individual claim in a language model's "
                            + "response is backed by the retrieved source context, flagging any claim that is "
                            + "not supported so it can be reviewed as a potential hallucination."));

    private final VectorStoreRepository vectorStoreRepository;
    private final boolean seedEnabled;

    public VectorStoreSeeder(
            VectorStoreRepository vectorStoreRepository,
            @Value("${anchor.seed.enabled:true}") boolean seedEnabled) {
        this.vectorStoreRepository = vectorStoreRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        List<String> ids = SEED_DOCUMENTS.stream().map(Document::getId).toList();
        vectorStoreRepository.delete(ids);
        vectorStoreRepository.save(SEED_DOCUMENTS);
    }

    private static Document document(String key, String text) {
        String id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        return Document.builder().id(id).text(text).metadata(Map.of()).build();
    }
}
