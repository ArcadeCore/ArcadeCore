# AGENTS.md

Paper 1.21.11 plugin (Java 21 toolchain; Kotlin `jvm` applied but no `.kt` sources yet). Gradle root with two subprojects:

- `ArcadeAPI` (`org.drappula.arcadeApi`): `java`-only interfaces/events, no implementation. What addon game plugins compile against.
- `ArcadePlugin` (`org.drappula.arcadeCore`): the implementation; produces the runnable shadowed jar. Entry `ArcadeCore.java` (`ArcadeCore.get()` singleton), loader `ArcadeCoreLoader`.

Paper API version (`1.21.11`) is declared in **both** build files (`implementation` in `ArcadeAPI`, `compileOnly` in `ArcadePlugin`) — bump both together.

## Build & verify (run from root)

- `./gradlew build` — compiles both modules, runs tests, incl. `shadowJar`; takes ~2 min.
- `./gradlew test` / `./gradlew :ArcadePlugin:test` — unit suite (JUnit 5 + Mockito + MockBukkit). For focused runs use `--tests "<class>"`.
- `./gradlew :ArcadePlugin:shadowJar` — runnable relocated jar → `ArcadePlugin/build/libs`.
- `./gradlew :ArcadePlugin:runServer` — local Paper 1.21.11 test server with the plugin installed (`run-paper`, `-Xms2G -Xmx2G`); server files land in `ArcadePlugin/run/` (gitignored via `run`). On memory-constrained machines lower `jvmArgs` first — a 2G server plus Gradle workers has OOM-crashed a loaded desktop.
- `./gradlew :ArcadePlugin:smokeTest` — real smoke test: boots Paper in tmux via `runServer`, asserts `Enabling ArcadeCore` with no errors/exceptions, sends `arcade` + `stop` via `tmux send-keys`, reads logs via `tmux capture-pane` (`scripts/smoke-test.sh`, ~1 min warm).
- Sibling-project live-server check (ArcadeBedrockPillars runs ArcadeCore in its own `run/` dir): polls `run/logs/latest.log` for boot, then shows ArcadeCore enable/errors. Assumes the server is already running there — it waits, it does not launch:
  `cd ~/Projects/ArcadeBedrockPillars && for i in $(seq 1 100); do sleep 5; LOG=run/logs/latest.log; if grep -qE "Done \([0-9.]+s\)" "$LOG" 2>/dev/null; then echo "BOOTED after ~$((i*5))s"; break; fi; if grep -qE "FAILED TO BIND TO PORT|OutOfMemoryError|You need to agree" "$LOG" 2>/dev/null; then echo FATAL; break; fi; done; grep -E "Enabling Arcade|ERROR|Exception" run/logs/latest.log | head -n 6`
- No CI, no lint/format config. `org.gradle.configuration-cache/parallel/caching` are on (`gradle.properties`).

## Test-driven development (mandatory)

TDD is required for all work in this repo — new features, bug fixes, refactors, behavior changes. No production code without a failing test first (RED → verify fail → GREEN → verify pass → REFACTOR). New cross-plugin surface still goes in `ArcadeAPI` first, but the test goes first of all.

Test harness notes (all verified against the current suite):

- Shared base is `ServerTest` (MockBukkit + mocked `ArcadeCore` over real config files). Events, scheduler ticks (`performTicks`), joins/leaves, full match lifecycle, and command handlers run for real.
- SQLite-backed managers use in-memory DB via test-only `database/TestDb.java` (reflection injection — never add test hooks to prod classes).
- `paper-api` is `compileOnly` in `ArcadePlugin`, so tests redeclare it as `testImplementation`; MockBukkit is declared first so its bundled `paper-api` wins classpath order. Keep that order.
- Known mock gap: MockBukkit records legacy titles only — Adventure `sendTitlePart` is a silent no-op, so assert task completion via state + lifecycle callbacks, not titles.

## Lifecycle (`ArcadeCore.java`)

`onEnable` order matters: `setupConfig()` → `connectDatabase()` (throws on failure) → `MapManager.get().load()` → `destroyOrphanedArenas()` (crash leftovers) → commands → listeners → 30 s arena sweeper task → `registerAPI()` (publishes `ArcadeAPIImpl` via `ArcadeAPIProvider`).
`onDisable`: `GameManager.reload()` (wipes in-memory matches/participants — nothing match-related survives restart) → `ArenaWorldManager.destroyAll()` (evacuates players, unloads + deletes leftover arena worlds) → `MapManager.releaseAll()` (clears `in_use` flags in memory and DB) → `Database.disconnect()`.

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

- Single static SQLite connection (`<data-folder>/database.db`, `PRAGMA foreign_keys = ON`), raw JDBC, all called synchronously on the server thread — keep queries small, no connection pooling to configure.
- Six tables (`Database.java`): `user_profiles`, `games`, `game_stats` (per-player wins/losses, `other_stats` JSON currently always NULL), `maps` (`enabled` + `in_use` flags), `map_spawns` (cascade-deletes with its map), `map_config` (per-map addon-option overrides, cascade-deletes with its map).
- `MapManager` loads the pool at enable, skipping maps whose world isn't loaded (warning, not error). All map admin is in-game via `/arcade map ...`; there is no file-based map config.

## Configs & messages

- Three BoostedYAML configs (`config/Config.java` wraps `YamlDocument` + `SpigotSerializer` + `BasicVersioning` on a `version` key, so files self-update to packaged defaults): `data.yml` (mutable runtime state, e.g. `SPAWN_LOCATION`), `config.yml` (`lobby.*`, `match.*`, `queue.start-countdown`), `messages.yml` (all player text).
- Player-facing text goes through `MessageUtil.sendMessage` (MiniMessage + Adventure placeholders; `;`-separated segments with `title:`/`subtitle:`/`titletime:`/`actionbar:`/`message:`/`bossbar:` prefixes route to title parts, action bar, chat, boss bars). Adding a message = new key in `messages.yml` **plus** a `MessagesConfig.get().getString("<key>")` call — both, or it NPEs/reads nothing.
- `/arcade reload` reloads all three configs.

## Commands (`MainCommand`, Brigadier via `LifecycleEvents.COMMANDS` — only `paper-plugin.yml`, no `plugin.yml`)

- `/arcade` info · `reload` · `setspawn` · `start <game>` (perm `arcade.force-start`, fails silently on success) · `queue <game>` (player-only, `queue-joined`/`queue-denied`) · `map create|addspawn|enable|disable|list|delete` (perm `arcade.map.admin`; `create` uses invoker's world, `addspawn` invoker's exact location) · `map config <mapId> set <key> <value>|unset <key>|list` (per-map values for addon-registered `Game.getMapConfigOptions()`, typed + validated, stored in `map_config`).
- `paper-plugin.yml` uses `processResources` token expansion for version/description — a literal `$` there breaks the build templating.

## Dependencies

`boosted-yaml` (+ spigot-serializer) and `sqlite-jdbc` are **both** shadow-bundled (`com.gradleup.shadow`, plugin module only) **and** runtime-resolved in `ArcadeCoreLoader` via `MavenLibraryResolver`. Don't remove either side without checking the other.

## Scope: framework only

Queue/match/map/profile plumbing lives here. Addons own: game rules, win conditions and `setWinnerParticipants`, `Match.end()` triggers, death-to-elimination wiring. Don't add game logic to this repo. New cross-plugin surface goes in `ArcadeAPI` first, then the impl singleton, then exposure through `ArcadeAPIImpl`.

## Docs upkeep

After any behavior change, update this file if its facts went stale, and `API_USAGE.md` if addon-facing surface changed (new `Game` defaults, events, commands, config knobs).

## Docs

`API_USAGE.md` is the addon-author guide (project setup → `Game` → arenas → queue → match lifecycle → ending a match). `CLAUDE.md` is partially stale (claims no test suite, predates orphan-arena sweeper/teams/winner APIs) — trust this file and the code over it.
