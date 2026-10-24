// TypeScript-Typen für die JSON-Daten der beiden Backend-Services.

export interface Vehicle {
  id: number;
  manufacturer: string;
  model: string;
  riskClass: number;
}

export type Coverage = 'HAFTPFLICHT' | 'TEILKASKO' | 'VOLLKASKO';

export interface QuoteRequest {
  vehicleId: number;
  driverAge: number;
  accidentFreeYears: number;
  coverage: Coverage;
}

export interface BreakdownStep {
  step: number;
  description: string;
  factorOrAmount: number;
  subtotal: number;
}

export interface QuoteResponse {
  vehicleName: string;
  coverage: Coverage;
  yearlyPrice: number;
  monthlyPrice: number;
  breakdown: BreakdownStep[];
}

/** Fehlerantwort nach RFC 9457. "errors" ist unsere Erweiterung für Feldfehler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
}
