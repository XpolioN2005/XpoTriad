Read `docs/ability-guide.md` before doing anything. Treat it as the source of truth for the existing XpoTriad API, targeting, runtime, particles, and fragment architecture.

Do not redesign any existing system. Do not invent API fields. Do not guess any numbers. Every gameplay number required for these fragments is explicitly defined below.

Implement the following fragments.

---

# Runtime Rules

Use the existing runtime system from `docs/ability-guide.md`.

## When a fragment MUST use RuntimeState

A fragment must create/use a runtime state when its effect continues after the initial `apply()` call and needs to do any of the following:

- Run for multiple ticks.
- Repeatedly perform an action.
- Wait for a future event.
- Listen for damage/attack/entity events during an active window.
- Maintain a temporary gameplay effect.
- Track a projectile or moving object.
- Record state over time.
- Follow an entity.
- Maintain a persistent gameplay/particle effect.
- Perform cleanup when a timed effect expires.
- Stop early because the target/entity becomes invalid.

Examples:

- Gravity Pull repeatedly pulls entities.
- Shield remains active and intercepts damage.
- Smoke Bomb maintains a temporary area.
- Thorns waits for incoming melee damage.
- Reflect waits for incoming attacks/projectiles.
- Soul Link remains active and transfers damage.
- Rewind records state over time.
- Mass Freeze maintains a temporary freeze.
- Meteor tracks a projectile until impact/expiry.
- Cheat Death remains active and intercepts damage.
- Delay waits before allowing the next stage.
- Repeat performs another fragment execution and must enforce an execution boundary.

## When RuntimeState is NOT needed

Do not create runtime state for a purely instantaneous action:

- Apply a normal potion effect.
- Deal immediate damage.
- Teleport immediately.
- Apply immediate knockback.
- Spawn a one-shot particle burst.
- Perform an immediate cleanse.
- Perform an immediate position swap.

## Runtime lifecycle

Every runtime effect follows this lifecycle:

```text
start
  ↓
active
  ↓
tick/update
  ↓
finished OR stopped early
  ↓
cleanup
```

For a timed runtime:

- Store its start time/tick.
- Check elapsed time every tick.
- Finish automatically at the specified duration.
- Remove event listeners/state when finished.
- Stop all associated particle handles.
- Stop immediately if its target/entity becomes invalid.
- Never leave a runtime state running indefinitely.

If a gameplay runtime owns a `ParticleHandle`, stopping the gameplay runtime must also stop that particle handle.

## Persistent particles

Persistent particles are also finite runtime state.

Use them when the visual must:

- Continue across multiple ticks.
- Follow an entity.
- Follow a moving point/projectile.
- Animate over time.
- Remain visible for a gameplay duration.

Do NOT use a persistent particle runtime for simple one-shot impacts.

---

# Fragment Constructor

Use the existing constructor format:

```java id="t9myda"
super(
    "id",
    "Display Name",
    List.of(Component.text("Description.", NamedTextColor.GRAY)),
    executionTime,
    Rarity.RARITY,
    Type.TYPE,
    cooldownModifier,
    new FragmentEffect()
);
```

For every fragment, use exactly the values specified below.

---

# COMMON

## 1. Weakening

```text
ID: weakening
Name: Weakening Fragment
Lore: Weakens the target.
Execution Time: 10 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 3 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Apply Weakness to the target.
- Weakness duration: 100 ticks.
- Weakness amplifier: 0.
- No runtime required after applying the status effect.

Visual:

- Use vanilla Weakness/status-effect particles around the target.
- One-shot visual only.

---

## 2. Knockback

```text
ID: knockback
Name: Knockback Fragment
Lore: Blasts the target backward.
Execution Time: 5 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 2 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Push the target directly away from the caster.
- Horizontal strength: `1.5`.
- Vertical velocity: `0.35`.
- Immediate effect.
- No runtime required.

Visual:

- Small impact burst centered on the target.
- Dust/cloud particles briefly spread in the knockback direction.

---

## 3. Leap

```text
ID: leap
Name: Leap Fragment
Lore: Launches you forward and upward.
Execution Time: 8 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 3 ticks
Target Type: SELF
```

Gameplay:

- Launch the caster forward and upward.
- Forward velocity: `1.0`.
- Vertical velocity: `0.8`.
- Immediate effect.
- No runtime required.

Visual:

- Compact dust/cloud burst around the caster's feet.
- Short upward particle trail lasting `6 ticks`.

The trail may use a small runtime because it lasts multiple ticks.

---

## 4. Shield

```text
ID: shield
Name: Shield Fragment
Lore: Temporarily reduces incoming damage.
Execution Time: 10 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 6 ticks
Target Type: SELF
```

Gameplay:

- Damage reduction: `40%`.
- Duration: `100 ticks`.
- Intercept qualifying incoming damage while active.
- Runtime required.
- End automatically after `100 ticks`.

Visual:

- Circular particle ring around the caster at waist height.
- Ring radius: `0.7 blocks`.
- Ring rotates continuously.
- Active for exactly `100 ticks`.
- Stop immediately when Shield ends early.

---

## 5. Smoke Bomb

```text
ID: smoke_bomb
Name: Smoke Bomb Fragment
Lore: Creates a temporary cloud of smoke.
Execution Time: 10 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 6 ticks
Target Type: SELF
```

Gameplay:

- Create a smoke area centered on the caster.
- Radius: `2.5 blocks`.
- Duration: `100 ticks`.
- The smoke provides visual cover only.
- Runtime required.

Visual:

- Dense smoke around the caster.
- Must cover the full player body, not only the feet.
- Slowly expand from radius `1.0` to `2.5` blocks over the first `20 ticks`.
- Remain at full size until expiry.
- Stop after exactly `100 ticks`.

---

## 6. Slow Falling

```text
ID: slow_falling
Name: Slow Falling Fragment
Lore: Slows the target's fall.
Execution Time: 10 ticks
Rarity: COMMON
Type: RANGED
Cooldown Modifier: 3 ticks
Target Type: SELF
```

Gameplay:

- Apply Slow Falling.
- Duration: `100 ticks`.
- No custom runtime required.

Visual:

- Vanilla Slow Falling particles.

---

# UNCOMMON

## 7. Cleanse

```text
ID: cleanse
Name: Cleanse Fragment
Lore: Removes negative effects from the target.
Execution Time: 8 ticks
Rarity: UNCOMMON
Type: MELEE
Cooldown Modifier: 5 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Remove all negative potion/status effects from the target.
- Immediate effect.
- No runtime.

Visual:

- Villager happy particles.
- Burst centered on the target.
- Radius: approximately `1.5 blocks`.
- One-shot effect.

---

## 8. Levitation

```text
ID: levitation
Name: Levitation Fragment
Lore: Lifts the target into the air.
Execution Time: 10 ticks
Rarity: UNCOMMON
Type: RANGED
Cooldown Modifier: 6 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Apply Levitation.
- Duration: `60 ticks`.
- Amplifier: `0`.
- No custom runtime required.

Visual:

- Vanilla-style rising particles around the target.

---

## 9. Thorns

```text
ID: thorns
Name: Thorns Fragment
Lore: Returns damage to attackers.
Execution Time: 10 ticks
Rarity: UNCOMMON
Type: MELEE
Cooldown Modifier: 7 ticks
Target Type: SELF
```

Gameplay:

- Active duration: `100 ticks`.
- Reflect `30%` of qualifying melee damage back to the attacker.
- Runtime required because it waits for incoming attacks.
- Do not recursively trigger Thorns from its own reflected damage.

Visual:

- On retaliation, sharp particle burst around attacker.
- Smaller defensive burst around caster.
- No continuous particle effect required.

---

# RARE

## 11. Gravity Pull

```text
ID: gravity_pull
Name: Gravity Pull Fragment
Lore: Pulls nearby enemies toward a point.
Execution Time: 15 ticks
Rarity: RARE
Type: MELEE
Cooldown Modifier: 12 ticks
Target Type: POINT
```

Gameplay:

- Create a pull field at the selected point.
- Radius: `4 blocks`.
- Duration: `60 ticks`.
- Every `5 ticks`, pull valid entities toward the center.
- Pull strength per tick: `0.35`.
- Runtime required.
- Stop immediately when the `60 ticks` expire.

Visual:

- Circular particle ring on the ground.
- Radius: `4 blocks`.
- Particles spiral inward toward the center.
- Ring continuously animates during the runtime.
- Stop immediately when the pull ends.

---

## 12. Blink

```text
ID: blink
Name: Blink Fragment
Lore: Teleports you a short distance.
Execution Time: 5 ticks
Rarity: RARE
Type: MELEE
Cooldown Modifier: 10 ticks
Target Type: POINT
```

Gameplay:

- Teleport caster to the selected valid location.
- Maximum teleport distance: `8 blocks`.
- Validate destination before teleporting.
- Immediate effect.
- No gameplay runtime.

Visual:

- Burst at origin.
- Burst at destination.
- Destination particles rise for `5 ticks`.

---

## 13. Line Snipe

```text
ID: line_snipe
Name: Line Snipe Fragment
Lore: Fires a piercing shot at the first target in line.
Execution Time: 8 ticks
Rarity: RARE
Type: RANGED
Cooldown Modifier: 10 ticks
Target Type: LINE
```

Gameplay:

- Fire a fast projectile along the caster's aimed direction.
- Maximum travel distance: `30 blocks`.
- Projectile lifetime: `20 ticks`.
- Hit the first valid target.
- Damage: `8`.
- Runtime required to track the projectile.

Visual:

- Very thin fast-moving projectile trail.
- Trail follows the projectile every tick.
- Small sharp impact burst on hit.
- Stop trail on impact or after `20 ticks`.

---

## 14. Barrier

```text
ID: barrier
Name: Barrier Fragment
Lore: Creates a temporary boundary.
Execution Time: 12 ticks
Rarity: RARE
Type: MELEE
Cooldown Modifier: 12 ticks
Target Type: POINT
```

Gameplay:

- Create circular boundary around selected point.
- Radius: `3 blocks`.
- Duration: `100 ticks`.
- Prevent valid entities from crossing the boundary.
- Runtime required.
- Stop automatically after `100 ticks`.

Visual:

- Circular particle boundary.
- Radius: `3 blocks`.
- Evenly spaced particles around circumference.
- Boundary remains visible for `100 ticks`.
- Remove immediately when runtime ends.

---

## 15. Execution

```text
ID: execution
Name: Execution Fragment
Lore: Deals bonus damage to weakened targets.
Execution Time: 5 ticks
Rarity: RARE
Type: MELEE
Cooldown Modifier: 8 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Check target health immediately.
- Threshold: `25%` of maximum health.
- If target is at or below `25%`, deal `10` bonus damage.
- Otherwise deal no additional damage.
- This is an instant-kill effect.
- No runtime.

Visual:

- Dark/red concentrated burst on target.
- Short outward particle spray.
- One-shot only.

---

# EPIC

## 17. Hound Call

```text
ID: hound_call
Name: Hound Call Fragment
Lore: Summons spectral hunting hounds.
Execution Time: 15 ticks
Rarity: EPIC
Type: MELEE
Cooldown Modifier: 15 ticks
Target Type: SELF
```

Gameplay:

- Summon exactly `5` tamed wolves.
- Wolves are owned by the caster.
- Wolf lifetime: `200 ticks`.
- Wolves assist/follow the caster normally.
- Runtime required to track lifetime and cleanup.
- Remove the summoned wolves when their `200 ticks` expire.

Visual:

- Small smoke/dust burst at each summon position.
- No permanent particle effect attached to each wolf.

---

## 18. Reflect

```text
ID: reflect
Name: Reflect Fragment
Lore: Reflects incoming attacks.
Execution Time: 10 ticks
Rarity: EPIC
Type: MELEE
Cooldown Modifier: 14 ticks
Target Type: SELF
```

Gameplay:

- Active duration: `80 ticks`.
- Reflect eligible incoming projectiles/attacks.
- Runtime required because it listens for incoming attacks.
- Reflected damage/projectiles must not recursively trigger Reflect.
- End automatically after `80 ticks`.

Visual:

- Brief defensive flash when a reflection occurs.
- Short particle trail following reflected projectile direction.
- No continuous aura required.

---

## 19. Life Steal

```text
ID: life_steal
Name: Life Steal Fragment
Lore: Converts part of your damage into health.
Execution Time: 8 ticks
Rarity: EPIC
Type: MELEE
Cooldown Modifier: 10 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Active duration: `100 ticks`.
- Heal caster for `25%` of qualifying damage dealt.
- Cannot exceed maximum health.
- Runtime required to track the temporary effect.
- End automatically after `100 ticks`.

Visual:

- Red particles travel from target toward caster when healing occurs.
- Finish with a small red burst around caster.
- No persistent visual required.

---

## 20. Swap

```text
ID: swap
Name: Swap Fragment
Lore: Swaps your position with the target.
Execution Time: 8 ticks
Rarity: EPIC
Type: MELEE
Cooldown Modifier: 14 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Immediately swap caster and target positions.
- Validate both locations first.
- No runtime.

Visual:

- Simultaneous circular bursts at both positions.
- Particles spiral upward for `5 ticks`.

---

## 21. Soul Link

```text
ID: soul_link
Name: Soul Link Fragment
Lore: Links you to the target and shares damage.
Execution Time: 12 ticks
Rarity: EPIC
Type: MELEE
Cooldown Modifier: 15 ticks
Target Type: SINGLE_ENTITY
```

Gameplay:

- Link caster and target.
- Duration: `100 ticks`.
- Transfer `30%` of qualifying damage from one linked entity to the other.
- Runtime required.
- End immediately if either entity becomes invalid.
- End automatically after `100 ticks`.

Visual:

- Persistent particle tether between entities.
- Tether follows both entities.
- When damage transfer happens, particles travel along the tether.
- Stop immediately when link ends.

---

# LEGENDARY

## 23. Rewind

```text
ID: rewind
Name: Rewind Fragment
Lore: Reverts you to a previous state.
Execution Time: 15 ticks
Rarity: LEGENDARY
Type: MELEE
Cooldown Modifier: 20 ticks
Target Type: SELF
```

Gameplay:

- Record caster position, health while activate.
- Recording duration: `10 ticks`.
- trigger after `100` ticks
- When rewind triggers, restore the caster to the state from earlier.
- Runtime required for recording.

Visual:

- Blue particle rings rotate around the caster while recording.
- Exactly `2` rings.
- Rings rotate in opposite directions.
- On rewind, create a burst at the current position.
- Restore the player.
- Create a second burst at the restored position.

---

## 24. Mass Freeze

```text
ID: mass_freeze
Name: Mass Freeze Fragment
Lore: Freezes enemies in an area.
Execution Time: 15 ticks
Rarity: LEGENDARY
Type: RANGED
Cooldown Modifier: 18 ticks
Target Type: AREA
```

Gameplay:

- Area radius: `5 blocks`.
- Freeze valid entities for `60 ticks`.
- Prevent movement while frozen.
- Runtime required.
- Automatically release entities after `60 ticks`.
- End early for invalid entities.

Visual:

- Frost particles around each frozen entity.
- thick freeze parcile around enity legs like the legs are stuck
- Frost ring around the affected area.
- Ring radius: `5 blocks`.
- Remove all freeze visuals when the effect ends.

---

## 25. Meteor

```text
ID: meteor
Name: Meteor Fragment
Lore: Calls down a devastating meteor.
Execution Time: 20 ticks
Rarity: LEGENDARY
Type: RANGED
Cooldown Modifier: 25 ticks
Target Type: POINT
```

Gameplay:

- Launch a large fireball-like projectile toward the selected point.
- Projectile lifetime: `40 ticks`.
- Impact damage: `20`.
- AOE radius: `4 blocks`.
- No terrain destruction.
- Runtime required to track the projectile.
- End runtime on impact or after `40 ticks`.

Visual:

- giant fireball visual
- Strong fiery/orange projectile trail.
- Smoke trailing behind the projectile.
- On impact, large expanding fire/smoke/debris particle burst.
- Stop projectile particles on impact or expiry.

---

## 26. Cheat Death

```text
ID: cheat_death
Name: Cheat Death Fragment
Lore: Become immune to damage for a brief moment.
Execution Time: 8 ticks
Rarity: LEGENDARY
Type: MELEE
Cooldown Modifier: 20 ticks
Target Type: SELF
```

Gameplay:

- Complete incoming damage immunity.
- Duration: `40 ticks`.
- Runtime required because damage must be intercepted during the active window.
- End automatically after `40 ticks`.

Visual:

- Protective aura around caster.
- rotating particle sphere around the body golden.
- Ring radius: `0.8 blocks`.
- Active for exactly `40 ticks`.
- Stop immediately when immunity ends.

---

# UTILITY

## 29. Delay

```text
ID: delay
Name: Delay Fragment
Lore: Delays the next stage.
Execution Time: 20 ticks
Rarity: COMMON
Type: MELEE
Cooldown Modifier: 0 ticks
Target Type: NONE
```

Gameplay:

- Pause ability progression for exactly `20 ticks`.
- Runtime required.
- Runtime simply waits for `20 ticks`, then finishes.
- It does not perform another gameplay effect.

Visual:

- No major visual effect.

---

## 30. Repeat

```text
ID: repeat
Name: Repeat Fragment
Lore: Repeats the previous fragment.
Execution Time: 5 ticks
Rarity: LEGENDARY
Type: MELEE
Cooldown Modifier: 15 ticks
Target Type: NONE
```

Gameplay:

- Execute the previous applicable fragment one additional time.
- Maximum Repeat chain depth: `1`.
- A Repeat may not execute another Repeat.
- Do not allow recursive or infinite execution.
- Preserve the previous fragment's targeting context.
- Runtime is only required if the repeated fragment itself requires runtime.
- Repeat itself does not create an independent persistent runtime.

Visual:

- No independent visual.
- The repeated fragment produces its normal visual.

---

# Implementation Rules

- Do not implement the open slots.
- Do not modify:
  - Invisibility
  - Speed
  - Heal
  - Explosion
  - Mark

- Do not invent Fragment API fields.
- Read `docs/ability-guide.md` for the actual target resolver and runtime APIs.
- Use the exact numeric values specified above. Do not rebalance or substitute values.
- Do not guess missing durations, ranges, damage values, particle durations, or cooldown modifiers.
- `MELEE` / `RANGED` describes the fragment's intended combat behavior, not its target.
- Target type describes what the fragment needs to operate on.
- Use RuntimeState only when the effect actually needs to remain active, tick, wait, listen, track, record, or clean itself up.
- A persistent particle effect is a finite runtime effect, never an indefinite task.
- Every runtime must have a clear completion condition.
- Every runtime must have a clear early-stop condition where applicable.
- Every runtime must clean up listeners, tracked entities, state, and particle handles.
- If an entity becomes invalid, terminate any runtime associated with that entity.
- Do not create custom persistent particles for simple one-shot effects.
- Keep one-shot visuals small and immediate.
- Persistent visuals should communicate the gameplay mechanic rather than simply decorating it.
- Keep the implementation within the existing architecture documented in `docs/ability-guide.md`.

# MAIN.JAVA

update the /xptext command to only give a cheast full of all the ability fragmnets

# EXISTING FRAGMENTS

check the items in @src\main\java\dev\xpolion\xpotriad\fragment\fragments
