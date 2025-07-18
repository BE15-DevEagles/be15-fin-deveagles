import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const wsUrl = `${apiBaseUrl}/ws-chat`;

const SUB_PREFIX = '/sub/chatroom';
const PUB_DESTINATION = '/pub/chat/send';

let stompClient = null;
const subscriptionMap = new Map();

let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

export const ensureSocketConnected = async (onReceive, onAuthError) => {
  if (stompClient?.connected) return;

  const token = localStorage.getItem('accessToken');
  if (!token) {
    console.warn('❌ WebSocket 연결 실패: accessToken 없음');
    onAuthError?.();
    return;
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    connectHeaders: { Authorization: `Bearer ${token}` },
    debug: () => {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    onConnect: () => {
      reconnectAttempts = 0;
    },

    onStompError: frame => {
      console.error('🚫 STOMP 오류', frame);
      onAuthError?.();
    },

    onWebSocketClose: () => {
      reconnectAttempts++;
      console.warn(
        `🔌 WebSocket 연결 종료됨 (시도 ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`
      );

      if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error('❌ WebSocket 재연결 포기');
        stompClient.deactivate();
      }
    },

    onWebSocketError: err => {
      console.error('❗ WebSocket 에러', err);
    },
  });

  stompClient.activate();

  // 연결 완료 대기 (최대 5초)
  const maxWaitTime = 5000;
  const startTime = Date.now();

  while (!stompClient.connected && Date.now() - startTime < maxWaitTime) {
    await new Promise(res => setTimeout(res, 100));
  }

  if (!stompClient.connected) {
    console.error('❌ WebSocket 연결 타임아웃');
    stompClient.deactivate();
    throw new Error('WebSocket 연결 타임아웃');
  }
};

export const safeSubscribeToRoom = (roomId, onReceive) => {
  if (!stompClient?.connected) {
    console.warn(`🚫 WebSocket 미연결 상태: ${roomId} 구독 실패`);
    return;
  }
  if (subscriptionMap.has(roomId)) return;

  const sub = stompClient.subscribe(`${SUB_PREFIX}/${roomId}`, msg => {
    const parsed = JSON.parse(msg.body);
    onReceive(parsed);
  });

  subscriptionMap.set(roomId, sub);
};

export const subscribeToNewRoom = (roomId, onReceive) => {
  if (!stompClient?.connected) {
    console.warn('🚫 연결 안 된 상태, 구독 생략:', roomId);
    return;
  }
  safeSubscribeToRoom(roomId, onReceive);
};

export const sendSocketMessage = (roomId, message) => {
  if (!stompClient?.connected) {
    console.warn('🚫 메시지 전송 실패: WebSocket 연결 안 됨');
    return;
  }

  try {
    stompClient.publish({
      destination: PUB_DESTINATION,
      body: JSON.stringify({ ...message, roomId }),
    });
  } catch (e) {
    console.error('❌ 메시지 발송 실패', e);
  }
};

export const disconnectSocket = () => {
  subscriptionMap.forEach(sub => sub.unsubscribe());
  subscriptionMap.clear();

  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }

  reconnectAttempts = 0;
  console.info('🔌 WebSocket 수동 연결 종료');
};
