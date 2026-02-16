# EZSurvivalStats

EZSurvivalStats is a production-ready Paper 1.21.x plugin for tracking and presenting all-time survival statistics with inventory GUIs and leaderboard caching.

## Features

- SQLite-backed all-time player stat tracking.
- Tracks:
  - Playtime (seconds)
  - Joins
  - Last seen timestamp
  - Deaths
  - Mob kills by entity type
  - Blocks mined by material
  - Items collected by material
- Configurable creative-mode tracking.
- Async database writes and periodic flushes.
- `/stats`, `/stats <player>`, `/top`, `/ezstats reload` commands with tab completion.
- 54-slot GUI menus with pagination.
- Leaderboards cached in memory and refreshed asynchronously.
- Optional PlaceholderAPI placeholders:
  - `%ezstats_playtime%`
  - `%ezstats_total_kills%`
  - `%ezstats_total_mined%`
  - `%ezstats_total_collected%`
  - `%ezstats_deaths%`

## Build

```bash
mvn clean package
```

The plugin jar is generated in `target/`.

## Configuration

### `config.yml`

- `flush_interval_seconds`
- `leaderboard_refresh_minutes`
- `track_creative`
- `gui_titles`

### `messages.yml`

All player/admin-facing chat text is configurable.

## Data storage

SQLite database path:

`plugins/EZSurvivalStats/data.db`

Schema supports:
- Per-player totals
- Per-player category breakdowns
- Leaderboard queries
- Future stat expansion via breakdown tables

## GitHub Actions

Workflow file: `.github/workflows/build.yml`

- Builds on push to `main` and tag push `v*`
- Uploads built jar as an artifact
- Creates a GitHub release and attaches jar assets on tag push
