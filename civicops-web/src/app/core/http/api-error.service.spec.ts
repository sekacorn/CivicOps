import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FormControl, FormGroup } from "@angular/forms";
import { ApiErrorService } from "./api-error.service";

describe("ApiErrorService", () => {
  const cases = [
    [400, "VALIDATION_ERROR"],
    [403, "ACCESS_DENIED"],
    [404, "NOT_FOUND"],
    [409, "CONFLICT"],
    [422, "BUSINESS_RULE_VIOLATION"],
    [500, "INTERNAL_ERROR"],
  ] as const;
  let service: ApiErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ApiErrorService);
  });

  it.each(cases)("maps HTTP %s to a safe %s error", (status, code) => {
    const result = service.from(new HttpErrorResponse({ status }));
    expect(result.code).toBe(code);
    expect(result.message).not.toContain("stack");
  });

  it("preserves the backend ApiError and applies field errors to forms", () => {
    const backend = {
      timestamp: "2026-01-01T00:00:00Z",
      status: 400,
      code: "VALIDATION_ERROR",
      message: "Invalid request",
      path: "/grants",
      fieldErrors: { grantName: "Required" },
    };
    const result = service.from(
      new HttpErrorResponse({ status: 400, error: backend }),
    );
    const form = new FormGroup({ grantName: new FormControl("") });
    service.applyFieldErrors(form, result);
    expect(form.controls.grantName.errors?.["server"]).toBe("Required");
  });
});
