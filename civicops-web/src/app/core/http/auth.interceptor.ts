import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from "@angular/common/http";
import { inject } from "@angular/core";
import {
  catchError,
  finalize,
  Observable,
  shareReplay,
  switchMap,
  throwError,
} from "rxjs";
import { AuthService } from "../auth/auth.service";
import { TokenStorageService } from "../auth/token-storage.service";
import { API_BASE_URL } from "./api-url";

let refreshInFlight: Observable<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const baseUrl = inject(API_BASE_URL);
  const tokens = inject(TokenStorageService);
  const auth = inject(AuthService);
  const civicOpsRequest = request.url.startsWith(baseUrl);
  const authEndpoint = /\/auth\/(login|refresh|logout)$/.test(request.url);

  if (!civicOpsRequest || authEndpoint) {
    return next(request);
  }

  const accessToken = tokens.accessToken;
  const authenticatedRequest = accessToken
    ? bearer(request, accessToken)
    : request;
  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (
        !(error instanceof HttpErrorResponse) ||
        error.status !== 401 ||
        !tokens.refreshToken
      ) {
        return throwError(() => error);
      }
      if (!refreshInFlight) {
        refreshInFlight = auth.refreshSession().pipe(
          catchError((refreshError) => {
            auth.handleAuthenticationFailure();
            return throwError(() => refreshError);
          }),
          finalize(() => (refreshInFlight = null)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
      }
      return refreshInFlight.pipe(
        switchMap((token) => next(bearer(request, token))),
      );
    }),
  );
};

function bearer(
  request: HttpRequest<unknown>,
  token: string,
): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
