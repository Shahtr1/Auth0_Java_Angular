## Boot time: how your “providers” become a working HTTP pipeline

- You call `bootstrapApplication(AppComponent, { providers: [...] })` (the CLI did this in `main.ts`). Angular creates the root EnvironmentInjector (the DI container) and registers every provider you passed
- `provideHttpClient(...)` drops HttpClient and its plumbing (handlers/backends) into that injector. You can enhance it with “features”, like `withInterceptors(...)` (functional) or `withInterceptorsFromDi()` (class/DI-based)
- `withInterceptorsFromDi()` specifically says: “also pick up any interceptors registered under the multi-token `HTTP_INTERCEPTORS` from DI.” That token is an array of interceptors collected in the order you provide them.

So in our `app.config.ts`:

```ts
(provideHttpClient(withInterceptorsFromDi()), // turns on DI-provided interceptors
  { provide: HTTP_INTERCEPTORS, useClass: AuthHttpInterceptor, multi: true }); // puts Auth0’s interceptor into that array
```

That’s the entire switch from “plain HttpClient” → “HttpClient with an interceptor chain”.
