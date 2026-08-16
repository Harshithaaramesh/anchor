import { useState } from 'react';
import { uploadDocument } from '../api';

export function DocumentPanel() {
  const [text, setText] = useState('');
  const [added, setAdded] = useState<string[]>([]);
  const [status, setStatus] = useState<{ message: string; isError: boolean } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleAdd() {
    const trimmed = text.trim();
    if (!trimmed) return;
    setSubmitting(true);
    setStatus(null);
    try {
      const doc = await uploadDocument(trimmed);
      setAdded((prev) => [doc.text, ...prev]);
      setStatus({ message: 'Added.', isError: false });
      setText('');
    } catch (err) {
      setStatus({ message: `Failed to add document: ${(err as Error).message}`, isError: true });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="panel">
      <div className="panel__heading">
        <span className="step-badge">1</span>
        <h2>Add source material</h2>
      </div>
      <p className="panel__hint">
        Paste a <strong>factual statement</strong> here — the ground truth that answers will
        be checked against. Not a question: e.g. "Copart is an online vehicle auction
        company headquartered in Dallas, Texas, founded in 1982," not "What is Copart?"
      </p>
      <textarea
        rows={3}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) handleAdd();
        }}
        placeholder="Copart is an online vehicle auction company headquartered in Dallas, Texas, founded in 1982."
      />
      <button onClick={handleAdd} disabled={submitting || !text.trim()}>
        Add document
      </button>
      {status && <div className={`status ${status.isError ? 'status--error' : ''}`}>{status.message}</div>}
      {added.length > 0 && (
        <div className="doc-list">
          {added.map((docText, i) => (
            <div className="doc-list__item" key={i}>
              {docText}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
