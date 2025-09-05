import { inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { firstValueFrom, of, from } from 'rxjs';
import { catchError, switchMap, take } from 'rxjs/operators';
import { TokenService } from '../token.service';

export function warmTokensInitializer() {
  const auth = inject(AuthService);
  const tokens = inject(TokenService);
  return () =>
    firstValueFrom(
      auth.isAuthenticated$.pipe(
        take(1),
        switchMap((isAuth) => (isAuth ? tokens.getToken$() : of(undefined))),
        catchError(() => of(undefined)), // never block boot
      ),
    );
}
