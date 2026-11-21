# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed
- Settings opens this plugin's RuneLite config. The button was talking to a panel that was not on screen.
- Item names stay on the last real name instead of flashing the item id. Right-click on a Grand Exchange slot adds one Runetick line, not two.
- The Runetick right-click line is green, so it is not the same color as the game's own options.
### Changed
- A sell shows what Grand Exchange history says you paid, and the profit against that price. The market quote is labeled Mkt buy when there is no history.
- The item card and the Grand Exchange tip always show high alch: how much more it keeps than the sell, or that the sell is better.
- Sidebar wealth is the full stack (coins, items, and open offers), and it keeps the last coin total if the bag is unreadable for a tick. Session starts after the bank has been opened once.
- The sidebar lists open flips and recent fills, and Settings opens this plugin's RuneLite config.
- The Grand Exchange tip and the search card say Alch only when high alchemy beats the taxed sell, after one nature rune.
- Exceptions go to the RuneLite client log. The separate error.log file is gone.
- Settings no longer repeat the install warning. Sending offers, history, cash, and item lookups is covered once, at install.
- The sidebar is a shorter list: gold and session on one line, item rows without boxes.
- The Grand Exchange tip shows how many you can afford and the profit on that quantity.
- The sidebar uses the same dark rows as the site: item icons, white prices, green profit. Search opens the quote in the panel.
- The Grand Exchange tip says Margin, and item tags say margin. The old fat label is gone.
- The Grand Exchange overview no longer lists a dip or an alchemy line.
- The sidebar lists sells the market already traded in the last 15 minutes, with the profit on that quantity. A search box opens the item on the site.
- The sidebar icon is the flap R. Reprice is ±, because the price can move either way.
- The GE tip is the item, its buy and sell, and the after-tax profit. The call legend sits under the Grand Exchange window. Opening an offer you already placed still shows that item.
- Numbers and the Runetick title use the highlight color chosen under Account → Theme.
- Sidebar and GE marks use the board colours. Calls are hold, reprice, and pull. The board section is labelled Margin, and the tip says full limit.

### Packaging
- Hosted download: `https://runetick.ironhopeshop.com/runelite.jar` (also `/runelite` install page).
- `./gradlew distZip` → `dist/runetick-runelite-*.zip` (plugin jar + INSTALL.txt). Sideload into `~/.runelite/sideloaded-plugins/`.

## [0.4.21] — 2026-11-19

### Changed
- GE marks are a corner badge, not a bar. A legend sits at the top of the Grand Exchange: `-` hold, `^` raise, `v` lower, `x` out.

## [0.4.20] — 2026-11-19

### Changed
- GE slot marks sit on the item icon: a color bar for buy or sell, and a glyph. `-` hold, `^` raise, `v` lower, `x` back out.

## [0.4.19] — 2026-11-19

### Added
- Plugin settings **Hide cheap**, **Hide slow**, and **Best only** (on by default). Best means not under 10k, at least 20 clears an hour, and 5m dream profit.

## [0.4.18] — 2026-11-19

### Fixed
- GE buy/sell page tip follows the offer you opened, not the previous search.

### Added
- Slot labels on the Grand Exchange (buy/sell price, or Raise / Lower / Back out).
- Right-click **Runetick** on a GE slot, and on Back while an offer is open.

## [0.4.17] — 2026-11-19

### Changed
- Prepared for the RuneLite Plugin Hub: plugin files stay in the plugin directory, and anything that contacts Runetick is off until you opt in.
- Session, cash, and the error log are read and written only through the RuneLite Filepath API.

## [0.4.16] — 2026-11-19

### Fixed
- Sidebar opens itself on startup. Labels no longer claim infinite height, which was swallowing the panel.

## [0.4.15] — 2026-11-19

### Fixed
- Sidebar panel was empty/collapsed after the overflow fix (viewport preferred height pinned to 0). Cap width only; height lays out normally again.

## [0.4.14] — 2026-11-19

### Fixed
- Sidebar **horizontal overflow**: column/card widths capped to the plugin panel; KPI + card footers wrap instead of forcing x-scroll.

### Added
- **LIVE GE** cards with volume-based **est. fill/sale ETA** (`~12m` / `~2h` / `slow`); also on GE tip + flip rows.
- **Offer price alerts**: desktop notify when market moves ≥2% against an open offer (raise bid / lower ask / margin crush), deduped per slot.
- Config: `Offer fill ETA`, `Offer price alerts` (both default on).

## [0.4.13] — 2026-11-19

### Changed
- Sidebar rebuilt: **wealth tiles** (Inv+GE / Net / Session) + **card rows** instead of a truncated HTML wall. Names wrap; GE tip overlay wider.

## [0.4.12] — 2026-11-19

### Added
- **Inv+GE** total (bag cash + bag items + open offers, no bank) in the panel and on `/track`.

### Fixed
- Live GE sync was POSTing on every offer tick and hitting GCS **429** → `/track` never got offers. Debounced to ≥10s with 60s backoff on failure.

## [0.4.11] — 2026-11-19

### Added
- Sync **live GE slots** + **net worth** to the Runetick site on every offer change and every 30s (shown on `/track`).
- Net worth revalues every ~6s while logged in so the panel stays fresh.

### Fixed
- Offers only updated site flips that matched a placed plan row — casual GE activity never appeared on Runetick.

## [0.4.10] — 2026-11-19

### Fixed
- Net worth **Session** delta only arms after inventory + bank are both scanned (opening bank no longer looks like profit). Shows “open bank to start” until then.

## [0.4.9] — 2026-11-19

### Added
- Config **Fat style**: ALL / BUSY (high volume) / PRICEY (high ticket, thinner).

## [0.4.8] — 2026-11-19

### Changed
- Net worth includes **GE offers**: buy slots count locked cash + filled items waiting; sell slots count unsold stock (GE price) + coins waiting to collect.

## [0.4.7] — 2026-11-19

### Added
- **Net worth** estimate in the cash panel: cash + GE-priced inventory/bank items, plus session delta. Open bank once for a full total.

## [0.4.6] — 2026-11-19

### Added
- Right-click **Runetick** on inventory/bank/GE items (and click/right-click board rows) opens `/item/{id}` on the site.
- Bank item scan: open bank once → **BANK HITS** lists fat/dip/flip overlaps you already hold; rows also tag `bank N`. Highlights apply in the bank UI.

## [0.4.5] — 2026-11-19

### Changed
- Board rows spell out `buy X -> sell Y` and `profit` / `margin` / `fill` so columns are obvious.

### Added
- Config: **Min fat profit**, **Min margin / item**, **Hide alch dips**.

## [0.4.4] — 2026-11-19

### Fixed
- Sidebar board text no longer clips: stacked `name` / `buy->sell  total` rows sized to the panel width.

### Added
- GE item tip shows **Can buy** from inv + last-known bank at the tip buy price, vs GE limit.

## [0.4.3] — 2026-11-19

### Fixed
- Fat dream stacks were nerfed because cash sync wrote inventory gp into `capitalGp`. Sync no longer touches plan capital; plugin sizes fat from account capital (floor 125m, or bank-seen wealth if higher).
- Sidebar uses full-width tables: `name | buy->sell | total`.

## [0.4.2] — 2026-11-19

### Changed
- Board (fat/dips/flips/history + cash) lives in the **sidebar panel**; left overlay is an optional tiny status chip (off by default). GE item tips stay on-screen when viewing an offer.
- Last-known inv/bank cash saved to `~/.runelite/runetick/cash.json` and restored after restart / logout until refreshed.

## [0.4.1] — 2026-11-19

### Fixed
- Overlay tofu squares: RuneLite’s font cannot draw braille/`·`/`…`/`→`/Unicode minus — HUD + GE tip are ASCII-only now.
- Sidebar panel shows connection, sync, cash, board counts, and Open site (not just Connect).

## [0.4.0] — 2026-11-19

### Added
- After you **log into OSRS**, the plugin uploads RuneLite GE history + last-known cash to `POST /v1/me/sync` so they appear on the website Trade log.
- HUD/panel loading states: “loading…”, “log in to sync”, “syncing…”, and sync result lines.

## [0.3.1] — 2026-11-19

### Changed
- Denser HUD/GE layout: FAT → DIPS first, one-line rows, cash on a single line, less chrome. Same data.

## [0.3.0] — 2026-11-19

### Changed
- HUD focus is **Fat spreads** (dream stack profit) and **Dips** (% under 30d / alch floor) — not lame day-trade z-scores.
- Pulls `/v1/plan` `fat[]` + `/v1/dips` for the board; GE tip prefers fat/dip prices when the open item matches.

## [0.2.0] — 2026-11-19

### Added
- HUD cash: inventory gp always, bank gp cached after you open the bank (coins + platinum×1k). Plan capital prefers live known cash.
- GE history on HUD from RuneLite’s `grandexchange.tradeHistory` (recent buys/sells).
- Desktop notifications on flip updates / logged trades (works in fullscreen). Overlays use `ALWAYS_ON_TOP` with a `⠿` drag tab — **Alt-drag** to move.
- Day-trade copy: Buy dip / Sell spike with z-score strength, concrete buy/sell prices, and hint text (not just “Cheap/Dear”).
- Exact gp everywhere that matters (`4,412` not `4.4k`). Bigger 320px HUD + GE tip panels; GE tip shows on the slot board overview and every offer screen.
- In-game HUD overlay (top-left): open flips, next recommended buys / day trades from `/v1/plan`, cheap/dear day-trade signals from `/v1/signals/reversion`, and today's profit.
- GE offer overlay: when an item is open on the Grand Exchange, shows **Buy at / Sell at** (recommended from your flip or plan) vs **Mkt avg**, after-tax sell, plan size, and day-trade z-score when relevant.
- Inventory / GE inventory item tags for open flips and recommended buys.
- Polls `/v1/me`, `/v1/plan`, and day-trade signals about every minute while connected; loads `/v1/items/:id` when the GE selection changes.
- Settings toggles for GE tips, item highlights, and day-trade signals.
- Harden GE tips: canonicalize noted item ids on the client thread (off-thread canonicalize was an AssertionError that killed the client), treat API 404 as “no tip”, catch overlay render errors, don’t tag the whole GE interface (only GE inventory).
- Flips + GE tip overlays are movable (Alt-drag). GE tip defaults to top-right so it doesn’t stack on the main HUD.
- Overlay render catches Throwable (AssertionError is an Error, not a RuntimeException) so a tip bug can’t kill the client; uncaught exceptions flush to `~/.runelite/runetick/error.log`.
- GE item tip refresh always canonicalizes via `ClientThread` — the previous off-thread `canonicalize` AssertionError crashed the client when selecting a GE item / interacting with the HUD.
- Item tip overlay no longer calls ItemManager at all; GE tip hides during bank PIN; offer handler catches Throwable so PIN/GE open can’t take down the client.

### Notes
- Plugin Hub compliance (for a future public build): third-party network calls must default off with the IP-disclosure warning, and the disk-stored sign-in / local link server would need replacing with a pasted API key. This build targets self-distribution via Runetick; those changes are the "Plan B" Hub fork.
- Initial RuneLite plugin scaffold (Gradle wrapper, JDK 11 target), based on the official `runelite/example-plugin` template.
- Runetick sidebar panel with Connect / Disconnect. Connect opens the Runetick `/link` page in the browser, which hands the player's existing (Firebase SSO) sign-in to the plugin over a one-shot `127.0.0.1` listener (`LinkServer`).
- `FirebaseSession`: exchanges the stored refresh token for ID tokens, refreshing them as they expire. The sign-in is kept in `~/.runelite/runetick/session.json` (owner-only), not in RuneLite config, so it is never part of a synced profile.
- `RuneTickApi` + `OfferSync`: Grand Exchange offers that match a flip placed in Runetick update its bought/sold counts (`PATCH /v1/me/placed/:id`); other finished buys and sells are logged once to the trade log (`POST /v1/me/trades`).
- `OfferTracker`: drops empty slots and repeated updates (e.g. the re-send on login).
- `ErrorLog`: exceptions are appended to `~/.runelite/runetick/error.log`.
- Settings: update placed flips, log other trades, Runetick address (for testing against a local copy).
- Unit tests (`./gradlew test`); `./gradlew run` launches a dev client with the plugin loaded.

### Removed
- Generic "POST offers to any endpoint with an API key" export from the first draft, replaced by the Runetick integration above.
