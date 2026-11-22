export interface UserResponse {
  id: number;
  username: string;
  email: string;
  balance: number;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
export interface RegisterRequest extends LoginRequest {
  username: string;
}
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
