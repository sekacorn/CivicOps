import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { OrganizationContextService } from "./organization-context.service";

const organizations = [
  { id: "org-1", name: "Northside", active: true },
  { id: "org-2", name: "Riverside", active: true },
];
const memberships = [
  {
    id: "m-1",
    organizationId: "org-1",
    userId: "user-1",
    role: "GRANT_MANAGER",
    active: true,
  },
  {
    id: "m-2",
    organizationId: "org-2",
    userId: "user-1",
    role: "VIEWER",
    active: true,
  },
];

describe("OrganizationContextService", () => {
  let service: OrganizationContextService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrganizationContextService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("loads organizations, memberships, and selects the first valid organization", async () => {
    const result = firstValueFrom(service.load());
    http.expectOne("/api/v1/organizations").flush(organizations);
    http.expectOne("/api/v1/auth/me/memberships").flush(memberships);
    await result;
    expect(service.selectedOrganizationId()).toBe("org-1");
    expect(service.selectedRole()).toBe("GRANT_MANAGER");
  });

  it("restores a persisted organization when membership is still valid", async () => {
    localStorage.setItem("civicops.selectedOrganizationId", "org-2");
    const result = firstValueFrom(service.load());
    http.expectOne("/api/v1/organizations").flush(organizations);
    http.expectOne("/api/v1/auth/me/memberships").flush(memberships);
    await result;
    expect(service.selectedOrganizationId()).toBe("org-2");
    expect(service.selectedRole()).toBe("VIEWER");
  });

  it("removes a stale persisted organization", async () => {
    localStorage.setItem("civicops.selectedOrganizationId", "stale-org");
    const result = firstValueFrom(service.load());
    http.expectOne("/api/v1/organizations").flush(organizations);
    http.expectOne("/api/v1/auth/me/memberships").flush(memberships);
    await result;
    expect(service.selectedOrganizationId()).toBe("org-1");
    expect(localStorage.getItem("civicops.selectedOrganizationId")).toBe(
      "org-1",
    );
  });

  it("switches organizations and resolves roles without granting viewer management", async () => {
    const result = firstValueFrom(service.load());
    http.expectOne("/api/v1/organizations").flush(organizations);
    http.expectOne("/api/v1/auth/me/memberships").flush(memberships);
    await result;
    service.select("org-2");
    expect(service.selectedRole()).toBe("VIEWER");
    expect(service.hasAnyRole("GRANT_MANAGER")).toBe(false);
  });
});
