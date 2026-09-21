import { HttpClient } from "@angular/common/http";
import { computed, inject, Injectable, signal } from "@angular/core";
import { Router } from "@angular/router";
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  switchMap,
  tap,
} from "rxjs";
import { API_BASE_URL } from "../http/api-url";
import {
  CurrentUser,
  LoginRequest,
  TokenResponse,
} from "../models/auth.models";
import { OrganizationContextService } from "../organization/organization-context.service";
import { TokenStorageService } from "./token-storage.service";

@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly tokens = inject(TokenStorageService);
  private readonly router = inject(Router);
  private readonly organizationContext = inject(OrganizationContextService);
  private readonly currentUserState = signal<CurrentUser | null>(null);
  private readonly bootstrapCompleteState = signal(false);

  readonly currentUser = this.currentUserState.asReadonly();
  readonly authenticated = computed(() => this.currentUserState() !== null);
  readonly bootstrapComplete = this.bootstrapCompleteState.asReadonly();

  login(request: LoginRequest): Observable<CurrentUser> {
    return this.http
      .post<TokenResponse>(`${this.apiBaseUrl}/auth/login`, request)
      .pipe(
        tap((response) => this.tokens.save(response)),
        switchMap(() => this.loadCurrentUser()),
        switchMap((user) =>
          this.organizationContext.load(true).pipe(map(() => user)),
        ),
        tap(() => this.bootstrapCompleteState.set(true)),
        catchError((error) => {
          this.clearSession();
          throw error;
        }),
      );
  }

  refreshSession(): Observable<string> {
    const refreshToken = this.tokens.refreshToken;
    if (!refreshToken) {
      throw new Error("No refresh token is available");
    }
    return this.http
      .post<TokenResponse>(`${this.apiBaseUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap((response) => this.tokens.save(response)),
        map((response) => response.accessToken),
      );
  }

  bootstrap(): Observable<boolean> {
    if (!this.tokens.accessToken || !this.tokens.refreshToken) {
      this.clearSession();
      this.bootstrapCompleteState.set(true);
      return of(false);
    }
    return this.loadCurrentUser().pipe(
      switchMap(() => this.organizationContext.load()),
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
      finalize(() => this.bootstrapCompleteState.set(true)),
    );
  }

  loadCurrentUser(): Observable<CurrentUser> {
    return this.http
      .get<CurrentUser>(`${this.apiBaseUrl}/auth/me`)
      .pipe(tap((user) => this.currentUserState.set(user)));
  }

  logout(): Observable<void> {
    const refreshToken = this.tokens.refreshToken;
    const request = refreshToken
      ? this.http.post<void>(`${this.apiBaseUrl}/auth/logout`, { refreshToken })
      : of(undefined);
    return request.pipe(
      catchError(() => of(undefined)),
      tap(() => this.clearSession()),
      tap(() => void this.router.navigateByUrl("/login")),
    );
  }

  handleAuthenticationFailure(): void {
    this.clearSession();
    void this.router.navigateByUrl("/login");
  }

  clearSession(): void {
    this.tokens.clear();
    this.currentUserState.set(null);
    this.bootstrapCompleteState.set(false);
    this.organizationContext.clear();
  }
}
