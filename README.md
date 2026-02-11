# MY4CUT-BE
<img width="773" height="430" alt="my4cut" src="https://github.com/user-attachments/assets/2e4b9d2b-fa14-4985-a17b-b9cf89934adf" />

---

## Back-end 팀원
- 구현모
- 설원준
- 이나영
- 이승준

---

## 사용 기술 스택
- **Framework**: Spring Boot 3.5.9
- **Language**: Java 17
- **Database**: MySQL 8.0 (AWS RDS)
- **ORM**: Spring Data JPA
- **Security**: Spring Security + JWT
- **Social Login**: OAuth2 (Kakao)
- **Build Tool**: Gradle
- **Infra / Deployment**: AWS EC2, Docker, Docker Compose, Nginx, GitHub Actions CI/CD
---

## 사용 라이브러리
- Spring Web : Android 앱과의 통신을 위한 REST API 구현에 사용
- Spring Data JPA : 데이터베이스 CRUD 처리를 위한 ORM 라이브러리
- Spring Validation : 요청 데이터의 유효성 검증을 위해 사용
- Lombok : 반복되는 코드 작성을 줄이기 위해 사용
- Swagger : API 문서화를 위해 사용

---

## branch 전략
main branch와 하위 각 팀원별 branch를 이용합니다.

- main branch: 배포 직전 단계의 브랜치. develop branch에서 개발이 끝나면 사용
- develop branch : main branch의 하위 브랜치로써, 개발 프로세스를 진행하는 브랜치
- 개인 branch : develop branch의 하위 브랜치로, 팀원 개개인이 담당한 기능을 개발하는 브랜치

---

## issue 전략
태그를 사용하여 두 가지 유형의 이슈를 관리합니다.
- [Request]: 개선이 필요한 사항에 대한 요청 이슈
- [Error]: 오류가 있는 경우에 대한 이슈
  
---

## Pull request 전략
작업 성격에 따라 태그를 사용하여 PR을 관리합니다.
- [Add]: 기능 추가
- [Fix]: 버그 수정
- [Refactor]: 코드 개선 (기능 변화 없음)

---

## commit 컨벤션
각 태그를 이용하여 어떤 내용이 변경되었는지를 나타냅니다.

- [Feat]: 새로운 기능 추가
- [Fix]: 버그 수정
- [Refactor]: 코드 리팩토링
- [Test]: 테스트 코드
- [Chore]: 설정 및 기타 작업

---

## 코드 컨벤션
가독성, 일관성, 협업 효율성을 위해 아래의 컨벤션을 따릅니다.

### 클래스
- **PascalCase** (파스칼 케이스)
    - 첫글자와 이어지는 단어의 첫글자를 대문자로 표기하는 방법
    - 예) `GoodPerson`, `MyKakaoCake`, `IAmDeveloper`
      
### 메서드, 변수
- **camelCase** (카멜 케이스)
    - 첫단어는 소문자로 표기하지만, 이어지는 단어의 첫글자는 대문자로 표기하는 방법
    - 예) `goodPerson`, `myKakaoCake`, `iAmDeveloper`

### 상수
- **UPPER_CASE** (어퍼 케이스)
    - 모든 단어를 대문자로 표기하고, 단어를 언더바(_) 로 연결하는 방법
    - 예) `GOOD_PERSON`, `MY_KAKAO_CAKE`, `I_AM_DEVELOPER`

### Boolean
- `is`, `has`로 시작
    - 예) `isDeleted`, `hasPermission`
 
---

## 프로젝트 구조
```
src/main/java/com/my4cut/domain/
├── common/
│   └── BaseEntity.java              # 공통 필드(createdAt) 관리
├── auth/
│   └── entity/
│       └── RefreshToken.java        # JWT Refresh Token
├── user/
│   ├── entity/
│   │   ├── User.java                # 사용자 정보
│   │   └── UserFcmToken.java        # FCM 푸시 알림 토큰
│   └── enums/
│       ├── LoginType.java           # 로그인 방식 (KAKAO, EMAIL)
│       ├── UserStatus.java          # 계정 상태 (ACTIVE, INACTIVE, DELETED)
│       └── DeviceType.java          # 기기 유형 (IOS, ANDROID)
├── friend/
│   ├── entity/
│   │   ├── Friend.java              # 친구 관계
│   │   └── FriendRequest.java       # 친구 요청
│   └── enums/
│       └── FriendRequestStatus.java # 요청 상태 (PENDING, ACCEPTED, REJECTED)
├── pose/
│   └── entity/
│       ├── Pose.java                # 포즈 정보
│       └── PoseFavorite.java        # 포즈 즐겨찾기
├── workspace/
│   └── entity/
│       ├── Workspace.java           # 공유 워크스페이스
│       └── WorkspaceMember.java     # 워크스페이스 멤버
├── media/
│   ├── entity/
│   │   ├── MediaFile.java           # 미디어 파일 (사진/동영상)
│   │   └── MediaComment.java        # 미디어 댓글
│   └── enums/
│       └── MediaType.java           # 미디어 유형 (PHOTO, VIDEO)
├── day4cut/
│   ├── entity/
│   │   ├── Day4Cut.java             # 하루네컷
│   │   └── Day4CutImage.java        # 하루네컷 이미지
│   └── enums/
│       └── EmojiType.java           # 이모지 타입 (HAPPY, SAD, ANGRY, CALM, TIRED)
└── notification/
    ├── entity/
    │   └── Notification.java        # 알림
    └── enums/
        └── NotificationType.java    # 알림 유형
```

---

## Database (ERD)
<img width="3112" height="1906" alt="image" src="https://github.com/user-attachments/assets/cc0570ce-1ac1-415b-ac1a-01f29ee02a37" />

---

## Infrastructure Architecture
<img width="763" height="485" alt="my4cut 시스템 아키텍처" src="https://github.com/user-attachments/assets/0483c96c-0fa4-4cf7-8f02-036ebbc05f1e" />


