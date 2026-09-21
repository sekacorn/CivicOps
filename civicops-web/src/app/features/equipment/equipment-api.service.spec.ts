import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { EquipmentApiService } from "./equipment-api.service";

describe("EquipmentApiService", () => {
  let api: EquipmentApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(EquipmentApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("lists inventory with backend filters", () => {
    api
      .assets("org", {
        status: "AVAILABLE",
        condition: "GOOD",
        categoryId: "cat",
        page: 1,
      })
      .subscribe();
    const r = http.expectOne((x) =>
      x.url.endsWith("/organizations/org/equipment"),
    );
    expect(r.request.params.get("status")).toBe("AVAILABLE");
    expect(r.request.params.get("sort")).toBe("assetTag,asc");
  });
  it("uses backend-authoritative checkout and check-in", () => {
    api.checkout("org", "asset", { borrowerName: "Pat" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/equipment/asset/checkouts")
        .request.method,
    ).toBe("POST");
    api.checkIn("org", "checkout", "DAMAGED", "Broken latch").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/equipment-checkouts/checkout/check-in",
      ).request.body.returnCondition,
    ).toBe("DAMAGED");
  });
  it("uses explicit lost and retirement operations", () => {
    api.markLost("org", "checkout").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/equipment-checkouts/checkout/mark-lost",
      ).request.method,
    ).toBe("POST");
    api.retire("org", "asset").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/equipment/asset/retire").request
        .method,
    ).toBe("POST");
  });
  it("uses maintenance lifecycle endpoints", () => {
    api
      .createMaintenance("org", "asset", { maintenanceType: "REPAIR" })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/equipment/asset/maintenance")
        .request.method,
    ).toBe("POST");
    api.completeMaintenance("org", "maintenance", "GOOD").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/equipment-maintenance/maintenance/complete",
      ).request.body.resultingCondition,
    ).toBe("GOOD");
  });
  it("loads authoritative inventory, utilization, overdue, and maintenance reports", () => {
    api.inventoryReport("org").subscribe();
    http.expectOne("/api/v1/organizations/org/equipment-reports/summary");
    api.utilizationReport("org").subscribe();
    http.expectOne("/api/v1/organizations/org/equipment-reports/utilization");
    api.maintenanceReport("org").subscribe();
    http.expectOne("/api/v1/organizations/org/equipment-reports/maintenance");
  });
});
