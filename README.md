# Crumb Codex — an unofficial Cookie Run: Kingdom companion

A native Android companion app for *Cookie Run: Kingdom*, written in Kotlin and Jetpack Compose (Material 3).

> **Unofficial fan project.** It is not affiliated with or endorsed by Devsisters. The app ships no game
> assets: every picture (cookie mascots, toppings, Beascuits, treasures, candy jars, the launcher icon) is
> original art drawn in code with Compose `Canvas`.

## Features

| Tab | What it does |
| --- | --- |
| **Cookies** | A searchable Cookie-Dex with filters for Rarity, Class, Position and Element, an *Owned* toggle and sorting. Each detail page has an art hero, skill card and separate **PvP / PvE / Guild Boss** build tabs: toppings, Topping Tart, priority substats with target thresholds, Beascuit grade, element and attunement order, play tips, and Magic Candy / Crystal Jam priority. |
| **Collection** | Mark cookies as owned and track level, stars, ascension, skill level, candy level, power, equipped toppings, tart and Beascuit. The **in-game setting comparison** scores your entered stats against each mode's benchmarks and lets you keep a checklist and notes per cookie. |
| **Teams** | A 5-slot builder for Arena, World Exploration and Guild Boss. It shows a Front/Middle/Rear formation preview and checks the lineup (no frontline, duplicates, too many healers, empty slots). The synergy summary covers role counts, elemental coverage and a 0–100 score, and treasures are suggested from what the team needs. |
| **Farming** | A drop finder for Beascuits, Magic Candy, Crystal Jam and topping materials. Filter by category, source (Beast-Yeast, Hard Mode, Monster Menace, Beast Raid…) or element. |
| **Meta** | Tier lists for each mode, and coupon codes with one-tap **Copy**, **Copy & redeem** (opens the official coupon page), a redeemed checkbox, expiry tracking and adding your own codes. |

It also has light, dark and system themes with pastel glassmorphism. On wide screens (tablets, unfolded foldables) the navigation bar becomes a rail and the Cookie-Dex becomes a two-pane list-detail layout.

## Architecture

MVVM with Clean Architecture layers in one module. Dependencies are wired by hand in `AppContainer`, with no DI framework.

```
app/src/main/java/com/crumbcodex/crk/
├── domain/            # Pure Kotlin: models, repository interfaces, use cases (unit-tested)
│   ├── model/         # Cookie, Build, OwnedCookie, Team, Treasure, DropItem, Coupon, enums
│   ├── repository/    # CookieRepository, CollectionRepository, TeamRepository, …
│   └── usecase/       # FilterCookiesUseCase, TeamSynergyCalculator, CompareBuildUseCase
├── data/
│   ├── local/         # Room: CrkDatabase, entities, DAOs, type converters
│   ├── seed/          # JSON DTOs, SeedDataParser (validating), DatabaseSeeder
│   ├── mapper/        # DTO ↔ entity ↔ domain mapping
│   ├── repository/    # Repository implementations (Flow-based)
│   └── prefs/         # Theme preference
├── di/AppContainer.kt
└── ui/
    ├── theme/         # Pastel light/dark colour schemes, typography, shapes
    ├── components/    # GlassCard, AppBackground, CookieAvatar, procedural GameArt, badges…
    ├── navigation/    # Type-safe routes, NavigationSuiteScaffold shell, theme menu
    ├── dex/ detail/ collection/ team/ farming/ meta/   # Screen + ViewModel per feature
    └── util/
```

* **Room** tables: `cookies`, `cookie_builds` (FK → cookies), `owned_cookies`, `teams`, `treasures`,
  `drop_items`, `tier_entries`, `coupons`, `seed_meta`.
* **Seeding**: on launch `DatabaseSeeder` reads `assets/seed/*.json`, validates it and replaces the
  static tables in one transaction when the seed version goes up. User data (collection, teams, your coupons) is never touched.

## Adding or editing cookies (JSON seed)

Edit `app/src/main/assets/seed/cookies.json`, then **bump `"version"`**:

```json
{
  "id": "my_cookie",
  "name": "My Cookie",
  "title": "Short tagline",
  "rarity": "LEGENDARY",          // BEAST ANCIENT LEGENDARY DRAGON SUPER_EPIC EPIC RARE COMMON SPECIAL
  "class": "MAGIC",               // BOMBER CHARGE AMBUSH MAGIC RANGED DEFENSE SUPPORT HEALING
  "position": "MIDDLE",           // FRONT MIDDLE REAR
  "elements": ["ICE"],            // POISON STEEL FIRE WATER EARTH LIGHT DARKNESS ICE ELECTRICITY WIND
  "palette": ["#B8E7FF", "#5C7AEA"],   // frosting + outfit colours of the generated mascot
  "imageUrl": null,               // optional: URL of artwork you have the rights to use
  "skill": { "name": "…", "cooldown": 15, "description": "…" },
  "magicCandy": { "kind": "MAGIC_CANDY", "priority": "HIGH", "summary": "…", "effects": ["…"], "ingredients": ["…"] },
  "builds": [{
    "mode": "ARENA",              // ARENA WORLD_EXPLORATION GUILD_BOSS
    "title": "Cooldown nuker",
    "toppings": [{ "type": "SWIFT_CHOCOLATE", "count": 5 }],   // must total 5
    "tart": { "name": "Chocolate Tart", "stat": "COOLDOWN" },
    "substats": [{ "stat": "COOLDOWN", "min": 25, "max": 29.5, "priority": 1 }],
    "beascuit": { "grade": "LEGENDARY", "element": "ICE", "attunements": ["ATK", "CRIT"] },
    "notes": ["…"]
  }]
}
```

Tier lists, drop sources, treasures and coupon codes live in `game_data.json` (with its own `"version"`).
Coupons in the seed use `{ "code": "…", "rewards": "…", "expires": "2026-12-31", "verified": true }`.
`SeedDataParserTest` checks the bundled files, so a typo in an enum fails the unit tests.

## Data source & artwork

`cookies.json` is generated from the **Cookie Run: Kingdom Wiki** (Fandom, CC BY-SA 3.0) by
`tools/import_wiki.py`, which uses the wiki's MediaWiki API:

```bash
python3 tools/import_wiki.py   # refetches 150+ cookies and bumps the seed version
```

* **Imported (facts only):** rarity, class, position, elements, skill name and cooldown, recommended topping sets
  and stat lines, Magic Candy / Crystal Jam availability, wiki page URL and illustration URL.
* **Not imported:** skill descriptions and other prose, which are the game's own text. Each cookie shows a short
  neutral line and an **Open on wiki** button instead.
* **Overrides:** `tools/overrides.json` holds hand-tuned corrections (whole fields, or individual builds by
  `mode`) and is applied after every import. Re-apply it without refetching with
  `python3 tools/import_wiki.py --offline`.
* **Builds** for Arena / World Exploration / Guild Boss are derived from the wiki's recommended toppings and
  stats, with general per-mode thresholds. Treat them as a starting point and refine by hand.
* **Artwork:** official illustrations are *not* in the repo or APK. `imageUrl` points at the wiki CDN
  (`static.wikia.nocookie.net`) and Coil loads the image at runtime into a 150 MB disk cache. If an image can't
  load, the app falls back to its original procedurally drawn mascot. Artwork © Devsisters.
* Tier lists in `game_data.json` are editorial and need updating each patch. The coupon list ships empty.

## Keeping data up to date

**1. Weekly refresh (GitHub Actions).** `.github/workflows/update-data.yml` runs every Monday, or on demand
from *Actions → Weekly wiki data refresh → Run workflow*. It runs `tools/import_wiki.py`, and only if the data
changed does it build, run the tests and open a **"Weekly wiki data refresh"** pull request. The importer bumps
`version` only on real changes. One-time setup: *Settings → Actions → General → Workflow permissions →* tick
**Allow GitHub Actions to create and approve pull requests**.

**2. Over-the-air updates in the app.** The app downloads `cookies.json` and `game_data.json` from
`BuildConfig.SEED_BASE_URL`, so new data needs no new APK. By default that is the `main` branch's raw seed
folder, and it can be overridden with `-Pcrk.seedBaseUrl=…` or in `gradle.properties`.
* Checks run automatically at most once a day, and on demand from **Meta → ⟳ / Check now**.
* Each file is imported only if its `version` is higher than the stored one. The download is validated before
  anything is written, and user data is never touched.
* The URL must be **publicly readable**. Raw files of a private repo return 404, and the app then reports
  "Couldn't reach the data server" and keeps its current data.

## Coupon codes

Add a code from **Actions → Add coupon code → Run workflow**, which also works in the GitHub mobile app. The
workflow runs `tools/add_coupon.py`, validates the code and the expiry date (YYYY-MM-DD), bumps
`game_data.json`'s version and pushes to `main`. Codes that expired more than 30 days ago are pruned.

Phones pick up new codes on their next data check, from a daily background job (WorkManager) or when the app
opens, and show a **"New coupon code!"** notification. Tapping it opens Meta → Coupons. The notification can be
switched off in the Coupons tab; on Android 13+ the app asks for notification permission there.

Locally: `python3 tools/add_coupon.py ADD CODE --rewards "300 Crystals" --expires 2026-12-31`
(or `REMOVE CODE`).

## Backup & restore

**Collection → ⋮ → Export backup** saves the collection, teams, user-added coupons and redeemed marks as one
JSON file, wherever Android's file picker allows (Drive, Downloads…). **Import backup** merges a file back in.
Cookie entries take the backup's values, teams are added unless an identical one exists, and nothing is
deleted. Invalid files are rejected before anything is written.

## Team suggestions

**Teams → New team → ✨ Suggest … team**, or the ✨ button inside any team, builds the strongest lineup for
that mode. It ranks cookies by their tier in that mode and by your recorded power and level, keeps the lineup
valid (frontline, a healer for PvE and Arena, at most two Front cookies), then improves the combined tier value
plus synergy score one swap at a time. It shows a short reason per pick. With fewer than 5 owned cookies it
fills from the full roster and says so.

## Continuous integration

`.github/workflows/android.yml` runs on every push and pull request. It checks that the overrides reference real
cookies, builds the debug APK, runs the unit tests (including seed validation), and uploads the APK and test
reports as downloadable artifacts.

## Building

Requirements: JDK 17+, Android SDK with platform 35. `settings.gradle.kts` lists Google's Maven Central
mirror ahead of `mavenCentral()` to avoid Central rate limits on shared CI hosts.

```bash
./gradlew assembleDebug        # build the APK
./gradlew testDebugUnitTest    # domain + seed-validation unit tests
```

Stack: AGP 8.7.3, Kotlin 2.0.21, KSP, Compose BOM 2024.12.01, Material 3 + adaptive navigation suite,
Navigation Compose 2.8 (type-safe routes), Room 2.6.1, kotlinx.serialization, Coil 2.7 (only for the optional `imageUrl`).
minSdk 26, targetSdk 35.
