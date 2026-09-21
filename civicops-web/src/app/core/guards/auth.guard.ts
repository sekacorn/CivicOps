import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { map } from "rxjs";
import { AuthService } from "../auth/auth.service";

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.bootstrapComplete()) {
    return auth.authenticated() || router.createUrlTree(["/login"]);
  }
  return auth
    .bootstrap()
    .pipe(
      map((authenticated) => authenticated || router.createUrlTree(["/login"])),
    );
};
