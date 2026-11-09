
const API_URL =
    process.env.EXPO_PUBLIC_API_URL?.replace(/\/+$/, "");

export type MatchingStatus =
    | "REQUESTED"
    | "HOST_ACCEPTED"
    | "CONFIRMED"
    | "REJECTED"
    | "CANCELED";

export type TradeStatus =
    | "REQUESTED"
    | "ACCEPTED"
    | "PAYMENT_PENDING"
    | "PAID"
    | "DELIVERED"
    | "COMPLETED"
    | "REJECTED"
    | "CANCELED";

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
    type: "TRADE" | "MATCHING";
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

// 공통 API 요청 함수
export async function apiRequest<T>(
    path: string,
    token: string,
    options: RequestInit = {}
): Promise<T> {
    if (!API_URL) {
        throw new Error("API 서버 주소가 설정되지 않았습니다.");
    }

    if (!token?.trim()) {
        throw new Error("실제 백엔드 JWT 로그인이 필요합니다.");
    }

    const headers = new Headers(options.headers);
    headers.set("Authorization", `Bearer ${token}`);
    headers.set("Accept", "application/json");

    if (options.body != null) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_URL}${path}`, {
        ...options,
        headers,
    });

    const text = await response.text();

    if (!response.ok) {
        throw new Error(
            `API 요청 실패 (${response.status}): ${text}`
        );
    }

    return text ? (JSON.parse(text) as T) : (undefined as T);
}

// 메이트·카풀 매칭
export const matchingApi = {
    apply: (postId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/posts/${postId}/matches`,
            token,
            { method: "POST" }
        ),

    mine: (postId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/posts/${postId}/matches/me`,
            token
        ),

    applications: (postId: number, token: string) =>
        apiRequest<MatchingResponse[]>(
            `/api/posts/${postId}/matches`,
            token
        ),

    accept: (requestId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/matches/${requestId}/accept`,
            token,
            { method: "PATCH" }
        ),

    reject: (requestId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/matches/${requestId}/reject`,
            token,
            { method: "PATCH" }
        ),

    confirm: (requestId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/matches/${requestId}/confirm`,
            token,
            { method: "PATCH" }
        ),

    cancel: (requestId: number, token: string) =>
        apiRequest<MatchingResponse>(
            `/api/matches/${requestId}/cancel`,
            token,
            { method: "PATCH" }
        ),
};

// 1:1 채팅 REST API
export const chatApi = {
    createRoom: (postId: number, token: string) =>
        apiRequest<ChatRoomResponse>(
            `/api/posts/${postId}/chat-rooms`,
            token,
            { method: "POST" }
        ),

    myRooms: (token: string) =>
        apiRequest<ChatRoomResponse[]>(
            "/api/chat/rooms",
            token
        ),

    messages: (roomId: number, token: string) =>
        apiRequest<ChatMessageResponse[]>(
            `/api/chat/rooms/${roomId}/messages`,
            token
        ),

    send: (
        roomId: number,
        content: string,
        token: string
    ) =>
        apiRequest<ChatMessageResponse>(
            `/api/chat/rooms/${roomId}/messages`,
            token,
            {
                method: "POST",
                body: JSON.stringify({ content }),
            }
        ),
};

// 티켓 거래 API
export const tradeApi = {
    request: (postId: number, token: string) =>
        apiRequest<TradeResponse>(
            `/api/posts/${postId}/trades`,
            token,
            { method: "POST" }
        ),

    mine: (token: string) =>
        apiRequest<TradeResponse[]>(
            "/api/trades/mine",
            token
        ),

    applications: (postId: number, token: string) =>
        apiRequest<TradeResponse[]>(
            `/api/posts/${postId}/trades`,
            token
        ),

    accept: (tradeId: number, token: string) =>
        apiRequest<TradeResponse>(
            `/api/trades/${tradeId}/accept`,
            token,
            { method: "PATCH" }
        ),

    reject: (tradeId: number, token: string) =>
        apiRequest<TradeResponse>(
            `/api/trades/${tradeId}/reject`,
            token,
            { method: "PATCH" }
        ),

    cancel: (tradeId: number, token: string) =>
        apiRequest<TradeResponse>(
            `/api/trades/${tradeId}/cancel`,
            token,
            { method: "PATCH" }
        ),
};

export type BackendPost = {
    id: number;
    category: string;
    team: string;
    title: string;
    content: string;
    facePrice: number | null;
    quantity: number | null;
    status: string;
    viewCount: number;
    imageUrls: string[];
    authorId: number;
    authorNickname: string;
    authorProfileImageUrl: string | null;
    createdAt: string;
};

// 서버에 저장된 실제 게시물 조회
export async function getBackendPosts(): Promise<BackendPost[]> {
    if (!API_URL) {
        throw new Error("API 서버 주소가 설정되지 않았습니다.");
    }

    const response = await fetch(`${API_URL}/api/posts`);

    if (!response.ok) {
        throw new Error(`게시물 조회 실패: HTTP ${response.status}`);
    }

    const data: unknown = await response.json();

    // 서버가 배열을 반환하는 경우
    if (Array.isArray(data)) {
        return data as BackendPost[];
    }

    // Spring 페이지네이션 응답인 경우
    if (data !== null && typeof data === "object" && "content" in data) {
        const content = (data as { content?: unknown }).content;

        if (Array.isArray(content)) {
            return content as BackendPost[];
        }
    }

    throw new Error("예상하지 못한 게시물 응답 형식입니다.");
}
