/** Reusable action handlers for the existing React Native buttons.
 * Each function uses REAL DB post IDs and an authenticated backend JWT.
 * UI is responsible for showing loading/errors and rendering returned statuses.
 */
import {chatApi, imageApi, matchingApi, paymentApi, postApi, ticketPriceApi, tradeApi, type CreatePostInput, type LocalImage} from './fanlinkApi';
import {requireBackendJwt} from './fanlinkSession';

export async function applyMateAndOpenChat(postId: number) {
  const jwt = await requireBackendJwt();
  const match = await matchingApi.apply(postId, jwt);
  const room = await chatApi.createRoom(postId, jwt);
  return { match, room };
}

export async function acceptMate(requestId: number) {
  return matchingApi.accept(requestId, await requireBackendJwt());
}

export async function confirmMate(requestId: number) {
  return matchingApi.confirm(requestId, await requireBackendJwt());
}

export async function cancelMate(requestId: number) {
  return matchingApi.cancel(requestId, await requireBackendJwt());
}

export async function openTicketChat(postId: number) {
  return chatApi.createRoom(postId, await requireBackendJwt());
}

export async function loadMyChatRooms() {
  return chatApi.myRooms(await requireBackendJwt());
}

export async function loadChatMessages(roomId: number) {
  return chatApi.messages(roomId, await requireBackendJwt());
}

export async function sendChatMessage(roomId: number, content: string) {
  return chatApi.send(roomId, content, await requireBackendJwt());
}

export async function requestTicketTrade(postId: number) {
  return tradeApi.request(postId, await requireBackendJwt());
}

export async function acceptTicketTrade(tradeId: number) {
  return tradeApi.accept(tradeId, await requireBackendJwt());
}

export async function cancelTicketTrade(tradeId: number) {
  return tradeApi.cancel(tradeId, await requireBackendJwt());
}

export async function createPaymentOrder(tradeId: number) {
  return paymentApi.prepare(tradeId, await requireBackendJwt());
}

export async function confirmActualPgTestPayment(
  paymentKeyFromPg: string,
  serverOrderId: string,
  serverAmount: number,
) {
  return paymentApi.confirm({
    paymentKey: paymentKeyFromPg,
    orderId: serverOrderId,
    amount: serverAmount,
  }, await requireBackendJwt());
}

/** OCR must report BOTH valid and verified. Never mark an unverified price as safe. */
export async function createVerifiedTicketPost(
  data: Omit<CreatePostInput, 'category' | 'priceCaptureImageUrl'> & {facePrice: number},
  priceCapture: LocalImage,
) {
  const jwt = await requireBackendJwt();
  if (data.facePrice <= 0) throw new Error('티켓 1매 가격이 필요합니다.');
  const result = await ticketPriceApi.verify(priceCapture, data.facePrice, jwt);
  if (!result.valid || !result.verified) {
    throw new Error(result.message || 'OCR 정가 검증을 완료하지 못했습니다.');
  }
  const uploaded = await imageApi.upload(priceCapture, jwt);
  return postApi.create({...data, category: 'TICKET', priceCaptureImageUrl: uploaded.url}, jwt);
}
