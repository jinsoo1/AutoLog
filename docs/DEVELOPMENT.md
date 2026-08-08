# 개발 노트

혼자 개발하며 잊기 쉬운, 데이터와 관련된 주의사항을 모아둔 문서입니다.

## 0. 빌드 툴체인 (AGP 9 전환기 구성) ⚠️

| 항목 | 버전 |
|---|---|
| AGP | 9.2.1 (최소 Gradle **9.4.1** 요구) |
| Gradle | 9.4.1 |
| Kotlin | 2.3.0 |
| KSP | 2.3.0 (Kotlin 과 버전 동일해야 함) |
| Hilt | 2.60.1 (2.57.x 는 AGP 9 비호환 — `BaseExtension not found`) |
| JDK | 17 |

### 애노테이션 처리는 kapt 가 아니라 KSP
AGP 9 로 올리며 kapt → KSP 로 전환했다. Room·Hilt 모두 KSP 를 지원한다.
`kapt(...)` 대신 `ksp(...)` 를 쓰고, kapt 전용 옵션(`correctErrorTypes`)은 필요 없다.

### `android.builtInKotlin=false`, `android.newDsl=false` 는 임시 설정
AGP 9 는 Kotlin 지원을 내장(built-in Kotlin)하지만, **KSP·kapt 가 아직 내장 Kotlin 과 호환되지 않는다.**
그래서 내장 Kotlin 과 새 DSL 을 끄고 기존 `kotlin.android` 플러그인 경로를 쓰고 있다.

- 두 플래그 모두 **AGP 10 에서 제거**된다(빌드 시 deprecation 경고가 뜬다).
- KSP 가 내장 Kotlin 을 지원하게 되면:
  1. `gradle.properties` 의 두 플래그 제거
  2. `kotlin.android` 플러그인 제거 (root/app 양쪽)
  3. `kotlin { compilerOptions {} }` → `android { kotlin { compilerOptions {} } }` 로 이동
  4. `compileSdk { version = release(36) }` 등 새 DSL 문법 확인
- 참고: https://kotl.in/gradle/agp-built-in-kotlin

## 0-1. 화면 구조 (탭 셸)

앱의 루트는 `Routes.MAIN` = `MainTabScreen` 이다. 탭은 **홈 · 정비 · 설정** 세 개이고,
주유 탭은 자리만 비활성으로 두었다(기능 완성 시 활성화).

### 탭 전환은 백스택을 쌓지 않는다
탭마다 화면이 하나뿐이라 중첩 `NavHost` 없이 `MainTabScreen` 안의 상태 하나로 전환한다.
- 이유: 예전에 "리스트 → 상세 → 목록 버튼"이 무한히 쌓이던 백스택 버그가 있었고,
  탭별 백스택을 도입하면 같은 종류의 문제가 다시 생길 여지가 크다.
- 홈이 아닌 탭에서 뒤로가기는 `BackHandler` 로 홈 탭으로 보낸다(앱 종료가 아니라).
- 스택을 루트로 되돌릴 때는 `navigateToMainRoot()` 를 쓴다. 그래프 전체를 비우므로
  스플래시가 제거된 뒤에도 화면이 쌓이지 않는다.

### ⚠️ 탭 안의 Scaffold 는 인셋을 소비하지 않아야 한다
`MainTabScreen` 의 Scaffold 가 이미 **탭바 + 시스템 내비게이션 인셋**만큼 콘텐츠를 밀어놓는다.
탭 화면이 자기 Scaffold 를 또 두면 하단 인셋이 **두 번** 적용돼서

- 탭바 위에 빈 여백이 생기고
- FAB 가 그만큼 떠오르고
- 스크롤 영역이 그만큼 짧아진다

→ 탭 안에서 쓰는 Scaffold 에는 `contentWindowInsets` 를 넘겨 인셋을 끈다.

```kotlin
// 정비·주유 탭: 상단은 statusBarsPadding 으로 직접 처리하므로 전부 0
contentWindowInsets = WindowInsets(0, 0, 0, 0)

// 설정 탭: TopAppBar 가 있어 상단 인셋은 필요하므로 상단만 남긴다
contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
```

설정 화면은 탭(`showBack = false`)과 단독 라우트 양쪽에서 쓰이므로 분기해야 한다.

### 현재 차량은 `CarContextViewModel` 이 갖는다
`MainActivity` 에서 한 번 만들어 `NavHost` 에 내려준다(액티비티 스코프).
선택 값은 DataStore(`selected_car_id`)에 저장하고, 그 차량이 삭제되면 **대표 차량 → 첫 차량** 순으로 대체한다.

### 정비 화면의 역할 분담
| 화면 | 담당 |
|---|---|
| 홈 탭 | 주행거리·올해 정비비, 임박/초과 항목, 다음 정비, 최근 정비 |
| 정비 탭 | 항목 구분 없는 **통합 정비 기록 타임라인**(월별) + 항목 칩 필터 |
| 정비 항목 관리 (`CarDetailScreen`) | 켜둔 항목 목록과 정렬, 항목 추가 |
| 정비 항목 상세 (`MaintenanceItemDetailScreen`) | 주기 + 교체 내역 + 평균 교체 주기/비용 |

기록 열람 경로가 예전에는 `차량 상세 → 항목 → 항목 수정 → 내역 보기`(4단계)였다.
정비 탭의 타임라인과 항목 상세(주기+내역 통합)로 이 경로를 대체했다.

### ⚠️ ViewModel 이 함수로 Flow 를 만들면 컴포지션에서 `remember` 로 고정할 것
`viewModel.observeUi(id)` 처럼 호출마다 새 Flow 를 반환하는 함수를 `collectAsState()` 에
바로 물리면, `collectAsState` 의 키가 Flow 인스턴스라서 **리컴포지션마다 재구독**된다.
그 Flow 가 `onStart { emit(loading = true) }` 를 갖고 있으면 loading 이 다시 흘러들어와
**무한 리컴포지션 루프**가 된다.

```kotlin
// 이렇게
val uiFlow = remember(settingId) { viewModel.observeUi(settingId) }
val ui by uiFlow.collectAsState(initial = ...)
```

## 0-2. 색상 롤은 빠짐없이 채운다 ⚠️

`lightColorScheme()` / `darkColorScheme()` 에서 **지정하지 않은 롤은 Material 기본값(라벤더 계열)** 이 남는다.
primary·surface 만 바꿔두면 아래 롤들이 보라 기를 띤 채로 앱 전체에 쓰인다.

| 롤 | 어디에 쓰이나 |
|---|---|
| `surfaceContainer` | `NavigationBar` 배경 |
| `surfaceContainerLow` | `ModalBottomSheet` |
| `surfaceContainerHighest` | 입력 필드 배경 |
| `outlineVariant` | **앱 전체 카드 테두리·구분선** (영향 범위가 가장 넓다) |
| `inverseSurface` | 스낵바 |
| `surfaceTint` | elevation 틴트 |

→ `Theme.kt` 에서 슬레이트 톤으로 전부 지정해 두었다. 새 롤이 생기면 같이 채울 것.

## 0-3. 주유(충전) 기록

- 테이블 `fuel_records`, DB 버전 **3** (`MIGRATION_2_3`, 순수 추가라 데이터 유실 위험 없음).
- **연비/전비를 계산하지 않는다.** PHEV 에서 성립하지 않고 만땅 여부를 사용자가 알기 어려워
  값이 신뢰되지 않는다. 그래서 `isFullTank` 같은 컬럼도 두지 않았다.
  대신 "이전 기록 이후 주행거리"와 월 지출로 사용성을 만든다.
- 금액 · 주유량 · 단가는 곱셈 관계라 **둘만 넣으면 나머지를 계산**한다(`FuelAmountCalc`).
  세 값을 모두 직접 넣으면 계산하지 않는다 — 영수증 값이 반올림 때문에 딱 맞지 않는 일이 흔하다.
  이 로직은 `FuelAmountCalcTest` 로 고정해 두었다.
### 에너지 종류는 차량이 아니라 **기록마다** 붙는다
`fuel_records.unit` 이 레코드 컬럼인 이유다. 플러그인 하이브리드는 한 차량이 주유와 충전을 모두 한다.

`FuelUnit.supportedUnits(fuelType)` 가 판정하고 `FuelUnitTest` 로 고정해 두었다.

| 연료 타입 | 지원 종류 |
|---|---|
| 가솔린 · 디젤 · LPG · 기타 · 미설정 | 주유(L) |
| 하이브리드(HEV) | 주유(L) — **외부 충전을 하지 않는다** |
| **플러그인 하이브리드 · PHEV** | **주유(L) + 충전(kWh)** |
| 전기 · EV | 충전(kWh) |
| 수소 | 충전(**kg**) — kWh 가 아니다 |

- 종류가 하나인 차량에서는 배지·필터·범례·선택 시트가 모두 사라져 단일 종류 화면처럼 보인다.
  (전기를 쓰지 않는 차량에 충전 UI 가 새어나오면 안 된다)
- 색은 **주유 = primary, 충전 = tertiary** 로 앱 전체에서 일관되게 쓴다.
- 평균 단가는 단위가 섞이면 의미가 없으므로 **단일 종류 차량에서만** 보여준다.
  PHEV 는 대신 주유비/충전비를 나란히 + 총 에너지비를 따로 표시한다.
- "이전 기록 이후 주행거리"는 종류를 **섞어서** 계산한다 — 주유든 충전이든 그 사이에 실제로 달린 거리다.
- 최근 장소 제안은 종류별로 나눈다(주유소와 충전소는 다른 장소다).

### ⚠️ 입력 기준과 표시 기준을 구분할 것
연료 타입을 바꿔도 **기존 주유/충전 기록은 지워지지 않는다**(지우는 코드는 복원뿐이다).
그래서 "지금 무엇을 넣을 수 있나"와 "무엇을 보여줘야 하나"가 달라진다.

| | 함수 | 쓰는 곳 |
|---|---|---|
| 입력 기준 | `FuelUnit.supportedUnits(fuelType)` | FAB, 종류 선택 시트 |
| 표시 기준 | `FuelUnit.displayUnits(기록의 종류들, fuelType)` | 통계 라벨, 배지, 필터, 섹션 제목 |

표시 기준을 차량 설정만으로 잡으면 이런 버그가 난다(실제로 났다):
- PHEV → 전기로 바꾸면 과거 **주유비가 통계에서 사라진다**(목록엔 남아 있는데 합계엔 없음)
- 라벨은 "충전비"인데 값에는 주유비가 섞여 들어간다
- 평균 단가가 L 과 kWh 를 함께 나눠 **말이 안 되는 숫자**가 나온다 → 단위가 하나일 때만 표시한다

엑셀도 같은 이유로 **종류별 시트**로 나눈다. 한 시트에 섞으면
`총 주유량 = 22.6L + 28.4kWh` 같은 합계가 나온다(PHEV 는 항상 섞이므로 상시 발생하던 버그).
단, "이전 기록 이후 주행"은 종류를 **섞어** 계산해 시트에 넣는다.
- 영수증 사진은 아직 첨부 UI 가 없다(`photoPath` 컬럼만 있음). 붙일 때
  **백업에는 사진을 담지 않으므로** 복원 후 파일이 없을 때 "사진 없음"으로 깨지지 않게 처리해야 한다.

### Room 스키마는 app/schemas 에 커밋한다
`ksp { arg("room.schemaLocation", ...) }` 로 내보낸다. 마이그레이션을 쓸 때는
**생성된 JSON 의 `createSql` 을 그대로 옮겨 적을 것.** 직접 쓰면 타입·FK 옵션이 미묘하게 달라져
실행 시 `Migration didn't properly handle` 크래시가 난다.

## 0-4. 일회성 수리 = 주기 없는 정비 항목

경고등 떠서 한 번 고치는 정비(써모스탯 등)는 별도 테이블이 아니라
**주기(intervalKm·intervalMonths·타입 기본값 모두)가 없는 정비 항목**으로 기록한다.

- 주기 없는 항목은 상태 계산(`buildMaintenanceUiModelsFromRoom`)에서 제외되므로
  임박 알림·다음 정비에 나타나지 않는다 — 이게 수리에 원하는 동작이다.
- 진입: 기록 입력의 항목 선택 시트 → "수리 기록 추가" → 이름만 입력.
  `getOrCreateRepairSetting()` 이 같은 이름의 항목을 재사용하므로
  같은 부품을 두 번 수리하면 이력이 한 항목에 쌓인다.
- 타임라인의 수리 판정은 SQL(`isRepair`)로 내려온다. 배지·아이콘은 **secondary 색**
  (주기 정비 primary, 충전 tertiary 와 구분).
- **승격**: 항목 상세의 "주기 설정"으로 주기를 붙이면 그 순간부터 관리 항목이 된다
  (냉각수처럼 알고 보니 반복 항목인 경우). 반대로 주기를 지우면 다시 수리로 돌아간다.
- 백업·엑셀·통계는 일반 정비와 같은 경로라 따로 처리할 것이 없다.

## 0-5. 앱 아이콘

원본은 `docs/store/icon_master.svg`, Play Console 업로드용 512×512는 `docs/store/icon_512.png`.
모양은 **오도미터 게이지의 바늘이 그대로 체크로 이어지는** 형태 — "주행거리를 기록하고, 상태는 정상".

| 파일 | 용도 |
|---|---|
| `drawable/ic_launcher_background.xml` | 적응형 아이콘 배경 (딥 블루 그라데이션) |
| `drawable/ic_launcher_foreground.xml` | 적응형 아이콘 전경 (컬러) |
| `drawable/ic_launcher_monochrome.xml` | 안드로이드 13+ 테마 아이콘 (시스템이 틴트) |
| `drawable/ic_stat_autolog.xml` | **알림 스몰 아이콘** (24dp, 흰 실루엣) |

지켜야 할 것:

- **안전영역**: 모든 좌표가 중심 `(54,54)` 반지름 **33** 안에 있어야 한다(108 캔버스 기준).
  현재 최대 32.4. 게이지 중심을 체크가 꺾이는 지점(= 바늘 축)에 맞추면서 아치 양 끝이
  대각선으로 멀어지므로, 게이지 크기를 바꿀 때는 반드시 다시 계산할 것.
- **알림 아이콘은 런처 아이콘을 재사용하지 말 것.** 시스템이 단색으로 틴트하므로 컬러가 뭉개지고,
  런처 아이콘은 중앙 65%만 쓰기 때문에 알림에서 너무 작게 보인다.
  그래서 `ic_stat_autolog.xml` 은 뷰포트를 68로 줄여 내용이 경계를 채우게 했다.
- **모노크롬의 허브는 링(evenOdd)** 이다. 단색이라 안쪽을 뚫지 않으면 형태가 사라진다.
- 밀도별 `mipmap-*/ic_launcher*.webp` 는 **없다.** minSdk 29 이므로 `mipmap-anydpi-v26` 이
  항상 선택되고, 적응형 아이콘이 벡터라 래스터 대체본이 필요 없다.
- 릴리스 APK 에서 아이콘을 확인할 때: `optimizedResourceShrinking` 이 **파일명을 난독화**하므로
  (`res/BW.xml`) 파일명으로 grep 하면 안 나온다. `aapt2 dump resources` 로 리소스 이름을 봐야 한다.

## 0-6. 홈 위젯 (Glance)

- 갱신 경로는 셋: ⑴ 데이터 변경 시 `WidgetUpdater`(500ms 디바운스)
  ⑵ 매일 00:05 `DailyWidgetRefreshWorker` — "N일 남음"은 날짜가 지나면 낡기 때문
  ⑶ 앱 실행 시 `ensureScheduled()` 로 체인 복구.
- ⚠️ 일일 갱신은 **자기 재예약 체인**이다(OS 주기 갱신 `updatePeriodMillis=0`).
  워커에서 **다음 예약을 갱신보다 먼저** 걸어야 한다 — 갱신 중 예외로 체인이 죽으면
  위젯을 다시 추가하기 전까지 되살릴 곳이 없다. 앱 실행 시 복구가 이중 안전장치.
- `sizeMode = SizeMode.Responsive` 필수. 지정하지 않으면 Single 모드라
  `LocalSize` 가 항상 최소 크기를 돌려줘 크기 분기가 전부 죽은 코드가 된다.
  버킷: COMPACT(2x2, 요약+최우선 1개) / WIDE(4x2, 요약+목록 3개).
- 색은 Glance 의 day/night `ColorProvider` 로 다크모드를 따라간다.
  진행바(RemoteViews)의 트랙 색은 `values-night/colors_widget.xml` 이 담당.

## 1. DB 스키마를 바꿀 때 (Room)

DB가 로컬에만 있으므로, 스키마 변경은 **기존 사용자의 데이터 유실**로 직결될 수 있습니다.

- 엔티티(테이블/컬럼)를 바꾸면 `AutoLogDatabase`의 `version`을 올리고 **반드시 `Migration`을 추가**한다.
  - 마이그레이션을 빠뜨리면 기존 사용자의 앱이 실행 시 크래시(`IllegalStateException`)하거나, `fallbackToDestructiveMigration` 사용 시 **데이터가 통째로 삭제**된다.
- `exportSchema = true`이므로 `room.schemaLocation`을 지정해 스키마 JSON을 저장하고 git에 포함시켜, 버전 간 마이그레이션을 테스트(`MigrationTestHelper`)할 수 있게 한다.
- 릴리스 전, **이전 버전 DB에서 새 버전으로 업데이트하는 시나리오**를 반드시 한 번 확인한다.

### Android 자동 백업(Auto Backup)은 꺼져 있다 (`allowBackup="false"`)
- 이유: 자동 백업이 **옛 스키마의 Room DB를 새 버전 앱에 자동 복원**하면 마이그레이션 불일치로 크래시/데이터 손상이 날 수 있고, 부분·오래된 데이터가 조용히 복원돼 **불완전한 상태**가 될 수 있다.
- 대신 데이터 보존은 **명시적 백업/복원 + 리마인더 배너 + 공유**로 사용자가 직접 관리하도록 유도한다.
- 부작용: 수동 백업을 하지 않은 사용자는 기기 분실/초기화 시 데이터를 잃는다 → 그래서 백업 유도(배너/빈 화면 복원 진입점)가 중요하다.
- 테스트 시 "깨끗한 새 설치"는 삭제=완전 초기화로 재현된다. (자동 백업이 켜져 있으면 옛 데이터가 되살아나 테스트가 왜곡됐었다.)

## 2. 백업 포맷을 바꿀 때 (⚠️ 이전 백업 호환성)

백업 파일(JSON)은 사용자가 **오래 전에 저장해 둔 것**을 나중에 복원할 수 있어야 한다.
즉 앱을 고도화해 데이터 구조가 바뀌어도 **과거 백업이 계속 복원 가능**해야 한다.

### 현재 구조
- 백업은 `AutoLogBackup`(kotlinx.serialization)을 JSON으로 직렬화한 것이며,
  `backupVersion`(현재 `CURRENT_BACKUP_VERSION`)과 `databaseVersion`을 함께 담는다.
- 복원(`BackupRepository.restoreBackup`)은
  ⑴ `backupVersion`이 현재 값과 **같아야만** 통과(`validateBackup`),
  ⑵ 통과하면 **기존 데이터를 전부 삭제하고** 백업 내용을 엔티티로 insert 한다.
- 복원은 **Room 마이그레이션을 타지 않는다.** 백업 DTO를 곧바로 "현재 스키마"의 엔티티로 만들어 넣는다.

### 데이터 구조를 바꿀 때 지켜야 할 것
1. **필드 추가(하위 호환 변경)**
   - 백업 DTO의 새 필드에는 **기본값을 준다.** (`ignoreUnknownKeys = true`이지만, 값이 없는 필드는 기본값이 있어야 파싱된다.)
   - 이러면 `CURRENT_BACKUP_VERSION`을 올리지 않아도 **과거 백업이 그대로 복원**되고, 새 필드는 기본값으로 채워진다.
2. **파괴적 변경(필드 제거/의미 변경 등)**
   - `CURRENT_BACKUP_VERSION`을 올린다.
   - 단, 올리는 순간 **기존 backupVersion을 가진 과거 백업은 복원이 거부**되므로,
     구버전 백업을 새 포맷으로 바꾸는 **백업 마이그레이션 로직**을 함께 넣어야 한다.
     (예: `backupVersion`에 따라 분기해 구조를 변환한 뒤 복원)
3. **릴리스 전 필수 점검**
   - 새 버전 앱에서 **과거(구버전) 백업 파일을 실제로 복원**해 본다.
   - 복원 후 신규 필드/화면이 기본값으로도 정상 동작하는지 확인한다.

### 요약
> DB를 고도화할 때는 ⑴ Room 마이그레이션(실행 중 앱용)과 ⑵ 백업 포맷 호환성(과거 백업 복원용)을
> **둘 다** 챙겨야 한다. 특히 백업 DTO는 "새 필드엔 기본값", "파괴적 변경엔 버전 업 + 마이그레이션" 원칙을 지킨다.

## 3. 백업 저장 위치

- 저장: `Download/AutoLog/` (MediaStore, API 29+). 권한 불필요, 앱 삭제 후에도 파일 생존.
- 복원: 위 폴더의 백업 목록에서 선택하거나, "다른 파일에서 복원"(SAF)으로 임의 파일 선택.
- 저장 실패 시(저장공간 부족 등) 미완성 파일은 삭제하고 실패를 사용자에게 안내한다.

### ⚠️ MediaStore 소유권과 복원 목록의 한계
- scoped storage에서 앱은 **자기가 만든 파일**만 권한 없이 MediaStore로 조회할 수 있다.
- **앱을 삭제하면 파일의 소유권 연결이 끊긴다.** 파일은 `Download/AutoLog/`에 남지만,
  **재설치한 앱은 소유자가 아니므로** `listAutoLogBackups()` 결과에 나오지 않는다.
- 즉 **재설치·기기 이전 후에는 복원 목록이 비어 보인다.** (데이터가 사라진 것은 아님)
- 이 경우의 정답은 **"다른 파일에서 복원"(SAF)** 이다. SAF는 소유권과 무관하게 파일을 선택·복원할 수 있고 권한도 필요 없다.
- 소유권 없는 파일을 자동 목록화하려면 광범위한 저장소 권한이 필요한데 Play 정책상 지양하며,
  SAF 폴더 접근 권한(`OPEN_DOCUMENT_TREE`)도 재설치 시 사라지므로 **재설치 후 자동 목록화는 불가**하다.
- 따라서 복원 UI는 **목록(같은 설치 상태의 편의) + SAF 직접 선택(범용·재설치 대응)** 을 항상 함께 제공해야 한다.
- 빈 목록 상태에서는 "재설치 시 목록이 비어 보일 수 있고 파일은 폴더에 남아 있으니 '다른 파일에서 복원'을 쓰라"는 안내를 노출한다.
