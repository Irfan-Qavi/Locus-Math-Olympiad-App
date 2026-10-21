# Locus — Olympiad Mathematics Preparation App

<p align="center">
  <b>Master Olympiad Mathematics with Adaptive Elo Matchmaking and Socratic AI Evaluation</b><br>
  <i>Tailored for IOQM, RMO, INMO, and International Mathematical Olympiad (IMO) aspirants</i>
</p>

## Overview

**Locus** is a native Android application designed to train competitive mathematics olympiad students through adaptive problem matchmaking, fully offline publication-grade LaTeX mathematical rendering, academic theory monographs, an AI-powered semantic grading engine, and silent cloud synchronization via Cloud Firestore.

Built using **Modern Android Architecture (MVVM + Clean Architecture)**, the application leverages an OLED dark-mode design system derived from Google Stitch UI, strict separation of concerns, Dagger-Hilt dependency injection, Room SQLite persistence, and Android WorkManager for background sync.

## Key Features

### 1. Competitive Elo Matchmaking Engine & Non-Repeating Problem Pool
- **4 Olympiad Domains**: Tracks independent competitive ratings across the core Olympiad pillars:
  - **Algebra** (Polynomials, Functional Equations, Inequalities)
  - **Combinatorics** (Graph Theory, Extremal Principles, Counting & Invariants)
  - **Number Theory** (Diophantine Equations, Modular Arithmetic, Divisibility)
  - **Geometry** (Projective Geometry, Incircle/Circumcircle, Cyclic Quadrilaterals)
- **Dynamic Logistic Curve**: Uses standard FIDE/logistic rating updates with adaptive K-factors based on student experience.
- **Matchmaking Window**: Matches problems within a $\pm 150$ Elo window of the user's current rating, with nearest-neighbor fallback.
- **Zero Question Repetition**: Database queries dynamically exclude previously solved problem IDs (`WHERE id NOT IN (:solvedIds)`), guaranteeing unique problems across sessions.
- **Tier Exhaustion State**: Detects when all questions for a selected tier/topic configuration have been solved, triggering a congratulatory celebration dialog (*"You have conquered this tier! No more questions available right now."*) and concluding the session cleanly.
- **Configurable Practice Sessions**: Allows students to configure target topics, difficulty tiers (Foundational, Intermediate, Olympiad), and problem counts (5, 10, 15, or Endless mode) without distraction from artificial timers.
- **Risk-Free Practice Mode**: Option to train in sandbox mode where rating delta is strictly 0 for all answers, allowing stress-free problem exploration.
- **Show Answer Functionality**: Instant surrender option deducting a standard 1.1x penalty (or 0 in Risk-Free Mode), revealing the official answer and transitioning directly to the next question.

### 2. Offline KaTeX Mathematical Rendering & Layout Engine
- **100% Offline Asset Bundling**: KaTeX JavaScript, CSS stylesheets, and all 20 WOFF2/TTF Computer Modern font binaries are bundled locally in application assets, eliminating CDN latency.
- **Decompression Bypass**: Font binaries (`.woff2`) bypass Gradle aapt compression (`noCompress "woff2"`), loading with zero decompression overhead at runtime.
- **Zero FOUC (Flash of Unstyled Content)**: Views initialize with zero opacity and smoothly transition to full visibility only after equation rendering and font layout are complete.
- **Structural Environment Macros**: Extends KaTeX rendering with custom structural environments, rendering `\section{}`, `\subsection{}`, `\begin{theorem}...\end{theorem}`, and `\begin{lemma}...\end{lemma}` as styled academic cards with accent borders and distinct backgrounds.
- **Single-Shot Layout Measuring**: Prevents infinite measuring loops and eliminates phantom WebKit margins through strict viewport metadata, bounded scrollHeight calculations, and thresholded Android layout updates.

### 3. Dual Problem Evaluation Engine (Objective & Subjective)
- **Objective Grading with Full LaTeX Solutions**: Evaluates numerical, modular, and short integer responses directly against verified answer keys. Upon completion, displays the full step-by-step mathematical breakdown and proof derivation via KaTeX.
- **Olympiad-Grade Semantic AI Grading**: Employs an LLM grading prompt with strict mathematical and semantic equivalence rules:
  - Fractions and decimals are recognized as identical real values (e.g., $1/2 \equiv 0.5$).
  - Isolated variable assignments vs scalar values (e.g., $x = 2 \equiv 2$).
  - Algebraic and LaTeX formatting variations (e.g., $x(x+1) \equiv x^2+x$).
  - Non-existence and impossibility synonyms (e.g., "No solution" $\equiv$ "None" $\equiv$ "Impossible" $\equiv$ $\emptyset$).
  - Resilient JSON regex extraction handling markdown fences (` ```json ... ``` `).
- **Streamlined Subjective Proof Validation**: Two-step proof workflow ("I HAVE SOLVED IT" vs "I COULDN'T SOLVE IT") immediately revealing the full canonical proof with a single "Next Problem" button and neutral rating impact.

### 4. Background Cloud Firestore Synchronization
- **Zero UI Latency**: The app reads strictly from the local Room database, ensuring 0 ms network delay during practice and study sessions.
- **RemoteContentSyncWorker**: Background WorkManager worker periodically checks Cloud Firestore in batches of 100 for newly published Olympiad questions and theory monographs.
- **Differential Insertion**: Compares incoming Firestore payloads against existing Room records, inserting only new content to conserve bandwidth and memory.

### 5. Production Optimization & R8 Minification
- **R8 Code Stripping & Resource Shrinking**: Release builds enable full code obfuscation, dead-code removal, and resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **Comprehensive ProGuard Safeguards**: Custom `proguard-rules.pro` strictly protects Gson serialization models, KaTeX `@JavascriptInterface` bridges (`MathResizeBridge`), Room entities/DAOs, and WorkManager workers.
- **WebP Asset Optimization**: High-resolution launcher assets converted to WebP format, reducing asset footprint by over 96%.

### 6. Jetpack Navigation, Session Lifecycle & Interruption Safety
- **Full Jetpack Navigation Graph**: Configured with single-top routing (`launchSingleTop = true`) and `popUpTo` state restoration, avoiding backstack duplication between Dashboard, Practice, and Theory.
- **Practice Tab Interception**: Bottom dock Practice tab intercepts navigation to launch the pre-practice configurator bottom sheet.
- **Session Lifecycle Protection**: Active session state is persisted across app minimizes and back presses. If navigating away during an active session, users are prompted to either continue practicing or stop the session.
- **Explicit End Practice**: Toolbar action allowing clean conclusion of an active session with 0 penalty and instant session summary reporting.

## Architecture & Tech Stack

```
com.locus.olympiad
├── data
│   ├── converter          # Room TypeConverters (JSON lists)
│   ├── dao                # PracticeQuestionDao, UserTopicProfileDao, TheoryModuleDao
│   ├── model              # Room Entities (PracticeQuestion, UserTopicProfile, TheoryModule)
│   ├── LocusDatabase      # Room SQLite Database with onCreate Seeding Callback
│   ├── DatabaseInitializer# Authentic IOQM/RMO/INMO seed problems
│   └── UserPreferencesRepository # Session state, user settings & solved problem IDs
├── di                     # Dagger-Hilt DI Modules (NetworkModule, DatabaseModule)
├── domain
│   ├── ai                 # AI Evaluator Repository & Semantic Grading Models
│   └── elo                # Logistic Elo Calculator & Window Matchmaker
├── network
│   ├── model              # OpenAI Chat Completion Request/Response DTOs
│   ├── AiTutorApiService  # Retrofit API Service Interface
│   └── AiTutorClient      # OkHttp & Retrofit Client Builder
├── repository             # MatchmakingRepository & AiTutorRepository Implementations
├── ui
│   ├── dashboard          # DashboardActivity & DashboardFragment
│   ├── main               # MainActivity (NavHostFragment & BottomNavigationView)
│   ├── math               # KatexWebView (Offline LaTeX Engine) & MathResizeBridge
│   ├── practice           # PracticeActivity, PracticeFragment & PracticeConfigBottomSheet
│   └── theory             # TheoryActivity & TheoryFragment
├── viewmodel              # PracticeViewModel, DashboardViewModel, UiEvent, UiStates
└── worker                 # RemoteContentSyncWorker (WorkManager + Firestore Sync)
```

| Component | Library / Framework | Version |
|---|---|---|
| **Language** | Kotlin | `1.9.23` |
| **Build System** | Gradle (Kotlin DSL) / AGP | `8.3.2` |
| **Min / Target SDK** | Android 7.0 (API 24) / Android 14 (API 34) | `24` / `34` |
| **Dependency Injection** | Dagger-Hilt | `2.51.1` |
| **Navigation** | AndroidX Jetpack Navigation Component | `2.7.7` |
| **Local Database** | AndroidX Room SQLite | `2.6.1` |
| **Background Sync** | AndroidX WorkManager & Google Play Services Tasks | `2.9.0` / `1.8.0` |
| **Cloud Database** | Firebase BoM & Cloud Firestore KTX | `32.8.0` |
| **Asynchronous** | Kotlin Coroutines & StateFlow | `1.8.0` |
| **Networking** | Retrofit 2, OkHttp 3, Gson | `2.9.0` / `4.12.0` |
| **UI & Styling** | Material Components 3, ViewBinding, DataBinding | `1.11.0` |
| **Unit Testing** | JUnit 4, MockK, Coroutines Test | `4.13.2` / `1.13.10` |

## Getting Started

### Prerequisites
1. **Android Studio**: Iguana (2023.2.1), Jellyfish (2023.3.1), Koala, or Ladybug+.
2. **Java Development Kit (JDK)**: JDK 17 (bundled with modern Android Studio).
3. **Android SDK**: API Level 34 platform & build tools.

### Setup Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/LocusMathOlympiad.git
   cd "Locus Math Olympiad App"
   ```

2. **Configure API Keys (`local.properties`)**:
   Copy the provided `local.properties.example` to `local.properties`:
   ```properties
   # Location of your Android SDK
   sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk

   # LLM API Key for the AI Subjective Evaluator (OpenAI API key)
   OPENAI_API_KEY=sk-your-openai-api-key-here
   # Or alternatively:
   LLM_API_KEY=sk-your-openai-api-key-here
   ```
   > [!NOTE]
   > `local.properties` is strictly ignored by `.gitignore` to guarantee your API key remains secure and never gets checked into version control. If no key is set, the app compiles cleanly with semantic evaluations falling back to rule-based mathematical matching.

3. **Open & Sync in Android Studio**:
   - Launch Android Studio and choose **Open Project**.
   - Select the root folder of the project.
   - Allow Gradle to perform the initial sync and download dependencies.

4. **Build & Run**:
   - Connect a physical Android device (with USB Debugging enabled) or start an Android Virtual Device (AVD).
   - Press **Run 'app'** (`Shift + F10`) in Android Studio, or execute from the terminal:
     ```bash
     ./gradlew assembleDebug
     ```

## Running Unit Tests

To run the full suite of unit tests (testing the Elo rating calculator, subjective proof self-evaluation, session progression, tier conquest completion, and question matchmaking):

```bash
./gradlew test
```

## Contributing

Contributions to problem sets, UI animations, and new mathematical modules are welcome!
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/new-olympiad-topic`).
3. Commit your changes using conventional commits (`git commit -m 'feat(data): add INMO 2024 combinatorics problems'`).
4. Push to your branch (`git push origin feature/new-olympiad-topic`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
