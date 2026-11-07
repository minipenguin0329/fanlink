import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator, Alert, BackHandler, Image, KeyboardAvoidingView, Platform,
  Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as SecureStore from 'expo-secure-store';

type Provider = 'kakao' | 'naver';
const sessionKey = 'fanlink.demo.session.v1';
const demoCode = '123456';
const brandMark = require('./assets/mark.png');
const providerNames = { kakao: '카카오', naver: '네이버' };

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

export default function AuthGate({ children }: { children: (logout: () => void) => React.ReactNode }) {
  const [session, setSession] = useState<Provider | null | undefined>(undefined);
  const [provider, setProvider] = useState<Provider | null>(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [bootError, setBootError] = useState(false);
  const [bootAttempt, setBootAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const pending = useRef(false);

  useEffect(() => {
    let active = true;
    SecureStore.getItemAsync(sessionKey).then(
      value => { if (active) setSession(value === 'kakao' || value === 'naver' ? value : null); },
      () => { if (active) setBootError(true); },
    );
    return () => { active = false; };
  }, [bootAttempt]);

  function cancelVerification() {
    if (pending.current) return;
    setProvider(null);
    setCode('');
    setError('');
  }

  useEffect(() => {
    if (!provider || session) return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      cancelVerification();
      return true;
    });
    return () => subscription.remove();
  }, [provider, session]);

  async function verify() {
    if (!provider || pending.current) return;
    if (code !== demoCode) {
      setError(code.length === 6 ? '인증번호가 올바르지 않아요. 다시 입력해 주세요.' : '인증번호 6자리를 입력해 주세요.');
      return;
    }
    pending.current = true;
    setSaving(true);
    setError('');
    try {
      await SecureStore.setItemAsync(sessionKey, provider);
      setSession(provider);
      setProvider(null);
      setCode('');
    } catch {
      setError('로그인 상태를 저장하지 못했어요. 다시 시도해 주세요.');
    } finally {
      pending.current = false;
      setSaving(false);
    }
  }

  async function logout() {
    if (pending.current) return;
    pending.current = true;
    try {
      await SecureStore.deleteItemAsync(sessionKey);
      setSession(null);
    } catch {
      Alert.alert('로그아웃 실패', '다시 시도해 주세요.');
    } finally {
      pending.current = false;
    }
  }

  if (session) return children(logout);

  if (session === undefined) {
    return (
      <SafeAreaView style={[styles.screen, styles.start]}>
        <Image source={brandMark} style={styles.logo} resizeMode="contain" />
        <Text style={styles.brand}>FANLINK</Text>
        {bootError ? (
          <View style={styles.startStatus}>
            <Text accessibilityRole="alert" style={styles.error}>로그인 상태를 불러오지 못했어요.</Text>
            <Action label="다시 시도" onPress={() => {
              setBootError(false);
              setBootAttempt(attempt => attempt + 1);
            }} />
          </View>
        ) : (
          <View style={styles.startStatus}>
            <ActivityIndicator color="#075cf1" />
            <Text style={styles.hint}>로그인 상태 확인 중</Text>
          </View>
        )}
      </SafeAreaView>
    );
  }

  if (provider) {
    return (
      <SafeAreaView style={styles.screen}>
        <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
          <ScrollView contentContainerStyle={styles.verification} keyboardShouldPersistTaps="handled">
            <Pressable accessibilityRole="button" disabled={saving} onPress={cancelVerification}>
              <Text style={styles.link}>‹ 로그인으로 돌아가기</Text>
            </Pressable>
            <Text accessibilityRole="header" style={styles.title}>실명 인증</Text>
            <Text style={styles.hint}>{providerNames[provider]} 데모 로그인을 선택했어요.</Text>
            <Text style={styles.hint}>테스트 인증번호 123456을 입력해 주세요.</Text>
            <TextInput accessibilityLabel="테스트 인증번호" value={code} editable={!saving}
              onChangeText={value => { setCode(value.replace(/\D/g, '')); setError(''); }}
              keyboardType="number-pad" maxLength={6} placeholder="인증번호 6자리"
              returnKeyType="done" onSubmitEditing={verify} style={styles.input} />
            {!!error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
            <Action label={saving ? '인증 확인 중…' : '인증 확인'} disabled={saving} onPress={verify} />
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <Image source={require('./assets/login-top.png')} style={styles.loginTop} resizeMode="contain" />
      <Image source={require('./assets/login-bottom.png')} style={styles.loginBottom} resizeMode="stretch" />
      <ScrollView contentContainerStyle={styles.loginContent} showsVerticalScrollIndicator={false}>
        <Image source={brandMark} style={styles.logo} resizeMode="contain" />
        <Text style={styles.brand}>FANLINK</Text>
        <Text style={styles.loginCopy}>티켓 예매부터 팬 활동까지,{'\n'}FANLINK에서 한 번에</Text>
        <View style={styles.loginActions}>
          <Action label="●   카카오로 로그인" color="#ffe500" textColor="#151009" onPress={() => setProvider('kakao')} />
          <Action label="N   네이버로 로그인" color="#04c442" onPress={() => setProvider('naver')} />
          <Text style={styles.demoNote}>데모 모드 · 실제 계정 정보가 필요 없어요.</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f8f9fc' },
  flex: { flex: 1 },
  start: { justifyContent: 'center', alignItems: 'center', padding: 24 },
  startStatus: { alignItems: 'center', gap: 10, marginTop: 22 },
  logo: { width: 110, height: 82 },
  brand: { color: '#0b1737', fontSize: 44, fontWeight: '900', letterSpacing: -0.8, textAlign: 'center' },
  loginTop: { width: '48%', height: '21%', position: 'absolute', top: 0, left: 0 },
  loginBottom: { width: '100%', height: '32%', position: 'absolute', bottom: 0 },
  loginContent: { flexGrow: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 28, paddingTop: 16, paddingBottom: 64 },
  loginCopy: { marginTop: 8, textAlign: 'center', lineHeight: 21, color: '#535b6d', fontSize: 14 },
  loginActions: { marginTop: 53, width: '100%', gap: 10 },
  demoNote: { color: '#535b6d', fontSize: 12, textAlign: 'center' },
  verification: { padding: 24, gap: 12 },
  title: { color: '#101623', fontSize: 28, fontWeight: '900', marginTop: 18 },
  hint: { color: '#535b6d', fontSize: 14, lineHeight: 21 },
  link: { color: '#075cf1', fontWeight: '700', paddingVertical: 8 },
  error: { color: '#ad2336', fontSize: 14, lineHeight: 20 },
  input: { minHeight: 48, backgroundColor: '#fff', borderColor: '#b9c8dd', borderWidth: 1, borderRadius: 11, paddingHorizontal: 14, color: '#101623', fontSize: 17 },
  button: { minHeight: 39, paddingVertical: 9, paddingHorizontal: 16, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: 13, fontWeight: '800', textAlign: 'center' },
});
