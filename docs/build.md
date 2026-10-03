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
├── docs/                        ← this documentation
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
│       │       ├── effects/
│       │       ├── fragment/
│       │       └── targeting/
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
- 1× Diamond Sword engraved with a MELEE ability (Invisibility → Speed → Explosion)
- 1× Invisibility Fragment item
- 1× Speed Fragment item
- 1× Explosion Fragment item

**To test MELEE:** hit any entity while holding the sword.  
**To test RANGED:** engrave a RANGED ability via code/command, then right-click to launch a projectile.

---

## PDC Key Reference

### Ability Items

| Key | Type | Value |
|---|---|---|
| `xpotriad:ability` | STRING | `"ability"` (presence marker) |
| `xpotriad:weapon_type` | STRING | `"MELEE"` or `"RANGED"` |
| `xpotriad:pre_cast_fragment` | STRING | fragment id, e.g. `"invisibility"` |
| `xpotriad:cast_fragment` | STRING | fragment id |
| `xpotriad:post_cast_fragment` | STRING | fragment id |

### Fragment Items

| Key | Type | Value |
|---|---|---|
| `xpotriad:item_type` | STRING | `"fragment"` |
| `xpotriad:fragment_id` | STRING | fragment id, e.g. `"speed"` |

### Projectiles

| Key | Type | Value |
|---|---|---|
| `xpotriad:context_id` | STRING | UUID string of the active `AbilityContext` |
