export interface VerificationResult {
  claim: string;
  supported: boolean;
  confidence: number;
  explanation: string | null;
}

export interface StreamMeta {
  query: string;
  response: string;
  sourceContext: string[];
}

export interface DocumentUploadResult {
  id: string;
  text: string;
}
