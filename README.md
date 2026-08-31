# Measure

A construction calculator for the Light Phone III.

Built on the [Light SDK](https://github.com/lightphone/light-sdk).

---

## Features

* Standard calculator, replicated pixel-for-pixel from the stock LightOS calculator
* Fraction Calc, with a dedicated key for mixed numbers and feet-inches entry that folds results into feet automatically
* Convert Units, for length (inches, feet, yards, millimeters, centimeters, meters)
* Ruler, turning the sides of the screen into a metric and imperial ruler calibrated to the display's real physical pixel density
* Long-press any result to copy it or pull up your last 6 calculations, shared across every tool

---

> [!WARNING]
> The ruler is calibrated against the display's reported physical pixel density and checks out against the math, but I haven't cross-checked it against a certified tape measure. Treat it as a quick reference, not a precision instrument for anything that matters.

---

## Local setup

This module isn't a standalone Gradle build. It compiles as a subproject of a local `light-sdk`
checkout, the same way other local Light SDK tools do.

Add this to the bottom of `light-sdk`'s `settings.gradle.kts` (local-only, do not commit):

```kotlin
include(":measure")
project(":measure").projectDir = file("/Users/zacksimpson/Dev/measure-tool")
```

Then from inside the `light-sdk` checkout:

```bash
./gradlew :measure:compileDebugKotlin
```

---

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to reach out [here](mailto:zacksimpson24@gmail.com). Another way to support is to [consider sponsoring](https://github.com/sponsors/zacksimpson). Either way, it means a lot!

## Credits

* [The Light Phone](https://www.thelightphone.com) - for building a phone worth making tools for
