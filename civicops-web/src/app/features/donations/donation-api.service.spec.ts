import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { DonationApiService } from "./donation-api.service";

describe("DonationApiService", () => {
  let api: DonationApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(DonationApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("lists privacy-safe donor summaries", () => {
    api.donors("org", 2).subscribe();
    const r = http.expectOne(
      (x) => x.url === "/api/v1/organizations/org/donors",
    );
    expect(r.request.params.get("page")).toBe("2");
    expect(r.request.method).toBe("GET");
  });
  it("creates donor records using the detail endpoint contract", () => {
    api
      .createDonor("org", {
        donorType: "INDIVIDUAL",
        firstName: "A",
        lastName: "B",
        organizationName: null,
        email: null,
        phone: null,
        addressLine1: null,
        addressLine2: null,
        city: null,
        state: null,
        postalCode: null,
        country: null,
        anonymous: false,
        communicationOptOut: false,
        notes: null,
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/donors").request.method,
    ).toBe("POST");
  });
  it("uses explicit campaign lifecycle endpoints", () => {
    api.transitionCampaign("org", "campaign", "activate").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/donation-campaigns/campaign/activate",
      ).request.method,
    ).toBe("POST");
  });
  it("records immutable anonymous donations without a fabricated donor", () => {
    api
      .createDonation("org", {
        donorId: null,
        anonymous: true,
        amount: 25,
        donationDate: "2026-09-12",
        paymentMethod: "CASH",
        inKindDescription: null,
        campaignId: null,
        restricted: false,
        restrictionDescription: null,
        designation: null,
        referenceNumber: null,
        receiptNumber: null,
        acknowledgementStatus: "PENDING",
        notes: null,
      })
      .subscribe();
    const r = http.expectOne("/api/v1/organizations/org/donations");
    expect(r.request.body.donorId).toBeNull();
    expect(r.request.body.anonymous).toBe(true);
  });
  it("reverses rather than editing or deleting financial history", () => {
    api.reverse("org", "gift", "Duplicate entry").subscribe();
    const r = http.expectOne(
      "/api/v1/organizations/org/donations/gift/reverse",
    );
    expect(r.request.method).toBe("POST");
    expect(r.request.body.reason).toBe("Duplicate entry");
  });
  it("reads backend-authoritative summary and campaign financials", () => {
    api.summary("org", "2026-01-01", "2026-12-31").subscribe();
    const s = http.expectOne((x) =>
      x.url.endsWith("/donation-reports/summary"),
    );
    expect(s.request.params.get("from")).toBe("2026-01-01");
    api.campaignFinancial("org", "campaign").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/donation-campaigns/campaign/financial-summary",
      ).request.method,
    ).toBe("GET");
  });
});
