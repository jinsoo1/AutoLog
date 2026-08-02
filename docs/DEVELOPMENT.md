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
