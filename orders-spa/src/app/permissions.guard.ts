import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { jwtDecode } from 'jwt-decode';
import { from, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

type JwtPayload = { permissions?: string[]; scope?: string };

export const permissionsGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const required = (route.data?.['permissions'] as string[]) ?? [];
  const auth = inject(AuthService);
  const router = inject(Router);

  // Rely on AuthGuard to ensure the user is authenticated.
  // Here we only fetch the token and check claims.
  return from(auth.getAccessTokenSilently({ cacheMode: 'off' })).pipe(
    map((token) => {
      const payload = jwtDecode<JwtPayload>(token || '');
      const perms = payload.permissions ?? [];
      const scopes = (payload.scope ?? '').split(' ').filter(Boolean);
      const hasAll = required.every((r) => perms.includes(r) || scopes.includes(r));
      return hasAll ? true : router.parseUrl('/forbidden'); // return UrlTree (no loops)
    }),
    // If we can’t get a token (e.g., session glitch), fail closed -> Forbidden page
    catchError(() => of(inject(Router).parseUrl('/forbidden'))),
  );
};
