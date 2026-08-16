import type { DocumentUploadResult, StreamMeta, VerificationResult } from './types';

export async function uploadDocument(text: string): Promise<DocumentUploadResult> {
  const res = await fetch('/api/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  });
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`);
  }
  return res.json();
}

export interface StreamHandlers {
  onMeta: (meta: StreamMeta) => void;
  onClaim: (claim: VerificationResult) => void;
  /**
   * Fires once the connection ends, for any reason. Plain EventSource has no distinct
   * "server finished the stream" signal separate from "connection error" - both surface
   * as the same "error" event - so this fires for both a normal completion and a failure.
   */
  onClose: () => void;
}

/**
 * Opens an SSE connection to /api/predictions/stream and dispatches "meta" and "claim"
 * events as they arrive. Returns a function that closes the connection early if needed.
 */
export function streamPrediction(query: string, handlers: StreamHandlers): () => void {
  const source = new EventSource(`/api/predictions/stream?query=${encodeURIComponent(query)}`);

  source.addEventListener('meta', (event: MessageEvent<string>) => {
    handlers.onMeta(JSON.parse(event.data) as StreamMeta);
  });

  source.addEventListener('claim', (event: MessageEvent<string>) => {
    handlers.onClaim(JSON.parse(event.data) as VerificationResult);
  });

  source.addEventListener('error', () => {
    source.close();
    handlers.onClose();
  });

  return () => source.close();
}
