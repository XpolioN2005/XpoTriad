# Build

## Requirements

| Tool | Version |
|---|---|
| Java | 25 |
| Paper API | 26.3-140-main |
| Build system | Pure `javac` + `jar` |

No Maven, Gradle, or any dependency manager is used.  
All required JARs must be present in `lib/`.

---

## Project Layout

```
XpoTriad/
├── docs/                        ← project documentation
│   ├── README.md
│   ├── architecture.md
│   ├── flows.md
│   ├── code-guide.md
│   └── build.md
├── lib/                         ← required JARs (not committed)
│   ├── paper-api-26.3.build.140-beta.jar
│   ├── adventure-api-5.2.0.jar
│   ├── adventure-key-5.2.0.jar
│   ├── annotations-26.1.0.jar
│   └── guava-33.4.8-jre.jar
├── src/
│   └── main/
│       ├── java/
│       │   └── dev/xpolion/xpotriad/
│       │       ├── Main.java
│       │       ├── ability/
│       │       │   ├── Ability.java
│       │       │   ├── AbilityContext.java
│       │       │   ├── AbilityEngine.java
│       │       │   ├── AbilityItem.java
│       │       │   └── AbilityListener.java
│       │       ├── effects/
│       │       │   ├── Effect.java
│       │       │   ├── ExplosionEffect.java
│       │       │   ├── InvisibilityEffect.java
│       │       │   └── SpeedEffect.java
│       │       ├── fragment/
│       │       │   ├── Fragment.java
│       │       │   ├── FragmentItem.java
│       │       │   ├── FragmentRegistry.java
│       │       │   ├── ExplosionFragment.java
│       │       │   ├── InvisibilityFragment.java
│       │       │   └── SpeedFragment.java
│       │       └── targeting/
│       │           └── TargetResolver.java
│       └── resources/
│           └── plugin.yml
└── build/                       ← generated; not committed
    ├── classes/
    └── XpoTriad.jar
```

---

## Build Command (PowerShell)

Run from the project root (`XpoTriad/`):

```powershell
if (Test-Path build) { Remove-Item build -Recurse -Force }
New-Item -ItemType Directory -Force build/classes | Out-Null
$classpath = (Get-ChildItem lib -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
if (-not $classpath) { throw 'No JARs found in lib/' }
$sources = Get-ChildItem src/main/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
javac -cp $classpath -d build/classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Copy-Item src/main/resources/plugin.yml build/classes/
Copy-Item src/main/resources/config.yml build/classes/
jar -cf build/XpoTriad.jar -C build/classes .
```

Output: `build/XpoTriad.jar`

---

## `plugin.yml`

```yaml
name: XpoTriad
version: 1.0.0
main: dev.xpolion.xpotriad.Main
api-version: "26.3"

commands:
  xptest:
    description: Test XpoTriad ability system
```

---

## Installation

1. Drop `build/XpoTriad.jar` into your Paper server's `plugins/` folder.
2. Start or reload the server.
3. Use `/xptest` in-game (as a player) to receive a test ability weapon.

---

## Testing in-game

```
/xptest
```

Gives:
- 1× Diamond Sword engraved with an Ability (Invisibility → Speed → Explosion)
- 1× Invisibility Fragment item
- 1× Speed Fragment item
- 1× Explosion Fragment item

**To test ability activation:** right-click while holding the sword in your main hand.  
**To test item-specific cooldown:** right-click again immediately to observe the cooldown message (`Ability is on cooldown! (X.Xs remaining)`).

---

## PDC Key Reference

### Ability Items

| Key | Type | Value |
|---|---|---|
| `xpotriad:ability` | STRING | `"ability"` (presence marker) |
| `xpotriad:item_type` | STRING | `"ability"` |
| `xpotriad:ability_id` | STRING | UUID string |
| `xpotriad:pre_cast_fragment` | STRING | fragment id, e.g. `"invisibility"` |
| `xpotriad:cast_fragment` | STRING | fragment id, e.g. `"speed"` |
| `xpotriad:post_cast_fragment` | STRING | fragment id, e.g. `"explosion"` |
| `xpotriad:cooldown_until` | LONG | Epoch millisecond timestamp until which item is on cooldown |

### Fragment Items

| Key | Type | Value |
|---|---|---|
| `xpotriad:item_type` | STRING | `"fragment"` |
| `xpotriad:fragment_id` | STRING | fragment id, e.g. `"speed"` |
