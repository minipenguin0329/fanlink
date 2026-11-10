/** Conversion helpers. Never use samplePosts IDs as backend post IDs. */
import type {BackendPost, PostCategory, Team} from './fanlinkApi';

export type UiGroup = '티켓' | '굿즈' | '카풀' | '직관메이트';
const GROUPS: Record<PostCategory, UiGroup> = {
  TICKET: '티켓', GOODS: '굿즈', CARPOOL: '카풀', MATE: '직관메이트',
};
const CLUBS: Record<Team, 0 | 1 | 2> = {
  SAMSUNG_LIONS: 0, DAEGU_FC: 1, POHANG_STEELERS: 2,
};

export function mapBackendPostBasic(post: BackendPost) {
  return {
    id: post.id,                 // Real database postId
    group: GROUPS[post.category],
    club: CLUBS[post.team],
    title: post.title,
    summary: post.content,
    likes: 0,                    // Likes API not connected; do not invent counts.
    replies: 0,
    status: post.status,
    author: post.authorNickname,
    facePrice: post.facePrice,
    quantity: post.quantity,
    imageUrls: post.imageUrls,
  };
}

/** Do not invent carpool departure/capacity or match game times:
 * PostResponse does not contain them yet. */
