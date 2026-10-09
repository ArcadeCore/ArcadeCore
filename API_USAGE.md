# ArcadeCore API Usage Guide

How to build a minigame addon on top of ArcadeCore, from project setup to the
final `end()` call, covering only the `ArcadeAPI` module (version 1.1.0): the
interfaces, events and message helper your plugin compiles against.
ArcadeBedrockPillars (`../ArcadeBedrockPillars`) and ArcadeFFA (`../ArcadeFFA`)
are working examples of everything below.

ArcadeCore runs on Minecraft 1.8 to 26.3 with one jar, and addons are expected
to do the same. Section 2.3 lists what that means for your code.

## 1. Who owns what

ArcadeCore owns the plumbing; your addon owns the rules.

| ArcadeCore (framework) | Your addon (game logic) |
|---|---|
| Queue join/leave, countdowns, match creation | Win conditions |
| Map pool acquire/release, spawn teleport | Arena building (optional, procedural) |
| Participant tracking, elimination bookkeeping | Death → elimination wiring |
| Match start/end countdowns, teardown, lobby return | Calling `IMatch.end()` at the right moment |
| Win/loss stats persistence | Declaring winners via `setWinnerParticipants` |
| `Game` registration, `ArcadeAPI` provider | Registering/unregistering your `Game` |

The core deliberately has no last-player-standing check, no timer expiry,
and no death listener. If your game never calls `IMatch.end()`, the
match runs forever.

## 2. Project setup

### 2.1 Gradle dependency

Compile against the 1.8.8 Spigot API and the ArcadeAPI. ArcadeCore provides
the ArcadeAPI classes at runtime (via `depend`, see below), so keep it
`compileOnly` to stay thin. Compile for Java 8 so the jar loads on 1.8 servers:

```kotlin
repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.drappula:ArcadeAPI:1.1.0")
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}
```

The existing addons get `org.drappula:ArcadeAPI` from a sibling checkout:
`settings.gradle.kts` has `includeBuild("../ArcadeCore")` and Gradle
substitutes the module. ArcadeCore has no publishing setup, so a standalone addon
has to get ArcadeAPI 1.1.0 into its build some other way.
If your tests touch ArcadeAPI classes, also declare it as `testImplementation`,
because `compileOnly` is not on the test classpath.

### 2.2 `plugin.yml`

Use a plain `plugin.yml` (no `paper-plugin.yml`, no `api-version`). `depend`
makes ArcadeCore load first and puts its classes, including `ArcadeAPI`, on
your plugin's classpath. Commands are declared here too (section 3):

```yaml
name: MyMinigame
version: '1.0.0'
main: com.example.myminigame.MyMinigame
load: POSTWORLD
depend: [ ArcadeCore ]
commands:
  mygame:
    description: My minigame
    usage: /mygame [join|leave]
```

### 2.3 Staying 1.8-compatible

The server may be anything from 1.8 to 26.3, so the same rules the core follows
apply to addons:

- No Paper-only API (`Player#sendRichMessage`, `Event#callEvent`,
  `getSLF4JLogger`, Adventure `Component`, MiniMessage, Brigadier command
  registration, `paper-plugin.yml`). They do not exist on Spigot or on 1.8.
  Use `Bukkit.getPluginManager().callEvent(...)`, `getLogger()` and
  `Messages` instead.
- Java 8 only: no `var`, no `List.of`/`Map.of`, no private interface methods,
  no switch expressions, no records or text blocks. `--release 8` rejects
  most of these at compile time.
- Check Bukkit calls against the 1.8.8 API, not just whatever your IDE
  completes. Newer-only methods compile fine against a newer API and then
  throw `NoSuchMethodError` on old servers. Example: use
  `player.getMaxHealth()` / `setMaxHealth()`, not the `GENERIC_MAX_HEALTH`
  attribute.
- Materials and items: names changed in 1.13 (`WOOL`/`WHITE_WOOL`,
  `STATIONARY_LAVA`/`LAVA`). Either resolve through XSeries
  (`XMaterial.matchXMaterial(name)`, `XMaterial.LAVA.parseMaterial()`) or read
  names from your config and skip what the running version does not know.
  ArcadeBedrockPillars does the latter: `config.yml` keeps modern names and
  unknown entries are skipped.
- XSeries is not on your classpath. ArcadeCore shades and relocates its own
  copy to `org.drappula.arcadeCore.libs.xseries`, which is not part of the API.
  If you use XSeries, shade and relocate your own copy into your jar
  (ArcadeBedrockPillars relocates to `org.drappula.arcadeBedrockPillars.libs.xseries`).
- Chunk generators: use the legacy `generateChunkData` API, which every
  version still accepts. `WorldCreator.name(...)` for world creation.
- Things that degrade on old versions: there is no boss bar on 1.8 (and
  `bossbar:` messages show as an action bar on every version), no shield or
  scaffolding items on 1.8 (skipped), and world height differs by version, so
  a map option range such as BedrockPillars' `lava-start-y` (declared -64 to
  320, which assumes 1.18+ height) is only meaningful up to what the server
  supports.
- Tests (MockBukkit v1.21, JDK 21) run on a modern API. A passing test does not
  prove a call exists on 1.8. Boot your jar on an old server before claiming
  compatibility; `../TestServer-matrix/matrix.sh` does that for 1.8.8, 1.12.2,
  1.16.5, 1.20.4, 1.21.4 and 26.3.

## 3. Plugin entrypoint: register your game

```java
public class MyMinigame extends JavaPlugin {
    private final MyGame game = new MyGame();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ArcadeAPIProvider.get().getGameManager().registerGame(game);
        getServer().getPluginManager().registerEvents(new MyListener(game), this);

        MyCommand command = new MyCommand(game);
        getCommand("mygame").setExecutor(command);       // declared in plugin.yml
        getCommand("mygame").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        ArcadeAPIProvider.get().getGameManager().unregisterGame(game);
    }
}
```

- `registerGame` calls your `Game.onRegister()`; `unregisterGame` calls
  `onUnregister()` and drops the game's queue, firing `QueueLeaveEvent`
  with `GAME_UNREGISTERED` for each queued player. Game ids are lowercased
  internally and must not be blank or contain spaces (`IllegalArgumentException`
  otherwise).
- `ArcadeAPIProvider.get()` is the single entry point. It throws
  `IllegalStateException` when the API isn't loaded. The `depend: [ ArcadeCore ]`
  entry above is what prevents that.
- Commands are plain Bukkit: a `CommandExecutor` (and optionally a
  `TabCompleter`) wired with `getCommand(name).setExecutor(...)` for a command
  declared under `commands:` in your `plugin.yml`. Core itself and the existing
  addons (`/bp`, `/ffa`, `/hub`) do it this way. There is no Brigadier. Check
  permissions and arguments by hand and reply with `Messages.chat` (section 8.4).
- In tests, back it with a mock: `ArcadeAPIProvider.register(mock(ArcadeAPI.class))`
  in setup, `ArcadeAPIProvider.unregister()` in teardown.

## 4. The `Game` class

Extend `AbstractGame` (stores id/displayName/counts) or implement `Game`
directly. Only three members are abstract. Everything else has a default:

```java
public class MyGame extends AbstractGame {
    public MyGame() {
        // (id, displayName, minPlayers, maxPlayers)
        super("my-game", "My Game", 2, 8);
    }

    @Override
    public int getPlayersRequired() {
        return getMinPlayers(); // or read from your config
    }

    @Override
    public IArcadeMap createArena(World world, List<Player> players) {
        return ArenaBuilder.build(world, players); // or null → use the map pool (§5)
    }
}
```

What you can override:

| Member | Default | Meaning |
|---|---|---|
| `isEnabled()` | `true` | Disabled games reject queue joins (`JoinResult.GAME_DISABLED`) and can't start matches |
| `getMinPlayers()` / `getMaxPlayers()` | `playersRequired` | Queue thresholds; countdown starts at min, match starts immediately at max |
| `getGameEndSettings()` | `GameEndSettings.DEFAULT` | Gamemode/fly applied to players when the match ends |
| `isTeamBased()` / `getTeamSettings()` | solo (`TeamSettings.DISABLED`) | Return `TeamSettings.of(teamSize, maxTeams)` for team games; the core partitions players at match start |
| `getQueueCountdownSeconds()` | empty → `queue.start-countdown` (20 s) | Per-game queue countdown override |
| `getStartCountdownSeconds()` | empty → `match.start-countdown` (10 s) | Per-game match-start countdown override |
| `getMapConfigOptions()` | empty | Per-map tuning knobs (e.g. `MapConfigOption.integer("lava-blocks-rise-per-minute", 2, 0, 60)`); admins set values via `/arcade map config <map> set <key> <value>`, read them via `map.getIntConfig(...)` / `getDecimalConfig` / `getBooleanConfig` / `getTextConfig` |
| `getMatchStartSettings()` | cages on, freeze on | Core spawn cages + countdown movement freeze; return `MatchStartSettings.create(false, ...)` to opt out of either |
| `createArena(world, players)` | `null` → static map pool | Build into the given arena world to bypass the pool |
| `onRegister` / `onUnregister` | no-op | Game lifecycle hooks |
| `onMatchStart` / `onMatchEnd` | no-op | Match lifecycle hooks: start/stop your tasks here, no listener required |
| `onParticipantEliminate(participant, killer)` | no-op | Per-elimination hook |

## 5. Arenas: two ways to get a map

### 5.1 Procedural (`createArena`), for generated arenas

The core provisions a fresh, empty arena world per match and hands it to
`createArena`. Return a ready-to-use `IArcadeMap` built inside it for exactly
these players. The core checks only that `getSpawnPoints().size() >=
players.size()` (player `i` is teleported to spawn `i`), then teleports
everyone itself. You never teleport, and you never touch play worlds or
their borders: the arena world is unloaded and deleted when the match ends.

```java
@Override
public IArcadeMap createArena(World world, List<Player> players) {
    return ArenaBuilder.build(world, players);
}
```

Implement `IArcadeMap` with your arena handle:

```java
public class MyMap implements IArcadeMap {
    public String getId() { ... }          // unique per arena instance
    public String getGameId() { return "my-game"; }
    public String getDisplayName() { ... }
    public String getWorldName() { ... }
    public boolean isEnabled() { return true; }
    public boolean isInUse() { return true; }   // procedural maps own their lifecycle
    public List<Location> getSpawnPoints() { ... }
    public World getWorld() { ... }        // null when the world isn't loaded
}
```

Gotchas:

- Returning `null` from `createArena` falls back to the static pool (§5.2).
- If the map has too few spawns, the core releases it, logs a warning, and
  `startMatch` returns `null` (queued players are recycled, §6).
- Your map is passed to `MatchStartEvent`/`MatchEndEvent`; tear it down in
  `onMatchEnd` or your `MatchEndEvent` handler (restore borders, clear
  blocks). The core only releases *pooled* maps itself.

### 5.2 Static pool (no code)

Return `null` (the default) and manage maps in-game via `/arcade map ...`
(`create`, `addspawn`, `enable`, `disable`, `list`, `delete`). The core
acquires an enabled, unused map with enough spawns, or fails the start with
`map-unavailable`. Use this for hand-built arenas; use §5.1 for generated ones.

## 6. Queue: getting players into a match

Players normally join through your own command (e.g. `/mygame join`), which
delegates to the core queue:

```java
JoinResult result = ArcadeAPIProvider.get()
        .getQueueManager()
        .joinQueue(player, game);
if (result != JoinResult.SUCCESS) {
    Messages.chat(player, "<red>Failed to join the queue: <reason>", "reason", result.name());
}
```

`JoinResult`: `SUCCESS`, `ALREADY_QUEUED`, `ALREADY_IN_MATCH`,
`EVENT_DENIED` (a `QueueEnterEvent` listener cancelled), `GAME_DISABLED`.
For parties, use `joinQueue(Collection<Player>, Game)`: one all-or-nothing
join that keeps the group contiguous for team partitioning.

What happens next is automatic:

1. Cancellable `QueueEnterEvent` fires (use it for entry requirements such as
   permissions or level gates).
2. At `minPlayers` a countdown starts (`queue.start-countdown`, default
   20 s, or your `getQueueCountdownSeconds()` override).
3. At `maxPlayers` the match starts immediately.
4. The core slices players and calls `startMatch`. If it returns `null`
   (no free map, too few spawns, `MatchStartEvent` denied), the players go
   back to the **front** of the queue, the countdown restarts, and they get
   `map-unavailable`.

Leaving: `getQueueManager().leaveQueue(player)` returns whether they were
queued. Fires `QueueLeaveEvent` with a `QueueLeaveReason`
(`LEAVE`, `QUIT`, `MATCH_START`, `KICK`, `GAME_UNREGISTERED`).
`leaveQueue` during a running countdown below minimum cancels it.

To start a match programmatically (admin command, tests), bypass the queue:

```java
IMatch match = ArcadeAPIProvider.get().getMatchManager().startMatch(game, players);
// null → same failure modes as above
```

## 7. Match lifecycle

States: `LOADING → STARTING → STARTED → ENDING → ENDED`.
`setState` fires `MatchStateChangeEvent(old, new)` on every transition.

| Transition | Trigger | What the core does |
|---|---|---|
| → `STARTING` | `startMatch` (queue filled or direct call); cancellable `MatchStartEvent` | Teleports players to spawns in order; runs `MatchStartTask` countdown (`match.start-countdown`, default 10 s, or your override) |
| → `STARTED` | Countdown expires | Sends `match-started`; calls `game.onMatchStart(match)`: start your repeating tasks here |
| → `ENDING` | `match.end()`; cancellable `MatchEndEvent` | Runs `MatchEndTask`: 5 s teardown |
| → `ENDED` | Teardown completes | Calls `game.onMatchEnd(match)`; records stats (§9); depopulates; releases pooled map; sends `match-ended`; returns everyone to the lobby |

Useful lookups on `IMatchManager`: `getMatch(player)`, `getMatchesForGame(game)`,
`getAllMatches()`, `getMatchById(uuid)`.

## 8. Running the match: participants, elimination, teams

### 8.1 Finding participants

```java
IParticipant participant = ArcadeAPIProvider.get().getParticipant(game, player);
// null when the player isn't in one of your matches
```

`IParticipant`: `getPlayer()`, `getProfile()`, `getUserData()`, `getMatch()`,
`getTeam()` (null for solo games), `isEliminated()`.
`IMatch`: `getParticipants()` (everyone who started, alive or not),
`getAliveParticipants()`, `getEliminatedParticipants()`, `getAliveCount()`.
All of these return defensive copies.

### 8.2 Elimination: you wire the trigger

The core has no death listener. Yours is three lines:

```java
@EventHandler
public void onDeath(PlayerDeathEvent event) {
    IParticipant participant =
            ArcadeAPIProvider.get().getParticipant(game, event.getEntity());
    if (participant != null && !participant.isEliminated()) {
        participant.eliminate(); // or eliminate(killer)
    }
}
```

`eliminate(killer)` fires `ParticipantEliminateEvent` (cancellable, so you
can save the player), marks them eliminated, moves them to spectators, and
calls `game.onParticipantEliminate(participant, killer)`. Eliminations during
`ENDING`/`ENDED` are ignored. Your win-condition check belongs in
`onParticipantEliminate` or a `ParticipantEliminateEvent` handler:

```java
@EventHandler
public void onEliminate(ParticipantEliminateEvent event) {
    IMatch match = event.getParticipant().getMatch();
    if (!match.getGame().getId().equals(game.getId())) return;
    if (match.getAliveCount() == 1) {
        match.endWithWinners(match.getAliveParticipants());
    }
}
```

To eliminate directly: `IMatchManager.eliminateParticipant(participant)`.

### 8.3 Teams

Return `TeamSettings.of(teamSize, maxTeams)` from `getTeamSettings()`. That
flips `isTeamBased()` on. The core partitions match players into `ITeam`s
at creation. Read them via `match.getTeams()` or `participant.getTeam()`:
`getMembers()`, `getAliveMembers()`, `isEliminated()` (no alive members left).

### 8.4 Talking to players

All player text goes through `org.drappula.arcadeApi.message.Messages`. It does
not use Adventure or MiniMessage, so it behaves the same on every version.

```java
match.broadcast("message:<gold>Final showdown!");                    // everyone in the match
match.broadcast("title:<red>Round <n>;subtitle:<gray>Fight!", "n", "3");

Messages.send(player, "actionbar:<yellow>Lava rising to <y>", "y", "40");   // prefix-routed
Messages.chat(sender, "<red>Unknown option <gray><key></gray>", "key", key); // plain chat, no prefix needed
```

- Format: `prefix:text;prefix:text`. Prefixes: `message:`, `title:`,
  `subtitle:`, `titletime:fadeInMs:stayMs:fadeOutMs`, `actionbar:`,
  `bossbar:text:progress:color:overlay`. Escape a literal semicolon as `\;`
  (in Java source `"\\;"`).
- **Unprefixed text is dropped for players** (the console gets it as chat), so
  `broadcast` and `Messages.send` need a prefix, usually `message:`.
  `Messages.chat` is the exception: it always sends chat and takes no prefix.
- Placeholders are `<name>` tokens in the text, filled from alternating
  key/value `String` varargs: `"name", "Bob"`. Values are inserted literally and
  never parsed as markup, so player names or user input cannot inject colours.
- Styling is a MiniMessage subset translated to section-sign codes: the 16
  colour names (`<red>`, `<dark_gray>`, ...), `<b>`, `<i>`, `<u>`, `<st>`,
  `<obf>`, `<reset>`, closing tags like `</b>`, and `<br>`. Hex colours,
  gradients, click and hover events are not supported; unknown tags stay as text.
- Titles and action bars are drawn by XSeries inside ArcadeCore. Boss bars are
  not implemented: a `bossbar:` segment is shown as an action bar on every
  version.
- `IMatch.broadcast(String text, String... placeholders)` is the 1.1.0
  signature. The 1.0.0 `broadcast(String, TagResolver...)` is gone, and no
  Adventure types appear in `ArcadeAPI` any more. Addons built against 1.0.0
  must be recompiled and their broadcast calls rewritten.

Other helpers:

- `addSpectator` / `removeSpectator` / `isSpectating` manage viewers.
- `ArcadeAPI.sendToLobby(player)` returns a player to the lobby manually.

### 8.5 Spawn rules (core-enforced)

Outside matches the core protects spawns: joining clears your inventory and
applies the lobby gamemode, damage for or by lobby players is cancelled,
hunger never changes, and only right-click interaction works (so hotbar menu
items keep working). Matches themselves are unaffected.

During the match start countdown the core additionally cages every
participant in glass and freezes movement (both default-on, see
`getMatchStartSettings()`). Cages snapshot whatever they cover and restore
it when the match starts - safe on hand-built maps. Your win-condition and
elimination wiring are unaffected; only block-breaking/placing during the
countdown stays yours to guard.

## 9. Ending the match and stats

Only your addon ends matches. The typical sequence:

```java
match.setWinnerParticipants(winners); // empty list = no winners → stats skipped
match.end();                          // or atomically: match.endWithWinners(winners)
```

- `MatchEndEvent` is cancellable; cancelling aborts the end.
- The 5 s `MatchEndTask` eliminates any remainder first, then on completion:
  `onMatchEnd` → **stats** → depopulate → release pooled map → lobby return.
- Stats: winners get +1 win, all other participants +1 loss for your
  `game_id`. Skipped entirely when no winners were declared. The core logs
  failures without breaking teardown.
- Read stats back via `ArcadeAPI.getStatsManager()` (`IGameStatsManager`) and
  player data via `getUserData(uuid)` / `getOrCreateUserData(uuid, name)`.

For generated arenas, free your own structures in `onMatchEnd(match)` or a
`MatchEndEvent` handler. The core only releases pooled maps.

## 10. Events reference

| Event | Cancellable | Fired when |
|---|---|---|
| `QueueEnterEvent(player, game)` | yes → `JoinResult.EVENT_DENIED` | Player (or party member) tries to queue |
| `QueueLeaveEvent(player, game, reason)` | no | Player leaves / is removed from queue |
| `MatchStartEvent(match)` | yes → start aborted, map released | Before teleport/countdown |
| `MatchStateChangeEvent(match, old, new)` | no | Every state transition |
| `ParticipantEliminateEvent(participant, killer)` | yes → elimination skipped | `eliminate()` called |
| `MatchEndEvent(match)` | yes → end aborted | `end()` called |

All live in `org.drappula.arcadeApi.events`. Always guard handlers with
`match.getGame().getId().equals("my-game")`, since events are global across games.

## 11. Config knobs that affect your game

Core `config.yml` (server owners tune these; your per-game overrides win):

| Key | Default | Your override |
|---|---|---|
| `queue.start-countdown` | 20 s | `getQueueCountdownSeconds()` |
| `match.start-countdown` | 10 s | `getStartCountdownSeconds()` |
| `lobby.*` | none | Spawn/lobby used by `sendToLobby` |

Per-map config (for values that differ per arena, not per game):
declare options in `getMapConfigOptions()` with a type, default, and bounds.
Admins set them in-game (`/arcade map config <map> set <key> <value>`,
`unset` reverts to your default, `list` shows effective values); the core
validates and persists them, pre-filling defaults as rows when a map is
created or your game registers. Read them off the match's map
(`getIntConfig` etc.); an unset key yields your default, so matches work
with zero admin setup.

Your own `config.yml` holds everything else (player counts, timings, arena
parameters). Read it via your plugin's `getConfig()`.

## 12. Minimal complete example (solo last-player-standing)

```java
// MyGame.java
public class MyGame extends AbstractGame {
    public MyGame() { super("my-game", "My Game", 2, 8); }

    @Override
    public int getPlayersRequired() { return getMinPlayers(); }

    @Override
    public IArcadeMap createArena(World world, List<Player> players) {
        return null; // use the static map pool managed via /arcade map
    }
}

// MyListener.java
public class MyListener implements Listener {
    private final MyGame game;
    public MyListener(MyGame game) { this.game = game; }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        IParticipant p = ArcadeAPIProvider.get().getParticipant(game, event.getEntity());
        if (p != null && !p.isEliminated()) p.eliminate();
    }

    @EventHandler
    public void onEliminate(ParticipantEliminateEvent event) {
        IMatch match = event.getParticipant().getMatch();
        if (!match.getGame().getId().equals(game.getId())) return;
        if (match.getAliveCount() == 1) {
            match.endWithWinners(match.getAliveParticipants());
        }
    }
}
```

Plus a `/mygame join|leave` command delegating to
`getQueueManager().joinQueue/leaveQueue` (§6) and the entrypoint from §3.

## 13. Testing your addon

Mirror the core's harness: MockBukkit plus a mocked `ArcadeAPI`.

```java
@BeforeEach
void setup() {
    server = MockBukkit.mock();
    ArcadeAPI api = mock(ArcadeAPI.class);
    when(api.getGameManager()).thenReturn(mock(IGameManager.class));
    when(api.getQueueManager()).thenReturn(mock(IQueueManager.class));
    ArcadeAPIProvider.register(api);
    plugin = MockBukkit.load(MyMinigame.class); // plugin class must NOT be final
}

@AfterEach
void teardown() {
    MockBukkit.unmock();
    ArcadeAPIProvider.unregister();
}
```

- Fire real events through `server.getPluginManager().callEvent(...)` and
  advance schedulers with `server.getScheduler().performTicks(n)`.
- MockBukkit's default worlds cap at y=128, so point your arena config at a
  taller `WorldMock` (e.g. `new WorldMock(Material.AIR, Biome.PLAINS, -64, 320, -64)`)
  for high-altitude builds.
- Assert player-visible text via `player.nextMessage()` (legacy section-sign
  string). Titles go through the `ScreenText` backend, which is not installed
  in tests, so do not assert titles; check state instead.

## 14. Pitfalls checklist

1. **Never forget `end()`.** No timer, player-count check, or death listener
   exists in the core. A match without `end()` runs forever.
2. **Declare winners or stats are skipped.** Ending with an empty winner list
   records nothing; usually a bug, occasionally intentional (abandoned match).
3. **Guard events by game id.** All framework events fire for every game.
4. **`MatchStartEvent` can deny your match.** If you cancel it (or the core
   finds no map/spawns), `startMatch` returns `null`. Handle it in manual
   start paths; the queue path recycles players automatically.
5. **Don't teleport players yourself at start.** The core teleports spawn `i`
   → player `i`, so make sure your spawn list order matches your intent.
6. **Keep DB work off the hot path.** The core calls the database
   synchronously on the server thread; your listeners should too stay small.
7. **New cross-plugin surface goes in `ArcadeAPI` first**, then the impl
   singleton, then exposure through `ArcadeAPIImpl`. Never reach into
   `arcadeCore` internals from an addon.
8. **Keep it 1.8-safe.** No Paper-only API, no Java 9+ syntax, no raw modern
   material names in code (section 2.3). `broadcast` and `Messages.send` text
   needs a `message:` (or other) prefix or players see nothing.
