# GlassCalc

An Android calculator with two modes:

1. **Standard** — a normal calculator, but every button press builds a Python
   expression under the hood, evaluated by an embedded CPython interpreter
   (Chaquopy). No custom expression parser to maintain.
2. **Python Formula** — a full code editor where you write real, multi-line
   Python (loops, conditionals, `math.*`, whatever) to compute a `result`.
   Formulas can be named and saved for later.

UI follows Apple's iOS 26 "Liquid Glass" language — translucent, blurred
panes with a bright top-left / dim bottom-right border, floating over soft
colored light "planes" in the background.

## Project layout

```
GlassCalc/
├── app/
│   ├── build.gradle.kts           # Chaquopy + Compose config
│   └── src/main/
│       ├── java/com/joshua/glasscalc/
│       │   ├── MainActivity.kt        # All Compose UI
│       │   ├── CalculatorViewModel.kt # State for both modes
│       │   ├── FormulaStore.kt        # DataStore persistence for saved formulas
│       │   ├── PythonBridge.kt        # Kotlin ↔ Chaquopy wrapper
│       │   └── ui/theme/              # Liquid Glass components (GlassPane, GlassButton…)
│       └── python/
│           └── formula_engine.py      # Runs user code, returns JSON result
├── .github/workflows/build.yml    # Cloud build — no local Android Studio needed
└── settings.gradle.kts / build.gradle.kts
```

## Building (matches your existing GitHub Actions workflow)

This project has **no committed Gradle wrapper jar** (binary files can't be
generated from this chat environment). The CI workflow sidesteps that by
using `gradle/actions/setup-gradle` to install Gradle 8.7 directly and
running `gradle assembleDebug` instead of `./gradlew assembleDebug`.

1. `git init && git add . && git commit -m "init"` inside this folder
2. Push to a new GitHub repo
3. GitHub Actions builds automatically on push to `main` (or trigger manually
   from the Actions tab — "Build APK" → "Run workflow")
4. Download the APK from the workflow run's **Artifacts** section

If you'd rather build locally later with a real Android Studio / SDK, run
`gradle wrapper --gradle-version 8.7` once inside the project to generate
the missing `gradlew` + wrapper jar, then commit those.

## Writing a custom formula

Anything you type in the Formula tab runs as real Python. The only rule:
bind your final answer to a variable named `result`.

```python
# compound interest
result = principal * (1 + rate / n) ** (n * years)
```

```python
# sum of squares up to n
total = 0
for i in range(1, n + 1):
    total += i ** 2
result = total
```

`math` is imported for you (`sin`, `sqrt`, `pi`, `log`, …), and `print()`
inside a formula shows up under the result so you can debug loops.

## Chaquopy licensing note

Chaquopy (the embedded Python interpreter) is free for personal/open-source
use. If you plan to ship this as a closed-source commercial app at scale,
check current terms at chaquo.com/chaquopy/pricing before release — that's
a business decision, not a code change.

## Known gaps / next steps

- No app icon PNGs shipped — only a vector adaptive icon (`ic_launcher_foreground.xml`).
  Swap in real artwork whenever you want a polished icon.
- `GlassButton`'s "planes" glow uses `Modifier.blur()`, which only renders
  real blur on API 31+ (Android 12+). Below that it still looks translucent,
  just without the blur — no crash, just a flatter look on old devices.
- Saved formulas persist via DataStore (`glasscalc_formulas` prefs file) —
  no cloud sync, purely local.
