# Conceptualization

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
| 2026년 6월 10일 수요일 <img height="50"> | v1.0 | MVVM + Flow + 트랜잭션 | 장민기 |
| 2026년 6월 10일 수요일 <img height="50"> | v1.2 | FGS · 장치매칭 · POWER 분리 | 장민기 |
| 2026년 6월 10일 수요일 <img height="50"> | v1.4 | 대시보드 개편 · BT 모니터링 | 장민기 |
| 2026년 6월 13일 토요일 <img height="50"> | v2.0 | DSA 인덱스 · 패키지 구조 정리 | 장민기 |
| 2026년 6월 13일 토요일 <img height="50"> | v2.0 | WAL · 부분 인덱스 · ANR 수정 | 장민기 |
| 2026년 6월 13일 토요일 <img height="50"> | v2.0 | DB 캐시 테이블 · DSA/DB 최적화 | 장민기 |
| 2026년 6월 15일 월요일 <img height="50"> | v2.0.2 | 설정·백업·구조 정리 | 장민기 |
| 2026년 6월 15일 월요일 <img height="50"> | v2.0.3 | BT 추적 수정 · 파일명/패키지 최적화 · 데드코드 제거 | 장민기 |

---

# Contents

1. Business purpose
2. System context diagram
3. Use case list
4. Concept of operation
5. Problem statement
6. Glossary
7. References

---

# 1. Business purpose

### 1.1 Project Background

스마트폰 사용자들은 무선 이어폰, 스마트워치, 충전기, USB-C 메모리, 보조배터리 등 다양한 주변기기를 일상적으로 사용한다. 특히 강의실, 실습실, 도서관, 카페, 사무실 등 여러 장소를 이동하며 작업하는 경우가 많아 주변기기를 두고 오거나 분실하는 상황이 빈번하게 발생한다. 이러한 소형 장치들은 크기가 작고 이동이 잦기 때문에 마지막으로 어디에서 사용했는지 기억하기 어려운 경우가 많다.

주변기기를 분실하게 되면 재구매 비용이 발생할 뿐만 아니라, 중요한 자료가 저장된 USB 장치와 같은 경우에는 학업 및 업무에도 큰 영향을 미칠 수 있다. 또한 사용자는 분실한 장치를 찾기 위해 여러 장소를 다시 방문하거나 마지막 사용 위치를 기억에 의존하여 탐색해야 하는 불편함을 겪게 된다.

현재 Google의 Find My Device 또는 삼성 SmartThings Find와 같은 위치 추적 서비스는 일부 인증된 기기에 대해서만 위치 확인 기능을 제공한다. 따라서 일반적인 USB 장치, 유선 주변기기 및 대부분의 블루투스 장치는 위치를 확인하기 어렵고, 사용자는 마지막 사용 위치를 기억에 의존하여 기기를 찾아야 하는 한계가 있다.

Last는 블루투스 및 USB 기반의 주변기기 연결 상태를 모니터링하는 Android 기반의 장치 연결 상태 모니터링 앱인 Last를 제안한다. Last는 블루투스 및 USB 장치의 연결 상태를 지속적으로 모니터링하고, 장치 연결 해제 이벤트가 발생하면 마지막 위치 정보와 연결 기록을 자동으로 저장한다. 사용자는 저장된 연결 이력과 마지막 위치 정보를 조회하여 분실한 주변기기를 보다 효율적으로 찾을 수 있다.

기존 위치 추적 서비스가 특정 기기에 한정되어 있는 것과 달리, Last는 별도의 추적 장치를 추가로 부착하지 않고도 장치의 연결 상태와 마지막 연결 위치를 기록함으로써 사용자의 주변기기 관리와 분실 예방을 지원한다. 또한 장치 연결 기록을 관리함으로써 사용자는 언제, 어디서, 어떤 장치가 연결 또는 연결 해제되었는지 확인할 수 있다.

본 시스템은 실시간 위치 추적보다는 주변기기의 마지막 연결 상태와 사용 기록을 관리하는 것에 중점을 둔다. 이를 통해 사용자는 주변기기의 분실 가능성을 줄이고, 마지막 사용 위치를 기반으로 보다 신속하게 기기를 찾을 수 있을 것으로 기대된다.

### 1.2 Goal

- 주변기기의 연결 상태를 실시간으로 모니터링할 수 있어야 한다.
- 연결 해제 이벤트 발생 시 마지막 위치 정보를 자동으로 저장할 수 있어야 한다.
- 장치 연결 이력과 이벤트 로그를 저장 및 관리할 수 있어야 한다.
- 저장된 마지막 위치 정보와 연결 기록을 사용자가 조회할 수 있어야 한다.
- 주변기기 분실 시 마지막 사용 위치를 기반으로 탐색할 수 있어야 한다.
- 사용자가 장치 연결 및 연결 해제 이력을 통해 주변기기를 효율적으로 관리할 수 있어야 한다.

### 1.3 Target Market

- 스마트폰과 다양한 주변기기를 사용하는 일반 사용자
- 카페, 도서관, 사무실 등 여러 장소를 이동하며 작업하는 사용자
- 블루투스 및 USB 장치를 자주 사용하는 Android 사용자
- 학생, 직장인, 프리랜서와 같은 스마트폰 기반 일상 사용자
- 주변기기 분실 경험이 있거나 분실 예방이 필요한 사용자

---

# 2. System context diagram

본 시스템은 사용자가 등록한 블루투스 및 USB 주변기기의 연결 상태를 모니터링하고, 연결 해제 시 마지막 위치 정보와 연결 기록을 저장하는 기능을 제공한다.

System Context Diagram은 시스템과 외부 엔티티 간의 관계 및 정보 흐름을 표현하기 위해 작성하였다. 본 시스템과 상호작용하는 주요 외부 엔티티는 User, Bluetooth Device, USB Device, Android System, Location Service로 구성된다.

---

### External Entities

- **User** : 시스템을 사용하는 사용자
- **Bluetooth Device** : 시스템에 등록되어 연결 상태를 모니터링하는 블루투스 장치
- **USB Device** : 시스템에 등록되어 연결 상태를 모니터링하는 USB 장치
- **Android System** : 장치 연결 및 연결 해제 이벤트를 제공하는 운영체제
- **Location Service** : 위치 정보를 제공하는 위치 서비스

---

### Data Flow

### User ↔ Last System

- Device Information
- Connection History
- Last Location

### Bluetooth Device ↔ Last System

- Device Information

### USB Device ↔ Last System

- Device Information

### Android System ↔ Last System

- Connection Event

### Location Service ↔ Last System

- Location Information

---

사용자는 Last System을 통해 장치를 등록하고 연결 상태, 연결 기록 및 마지막 위치 정보를 조회할 수 있다. 시스템은 Android System으로부터 장치 연결 및 연결 해제 이벤트를 수신하며, Location Service로부터 위치 정보를 제공받아 마지막 위치를 저장한다. 또한 저장된 연결 기록과 위치 정보를 사용자에게 제공하여 주변기기 관리 및 분실 예방을 지원한다.

---

<p align="center">

  <img src="assets/System context.png" width="1000">

</p>

---

# 3. Use Case List

**1) Register Device**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Actor | User |
| Description | 사용자가 블루투스 또는 USB 장치를 시스템에 등록한다. |

**2) Monitor Device Connection**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Actor | User |
| Description | 사용자가 등록된 장치의 연결 상태를 확인한다. 시스템은 연결 및 연결 해제 이벤트를 감지하고, 마지막 위치 정보와 연결 기록을 자동으로 저장하며 필요한 경우 사용자에게 알림을 제공한다. |

**3) View Last Location**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Actor | User |
| Description | 사용자가 저장된 장치의 마지막 위치 정보를 조회하여 분실한 주변기기의 마지막 사용 위치를 확인한다. |

**4) View Connection History**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Actor | User |
| Description | 사용자가 장치의 연결 및 연결 해제 기록을 조회하여 장치 사용 이력을 확인한다. |

**5) Manage Settings**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Actor | User |
| Description | 사용자가 알림, 위치 정보 사용 및 기타 시스템 설정을 관리한다. |

---

# 4. Concept of Operation

**1) Register Device**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Purpose | 사용자가 추적 및 관리할 주변기기를 시스템에 등록하기 위함 |
| Approach | 사용자가 블루투스 또는 USB 장치를 선택하여 등록 |
| Dynamics | 사용자가 새로운 주변기기를 등록할 경우 |
| Goals | 등록된 주변기기를 지속적으로 관리할 수 있도록 지원 |

**2) Monitor Device Connection**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Purpose | 등록된 주변기기의 연결 상태를 지속적으로 확인하기 위함 |
| Approach | 시스템이 백그라운드에서 연결 및 연결 해제 이벤트를 모니터링 |
| Dynamics | 등록된 주변기기가 연결되거나 연결이 해제될 경우 |
| Goals | 주변기기의 상태를 관리하고 연결 해제 시 마지막 위치 및 연결 기록을 자동으로 저장 |

**3) View Last Location**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Purpose | 사용자가 주변기기의 마지막 사용 위치를 확인하기 위함 |
| Approach | 저장된 위치 정보를 사용자에게 제공 |
| Dynamics | 사용자가 특정 주변기기의 위치 조회를 요청할 경우 |
| Goals | 분실한 주변기기의 마지막 사용 위치를 파악하여 보다 쉽게 찾을 수 있도록 지원 |

**4) View Connection History**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Purpose | 사용자가 주변기기의 사용 이력을 확인하기 위함 |
| Approach | 저장된 연결 및 연결 해제 기록을 조회하여 제공 |
| Dynamics | 사용자가 특정 주변기기의 기록 조회를 요청할 경우 |
| Goals | 주변기기의 사용 이력과 마지막 연결 상태를 확인할 수 있도록 지원 |

**5) Manage Settings**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Purpose | 사용자가 시스템 환경을 자신의 사용 목적에 맞게 설정하기 위함 |
| Approach | 알림, 위치 정보 사용 및 기타 시스템 설정을 변경 |
| Dynamics | 사용자가 설정 화면에서 환경설정을 변경할 경우 |
| Goals | 사용자 맞춤형 시스템 환경을 제공하여 편의성을 향상 |

---

# 5. Problem Statement

**1. System Inactive State Limitation**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Problem | 시스템이 종료되거나 비활성 상태인 경우 장치 상태 변화를 감지할 수 없음 |
| Example | 사용자가 스마트폰 앱을 종료하거나 백그라운드에서 강제 종료한 후 주변기기의 연결이 해제되는 경우 |
| Consideration | 시스템 실행 중 마지막 연결 상태를 기록하여 데이터 손실을 최소화하도록 설계 |

**2. Bluetooth Disconnect Reliability**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Problem | 블루투스 장치는 일시적인 연결 해제가 발생할 수 있어 실제 분실 상황과 구분이 어려울 수 있음 |
| Example | 절전 모드 전환 또는 전파 간섭으로 인해 일시적으로 연결이 끊기는 경우 |
| Consideration | 단순 연결 해제만으로 분실 여부를 판단하지 않고 이벤트 기록 중심으로 관리 |

**3. Location Accuracy Limitation**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Problem | 위치 정보의 정확도에 따라 마지막 위치 기록이 실제 위치와 차이가 발생할 수 있음 |
| Example | 강의실, 도서관, 실습실과 같은 실내 공간에서 위치 오차가 발생하는 경우 |
| Consideration | 위치 정보와 함께 주변 환경 정보를 저장하여 위치 식별을 보완 |

**4. Android Permission and Background Constraints**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Problem | 위치 정보 및 장치 상태 접근을 위해 필요한 권한이 허용되지 않을 수 있음 |
| Example | 사용자가 위치 서비스 또는 Bluetooth 접근 권한을 비활성화한 경우 |
| Consideration | 필요한 권한을 사용자에게 안내하고 Android 백그라운드 실행 제한 정책을 고려하여 안정적인 백그라운드 동작을 고려 |

**5. Event Record Storage Failure**

| **Item** <img width="10%"> | **Description** <img width="1000"> |
| --- | --- |
| Problem | 이벤트 기록 또는 위치 정보 저장이 실패할 경우 마지막 사용 정보를 확인할 수 없음 |
| Example | 시스템 오류 또는 예기치 않은 종료로 인해 기록이 저장되지 않는 경우 |
| Consideration | 중요한 이벤트가 누락되지 않도록 기록 저장 방안을 고려 |

---

# 6. Glossary

| Term <img width="10%"> | Description <img width="1000"> |
| --- | --- |
| Last | 주변기기의 마지막 위치와 연결 이력을 관리하는 시스템 |
| User | 시스템을 사용하는 사용자 |
| Peripheral Device | 노트북과 함께 사용하는 주변기기 |
| Bluetooth Device | Bluetooth로 연결되는 무선 주변기기 |
| USB Device | USB-C 포트를 통해 연결되는 충전기 및 유선 주변기기 |
| Device Status | 주변기기의 현재 연결 상태 정보 |
| Device Connection | 주변기기가 시스템과 연결된 상태 |
| Disconnect Event | 주변기기의 연결이 종료되는 시스템 이벤트 |
| Background Monitoring | 시스템이 백그라운드에서 장치 상태를 감시하는 기능 |
| Last Location | 주변기기의 마지막 연결 종료 시점에 기록된 위치 정보 |
| Connection History | 장치의 연결 및 연결 해제 이력 정보 |
| Event Log | 장치 상태 변화와 관련된 기록 데이터 |
| Location Service | 현재 위치 정보를 제공하는 시스템 서비스 |
| System Event | 운영체제에서 발생하는 장치 관련 이벤트 |
| Local Storage | 이벤트 기록과 위치 정보를 저장하는 공간 |
| Notification | 장치 상태 변화와 관련하여 사용자에게 제공되는 알림 |
| Permission | 시스템 기능 사용을 위해 필요한 사용자 권한 |
| Android System Google | 장치 이벤트를 제공하는 Apple 운영체제 환경 |

---

# 7. References

[1] Software Engineering Course Materials

시스템 분석, 요구사항 정의 및 UML 모델링 방법 참고

[2] Android Developer Documentation – Location (FusedLocationProvider) API

위치 정보 수집 및 위치 서비스 기능 조사

[3] Android Developer Documentation – Bluetooth (BluetoothManager) API

Bluetooth 장치 연결 및 상태 감지 기능 조사

[4] Android Developer Documentation – USB Host & Accessory (UsbManager) API

USB 장치 연결 및 시스템 이벤트 처리 방식 조사

[5] Android Developer Documentation – Notifications API

Android 알림 기능 구현 방식 조사

[6] GitHub Open Source Projects

Bluetooth 및 장치 모니터링 구현 사례 분석

[7] Material Design Guidelines

Android 애플리케이션 UI/UX 설계 원칙 참고

---
