/** FANLINK backend REST API – aligned with Spring Boot controllers (Nov 2026).
 * The existing UI may keep its demo state until a real JWT is available.
 * Do NOT put JWTs, private keys or PG secret keys in EXPO_PUBLIC_ variables.
 */
const API_URL = process.env.EXPO_PUBLIC_API_URL?.replace(/\/+$/, '');

export type PostCategory = 'TICKET' | 'GOODS' | 'CARPOOL' | 'MATE';
export type Team = 'SAMSUNG_LIONS' | 'DAEGU_FC' | 'POHANG_STEELERS';
export type PostStatus = 'ACTIVE' | 'COMPLETED' | 'CLOSED';
export type MatchingStatus = 'REQUESTED' | 'HOST_ACCEPTED' | 'CONFIRMED' | 'REJECTED' | 'CANCELED';
export type TradeStatus = 'REQUESTED' | 'ACCEPTED' | 'PAYMENT_PENDING' | 'PAID' | 'DELIVERED' | 'COMPLETED' | 'REJECTED' | 'CANCELED';
export type PaymentOrderStatus = 'READY' | 'APPROVED' | 'FAILED' | 'CANCELED';

export type BackendPost = {
  id: number;
  category: PostCategory;
  team: Team;
  title: string;
  content: string;
  facePrice: number | null;
  quantity: number | null;
  status: PostStatus;
  viewCount: number;
  imageUrls: string[];
  authorId: number;
  authorNickname: string;
  authorProfileImageUrl: string | null;
  createdAt: string;
};
export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};
export type CreatePostInput = {
  category: PostCategory;
  team: Team;
  title: string;
  content: string;
  facePrice?: number | null;
  quantity?: number | null;
  imageUrls?: string[];
  /** Required for priced ticket posts: URL from POST /api/images */
  priceCaptureImageUrl?: string | null;
};
export type UpdatePostInput = {
  title?: string;
  content?: string;
  facePrice?: number;
  quantity?: number;
  imageUrls?: string[];
};
export type MatchingResponse = {
  requestId: number;
  postId: number;
  applicantId: number;
  status: MatchingStatus;
  createdAt: string;
};
export type ChatRoomResponse = {
  roomId: number;
  postId: number;
  hostId: number;
  participantId: number;
  type: 'TRADE' | 'MATCHING';
  createdAt: string;
};
export type ChatMessageResponse = {
  messageId: number;
  roomId: number;
  senderId: number;
  content: string;
  createdAt: string;
};
export type TradeResponse = {
  tradeId: number;
  postId: number;
  buyerId: number;
  sellerId: number;
  unitFacePrice: number;
  status: TradeStatus;
  createdAt: string;
};
export type PriceVerificationResponse = {
  valid: boolean;
  verified: boolean;
  extractedFacePrice: number | null;
  declaredUnitPrice: number;
  message: string;
};
export type LocalImage = {uri: string; name?: string; mimeType?: string};
export type UserResponse = {
  id: number;
  nickname: string;
  email: string | null;
  profileImageUrl: string | null;
  favoriteTeam: Team | null;
  level: number;
};

export type PaymentOrderResponse = {
  paymentOrderId: number;
  tradeId: number;
  orderId: string;
  amount: number;
  status: PaymentOrderStatus;
  createdAt: string;
};

export class FanlinkApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'FanlinkApiError';
  }
}

export function getFanlinkApiUrl(): string {
  if (!API_URL) throw new Error('EXPO_PUBLIC_API_URL 설정을 확인해 주세요.');
  return API_URL;
}

function authorize(token?: string): Record<string, string> {
  if (token === undefined) return {};
  if (!token.trim()) throw new Error('백엔드에서 발급한 실제 JWT가 필요합니다.');
  return { Authorization: `Bearer ${token}` };
}

/** Keep signature compatible with earlier integration/fanlinkApi.ts. */
export async function apiRequest<T>(
  path: string,
  token: string,
  options: RequestInit = {},
): Promise<T> {
  return request<T>(path, options, token);
}

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  for (const [name, value] of Object.entries(authorize(token))) headers.set(name, value);
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${getFanlinkApiUrl()}${path}`, { ...options, headers });
  const text = await response.text();
  if (!response.ok) {
    // Never log or include the Authorization header or raw token in errors.
    throw new FanlinkApiError(response.status, `HTTP ${response.status}: ${text.slice(0, 500)}`);
  }
  if (response.status === 204 || text.length === 0) return undefined as T;
  return JSON.parse(text) as T;
}

function query(params: Record<string, string | number | undefined>): string {
  const pairs = Object.entries(params)
    .filter(([, v]) => v !== undefined)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return pairs.length ? `?${pairs.join('&')}` : '';
}

export const postApi = {
  list: (options: { category?: PostCategory; team?: Team; page?: number; size?: number } = {}) =>
    request<Page<BackendPost>>(`/api/posts${query(options)}`),
  detail: (postId: number) => request<BackendPost>(`/api/posts/${postId}`),
  mine: (token: string, page = 0, size = 20) =>
    request<Page<BackendPost>>(`/api/posts/mine${query({page, size})}`, {}, token),
  create: (data: CreatePostInput, token: string) =>
    request<BackendPost>('/api/posts', { method: 'POST', body: JSON.stringify(data) }, token),
  update: (postId: number, data: UpdatePostInput, token: string) =>
    request<BackendPost>(`/api/posts/${postId}`, { method: 'PATCH', body: JSON.stringify(data) }, token),
  status: (postId: number, status: PostStatus, token: string) =>
    request<BackendPost>(`/api/posts/${postId}/status${query({status})}`, { method: 'PATCH' }, token),
  delete: (postId: number, token: string) =>
    request<void>(`/api/posts/${postId}`, { method: 'DELETE' }, token),
};

/** Existing App.tsx calls this. Spring returns a Page<PostResponse>. */
export async function getBackendPosts(): Promise<BackendPost[]> {
  const page = await postApi.list({ page: 0, size: 50 });
  return page.content;
}

export const matchingApi = {
  apply: (postId: number, token: string) =>
    request<MatchingResponse>(`/api/posts/${postId}/matches`, {method: 'POST'}, token),
  mine: (postId: number, token: string) =>
    request<MatchingResponse>(`/api/posts/${postId}/matches/me`, {}, token),
  applications: (postId: number, token: string) =>
    request<MatchingResponse[]>(`/api/posts/${postId}/matches`, {}, token),
  accept: (requestId: number, token: string) =>
    request<MatchingResponse>(`/api/matches/${requestId}/accept`, {method: 'PATCH'}, token),
  reject: (requestId: number, token: string) =>
    request<MatchingResponse>(`/api/matches/${requestId}/reject`, {method: 'PATCH'}, token),
  confirm: (requestId: number, token: string) =>
    request<MatchingResponse>(`/api/matches/${requestId}/confirm`, {method: 'PATCH'}, token),
  cancel: (requestId: number, token: string) =>
    request<MatchingResponse>(`/api/matches/${requestId}/cancel`, {method: 'PATCH'}, token),
};

export const chatApi = {
  createRoom: (postId: number, token: string) =>
    request<ChatRoomResponse>(`/api/posts/${postId}/chat-rooms`, {method: 'POST'}, token),
  myRooms: (token: string) => request<ChatRoomResponse[]>('/api/chat/rooms', {}, token),
  messages: (roomId: number, token: string) =>
    request<ChatMessageResponse[]>(`/api/chat/rooms/${roomId}/messages`, {}, token),
  send: (roomId: number, content: string, token: string) => {
    if (!content.trim() || content.length > 2000) throw new Error('메시지는 1~2000자여야 합니다.');
    return request<ChatMessageResponse>(`/api/chat/rooms/${roomId}/messages`, {
      method: 'POST', body: JSON.stringify({content: content.trim()}),
    }, token);
  },
};

export const tradeApi = {
  request: (postId: number, token: string) =>
    request<TradeResponse>(`/api/posts/${postId}/trades`, {method: 'POST'}, token),
  mine: (token: string) => request<TradeResponse[]>('/api/trades/mine', {}, token),
  applications: (postId: number, token: string) =>
    request<TradeResponse[]>(`/api/posts/${postId}/trades`, {}, token),
  accept: (tradeId: number, token: string) =>
    request<TradeResponse>(`/api/trades/${tradeId}/accept`, {method: 'PATCH'}, token),
  reject: (tradeId: number, token: string) =>
    request<TradeResponse>(`/api/trades/${tradeId}/reject`, {method: 'PATCH'}, token),
  cancel: (tradeId: number, token: string) =>
    request<TradeResponse>(`/api/trades/${tradeId}/cancel`, {method: 'PATCH'}, token),
};

export const paymentApi = {
  /** Buyer only; use returned orderId and amount to initiate actual PG checkout. */
  prepare: (tradeId: number, token: string) =>
    request<PaymentOrderResponse>(`/api/trades/${tradeId}/payment-orders`, {method: 'POST'}, token),
  /** Call ONLY with a paymentKey returned by a real PG test checkout. */
  confirm: (args: { paymentKey: string; orderId: string; amount: number }, token: string) =>
    request<PaymentOrderResponse>('/api/payments/confirm', {
      method: 'POST', body: JSON.stringify(args),
    }, token),
};

/** React Native FormData file object; let fetch set the multipart boundary. */
function imageMultipart(image: LocalImage, declaredUnitPrice?: number): FormData {
  const form = new FormData();
  form.append('file', {
    uri: image.uri,
    name: image.name || 'ticket-capture.jpg',
    type: image.mimeType || 'image/jpeg',
  } as unknown as Blob);
  if (declaredUnitPrice !== undefined) {
    form.append('declaredUnitPrice', String(declaredUnitPrice));
  }
  return form;
}

export const imageApi = {
  upload: (image: LocalImage, token: string) =>
    request<{url: string}>('/api/images', {method: 'POST', body: imageMultipart(image)}, token),
};

export const ticketPriceApi = {
  verify: (image: LocalImage, declaredUnitPrice: number, token: string) =>
    request<PriceVerificationResponse>('/api/ocr/verify-ticket-price', {
      method: 'POST', body: imageMultipart(image, declaredUnitPrice),
    }, token),
};

export const userApi = {
  me: (token: string, signal?: AbortSignal) => request<UserResponse>('/api/users/me', { signal }, token),
};

export const authApi = {
  testLogin: (account: string, password: string, signal: AbortSignal) =>
    request<{ token?: string; accessToken?: string }>('/api/auth/test-login', {
      method: 'POST', body: JSON.stringify({ account, password }), signal,
    }),
  exchangeMobileCode: (code: string, verifier: string) =>
    request<{ token: string }>('/api/auth/mobile/token', {
      method: 'POST', body: JSON.stringify({ code, verifier }),
    }),
  providers: () => request<Record<'kakao' | 'naver', string>>('/api/auth/providers'),
};
