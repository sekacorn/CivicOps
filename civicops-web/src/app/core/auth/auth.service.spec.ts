import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { provideRouter, Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { authInterceptor } from "../http/auth.interceptor";
import { CurrentUser, TokenResponse } from "../models/auth.models";
import { AuthService } from "./auth.service";
import { TokenStorageService } from "./token-storage.service";

const user: CurrentUser = {
  id: "user-1",
  firstName: "Avery",
  lastName: "Morgan",
  email: "avery@example.org",
  active: true,
  emailVerified: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};
const tokens: TokenResponse = {
  accessToken: "access-1",
  refreshToken: "refresh-1",
  tokenType: "Bearer",
  expiresIn: 900,
};

describe("AuthService", () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("logs in, stores tokens, and loads the current user", async () => {
    const result = firstValueFrom(
      service.login({ email: user.email, password: "correct-password" }),
    );
    http.expectOne("/api/v1/auth/login").flush(tokens);
    http.expectOne("/api/v1/auth/me").flush(user);
    http.expectOne("/api/v1/organizations").flush([]);
    http.expectOne("/api/v1/auth/me/memberships").flush([]);
    expect(await result).toEqual(user);
    expect(TestBed.inject(TokenStorageService).accessToken).toBe("access-1");
    expect(service.authenticated()).toBe(true);
  });

  it("clears session after failed login", async () => {
    localStorage.setItem("civicops.accessToken", "stale");
    const result = firstValueFrom(
      service.login({ email: user.email, password: "wrong" }),
    );
    http
      .expectOne("/api/v1/auth/login")
      .flush(
        { message: "Invalid credentials" },
        { status: 401, statusText: "Unauthorized" },
      );
    await expect(result).rejects.toBeDefined();
    expect(TestBed.inject(TokenStorageService).accessToken).toBeNull();
  });

  it("bootstraps the session and organization context from existing tokens", async () => {
    TestBed.inject(TokenStorageService).save(tokens);
    const result = firstValueFrom(service.bootstrap());
    http.expectOne("/api/v1/auth/me").flush(user);
    http
      .expectOne("/api/v1/organizations")
      .flush([{ id: "org-1", name: "Civic Group", active: true }]);
    http.expectOne("/api/v1/auth/me/memberships").flush([
      {
        id: "m-1",
        organizationId: "org-1",
        userId: user.id,
        role: "GRANT_MANAGER",
        active: true,
      },
    ]);
    expect(await result).toBe(true);
    expect(service.bootstrapComplete()).toBe(true);
  });

  it("calls backend logout before clearing local state and redirecting", async () => {
    TestBed.inject(TokenStorageService).save(tokens);
    const navigation = vi
      .spyOn(TestBed.inject(Router), "navigateByUrl")
      .mockResolvedValue(true);
    const result = firstValueFrom(service.logout());
    const request = http.expectOne("/api/v1/auth/logout");
    expect(request.request.body).toEqual({ refreshToken: "refresh-1" });
    request.flush(null);
    await result;
    expect(TestBed.inject(TokenStorageService).accessToken).toBeNull();
    expect(navigation).toHaveBeenCalledWith("/login");
  });
});

describe("authInterceptor", () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("attaches Bearer token only to CivicOps API requests", () => {
    const storage = TestBed.inject(TokenStorageService);
    storage.save(tokens);
    const api = TestBed.inject(HttpClient);
    api.get("/api/v1/organizations").subscribe();
    expect(
      http
        .expectOne("/api/v1/organizations")
        .request.headers.get("Authorization"),
    ).toBe("Bearer access-1");
    api.get("https://example.com/data").subscribe();
    expect(
      http
        .expectOne("https://example.com/data")
        .request.headers.has("Authorization"),
    ).toBe(false);
  });

  it("uses one refresh for simultaneous 401 responses and retries both requests", () => {
    TestBed.inject(TokenStorageService).save(tokens);
    const api = TestBed.inject(HttpClient);
    api.get("/api/v1/a").subscribe();
    api.get("/api/v1/b").subscribe();
    http
      .expectOne("/api/v1/a")
      .flush(null, { status: 401, statusText: "Unauthorized" });
    http
      .expectOne("/api/v1/b")
      .flush(null, { status: 401, statusText: "Unauthorized" });
    const refresh = http.expectOne("/api/v1/auth/refresh");
    expect(refresh.request.body).toEqual({ refreshToken: "refresh-1" });
    refresh.flush({
      ...tokens,
      accessToken: "access-2",
      refreshToken: "refresh-2",
    });
    expect(
      http.expectOne("/api/v1/a").request.headers.get("Authorization"),
    ).toBe("Bearer access-2");
    expect(
      http.expectOne("/api/v1/b").request.headers.get("Authorization"),
    ).toBe("Bearer access-2");
  });

  it("clears the session when refresh fails", () => {
    TestBed.inject(TokenStorageService).save(tokens);
    vi.spyOn(TestBed.inject(Router), "navigateByUrl").mockResolvedValue(true);
    const api = TestBed.inject(HttpClient);
    api.get("/api/v1/a").subscribe({ error: () => undefined });
    http
      .expectOne("/api/v1/a")
      .flush(null, { status: 401, statusText: "Unauthorized" });
    http
      .expectOne("/api/v1/auth/refresh")
      .flush(null, { status: 401, statusText: "Unauthorized" });
    expect(TestBed.inject(TokenStorageService).accessToken).toBeNull();
  });
});
