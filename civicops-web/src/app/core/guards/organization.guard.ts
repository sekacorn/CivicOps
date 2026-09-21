import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { map } from "rxjs";
import { OrganizationContextService } from "../organization/organization-context.service";

export const organizationGuard: CanActivateFn = (route) => {
  const context = inject(OrganizationContextService);
  const router = inject(Router);
  const organizationId = route.paramMap.get("organizationId");
  const resolve = () => {
    if (!organizationId || !context.contains(organizationId)) {
      return router.createUrlTree(["/dashboard"]);
    }
    context.select(organizationId);
    return true;
  };
  return context.organizations().length
    ? resolve()
    : context.load().pipe(map(resolve));
};
