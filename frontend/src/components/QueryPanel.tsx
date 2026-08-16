import { useRef, useState } from 'react';
import { streamPrediction } from '../api';
import type { StreamMeta, VerificationResult } from '../types';
import { ClaimCard } from './ClaimCard';

export function QueryPanel() {
  const [query, setQuery] = useState('');
  const [meta, setMeta] = useState<StreamMeta | null>(null);
  const [claims, setClaims] = useState<VerificationResult[]>([]);
  const [loading, setLoading] = useState(false);
  const closeStreamRef = useRef<(() => void) | null>(null);

  function ask() {
    const trimmed = query.trim();
    if (!trimmed) return;

    closeStreamRef.current?.();
    setMeta(null);
    setClaims([]);
    setLoading(true);

    closeStreamRef.current = streamPrediction(trimmed, {
      onMeta: (m) => setMeta(m),
      onClaim: (c) => setClaims((prev) => [...prev, c]),
      onClose: () => setLoading(false),
    });
  }

  return (
    <section className="panel">
      <div className="panel__heading">
        <span className="step-badge">2</span>
        <h2>Ask a question</h2>
      </div>
      <p className="panel__hint">Claims light up below as each one finishes being checked.</p>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && ask()}
        placeholder="Where is Copart headquartered and when was it founded?"
      />
      <button onClick={ask} disabled={loading || !query.trim()}>
        {loading ? 'Verifying…' : 'Ask'}
      </button>

      {meta && (
        <div className="answer-block">
          <div className="answer-text">{meta.response}</div>

          <div className="claims">
            {claims.map((c, i) => (
              <ClaimCard claim={c} key={i} />
            ))}
            {loading && <div className="status">Checking remaining claims…</div>}
          </div>

          <details className="context-details">
            <summary>Source context used ({meta.sourceContext.length})</summary>
            <ul>
              {meta.sourceContext.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          </details>
        </div>
      )}
    </section>
  );
}
