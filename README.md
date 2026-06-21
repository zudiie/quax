# Quax

A two-player strategy board game built in Java with LibGDX. One side is always you, the other is a computer-controlled bot that uses a Dijkstra-based pathfinding strategy to evaluate the board in real time.

---

## How the game works

The board is an 11×11 grid of octagonal cells, with smaller diamond-shaped rhombus tiles sitting between them that act as diagonal connectors.

- **BLACK** wins by forming a connected chain of stones from **row 11 (top) to row 1 (bottom)**
- **WHITE** wins by forming a connected chain of stones from **column A (left) to column K (right)**

Two stones are connected if they share a flat edge (orthogonal), or if they are diagonally adjacent and a rhombus tile of the same colour sits between them.

At the start of each game, the bot is randomly assigned either BLACK or WHITE. BLACK always moves first.

---

## Bot strategy

The bot uses a three-step decision process on every move:

1. **Win immediately** - if any move completes a winning chain, the bot plays it
2. **Block the human** - if you are one move from winning, the bot blocks that cell
3. **Strategic positioning** - otherwise, the bot runs Dijkstra's algorithm from all four edges (its own two and the opponent's two) and scores every empty cell based on how central it is to both players' shortest paths. The cell with the lowest combined cost - the most contested position on the board - gets played

This means the bot is not just building its own path, it is actively disrupting yours from the very first move. The scores update after every move, so the strategy adapts in real time.

The bot also considers rhombus tiles and will place one when it genuinely improves its position score.

---

## Strategy heat map

Click **Show Strategy** at any point during the game to see how the bot is currently rating every cell:

- 🟢 **Green** = high priority
- 🟡 **Yellow** = medium priority
- 🔴 **Red** = low priority

Each cell shows a percentage score from 0–100%. The overlay updates after every move and can be toggled on or off without affecting gameplay.

---

## The Pie Rule

After BLACK places the very first stone, WHITE gets one opportunity to activate the pie rule - swapping colours before making their first move.

- If **you** are WHITE on move 2, an **Activate Pie Rule** button appears. Click it to swap, or just make a normal move and the option disappears
- If the **bot** is WHITE on move 2, it has a 50/50 chance of activating it

---

## How to run

**Requirements:** Java 25 or compatible JDK

```bash
java -jar Quax.jar
```

Download the latest `Quax.jar` from the [Releases](../../releases) page and run it from your terminal. The game window opens automatically.

---

## Build from source

```bash
# Clone the repo
git clone https://github.com/zudiie/quax.git
cd quax

# Run the game directly
./gradlew lwjgl3:run

# Build a runnable JAR
./gradlew lwjgl3:jar
# Output: lwjgl3/build/libs/Quax.jar
```

---

## Project structure

```
quax/
├── assets/               ← game assets (fonts, textures)
├── core/
│   └── src/main/java/src/softies/
│       ├── board/        ← QuaxBoard, board initialisation and cell maps
│       ├── bot/          ← BotPlayer, Dijkstra strategy
│       ├── renderer/     ← WorldCalculator, screen rendering
│       ├── screens/      ← WelcomeScreen, GameScreen, WinScreen
│       ├── Cell.java     ← base class for all board cells
│       ├── OctagonalCell.java
│       ├── RhombicCell.java
│       ├── WinCheck.java ← DFS-based win detection
│       ├── PlayerColour.java
│       ├── CellType.java
│       ├── Point.java
│       ├── FontLoader.java
│       └── GameMode.java
├── lwjgl3/               ← desktop launcher (LWJGL3)
├── gradle/               ← Gradle wrapper
├── build.gradle
└── settings.gradle
```

---

## Tests

Four test files cover the core game logic:

| File | Coverage |
|---|---|
| `QuaxTestSuite` | Cell display symbols, stone placement, tile proximity |
| `QuaxModelTest` | Board initialisation (121 octagons, 100 rhombuses), label validation, overlap prevention |
| `QuaxSprintThreeTest` | Pie rule behaviour, win detection, end-of-game handling, rhombus placement |
| `QuaxSprintFourTest` | Human vs Bot mode, bot decision making, strategy heat map data |

Run all tests:

```bash
./gradlew test
```

---

## Built with

- [Java](https://www.java.com)
- [LibGDX](https://libgdx.com)
- [LWJGL3](https://www.lwjgl.org)
- [Gradle](https://gradle.org)