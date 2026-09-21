import { HttpErrorResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { ApiError, isApiError } from "../models/api.models";

@Injectable({ providedIn: "root" })
export class ApiErrorService {
  from(error: unknown): ApiError {
    if (error instanceof HttpErrorResponse && isApiError(error.error)) {
      return { ...error.error, fieldErrors: error.error.fieldErrors ?? {} };
    }
    const status = error instanceof HttpErrorResponse ? error.status : 500;
    return {
      timestamp: new Date().toISOString(),
      status,
      code: this.code(status),
      message: this.safeMessage(status),
      path: "",
      fieldErrors: {},
    };
  }

  applyFieldErrors(form: FormGroup, error: ApiError): void {
    Object.entries(error.fieldErrors).forEach(([field, message]) => {
      const control: AbstractControl | null = form.get(field);
      if (control) {
        control.setErrors({ ...control.errors, server: message });
      }
    });
  }

  private code(status: number): string {
    return (
      (
        {
          400: "VALIDATION_ERROR",
          401: "UNAUTHORIZED",
          403: "ACCESS_DENIED",
          404: "NOT_FOUND",
          409: "CONFLICT",
          422: "BUSINESS_RULE_VIOLATION",
        } as Record<number, string>
      )[status] ?? "INTERNAL_ERROR"
    );
  }

  private safeMessage(status: number): string {
    return (
      (
        {
          400: "Please correct the highlighted fields.",
          401: "Your session is no longer valid. Please sign in again.",
          403: "You do not have permission to perform this action.",
          404: "The requested information could not be found.",
          409: "This change conflicts with existing information.",
          422: "The requested change is not allowed in the current state.",
          500: "CivicOps could not complete the request. Please try again.",
        } as Record<number, string>
      )[status] ?? "CivicOps could not complete the request."
    );
  }
}
