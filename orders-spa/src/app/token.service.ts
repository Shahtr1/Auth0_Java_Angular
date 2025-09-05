import { Injectable } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { defer, from, Observable } from 'rxjs';
import { finalize, shareReplay } from 'rxjs/operators';

type TokenOpts = {
  audience?: string;
  scope?: string; // space-delimited
};

@Injectable({ providedIn: 'root' })
export class TokenService {
  private inflight = new Map<string, Observable<string>>();

  constructor(private auth: AuthService) {}

  /** Returns one shared refresh per key (audience+scope). */
  getToken$(opts: TokenOpts = {}): Observable<string> {
    const key = `${opts.audience ?? 'default'}|${opts.scope ?? 'default'}`;

    // If a refresh is already running for this key, return it.
    const existing = this.inflight.get(key);
    if (existing) return existing;

    // Otherwise, start a single refresh and share it.
    const obs = defer(() =>
      from(
        this.auth.getAccessTokenSilently(
          opts.audience || opts.scope
            ? { authorizationParams: { audience: opts.audience, scope: opts.scope } }
            : undefined, // uses defaults from provideAuth0(...)
        ),
      ),
    ).pipe(
      shareReplay(1), // everyone gets the same result
      finalize(() => this.inflight.delete(key)), // cleanup no matter what
    );

    this.inflight.set(key, obs);
    return obs;
  }
}
