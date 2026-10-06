# AGENTS.md

Paper 1.21.11 plugin (Java 21 toolchain; Kotlin `jvm` only in `ArcadePlugin`). Gradle root is `ArcadeCore/`; subprojects `ArcadeAPI` / `ArcadePlugin`.

- `ArcadeAPI` (`org.drappula.arcadeApi`): `java`-only interfaces/events, no implementation. Other plugins depend on this.
- `ArcadePlugin` (`org.drappula.arcadeCore`): the implementation; produces the runnable jar. Entry `ArcadeCore.java` (`ArcadeCore.get()` singleton), loader `ArcadeCoreLoader`.

## Commands (run from root)

- `./gradlew build` — builds both modules; shadowJar runs as part of build.
- `./gradlew :ArcadePlugin:shadowJar` — runnable relocated jar → `ArcadePlugin/build/libs`.
- `./gradlew :ArcadePlugin:runServer` — local Paper 1.21.11 test server with plugin installed (`run-paper`, `-Xms2G -Xmx2G`).
- No test suite configured.

## Conventions / gotchas

- New cross-plugin feature: add interface/event to `ArcadeAPI` first, implement as `X.get()` singleton under `ArcadePlugin/.../managers/`, expose via `api/ArcadeAPIImpl` (registered once in `ArcadeCore#onEnable`).
- Deps `boosted-yaml` (+ spigot-serializer) and `sqlite-jdbc` are **both** shadow-bundled (`com.gradleup.shadow`, plugin module only) **and** runtime-resolved in `ArcadeCoreLoader` via `MavenLibraryResolver`. Don't remove either side without checking the other.
- Commands are Brigadier via `LifecycleEvents.COMMANDS` — only `paper-plugin.yml`, no `plugin.yml`. `paper-plugin.yml` uses `${version}`/`${description}` token expansion in `processResources`.
- `/arcade reload` reloads `DataConfig` (`data.yml`) only, not `MainConfig`/`MessagesConfig`. `/arcade queue <game>` handler is currently a no-op (does not join queue).
- `MatchManager.startMatch` can return null: no free map, map has fewer spawns than players, or cancellable `MatchStartEvent` denied. `Match` winner setter exists but nothing calls it yet.
- Maps live in SQLite (`database.db`, single connection, `PRAGMA foreign_keys = ON`), administered in-game via `/arcade map ...` — no file config. Worlds not loaded at enable are skipped. `onDisable` calls `MapManager.releaseAll()` to clear `in_use` flags.
- `ProfileManager` is in-memory session state; persisted user data is separate (`UserData`/`user_profiles` via `UserDataManager`). `games`/`game_stats` tables exist but are unused.
