import React, { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { composerStyles } from './ComposerUI';

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
        <Text style={[composerStyles.label, styles.shrink]}>{carpool ? '이동 경로' : '직관 모임 정보'}</Text>
        <Text style={styles.status}>{status}</Text>
      </View>
      <Text style={styles.copy} numberOfLines={compact ? 1 : undefined}>
        {carpool ? [departure, route, destination].filter(Boolean).join(' → ') : departure}
      </Text>
      <Text style={composerStyles.hint} numberOfLines={compact ? 1 : undefined}>
        {carpool ? '출발 ' + departureAt : '경기 ' + gameAt} · {capacity}명
      </Text>
      {carpool && <Text style={composerStyles.hint} numberOfLines={compact ? 1 : undefined}>경기 {gameAt}</Text>}
    </View>
  );
}

type Field = 'title' | 'gameAt' | 'capacity' | 'departure' | 'destination' | 'departureAt' | 'route' | 'conditions';
type Form = Record<Field, string>;

export default function RecruitmentComposer({ initialGroup: group, initialClub: club, onPublish }: {
  initialGroup: RecruitmentGroup;
  initialClub: number;
  onPublish: (draft: RecruitmentDraft) => void;
}) {
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
        <Text style={composerStyles.label}>{label}</Text>
        <TextInput accessibilityLabel={label} value={form[key]} placeholder={placeholder} maxLength={maxLength}
          keyboardType={key === 'capacity' ? 'number-pad' : 'default'} multiline={key === 'conditions'}
          style={[composerStyles.input, key === 'conditions' && styles.conditions]}
          onChangeText={value => setForm(old => ({ ...old, [key]: value }))} />
        {submitted && !!errors[key] && <Text accessibilityRole="alert" style={composerStyles.error}>{errors[key]}</Text>}
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
    <View style={composerStyles.form}>
      <Text accessibilityRole="header" style={composerStyles.title}>{carpool ? '카풀 모집 글쓰기' : '메이트 모집 글쓰기'}</Text>
      <View style={composerStyles.panel}>
        {field('title', '제목', carpool ? '예) 동대구역에서 경기장까지 같이 가요' : '예) 주말 경기 같이 응원해요', 80)}
        {field('gameAt', '경기 일정', 'YYYY-MM-DD HH:mm', 16)}
        {field('capacity', '모집 인원', '예) 2', 2)}
        {field('departure', carpool ? '출발지' : '만남 장소', carpool ? '예) 동대구역 2번 출구' : '예) 경기장 3번 게이트', 100)}
      </View>

      <View style={composerStyles.panel}>
        {carpool && <>
          {field('destination', '목적지', '예) 대구 삼성라이온즈파크', 100)}
          {field('departureAt', '출발 일시', 'YYYY-MM-DD HH:mm', 16)}
          {field('route', '경유지 또는 이동 경로 (선택)', '예) 수성IC 경유', 120)}
        </>}
        {field('conditions', '세부 조건', '예) 경기 시작 30분 전에 만나요.', 1000)}
      </View>

      <Pressable accessibilityRole="button" style={composerStyles.publish} onPress={publish}>
        <Text style={composerStyles.publishText}>모집 글 등록</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  field: { minWidth: 0, gap: 5 },
  copy: { fontSize: 13, color: '#101623' },
  conditions: { minHeight: 92, textAlignVertical: 'top' },
  card: { minWidth: 0, gap: 5, backgroundColor: '#eef5ff', borderColor: '#d8e7fb', borderWidth: 1, borderRadius: 12, padding: 11 },
  compact: { paddingVertical: 7 },
  cardHeader: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 7 },
  shrink: { flexShrink: 1, minWidth: 0 },
  status: { color: '#075cf1', fontSize: 11, fontWeight: '700' },
});
