export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface CurrentUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  active: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}
