# First balance pass — 2026-11-11

Historical snapshot: this report predates queued actions, stamina-costed repeatable Punch, and per-copy escalating reuse costs. Its match statistics do not measure the current redesigned rules.

The default names are **Hráč 1** and **Hráč 2**. The game still starts with **15 gold**, awards **6 for winning / 9 for losing**, and uses **40 health / 12 base stamina**.

## How the games were played

`src/Playtest.java` is a playable line-command front end to the existing `Game` engine. I controlled both players through terminal input, selected items, inspected state, and played a battle to completion before and after the changes. Repeated turn patterns were submitted in batches. This was not hundreds of manually or visually played matches.

The exact commands are in [manual-before.txt](manual-before.txt) and [manual-after.txt](manual-after.txt), both using seed 17. The pre-change poison/healing player won with 8 health on displayed turn 24. The post-change physical player won with 10 health on displayed turn 10. Shops changed with the selection weights, so these two battles are **not a controlled comparison of the same loadouts**.

```bash
javac --release 17 -encoding UTF-8 -d build/check src/*.java tests/*.java
java -cp build/check Playtest 17
# Repeat the post-change battle:
java -cp build/check Playtest 17 < playtests/manual-after.txt
```

`BalanceSimulation` supplies the controlled checks: targeted loadouts with starting positions reversed, and seeded complete matches using actual shop transactions. Its three policies are simple greedy heuristics, not optimal players. They do not sell equipment or search all combinations. The fixed fixtures assign gear directly to isolate an interaction; their costs are similar, not universally identical.

## Changes and reasons

| Problem reproduced | Change |
|---|---|
| Two leather shields per side blocked all attacks indefinitely | Lower shield powers; stacking, stamina costs, and lifetime remain intact |
| Armor plus unconditional regeneration exactly replaced all incoming basic-attack damage | Regeneration ring heals 1 instead of 2 and costs 6 instead of 8; amber brooch now offers stronger but below-half healing; phoenix healing reduced from 5 to 4 |
| Cleansing erased arbitrary amounts of fire or poison before any tick | Remove at most 3 remaining damage per own turn, sharing one budget across all matching effects |
| Threshold armor erased every small poison tick | Leave at least half of an eligible hit, rounded to nearest integer; a shield can still absorb that remainder |
| Cheap fire lost both starting orders against equal-cost daggers | Increase fire totals, keeping the conserved-total merge formula and durations |
| One stamina potion was strictly worse than another at the same price | Doušek síly now grants 3 next-turn stamina for its existing 3 stamina cost |
| Weaponless shops and a large opening-shopper advantage | Triple selection weights for weapons costing at most 7; all ten offers remain random, with no reservations or guarantees |

All item descriptions reflect their changed effects. **No exhaustion, automatic damage, turn cap, guaranteed weapon offer, or economy change was added.**

The numerical item changes and detailed rationale are recorded in the English `.leave/progress.txt`, section 13.

## Fresh-seed comparison

Each policy used 1,000 matches with seeds `(10000 + i) * 7919`. The same harness ran against a locally retained pre-change compiled build and the updated build. The simulation stops a stalled battle at 160 player turns; the actual game has no such cap.

| Policy | Stalled before → after | Initial shopper's full-match win rate before → after | Mean completed battle length before → after |
|---|---:|---:|---:|
| Balanced | 29 → 0 | 63.6% → 47.4% | 8.65 → 6.49 turns |
| Defensive | 58 → 0 | 61.1% → 47.3% | 11.86 → 6.80 turns |
| Aggressive | 19 → 0 | 65.9% → 48.2% | 7.61 → 6.24 turns |

Lengths include both preparation turns and the final player turn. Win percentages exclude incomplete matches. The two targeted defensive mirrors previously stalled in both starting orders; now they finish in 81 and 79 player turns respectively. Those remain slow extreme cases.

The updated initial shopper's **opening-battle** win rates are only 38.9%, 40.3%, and 37.9%. The opposite player starts combat, so this still indicates an initiative advantage. Close full-match rates include alternating starts and the catch-up economy; they do not prove a balanced opening battle.

The 300-match development set still generated one weaponless shop after the change. No such shop appeared in the fresh-seed sample, but weapon availability is deliberately not guaranteed. Raw reports are retained in [results](results/).

```bash
java -cp build/check BalanceSimulation 1000 10000 balanced
java -cp build/check BalanceSimulation 1000 10000 defensive
java -cp build/check BalanceSimulation 1000 10000 aggressive
java -cp build/check BalanceRegressionTest
```

## Limits

This is evidence that the specific problems improved, not proof of competitive balance. Ordinary bot battles are faster after weakening defenses; some defensive extremes are still slow. Players can still deliberately pass forever, and unexplored loadouts may still stall. Inventory expansion, expensive equipment, and stamina consumables need more human playtesting than these heuristics provide.

The compiled pre-change snapshot is local and ignored under `build/balance-before`; the reports and command scripts are retained here. Replaying `manual-before.txt` against the current build uses changed shop draws and is not a reconstruction of the old battle.
