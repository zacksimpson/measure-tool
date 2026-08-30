# Measure

A construction calculator for the Light Phone III.

Built on the [Light SDK](https://github.com/lightphone/light-sdk).

---

## Status

Early. This currently replicates the stock LightOS calculator screen exactly, as the foundation
the construction-specific screens will build on top of.

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
