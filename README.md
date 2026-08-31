# Measure

A handy multi-tool and construction calculator for taking measurements on the Light Phone III.

Built on the [Light SDK](https://github.com/lightphone/light-sdk).

![hero image](/measure.png)

---

## Ways to Measure

* **Standard Calculator**, designed to resemble LightOS' Calculator tool as closely as possible
* **Fraction Calculator**, with dedicated keys for fractions, mixed numbers and feet/inches entry
* **Unit Conversion**, for length (inches, feet, yards, millimeters, centimeters, meters)
* **Ruler**, turning the sides of the screen into metric and imperial rulers
* Long-press the result to copy it or view history of previous calculations

---

> [!WARNING]
> The ruler is calibrated against the Light Phone III display's reported physical pixel density. If you've changed your Android display density settings, or if you are attempting to install this anywhere else, Ruler will likely not work at this point. (Also, nothing beats a good ole tape measure.) Treat this as a tool for quick reference, not for anything that requires extreme precision.

---

## Local Setup

<details>
<summary>Building from source</summary>

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

</details>

---

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to reach out [here](mailto:zacksimpson24@gmail.com). Another way to support is to [consider sponsoring](https://github.com/sponsors/zacksimpson). Either way, it means a lot!

## Credits

* [The Light Phone](https://www.thelightphone.com) - Measure was designed to resemble LightOS' Calculator tool as closely as possible. I do not take credit for Light's original design work, which is foundational to this project.
* My [Gerber Center Drive](https://gerbergear.com/products/center-drive-bit-set-molle-compatible-sheath-31-003076n) multi-tool, inspiration for the Ruler idea
