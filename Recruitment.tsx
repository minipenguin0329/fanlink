import React, { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

export type RecruitmentGroup = '직관메이트' | '카풀';
export type RecruitmentStatus = '모집 중' | '모집 완료' | '취소';
export type RecruitmentDraft = {
  group: RecruitmentGroup;
  club: number;
  title: string;
  gameAt: string;
  capacity: number;
  departure: string;
  destination: string;
  departureAt: string;
  route: string;
  conditions: string;
};
export type Recruitment = RecruitmentDraft & { author: string; status: RecruitmentStatus; own?: boolean };

export function isRecruitmentGroup(value: string): value is RecruitmentGroup {
  return value === '직관메이트' || value === '카풀';
}

function validDateTime(value: string) {
  const match = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})$/.exec(value);
  if (!match) return false;
  const [, y, m, d, h, min] = match;
  const date = new Date(Date.UTC(Number(y), Number(m) - 1, Number(d)));
  return date.getUTCFullYear() === Number(y) && date.getUTCMonth() + 1 === Number(m)
    && date.getUTCDate() === Number(d) && Number(h) < 24 && Number(min) < 60;
}

export function RecruitmentCard({ recruitment, compact = false }: {
  recruitment: Recruitment;
  compact?: boolean;
}) {
  const { group, gameAt, capacity, departure, destination, departureAt, route, status } = recruitment;
  const carpool = group === '카풀';
  return (
    <View style={[styles.card, compact && styles.compact]}>
      <View style={styles.cardHeader}>
        <Ionicons name={carpool ? 'car-sport-outline' : 'people-outline'} size={18} color="#075cf1" />
        <Text style={[styles.label, styles.shrink]}>{carpool ? '이동 경로' : '직관 모임 정보'}</Text>
        <Text style={styles.status}>{status}</Text>
      </View>
      <Text style={styles.copy} numberOfLines={compact ? 1 : undefined}>
        {carpool ? [departure, route, destination].filter(Boolean).join(' → ') : departure}
      </Text>
      <Text style={styles.hint} numberOfLines={compact ? 1 : undefined}>
        {carpool ? '출발 ' + departureAt : '경기 ' + gameAt} · {capacity}명
      </Text>
      {carpool && <Text style={styles.hint} numberOfLines={compact ? 1 : undefined}>경기 {gameAt}</Text>}
    </View>
  );
}

type Field = 'title' | 'gameAt' | 'capacity' | 'departure' | 'destination' | 'departureAt' | 'route' | 'conditions';
type Form = Record<Field, string>;

export default function RecruitmentComposer({ initialGroup, initialClub, clubs, onPublish }: {
  initialGroup: RecruitmentGroup;
  initialClub: number;
  clubs: string[];
  onPublish: (draft: RecruitmentDraft) => void;
}) {
  const [group, setGroup] = useState(initialGroup);
  const [club, setClub] = useState(initialClub);
  const [form, setForm] = useState<Form>({
    title: '', gameAt: '', capacity: '', departure: '', destination: '', departureAt: '', route: '', conditions: '',
  });
  const [submitted, setSubmitted] = useState(false);
  const published = useRef(false);
  const carpool = group === '카풀';
  const capacity = Number(form.capacity);
  const errors: Partial<Record<Field, string>> = {
    title: form.title.trim() ? undefined : '제목을 입력해 주세요.',
    gameAt: validDateTime(form.gameAt.trim()) ? undefined : '경기 일정을 YYYY-MM-DD HH:mm 형식으로 입력해 주세요.',
    capacity: /^\d+$/.test(form.capacity) && capacity >= 1 && capacity <= 99 ? undefined : '모집 인원은 1~99명으로 입력해 주세요.',
    departure: form.departure.trim() ? undefined : '장소를 입력해 주세요.',
    destination: !carpool || form.destination.trim() ? undefined : '목적지를 입력해 주세요.',
    departureAt: !carpool || validDateTime(form.departureAt.trim()) ? undefined : '출발 일시를 YYYY-MM-DD HH:mm 형식으로 입력해 주세요.',
    conditions: form.conditions.trim() ? undefined : '세부 조건을 입력해 주세요.',
  };

  function field(key: Field, label: string, placeholder: string, maxLength: number) {
    return (
      <View style={styles.field}>
        <Text style={styles.label}>{label}</Text>
        <TextInput accessibilityLabel={label} value={form[key]} placeholder={placeholder} maxLength={maxLength}
          keyboardType={key === 'capacity' ? 'number-pad' : 'default'} multiline={key === 'conditions'}
          style={[styles.input, key === 'conditions' && styles.conditions]}
          onChangeText={value => setForm(old => ({ ...old, [key]: value }))} />
        {submitted && !!errors[key] && <Text accessibilityRole="alert" style={styles.error}>{errors[key]}</Text>}
      </View>
    );
  }

  function publish() {
    setSubmitted(true);
    if (published.current || Object.values(errors).some(Boolean)) return;
    published.current = true;
    onPublish({
      group, club, capacity, title: form.title.trim(), gameAt: form.gameAt.trim(),
      departure: form.departure.trim(), destination: carpool ? form.destination.trim() : '',
      departureAt: carpool ? form.departureAt.trim() : '', route: carpool ? form.route.trim() : '',
      conditions: form.conditions.trim(),
    });
  }

  return (
    <View style={styles.form}>
      <Text accessibilityRole="header" style={styles.title}>{carpool ? '카풀 모집 글쓰기' : '메이트 모집 글쓰기'}</Text>
      <Text style={styles.hint}>등록한 모집 글은 현재 앱 실행 중에 표시돼요.</Text>
      <Text style={styles.label}>모집 유형</Text>
      <View style={styles.choices}>
        {(['직관메이트', '카풀'] as const).map(item => (
          <Pressable key={item} accessibilityRole="radio" accessibilityState={{ checked: group === item }}
            style={[styles.choice, group === item && styles.selected]} onPress={() => setGroup(item)}>
            <Text style={[styles.label, styles.shrink]}>{item}</Text>
          </Pressable>
        ))}
      </View>
      <Text style={styles.label}>구단</Text>
      <View style={styles.choices}>
        {clubs.map((name, index) => (
          <Pressable key={name} accessibilityRole="radio" accessibilityState={{ checked: index === club }}
            style={[styles.choice, index === club && styles.selected]} onPress={() => setClub(index)}>
            <Text style={[styles.label, styles.shrink]}>{name}</Text>
          </Pressable>
        ))}
      </View>
      {field('title', '제목', carpool ? '예) 동대구역에서 경기장까지 같이 가요' : '예) 주말 경기 같이 응원해요', 80)}
      {field('gameAt', '경기 일정', 'YYYY-MM-DD HH:mm', 16)}
      {field('capacity', '모집 인원', '예) 2', 2)}
      {field('departure', carpool ? '출발지' : '만남 장소', carpool ? '예) 동대구역 2번 출구' : '예) 경기장 3번 게이트', 100)}
      {carpool && <>
        {field('destination', '목적지', '예) 대구 삼성라이온즈파크', 100)}
        {field('departureAt', '출발 일시', 'YYYY-MM-DD HH:mm', 16)}
        {field('route', '경유지 또는 이동 경로 (선택)', '예) 수성IC 경유', 120)}
      </>}
      {field('conditions', '세부 조건', '예) 경기 시작 30분 전에 만나요.', 1000)}
      <Pressable accessibilityRole="button" style={styles.publish} onPress={publish}>
        <Text style={styles.publishText}>모집 글 등록</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  form: { minWidth: 0, gap: 10 },
  field: { minWidth: 0, gap: 5 },
  title: { fontSize: 25, fontWeight: '900', color: '#101623' },
  label: { fontSize: 13, fontWeight: '700', color: '#101623' },
  copy: { fontSize: 13, color: '#101623' },
  hint: { fontSize: 12, color: '#626b7a' },
  error: { fontSize: 12, color: '#ad2336' },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  choice: { maxWidth: '100%', borderColor: '#d9e2ef', borderWidth: 1, borderRadius: 10, paddingHorizontal: 11, paddingVertical: 9, backgroundColor: '#fff' },
  selected: { borderColor: '#82b4ff', backgroundColor: '#e9f3ff' },
  input: { minHeight: 44, paddingHorizontal: 12, paddingVertical: 10, borderRadius: 11, borderWidth: 1, borderColor: '#d9e2ef', backgroundColor: '#fff', color: '#101623' },
  conditions: { minHeight: 92, textAlignVertical: 'top' },
  publish: { marginTop: 8, minHeight: 45, justifyContent: 'center', alignItems: 'center', padding: 10, backgroundColor: '#075cf1', borderRadius: 11 },
  publishText: { color: '#fff', fontWeight: '800', fontSize: 14, textAlign: 'center' },
  card: { minWidth: 0, gap: 5, backgroundColor: '#eef5ff', borderColor: '#d8e7fb', borderWidth: 1, borderRadius: 12, padding: 11 },
  compact: { paddingVertical: 7 },
  cardHeader: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 7 },
  shrink: { flexShrink: 1, minWidth: 0 },
  status: { color: '#075cf1', fontSize: 11, fontWeight: '700' },
});
