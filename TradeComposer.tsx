import React, { useRef, useState } from 'react';
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { composerStyles } from './ComposerUI';

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

type Analysis = { status: 'idle' | 'pending' | 'unreadable' }
  | { status: 'ready'; referencePrice: number };
const formatWon = (price: number) => price.toLocaleString('ko-KR') + '원';

type Props = {
  initialDraft?: TradeDraft;
  initialGroup: TradeGroup;
  initialClub: number;
  onPublish: (draft: TradeDraft) => void | Promise<void>;
  onVerifyPrice: (imageUri: string, declaredPrice: number) => Promise<number>;
};

export default function TradeComposer({ initialGroup, initialClub, initialDraft, onPublish, onVerifyPrice }: Props) {
  const group = initialDraft?.group ?? initialGroup;
  const club = initialDraft?.club ?? initialClub;
  const [title, setTitle] = useState(initialDraft?.title ?? '');
  const [body, setBody] = useState(initialDraft?.body ?? '');
  const [price, setPrice] = useState(initialDraft ? String(initialDraft.price) : '');
  const [imageUri, setImageUri] = useState(initialDraft?.imageUri);
  const [imageRatio, setImageRatio] = useState(1.7);
  const [analysis, setAnalysis] = useState<Analysis>(initialDraft
    ? { status: 'ready', referencePrice: initialDraft.referencePrice }
    : { status: 'idle' });
  const [imageError, setImageError] = useState('');
  const [picking, setPicking] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState('');
  const pickerPending = useRef(false);
  const published = useRef(false);
  const amount = Number(price);
  const validPrice = /^\d+$/.test(price.trim()) && Number.isSafeInteger(amount) && amount > 0;
  const priceTooHigh = validPrice && analysis.status === 'ready' && amount > analysis.referencePrice;
  const analyzing = analysis.status === 'pending';

  async function analyzePrice() {
    if (!imageUri || !validPrice || analyzing) {
      setSubmitted(true);
      return;
    }
    setAnalysis({status: 'pending'});
    setImageError('');
    try {
      const verifiedFacePrice = await onVerifyPrice(imageUri, amount);
      setAnalysis({status: 'ready', referencePrice: verifiedFacePrice});
    } catch (error) {
      setAnalysis({status: 'unreadable'});
      setImageError(error instanceof Error ? error.message : 'OCR 검증에 실패했습니다.');
    }
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

  async function publish() {
    if (pickerPending.current || published.current || publishing) return;
    setSubmitted(true);
    if (!title.trim() || !body.trim() || !validPrice || !imageUri || (group === '티켓' && (analysis.status !== 'ready' || priceTooHigh))) return;
    published.current = true;
    setPublishing(true);
    setPublishError('');
    try {
      await onPublish({ group, club, title: title.trim(), body: body.trim(), price: amount, imageUri,
        referencePrice: analysis.status === 'ready' ? analysis.referencePrice : amount });
    } catch (error) {
      published.current = false;
      setPublishError(error instanceof Error ? error.message : '등록에 실패했습니다.');
    } finally {
      setPublishing(false);
    }
  }

  return (
    <View style={composerStyles.form}>
      <Text accessibilityRole="header" style={composerStyles.title}>{group + (initialDraft ? ' 거래 글 수정' : ' 거래 글쓰기')}</Text>

      <View style={composerStyles.panel}>
        <Text style={composerStyles.label}>{group === '티켓' ? '예매 내역 이미지' : '굿즈 이미지'}</Text>
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
        <View style={composerStyles.choices}>
          <Pressable style={composerStyles.choice} accessibilityRole="button" disabled={picking} onPress={pickImage}>
            <Ionicons name="image-outline" size={18} color="#075cf1" />
            <Text style={[composerStyles.link, composerStyles.choiceText]}>{picking ? '사진 선택 중…' : imageUri ? '사진 교체' : '사진 선택'}</Text>
          </Pressable>
          {imageUri && <Pressable style={composerStyles.choice} accessibilityRole="button" disabled={picking}
            onPress={() => changeImage()}>
            <Text style={composerStyles.label}>사진 삭제</Text>
          </Pressable>}
        </View>
        {(imageError || (submitted && !imageUri)) && (
          <Text accessibilityRole="alert" style={composerStyles.error}>{imageError || '거래 사진을 한 장 첨부해 주세요.'}</Text>
        )}

      </View>

      <View style={composerStyles.panel}>
        <Text style={composerStyles.label}>제목</Text>
        <TextInput accessibilityLabel="제목" style={composerStyles.input} value={title} onChangeText={setTitle}
          placeholder={group === '티켓' ? '예) 삼성 라이온즈 경기 티켓 양도' : '예) 삼성 라이온즈 유니폼 양도'} maxLength={80} />
        {submitted && !title.trim() && <Text style={composerStyles.error}>제목을 입력해 주세요.</Text>}

        <Text style={composerStyles.label}>판매 희망가</Text>
        <TextInput accessibilityLabel="판매 희망가" style={composerStyles.input} value={price} onChangeText={next => {setPrice(next);setAnalysis({status: 'idle'});}}
          keyboardType="number-pad" placeholder="원 단위로 입력" maxLength={12} />
        {submitted && !validPrice && <Text style={composerStyles.error}>희망가를 1원 이상의 숫자로 입력해 주세요.</Text>}

        <Text style={composerStyles.label}>본문</Text>
        <TextInput accessibilityLabel="본문" style={[composerStyles.input, styles.body]} value={body} onChangeText={setBody}
          multiline placeholder="거래할 티켓이나 굿즈의 정보를 적어 주세요." maxLength={1500} />
        {submitted && !body.trim() && <Text style={composerStyles.error}>본문을 입력해 주세요.</Text>}

      </View>

      {group === '티켓' && <View style={[composerStyles.panel, styles.analysis]}>
        <Text style={composerStyles.label}>티켓 정가 OCR · 실제 서버 검증</Text>
        <Text style={composerStyles.hint}>가격과 구매 캡처를 입력한 뒤 서버에서 정가를 검증하세요. 검증 전에는 등록할 수 없습니다.</Text>
        <Pressable accessibilityRole="button" disabled={!imageUri || !validPrice || picking || analyzing}
          style={[composerStyles.choice, (!imageUri || !validPrice || picking || analyzing) && styles.disabled]}
          onPress={() => {void analyzePrice();}}>
          {analyzing && <ActivityIndicator color="#075cf1" />}
          <Text style={[composerStyles.link, composerStyles.choiceText]}>{analyzing ? '서버 OCR 검사 중…' : '정가 검증하기'}</Text>
        </Pressable>
        {analysis.status === 'ready' && <>
          <Text style={composerStyles.label}>OCR 확인 정가 · {formatWon(analysis.referencePrice)}</Text>
          {priceTooHigh ? <Text accessibilityRole="alert" style={composerStyles.error}>정가를 초과합니다.</Text>
            : <Text style={styles.success}>서버에서 정가 검증이 완료되었습니다.</Text>}
        </>}
        {analysis.status === 'unreadable' && <Text accessibilityRole="alert" style={composerStyles.error}>정가 인증을 완료하지 못했습니다. 캡처를 다시 확인해주세요.</Text>}
        {submitted && analysis.status === 'idle' && <Text accessibilityRole="alert" style={composerStyles.error}>먼저 서버 OCR 정가 검증을 진행해주세요.</Text>}
      </View>}
      {!!publishError && <Text accessibilityRole="alert" style={composerStyles.error}>{publishError}</Text>}

      <Pressable accessibilityRole="button" disabled={picking || analyzing || publishing || priceTooHigh} onPress={() => {void publish();}}
        style={[composerStyles.publish, (picking || analyzing || publishing || priceTooHigh) && styles.disabled]}>
        <Text style={composerStyles.publishText}>{publishing ? '서버에 저장 중…' : initialDraft ? '수정 저장' : '등록하기'}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  body: { minHeight: 112, textAlignVertical: 'top' },
  preview: { alignSelf: 'stretch', backgroundColor: '#eef3fa', borderRadius: 12, overflow: 'hidden' },
  image: { width: '100%', height: '100%' },
  analysis: { backgroundColor: '#f1f6ff' },
  disabled: { opacity: 0.5 },
  success: { color: '#17724c', fontSize: 12, lineHeight: 18 },
});
