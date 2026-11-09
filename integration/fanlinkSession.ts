/** Stores the REAL server-issued access token, not the frontend demo login code. */
import * as SecureStore from 'expo-secure-store';

const KEY = 'fanlink.backend.jwt.v1';

export async function saveBackendJwt(jwt: string): Promise<void> {
  // Shape check is for accidental input mistakes only – server validates signature.
  if (!/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(jwt)) {
    throw new Error('유효한 JWT 문자열이 아닙니다.');
  }
  await SecureStore.setItemAsync(KEY, jwt);
}

export async function readBackendJwt(): Promise<string | null> {
  return SecureStore.getItemAsync(KEY);
}

export async function requireBackendJwt(): Promise<string> {
  const jwt = await readBackendJwt();
  if (!jwt) throw new Error('서버 로그인이 필요합니다. 데모 인증번호 123456은 서버 JWT가 아닙니다.');
  return jwt;
}

export async function clearBackendJwt(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY);
}
