# AutoLog (오토로그)

내 차의 정비 주기를 잊지 않게 도와주는 개인용 차량 정비 관리 앱입니다.

엔진오일, 타이어, 브레이크 같은 소모품 교체 주기를 차량별로 등록해두면 남은 주행거리와 기간을 계산해 교체 시기가 임박했는지 한눈에 확인할 수 있습니다. 서버 없이 기기 내부(Room)에만 데이터를 저장하는 로컬 전용 앱입니다.

최신 안드로이드 기술 스택(Compose, Coroutine/Flow, Hilt, Clean Architecture)을 학습하며 1인 개발로 만들고 있습니다.

<br>

## 주요 기능

- **차량 관리** — 여러 대 차량 등록, 대표 차량 지정, 연식·연료·주행거리 관리
- **정비 주기 설정** — 차량별 정비 항목과 km/개월 주기 설정 (기본 소모품 프리셋 제공)
- **상태 판정** — 항목별 정상 / 임박 / 초과 상태와 남은 주행거리·기간 표시
- **정비 이력** — 교체일, 주행거리, 정비소, 비용, 메모 기록
- **주행거리 이력** — 시점별 주행거리를 별도로 기록·추적
- **홈 위젯** — 홈 화면에서 차량 정비 상태를 바로 확인
- **주간 알림** — 주행거리 업데이트가 필요한 차량을 매주 알림
- **엑셀 내보내기** — 차량별 정비 내역을 엑셀 파일로 저장
- **백업 / 복원** — 전체 데이터를 JSON 파일로 백업하고 복원

<br>

## 기술 스택

- Language : Kotlin
- UI : Jetpack Compose, Navigation Compose
- Architecture : Clean Architecture + MVVM
- Async : Coroutines, Flow
- DI : Hilt
- Local DB : Room
- Preferences : DataStore
- Background : WorkManager
- Widget : Glance
- 기타 : Apache POI(엑셀), kotlinx.serialization(백업)

<br>

## 아키텍처

계층 간 의존 방향은 `presentation → domain ← data` 를 따릅니다.

```
com.jsworld.android.autolog
├─ data           # Room(dao·entity·db), DataStore, Repository 구현, Mapper
├─ domain         # 도메인 모델, Repository 인터페이스
├─ presentation   # Compose 화면, ViewModel, 네비게이션, 위젯, 테마
├─ di             # Hilt 모듈
└─ core           # 공용 유틸
```

- domain은 프레임워크에 의존하지 않는 순수 계층으로, Repository 인터페이스와 모델을 정의합니다.
- data는 domain 인터페이스를 구현하며, Room·DataStore 등 실제 데이터 소스를 다룹니다.
- presentation은 ViewModel을 통해 상태를 관리하고 Compose로 화면을 그립니다.

<br>

## 개발 환경

- minSdk : 28
- targetSdk : 36
- JDK : 17

### 빌드 & 실행
```bash
# 저장소 클론 후
./gradlew :app:assembleDebug
```

Android Studio에서 프로젝트를 열면 Gradle Sync 후 바로 실행할 수 있습니다.
(`local.properties` 의 Android SDK 경로는 각자 환경에 맞게 설정됩니다.)

<br>

## 로드맵

- [ ] 주유 데이터 기록 및 연비 그래프
- [ ] 화면 사용성 개선
- [ ] 정비 임박/초과 항목 알림

<br>

## 참고

- 데이터는 서버 없이 기기 내부에만 저장됩니다. 기기 변경이나 앱 삭제에 대비해 백업 기능 사용을 권장합니다.
- 1인 개발로 꾸준히 개선하고 있습니다.
