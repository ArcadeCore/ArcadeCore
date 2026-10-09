# AGENTS.md

Spigot/Paper plugin, one jar for Minecraft 1.8 to 26.3 (and forks). Production code is Java 8 bytecode (`options.release.set(8)`) compiled against `spigot-api:1.8.8-R0.1-SNAPSHOT` (`compileOnly`); tests run on JDK 21 with MockBukkit v1.21. Gradle root with two subprojects:

- `ArcadeAPI` (`org.drappula.arcadeApi`, version `1.1.0`): `java`-only interfaces/events plus `message/Messages`, `LegacyText`, `ScreenText`. No implementation of the core. What addon game plugins compile against. No paper-api or Adventure types leak through it.
- `ArcadePlugin` (`org.drappula.arcadeCore`): the implementation; produces the runnable shadowed jar. Entry `ArcadeCore.java` (`ArcadeCore.get()` singleton). Descriptor is `plugin.yml` (no `api-version`); there is no `paper-plugin.yml` and no loader class.

Keep production code version-neutral: no Paper-only API, no Adventure/MiniMessage, no Brigadier, no SLF4J logger, no Java 9+ syntax or library calls, no `Event#callEvent`. Use `Log`, `Events.call`, `Messages`, and XSeries (`XMaterial`) for anything whose name changed across versions. If an API only exists on newer servers, find the 1.8 equivalent or degrade, and say so in `API_USAGE.md`.

## Build & verify (run from root)

- `./gradlew build` - compiles both modules, runs tests, incl. `shadowJar`.
- `./gradlew test` / `./gradlew :ArcadePlugin:test` / `./gradlew :ArcadeAPI:test` - unit suites (JUnit 5 + Mockito; ArcadePlugin also MockBukkit). For focused runs use `--tests "<class>"`. Last full run: 220 tests pass in ArcadeCore.
- `./gradlew :ArcadePlugin:shadowJar` - runnable jar → `ArcadePlugin/build/libs/ArcadePlugin-1.0.0-all.jar`. Shades `boosted-yaml`, `boosted-yaml-spigot`, `XSeries` (relocated to `org.drappula.arcadeCore.libs.xseries`) and `sqlite-jdbc`. `mergeServiceFiles()` is required so the shaded sqlite driver is visible; `Database.connect` also calls `Class.forName("org.sqlite.JDBC")`.
- There is no `runServer` task any more (run-paper and the Kotlin plugin were removed). For a live server use the matrix scripts in `../TestServer-matrix` (`matrix.sh` boots 1.8.8, 1.12.2, 1.16.5, 1.20.4, 1.21.4 and 26.3 with the current jars; `run-server.sh` feeds console commands over stdin) or a manual server with the jar in `plugins/`.
- `scripts/smoke-test.sh` and `:ArcadePlugin:smokeTest` still exist but were written for the old `runServer` flow; check them before relying on them.
- Never run Gradle in two of the four repos at once: addons include this build via `includeBuild`, and they share its `build` directory.
- Verified: boot + command smoke on Paper 1.8.8, 1.12.2, 1.16.5, 1.20.4, 1.21.4 and 26.3 with ArcadeCore + BedrockPillars + FFA + Hub, no errors. A bot-driven full match on each version has not been done.
- No CI, no lint/format config. `org.gradle.configuration-cache/parallel/caching` are on (`gradle.properties`).

## Test-driven development (mandatory)

TDD is required for all work in this repo - new features, bug fixes, refactors, behavior changes. No production code without a failing test first (RED → verify fail → GREEN → verify pass → REFACTOR). New cross-plugin surface still goes in `ArcadeAPI` first, but the test goes first of all.

Test harness notes (all verified against the current suite):

- Shared base is `ServerTest` (MockBukkit + mocked `ArcadeCore` over real config files). Events, scheduler ticks (`performTicks`), joins/leaves, full match lifecycle, and command handlers run for real.
- SQLite-backed managers use in-memory DB via test-only `database/TestDb.java` (reflection injection - never add test hooks to prod classes).
- `spigot-api` is `compileOnly`, so tests redeclare what they need. In `ArcadePlugin` MockBukkit is declared first so its bundled `paper-api` wins classpath order; keep that order. Paper API is only on the test classpath, so a passing test does not prove a call exists on 1.8.
- Message assertions: core text goes through `Messages`, which ends in `sendMessage(String)`, so tests read `player.nextMessage()`. Titles and action bars go through the `ScreenText` backend (`Messages.useScreenText`); tests do not install `XSeriesScreenText`, so assert task completion via state + lifecycle callbacks, not titles.

## Lifecycle (`ArcadeCore.java`)

`onEnable` order matters: `Messages.useScreenText(new XSeriesScreenText())` → `setupConfig()` → `connectDatabase()` (throws on failure) → `MapManager.get().load()` → `destroyOrphanedArenas()` (crash leftovers) → commands (`getCommand("arcade")` executor + tab completer, declared in `plugin.yml`) → listeners (`LobbyListener`, `MatchListener`) → 30 s arena sweeper task → `registerAPI()` (publishes `ArcadeAPIImpl` via `ArcadeAPIProvider`).
`onDisable`: `GameManager.reload()` (wipes in-memory matches/participants - nothing match-related survives restart) → `ArenaWorldManager.destroyAll()` (evacuates players, unloads + deletes leftover arena worlds) → `MapManager.releaseAll()` (clears `in_use` flags in memory and DB) → `Database.disconnect()`.

## Match / queue flow

- States: `LOADING → STARTING → STARTED → ENDING → ENDED`. Only the addon ends a match: something must call `IMatch.end()` → `MatchManager.endMatch` (announces winner + `match-ended` immediately) → 5 s `MatchEndTask` (eliminates remainder, depopulates, releases map, clears inventories, lobbies spectators). There is no last-player-standing check, timer expiry, or death listener in this repo.
- `QueueManager.joinQueue` fires cancellable `QueueEnterEvent`; at `minPlayers` a countdown starts (`queue.start-countdown`, default 20 s, overridable per-game via `getQueueCountdownSeconds()`), at `maxPlayers` the match starts immediately. If `startMatch` returns null (no free map, too few spawns, `MatchStartEvent` denied), the sliced players go back to the front of the queue, the countdown restarts at threshold, and they get `map-unavailable`.
- `Match.end()` results persist via `GameStatsManager.recordMatchResult` in teardown: winners +1 win, other participants +1 loss; skipped entirely when the addon declared no winners via `Match.setWinnerParticipants` (or the atomic `endWithWinners`). Failures log, never break teardown.
- `Game` interface defaults addons inherit: `isEnabled → true`, `min/maxPlayers → playersRequired`, `getGameEndSettings → DEFAULT`, per-game countdown overrides empty (fall back to `queue`/`match.start-countdown`), `createArena(world, players) → null` (null = use the shared map pool).
- Every match gets a throwaway void arena world (`ArenaWorldManager`, `arcade-<game>-<8hex>`, `VoidChunkGenerator`, autosave off); `createArena` builds into it, teardown unloads + deletes it, `onDisable` destroys leftovers. Play worlds and their borders are never touched. Successful `startMatch` also runs `MatchStartTask` (`match.start-countdown`, default 10 s, per-game overridable).
- `GameManager.unregisterGame` also drops that game's queue via `QueueManager.removeGame` (cancels its countdown, fires `QueueLeaveEvent` with `GAME_UNREGISTERED` per queued player).
- Managers are hand-rolled `X.get()` lazy singletons (not DI). `GameManager` keys in-memory matches/participants by game ID.

## Lobby rules (`LobbyListener`, players with no match)

- Join: register profile → clear inventory → `sendToLobby` (lobby gamemode/fly from config; teleports to spawn, or default world spawn when unset).
- Quit: `leaveQueue(QUIT)` → eliminate from live match → drop profile.
- Outside matches: damage dealt/taken cancelled, hunger frozen, left-click/physical interact cancelled (right-click allowed, so menu items work).

## Maps & database

- Single static SQLite connection (`<data-folder>/database.db`, `PRAGMA foreign_keys = ON`), raw JDBC, all called synchronously on the server thread - keep queries small, no connection pooling to configure.
- Six tables (`Database.java`): `user_profiles`, `games`, `game_stats` (per-player wins/losses, `other_stats` JSON currently always NULL), `maps` (`enabled` + `in_use` flags), `map_spawns` (cascade-deletes with its map), `map_config` (per-map addon-option overrides, cascade-deletes with its map).
- `MapManager` loads the pool at enable, skipping maps whose world isn't loaded (warning, not error). All map admin is in-game via `/arcade map ...`; there is no file-based map config.

## Configs & messages

- Three BoostedYAML configs (`config/Config.java` wraps `YamlDocument` + `SpigotSerializer` + `BasicVersioning` on a `version` key, so files self-update to packaged defaults): `data.yml` (mutable runtime state, e.g. `SPAWN_LOCATION`), `config.yml` (`lobby.*`, `match.*`, `queue.start-countdown`), `messages.yml` (all player text).
- Player-facing text goes through `MessageUtil.sendMessage`, a thin wrapper over `org.drappula.arcadeApi.message.Messages` (it lives in `ArcadeAPI` so addons share it). Format `prefix:text;prefix:text`; prefixes `message:`, `title:`, `subtitle:`, `titletime:fadeInMs:stayMs:fadeOutMs`, `actionbar:`, `bossbar:text:progress:color:overlay`. Escape a literal `;` as `\;`. Unprefixed text is dropped for players and sent as chat to the console. Placeholders are `<name>` tokens filled from alternating key/value varargs; values are inserted literally, never parsed as markup.
- Text is a MiniMessage subset translated to section-sign codes by `LegacyText`: colour names, `<b>`, `<i>`, `<u>`, `<st>`, `<obf>`, `<reset>`, closing tags, `<br>`. Hex colours, gradients and click/hover events are not supported.
- Titles and action bars go through a `ScreenText` implementation; `ArcadeCore.onEnable` installs `XSeriesScreenText` (XSeries `Titles`/`ActionBar`). Without one, `Messages` falls back to chat. Boss bars are not implemented on any version: a `bossbar:` segment is shown as an action bar with its text.
- Adding a message = new key in `messages.yml` **plus** a `MessagesConfig.get().getString("<key>")` call - both, or it NPEs/reads nothing.
- `/arcade reload` reloads all three configs.

## Commands (`MainCommand`, plain Bukkit `CommandExecutor` + `TabCompleter`, declared in the `commands:` block of `plugin.yml`)

- No Brigadier: argument checks and tab completion are hand-written in `MainCommand`. Admin feedback with no `messages.yml` key goes through `Messages.chat`.
- `/arcade` info · `reload` · `setspawn` · `start <game>` (perm `arcade.force-start`, fails silently on success) · `queue <game>` (player-only, `queue-joined`/`queue-denied`) · `map create|addspawn|enable|disable|list|delete` (perm `arcade.map.admin`; `create` uses invoker's world, `addspawn` invoker's exact location) · `map config <mapId> set <key> <value>|unset <key>|list` (per-map values for addon-registered `Game.getMapConfigOptions()`, typed + validated, stored in `map_config`).
- `plugin.yml` uses `processResources` token expansion for version/description, so a literal `$` there breaks the build templating.

## Dependencies

`boosted-yaml` (+ `boosted-yaml-spigot`), `XSeries` and `sqlite-jdbc` are shadow-bundled into the jar (`com.gradleup.shadow`, plugin module only). Nothing is resolved at runtime; there is no `MavenLibraryResolver`. Only XSeries is relocated. Addons depend on ArcadeCore with `depend: [ArcadeCore]` in their own `plugin.yml`, which puts the ArcadeAPI classes on their classpath.

## Scope: framework only

Queue/match/map/profile plumbing lives here. Addons own: game rules, win conditions and `setWinnerParticipants`, `Match.end()` triggers, death-to-elimination wiring. Don't add game logic to this repo. New cross-plugin surface goes in `ArcadeAPI` first, then the impl singleton, then exposure through `ArcadeAPIImpl`.

## Docs upkeep

After any behavior change, update this file if its facts went stale, and `API_USAGE.md` if addon-facing surface changed (new `Game` defaults, events, commands, config knobs).

## Docs

`API_USAGE.md` is the addon-author guide (project setup → `Game` → arenas → queue → match lifecycle → ending a match). `CLAUDE.md` is a shorter overview for Claude Code; where the two disagree, trust this file and the code.
