import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';

export type TradeGroup = '티켓' | '굿즈';
export type TradeDraft = {
  group: TradeGroup;
  club: number;
  title: string;
  body: string;
  price: number;
  imageUri: string;
  referencePrice: number;
};

type Scenario = 'success' | 'unreadable' | 'mismatch';
type Analysis = { status: 'idle' | 'pending' | 'unreadable' | 'mismatch' }
  | { status: 'ready'; referencePrice: number };

const demoPrices = { 티켓: 26000, 굿즈: 30000 };
const formatWon = (price: number) => price.toLocaleString('ko-KR') + '원';

type Props = {
  initialDraft?: TradeDraft;
  initialGroup: TradeGroup;
  initialClub: number;
  clubs: string[];
  onPublish: (draft: TradeDraft) => void;
};

function Choice<T extends string | number>({ options, value, onChange }: {
  options: { label: string; value: T }[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View style={styles.choices}>
      {options.map(option => (
        <Pressable key={option.value} accessibilityRole="radio"
          accessibilityState={{ checked: value === option.value }} onPress={() => onChange(option.value)}
          style={[styles.choice, value === option.value && styles.selected]}>
          <Text style={[styles.choiceText, value === option.value ? styles.link : styles.label]}>{option.label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

export default function TradeComposer({ initialGroup, initialClub, initialDraft, clubs, onPublish }: Props) {
  const [group, setGroup] = useState<TradeGroup>(initialDraft?.group ?? initialGroup);
  const [club, setClub] = useState(initialDraft?.club ?? initialClub);
  const [title, setTitle] = useState(initialDraft?.title ?? '');
  const [body, setBody] = useState(initialDraft?.body ?? '');
  const [price, setPrice] = useState(initialDraft ? String(initialDraft.price) : '');
  const [imageUri, setImageUri] = useState(initialDraft?.imageUri);
  const [imageRatio, setImageRatio] = useState(1.7);
  const [scenario, setScenario] = useState<Scenario>('success');
  const [analysis, setAnalysis] = useState<Analysis>(initialDraft
    ? { status: 'ready', referencePrice: initialDraft.referencePrice }
    : { status: 'idle' });
  const [imageError, setImageError] = useState('');
  const [picking, setPicking] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const pickerPending = useRef(false);
  const published = useRef(false);
  const amount = Number(price);
  const validPrice = /^\d+$/.test(price.trim()) && Number.isSafeInteger(amount) && amount > 0;
  const priceTooHigh = validPrice && analysis.status === 'ready' && amount > analysis.referencePrice;
  const analyzing = analysis.status === 'pending';

  useEffect(() => {
    if (!analyzing) return;
    const timer = setTimeout(() => {
      setAnalysis(scenario === 'success'
        ? { status: 'ready', referencePrice: demoPrices[group] }
        : { status: scenario });
    }, 450);
    return () => clearTimeout(timer);
  }, [analyzing, group, scenario]);

  function changeGroup(next: TradeGroup) {
    if (next === group) return;
    setGroup(next);
    setAnalysis({ status: 'idle' });
  }

  function changeScenario(next: Scenario) {
    if (next === scenario) return;
    setScenario(next);
    setAnalysis({ status: 'idle' });
  }

  function changeImage(uri?: string) {
    setImageUri(uri);
    setImageRatio(1.7);
    setAnalysis({ status: 'idle' });
    setImageError('');
  }

  async function pickImage() {
    if (pickerPending.current) return;
    pickerPending.current = true;
    setPicking(true);
    setImageError('');
    try {
      const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 1 });
      if (!result.canceled) changeImage(result.assets[0].uri);
    } catch {
      setImageError('사진을 열지 못했어요. 사진 선택을 다시 시도해 주세요.');
    } finally {
      pickerPending.current = false;
      setPicking(false);
    }
  }

  function publish() {
    if (pickerPending.current || published.current) return;
    setSubmitted(true);
    if (!title.trim() || !body.trim() || !validPrice || !imageUri || analysis.status !== 'ready' || priceTooHigh) return;
    published.current = true;
    onPublish({ group, club, title: title.trim(), body: body.trim(), price: amount, imageUri, referencePrice: analysis.referencePrice });
  }

  return (
    <View style={styles.form}>
      <Text accessibilityRole="header" style={styles.title}>{initialDraft ? '거래 글 수정' : '거래 글쓰기'}</Text>
      <Text style={styles.hint}>등록한 글과 사진은 현재 앱 실행 중에만 표시돼요.</Text>
      <Text style={styles.label}>거래 유형</Text>
      <Choice<TradeGroup> options={[{ label: '티켓', value: '티켓' }, { label: '굿즈', value: '굿즈' }]} value={group} onChange={changeGroup} />
      <Text style={styles.label}>구단</Text>
      <Choice options={clubs.map((label, value) => ({ label, value }))} value={club} onChange={setClub} />

      <Text style={styles.label}>제목</Text>
      <TextInput accessibilityLabel="제목" style={styles.input} value={title} onChangeText={setTitle}
        placeholder={group === '티켓' ? '예) 삼성 라이온즈 경기 티켓 양도' : '예) 삼성 라이온즈 유니폼 양도'} maxLength={80} />
      {submitted && !title.trim() && <Text style={styles.error}>제목을 입력해 주세요.</Text>}

      <Text style={styles.label}>판매 희망가</Text>
      <TextInput accessibilityLabel="판매 희망가" style={styles.input} value={price} onChangeText={setPrice}
        keyboardType="number-pad" placeholder="원 단위로 입력" maxLength={12} />
      {submitted && !validPrice && <Text style={styles.error}>희망가를 1원 이상의 숫자로 입력해 주세요.</Text>}

      <Text style={styles.label}>본문</Text>
      <TextInput accessibilityLabel="본문" style={[styles.input, styles.body]} value={body} onChangeText={setBody}
        multiline placeholder="거래할 티켓이나 굿즈의 정보를 적어 주세요." maxLength={1500} />
      {submitted && !body.trim() && <Text style={styles.error}>본문을 입력해 주세요.</Text>}

      <Text style={styles.label}>{group === '티켓' ? '예매 내역 이미지' : '굿즈 이미지'}</Text>
      {imageUri && (
        <View style={[styles.preview, { aspectRatio: imageRatio }]}>
          <Image key={imageUri} accessibilityLabel="선택한 거래 사진" source={{ uri: imageUri }} style={styles.image}
            resizeMode="contain" onLoad={({ nativeEvent: { source } }) => {
              if (source.width > 0 && source.height > 0) {
                setImageRatio(Math.min(2, Math.max(0.85, source.width / source.height)));
              }
            }} onError={() => {
              changeImage();
              setImageError('사진을 미리 볼 수 없어요. 다른 사진을 선택해 주세요.');
            }} />
        </View>
      )}
      <View style={styles.choices}>
        <Pressable style={styles.choice} accessibilityRole="button" disabled={picking} onPress={pickImage}>
          <Ionicons name="image-outline" size={18} color="#075cf1" />
          <Text style={[styles.link, styles.choiceText]}>{picking ? '사진 선택 중…' : imageUri ? '사진 교체' : '사진 선택'}</Text>
        </Pressable>
        {imageUri && <Pressable style={styles.choice} accessibilityRole="button" disabled={picking}
          onPress={() => changeImage()}>
          <Text style={styles.label}>사진 삭제</Text>
        </Pressable>}
      </View>
      {(imageError || (submitted && !imageUri)) && (
        <Text accessibilityRole="alert" style={styles.error}>{imageError || '거래 사진을 한 장 첨부해 주세요.'}</Text>
      )}

      <View style={styles.analysis}>
        <Text style={styles.label}>OCR 가격 분석 · 데모</Text>
        <Text style={styles.hint}>사진을 분석하는 대신 아래 테스트 결과를 사용해요.</Text>
        {group === '굿즈' && <Text style={styles.hint}>굿즈는 데모 정가를 사용해요. 중고 굿즈 검증 기준은 별도 정책이 필요해요.</Text>}
        <Choice<Scenario> options={[
          { label: '정가 인식', value: 'success' },
          { label: '인식 실패', value: 'unreadable' },
          { label: '정보 불일치', value: 'mismatch' },
        ]} value={scenario} onChange={changeScenario} />
        <Pressable accessibilityRole="button" disabled={!imageUri || picking || analyzing}
          style={[styles.choice, (!imageUri || picking || analyzing) && styles.disabled]}
          onPress={() => setAnalysis({ status: 'pending' })}>
          {analyzing && <ActivityIndicator color="#075cf1" />}
          <Text style={[styles.link, styles.choiceText]}>{analyzing ? '가격 분석 중…' : '가격 분석하기'}</Text>
        </Pressable>
        {analysis.status === 'ready' && <>
          <Text style={styles.label}>확인한 데모 정가 · {formatWon(analysis.referencePrice)}</Text>
          {validPrice && (priceTooHigh
            ? <Text accessibilityRole="alert" style={styles.error}>정가를 초과해요. {formatWon(analysis.referencePrice)} 이하로 입력해 주세요.</Text>
            : <Text style={styles.success}>가격 검증을 통과했어요.</Text>)}
        </>}
        {analysis.status === 'unreadable' && <Text accessibilityRole="alert" style={styles.error}>가격을 인식하지 못했어요. 사진을 교체하거나 분석을 다시 시도해 주세요.</Text>}
        {analysis.status === 'mismatch' && <Text accessibilityRole="alert" style={styles.error}>가격 정보가 일치하지 않아요. 사진을 확인하고 다시 분석해 주세요.</Text>}
        {submitted && !!imageUri && analysis.status === 'idle' && <Text accessibilityRole="alert" style={styles.error}>등록 전에 가격을 분석해 주세요.</Text>}
      </View>

      <Pressable accessibilityRole="button" disabled={picking || analyzing || priceTooHigh} onPress={publish}
        style={[styles.publish, (picking || analyzing || priceTooHigh) && styles.disabled]}>
        <Text style={styles.publishText}>{initialDraft ? '수정 저장' : '등록하기'}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  form: { minWidth: 0, gap: 10 },
  title: { fontSize: 25, fontWeight: '900', color: '#101623' },
  label: { fontSize: 13, fontWeight: '700', color: '#101623' },
  hint: { fontSize: 12, color: '#626b7a', lineHeight: 18 },
  link: { fontSize: 13, fontWeight: '700', color: '#075cf1' },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  choiceText: { flexShrink: 1 },
  choice: { maxWidth: '100%', flexShrink: 1, flexDirection: 'row', alignItems: 'center', gap: 6, borderColor: '#d9e2ef', borderWidth: 1, borderRadius: 10, paddingHorizontal: 11, paddingVertical: 9, backgroundColor: '#fff' },
  selected: { borderColor: '#82b4ff', backgroundColor: '#e9f3ff' },
  input: { minHeight: 44, paddingHorizontal: 12, paddingVertical: 10, borderRadius: 11, borderWidth: 1, borderColor: '#d9e2ef', backgroundColor: '#fff', color: '#101623' },
  body: { minHeight: 112, textAlignVertical: 'top' },
  preview: { alignSelf: 'stretch', backgroundColor: '#eef3fa', borderRadius: 12, overflow: 'hidden' },
  image: { width: '100%', height: '100%' },
  analysis: { minWidth: 0, gap: 8, borderWidth: 1, borderColor: '#d9e2ef', borderRadius: 12, padding: 12 },
  disabled: { opacity: 0.5 },
  success: { color: '#17724c', fontSize: 12, lineHeight: 18 },
  error: { color: '#ad2336', fontSize: 12, lineHeight: 18 },
  publish: { marginTop: 8, minHeight: 45, justifyContent: 'center', alignItems: 'center', padding: 10, backgroundColor: '#075cf1', borderRadius: 11 },
  publishText: { color: 'white', fontWeight: '800', fontSize: 14 },
});
