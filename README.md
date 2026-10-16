# Locus — Olympiad Mathematics Preparation App

<p align="center">
  <b>Master Olympiad Mathematics with Adaptive Elo Matchmaking and Socratic AI Tutoring</b><br>
  <i>Tailored for IOQM, RMO, INMO, and International Mathematical Olympiad (IMO) aspirants</i>
</p>

---

## 📐 Overview

**Locus** (Euler IMO) is a native Android application designed to train competitive mathematics olympiad students through adaptive problem matchmaking, publication-grade LaTeX mathematical rendering, progressive pedagogical hints, and a diagnostic Socratic AI tutor.

Built using **Modern Android Architecture (MVVM + Clean Architecture)**, the application leverages an OLED dark-mode design system derived from Google Stitch UI, strict separation of concerns, Dagger-Hilt dependency injection, and Room SQLite persistence.

---

## ✨ Key Features

### 1. ⚔️ Competitive Elo Matchmaking Engine
- **4 Olympiad Domains**: Tracks independent competitive ratings across the core Olympiad pillars:
  - **Algebra** (Polynomials, Functional Equations, Inequalities)
  - **Combinatorics** (Graph Theory, Extremal Principles, Counting & Invariants)
  - **Number Theory** (Diophantine Equations, Modular Arithmetic, Divisibility)
  - **Geometry** (Projective Geometry, Incircle/Circumcircle, Cyclic Quadrilaterals)
- **Dynamic Logistic Curve**: Uses standard FIDE/logistic rating updates with adaptive K-factors based on student experience.
- **Matchmaking Window**: Matches problems within a $\pm 150$ Elo window of the user's current rating, with nearest-neighbor fallback.

### 2. 📝 Academic KaTeX Mathematical Rendering
- Custom `KatexWebView` rendering mathematical expressions in authentic **Computer Modern (CMU Serif)** typography.
- Dynamic JavaScript height resizing ensures smooth inline rendering without layout shifts or clipped equations.
- Seamless presentation of multi-line theorem environments, proofs, summation bounds, and modular notation.

### 3. 💡 Progressive Pedagogical Hints System
- Problems include multi-tiered progressive hints designed as conceptual stepping stones.
- Students can unlock hints sequentially without spoiling the final numerical or algebraic solution.

### 4. 🤖 Euler AI Tutor (LLM Integration)
- Powered by Retrofit and OpenAI/LLM endpoints.
- **Hard-Stuck Trigger**: Automatically diagnoses errors and provides a custom hint when a student submits 3 consecutive incorrect attempts.
- **Socratic Prompt Engineering**: The tutor analyzes previous attempts to identify false lemmas and nudges the student in the right direction without giving away the answer.

### 5. 🗄️ Database Pre-population & Seed Data
- Pre-loaded with authentic **IOQM / RMO / INMO** questions across all 4 categories directly from `assets/questions_seed.json`.
- Automatically seeded on the very first launch via Room's database lifecycle callback.

### 6. 🧭 Jetpack Navigation & Bottom Navigation
- Full Jetpack Navigation Graph routing between:
  - **Dashboard**: Radar profile, aggregate Elo, topic rating breakdown, and quick action cards.
  - **Practice Arena**: Interactive math arena with KaTeX WebView, quick-syntax keypad, progressive hints, and submission verification.
  - **Theory Monographs**: Academic theory reader covering advanced lemmas (e.g., Alon's Nullstellensatz, Vieta Jumping, Barycentric Coordinates).

---

## 🛠️ Architecture & Tech Stack

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
│   ├── ai                 # AI Tutor Repository & Pedagogical Prompt Builder
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
│   ├── practice           # PracticeActivity & PracticeFragment
│   └── theory             # TheoryActivity & TheoryFragment
└── viewmodel              # PracticeViewModel, DashboardViewModel, UiStates
```

| Component | Library / Framework | Version |
| :--- | :--- | :--- |
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

---

## 🚀 Getting Started

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

   # LLM API Key for the Euler AI Tutor (OpenAI API key)
   OPENAI_API_KEY=sk-your-openai-api-key-here
   # Or alternatively:
   LLM_API_KEY=sk-your-openai-api-key-here
   ```
   > [!NOTE]
   > `local.properties` is strictly ignored by `.gitignore` to guarantee your API key remains secure and never gets checked into version control. If no key is set, the app compiles cleanly with AI tutor calls failing gracefully without crashes.

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

---

## 🧪 Running Unit Tests

To run the full suite of unit tests (testing the Elo rating calculator, AI Tutor prompt generator, progressive hints ViewModel state, and seed data parser):

```bash
./gradlew test
```

---

## 🤝 Contributing

Contributions to problem sets, UI animations, and new mathematical modules are welcome!
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/new-olympiad-topic`).
3. Commit your changes using conventional commits (`git commit -m 'feat(data): add INMO 2024 combinatorics problems'`).
4. Push to your branch (`git push origin feature/new-olympiad-topic`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
