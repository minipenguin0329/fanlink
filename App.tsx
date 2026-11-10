import React, { useState } from 'react';
import {
  Alert, BackHandler, Image, ImageSourcePropType, KeyboardAvoidingView, Platform, Pressable,
  ScrollView, StatusBar, StyleSheet, Switch, Text, TextInput, useWindowDimensions, View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import AuthGate from './AuthGate';
import TradeComposer, { type TradeDraft } from './TradeComposer';
import RecruitmentComposer, {
  RecruitmentCard, isRecruitmentGroup, type Recruitment, type RecruitmentDraft, type RecruitmentStatus,
} from './Recruitment';

type IconName = React.ComponentProps<typeof Ionicons>['name'];
type Screen = 'home' | 'community' | 'chats' | 'my' | 'detail' | 'chat' |
  'profile' | 'security' | 'notifications' | 'settings' | 'posts' | 'comments' |
  'history' | 'notices' | 'guide' | 'help' | 'empty' | 'trade-compose' | 'trade-edit' | 'recruit-compose' | 'joined' | 'cleanpay';
type Group = '티켓' | '굿즈' | '카풀' | '직관메이트';

const blue = '#075cf1';
const ink = '#101623';
const muted = '#626b7a';
const images = {
  stadium: require('./assets/stadium.png'),
  cap: require('./assets/cap.png'),
  jersey: require('./assets/jersey.png'),
  avatar: require('./assets/avatar.png'),
  samsung: require('./assets/samsung.png'),
  kt: require('./assets/kt.png'),
  daegu: require('./assets/daegu.png'),
  seongnam: require('./assets/seongnam.png'),
  pohang: require('./assets/pohang.png'),
  bucheon: require('./assets/bucheon.png'),
  profileArt: require('./assets/profile-art.png'),
  backdrop: require('./assets/stadium-backdrop.png'),
  weather: require('./assets/weather-art.png'),
};
const clubs = [
  { name: '삼성 라이온즈', league: 'KBO', logo: images.samsung, rival: 'kt wiz', rivalLogo: images.kt, date: '11월 8일 (일)', time: '14:00', place: '대구', color: '#0654c8' },
  { name: '대구FC', league: 'K리그1', logo: images.daegu, rival: '성남FC', rivalLogo: images.seongnam, date: '11월 7일 (토)', time: '14:00', place: '', color: '#38a6d9' },
  { name: '포항 스틸러스', league: 'K리그1', logo: images.pohang, rival: '부천FC', rivalLogo: images.bucheon, date: '11월 8일 (일)', time: '시간 확인 필요', place: '', color: '#c8212f' },
];
const categories: { name: Group; icon: IconName }[] = [
  { name: '티켓', icon: 'ticket' }, { name: '굿즈', icon: 'bag-handle' },
  { name: '카풀', icon: 'car-sport' }, { name: '직관메이트', icon: 'people' },
];
type Post = { id: number; group: Group; club: number; title: string; summary: string; image: ImageSourcePropType; likes: number; replies: number; trade?: Pick<TradeDraft, 'price' | 'referencePrice' | 'imageUri'> & { author: string }; recruitment?: Recruitment };
type DemoPayment = {
  title: string;
  amount: number;
  status: '대금 보관' | '구매 확정';
};
const sampleTradeAmounts: Record<number, number> = { 1: 52000, 2: 28000, 3: 24000, 4: 50000 };
const won = (amount: number) => amount.toLocaleString('ko-KR') + '원';
const tradeRoomKey = (name: string, id: number) => 'trade:' + id + ':' + name;
type MatchStatus = '신청 중' | '호스트 동의' | '상호 동의' | '참여 확정' | '참여 취소' | '거절됨';
const samplePosts: Post[] = [
  { id: 1, group: '티켓', club: 0, title: '[정가양도] 삼성 라이온즈 vs 키움 히어로즈 (11/8 토)', summary: '3루 내야지정석 13블록 8열 12, 13번 (연석 2매)\n정가 26,000원/매 (총 52,000원)', image: images.stadium, likes: 24, replies: 7 },
  { id: 2, group: '티켓', club: 0, title: '[정가양도] 삼성 라이온즈 vs SSG 랜더스 (11/9 일)', summary: '1루 내야지정석 5블록 10열 5번 (1매)\n정가 28,000원', image: images.stadium, likes: 18, replies: 4 },
  { id: 3, group: '티켓', club: 0, title: '[정가양도] 삼성 라이온즈 홈경기 티켓 (11/15 토)', summary: '1루 외야지정석 102블록 6열 21, 22번 (연석 2매)\n정가 12,000원/매', image: images.stadium, likes: 31, replies: 9 },
  { id: 4, group: '굿즈', club: 0, title: '[양도] 삼성라이온즈 유니폼 (L, 유광)', summary: '홈 유니폼 · L사이즈 · 1회 착용', image: images.jersey, likes: 12, replies: 5 },
  { id: 5, group: '직관메이트', club: 0, title: '[모집] 11/15(토) 두산전 직관 메이트 구해요!', summary: '3루 응원석에서 같이 응원해요! 경기 시작 전에 만나 좌석으로 이동해요.', image: images.stadium, likes: 18, replies: 11,
    recruitment: { group: '직관메이트', club: 0, title: '두산전 직관 메이트', gameAt: '2026-11-15 17:00', capacity: 4, departure: '삼성라이온즈파크 3번 게이트', destination: '', departureAt: '', route: '', conditions: '경기 30분 전에 만나요.', author: '직관메이트', status: '모집 중' } },
  { id: 6, group: '굿즈', club: 0, title: '[판매] 삼성 라이온즈 모자 (신형)', summary: '공식 굿즈 · 프리사이즈 · 미사용', image: images.cap, likes: 5, replies: 3 },
  { id: 7, group: '카풀', club: 1, title: '[카풀] 동대구역에서 경기장 같이 가요', summary: '동대구역에서 만나 함께 경기장으로 이동해요.', image: images.stadium, likes: 8, replies: 2,
    recruitment: { group: '카풀', club: 1, title: '동대구역에서 경기장까지 카풀', gameAt: '2026-11-15 15:00', capacity: 2, departure: '동대구역 2번 출구', destination: '대구iM뱅크파크', departureAt: '2026-11-15 13:00', route: '신천대로', conditions: '출발 10분 전에 모여 주세요.', author: '카풀메이트', status: '모집 중' } },
];
const chatSamples = [
  ['라팍티켓', '네! 아직 예매 가능해요!', '오후 5:24', 2, 0],
  ['삼성팬⚾', '3루 내야지정석 2연석 가능할까요?', '오후 4:50', 1, 0],
  ['라이온즈사랑', '가격은 정가로 2장입니다!', '오후 4:37', 0, 0],
  ['라팍가는중', '혹시 오늘 경기 취소됐나요??', '오후 3:21', 0, 0],
  ['삼성직관', '네 그럼 1루쪽으로 예약할게요', '오후 2:54', 0, 0],
  ['승리의라이온즈', '늦게 확인해서 죄송해요', '오후 1:20', 0, 0],
  ['라온이', '좌석 사진 한 번만 더 보내주실 수 있을까요?', '오전 11:48', 0, 0],
  ['푸른물결', '넵! 거래 방법은 어떻게 하실까요?', '오전 10:06', 0, 0],
] as const;
const myPosts = [samplePosts[0], samplePosts[3], samplePosts[5]];
const matchRoomKey = (id: number) => "matching:" + id;
const tabScreens: Screen[] = ['home', 'community', 'chats', 'my'];
const tabIcons: IconName[] = ['home', 'list', 'chatbubble-ellipses', 'person'];
const tabLabels = ['홈', '커뮤니티', '채팅', '마이'];
const titles: Partial<Record<Screen, string>> = {
  cleanpay: '클린페이 센터', joined: '참여 중인 모임', profile: '내 프로필', security: '계정 및 보안', notifications: '알림 설정',
  settings: '환경 설정', posts: '내 게시글', comments: '내 댓글', history: '거래내역',
  notices: '공지사항', guide: '이용가이드', help: '문의하기',
};
const intro: Partial<Record<Screen, string>> = {
  security: '계정 정보와 보안 설정을 관리하세요.', notifications: '원하는 알림만 골라서 받을 수 있어요.',
  settings: '앱 사용 환경을 내 취향에 맞게 설정해요.', notices: 'FANLINK의 새로운 소식과 중요한 안내를 확인하세요.',
  guide: 'FANLINK를 처음 이용하시나요? 주요 기능과 사용 방법을 쉽게 안내해 드려요.',
  help: '서비스 이용 중 궁금한 점이나 문제가 있나요? 언제든지 문의해 주세요.',
};
function Icon({ name, size = 18, color = ink }: { name: IconName; size?: number; color?: string }) {
  return <Ionicons name={name} size={size} color={color} />;
}
function IconButton({ name, label, onPress }: { name: IconName; label: string; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityLabel={label} style={s.iconButton} onPress={onPress}><Icon name={name} size={21} /></Pressable>;
}
function Button({ label, onPress, color = blue, textColor = 'white' }: { label: string; onPress: () => void; color?: string; textColor?: string }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [s.button, { backgroundColor: color, opacity: pressed ? .8 : 1 }]}><Text style={[s.buttonText, { color: textColor }]}>{label}</Text></Pressable>;
}
function Pill({ label, selected, icon, onPress }: { label: string; selected?: boolean; icon?: IconName; onPress: () => void }) {
  return <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[s.pill, selected && s.pillActive]}>
    {icon && <Icon name={icon} size={13} color={selected ? blue : muted} />}<Text style={[s.small, s.shrink, { color: selected ? blue : ink, fontWeight: '700' }]}>{label}</Text>
  </Pressable>;
}
function Pills({ items, value, onChange }: { items: string[]; value: string; onChange: (v: string) => void }) {
  return <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.pills}>
    {items.map(item => <Pill key={item} label={item} selected={item === value} icon={categories.find(c => c.name === item)?.icon} onPress={() => onChange(item)} />)}
  </ScrollView>;
}
function Avatar({ source = images.avatar, size = 42 }: { source?: ImageSourcePropType; size?: number }) {
  return <Image source={source} style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: '#e5eefc' }} />;
}
function Section({ title, children, action }: { title: string; children: React.ReactNode; action?: () => void }) {
  return <View style={s.section}><View style={s.between}><Text style={[s.sectionTitle, s.shrink]}>{title}</Text>{action && <Pressable onPress={action}><Text style={s.link}>전체보기 ›</Text></Pressable>}</View>{children}</View>;
}
function Row({ icon, label, hint, value, onPress, toggle, danger }: { icon: IconName; label: string; hint?: string; value?: string; onPress?: () => void; toggle?: { value: boolean; onChange: (v: boolean) => void }; danger?: boolean }) {
  return <Pressable accessibilityRole={onPress && !toggle ? 'button' : undefined} onPress={onPress} disabled={!onPress} style={s.rowItem}>
    <Icon name={icon} size={18} color={danger ? '#db3248' : '#344052'} />
    <View style={s.flex}><Text style={[s.rowTitle, danger && { color: '#db3248' }]}>{label}</Text>{hint && <Text style={s.tiny}>{hint}</Text>}</View>
    {!!value && <Text style={[s.small, s.rowValue]}>{value}</Text>}
    {toggle ? <Switch accessibilityLabel={label} value={toggle.value} onValueChange={toggle.onChange} trackColor={{ false: '#c8cbd3', true: blue }} /> : onPress && <Icon name="chevron-forward" size={13} color={muted} />}
  </Pressable>;
}
function Banner({ title, hint, icon, children }: { title: string; hint: string; icon: IconName; children?: React.ReactNode }) {
  return <View style={s.banner}><View style={s.bannerCircle} /><View style={s.flex}><Text style={s.whiteTitle}>{title}</Text><Text style={s.whiteSmall}>{hint}</Text>{children}</View><Icon name={icon} size={48} color="#b7d7ff" /></View>;
}
function Empty({ text = '표시할 항목이 없어요.' }: { text?: string }) {
  return <View style={s.empty}><Icon name="file-tray-outline" color="#aab5c5" size={28} /><Text style={s.small}>{text}</Text></View>;
}

export default function App() {
  return <SafeAreaProvider><StatusBar barStyle="dark-content" /><AuthGate>{logout => <FanLink logout={logout} />}</AuthGate></SafeAreaProvider>;
}
function FanLink({ logout }: { logout: () => void }) {
  const { height, fontScale } = useWindowDimensions();
  const compactChat = height < 650 || fontScale > 1.35;
  const [stack, setStack] = useState<Screen[]>(['home']);
  const screen = stack[stack.length - 1];
  const push = (route: Screen) => setStack(old => [...old, route]);
  const replace = (route: Screen) => setStack([route]);
  const back = () => setStack(old => old.length > 1 ? old.slice(0, -1) : old);
  React.useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (stack.length <= 1) return false;
      back();
      return true;
    });
    return () => subscription.remove();
  }, [stack.length]);

  const [club, setClub] = useState(0);
  const [group, setGroup] = useState<Group>('티켓');
  const [chatGroup, setChatGroup] = useState('티켓');
  const [activityFilter, setActivityFilter] = useState('전체');
  const [noticeFilter, setNoticeFilter] = useState('전체');
  const [historyFilter, setHistoryFilter] = useState('전체');
  const [post, setPost] = useState(samplePosts[0]);
  const [tradePosts, setTradePosts] = useState<Post[]>([]);
  const nextPostId = React.useRef(samplePosts.length + 1);
  const [recruitmentPosts, setRecruitmentPosts] = useState<Post[]>([]);
  const [matches, setMatches] = useState<Record<number, MatchStatus>>({});
  const posts = [...recruitmentPosts, ...tradePosts, ...samplePosts];
  const ownPosts = [...recruitmentPosts, ...tradePosts, ...myPosts];
  const [chat, setChat] = useState('라팍티켓');
  const [readRooms, setReadRooms] = useState<string[]>([]);
  const [saved, setSaved] = useState<number[]>([]);
  const [liked, setLiked] = useState<number[]>([]);
  const [comments, setComments] = useState<Record<number, string[]>>({});
  const [messages, setMessages] = useState<Record<string, string[]>>({});
  const [draft, setDraft] = useState('');
  const [following, setFollowing] = useState(false);
  const [payments, setPayments] = useState<Record<string, DemoPayment>>({});
  const [chatPost, setChatPost] = useState(samplePosts[0]);
  const [viewed, setViewed] = useState<number[]>([]);
  const [settings, setSettings] = useState<Record<string, boolean>>({ 이메일: false });
  const [theme, setTheme] = useState('라이트');
  const [accent, setAccent] = useState(0);
  const [font, setFont] = useState(1);
  const [month, setMonth] = useState(11);
  const [search, setSearch] = useState<string | null>(null);
  const [activity, setActivity] = useState('게시글');
  const [nickname, setNickname] = useState('라팍직관러');
  const [fanClub, setFanClub] = useState(0);
  const [fanVerified, setFanVerified] = useState(false);
  const [profileImage, setProfileImage] = useState<ImageSourcePropType>(images.avatar);
  const profilePickerPending = React.useRef(false);
  const [editing, setEditing] = useState(false);
  const [expanded, setExpanded] = useState<string | null>(null);
  async function pickProfileImage() {
    if (profilePickerPending.current) return;
    profilePickerPending.current = true;
    try {
      const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });
      if (!result.canceled) setProfileImage({ uri: result.assets[0].uri });
    } catch {
      Alert.alert('사진 선택 실패', '사진을 다시 선택해 주세요.');
    } finally {
      profilePickerPending.current = false;
    }
  }
  const publishTrade = ({ group, club, title, body, price, imageUri, referencePrice }: TradeDraft) => {
    const existing = screen === 'trade-edit' && post.trade ? post : undefined;
    const savedPost: Post = {
      id: existing?.id ?? nextPostId.current++, group, club, title, summary: body,
      image: { uri: imageUri }, likes: existing?.likes ?? 0, replies: existing?.replies ?? 0,
      trade: { price, referencePrice, imageUri, author: existing?.trade?.author ?? nickname },
    };
    if (existing) {
      setTradePosts(old => old.map(item => item.id === savedPost.id ? savedPost : item));
      setPost(savedPost);
    } else {
      setTradePosts(old => [savedPost, ...old]);
    }
    setGroup(group);
    setClub(club);
    setActivityFilter('전체');
    back();
  };

  const deleteTrade = () => {
    if (!post.trade) return;
    const id = post.id;
    Alert.alert('거래 글 삭제', '이 글을 삭제할까요?', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => {
        setTradePosts(old => old.filter(item => item.id !== id));
        setSaved(old => old.filter(item => item !== id));
        setLiked(old => old.filter(item => item !== id));
        setViewed(old => old.filter(item => item !== id));
        setComments(old => {
          const next = { ...old };
          delete next[id];
          return next;
        });
        setDraft('');
        back();
      } },
    ]);
  };

  const publishRecruitment = (draft: RecruitmentDraft) => {
    const item: Post = {
      id: nextPostId.current++, group: draft.group, club: draft.club, title: draft.title,
      summary: draft.conditions, image: images.stadium, likes: 0, replies: 0,
      recruitment: { ...draft, author: nickname, status: '모집 중', own: true },
    };
    setRecruitmentPosts(old => [item, ...old]);
    setGroup(item.group);
    setClub(item.club);
    setActivityFilter('전체');
    back();
  };
  const updateRecruitmentStatus = (status: RecruitmentStatus) => {
    if (!post.recruitment?.own || post.recruitment.status === status) return;
    const updated = { ...post, recruitment: { ...post.recruitment, status } };
    setRecruitmentPosts(old => old.map(item => item.id === updated.id ? updated : item));
    setPost(updated);
  };
  const openPost = (p: Post) => {
    setPost(p);
    setViewed(old => [p.id, ...old.filter(id => id !== p.id)]);
    setDraft('');
    push('detail');
  };
  const openChat = (name: string, context = samplePosts[0]) => {
    setChat(name);
    setReadRooms(old => old.includes(name) ? old : [...old, name]);
    setChatPost(context);
    setDraft('');
    push('chat');
  };
  const openMatch = (item: Post) => {
    const recruitment = item.recruitment;
    if (!recruitment || recruitment.own) return;
    if (!matches[item.id] && recruitment.status !== '모집 중') return;
    setMatches(old => old[item.id] ? old : { ...old, [item.id]: '신청 중' });
    setChatGroup(recruitment.group);
    openChat(recruitment.author, item);
  };
  const changeMatch = (id: number, expected: MatchStatus, next: MatchStatus) => {
    setMatches(old => old[id] === expected ? { ...old, [id]: next } : old);
  };
  const toggle = (key: string, initial = true) => ({
    value: settings[key] ?? initial,
    onChange: (value: boolean) => setSettings(old => ({ ...old, [key]: value })),
  });
  const flip = (ids: number[], id: number) => ids.includes(id) ? ids.filter(item => item !== id) : [...ids, id];
  const info = (title: string, message = 'FANLINK UI 데모 화면입니다.') => Alert.alert(title, message);
  const select = (title: string, options: string[]) => Alert.alert(title, undefined, [...options.map(option => ({ text: option, onPress: () => info(option) })), { text: '닫기', style: 'cancel' }]);
  const send = () => {
    const value = draft.trim();
    if (!value) return;
    if (screen === 'chat') {
      const status = matches[chatPost.id];
      if (chatPost.recruitment && (status === '거절됨' || status === '참여 취소')) return;
      const key = chatPost.recruitment ? matchRoomKey(chatPost.id) : tradeRoomKey(chat, chatPost.id);
      setMessages(old => ({ ...old, [key]: [...(old[key] ?? []), value] }));
    }
    else setComments(old => ({ ...old, [post.id]: [...(old[post.id] ?? []), value] }));
    setDraft('');
  };

  function postCard(item: Post, compact = false) {
    return <View key={item.id} style={s.card}>
      <Pressable onPress={() => openPost(item)} style={compact ? s.inline : undefined}>
        {compact ? <Image source={item.image} style={s.thumb} resizeMode={item.trade ? 'contain' : 'cover'} /> : <View style={s.inline}>
          <Avatar source={clubs[item.club].logo} size={33} /><View style={s.flex}><Text style={s.rowTitle}>{item.recruitment?.author ?? item.trade?.author ?? '라팍티켓'}  <Text style={s.link}>{clubs[item.club].name}</Text></Text><Text style={s.tiny}>{item.trade || item.recruitment?.own ? '내 게시글' : '23분 전'}</Text></View>
        </View>}
        <View style={compact ? s.flex : { marginTop: 7 }}>
          {compact && <Text style={s.link}>{item.group}</Text>}
          <Text style={s.postTitle} numberOfLines={compact ? 2 : undefined}>{item.title}</Text>
          {item.trade && <Text style={s.link}>{item.trade.price.toLocaleString('ko-KR')}원</Text>}
          <Text style={s.small} numberOfLines={compact || item.trade ? 2 : undefined}>{item.summary}</Text>
          {!!item.trade && !compact && <Image source={item.image} style={s.thumb} resizeMode="contain" />}
          {item.recruitment && <RecruitmentCard recruitment={item.recruitment} compact />}
          {!compact && !item.trade && !item.recruitment && <Text style={s.small}>💬 FANLINK 안전거래 또는 현장 전달 가능</Text>}
        </View>
      </Pressable>
      <View style={[s.between, { marginTop: 6 }]}><View style={s.inline}>
        <Pressable style={s.inline} onPress={() => setLiked(flip(liked, item.id))}><Icon name={liked.includes(item.id) ? 'heart' : 'heart-outline'} color="#ee4960" size={16} /><Text style={s.small}>{item.likes + Number(liked.includes(item.id))}</Text></Pressable>
        <Pressable style={s.inline} onPress={() => openPost(item)}><Icon name="chatbubble-outline" size={14} /><Text style={s.small}>{item.replies + (comments[item.id]?.length ?? 0)}</Text></Pressable>
      </View><IconButton name={saved.includes(item.id) ? 'bookmark' : 'bookmark-outline'} label="게시글 저장" onPress={() => setSaved(flip(saved, item.id))} /></View>
    </View>;
  }
  function linkPay(onPress: () => void) {
    return <Pressable onPress={onPress} style={[s.banner, s.payBanner]}><View><Text style={s.whiteTitle}>Ⅼ LinkPay</Text><Text style={s.whiteSmall}>앱 내 안심결제 (클린페이)</Text></View><View style={s.payBalance}><Text style={s.whiteSmall}>거래잔액</Text><Text style={s.whiteTitle}>150,000원 ›</Text></View><Icon name="wallet" color="#9ecbff" size={32} /></Pressable>;
  }
  function stats() {
    return <View style={s.stats}>{[['128', '게시글'], ['432', '댓글'], ['56', '거래내역'], ['18', '팔로워']].map(([n, label]) =>
      <Pressable key={label} style={s.stat} onPress={() => label === '게시글' ? openActivity('게시글') : label === '댓글' ? openActivity('댓글') : label === '거래내역' ? push('history') : info(label)}><Text style={s.statNumber}>{n}</Text><Text style={s.tiny}>{label}</Text></Pressable>)}</View>;
  }
  function openActivity(next: string) {
    setActivity(next);
    setActivityFilter('전체');
    push(next === '댓글' ? 'comments' : 'posts');
  }

  function home() {
    const orderedClubs = [clubs[fanClub], ...clubs.filter((_, index) => index !== fanClub)];
    return <>
      <View style={[s.banner, s.weather]}>
        <Icon name="partly-sunny" color="#fff49a" size={36} />
        <View style={s.flex}>
          <Text style={s.whiteSmall}>⌖ 구미시 산동읍</Text>
          <View style={s.weatherReading}>
            <Text style={s.weatherTemp}>17°C</Text>
            <Text style={s.whiteSmall}>구름 조금</Text>
          </View>
        </View>
        <Image source={images.weather} style={s.weatherArt} />
      </View>
      <Section title="주요 경기 일정">{orderedClubs.map(c => <View key={c.name} style={s.matchCard}>
        <View style={[s.matchHead, { backgroundColor: c.color }]}><Icon name={c.league === 'KBO' ? 'baseball-outline' : 'football-outline'} color="white" size={17} /><Text style={s.whiteSmall}>{c.name} ({c.league})</Text></View>
        <View style={s.matchBody}><View style={s.team}><Image source={c.logo} style={s.teamLogo} /><Text style={s.rowTitle}>{c.name}</Text></View><Text style={s.vs}>VS</Text>
          <View style={s.team}><Image source={c.rivalLogo} style={s.teamLogo} /><Text style={s.rowTitle}>{c.rival}</Text></View>
          <View style={s.matchTime}><Text style={s.rowTitle}>▣  {c.date}</Text><Text style={s.small}>◷  {c.time}</Text>{!!c.place && <Text style={s.small}>⌖  {c.place}</Text>}</View>
        </View>
      </View>)}</Section>
      <Section title="🔥 인기게시물" action={() => replace('community')}>
        <View style={s.panel}>
          {[...posts].sort((a, b) => b.likes - a.likes).slice(0, 3).map(item =>
            <Row key={item.id} icon="trophy-outline" label={item.title}
              hint={'좋아요 ' + item.likes + ' · 댓글 ' + item.replies} onPress={() => openPost(item)} />)}
        </View>
      </Section>
    </>;
  }
  function community() {
    const filtered = posts.filter(item => item.club === club && item.group === group);
    return <>
      <View style={[s.between, s.wrap]}>
        <Text style={[s.title, s.shrink]}>커뮤니티</Text>
        <IconButton name="create-outline" label={isRecruitmentGroup(group) ? '모집 글쓰기' : '거래 글쓰기'}
          onPress={() => push(isRecruitmentGroup(group) ? 'recruit-compose' : 'trade-compose')} />
      </View>
      <View style={s.tileRow}>{clubs.map((c, i) => <Pressable accessibilityState={{ selected: i === club }} key={c.name} onPress={() => setClub(i)} style={[s.tile, { backgroundColor: ['#eaf2fc', '#e7f6fa', '#fbecec'][i] }, i === club && s.selectedTile]}><Image source={c.logo} style={s.clubLogo} /><Text style={s.rowTitle}>{c.name}</Text></Pressable>)}</View>
      <View style={s.tileRow}>{categories.map(c => <Pressable accessibilityState={{ selected: c.name === group }} key={c.name} onPress={() => setGroup(c.name)} style={[s.tile, c.name === group && s.selectedTile]}><Icon name={c.icon} size={27} color={blue} /><Text style={s.rowTitle}>{c.name}</Text></Pressable>)}</View>
      {filtered.length ? filtered.map(item => postCard(item)) : <Empty text="이 구단의 해당 카테고리에 게시글이 없어요." />}
    </>;
  }
  function chats() {
    const index = categories.findIndex(c => c.name === chatGroup);
    const extraRooms = [
      ['라이온즈굿즈', '유니폼 아직 있어요!', '오후 1:10', 0, 1],
    ] as const;
    const list = [...chatSamples, ...extraRooms].filter(item =>
      item[4] === index && (!search || item[0].includes(search) || item[1].includes(search))
    );
    const matchingRooms = posts.filter(item => item.recruitment && item.group === chatGroup && matches[item.id]
      && (!search || item.title.includes(search) || item.recruitment.author.includes(search)));
    return <>
      <View style={s.between}><Text style={[s.title, s.shrink]}>1대1 채팅</Text><View style={s.inline}><IconButton name="search-outline" label="채팅 검색" onPress={() => setSearch(search === null ? '' : null)} /><IconButton name="create-outline" label="새 채팅" onPress={() => replace('community')} /></View></View>
      {search !== null && <TextInput placeholder="채팅방 검색" style={s.input} value={search} onChangeText={setSearch} />}
      <Pills items={categories.map(c => c.name)} value={chatGroup} onChange={setChatGroup} />
      {matchingRooms.map(item => <Pressable key={item.id} onPress={() => openMatch(item)} style={s.chatRow}>
        <Avatar source={clubs[item.club].logo} size={47} />
        <View style={s.flex}>
          <Text style={s.postTitle}>{item.title}</Text>
          <Text style={s.small} numberOfLines={2}>{messages[matchRoomKey(item.id)]?.at(-1) ?? matches[item.id]}</Text>
        </View>
        <Icon name="chevron-forward" size={14} />
      </Pressable>)}
      {list.length ? list.map(([name, message, time, unread], i) => <Pressable key={name} onPress={() => openChat(name, samplePosts[[0, 3, 6, 4][index]])} style={s.chatRow}>
        <Avatar source={[images.avatar, images.samsung, images.cap, images.stadium][i % 4]} size={47} />
        <View style={s.flex}><Text style={s.postTitle}>{name}</Text><Text style={s.small} numberOfLines={1}>{messages[tradeRoomKey(name, samplePosts[[0, 3, 6, 4][index]].id)]?.at(-1) ?? message}</Text></View>
        <View style={s.chatMeta}><Text style={s.tiny}>{time}</Text>{!!unread && !readRooms.includes(name) && <Text style={s.unread}>{unread}</Text>}</View>
      </Pressable>) : !matchingRooms.length && <Empty text="채팅 내역이 없어요." />}
    </>;
  }
  function my() {
    return <>
      <Text style={s.title}>마이페이지</Text>
      <View style={[s.banner, s.profileBanner]}><Image source={images.profileArt} style={s.profileArt} /><View style={s.between}><View style={[s.inline, s.flex]}><Avatar size={54} source={profileImage} /><View style={s.flex}><Text style={s.whiteTitle}>{nickname}{fanVerified ? ' ☑' : ''}</Text><Text style={s.whiteSmall}>{clubs[fanClub].name} 팬 · 매너온도 36.5°C (샘플)</Text></View></View><Pill label="프로필 편집" onPress={() => push('profile')} /></View>
        <View style={s.profileCounts}>{[['128', '게시글'], ['432', '댓글'], ['56', '거래내역'], ['18', '팔로워']].map(([number, name]) => <View key={name} style={s.stat}><Text style={s.whiteTitle}>{number}</Text><Text style={s.whiteSmall}>{name}</Text></View>)}</View>
      </View>
      {linkPay(() => push('cleanpay'))}
      <Section title="나의 활동"><View style={s.tileRow}>{([
        ['내 게시글', 'create-outline', () => openActivity('게시글')],
        ['내 댓글', 'chatbubble-ellipses-outline', () => openActivity('댓글')],
        ['거래/정산 내역', 'swap-horizontal-outline', () => push('history')],
        ['최근 본 글', 'time-outline', () => openActivity('최근 본 글')],
      ] as [string, IconName, () => void][]).map(([label, icon, fn]) => <Pressable key={label} style={s.tile} onPress={fn}><Icon name={icon} color={blue} size={20} /><Text style={s.tiny}>{label}</Text></Pressable>)}</View></Section>
      <View style={s.panel}>
        <Row icon="people-outline" label="참여 중인 모임" value={String(Object.values(matches).filter(status => status === '참여 확정').length)}
          onPress={() => push('joined')} />
      </View>
      <Section title="계정 관리"><View style={s.panel}>
        <Row icon="person-outline" label="내 프로필" value="닉네임, 소개, 관심팀 설정" onPress={() => push('profile')} />
        <Row icon="shield-checkmark-outline" label="계정 및 보안" value="비밀번호, 로그인 관리" onPress={() => push('security')} />
        <Row icon="card-outline" label="결제수단 관리" value="클린페이 센터" onPress={() => push('cleanpay')} />
        <Row icon="notifications-outline" label="알림 설정" value="새 댓글, 거래, 서비스 소식 알림" onPress={() => push('notifications')} />
        <Row icon="settings-outline" label="환경 설정" value="테마, 언어, 앱 설정" onPress={() => push('settings')} />
      </View></Section>
      <Section title="고객센터"><View style={s.panel}>
        <Row icon="help-circle-outline" label="문의하기" value="서비스 이용 문의 및 신고" onPress={() => push('help')} />
        <Row icon="book-outline" label="이용 가이드" value="FANLINK 사용법과 규칙" onPress={() => push('guide')} />
        <Row icon="megaphone-outline" label="공지사항" value="업데이트, 이벤트, 점검 안내" onPress={() => push('notices')} />
      </View></Section>
    </>;
  }

  function detail() {
    return <>
      <Text style={s.link}>{post.group}   ·   {clubs[post.club].name} ›</Text>
      <Text style={s.detailTitle}>{post.title}</Text>
      <View style={s.between}><View style={[s.inline, s.flex]}><Avatar size={35} /><View style={s.flex}><Text style={s.rowTitle}>{post.recruitment?.author ?? post.trade?.author ?? '라팍티켓'} {!post.trade && !post.recruitment && <Text style={s.link}>☑ Lv.4</Text>}</Text><Text style={s.tiny}>{post.trade || post.recruitment?.own ? '내 게시글' : '23분 전 · 조회 312'}</Text></View></View>{!post.trade && !post.recruitment && <Pill label={following ? '팔로잉' : '팔로우'} selected={following} onPress={() => setFollowing(!following)} />}</View>
      {post.recruitment && <RecruitmentCard recruitment={post.recruitment} />}
      <View style={s.detailCopy}>
        {!post.trade && post.group === '티켓' && <Text style={s.body}>🗓 경기정보 · {post.title.replace('[정가양도] ', '')}{'\n'}⏰ 일시 · 11/8 (토) 17:00{'\n'}📍 장소 · 대구 삼성라이온즈파크</Text>}
        {post.trade && <>
          <Text style={s.postTitle}>판매 희망가 · {post.trade.price.toLocaleString('ko-KR')}원</Text>
          <Text style={s.small}>검증 기준 데모 정가 · {post.trade.referencePrice.toLocaleString('ko-KR')}원</Text>
        </>}
        <Text style={s.body}>{post.summary}</Text>
        {!post.trade && !post.recruitment && <>
          <Text style={s.body}>💬 거래방법 · FANLINK 안전거래 또는 현장 전달 가능</Text>
          <Text style={s.body}>개인 사정으로 못 가게 되어 양도합니다! 관심 있으신 분은 편하게 1:1 채팅 주세요. 🙏</Text>
        </>}
      </View>
      {!post.recruitment?.own && <View style={s.detailImageFrame}>
        <Image source={post.image} style={s.detailImage} resizeMode="contain" />
      </View>}
      <View style={[s.between, s.wrap]}>
        <View style={s.inline}><Pill label={String(post.likes + Number(liked.includes(post.id)))} icon={liked.includes(post.id) ? 'heart' : 'heart-outline'} onPress={() => setLiked(flip(liked, post.id))} /><Pill label={String(post.replies + (comments[post.id]?.length ?? 0))} icon="chatbubble-outline" onPress={() => scroller.current?.scrollToEnd()} /></View>
        {!post.trade && !post.recruitment && <Pill label="1:1 채팅" icon="chatbubble-outline"
          onPress={() => openChat(post.group === '굿즈' ? '라이온즈굿즈' : '라팍티켓', post)} />}
        {post.recruitment && !post.recruitment.own && (matches[post.id] || post.recruitment.status === '모집 중')
          && <Pill label={matches[post.id] ? '매칭 채팅 열기' : '1:1 매칭 신청'} icon="chatbubble-outline" onPress={() => openMatch(post)} />}
        {post.recruitment && !post.recruitment.own && !matches[post.id] && post.recruitment.status !== '모집 중'
          && <Text style={[s.small, s.shrink]}>이 모집 글은 신청이 마감됐어요.</Text>}
      </View>
      {post.recruitment?.own && <View style={[s.inline, s.wrap]}>
        {(['모집 중', '모집 완료', '취소'] as const).map(status =>
          <Pill key={status} label={status} selected={post.recruitment?.status === status}
            onPress={() => updateRecruitmentStatus(status)} />)}
      </View>}
      {post.trade && <View style={[s.inline, s.wrap]}>
        <Pill label="글 수정" icon="create-outline" onPress={() => push('trade-edit')} />
        <Pill label="글 삭제" icon="trash-outline" onPress={deleteTrade} />
      </View>}
      <Section title={'댓글 ' + (post.replies + (comments[post.id]?.length ?? 0))}>
        {[...(post.trade || post.recruitment ? [] : ['아직 구매 가능할까요?', '혹시 현장 수령만 가능한가요?']), ...(comments[post.id] ?? [])].map((value, i) => <View key={i} style={s.comment}><Avatar size={31} source={i === 0 ? images.samsung : images.avatar} /><View style={s.flex}><Text style={s.rowTitle}>{!post.trade && !post.recruitment && i === 0 ? '삼성팬⚾' : !post.trade && !post.recruitment && i === 1 ? '라이온즈사랑' : nickname}</Text><Text style={s.body}>{value}</Text><Pressable onPress={() => setDraft(!post.trade && !post.recruitment && i < 2 ? '@' + (i === 0 ? '삼성팬 ' : '라이온즈사랑 ') : '')}><Text style={s.tiny}>답글쓰기</Text></Pressable></View><Icon name="heart" size={13} color="#f25165" /></View>)}
      </Section>
    </>;
  }
  function matchingChat() {
    const status = matches[chatPost.id];
    return <>
      <Text style={s.dateChip}>오늘</Text>
      <View style={s.card}>
        <Text style={s.postTitle}>{chat} · 매칭 채팅</Text>
        <Text style={s.body}>현재 상태 · {status}</Text>
        <Text style={s.small}>출발 시간과 장소, 참여 조건을 채팅으로 조율해요.</Text>
        {status === '신청 중' && <>
          <Text style={s.small}>데모에서 상대방의 응답을 선택할 수 있어요.</Text>
          <View style={[s.inline, s.wrap]}>
            <Pill label="호스트 동의 (데모)" onPress={() => changeMatch(chatPost.id, status, '호스트 동의')} />
            <Pill label="호스트 거절 (데모)" onPress={() => changeMatch(chatPost.id, status, '거절됨')} />
          </View>
        </>}
      </View>
      {(messages[matchRoomKey(chatPost.id)] ?? []).map((message, i) =>
        <View key={i} style={[s.messageRow, s.messageMine]}>
          <Text style={[s.bubble, s.bubbleMine]}>{message}</Text>
        </View>)}
    </>;
  }

  function matchActions(status: MatchStatus) {
    const id = chatPost.id;
    if (status === '참여 취소' || status === '거절됨') {
      return <View style={s.matchActions}><Text style={s.small}>매칭이 종료됐어요 · {status}</Text></View>;
    }
    return <View style={s.matchActions}>
      {status === '신청 중' && <Text style={[s.small, s.shrink]}>호스트 동의 대기 중</Text>}
      {status === '호스트 동의' && <Pill label="나도 동의하기" selected onPress={() => changeMatch(id, status, '상호 동의')} />}
      {status === '상호 동의' && <Pill label="참여 확정" selected onPress={() => changeMatch(id, status, '참여 확정')} />}
      {status === '참여 확정' && <Pill label="참여 중인 모임" selected onPress={() => setStack(['my', 'joined'])} />}
      <Pill label="참여 취소" onPress={() => Alert.alert('참여 취소', '이 모임 참여를 취소할까요?', [
        { text: '닫기', style: 'cancel' },
        { text: '참여 취소', style: 'destructive', onPress: () => changeMatch(id, status, '참여 취소') },
      ])} />
    </View>;
  }

  function joined() {
    const confirmed = posts.filter(item => matches[item.id] === '참여 확정');
    if (!confirmed.length) return <Empty text="확정된 모임이 없어요." />;
    return confirmed.map(item => <View key={item.id} style={s.section}>
      {postCard(item, true)}
      <View style={s.endButton}><Pill label="매칭 채팅 열기" onPress={() => openMatch(item)} /></View>
    </View>);
  }

  function tradeCard() {
    const key = tradeRoomKey(chat, chatPost.id);
    const payment = payments[key];
    const price = sampleTradeAmounts[chatPost.id];
    const fee = chatPost.group === '티켓' ? 500 : 0;
    return <View style={[s.card, s.tradeCard]}>
      <Pressable accessibilityRole="button" style={[s.inline, s.wrap]} onPress={() => openPost(chatPost)}>
        <Image source={chatPost.image} style={s.thumb} resizeMode="contain" />
        <View style={s.flex}>
          <Text style={s.link}>안전거래 · {chatPost.group}</Text>
          <Text style={s.postTitle}>{chatPost.title}</Text>
        </View>
        <Icon name="chevron-forward" size={14} />
      </Pressable>
      <Text style={s.small}>{payment?.status ?? (price ? '결제 대기' : '가격 정보 없음')} · 데모</Text>
      {!!price && <Text style={s.small}>판매가 {won(price)}{fee ? ' · 안심 수수료 ' + won(fee) : ''} · 결제액 {won(price + fee)}</Text>}
    </View>;
  }

  function tradeActions() {
    const key = tradeRoomKey(chat, chatPost.id);
    const payment = payments[key];
    const price = sampleTradeAmounts[chatPost.id];
    if (!price) return null;
    const amount = price + (chatPost.group === '티켓' ? 500 : 0);
    if (payment?.status === '구매 확정') {
      return <View style={s.matchActions}>
        <Text style={[s.small, s.shrink]}>구매 확정 완료</Text>
        <Pill label="클린페이 보기" onPress={() => push('cleanpay')} />
      </View>;
    }
    return <View style={s.matchActions}>
      <Pill label={payment ? '수령 확인 및 구매 확정' : '결제하기'} selected onPress={() => {
        if (payment) {
          Alert.alert('구매 확정', '상품을 수령했나요? 데모 거래를 구매 확정으로 변경합니다.', [
            { text: '취소', style: 'cancel' },
            { text: '구매 확정', onPress: () => setPayments(old => old[key]?.status === '대금 보관'
              ? { ...old, [key]: { ...old[key], status: '구매 확정' } } : old) },
          ]);
        } else {
          Alert.alert('데모 결제', won(amount) + ' 결제를 시연할까요?', [
            { text: '취소', style: 'cancel' },
            { text: '결제', onPress: () => setPayments(old => old[key] ? old : {
              ...old, [key]: { title: chatPost.title, amount, status: '대금 보관' },
            }) },
          ]);
        }
      }} />
    </View>;
  }

  function chatRoom() {
    const key = tradeRoomKey(chat, chatPost.id);
    const payment = payments[key];
    const ticket = chatPost.group === '티켓';
    return <>
      <View style={[s.inline, s.wrap]}><Avatar /><View style={s.flex}>
        <Text style={s.postTitle}>{chat}</Text>
        <Text style={s.tiny}>최근 접속 2분 전</Text>
      </View></View>
      <Text style={s.dateChip}>오늘</Text>
      {(chatPost.id === 1 ? [
        '안녕하세요!\n티켓 아직 구매 가능할까요?',
        '네! 아직 2연석 모두 가능합니다!',
        '다행이네요!\n가격 그대로 52,000원 맞을까요?',
        '네 맞습니다!\nFANLINK 링크페이로 결제하시면 돼요.\n결제 링크 보내드릴게요!',
      ] : ticket ? ['안녕하세요! 티켓 아직 구매 가능할까요?', '네! FANLINK 링크페이로 결제하시면 돼요.']
        : ['안녕하세요! 굿즈 아직 구매 가능할까요?', '네! 수령 방법을 채팅으로 맞춰요.']).map((message, i) => {
        const mine = i % 2 === 1;
        return <View key={i} style={[s.messageRow, mine ? s.messageMine : s.messageTheirs]}>
          {!mine && <Avatar size={28} />}
          <Text style={[s.bubble, mine && s.bubbleMine]}>{message}</Text>
          <Text style={[s.tiny, s.shrink]}>오후 5:{24 + i}</Text>
        </View>;
      })}
      {payment && <View style={s.card}>
        <Text style={s.postTitle}>LinkPay · {payment.status}</Text>
        <Text style={s.small}>{payment.status === '구매 확정' ? '데모 구매 확정을 완료했어요.'
          : ticket ? '외부 예매 서비스에서 스마트티켓을 받은 뒤 구매를 확정해 주세요.'
            : '굿즈를 받은 뒤 구매를 확정해 주세요.'}</Text>
      </View>}
      {(messages[key] ?? []).map((message, i) =>
        <View key={i} style={[s.messageRow, s.messageMine]}>
          <Text style={[s.bubble, s.bubbleMine]}>{message}</Text>
        </View>)}
    </>;
  }

  function profile() {
    return <>
      <Text style={s.title}>내 프로필</Text>
      <Image source={images.profileArt} style={s.profileCover} />
      <View style={s.between}><View style={[s.inline, s.flex, { marginTop: -17 }]}><Avatar size={66} source={profileImage} /><View style={s.flex}><Text style={s.detailTitle}>{nickname}{fanVerified ? ' ☑' : ''}</Text><Text style={s.small}>{clubs[fanClub].name} 팬 · 매너온도 36.5°C (샘플)</Text></View></View><Pill label={editing ? '완료' : '프로필 편집'} icon="create-outline" onPress={() => setEditing(!editing)} /></View>
      {editing && <View style={s.section}>
        <TextInput accessibilityLabel="닉네임" style={s.input} value={nickname} maxLength={24} onChangeText={setNickname} />
        <View style={[s.inline, s.wrap]}><Pill label="프로필 사진 변경" icon="image-outline" onPress={pickProfileImage} /></View>
      </View>}
      <Section title="응원 구단과 팬 인증">
        <Pills items={clubs.map(item => item.name)} value={clubs[fanClub].name} onChange={name => {
          const next = clubs.findIndex(item => item.name === name);
          if (next !== fanClub) { setFanClub(next); setFanVerified(false); }
        }} />
        <View style={[s.inline, s.wrap]}>
          <Pill label={fanVerified ? '팬 인증 완료 (데모)' : '팬 인증하기 (데모)'} selected={fanVerified}
            onPress={() => setFanVerified(true)} />
        </View>
      </Section>
      {stats()}
      <Section title="소개"><View style={s.panel}><Text style={s.padded}>삼성 라이온즈를 사랑하는 야구팬입니다!{'\n'}주로 직관 티켓 거래하고, 굿즈도 가끔 올려요⚾💙</Text></View></Section>
      <Section title="활동 요약" action={() => openActivity('게시글')}><View style={s.tileRow}>{(['거래 완료', '거래 중', '관심 게시물', '저장한 글']).map((label, i) => <Pressable key={label} style={s.tile} onPress={() => i < 2 ? push('history') : openActivity('저장한 글')}><Icon name={(['ticket-outline', 'swap-horizontal-outline', 'star-outline', 'bookmark-outline'] as const)[i]} color={blue} /><Text style={s.postTitle}>{[12, 8, 56, 24][i]}</Text><Text style={s.tiny}>{label}</Text></Pressable>)}</View></Section>
      <Section title="획득 배지"><View style={s.tileRow}>{['직관러', '신뢰거래', '소통왕', '티켓 마스터', '활동왕'].map((label, i) => <Pressable key={label} style={s.stat} onPress={() => info(label)}><View style={s.badge}><Icon name={(['trophy', 'shield-checkmark', 'people', 'ticket', 'star'] as const)[i]} color="white" size={20} /></View><Text style={s.tiny}>{label}</Text></Pressable>)}</View></Section>
      <Section title="최근 게시글" action={() => openActivity('게시글')}><View style={s.tileRow}>{myPosts.map(item => <Pressable key={item.id} style={s.flex} onPress={() => openPost(item)}><Image source={item.image} style={s.recent} /><Text style={s.tiny} numberOfLines={2}>{item.title}</Text></Pressable>)}</View></Section>
    </>;
  }
  function security() {
    return <>
      <Banner title={nickname + (fanVerified ? ' ☑' : '')} hint={clubs[fanClub].name + ' 팬'} icon="shield-checkmark" />
      <Section title="계정 정보"><View style={s.panel}>
        <Row icon="person-outline" label="닉네임" value={nickname} onPress={() => { setEditing(true); push('profile'); }} />
        <Row icon="id-card-outline" label="아이디" value="fanlink_2471" onPress={() => info('아이디', 'fanlink_2471')} />
        <Row icon="mail-outline" label="이메일" value="fanlink@email.com" onPress={() => info('이메일', 'fanlink@email.com')} />
        <Row icon="phone-portrait-outline" label="휴대폰 번호" value="010-1234-5678" onPress={() => info('휴대폰 번호', '010-1234-5678')} />
      </View></Section>
      <Section title="보안 설정"><View style={s.panel}>
        <Row icon="lock-closed-outline" label="비밀번호 변경" hint="계정 비밀번호를 변경해요." onPress={() => info('비밀번호 변경')} />
        <Row icon="shield-checkmark-outline" label="2단계 인증 (OTP)" hint="로그인 시 추가 인증으로 계정을 보호해요." toggle={toggle('otp')} />
        <Row icon="phone-portrait-outline" label="로그인 기기 관리" hint="내 계정으로 로그인한 기기를 확인해요." onPress={() => info('로그인 기기 관리', '현재 기기에서 접속 중이에요.')} />
      </View></Section>
      <Section title="결제 및 연동"><View style={s.panel}><Row icon="card-outline" label="결제수단 관리" hint="클린페이에서 결제·정산 계좌 확인" onPress={() => push('cleanpay')} /><Row icon="link-outline" label="연동 계정 관리" hint="SNS 계정 연동을 관리할 수 있어요." onPress={() => info('연동 계정 관리')} /></View></Section>
      <Section title="계정 관리"><View style={s.panel}><Row icon="download-outline" label="데이터 다운로드" hint="내 활동 데이터를 확인해요." onPress={() => info('데이터 다운로드')} /><Row icon="log-out-outline" label="로그아웃" onPress={logout} /><Row icon="person-remove-outline" label="계정 탈퇴" danger onPress={() => info('계정 탈퇴')} /></View></Section>
    </>;
  }
  function notifications() {
    const master = {
      value: settings['알림 전체'] ?? true,
      onChange: (value: boolean) => {
        const labels = ['푸시 알림', '이메일 알림', '거래 관련 알림', '소통 알림', '관심 품목 알림', '공지 및 소식', '결제 및 계정 알림'];
        setSettings(old => ({ ...old, '알림 전체': value, ...Object.fromEntries(labels.map(label => [label, value])) }));
      },
    };
    return <>
      <View style={s.banner}><Icon name="notifications-outline" size={27} color="white" /><View style={s.flex}><Text style={s.whiteTitle}>알림 전체 관리</Text><Text style={s.whiteSmall}>모든 서비스 알림을 한 번에 설정해요.</Text></View><Switch accessibilityLabel="알림 전체 관리" value={master.value} onValueChange={master.onChange} /></View>
      <Section title="알림 방식"><View style={s.panel}>{([['푸시 알림', '앱 푸시로 실시간 알림을 받아요.', 'phone-portrait-outline'], ['이메일 알림', '주요 소식을 이메일로 받아요.', 'mail-outline']] as [string, string, IconName][]).map(([label, hint, icon]) => <Row key={label} label={label} hint={hint} icon={icon} toggle={toggle(label, label === '푸시 알림')} />)}</View></Section>
      <Section title="알림 항목"><View style={s.panel}>{([
        ['거래 관련 알림', '구매·판매, 정산, 거래 상태 변경', 'ticket-outline'], ['소통 알림', '댓글, 답글, 1대1 채팅 메시지', 'chatbubble-outline'],
        ['관심 품목 알림', '내 게시글, 찜, 키워드에 대한 관심 알림', 'star-outline'], ['공지 및 소식', '서비스 공지, 이벤트 및 업데이트', 'megaphone-outline'],
        ['결제 및 계정 알림', '송금, 출금, 결제 및 계정 보안 알림', 'card-outline'],
      ] as [string, string, IconName][]).map(([label, hint, icon]) => <Row key={label} label={label} hint={hint} icon={icon} toggle={toggle(label)} />)}</View></Section>
      <Section title="추가 설정"><View style={s.panel}><Row icon="moon-outline" label="방해 금지 시간" hint="지정한 시간에는 알림을 받지 않아요." value="오후 11:00 ~ 오전 7:00" onPress={() => info('방해 금지 시간')} /><Row icon="phone-portrait-outline" label="진동 알림" toggle={toggle('진동')} /><Row icon="volume-high-outline" label="알림음" value="기본 알림음" onPress={() => select('알림음', ['기본 알림음', '무음'])} /></View></Section>
    </>;
  }
  function preferences() {
    return <>
      <Section title="화면 설정"><View style={s.panel}>
        <Row icon="moon-outline" label="테마" hint="앱 화면 모드를 선택해요." /><Pills items={['라이트', '다크', '시스템']} value={theme} onChange={setTheme} />
        <Row icon="color-palette-outline" label="앱 테마 색상" hint="강조 색상을 선택해요." />
        <View style={s.swatches}>{[blue, '#8b6be7', '#ed6f9c', '#5bbb97', '#ffa53b'].map((color, i) => <Pressable key={color} accessibilityLabel={'테마 색상 ' + (i + 1)} accessibilityState={{ selected: i === accent }} onPress={() => setAccent(i)} style={[s.swatch, { backgroundColor: color, borderColor: i === accent ? '#24334d' : 'white' }]} />)}</View>
        <Row icon="text-outline" label="글자 크기" value={['작게', '보통', '크게'][font]} />
        <View style={s.swatches}><Pill label="−" onPress={() => setFont(Math.max(0, font - 1))} /><Text style={[s.body, { fontSize: 12 + font * 2 }]}>미리보기</Text><Pill label="＋" onPress={() => setFont(Math.min(2, font + 1))} /></View>
      </View></Section>
      <Section title="언어"><View style={s.panel}><Row icon="globe-outline" label="언어" hint="앱의 표시 언어를 변경해요." value="한국어" onPress={() => select('언어', ['한국어'])} /></View></Section>
      <Section title="데이터 및 저장공간"><View style={s.panel}><Row icon="wifi-outline" label="모바일 데이터 사용" hint="모바일 데이터로 콘텐츠를 자동으로 불러와요." toggle={toggle('모바일 데이터')} /><Row icon="trash-outline" label="캐시 데이터 관리" value="52.3MB" onPress={() => info('캐시 데이터 관리')} /></View></Section>
      <Section title="기타"><View style={s.panel}><Row icon="grid-outline" label="홈 화면 메뉴 설정" onPress={() => push('empty')} /><Row icon="layers-outline" label="앱 정보" value="버전 1.4.0" onPress={() => info('FANLINK', '디자인 예시 버전 1.4.0')} /><Row icon="refresh-outline" label="설정 초기화" onPress={() => { setTheme('라이트'); setAccent(0); setFont(1); setSettings({}); }} /></View></Section>
    </>;
  }
  function activities() {
    const items = activity === '저장한 글' ? posts.filter(item => saved.includes(item.id))
      : activity === '최근 본 글' ? viewed.flatMap(id => posts.filter(item => item.id === id))
      : activity === '게시글' ? ownPosts : myPosts;
    const filtered = items.filter(item => activityFilter === '전체' || item.group === activityFilter);
    return <>
      <Pills items={['게시글', '댓글', '거래내역', '저장한 글']} value={activity} onChange={value => value === '거래내역' ? push('history') : openActivity(value)} />
      <Pills items={['전체', ...categories.map(item => item.name)]} value={activityFilter} onChange={setActivityFilter} />
      {filtered.length ? filtered.map((item, i) => activity === '댓글' ? <View key={item.id} style={s.card}><Text style={s.body}>◌  {['네! 아직 2연석 모두 가능합니다!', '거래 방법은 링크페이로 진행하시면 됩니다!', '저도 참여하고 싶어요! 2명 가능합니다!', '혹시 사이즈 알 수 있을까요?'][i]}</Text><Pressable style={[s.inline, s.commentPreview]} onPress={() => openPost(item)}><Image source={item.image} style={s.thumb} /><View style={s.flex}><Text style={s.link}>{item.group}</Text><Text style={s.postTitle} numberOfLines={2}>{item.title}</Text><Text style={s.tiny}>{item.summary}</Text></View></Pressable></View> : postCard(item, true)) : <Empty />}
      {activity === '게시글' && <View style={[s.inline, s.wrap]}>
        <Pill label="거래 글쓰기" icon="create-outline" onPress={() => push('trade-compose')} />
        <Pill label="모집 글쓰기" icon="create-outline" onPress={() => push('recruit-compose')} />
      </View>}
    </>;
  }
  function paymentRows() {
    const rows = Object.entries(payments).reverse();
    return rows.length ? rows.map(([key, item]) => <View key={key} style={[s.card, s.tradeLedger]}>
      <View style={s.flex}><Text style={s.rowTitle}>{item.title}</Text><Text style={s.small}>{item.status}</Text></View>
      <Text style={[s.link, s.shrink]}>−{won(item.amount)}</Text>
    </View>) : <Empty text="아직 이 앱에서 진행한 데모 거래가 없어요." />;
  }

  function cleanPay() {
    const held = Object.values(payments).reduce((sum, item) => item.status === '대금 보관' ? sum + item.amount : sum, 0);
    return <>
      <View style={[s.banner, s.cleanPayBanner]}>
        <Text style={s.whiteTitle}>LinkPay · 안심결제</Text>
        <Text style={s.whiteSmall}>임시 보관 잔액 (샘플 포함)</Text>
        <Text style={s.whiteTitle}>{won(150000 + held)}</Text>
        <Text style={s.whiteSmall}>정산 예정 40,000원 · 출금 가능 110,000원 (샘플)</Text>
      </View>
      <Section title="계좌 관리"><View style={s.panel}>
        <Row icon="card-outline" label="정산 계좌 (샘플)" hint="카카오뱅크 3333-**-****567" />
      </View></Section>
      <Section title="이번 실행의 거래" action={stack.at(-2) === 'history' ? back : () => push('history')}>{paymentRows()}</Section>
    </>;
  }

  function history() {
    const rows = [
      { label: samplePosts[0].title, kind: '구매', amount: '-52,000원', image: images.stadium },
      { label: '판매 정산 입금', kind: '정산(입금)', amount: '+85,000원', image: undefined },
      { label: samplePosts[3].title, kind: '구매', amount: '-50,000원', image: images.jersey },
      { label: '링크페이 충전', kind: '충전/출금', amount: '+100,000원', image: undefined },
      { label: '[판매] 삼성 vs LG (11/1)', kind: '판매', amount: '+85,000원', image: images.stadium },
    ];
    return <>
      <Pills items={['전체', '구매', '판매', '정산(입금)', '충전/출금']} value={historyFilter} onChange={setHistoryFilter} />
      {linkPay(() => push('cleanpay'))}
      {(historyFilter === '전체' || historyFilter === '구매') && Object.keys(payments).length > 0
        && <Section title="이번 실행의 거래">{paymentRows()}</Section>}
      <View style={[s.panel, s.between]}><IconButton name="chevron-back" label="이전 달" onPress={() => setMonth(Math.max(1, month - 1))} /><Text style={s.postTitle}>2025년 {month}월 ⌄</Text><IconButton name="chevron-forward" label="다음 달" onPress={() => setMonth(Math.min(12, month + 1))} /></View>
      <Section title={month + '월 거래 요약'}><View style={[s.panel, s.stats]}>{[['구매', '-102,000원'], ['판매', '+85,000원'], ['정산(입금)', '+85,000원']].map(([label, value]) => <View key={label} style={s.stat}><Text style={s.small}>{label}</Text><Text style={s.link}>{month === 11 ? value : '0원'}</Text></View>)}</View></Section>
      <Section title="거래 내역">{month === 11 ? rows.filter(item => historyFilter === '전체' || item.kind === historyFilter).map(item => <Pressable key={item.label} style={[s.card, s.inline]} onPress={() => info(item.kind, item.label + '\n' + item.amount)}>{item.image ? <Image source={item.image} style={s.thumb} /> : <View style={s.squareIcon}><Icon name="wallet-outline" color={blue} /></View>}<View style={s.flex}><Text style={s.link}>{item.kind}</Text><Text style={s.rowTitle}>{item.label}</Text><Text style={s.tiny}>2025.11.08 17:02</Text></View><View><Text style={s.link}>{item.amount}</Text><Text style={s.complete}>거래완료</Text></View></Pressable>) : <Empty text="이 달의 거래 내역이 없어요." />}</Section>
    </>;
  }
  function support() {
    if (screen === 'notices') {
      const notices = [
        ['공지', '개인정보처리방침 개정 안내', '2026.11.03'], ['공지', 'FANLINK 서비스 이용 약관 변경 안내', '2026.10.28'],
        ['업데이트', 'FANLINK v1.4.0 업데이트 안내', '2026.10.20'], ['이벤트', '가을 시즌 예매 이벤트 안내 🎁', '2026.10.15'],
        ['공지', '고객센터 운영시간 안내', '2026.10.10'],
      ];
      return <>
        <Banner title="FANLINK v1.4.0 업데이트 안내" hint="더 편리한 예매와 향상된 사용성을 만나보세요!" icon="megaphone"><View style={s.bannerAction}><Button label="자세히 보기 ›" color="white" textColor={blue} onPress={() => setExpanded(expanded === notices[2][1] ? null : notices[2][1])} /></View></Banner>
        <Pills items={['전체', '공지', '업데이트', '이벤트']} value={noticeFilter} onChange={setNoticeFilter} />
        {notices.filter(([type]) => noticeFilter === '전체' || noticeFilter === type).map(([type, title, date]) => <View key={title} style={s.panel}><Row icon={type === '이벤트' ? 'gift-outline' : type === '업데이트' ? 'refresh-outline' : 'megaphone-outline'} label={title} hint={type + '  ·  ' + date} onPress={() => setExpanded(expanded === title ? null : title)} />{expanded === title && <Text style={s.padded}>FANLINK의 {type} 소식입니다. 이용해 주셔서 감사합니다.</Text>}</View>)}
        <Button label="더 많은 공지 보기 ⌄" color="#e9edf7" textColor={ink} onPress={() => info('공지사항', '표시할 추가 공지사항이 없어요.')} />
      </>;
    }
    const guide = screen === 'guide';
    const items: [string, string, IconName][] = guide ? [
      ['티켓 예매하기', '경기 일정 확인부터 예매까지 안내해요.', 'ticket-outline'],
      ['결제 및 환불', '다양한 결제수단과 환불 정책을 확인해요.', 'card-outline'],
      ['이용내역 확인', '예매·거래·환불 내역을 확인해요.', 'receipt-outline'],
      ['알림 설정', '예매 정보, 공지 등 필요한 알림만 받아요.', 'notifications-outline'],
      ['내 프로필 관리', '계정 정보, 보안, 결제수단 등을 관리해요.', 'person-outline'],
    ] : [
      ['서비스 이용 문의', '앱 사용 방법, 기능 관련 문의', 'chatbubble-ellipses-outline'],
      ['결제·환불 문의', '결제, 환불, 정산, 결제 관련 문의', 'card-outline'],
      ['오류·장애 신고', '앱 오류, 접속 문제, 버그 신고', 'warning-outline'],
      ['계정·보안 문의', '로그인, 계정 보호, 보안 관련 문의', 'shield-outline'],
      ['기능 제안', '기능 개선, 서비스 관련 제안', 'bulb-outline'],
      ['기타 문의', '그 외 궁금한 사항 문의', 'ellipsis-horizontal-circle-outline'],
    ];
    const faq = guide ? ['티켓 예매는 어떻게 하나요?', '예매한 티켓은 어디서 확인하나요?', '결제수단은 어떻게 등록하나요?', '예매를 취소하고 환불받고 싶어요.', '계정 정보를 변경하고 싶어요.'] :
      ['충전은 어떻게 하나요?', '출금은 언제 처리되나요?', '결제 내역은 어디서 확인할 수 있나요?', '계정 비밀번호를 잊어버렸어요.', '환불은 어떻게 진행되나요?'];
    const toggleArticle = (label: string) => setExpanded(expanded === label ? null : label);
    return <>
      <Banner title={guide ? 'FANLINK 사용법 한눈에 보기' : '빠르고 친절한 고객지원'} hint={guide ? '3분이면 기본 사용법을 익힐 수 있어요.' : '평균 답변 시간 24시간 이내'} icon={guide ? 'phone-portrait' : 'headset'}>{guide && <View style={s.bannerAction}><Button label="▶ 시작 가이드 영상 보기" color="white" textColor={blue} onPress={() => toggleArticle('티켓 예매하기')} /></View>}</Banner>
      <Section title={guide ? '기능별 이용가이드' : '문의 유형 선택'}><View style={s.panel}>{items.map(([label, hint, icon]) => <View key={label}><Row label={label} hint={hint} icon={icon} onPress={() => toggleArticle(label)} />{expanded === label && <Text style={s.padded}>{hint}</Text>}</View>)}</View></Section>
      <Section title={guide ? '자주 찾는 도움말' : '자주 묻는 질문 (FAQ)'} action={() => toggleArticle(faq[0])}><View style={s.panel}>{faq.map(label => <View key={label}><Row label={label} icon="help-circle-outline" onPress={() => toggleArticle(label)} />{expanded === label && <Text style={s.padded}>마이페이지에서 관련 메뉴를 확인하거나 고객센터에 문의해 주세요.</Text>}</View>)}</View></Section>
      <View style={[s.between, s.wrap]}>{!guide && <Pill label="내 문의 내역" icon="receipt-outline" onPress={() => info('내 문의 내역', '등록된 문의가 없어요.')} />}<Button label="✎  1:1 문의하기" onPress={() => guide ? push('help') : info('1:1 문의하기', '위 목록에서 문의 유형을 선택해 주세요.')} /></View>
    </>;
  }
  const isTab = tabScreens.includes(screen);
  const matchingStatus = screen === 'chat' && chatPost.recruitment ? matches[chatPost.id] : undefined;
  const isComposer = screen === 'detail' || (screen === 'chat' && matchingStatus !== '거절됨' && matchingStatus !== '참여 취소');
  const scroller = React.useRef<ScrollView>(null);
  const editDraft: TradeDraft | undefined = screen === 'trade-edit' && post.trade
    && (post.group === '티켓' || post.group === '굿즈') ? {
      group: post.group, club: post.club, title: post.title, body: post.summary,
      price: post.trade.price, imageUri: post.trade.imageUri, referencePrice: post.trade.referencePrice,
    } : undefined;
  const content = screen === 'recruit-compose' ? (
    <RecruitmentComposer initialGroup={isRecruitmentGroup(group) ? group : '직관메이트'} initialClub={club}
      onPublish={publishRecruitment} />
  ) : screen === 'joined' ? joined() : screen === 'trade-compose' || screen === 'trade-edit' ? (
    <TradeComposer initialGroup={group === '굿즈' ? '굿즈' : '티켓'} initialClub={club}
      initialDraft={editDraft} onPublish={publishTrade} />
  ) : screen === 'home' ? home() : screen === 'community' ? community() : screen === 'chats' ? chats() :
    screen === 'my' ? my() : screen === 'detail' ? detail() : screen === 'chat' ? (matchingStatus ? matchingChat() : chatRoom()) :
    screen === 'profile' ? profile() : screen === 'security' ? security() : screen === 'notifications' ? notifications() :
    screen === 'settings' ? preferences() : screen === 'cleanpay' ? cleanPay() : screen === 'history' ? history() :
    screen === 'posts' || screen === 'comments' ? activities() :
    screen === 'notices' || screen === 'guide' || screen === 'help' ? support() : null;

  const pinnedMatch = matchingStatus && chatPost.recruitment ? (
    <Pressable accessibilityRole="button" accessibilityLabel="모집 상세 보기" onPress={() => openPost(chatPost)}>
      <RecruitmentCard recruitment={chatPost.recruitment} compact={!compactChat} />
    </Pressable>
  ) : null;
  const chatPin = pinnedMatch ?? (screen === 'chat' && !chatPost.recruitment ? tradeCard() : null);
  const chatActions = matchingStatus ? matchActions(matchingStatus)
    : screen === 'chat' && !chatPost.recruitment ? tradeActions() : null;

  return <SafeAreaView style={[s.safe, (isTab || screen === 'empty') && { backgroundColor: '#fff' }]}>
    <KeyboardAvoidingView style={s.flex} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={[s.header, !isTab && screen !== 'empty' && s.subHeader]}>
        {!isTab && screen !== 'empty' && <IconButton name="chevron-back" label="뒤로 가기" onPress={back} />}
        <Text style={[s.wordmark, !isTab && screen !== 'empty' && s.centerWordmark]}>FANLINK</Text>
        {screen === 'my' && <View style={s.inline}><IconButton name="notifications-outline" label="알림 설정" onPress={() => push('notifications')} /><IconButton name="settings-outline" label="환경 설정" onPress={() => push('settings')} /></View>}
        {(screen === 'detail' || screen === 'chat' || screen === 'profile') && <View style={s.headerActions}>
          {screen === 'detail' && <IconButton name={saved.includes(post.id) ? 'bookmark' : 'bookmark-outline'} label="게시글 저장" onPress={() => setSaved(flip(saved, post.id))} />}
          <IconButton name="ellipsis-horizontal" label="더 보기" onPress={() => select('더 보기', ['공유', '신고'])} />
        </View>}
      </View>
      {(['chat', 'posts', 'comments', 'history'] as Screen[]).includes(screen) && <Image source={images.backdrop} style={s.backdrop} />}
      {!compactChat && chatPin && <View style={s.pinnedMatch}>{chatPin}</View>}
      <ScrollView key={screen} ref={scroller} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}
        style={s.flex} contentContainerStyle={s.content}>
        {!!titles[screen] && screen !== 'profile' && <Text style={s.title}>{titles[screen]}</Text>}
        {!!intro[screen] && <Text style={s.intro}>{intro[screen]}</Text>}
        {compactChat && chatPin}
        {content}
        {compactChat && chatActions}
      </ScrollView>
      {!compactChat && chatActions}
      {isComposer && <View style={s.composer}>
        <IconButton name={screen === 'chat' ? 'add-circle-outline' : 'image-outline'} label="첨부 메뉴" onPress={() => select('첨부', ['사진', '파일'])} />
        <TextInput style={[s.input, s.flex]} placeholder={screen === 'chat' ? '메시지를 입력하세요...' : '댓글을 입력하세요...'} value={draft} onChangeText={setDraft} onSubmitEditing={() => { send(); setTimeout(() => scroller.current?.scrollToEnd(), 80); }} returnKeyType="send" />
        <Pressable accessibilityRole="button" accessibilityLabel="보내기" style={s.send} onPress={() => { send(); setTimeout(() => scroller.current?.scrollToEnd(), 80); }}><Icon name="send" size={17} color="white" /></Pressable>
      </View>}
      {(isTab || screen === 'empty') && <View style={s.tabbar}>{tabScreens.map((tab, i) => <Pressable key={tab} accessibilityRole="tab" accessibilityLabel={tabLabels[i]} accessibilityState={{ selected: screen === tab }} style={s.tab} onPress={() => replace(tab)}><Icon name={screen === tab ? tabIcons[i] : (tabIcons[i] + '-outline') as IconName} color={screen === tab ? '#0b111a' : '#59616c'} size={24} /></Pressable>)}</View>}
    </KeyboardAvoidingView>
  </SafeAreaView>;
}

const s = StyleSheet.create({
  backdrop: { position: 'absolute', bottom: 0, left: 0, width: '100%', height: 110, opacity: .67 },
  shrink: { flexShrink: 1, minWidth: 0 },
  flex: { flex: 1, minWidth: 0 },
  safe: { flex: 1, backgroundColor: '#f8f9fc' },
  content: { paddingHorizontal: 16, paddingBottom: 20, gap: 11 },
  header: { height: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16 },
  subHeader: { height: 47, paddingHorizontal: 9, justifyContent: 'flex-start' },
  headerActions: { marginLeft: 'auto', flexDirection: 'row' },
  wordmark: { fontSize: 20, fontWeight: '900', color: ink, letterSpacing: -.5 },
  centerWordmark: { position: 'absolute', left: 68, right: 68, textAlign: 'center', fontSize: 17 },
  iconButton: { width: 34, height: 34, justifyContent: 'center', alignItems: 'center' },
  title: { color: ink, fontSize: 25, fontWeight: '900', letterSpacing: -.8 },
  intro: { fontSize: 12, marginTop: -7, color: muted, lineHeight: 17 },
  section: { gap: 8 },
  sectionTitle: { color: ink, fontSize: 17, fontWeight: '900', letterSpacing: -.4 },
  inline: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  wrap: { flexWrap: 'wrap' },
  between: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  panel: { borderRadius: 12, borderWidth: 1, borderColor: '#eaedf4', backgroundColor: '#fff', overflow: 'hidden' },
  card: { backgroundColor: 'white', borderWidth: 1, borderColor: '#eaedf4', borderRadius: 13, padding: 10, gap: 4 },
  rowItem: { minHeight: 37, paddingHorizontal: 10, paddingVertical: 6, flexDirection: 'row', alignItems: 'center', gap: 8, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#edf0f5' },
  rowValue: { maxWidth: '44%', flexShrink: 1, textAlign: 'right' },
  rowTitle: { fontSize: 12, fontWeight: '700', color: ink },
  postTitle: { fontSize: 13, fontWeight: '800', color: ink, lineHeight: 18 },
  detailTitle: { fontSize: 18, fontWeight: '900', color: ink, lineHeight: 24 },
  body: { fontSize: 13, lineHeight: 19, color: ink },
  small: { color: muted, fontSize: 11, lineHeight: 16 },
  tiny: { color: muted, fontSize: 10, lineHeight: 15 },
  link: { color: blue, fontSize: 11, fontWeight: '700' },
  whiteSmall: { color: '#eff6ff', fontSize: 11, fontWeight: '600', lineHeight: 16 },
  whiteTitle: { color: 'white', fontSize: 16, fontWeight: '800' },
  button: { minHeight: 39, paddingVertical: 9, paddingHorizontal: 16, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: 13, fontWeight: '800', textAlign: 'center' },
  pill: { flexShrink: 1, maxWidth: '100%', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 4, minHeight: 28, paddingVertical: 4, paddingHorizontal: 9, borderRadius: 18, borderColor: 'transparent', borderWidth: 1, backgroundColor: '#f1f3f8' },
  pillActive: { borderColor: '#82b4ff', backgroundColor: '#e9f3ff' },
  pills: { alignItems: 'center', gap: 5 },
  tabbar: { minHeight: 53, borderTopWidth: 1, borderTopColor: '#eef0f3', backgroundColor: 'white', flexDirection: 'row' },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  tileRow: { flexDirection: 'row', gap: 6 },
  tile: { flex: 1, minWidth: 0, alignItems: 'center', justifyContent: 'center', gap: 5, paddingVertical: 9, paddingHorizontal: 2, borderRadius: 11, borderWidth: 1, borderColor: 'transparent', backgroundColor: '#f1f4fa' },
  selectedTile: { borderColor: '#83b2fd' },
  clubLogo: { height: 57, width: 62, resizeMode: 'contain' },
  banner: { overflow: 'hidden', position: 'relative', borderRadius: 13, backgroundColor: '#0958e9', paddingHorizontal: 13, paddingVertical: 12, gap: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  bannerCircle: { position: 'absolute', width: 148, height: 148, borderRadius: 74, right: -37, top: -27, backgroundColor: '#1673f7' },
  bannerAction: { alignSelf: 'flex-start', marginTop: 6 },
  payBanner: { flexWrap: 'wrap', backgroundColor: '#0647c5' },
  payBalance: { borderLeftColor: '#74a9ff', borderLeftWidth: 1, paddingLeft: 10 },
  weather: { minHeight: 67, paddingVertical: 8, backgroundColor: '#239ce6' },
  weatherReading: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'baseline', columnGap: 6 },
  weatherTemp: { color: 'white', fontSize: 23, lineHeight: 29, fontWeight: '800' },
  weatherArt: { width: 75, height: 68, resizeMode: 'cover', marginVertical: -9, marginRight: -13 },
  matchCard: { borderRadius: 12, backgroundColor: '#fff', overflow: 'hidden', borderWidth: 1, borderColor: '#e4e7ec' },
  matchHead: { paddingHorizontal: 11, paddingVertical: 5, flexDirection: 'row', gap: 6, alignItems: 'center' },
  matchBody: { paddingVertical: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around' },
  team: { flex: 1, alignItems: 'center', gap: 3 },
  teamLogo: { height: 43, width: 54, resizeMode: 'contain' },
  vs: { color: '#596475', backgroundColor: '#f2f4f8', borderRadius: 10, padding: 4, fontSize: 10, fontWeight: '800' },
  matchTime: { borderLeftColor: '#ebedf2', borderLeftWidth: 1, paddingHorizontal: 9, width: '35%' },
  empty: { alignItems: 'center', gap: 8, padding: 24 },
  profileBanner: { display: 'flex', flexDirection: 'column', alignItems: 'stretch', backgroundColor: '#0648bb', gap: 15 },
  profileArt: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, height: '120%', width: '110%', opacity: .29 },
  profileCounts: { flexDirection: 'row' },
  profileCover: { height: 108, width: '100%', borderRadius: 14 },
  stats: { flexDirection: 'row', paddingVertical: 9 },
  stat: { flex: 1, alignItems: 'center', gap: 3 },
  statNumber: { fontSize: 16, fontWeight: '800', color: ink },
  padded: { padding: 11, fontSize: 12, color: ink, lineHeight: 18 },
  badge: { width: 36, height: 36, backgroundColor: '#2068f0', borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
  recent: { width: '100%', height: 53, borderRadius: 8 },
  thumb: { width: 57, height: 56, borderRadius: 8, backgroundColor: '#eef3fa' },
  detailCopy: { gap: 5, paddingVertical: 5 },
  detailImageFrame: { alignSelf: 'stretch', aspectRatio: 2.08, borderRadius: 13, overflow: 'hidden', backgroundColor: '#eef3fa' },
  detailImage: { width: '100%', height: '100%' },
  comment: { flexDirection: 'row', alignItems: 'flex-start', gap: 8, paddingVertical: 6, borderBottomWidth: 1, borderBottomColor: '#edf0f5' },
  commentPreview: { backgroundColor: '#f5f7fb', padding: 6, borderRadius: 9 },
  chatRow: { flexDirection: 'row', alignItems: 'center', gap: 10, minHeight: 55, paddingVertical: 4 },
  chatMeta: { alignItems: 'flex-end', gap: 3 },
  unread: { backgroundColor: blue, color: 'white', textAlign: 'center', width: 17, borderRadius: 9, fontSize: 10, lineHeight: 17, fontWeight: '800' },
  dateChip: { alignSelf: 'center', backgroundColor: '#e9edf5', color: muted, borderRadius: 12, paddingHorizontal: 11, paddingVertical: 4, fontSize: 10 },
  messageRow: { maxWidth: '100%', flexDirection: 'row', gap: 5, alignItems: 'flex-end' },
  messageMine: { alignSelf: 'flex-end', flexDirection: 'row-reverse' },
  messageTheirs: { alignSelf: 'flex-start' },
  bubble: { flexShrink: 1, maxWidth: '76%', paddingHorizontal: 11, paddingVertical: 8, color: ink, backgroundColor: '#e9edf4', fontSize: 12, lineHeight: 18, borderRadius: 13 },
  bubbleMine: { color: 'white', backgroundColor: blue },
  tradeCard: { backgroundColor: '#eef5ff', gap: 6 },
  tradeLedger: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 8 },
  cleanPayBanner: { flexDirection: 'column', alignItems: 'flex-start', backgroundColor: '#0647c5' },
  pinnedMatch: { paddingHorizontal: 16, paddingBottom: 8 },
  matchActions: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 6, paddingHorizontal: 12, paddingVertical: 7, backgroundColor: '#fff' },
  composer: { flexDirection: 'row', alignItems: 'center', gap: 7, padding: 8, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#edf0f5' },
  input: { backgroundColor: '#eef2f8', borderRadius: 20, paddingHorizontal: 13, paddingVertical: 8, fontSize: 13, color: ink, minHeight: 38 },
  send: { width: 36, height: 36, borderRadius: 18, backgroundColor: blue, justifyContent: 'center', alignItems: 'center' },
  swatches: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 6, padding: 6 },
  swatch: { width: 23, height: 23, borderWidth: 2, borderRadius: 12 },
  squareIcon: { width: 46, height: 46, borderRadius: 9, backgroundColor: '#eaf3ff', justifyContent: 'center', alignItems: 'center' },
  complete: { color: '#24814f', backgroundColor: '#e4f5e9', borderRadius: 9, fontSize: 9, padding: 3, textAlign: 'center' },
  endButton: { alignSelf: 'flex-end' },
});

