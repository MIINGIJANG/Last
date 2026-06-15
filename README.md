<p align="center">

  <img src="assets/Logo.png" width="750">

</p>

| No <img width="10%"> | 22212058 <img width="10%"> |
| --- | --- |
| Name | 장민기 |
| E-mail | mingijang@yu.ac.kr |

---

# Revision history

| Revision date <img width="10%"> | Version # <img width="10%"> | Description <img width="10%"> | Anthor <img width="10%"> |
| --- | --- | --- | --- |
| <img height="50">  |  |  |  |
| <img height="50"> |  |  |  |
| <img height="50"> |  |  |  |
| <img height="50"> |  |  |  |
| <img height="50"> |  |  |  |

---


## 1. 프로젝트 개요

LAST는 등록된 주변기기의 연결 상태를 백그라운드에서 모니터링하고, 연결·해제 시점의 위치·시간·WiFi 정보를 자동 기록합니다. 사용자는 대시보드, 연결 기록, 마지막 위치 지도를 통해 분실 가능성을 줄일 수 있습니다.

| 항목 | 내용 |
|------|------|
| 앱 버전 | **2.0.3 최종** (versionCode 14) |
| 소스 파일 | Kotlin **95개** (main) + 테스트 1개 |
| 플랫폼 | Android 8.0+ (API 26) |
| 언어 | Kotlin 17 |
| UI | Jetpack Compose + Material 3 |
| 아키텍처 | MVVM + Domain / Data / Infrastructure / External |
| DB | Room (SQLite) **v14** — WAL · 부분 인덱스 · 캐시 테이블 · 마이그레이션 |
| 지도 | osmdroid (OpenStreetMap) |
| 위치 | FusedLocationProvider (Google Play Services) |

---

## 2. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│  Presentation (Jetpack Compose)                                     │
│  presentation/app/ — MainActivity · LastApp · KeepAliveTab          │
│  presentation/ — Dashboard · Device · History · Location · Settings │
│       ↕ ViewModel (StateFlow)                                       │
│  LastRepository (Flow shareIn + Transaction + Index + Cache)        │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│  Domain                                                             │
│  index/ · history/ · location/ · util/ · model/                     │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│  Data (Room v14) · External · Infrastructure                        │
│  repository/ · entity/ · dao/ · database/                           │
│  external/ — system · device · location                             │
│  infrastructure/ — Service · monitoring · platform                  │
└─────────────────────────────────────────────────────────────────────┘
```

### 상태 머신 (ConnectionMonitor)

```
IDLE → MONITORING → CONNECTED / DISCONNECTED
                         ↓
              LOCATION_SAVED (위치 성공 시)
                         ↓
              EVENT_RECORDED → STATUS_UPDATED → MONITORING
                         ↓
              FINAL (stopMonitoring)
```

---

## 3. 패키지 구조 (최종)

```
com.last.app/
├── LastApplication.kt
│
├── presentation/
│   ├── app/       MainActivity · LastApp · KeepAliveTab · ViewModelFactory
│   ├── dashboard/ · device/ · history/ · location/ · settings/
│   ├── map/       LastMapView · MapMarkerIconFactory · MapTileSources
│   ├── components/ · layout/ · navigation/ · permission/ · theme/ · util/
│
├── domain/
│   ├── index/     DeviceLocationIndex · RegisteredDeviceIndex · ScannedDeviceIndex · RegisteredDeviceOrdering
│   ├── history/   HistoryTimelineBuilder · HistoryTimelineCache · HistoryEventLabels
│   ├── location/  EventLocationLabelResolver
│   ├── util/      StateSignatures · DateFormats
│   └── model/     dashboard · history · location · device
│
├── data/
│   ├── entity/    Room Entity 9종 (DeviceLatestLocatedEvent 포함)
│   ├── dao/       Room DAO 9개
│   ├── database/
│   │   ├── AppDatabase.kt           # v14
│   │   ├── DatabaseMigrations.kt    # v9→10→11→12→13→14
│   │   ├── DatabaseIndexes.kt       # 부분 인덱스 중앙 관리
│   │   ├── DatabaseRetention.kt     # 보존 정책 · prune 간격 · History LIMIT
│   │   ├── DatabaseQueryLimits.kt   # IN 절 청크 (400)
│   │   └── DatabaseOpenCallback.kt  # WAL · PRAGMA · 인덱스 보장
│   └── repository/LastRepository.kt
│
├── external/
│   ├── system/    BluetoothEventActions · EventIntentParser · SystemEventListener
│   ├── device/    DeviceScanner · DeviceIdentifier
│   └── location/  AddressResolver · FusedLocationService · LocationService
│
└── infrastructure/
    ├── ConnectionMonitorService.kt · BootReceiver.kt
    ├── monitoring/  ConnectionMonitor · ConnectionState · PeriodicLocationRecorder
    └── platform/    PermissionHelper · ContextExtensions
```

**파일명 규칙**
- 화면: `*View.kt` + `*ViewModel.kt`
- 도메인 로직: `*Builder`, `*Cache`, `*Resolver`, `*Index`, `*Ordering`
- extension 전용: `StateSignatures.kt`, `MapMarkerExtensions.kt`, `DeviceIcons.kt`, `HistoryEventLabels.kt`
- 테마: `LastTheme.kt`, `LastColors.kt`

---

## 4. Use Case ↔ 구현 매핑

| UC | 화면 | ViewModel | 핵심 동작 |
|----|------|-----------|-----------|
| UC-01 장치 등록 | DeviceView | DeviceViewModel | 등록/해제, BLE·USB·POWER 스캔, 카테고리별 표시 |
| UC-02 장치 상태 | DashboardView | DashboardViewModel | 연결 상태, 최근 기록 3건, 지도 연동, 장치 필터 |
| UC-03 마지막 위치 | LocationView | LocationViewModel | 장치 선택, 이벤트 상태 카드, KeepAlive 지도 |
| UC-04 연결 기록 | HistoryView | HistoryViewModel | 타임라인, 장치 필터, 좌표별 병렬 지오코딩 |
| UC-05 설정 | SettingsView | SettingsViewModel | 위치·Bluetooth 권한/토글 |

---

## 5. 핵심 최적화 (DSA + DB)

### 5.1 Flow · UI

| 항목 | 내용 |
|------|------|
| `shareIn` | 정렬 기기 목록 · 최신 위치 · 주기 위치 Flow 공유 |
| `stateSignature` | Dashboard·지도 상태 중복 방출 억제 |
| `deviceListSignature` | 등록 기기 목록 변경 감지 (deviceType 포함) |
| History debounce | 250ms + `HistoryTimelineCache` LRU (4엔트리) |
| Geocoder LRU | `AddressResolver` 256엔트리 LRU 캐시 |
| BT 스캔 TTL | 2초 캐시 — 스캔 직후 중복 스캔 방지 |
| BT 연결 추적 | INPUT_DEVICE 프로필 · `isConnected()` fallback · 5초 폴링 |

### 5.2 데이터베이스 (Room v14)

| 항목 | 내용 |
|------|------|
| `device_latest_located_events` | 기기별 최신 위치 이벤트 캐시 — JOIN O(기기 수) |
| `upsertIfNewer` | 캐시 갱신 시 SELECT 없이 단일 SQL |
| 고아 삭제 | `NOT EXISTS` anti-join (prune 시) |
| History 조회 | `devices` JOIN — IN 절 없이 등록 기기 이벤트 |
| 부분 인덱스 | `onCreate`/`onOpen`에서 신규 설치에도 보장 |
| 배치 UPDATE | 연결 상태 `IN (:ids)` 청크 갱신 (400) |
| prune 스로틀 | 24시간 1회 retention 정리 |
| WAL + PRAGMA | `synchronous=NORMAL`, `temp_store=MEMORY`, `PRAGMA optimize` |
| 백업 제외 | Room DB는 클라우드·기기 이전 백업에서 제외 |

### 5.3 ANR 대응

- `KeepAliveTab` — MapView 인스턴스 유지, 탭별 lazy 초기화
- `setScreenActive()` — 비활성 탭 Flow 수집 중단
- OsmDroid 선로딩 + `tileDownloadThreads = 2`

### 5.4 백그라운드 서비스

- Foreground Service (기본 `autoMonitoringEnabled = true`)
- **5초** 주기 BLE 연결 상태 동기화 + 이벤트 기록
- **2초 TTL** Bluetooth 스캔 결과 캐시
- **15분** 주기 위치 저장

---

## 6. 데이터베이스 스키마 (Room v14)

### 테이블 (9종)

| 테이블 | 설명 |
|--------|------|
| devices | 등록·미등록 주변기기 |
| device_connection_events | 연결/해제 이벤트 + 좌표 |
| **device_latest_located_events** | **기기별 최신 위치 이벤트 캐시 (PK: deviceId)** |
| device_locations | 위치 스냅샷 |
| device_last_known_locations | 장치별 주기 위치 (PK: deviceId) |
| system_logs | 시스템·이벤트 로그 |
| wifi_information | WiFi SSID/BSSID/RSSI |
| app_settings | 사용자 설정 |
| users | 사용자 (스키마 예약) |

### 마이그레이션

| 버전 | 주요 변경 |
|------|-----------|
| v10 | events·devices·logs·wifi 인덱스 |
| v11 | devices(eventSource), events 부분 인덱스 |
| v12 | locationId 부분 인덱스, 등록 기기 name 인덱스 |
| v13 | 최신 위치 캐시 테이블 + 백필, MAC 조회 인덱스 |
| **v14** | **캐시 테이블 (eventTime, eventId) 인덱스** |

### 보존 정책

- 이벤트: 180일 · 로그: 90일
- History 조회 LIMIT: **300건** · 로그 observe: 500건
- IN 절 청크: 400건

---

## 7. 화면 구성

| 탭 | 화면 | 설명 |
|----|------|------|
| 대시보드 | DashboardView | 연결 상태·최근 기록 3건·지도, 장치 클릭 필터 |
| 장치 관리 | DeviceView | 등록/미등록, BT·USB·POWER 카테고리, 스캔 |
| 연결 기록 | HistoryView | 날짜별 타임라인, 장치 필터, 연결/해제 색상 |
| 마지막 위치 | LocationView | 지도 마커, 이벤트 상태 카드 (연결/해제만) |
| 설정 | SettingsView | 위치·Bluetooth 토글 |

---

## 8. 빌드 및 실행

### 요구사항

- Android Studio Ladybug 이상 · JDK 17+
- GPS·Bluetooth 지원 실기기 권장

### Debug 빌드

```bash
./gradlew assembleDebug
```

출력: `app/build/outputs/apk/debug/app-debug.apk`

### Release 빌드

1. keystore 생성 (최초 1회):

```bash
keytool -genkeypair -v \
  -keystore last-release.jks \
  -alias last-release \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. 서명 설정:

```bash
cp keystore.properties.example keystore.properties
# keystore.properties 에 비밀번호·경로 입력
```

3. 빌드:

```bash
./gradlew assembleRelease
```

| 조건 | 출력 |
|------|------|
| `keystore.properties` 있음 | `app-release.apk` (서명됨) |
| 없음 | `app-release-unsigned.apk` |

### 권한

- 위치 (FINE / COARSE / BACKGROUND)
- Bluetooth (CONNECT / SCAN, API 31+)
- 알림 (POST_NOTIFICATIONS, API 33+)

---

## 9. 실기기 검증 체크리스트

1. 앱 최초 실행 → 위치·Bluetooth·알림 권한 요청
2. BT 기기 연결/해제 → **대시보드·연결 기록** 5초 이내 반영
3. USB 연결/해제 → 등록 USB 장치 이벤트 기록
4. 연결 해제 시 좌표·주소 저장 (위치 권한 허용 시)
5. 대시보드 장치 클릭 → 지도 필터 / 재클릭 → 전체
6. 마지막 위치 탭 복귀 → ANR 없음
7. 장치 삭제 → 연결 기록·위치·로그·캐시 cascade 삭제
8. 설정 → 위치·Bluetooth 토글 동작 확인
9. v13 → v14 업그레이드 시 인덱스 추가 정상
10. v9 미만 DB → 재설치 필요 (`adb uninstall com.last.app`)

---

## 10. 버전 이력

| Version | DB | versionCode | 주요 변경 |
|---------|-----|-------------|-----------|
| v1.0 | v5 | 1 | MVVM + Flow + 트랜잭션 |
| v1.2 | v6 | 2 | FGS · 장치매칭 · POWER 분리 |
| v1.4 | v9 | 3 | 대시보드 개편 · BT 모니터링 |
| v2.0 | v10–11 | 4–8 | DSA 인덱스 · 패키지 구조 정리 |
| v2.0 | v12 | 9–10 | WAL · 부분 인덱스 · ANR 수정 |
| v2.0 | v13 | 11–12 | DB 캐시 테이블 · DSA/DB 최적화 |
| v2.0.2 | v14 | 13 | 설정·백업·구조 정리 |
| **v2.0.3 최종** | **v14** | **14** | **BT 추적 수정 · 파일명/패키지 최적화 · 데드코드 제거** |

---

## 11. 최종본 산출물

| 항목 | 경로/값 |
|------|---------|
| Kotlin 소스 | `app/src/main/java/com/last/app/` (95개) |
| Room DB | `last_app_database` v14 |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| 서명 템플릿 | `keystore.properties.example` |
| 진입점 | `presentation.app.MainActivity` |
| 백그라운드 | `infrastructure.ConnectionMonitorService` |

---

## 12. 참고 문서

- Analysis Document — Use Case, Domain, UI Prototype
- Design Document — Class Diagram, Sequence, State Machine, MVVM
