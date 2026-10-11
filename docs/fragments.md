# Fragments

Every fragment is a single gameplay unit slotted into a weapon at the EtchLoom.
**Rarity** sets how hard a fragment is to find. **Type** is separate: `MELEE`
fragments act around the player or a nearby target; `RANGED` fragments act on a
point or line the player looks at. Each name links to the effect that implements it.

| Rarity | Fragment | Type | What it does |
| ------ | -------- | ---- | ------------ |
| COMMON | [Blind](../src/main/java/dev/xpolion/xpotriad/effect/effects/BlindEffect.java) | MELEE | Blinds nearby enemies by applying Blindness in a short radius. |
| COMMON | [Delay](../src/main/java/dev/xpolion/xpotriad/effect/effects/DelayEffect.java) | MELEE | Holds the sequence for a beat before the next stage runs. |
| COMMON | [Heal](../src/main/java/dev/xpolion/xpotriad/effect/effects/HealEffect.java) | MELEE | Grants the caster Regeneration. |
| COMMON | [Knockback](../src/main/java/dev/xpolion/xpotriad/effect/effects/KnockbackEffect.java) | MELEE | Blasts the target backward. |
| COMMON | [Leap](../src/main/java/dev/xpolion/xpotriad/effect/effects/LeapEffect.java) | MELEE | Launches the caster forward and upward. |
| COMMON | [Shield](../src/main/java/dev/xpolion/xpotriad/effect/effects/ShieldEffect.java) | MELEE | Temporarily reduces incoming damage. |
| COMMON | [Slow Falling](../src/main/java/dev/xpolion/xpotriad/effect/effects/SlowFallingEffect.java) | RANGED | Slows the target's fall. |
| COMMON | [Smoke Bomb](../src/main/java/dev/xpolion/xpotriad/effect/effects/SmokeBombEffect.java) | MELEE | Creates a temporary cloud of smoke. |
| COMMON | [Speed](../src/main/java/dev/xpolion/xpotriad/effect/effects/SpeedEffect.java) | MELEE | Grants the caster increased movement speed. |
| COMMON | [Weakening](../src/main/java/dev/xpolion/xpotriad/effect/effects/WeaknessEffect.java) | MELEE | Weakens the target. |
| UNCOMMON | [Area Poison](../src/main/java/dev/xpolion/xpotriad/effect/effects/AreaPoisonEffect.java) | RANGED | Poisons enemies in a targeted area and leaves a lingering green cloud. |
| UNCOMMON | [Cleanse](../src/main/java/dev/xpolion/xpotriad/effect/effects/CleanseEffect.java) | MELEE | Removes negative effects from the target. |
| UNCOMMON | [Invisibility](../src/main/java/dev/xpolion/xpotriad/effect/effects/InvisibilityEffect.java) | MELEE | Grants the caster invisibility. |
| UNCOMMON | [Levitation](../src/main/java/dev/xpolion/xpotriad/effect/effects/LevitationEffect.java) | RANGED | Lifts the target into the air. |
| RARE | [Barrier](../src/main/java/dev/xpolion/xpotriad/effect/effects/BarrierEffect.java) | MELEE | Creates a temporary boundary that keeps others out. |
| RARE | [Blink](../src/main/java/dev/xpolion/xpotriad/effect/effects/BlinkEffect.java) | MELEE | Teleports the caster a short distance. |
| RARE | [Execution](../src/main/java/dev/xpolion/xpotriad/effect/effects/ExecutionEffect.java) | MELEE | Deals bonus damage to weakened targets. |
| RARE | [Explosion](../src/main/java/dev/xpolion/xpotriad/effect/effects/ExplosionEffect.java) | MELEE | Creates an explosion. |
| RARE | [Gravity Pull](../src/main/java/dev/xpolion/xpotriad/effect/effects/GravityPullEffect.java) | MELEE | Pulls nearby enemies toward a point. |
| RARE | [Line Snipe](../src/main/java/dev/xpolion/xpotriad/effect/effects/LineSnipeEffect.java) | RANGED | Fires a piercing shot at the first target in line. |
| EPIC | [Hound Call](../src/main/java/dev/xpolion/xpotriad/effect/effects/HoundCallEffect.java) | MELEE | Summons spectral hunting hounds. |
| EPIC | [Life Steal](../src/main/java/dev/xpolion/xpotriad/effect/effects/LifeStealEffect.java) | MELEE | Converts part of the caster's damage into health. |
| EPIC | [Mark](../src/main/java/dev/xpolion/xpotriad/effect/effects/MarkEffect.java) | MELEE | Marks a target so the next damage dealt to it is multiplied by 1.5x. |
| EPIC | [Reflect](../src/main/java/dev/xpolion/xpotriad/effect/effects/ReflectEffect.java) | MELEE | Reflects incoming attacks. |
| EPIC | [Shockwave](../src/main/java/dev/xpolion/xpotriad/effect/effects/ShockwaveEffect.java) | MELEE | Slams the caster to the ground and erupts; radius scales with fall distance. |
| EPIC | [Soul Link](../src/main/java/dev/xpolion/xpotriad/effect/effects/SoulLinkEffect.java) | MELEE | Links the caster to the target and shares damage. |
| EPIC | [Swap](../src/main/java/dev/xpolion/xpotriad/effect/effects/SwapEffect.java) | MELEE | Swaps the caster's position with the target. |
| LEGENDARY | [Cheat Death](../src/main/java/dev/xpolion/xpotriad/effect/effects/CheatDeathEffect.java) | MELEE | Makes the caster briefly immune to damage. |
| LEGENDARY | [Mass Freeze](../src/main/java/dev/xpolion/xpotriad/effect/effects/MassFreezeEffect.java) | RANGED | Freezes enemies in an area. |
| LEGENDARY | [Meteor](../src/main/java/dev/xpolion/xpotriad/effect/effects/MeteorEffect.java) | RANGED | Calls down a devastating meteor. |
| LEGENDARY | [Repeat](../src/main/java/dev/xpolion/xpotriad/effect/effects/RepeatEffect.java) | MELEE | Repeats the previously executed fragment. |
| LEGENDARY | [Rewind](../src/main/java/dev/xpolion/xpotriad/effect/effects/RewindEffect.java) | MELEE | Reverts the caster to a previous state. |
