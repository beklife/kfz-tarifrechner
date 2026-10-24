import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Vehicle } from './api.models';

@Injectable({ providedIn: 'root' })
export class VehicleApi {
  private readonly http = inject(HttpClient);

  // Relative URL: Im Dev-Modus leitet der Angular-Proxy weiter, in Docker/OpenShift nginx.
  getVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>('/api/vehicles');
  }
}
