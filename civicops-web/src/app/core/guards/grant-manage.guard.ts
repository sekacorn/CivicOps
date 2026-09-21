import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { OrganizationContextService } from "../organization/organization-context.service";

export const grantManageGuard: CanActivateFn = () => {
  const organizations = inject(OrganizationContextService);
  const router = inject(Router);
  return (
    organizations.hasAnyRole("GRANT_MANAGER") ||
    router.createUrlTree(["/dashboard"])
  );
};
