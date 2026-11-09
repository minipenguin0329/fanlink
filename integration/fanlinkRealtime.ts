/** JWT-authenticated STOMP chat, matched to:
 * endpoint /ws/chat, SUBSCRIBE /user/queue/chat, SEND /app/chat.send.
 * Requires: npm.cmd install @stomp/stompjs
 */
import {Client, type IMessage} from '@stomp/stompjs';
import {getFanlinkApiUrl, type ChatMessageResponse} from './fanlinkApi';

export function connectFanlinkChat(
  jwt: string,
  onMessage: (message: ChatMessageResponse) => void,
  onError?: (error: string) => void,
): Client {
  if (!jwt.trim()) throw new Error('실제 JWT가 있어야 WebSocket에 연결할 수 있습니다.');
  const wsUrl = `${getFanlinkApiUrl().replace(/^http/, 'ws')}/ws/chat`;

  const client = new Client({
    webSocketFactory: () => new WebSocket(wsUrl),
    connectHeaders: { Authorization: `Bearer ${jwt}` },
    reconnectDelay: 4000,
    heartbeatIncoming: 0,
    heartbeatOutgoing: 0,
    onConnect: () => {
      client.subscribe('/user/queue/chat', (frame: IMessage) => {
        try {
          onMessage(JSON.parse(frame.body) as ChatMessageResponse);
        } catch {
          onError?.('서버 메시지 형식이 올바르지 않습니다.');
        }
      });
    },
    onStompError: frame => onError?.(frame.headers['message'] || 'STOMP 오류'),
    onWebSocketError: () => onError?.('WebSocket 연결 오류'),
  });
  client.activate();
  return client;
}

export function sendRealtimeChat(client: Client, roomId: number, content: string): void {
  if (!client.connected) throw new Error('채팅 서버에 연결되지 않았습니다.');
  if (!content.trim() || content.length > 2000) throw new Error('메시지는 1~2000자여야 합니다.');
  client.publish({destination: '/app/chat.send', body: JSON.stringify({roomId, content: content.trim()})});
}

// Unmount / logout: void client.deactivate();
