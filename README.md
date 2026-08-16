# Anchor — Faithfulness Verification for LLM Outputs

An explainability/faithfulness verification layer for LLM outputs.

Given an LLM-generated response and the source context it should be grounded in, Anchor
splits the response into individual claims, checks each one against the retrieved
context, flags anything unsupported (a hallucination), and generates a human-readable
explanation for each flag — instead of trusting the model's output as one unverified
blob.

Built to demonstrate Java + Spring Boot proficiency (concurrency, testable service
design, REST/SSE APIs) alongside a real, typed React frontend.

## How it works

1. You submit a query, or add your own source documents first.
2. `PredictionService` (LLM generation) and `VectorRetrievalService` (pgvector similarity
   search) run **concurrently** — the answer and its grounding context are independent
   of each other.
3. `VerificationService` splits the answer into individual claims and checks each one
   against the retrieved context **concurrently**, one virtual thread per claim, via an
   LLM-judge prompt.
4. `ExplanationService` generates a plain-English explanation for any claim flagged
   unsupported.
5. The result — the answer, the context it was checked against, and a per-claim verdict
   with confidence and explanation — is returned as JSON, or streamed over SSE as each
   claim finishes.

## Architecture

```
Client → PredictionController ─┬→ PredictionService (LLM)
                                └→ VectorRetrievalService (pgvector)
                                        ↓
                                VerificationService (concurrent per-claim judging)
                                        ↓
                                ExplanationService (explains unsupported claims)
```

- `controller/PredictionController` — `POST /api/predictions`, full result in one response
- `controller/StreamController` — `GET /api/predictions/stream`, SSE: one `meta` event
  (answer + context) followed by one `claim` event per verdict as it completes
- `controller/DocumentController` — `POST /api/documents`, add your own ground-truth text
- `service/PredictionService` — calls the LLM for the raw answer
- `service/VectorRetrievalService` + `repository/VectorStoreRepository` — pgvector-backed
  similarity search
- `service/VerificationService` — the core faithfulness check; tested in isolation with
  JUnit + Mockito, no Spring context required
- `service/ExplanationService` — explains flagged claims
- `config/VectorStoreSeeder` — loads a handful of sample facts into the vector store on
  startup so there's something to verify against out of the box
- `config/ConcurrencyConfig` — the virtual-thread executor used for concurrent
  retrieval/generation and per-claim verification

## Stack

- **Backend**: Spring Boot 4.1, Java 21 (virtual threads), Spring AI 2.0
- **Model**: [Ollama](https://ollama.com) running locally — `llama3.2` for chat,
  `nomic-embed-text` for embeddings. No API key, no cost, no external dependency.
  (Swapping to a hosted provider is a Spring AI starter swap; see `pom.xml` /
  `application.yml`.)
- **Vector store**: Postgres + [pgvector](https://github.com/pgvector/pgvector), via
  Docker Compose
- **Frontend**: React + TypeScript + Vite, in `frontend/`
- **Tests**: JUnit 5, Mockito, AssertJ

## Running it

Four things need to be running, in this order:

1. **Postgres**
   ```bash
   docker compose up -d
   ```
2. **Ollama**, with both models pulled (one-time):
   ```bash
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```
3. **Backend**
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
   ./mvnw spring-boot:run
   ```
4. **Frontend**
   ```bash
   cd frontend
   npm install   # first time only
   npm run dev
   ```

Then open whatever URL Vite prints (`http://localhost:5173` by default).

### Trying it via curl

```bash
curl -X POST localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"text": "Copart is an online vehicle auction company headquartered in Dallas, Texas, founded in 1982."}'

curl -X POST localhost:8080/api/predictions \
  -H "Content-Type: application/json" \
  -d '{"query": "When was Copart founded and where is it based?"}'
```

## Testing

```bash
./mvnw test
```

`VerificationServiceTest` covers claim extraction, supported/unsupported judging, and
malformed-response fallback, entirely with mocked dependencies — no LLM or database
required to run it.
