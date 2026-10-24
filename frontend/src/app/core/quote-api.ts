import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { QuoteRequest, QuoteResponse } from './api.models';

@Injectable({ providedIn: 'root' })
export class QuoteApi {
  private readonly http = inject(HttpClient);

  createQuote(request: QuoteRequest): Observable<QuoteResponse> {
    return this.http.post<QuoteResponse>('/api/quotes', request);
  }
}
