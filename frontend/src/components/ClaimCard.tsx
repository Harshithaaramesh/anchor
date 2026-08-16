import type { VerificationResult } from '../types';

function CheckIcon() {
  return (
    <svg viewBox="0 0 12 12" fill="none">
      <path d="M2.5 6.5L4.75 8.75L9.5 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CrossIcon() {
  return (
    <svg viewBox="0 0 12 12" fill="none">
      <path d="M3 3L9 9M9 3L3 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function ClaimCard({ claim }: { claim: VerificationResult }) {
  const confidencePct = Math.round(claim.confidence * 100);

  return (
    <div className={`claim ${claim.supported ? 'claim--supported' : 'claim--unsupported'}`}>
      <div className="claim__top">
        <span className="claim__text">{claim.claim}</span>
        <span className="badge">
          {claim.supported ? <CheckIcon /> : <CrossIcon />}
          {claim.supported ? 'Supported' : 'Unsupported'}
        </span>
      </div>
      <div className="claim__confidence-row">
        <div className="confidence-bar">
          <div className="confidence-bar__fill" style={{ width: `${confidencePct}%` }} />
        </div>
        <span className="claim__confidence-label">{confidencePct}%</span>
      </div>
      {claim.explanation && <div className="claim__explanation">{claim.explanation}</div>}
    </div>
  );
}
