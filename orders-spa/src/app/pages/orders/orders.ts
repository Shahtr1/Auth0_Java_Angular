import { Component, inject, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { HttpClient } from '@angular/common/http';
import { AsyncPipe, JsonPipe } from '@angular/common';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [AsyncPipe, JsonPipe],
  templateUrl: './orders.html',
})
export class Orders {
  private http = inject(HttpClient);
  auth = inject(AuthService);

  data = signal<any>(null);
  who = signal<any>(null);
  error = signal<string | null>(null);

  login() {
    this.auth.loginWithRedirect();
  }
  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin } });
  }

  load() {
    this.error.set(null);
    this.http.get('/api/orders').subscribe({
      next: (d) => this.data.set(d),
      error: (e) => this.error.set(this.explain(e)),
    });
  }

  create() {
    this.error.set(null);
    this.http.post('/api/orders', { item: 'Coffee' }).subscribe({
      next: (d) => this.data.set(d),
      error: (e) => this.error.set(this.explain(e)),
    });
  }

  whoami() {
    this.error.set(null);
    this.http.get('/api/whoami').subscribe({
      next: (d) => this.who.set(d),
      error: (e) => this.error.set(this.explain(e)),
    });
  }

  private explain(e: any): string {
    const status = e?.status;
    if (status === 401) return '401 Unauthorized: missing/invalid token (check audience & issuer).';
    if (status === 403) return '403 Forbidden: token valid but lacks required permission.';
    return `Error ${status ?? ''}: ${e?.message ?? 'Unknown error'}`;
  }
}
