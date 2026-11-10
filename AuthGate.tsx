import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Image, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as Crypto from 'expo-crypto';
import * as WebBrowser from 'expo-web-browser';
import { authApi, FanlinkApiError, getFanlinkApiUrl, userApi } from './integration/fanlinkApi';
import { clearBackendJwt, readBackendJwt, saveBackendJwt } from './integration/fanlinkSession';

type Provider = 'kakao' | 'naver';
const mobileCallback = 'fanlink://oauth/callback';
const webLoginStartedKey = 'fanlink.oauth.started';
const brandMark = require('./assets/mark.png');
type ActionProps = {
  label: string;
  onPress: () => void;
  color?: string;
  textColor?: string;
  disabled?: boolean;
};

function Action({ label, onPress, color = '#075cf1', textColor = '#fff', disabled }: ActionProps) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ disabled: !!disabled }}
      disabled={disabled} onPress={onPress}
      style={({ pressed }) => [styles.button, { backgroundColor: color, opacity: disabled || pressed ? 0.6 : 1 }]}>
      <Text style={[styles.buttonText, { color: textColor }]}>{label}</Text>
    </Pressable>
  );
}

function readWebCallback(): { token: string | null } | null {
  if (Platform.OS !== 'web' || window.location.pathname !== '/oauth/callback') return null;
  const token = new URLSearchParams(window.location.search).get('token');
  window.history.replaceState(null, '', '/');
  const started = window.sessionStorage.getItem(webLoginStartedKey);
  window.sessionStorage.removeItem(webLoginStartedKey);
  return { token: started ? token : null };
}

export default function AuthGate({ children }: { children: (logout: () => void) => React.ReactNode }) {
  const [session, setSession] = useState<'loading' | 'signedIn' | 'signedOut'>('loading');
  const [error, setError] = useState('');
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');
  const [testLoginPending, setTestLoginPending] = useState(false);
  const [provider, setProvider] = useState<Provider | null>(null);
  const callback = useRef<ReturnType<typeof readWebCallback> | undefined>(undefined);
  const pending = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    let active = true;

    async function restoreSession() {
      let message = '';
      try {
        if (callback.current === undefined) callback.current = readWebCallback();
        const result = callback.current;
        const token = result ? result.token : await readBackendJwt();
        if (token) {
          await userApi.me(token, controller.signal);
          if (!active) return;
          if (result) await saveBackendJwt(token);
          if (active) setSession('signedIn');
          return;
        }
        if (result) message = '로그인을 완료하지 못했어요. 다시 로그인해 주세요.';
      } catch (cause) {
        if (!active) return;
        if (cause instanceof FanlinkApiError && (cause.status === 401 || cause.status === 403)) {
          await clearBackendJwt().catch(() => {});
          message = '로그인이 만료됐어요. 다시 로그인해 주세요.';
        } else {
          message = '서버에서 로그인 상태를 확인하지 못했어요. 다시 로그인해 주세요.';
        }
      }
      if (active) {
        setError(message);
        setSession('signedOut');
      }
    }

    void restoreSession().finally(() => clearTimeout(timeout));
    return () => {
      active = false;
      clearTimeout(timeout);
      controller.abort();
    };
  }, []);

  async function signIn(selected: Provider) {
    if (pending.current) return;
    pending.current = true;
    setProvider(selected);
    setError('');
    try {
      let apiUrl: string;
      try {
        apiUrl = getFanlinkApiUrl();
      } catch {
        setError('로그인 서버 주소가 설정되지 않았어요.');
        return;
      }
      if (Platform.OS === 'web') {
        window.sessionStorage.setItem(webLoginStartedKey, '1');
        window.location.assign(apiUrl + '/oauth2/authorization/' + selected);
        return;
      }

      const bytes = await Crypto.getRandomBytesAsync(32);
      const verifier = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
      const challenge = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, verifier);
      const result = await WebBrowser.openAuthSessionAsync(
        apiUrl + '/api/auth/mobile/' + selected + '?challenge=' + challenge, mobileCallback,
      );
      if (result.type !== 'success') {
        setError('로그인이 완료되지 않았어요. 다시 시도해 주세요.');
        return;
      }
      const redirect = new URL(result.url);
      const code = redirect.searchParams.get('code');
      if (redirect.protocol !== 'fanlink:' || redirect.host !== 'oauth' ||
          redirect.pathname !== '/callback' || !code) {
        setError('로그인 결과를 확인하지 못했어요. 다시 시도해 주세요.');
        return;
      }
      const { token } = await authApi.exchangeMobileCode(code, verifier);
      await userApi.me(token);
      await saveBackendJwt(token);
      setSession('signedIn');
    } catch {
      setError('서버 로그인을 완료하지 못했어요. 다시 시도해 주세요.');
    } finally {
      pending.current = false;
      setProvider(null);
    }
  }

  async function signInWithTestAccount() {
    if (pending.current) return;
    const username = account.trim();
    if (!username || !password) {
      setError('아이디와 비밀번호를 입력해 주세요.');
      return;
    }

    pending.current = true;
    setTestLoginPending(true);
    setError('');
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const result = await authApi.testLogin(username, password, controller.signal);
      const token = result?.token ?? result?.accessToken;
      if (!token) {
        setError('서버 응답에 로그인 토큰이 없어요. 백엔드 설정을 확인해 주세요.');
        return;
      }
      await userApi.me(token, controller.signal);
      await saveBackendJwt(token);
      setAccount('');
      setPassword('');
      setSession('signedIn');
    } catch (cause) {
      if (cause instanceof FanlinkApiError && (cause.status === 401 || cause.status === 403)) {
        setError('서버가 테스트 로그인을 거절했어요. 계정 정보와 서버 설정을 확인해 주세요.');
      } else if (cause instanceof FanlinkApiError && cause.status === 404) {
        setError('서버에서 테스트 로그인 API를 찾을 수 없어요.');
      } else {
        setError('테스트 로그인을 완료하지 못했어요. 서버 연결을 확인하고 다시 시도해 주세요.');
      }
    } finally {
      clearTimeout(timeout);
      pending.current = false;
      setTestLoginPending(false);
    }
  }

  async function logout() {
    if (pending.current) return;
    pending.current = true;
    try {
      await clearBackendJwt();
      setError('');
      setSession('signedOut');
    } catch {
      Alert.alert('로그아웃 실패', '다시 시도해 주세요.');
    } finally {
      pending.current = false;
    }
  }

  if (session === 'signedIn') return children(logout);

  if (session === 'loading') {
    return (
      <SafeAreaView style={[styles.screen, styles.start]}>
        <Image source={brandMark} style={styles.logo} resizeMode="contain" />
        <Text style={styles.brand}>FANLINK</Text>
        <View style={styles.startStatus}>
          <ActivityIndicator color="#075cf1" />
          <Text style={styles.hint}>로그인 상태 확인 중</Text>
        </View>
      </SafeAreaView>
    );
  }

  const signingIn = testLoginPending || provider !== null;

  return (
    <SafeAreaView style={styles.screen}>
      <Image source={require('./assets/login-top.png')} style={styles.loginTop} resizeMode="contain" />
      <Image source={require('./assets/login-bottom.png')} style={styles.loginBottom} resizeMode="stretch" />
      <ScrollView contentContainerStyle={styles.loginContent} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <Image source={brandMark} style={styles.logo} resizeMode="contain" />
        <Text style={styles.brand}>FANLINK</Text>
        <Text style={styles.loginCopy}>티켓 예매부터 팬 활동까지,{'\n'}FANLINK에서 한 번에</Text>
        <View style={styles.loginActions}>
          <Text accessibilityRole="header" style={styles.testLoginTitle}>테스트 계정 로그인</Text>
          <TextInput accessibilityLabel="아이디" placeholder="아이디" placeholderTextColor="#535b6d"
            style={styles.loginInput} value={account} onChangeText={setAccount} editable={!signingIn}
            autoCapitalize="none" autoCorrect={false} autoComplete="username" />
          <TextInput accessibilityLabel="비밀번호" placeholder="비밀번호" placeholderTextColor="#535b6d"
            style={styles.loginInput} value={password} onChangeText={setPassword} editable={!signingIn}
            autoCapitalize="none" autoCorrect={false} autoComplete="current-password" secureTextEntry
            returnKeyType="go" onSubmitEditing={() => void signInWithTestAccount()} />
          <Action label={testLoginPending ? '로그인 중…' : '테스트 계정으로 로그인'}
            disabled={signingIn} onPress={() => void signInWithTestAccount()} />
          <Action label={provider === 'kakao' ? '카카오 로그인 중…' : '●   카카오로 로그인'}
            color="#ffe500" textColor="#151009" disabled={signingIn} onPress={() => void signIn('kakao')} />
          <Action label={provider === 'naver' ? '네이버 로그인 중…' : 'N   네이버로 로그인'}
            color="#04c442" disabled={signingIn} onPress={() => void signIn('naver')} />
          {!!error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f8f9fc' },
  start: { justifyContent: 'center', alignItems: 'center', padding: 24 },
  startStatus: { alignItems: 'center', gap: 10, marginTop: 22 },
  logo: { width: 110, height: 82 },
  brand: { color: '#0b1737', fontSize: 44, fontWeight: '900', letterSpacing: -0.8, textAlign: 'center' },
  loginTop: { width: '48%', height: '21%', position: 'absolute', top: 0, left: 0 },
  loginBottom: { width: '100%', height: '32%', position: 'absolute', bottom: 0 },
  loginContent: { flexGrow: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 28, paddingTop: 16, paddingBottom: 64 },
  loginCopy: { marginTop: 8, textAlign: 'center', lineHeight: 21, color: '#535b6d', fontSize: 14 },
  loginActions: { marginTop: 28, width: '100%', maxWidth: 400, gap: 10 },
  testLoginTitle: { color: '#0b1737', fontSize: 16, fontWeight: '800' },
  loginInput: { minHeight: 44, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 11,
    borderWidth: 1, borderColor: '#c8d2e2', backgroundColor: '#fff', color: '#0b1737', fontSize: 16 },
  hint: { color: '#535b6d', fontSize: 14, lineHeight: 21 },
  error: { color: '#ad2336', fontSize: 14, lineHeight: 20 },
  button: { minHeight: 39, paddingVertical: 9, paddingHorizontal: 16, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: 13, fontWeight: '800', textAlign: 'center' },
});
