import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const KEY = 'fanlink.backend.jwt.v1';

export async function saveBackendJwt(jwt: string): Promise<void> {
  // Shape check is for accidental input mistakes only – server validates signature.
  if (!/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(jwt)) {
    throw new Error('유효한 JWT 문자열이 아닙니다.');
  }
  if (Platform.OS === 'web') window.sessionStorage.setItem(KEY, jwt);
  else await SecureStore.setItemAsync(KEY, jwt);
}

export async function readBackendJwt(): Promise<string | null> {
  return Platform.OS === 'web' ? window.sessionStorage.getItem(KEY) : SecureStore.getItemAsync(KEY);
}

export async function requireBackendJwt(): Promise<string> {
  const jwt = await readBackendJwt();
  if (!jwt) throw new Error('로그인이 필요합니다.');
  return jwt;
}

export async function clearBackendJwt(): Promise<void> {
  if (Platform.OS === 'web') window.sessionStorage.removeItem(KEY);
  else await SecureStore.deleteItemAsync(KEY);
}
