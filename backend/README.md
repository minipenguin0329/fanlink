# FANLINK 백엔드

Spring Boot 3 / Java 17 / PostgreSQL / Spring Data JPA로 구현한 FANLINK 백엔드입니다.

---

## 1. 로컬 실행 방법

```bash
docker compose up -d        # Postgres 실행 (:5432, db/user/pw = fanlink/fanlink/fanlink)
./gradlew bootRun           # 앱 실행 (:8080)
```

카카오/네이버 앱 키나 네이버 OCR 키가 없어도 바로 뜹니다 - 둘 다 키가 없으면
안전한 개발용 기본 동작으로 대체됩니다 (아래 "AI 정가 검증" 참고). 실제 키가 생기면
환경변수로 주입하면 됩니다.

| 환경변수 | 용도 |
|---|---|
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | 카카오 로그인 (Kakao Developers 콘솔) |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | 네이버 로그인 (Naver Developers 콘솔) |
| `NAVER_OCR_INVOKE_URL` / `NAVER_OCR_SECRET_KEY` | CLOVA OCR General (NCP 콘솔) |
| `FRONTEND_URL` | 로그인 성공 후 `?token=`을 붙여 리다이렉트할 프론트 주소 (기본값 `http://localhost:5173`) |
| `JWT_SECRET` | 실제 배포 전엔 **반드시** 교체 |

---

## 2. 인증 동작 방식

로그인 화면 디자인 그대로 **소셜 로그인만 지원**하며, 비밀번호 체계는 없습니다.

1. 프론트에서 `GET /oauth2/authorization/kakao` (또는 `naver`)로 이동시킵니다.
2. Spring Security가 OAuth2 코드 플로우를 처리하고, `CustomOAuth2UserService`가
   `User`를 조회/생성합니다. 이후 `OAuth2SuccessHandler`가 JWT를 발급해
   `${FRONTEND_URL}/oauth/callback?token=...`으로 리다이렉트합니다.
3. 프론트는 이 JWT를 저장해두고, 이후 모든 API 요청에 `Authorization: Bearer <token>`
   헤더를 붙입니다. `JwtAuthenticationFilter`가 토큰을 현재 사용자로 변환합니다.
4. 인증 없이 `/api/**`를 호출하면 리다이렉트가 아니라 깔끔한 `401`이 내려갑니다.

---

## 3. 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/auth/providers` | 로그인 버튼용 URL 목록 |
| GET / PATCH | `/api/users/me` | 내 프로필 조회/수정 (닉네임·관심팀·프로필사진) |
| GET | `/api/posts?category=&team=&page=&size=` | 커뮤니티 피드 (비로그인 가능) |
| GET | `/api/posts/mine` | 내 게시글 (로그인 필요) |
| GET / POST / PATCH / DELETE | `/api/posts/{id}` | 게시글 CRUD |
| PATCH | `/api/posts/{id}/status?status=COMPLETED` | 거래완료 등 상태 변경 |
| GET / POST | `/api/posts/{id}/comments` | 댓글 조회/작성 (1단 대댓글 지원) |
| DELETE | `/api/comments/{id}` | 댓글 삭제 |
| POST | `/api/images` (multipart `file`) | 이미지 업로드, `{url}` 반환 |
| POST | `/api/ocr/verify-ticket-price` (multipart `file` + `declaredUnitPrice`) | 글쓰기 화면에서 실시간 정가 검증, 초과 시 등록 버튼 비활성화용 |

---

## 4. AI 정가 검증, 구체적으로

`TICKET` 카테고리 게시글을 `facePrice`(정가)와 함께 등록하려면
(`/api/images`로 미리 업로드해둔) `priceCaptureImageUrl`이 반드시 필요합니다.

서버는 저장 직전에 **그 이미지로 OCR을 직접 다시 돌려** 검증합니다 — 클라이언트가
"검증됨"이라고 보낸 값을 그대로 믿지 않습니다. 가격 추출 로직은
`TicketPriceExtractor`에 있고, 유닛테스트는
`src/test/.../ocr/TicketPriceExtractorTest.java`에 있습니다. OCR 텍스트에서
"정가"가 포함된 줄을 우선 읽고, 없으면 금액 중 가장 작은 값을 정가로 추정합니다.

CLOVA OCR 키가 설정되지 않았거나 영수증 레이아웃을 못 읽은 경우에는, 정상 판매자를
무작정 막지 않기 위해 `verified:false`로 표시한 채 등록은 허용하고 관리자 확인으로
넘깁니다. 이건 제품 판단이 들어간 선택이라, 실제로 악용 사례가 나오면 정책을
다시 검토하는 게 맞습니다.
