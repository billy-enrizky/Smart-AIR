# SMART AIR

A kid-friendly Android application designed to help children (ages 6-16) understand asthma, practice proper inhaler technique, log symptoms and medicine use, and share parent-approved information with healthcare providers via concise, exportable reports.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [User Roles & Features](#user-roles--features)
- [Technical Stack](#technical-stack)
- [Key Features](#key-features)
- [System Architecture Diagrams](#system-architecture-diagrams)
- [User Flows](#user-flows)
- [Data Models](#data-models)
- [Setup & Installation](#setup--installation)
- [Project Structure](#project-structure)
- [Firebase Configuration](#firebase-configuration)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments and Contact](#acknowledgments-and-contact)

## Overview

SMART AIR is an Android application built with Java that provides a comprehensive asthma management solution for children, parents, and healthcare providers. The app emphasizes privacy, safety, and user-friendly design to help children manage their asthma effectively.

### Core Purpose

- **Education**: Help children understand asthma and proper inhaler technique
- **Tracking**: Log medicines, symptoms, triggers, and peak flow readings
- **Safety**: Provide triage guidance and emergency decision support
- **Sharing**: Enable parent-controlled data sharing with healthcare providers
- **Motivation**: Gamify medication adherence with streaks and badges

## Architecture

### System Architecture

```mermaid
graph TB
    subgraph "Android Application"
        A[MainActivity] --> B{User Type?}
        B -->|Child| C[ChildActivity]
        B -->|Parent| D[ParentActivity]
        B -->|Provider| E[ProviderActivity]
        
        C --> F[Inhaler Module]
        C --> G[Safety Module]
        C --> H[Check-in Module]
        
        D --> I[Dashboard]
        D --> J[Children Management]
        D --> K[Provider Sharing]
        D --> L[Reports]
        
        E --> M[Read-only Reports]
    end
    
    subgraph "Firebase Backend"
        N[Firebase Auth]
        O[Realtime Database]
        P[Cloud Functions]
        Q[FCM Notifications]
    end
    
    A --> N
    C --> O
    D --> O
    E --> O
    O --> P
    P --> Q
    Q --> D
```

### User Role Architecture

```mermaid
graph LR
    subgraph "User Hierarchy"
        A[UserData Base Class]
        A --> B[ParentAccount]
        A --> C[ChildAccount]
        A --> D[ProviderAccount]
        
        B --> E[Children List]
        E --> C
        
        B --> F[Invite Codes]
        F --> D
        
        C --> G[Permissions]
        G --> D
    end
```

## User Roles & Features

### 1. Child User

**Access Methods:**
- Independent account with email/password
- Child profile under parent account (username/password, no email)

**Key Features:**
- Log rescue and controller medications
- Practice inhaler technique with step-by-step guidance and video
- Enter peak flow (PEF) readings
- Daily check-in for symptoms and triggers
- View current asthma zone (Green/Yellow/Red)
- Access triage tool for breathing difficulties
- Track streaks and earn badges
- View medication history

**Privacy:**
- Sees only their own data
- Cannot access other children's information

### 2. Parent User

**Key Features:**
- Manage multiple children
- View dashboard with real-time status for each child
- Receive push notifications for:
  - Red zone days
  - Rapid rescue repeats (≥3 in 3 hours)
  - Triage escalations
  - Inventory alerts
- Set personal best (PB) for each child
- Configure controller medication schedule
- Manage inhaler inventory
- Control granular sharing permissions per child
- Generate and share provider reports (PDF/CSV)
- Create and manage provider invite codes

**Privacy Controls:**
- Granular toggles for each data type per child
- Real-time permission updates
- Reversible sharing at any time

### 3. Provider User (Read-Only)

**Access:**
- One-time invite code/link (expires in 7 days)
- Can be revoked by parent at any time

**Key Features:**
- View shared data based on parent permissions
- Access provider reports with charts and statistics
- Read-only access to:
  - Rescue logs (if enabled)
  - Controller adherence summary (if enabled)
  - Symptoms (if enabled)
  - Triggers (if enabled)
  - Peak flow (PEF) (if enabled)
  - Triage incidents (if enabled)
  - Summary charts (if enabled)

## Technical Stack

### Frontend
- **Language**: Java 11
- **Platform**: Android (minSdk 24, targetSdk 36)
- **UI Framework**: Android Material Design Components
- **Architecture**: MVP (Model-View-Presenter) pattern
- **Charts**: MPAndroidChart v3.1.0
- **Calendar**: Material Calendar View 1.9.2

### Backend
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Cloud Functions**: Firebase Cloud Functions (Node.js)
- **Notifications**: Firebase Cloud Messaging (FCM) V1 API
- **File Storage**: Android File System (PDF generation)

### Key Libraries
```gradle
- Firebase BOM: 34.5.0
- Firebase Auth
- Firebase Database
- Firebase Cloud Messaging
- MPAndroidChart: v3.1.0
- Material Calendar View: 1.9.2
```

## Key Features

### R1: Accounts, Roles & Onboarding

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Firebase Auth
    participant Database
    
    User->>App: Launch App
    App->>Firebase Auth: Check Auth State
    alt Not Authenticated
        App->>User: Show Sign In
        User->>App: Enter Credentials
        App->>Firebase Auth: Sign In
        Firebase Auth-->>App: Auth Result
        App->>Database: Load User Data
        Database-->>App: User Account
        alt First Time
            App->>User: Show Onboarding
        else Returning User
            App->>User: Route to Role Home
        end
    else Authenticated
        App->>Database: Load User Data
        Database-->>App: User Account
        App->>User: Route to Role Home
    end
```

**Implementation:**
- Email/password authentication for Parent and Provider
- Username/password for Child profiles
- Role-based routing (`MainActivity` → `ChildActivity` / `ParentActivity` / `ProviderActivity`)
- First-time onboarding flow (`OnBoardingActivity`)
- Protected screens with authentication checks

### R2: Parent/Child Linking & Selective Sharing

```mermaid
graph TD
    A[Parent Account] --> B[Add Child]
    B --> C[Child Profile Created]
    C --> D[Set Permissions]
    
    D --> E{Data Types}
    E --> F[Rescue Logs]
    E --> G[Controller Adherence]
    E --> H[Symptoms]
    E --> I[Triggers]
    E --> J[Peak Flow]
    E --> K[Triage Incidents]
    E --> L[Summary Charts]
    
    D --> M[Generate Invite Code]
    M --> N[Provider Accepts]
    N --> O[Provider Views Shared Data]
    
    D -.->|Real-time Update| O
```

**Implementation:**
- `Permission` class with 7 boolean flags
- `ProvidersFragment` for managing permissions
- `InvitationCreateActivity` for generating invite codes
- `InvitationAcceptActivity` for provider acceptance
- Real-time permission updates in database

### R3: Medicines, Technique & Motivation

```mermaid
stateDiagram-v2
    [*] --> SelectMedicine
    SelectMedicine --> Rescue: Rescue Selected
    SelectMedicine --> Controller: Controller Selected
    
    Rescue --> TechniqueHelper
    Controller --> TechniqueHelper
    
    TechniqueHelper --> Video: Watch Video
    TechniqueHelper --> Steps: Follow Steps
    
    Steps --> PreCheck
    PreCheck --> UseMedicine
    UseMedicine --> PostCheck
    PostCheck --> LogEntry
    
    LogEntry --> UpdateStreak
    UpdateStreak --> CheckBadges
    CheckBadges --> [*]
```

**Implementation:**
- `RescueLog` and `ControllerLog` models
- `ChildInhalerInstructions` with step-by-step guidance
- `ChildInhalerVideo` with embedded video
- `ChildInhalerUse` for medication logging
- `Achievement` class for streaks and badges
- `ControllerSchedule` for adherence tracking

**Badges:**
1. First perfect controller week (7 consecutive scheduled days)
2. 10 high-quality technique sessions
3. Low rescue month (≤4 rescue days in 30 days)

### R4: Safety & Control (PEF, Zones & Triage)

```mermaid
flowchart TD
    A[Child Enters PEF] --> B[Calculate Zone]
    B --> C{Zone?}
    C -->|Green ≥80% PB| D[Green Zone]
    C -->|Yellow 50-79% PB| E[Yellow Zone]
    C -->|Red <50% PB| F[Red Zone]
    
    F --> G[Alert Parent]
    
    H[Triage Button] --> I[Red Flag Check]
    I --> J{Red Flags?}
    J -->|Yes| K[Emergency Decision]
    J -->|No| L[Home Steps]
    
    K --> M[Call Emergency]
    L --> N[10-min Timer]
    N --> O{Improving?}
    O -->|No| P[Escalate]
    P --> G
    O -->|Yes| Q[Continue Monitoring]
    
    I --> R[Log Incident]
    R --> S[Save to History]
```

**Implementation:**
- `ZoneCalculator` for zone calculation
- `PEFEntryActivity` for PEF input
- `TriageActivity` with multi-step flow
- `TriageSession` and `TriageIncident` models
- `AlertDetector` for safety alerts
- `NotificationManager` for FCM push notifications

**Zone Thresholds:**
- Green: ≥80% of Personal Best
- Yellow: 50-79% of Personal Best
- Red: <50% of Personal Best

### R5: Symptoms, Triggers & History

```mermaid
graph LR
    A[Daily Check-in] --> B[Night Waking]
    A --> C[Activity Limits]
    A --> D[Cough/Wheeze Level]
    A --> E[Triggers]
    
    E --> F[Exercise]
    E --> G[Cold Air]
    E --> H[Dust/Pets]
    E --> I[Smoke]
    E --> J[Illness]
    E --> K[Perfume/Cleaners]
    
    A --> L[Save Entry]
    L --> M[History Browser]
    M --> N[Filter by Date]
    M --> O[Filter by Symptoms]
    M --> P[Filter by Triggers]
    M --> Q[Export PDF/CSV]
```

**Implementation:**
- `CheckInView` for daily entry
- `CheckInEntry` model
- `FilterCheckInByDate` and `FilterCheckInBySymptoms` for filtering
- `ViewCheckInHistory` for browsing history
- `ProviderReportGeneratorActivity` for PDF/CSV export

### R6: Parent Home, Notifications & Provider Report

```mermaid
graph TB
    A[Parent Dashboard] --> B[Today's Zone]
    A --> C[Last Rescue Time]
    A --> D[Weekly Rescue Count]
    A --> E[Trend Snippet]
    
    E --> F[7 Days View]
    E --> G[30 Days View]
    
    A --> H[Notifications]
    H --> I[Red Zone Alert]
    H --> J[Rapid Rescue Alert]
    H --> K[Worse After Dose]
    H --> L[Triage Escalation]
    H --> M[Inventory Alert]
    
    A --> N[Provider Report]
    N --> O[Select Date Range]
    O --> P[Generate PDF]
    P --> Q[Share with Provider]
```

**Implementation:**
- `DashboardFragment` with real-time statistics
- `TrendSnippetActivity` with chart visualization
- `ProviderReportGeneratorActivity` for PDF generation
- `NotificationManager` and `AlertDetector` for alerts
- `ChartComponent` for data visualization

**Report Contents:**
- Rescue frequency
- Controller adherence percentage
- Symptom burden counts
- Zone distribution over time
- Notable triage incidents
- Time-series charts (PEF trend)
- Categorical charts (zone distribution, rescue frequency)

## System Architecture Diagrams

### Data Flow Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        A1[Child Activity]
        A2[Parent Activity]
        A3[Provider Activity]
    end
    
    subgraph "Business Logic Layer"
        B1[UserManager]
        B2[CheckInPresenter]
        B3[SignInPresenter]
        B4[AlertDetector]
        B5[ZoneCalculator]
        B6[AdherenceCalculator]
    end
    
    subgraph "Data Access Layer"
        C1[RescueLogModel]
        C2[ControllerLogModel]
        C3[CheckInModel]
        C4[AchievementsModel]
    end
    
    subgraph "Firebase Services"
        D1[Firebase Auth]
        D2[Realtime Database]
        D3[Cloud Functions]
        D4[FCM]
    end
    
    A1 --> B1
    A2 --> B1
    A3 --> B1
    
    B1 --> C1
    B1 --> C2
    B1 --> C3
    
    C1 --> D2
    C2 --> D2
    C3 --> D2
    
    B4 --> D3
    D3 --> D4
    D4 --> A2
```

### Notification Flow

```mermaid
sequenceDiagram
    participant Child
    participant App
    participant Database
    participant CloudFunction
    participant FCM
    participant Parent
    
    Child->>App: Triggers Triage/Alert
    App->>Database: Write Notification Event
    Database->>CloudFunction: Trigger onWrite
    CloudFunction->>Database: Read FCM Token
    CloudFunction->>FCM: Send Push Notification
    FCM->>Parent: Deliver Notification
    Parent->>App: Tap Notification
    App->>Parent: Show Notification Details
```

## User Flows

### Child Medication Logging Flow

```mermaid
flowchart TD
    Start([Child Opens App]) --> Home[Child Home]
    Home --> Select[Select Medicine Type]
    Select --> Rescue{Rescue or Controller?}
    
    Rescue -->|Rescue| R1[Rescue Logging]
    Rescue -->|Controller| C1[Controller Logging]
    
    R1 --> R2[Technique Helper]
    C1 --> C2[Technique Helper]
    
    R2 --> R3[Watch Video]
    C2 --> C3[Watch Video]
    
    R3 --> R4[Follow Steps]
    C3 --> C4[Follow Steps]
    
    R4 --> R5[Pre-Med Check]
    C4 --> C5[Pre-Med Check]
    
    R5 --> R6[Use Medicine]
    C5 --> C6[Use Medicine]
    
    R6 --> R7[Post-Med Check]
    C6 --> C7[Post-Med Check]
    
    R7 --> R8[Save Log]
    C7 --> C8[Save Log]
    
    R8 --> R9[Update Streak]
    C8 --> C9[Update Streak]
    
    R9 --> R10[Check Badges]
    C9 --> C10[Check Badges]
    
    R10 --> End([Complete])
    C10 --> End
```

### Triage Flow

```mermaid
flowchart TD
    Start([Having Trouble Breathing?]) --> RedFlags[Red Flag Questions]
    RedFlags --> Q1{Can speak full sentences?}
    RedFlags --> Q2{Chest pulling in?}
    RedFlags --> Q3{Blue/gray lips/nails?}
    
    Q1 --> Collect[Collect Responses]
    Q2 --> Collect
    Q3 --> Collect
    
    Collect --> RescueQ{Recent rescue attempts?}
    RescueQ -->|Yes| RescueCount[Enter Count]
    RescueQ -->|No| PEFQ
    RescueCount --> PEFQ{Enter PEF?}
    PEFQ -->|Yes| PEFEntry[Enter PEF Value]
    PEFQ -->|No| Calculate
    PEFEntry --> Calculate[Calculate Zone]
    
    Calculate --> Decision{Has Red Flags?}
    Decision -->|Yes| Emergency[Emergency Decision Card]
    Decision -->|No| HomeSteps[Home Steps Card]
    
    Emergency --> Call911[Call Emergency Now]
    HomeSteps --> Timer[10-minute Timer]
    
    Timer --> Recheck{Re-check Symptoms}
    Recheck -->|Not Improving| Escalate[Escalate to Parent]
    Recheck -->|Improving| Continue[Continue Monitoring]
    
    Escalate --> Save[Save Incident]
    Continue --> Save
    Call911 --> Save
    
    Save --> Alert[Alert Parent via FCM]
    Alert --> History[Add to History]
    History --> End([Complete])
```

### Provider Sharing Flow

```mermaid
sequenceDiagram
    participant Parent
    participant App
    participant Database
    participant Provider
    participant ProviderApp
    
    Parent->>App: Open Provider Management
    App->>Parent: Show Children List
    Parent->>App: Select Child
    App->>Parent: Show Permission Toggles
    Parent->>App: Enable/Disable Permissions
    App->>Database: Update Permissions (Real-time)
    
    Parent->>App: Generate Invite Code
    App->>Database: Create Invite Code (7-day expiry)
    Database-->>App: Return Code
    App->>Parent: Display Code
    
    Parent->>Provider: Share Code (Email/SMS/etc)
    Provider->>ProviderApp: Enter Code
    ProviderApp->>Database: Validate Code
    Database-->>ProviderApp: Link Parent-Provider
    
    Provider->>ProviderApp: View Shared Data
    ProviderApp->>Database: Query (Filtered by Permissions)
    Database-->>ProviderApp: Return Allowed Data Only
    ProviderApp->>Provider: Display Report
```

## Data Models

### Core User Models

```mermaid
classDiagram
    class UserData {
        +String ID
        +AccountType Account
        +Boolean FirstTime
        +WriteIntoDatabase()
        +ReadFromDatabase()
    }
    
    class ParentAccount {
        +String Email
        +HashMap~String,ChildAccount~ children
        +InviteCode inviteCode
        +ArrayList~String~ linkedProviders
    }
    
    class ChildAccount {
        +String Parent_id
        +String password
        +String name
        +String dob
        +String age
        +String notes
        +Integer personalBest
        +Permission permission
        +ControllerSchedule controllerSchedule
        +String actionPlanGreen
        +String actionPlanYellow
        +String actionPlanRed
    }
    
    class ProviderAccount {
        +String Email
        +ArrayList~String~ LinkedParentsId
    }
    
    class Permission {
        +Boolean rescueLogs
        +Boolean controllerAdherenceSummary
        +Boolean symptoms
        +Boolean triggers
        +Boolean peakFlow
        +Boolean triageIncidents
        +Boolean summaryCharts
    }
    
    UserData <|-- ParentAccount
    UserData <|-- ChildAccount
    UserData <|-- ProviderAccount
    ChildAccount --> Permission
```

### Medication & Logging Models

```mermaid
classDiagram
    class RescueLog {
        +String username
        +long timestamp
        +String feeling
        +int rating
        +String extraInfo
    }
    
    class ControllerLog {
        +String username
        +long timestamp
        +String feelingB
        +String feelingA
        +int ratingB
        +int ratingA
        +String extraInfo
    }
    
    class ControllerSchedule {
        +HashMap~String,Boolean~ dates
        +getDates()
        +isEmpty()
    }
    
    class Inhaler {
        +String username
        +long datePurchased
        +long dateExpiry
        +int maxcapacity
        +int spraycount
        +boolean isRescue
        +checkExpiry()
        +checkEmpty()
    }
    
    class Achievement {
        +String username
        +int currentStreak
        +int videoswatched
        +List~Long~ rescueTimes
        +List~Boolean~ badges
        +updateStreak()
        +updateStreakTechnique()
        +checkBadge1()
        +checkBadge2()
        +checkBadge3()
    }
```

### Safety & Triage Models

```mermaid
classDiagram
    class PEFReading {
        +int value
        +long timestamp
        +boolean preMedication
        +boolean postMedication
        +String notes
    }
    
    class Zone {
        <<enumeration>>
        GREEN
        YELLOW
        RED
        UNKNOWN
    }
    
    class TriageSession {
        +long startTime
        +Map~String,Boolean~ redFlags
        +boolean rescueAttempts
        +int rescueCount
        +Integer pefValue
        +Zone currentZone
        +String decisionShown
        +boolean escalated
        +String sessionId
    }
    
    class TriageIncident {
        +long timestamp
        +Map~String,Boolean~ redFlags
        +boolean rescueAttempts
        +int rescueCount
        +Integer pefValue
        +String decisionShown
        +Zone zone
        +boolean escalated
        +String sessionId
    }
    
    class ZoneCalculator {
        +calculateZone(int pef, Integer pb) Zone
        +calculatePercentage(int pef, Integer pb) double
    }
    
    PEFReading --> Zone
    TriageSession --> Zone
    TriageIncident --> Zone
    ZoneCalculator ..> Zone
```

### Check-in & Reporting Models

```mermaid
classDiagram
    class CheckInEntry {
        +String username
        +boolean nightWaking
        +String activityLimits
        +double coughWheezeLevel
        +ArrayList~String~ triggers
    }
    
    class DailyCheckin {
        +String username
        +boolean nightWaking
        +String activityLimits
        +double coughWheezeLevel
        +ArrayList~String~ triggers
        +String author
    }
    
    class ProviderReportData {
        +String childName
        +long startDate
        +long endDate
        +int totalRescueUses
        +double controllerAdherence
        +Map~String,Integer~ zoneDistribution
        +List~PEFDataPoint~ pefTrendData
        +List~TriageIncident~ incidents
    }
    
    class NotificationItem {
        +NotificationType type
        +String childId
        +String childName
        +String message
        +long timestamp
    }
    
    CheckInEntry --> DailyCheckin
```

## Setup & Installation

### Prerequisites

- Android Studio (latest version)
- JDK 11 or higher
- Android SDK (minSdk 24, targetSdk 36)
- Firebase project with:
  - Authentication enabled
  - Realtime Database enabled
  - Cloud Functions enabled
  - Cloud Messaging (FCM) enabled

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd CSCB07Project
   ```

2. **Configure Firebase**
   - Add `google-services.json` to `app/` directory
   - Update Firebase project ID in `firebase.json` and `.firebaserc`

3. **Install dependencies**
   ```bash
   ./gradlew build
   ```

4. **Set up Cloud Functions** (optional, for notifications)
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

5. **Build and run**
   - Open project in Android Studio
   - Sync Gradle files
   - Run on emulator or physical device

### Firebase Configuration

The app requires the following Firebase services:

1. **Authentication**
   - Email/Password provider enabled
   - User management in Firebase Console

2. **Realtime Database**
   - Security rules configured in `database.rules.json`
   - Structure: `/users/{userId}/...`

3. **Cloud Functions**
   - Deployed functions in `functions/` directory
   - FCM token management
   - Notification triggers

4. **Cloud Messaging**
   - FCM V1 API enabled
   - Android app registered with FCM

## Project Structure

```
CSCB07Project/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── achievements/          # Streaks and badges
│   │   │   │   ├── auth/                  # Authentication
│   │   │   │   ├── charts/                # Chart components
│   │   │   │   ├── childmanaging/         # Child profile management
│   │   │   │   ├── core/                  # Core utilities (UserManager)
│   │   │   │   ├── dailycheckin/          # Daily check-in feature
│   │   │   │   ├── home/                  # Home activities (Parent, Child)
│   │   │   │   ├── inhaler/               # Inhaler technique and logging
│   │   │   │   ├── medication/            # Medication schedules
│   │   │   │   ├── notifications/          # Push notifications
│   │   │   │   ├── onboarding/            # First-time user onboarding
│   │   │   │   ├── providermanaging/      # Provider invite system
│   │   │   │   ├── providers/             # Provider read-only views
│   │   │   │   ├── reports/               # Report generation
│   │   │   │   ├── safety/                # Triage and PEF
│   │   │   │   ├── SignIn/                # Sign-in MVP
│   │   │   │   ├── userdata/              # User account models
│   │   │   │   └── utils/                 # Utility classes
│   │   │   ├── res/                       # Resources (layouts, drawables)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                          # Unit tests
│   ├── build.gradle.kts
│   └── google-services.json
├── functions/                             # Firebase Cloud Functions
├── docs/                                  # Documentation
├── build.gradle.kts
├── firebase.json
├── database.rules.json
└── README.md
```

## Firebase Configuration

### Database Structure

```
/users/
  /{parentId}/
    /children/
      /{childId}/
        /pefReadings/
        /incidents/
        /rescueUsage/
        /history/
    /fcmToken
    /inviteCode
  /{providerId}/
    /LinkedParentsId

/CheckInManager/
  /{childId}/
    /{date}/

/RescueLogManager/
  /{childId}/
    /{timestamp}/

/ControllerLogManager/
  /{childId}/
    /{timestamp}/

/notifications/
  /{parentId}/
    /{notificationId}/
```

### Security Rules

The database uses Firebase Realtime Database security rules:
- Users can only read/write their own data
- Children data is readable by authenticated users (for provider access)
- Notifications can be written by anyone (for Cloud Functions)
- Notifications are only readable by the parent

## Key Components

### UserManager
Central singleton for managing current user state and Firebase instances.

### ZoneCalculator
Calculates asthma zones (Green/Yellow/Red) based on PEF and Personal Best.

### AlertDetector
Detects safety conditions and triggers notifications:
- Red zone days
- Rapid rescue repeats
- Worse after dose
- Triage escalations

### NotificationManager
Manages FCM push notifications to parents.

### ChartComponent
Shared component for generating charts (line, bar, pie) used in reports and dashboards.

### AdherenceCalculator
Calculates controller medication adherence percentage based on schedule and logs.

## Contributing

1. Follow the existing code structure and patterns
2. Use MVP architecture for new features
3. Ensure all database operations use Firebase Realtime Database
4. Add appropriate error handling and logging
5. Update documentation for new features
6. Test on multiple Android versions (API 24+)

## License

MIT License

##  Acknowledgments and Contact

- billy.suharno@gmail.com (Muhammad Enrizky Brillian)
- pe.zhao@mail.utoronto.ca (Yipeng Zhao)
- kingsleyy.chan@mail.utoronto.ca (Kingsley Chan)
- terry.gao@mail.utoronto.ca (Terry Gao)
- minhanh.dong@mail.utoronto.ca (Minh Anh Dong Nguyen)

