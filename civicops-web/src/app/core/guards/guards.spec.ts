import { TestBed } from "@angular/core/testing";
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from "@angular/router";
import { of } from "rxjs";
import { AuthService } from "../auth/auth.service";
import { OrganizationContextService } from "../organization/organization-context.service";
import { authGuard } from "./auth.guard";
import { organizationGuard } from "./organization.guard";

describe("route guards", () => {
  it("authGuard redirects an unauthenticated user to login", () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            bootstrapComplete: () => true,
            authenticated: () => false,
          },
        },
      ],
    });
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      "/login",
    );
  });

  it("authGuard permits an authenticated user after bootstrap", () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            bootstrapComplete: () => true,
            authenticated: () => true,
          },
        },
      ],
    });
    expect(
      TestBed.runInInjectionContext(() =>
        authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      ),
    ).toBe(true);
  });

  it("organizationGuard selects a valid route organization", () => {
    const context = {
      organizations: () => [{ id: "org-1" }],
      contains: (id: string) => id === "org-1",
      select: vi.fn(),
      load: () => of([]),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OrganizationContextService, useValue: context },
      ],
    });
    const route = {
      paramMap: { get: () => "org-1" },
    } as unknown as ActivatedRouteSnapshot;
    expect(
      TestBed.runInInjectionContext(() =>
        organizationGuard(route, {} as RouterStateSnapshot),
      ),
    ).toBe(true);
    expect(context.select).toHaveBeenCalledWith("org-1");
  });
});
