# FANLINK UI

첨부된 FANLINK 화면 디자인을 TypeScript, React Native, Expo SDK 58로 구현한 모바일 UI 프로토타입입니다.

## 실행

Node.js 22 LTS(22.13 이상)와 npm을 준비한 뒤, 압축을 풀고 `package.json`이 있는 폴더에서 실행합니다.

```powershell
npm install
npx expo start
```

- Android 에뮬레이터를 켜고 터미널에서 `a`를 눌러 실행할 수 있습니다.
- 실기기 로그인은 FANLINK 개발 빌드에서 실행합니다. Expo Go는 FANLINK 전용 로그인 콜백을 열 수 없습니다.
- 타입 검사는 `npm run typecheck`로 실행합니다.

## 화면과 동작

- 카카오·네이버 로그인 UI와 홈·커뮤니티·채팅·마이 하단 탭
- 경기 일정, 인기 게시물, 구단·카테고리 필터, 게시글 상세, 좋아요·저장·댓글
- 채팅 목록·검색, 1대1 채팅방, 메시지 입력, 모의 결제·참여 확정
- 내 프로필·계정 및 보안·환경 설정·알림 설정·내 게시글·내 댓글·거래내역
- 공지사항·이용가이드·문의하기 및 빈 탭 레이아웃

## 서버 로그인

- 프론트 프로젝트의 `.env.local`에 `EXPO_PUBLIC_API_URL`로 백엔드 기본 주소를 설정합니다.
- 웹은 `npx expo start --web`으로 열고, Railway의 `FRONTEND_URL`을 실제로 열린 웹 주소와 맞춥니다. 웹 호스팅은 `/oauth/callback` 요청에도 앱 진입 페이지를 제공해야 합니다.
- 카카오·네이버 콘솔의 OAuth Redirect URI에는 각 제공자에 해당하는 백엔드의 `/login/oauth2/code/kakao` 또는 `/login/oauth2/code/naver` 주소를 등록합니다.
- 실기기에서는 모바일 OAuth 엔드포인트가 포함된 백엔드를 배포하고, `fanlink` 스킴이 포함된 개발 빌드를 사용합니다.
