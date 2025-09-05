import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, EMPTY, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err) => {
      if (err?.status === 401) {
        // Not logged in or session expired → send to login and return to the same URL
        auth.loginWithRedirect({ appState: { target: router.url } });
        return EMPTY; // we handled it
      }
      if (err?.status === 403) {
        // Logged in but lacking permission → route to a “Forbidden” page (optional)
        router.navigateByUrl('/forbidden');
      }
      return throwError(() => err);
    }),
  );
};
