import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from '../app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authHttpInterceptorFn, AuthService, provideAuth0 } from '@auth0/auth0-angular';
import { authConfig } from './auth.config';
import { apiErrorInterceptor } from '../api-error.interceptor';
import { warmTokensInitializer } from './token-warmup';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    // 1) Register Auth0’s interceptor so HttpClient can attach tokens
    provideHttpClient(withInterceptors([authHttpInterceptorFn, apiErrorInterceptor])),

    // 2) Initialize Auth0 for this SPA
    provideAuth0({
      domain: authConfig.domain,
      clientId: authConfig.clientId,

      // What we ask the Authorization Server for at login time
      authorizationParams: {
        redirect_uri: window.location.origin,
        audience: authConfig.audience,
        scope: authConfig.scope,
      },

      // Use Refresh Token Rotation for smooth long-lived sessions in a SPA
      useRefreshTokens: true,

      // Only attach tokens to requests we specify
      httpInterceptor: {
        allowedList: [
          {
            // any call we make to our proxied backend
            uri: '/api/*',
            tokenOptions: {
              authorizationParams: {
                audience: authConfig.audience,
                scope: 'read:orders write:orders',
              },
            },
          },
        ],
      },
    }),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: warmTokensInitializer,
    },
  ],
};
