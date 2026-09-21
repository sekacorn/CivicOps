import { Injectable } from "@angular/core";
import { TokenResponse } from "../models/auth.models";

const ACCESS_TOKEN = "civicops.accessToken";
const REFRESH_TOKEN = "civicops.refreshToken";

@Injectable({ providedIn: "root" })
export class TokenStorageService {
  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN);
  }

  save(tokens: TokenResponse): void {
    localStorage.setItem(ACCESS_TOKEN, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN, tokens.refreshToken);
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN);
    localStorage.removeItem(REFRESH_TOKEN);
  }
}
