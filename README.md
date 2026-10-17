# Locus — Olympiad Mathematics Preparation App

<p align="center">
  <b>Master Olympiad Mathematics with Adaptive Elo Matchmaking and Socratic AI Evaluation</b><br>
  <i>Tailored for IOQM, RMO, INMO, and International Mathematical Olympiad (IMO) aspirants</i>
</p>

## Overview

**Locus** is a native Android application designed to train competitive mathematics olympiad students through adaptive problem matchmaking, fully offline publication-grade LaTeX mathematical rendering, academic theory monographs, and an AI-powered evaluation engine for subjective solutions.

Built using **Modern Android Architecture (MVVM + Clean Architecture)**, the application leverages an OLED dark-mode design system derived from Google Stitch UI, strict separation of concerns, Dagger-Hilt dependency injection, and Room SQLite persistence.

## Key Features

### 1. Competitive Elo Matchmaking Engine
- **4 Olympiad Domains**: Tracks independent competitive ratings across the core Olympiad pillars:
  - **Algebra** (Polynomials, Functional Equations, Inequalities)
  - **Combinatorics** (Graph Theory, Extremal Principles, Counting & Invariants)
  - **Number Theory** (Diophantine Equations, Modular Arithmetic, Divisibility)
  - **Geometry** (Projective Geometry, Incircle/Circumcircle, Cyclic Quadrilaterals)
- **Dynamic Logistic Curve**: Uses standard FIDE/logistic rating updates with adaptive K-factors based on student experience.
- **Matchmaking Window**: Matches problems within a $\pm 150$ Elo window of the user's current rating, with nearest-neighbor fallback.
- **Configurable Practice Sessions**: Allows students to configure target topic, difficulty tiers (Foundational, Intermediate, Olympiad), and problem counts (5, 10, 15, or Endless mode) without distraction from artificial timers.

### 2. Offline KaTeX Mathematical Rendering & Structural Macros
- **100% Offline Asset Bundling**: KaTeX JavaScript, CSS stylesheets, and all 20 WOFF2/TTF Computer Modern font binaries are bundled locally in the application assets, eliminating CDN latency and enabling offline functionality.
- **Zero FOUC (Flash of Unstyled Content)**: Views initialize with zero opacity and smoothly transition to full visibility only after equation rendering and font layout are complete.
- **Structural Environment Macros**: Extends KaTeX rendering with custom structural environments, rendering `\section{}`, `\subsection{}`, `\begin{theorem}...\end{theorem}`, and `\begin{lemma}...\end{lemma}` as styled academic cards with accent borders and distinct backgrounds.
- **Dynamic Auto-Sizing**: Bidirectional JavaScript-Android bridge dynamically recalculates document height to prevent tall equations, fractions, and matrices from clipping.

### 3. Dual Problem Evaluation Engine (Objective & Subjective)
- **Objective Grading**: Evaluates numerical, modular, and short integer responses directly against verified answer keys.
- **Subjective Short-Answer Semantic Grading**: Employs an LLM grading prompt to accept mathematically equivalent answers regardless of student phrasing (e.g., recognizing "Such a configuration does not exist", "0 solutions", "None", and "Impossible" as strictly equivalent).
- **Subjective Proof Validation**: Presents full official Olympiad proofs with structured lemmas, allowing students to compare proofs and perform structured self-validation without rating penalties.

### 4. Database Pre-population & Seed Data
- Pre-loaded with authentic **IOQM / RMO / INMO** questions across all 4 categories directly from `assets/questions_seed.json`.
- Streamed in memory-safe chunks and automatically seeded on the very first launch via Room's database lifecycle callback.

### 5. Jetpack Navigation & Bottom Navigation
- Full Jetpack Navigation Graph routing between:
  - **Dashboard**: Radar profile, aggregate Elo, topic rating breakdown, and quick action cards.
  - **Practice Arena**: Interactive math arena with offline KaTeX WebView, quick-syntax keypad, and centered evaluation overlays.
  - **Theory Monographs**: Academic theory reader covering advanced lemmas (e.g., Alon's Nullstellensatz, Vieta Jumping, Barycentric Coordinates).

## Architecture & Tech Stack

```
com.locus.olympiad
├── data
│   ├── converter          # Room TypeConverters (JSON lists)
│   ├── dao                # PracticeQuestionDao, UserTopicProfileDao, TheoryModuleDao
│   ├── model              # Room Entities (PracticeQuestion, UserTopicProfile, TheoryModule)
│   ├── LocusDatabase      # Room SQLite Database with onCreate Seeding Callback
│   └── SeedDataParser     # Asset JSON parser for pre-populated problems
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
│   ├── math               # KatexWebView & DataBinding Adapters
│   ├── practice           # PracticeActivity, PracticeFragment & PracticeConfigBottomSheet
│   └── theory             # TheoryActivity & TheoryFragment
└── viewmodel              # PracticeViewModel, DashboardViewModel, UiStates
```

| Component | Library / Framework | Version |
|---|---|---|
| **Language** | Kotlin | `1.9.23` |
| **Build System** | Gradle (Kotlin DSL) / AGP | `8.3.2` |
| **Min / Target SDK** | Android 7.0 (API 24) / Android 14 (API 34) | `24` / `34` |
| **Dependency Injection** | Dagger-Hilt | `2.51.1` |
| **Navigation** | AndroidX Jetpack Navigation Component | `2.7.7` |
| **Local Database** | AndroidX Room SQLite | `2.6.1` |
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

To run the full suite of unit tests (testing the Elo rating calculator, subjective proof self-evaluation, session progression, and seed data parser):

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
