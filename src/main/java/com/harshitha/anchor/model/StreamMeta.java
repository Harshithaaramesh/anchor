package com.harshitha.anchor.model;

import java.util.List;

/**
 * First event sent on the SSE stream: the raw LLM answer and the context it was
 * retrieved against, before individual per-claim verdicts start arriving.
 */
public record StreamMeta(String query, String response, List<String> sourceContext) {
}
