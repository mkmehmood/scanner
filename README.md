# Jampuri Leaf & Powder Check (Android)

Material 3 quality check for Jampuri tobacco: whole leaf (patta) or powder (choora).
Everything is graded **on the phone**, with no internet and no account. An optional second opinion from Claude is available if you add your own API key.

## What it does
- **Scan box:** drag the box over the tobacco, drag the round handle to resize it, 1x/2x zoom, and a colour map that paints each pixel brown (cured), green (raw), yellow (dry stem or bleached) or red (mold, dark spot, grit).
- **Researcher-grade analysis:** white-balance and exposure normalisation, glare/shadow/skin/background rejection, CIE L*a*b* and HSV classification, Sobel / Laplacian / multi-scale texture, tile colour spread and delta-E uniformity. The engine in `engine/Analyzer.kt` is a verified port of the web page: it gives identical grades, scores, check values and percentages on test photos.
- **Four plain-language checks** per mode, an A / B / C grade, and a "Why this grade?" explanation.
- **3-step hand and smell check** combined with the photo score into a final buy / discount / do-not-buy verdict.
- **Saved checks:** keep up to 20, compare any two side by side, share a report text.
- **English and Urdu** (right-to-left), switchable in the app and following the phone language by default.
- **Optional Smart report (Claude):** your own Anthropic API key, stored only on the phone and sent only to `api.anthropic.com`.

A photo cannot judge smell, moisture or chemicals. Always confirm by smell and touch before buying.

## Build and release
GitHub Actions builds the app on every commit to `main` and refreshes **one rolling release** (tag `latest`):

- Download **TobaccoScanner-release.apk** from the [latest release](../../releases/tag/latest). The debug APK is there too.
- Each commit replaces the APKs in that release and moves the `latest` tag to the new commit. There are no other tags.
- Builds are signed with the release key stored in the repository secrets, so every new APK installs over the previous one (no uninstall needed). The version number goes up with every build.
- Pull requests only build; they do not touch the release.

Locally (Android SDK 34, JDK 17, Gradle 8.9): `gradle assembleRelease`

## Signing
Repository secrets used by the workflow: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Keep a private backup of the keystore: if it is lost, later builds can no longer update installed copies.
Without these secrets the release APK falls back to the debug key.
