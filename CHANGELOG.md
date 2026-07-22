# Changelog

## 0.33.0 - 2026-07-22

Quiet product consistency: full-page loading surfaces share `ThreadLoadingState`.

- App workspace restore, Social/Article detail, User, Source Manager, Subscription paging, Reader list, and Inbox initial refresh use the shared loading title/message instead of bare `CircularProgressIndicator`.
- Inline overlays, append footers, and image preview spinners remain compact indicators (not full-page states).
- Desktop compile + jvmTest matrix green after the adoption pass.

## 0.32.0 - 2026-07-22

Release-candidate Desktop gates after the 0.31 i18n completion wave.

- Re-verified jvmTest + compileKotlinJvm matrix: core-domain, core-data, core-ui, composeApp.
- `createReleaseDistributable` wrote main-release app image under `composeApp/build/compose/binaries/main-release/app`.
- Launcher `--smoke-check` passed: `Thread Desktop offline startup probe passed: discourse, nmb, tieba` (exit 0; incidental SLF4J / pure-virtual noise ignored).
- No product-code delta beyond 0.31 tip; this release marks the RC gate set green on current main.
- Annotated tag `v0.32.0` pushed to origin (points at RC docs tip).

## 0.31.0 - 2026-07-22

i18n completion wave: non-composable `getString` + residual secondary pages, semantics, and filter labels.

- **ViewModel / side-effect**: snackbar, notification, and status messages resolve via suspend `getString(Res.string.*)` (or `screenModelScope.launch` / `LaunchedEffect` bridges) across composeApp, forum, reader, and feed.
- **AppError**: `messageRes` + `localizedMessage()`; `Throwable.toAppError` maps failure kinds to core-ui string resources instead of hard-coded Chinese.
- **Secondary UI**: Command Palette, Operations, Global Search, History filters, Search type labels, Emoticon group titles, SyncSettings, Desktop attachment/notification strings, and semantics content descriptions resourceized; `stringResource` hoisted out of `semantics {}` / non-composable lambdas.
- **Resources**: new zh/en keys for VM and secondary paths; English catalog cleaned so values-en no longer contains Chinese literals.
- Desktop gates: compileKotlinJvm (core-ui / forum / reader / feed / composeApp) and jvmTest (core-domain / core-data / core-ui / composeApp).

## 0.30.0 - 2026-07-22

i18n foundation: composeResources for every product shell/feature + composable UI string migration.

- **composeApp / feature-reader / feature-feed**: first-class `composeResources/values` + `values-en` with product strings; feed already depended on compose resources and now ships its own catalog.
- **feature-forum / core-ui**: expanded zh/en catalogs (legacy keys retained; auto-migrated composable UI strings appended).
- Migrated high-traffic `@Composable` UI copy to `stringResource(Res.string.*)` across shell, Reader, Feed, Forum and core-ui widgets (filters, empty states, banners, dialogs, nav labels).
- Non-composable side effects (ViewModel messages, `LaunchedEffect`/`scope.launch` snackbars, semantics-only a11y strings, notification bridge) intentionally keep Kotlin literals for now — Compose Resources `stringResource` is composable-only; full suspend `getString` plumbing is a follow-up.
- Desktop gates: compileKotlinJvm (core-ui / forum / reader / feed / composeApp) and jvmTest (domain / data / ui / composeApp).

## 0.29.0 - 2026-07-22

Product polish wave (0.26–0.29): de-console copy, cache story, empty workspace and quiet reading motion.

- **0.26 De-console**: human-case Chinese eyebrows on forum secondary pages; residual Material buttons moved to the `SaniouButton` family (Post dice insert, Bookmark edit, ZoomImage, list cache CTA).
- **0.26 i18n**: forum product strings for eyebrows / empty workspace / cache banner; `values-en` added for `feature-forum` and `core-ui` so English locale falls back cleanly.
- **0.27 Cache story**: Forum Channel shows `CacheStatusBanner` with retry + “查看健康”; session destination updates let App shell navigate to Operations without feature→shell coupling.
- **0.28 Empty workspace**: no forum sources → add source / import backup CTAs; Source Manager empty state; Reader empty state offers add-subscription; Feed empty explains missing sources and refresh.
- **0.29 Reading rhythm**: article detail quieter dividers and denser reading spacing; Reader list-detail preview enters with `threadTweenSpec`; topic cards honor soft content-size motion.
- Desktop gates: compileKotlinJvm (core-ui / forum / reader / feed / composeApp) and jvmTest (domain / data / ui / composeApp).

## 0.25.0 - 2026-07-22

Experience deep-dive second wave: forum secondary paths, cache visualization and quiet motion.

- Forum dialogs and secondary CTAs (manual login, page jump, source manager, subscription ID, reference sheet, user retry, topic empty refresh) use the shared `SaniouButton` family.
- Reader surfaces the same cache-status banner as Unified Feed (refreshing / stale / cached) without clearing the article list.
- History and Global Search empty/loading states reuse `ModernEmptyState` / `ThreadLoadingState`; search eyebrow is human-case Chinese.
- Channel drawer expand, Post success overlay and cache banner enter/exit honor `threadTweenSpec` / reducedMotion.
- Desktop gates cover compile and jvmTest on the UX surface.

## 0.24.0 - 2026-07-21

Experience deep-dive for main user paths: buttons, detail reading rhythm, list hierarchy and shared feedback.

- Settings, Operations, Activity Center, Inbox, Reader transfer dialogs and Add Feed sheet use the shared `SaniouButton` family, including loading and destructive confirmation.
- Product command confirmation in App routes destructive actions through `SaniouDangerButton`.
- Article detail gains a bottom `UnifiedActionBar` for bookmark / share / browser while keeping top-bar font controls; reading copy uses human-case eyebrows and density tokens.
- Social detail, Reader list cards and paging append/refresh banners align retry actions and spacing with the design system.
- `ArticleItem` tightens title/summary/meta hierarchy with density-aware thumbnails and quieter unread markers.
- Desktop gates cover compile and jvmTest on the UX surface.

## 0.23.0 - 2026-07-21

Quiet product UX polish for Desktop: motion, buttons, feed refresh and cache clarity.

- Motion tokens in `core-ui` honor `reducedMotion` with short fades/rises instead of bounce-heavy animation.
- Unified button family (`SaniouButton` / Outlined / Text / Tonal / Danger) supports loading state and shared min height.
- `ThreadCard` and page headers follow density-aware spacing; eyebrow copy stays human case.
- Unified Feed shows a cache-status banner (cached / refreshing / stale) and pull-to-refresh without clearing the timeline.
- Refresh diagnostics and error states use the shared button system; failure attempt labels are localized.
- Desktop gates cover compile and jvmTest on the UX surface.

## 0.22.0 - 2026-07-21

System-entry bridge release for Desktop: share, deep links, notifications and portable files.

- Platform contracts live in `core-ui` (`ShareService`, `AppEntryController`, `UserDataFileService`, `SystemNotificationService`, `BackgroundRefreshBridge`) so feature modules stay free of shell dependencies.
- Desktop share falls back to the system clipboard; Article and Social detail pages share through the same boundary.
- Launch args accept `thread://…`, `http(s)://…` and local `.json/.txt` user-data files; App consumes the entry bus and routes through the cache-safe content resolver.
- Workspace deep links `thread://inbox` and `thread://feed` open the matching primary destinations; notification clicks re-enter the same bus.
- Desktop tray notifications fire when Inbox unread increases, with a click payload that returns to the Inbox.
- Settings can import/export portable user-data via OS file dialogs in addition to the existing paste/copy dialog.
- Background refresh keeps Reader due-feeds and Social newer timelines on a 15-minute lifecycle bridge owned by the live App DI graph.
- Desktop gates cover compile, jvmTest and the existing release/smoke path.

## 0.21.0 - 2026-07-21

Production Social runtime, content-graph and unified discovery release.

- ActivityPub/Mastodon-compatible Social connector authenticates with a local token, refreshes bidirectional home timelines, and supports like/repost/bookmark interactions without provider-specific UI branches.
- Database schema v6 adds Social sources/posts/cursors, a durable content-graph edge table, and a materialized smart-collection index so related content and saved views page from SQL instead of ad-hoc Kotlin scans.
- Unified Feed can include Social sources beside Forum and Reader items; social cards open an in-app detail page with media, interactions, share/copy and related content.
- `thread://social/{sourceId}/{postId}` deep links, Inbox notifications, global search and content-link resolution all route Social posts in-app when the cache has them, and fall back safely when they do not.
- Smart collections keep a recency-bounded catalog of 50 views and resolve Forum/Comment/Reader/Social caches through one indexed SQL path.
- Desktop gates cover schema v6 migration, offline Social search, concurrent collection bounds, product-graph discovery and `:composeApp:compileKotlinJvm`.

## 0.20.0 - 2026-07-17

Unified content-routing, notification-inbox and portable-appearance release.

- `ContentReference` gives Forum topics/comments, Reader articles, future Social posts and external URLs one source-aware routing contract. Rich text delegates links to the root cache-safe resolver; unsupported protocols never reach a platform launcher.
- Database schema v5 adds a durable Inbox event log and source mute preferences. Forum announcements and newly cached Reader articles feed the same SQLDelight `QueryPagingSource` timeline with live totals, unread/source counts, search, filters, batch read, mute and delete actions.
- Inbox replaces History in primary navigation while History remains available from the command palette. The adaptive page uses the same common Compose layout and Paging collector on Android, iOS and Desktop.
- Cross-source smart collections persist source, author, tag, query, unread, bookmark, media and content-kind rules. Discovery can execute a saved collection against bounded Forum/Comment/Reader caches and route every result back to its original context.
- Theme mode, interface density, font scale, reduced motion, Reader width and line height are live portable preferences. Reader consumes the shared typography tokens, and user-data bundle v2 carries appearance and smart collections through local/WebDAV transfer.
- A deterministic fixture Social runtime supports opaque bidirectional cursors, capability validation, content warnings, bounded media and idempotent interactions without adding provider-specific UI.
- Desktop gates cover schema v5 migration/indexes, 240-event Inbox behavior, inherited mute state, concurrent bounded collection writes, saved-view resolution and a 154-post Social timeline.

## 0.19.0 - 2026-07-17

Unified activity, explicit identity and serialized-action release.

- The low-value Lab primary destination is replaced by an adaptive Activity Center. The nine stable workspaces now lead with Forum/Reader/Feed and provide a first-class queue for refreshes, authentication, drafts, transfers and source lifecycle events.
- Source identity is a credential-free, persistent domain fact with anonymous, valid, expired, disabled and not-applicable states. Successful login/refresh and classified authentication failure update the same state instead of leaving pages to infer validity from error strings.
- A single `ProductActionExecutor` owns source refresh, Reader refresh, connector enable/disable, diagnostic export, Reader JSON/OPML transfer, user-data export/import, WebDAV backup/restore and draft deletion. Conflict keys reject overlapping work while unrelated sources remain concurrent.
- Action history survives process death, redacts failure text and converts interrupted running work into an explicit retryable activity on restart. Dangerous command-palette and Activity Center actions require confirmation.
- Forum drafts maintain a bounded observable index, appear in the Activity Center and command palette, and deep-link back into the correct composer. Drafts whose source was removed remain preserved but no longer expose an invalid navigation action.
- Reader, Settings, Operations and the global command palette now execute through the same action boundary. Dynamic commands include both Reader formats, local user-data transfer, WebDAV operations, draft resume/discard, identity recovery and activity navigation.
- A platform-neutral Social/stream contract fixes opaque bidirectional cursors, author identity, interaction capabilities, content warnings and bounded media grids without naming a provider in common UI.
- Desktop tests cover explicit identity expiry, action conflict rejection, interrupted-action restart recovery, orphan draft fallback, a 154-item mixed activity seed and the Social connector contract.

## 0.18.0 - 2026-07-17

Continuous workspace, durable operations and safe-diagnostics release.

- Workspace session v2 restores Forum source/channel/group state, Reader source/filter/query state, Feed source selection, and stable list anchors across process restarts. The v1 payload remains readable and partial writers still merge under one mutex.
- Topic and article details record a source-aware continuation reference. Startup validates that reference directly against SQLDelight before rebuilding the detail route, so deleted content falls back to its workspace without a recovery network call.
- Forum composer drafts are independently versioned per source and channel/topic, debounce-save text/options/attachments, survive restart, and expose an explicit discard action. Successful publication removes only its own draft.
- Refresh outcomes now retain last success, consecutive failures, cache age, failure class and rate-limit recovery across process death. Volatile retries and durable history feed the same source-health projection.
- The command palette combines workspace navigation and cached search with capability-driven actions for per-source refresh, connector enable/disable, authentication recovery, all-Reader sync and safe diagnostic export.
- Operations can preview and copy a redacted JSON diagnostic. Credential-shaped values are removed before persistence and export never reads account values, connector options, content bodies or the local absolute storage path.
- Product tests cover v1-to-v2 session migration, cache-only detail restoration, persistent refresh recovery, diagnostic leak prevention, draft attachment round trips, dynamic-command availability and a 3,600-row mixed offline search seed.

## 0.17.0 - 2026-07-17

Offline discovery, session continuity and source-operations release.

- The adaptive shell now exposes nine stable workspaces: Forum, Reader, Feed, Discovery, Bookmarks, History, Operations, Lab and Settings. Touch navigation and Ctrl/Command+1–9 resolve through the same destination model.
- Ctrl/Command+Shift+P opens a global command palette with keyboard selection, shortcut discovery, workspace routing and immediate cached-content results.
- A new offline discovery repository searches SQLDelight Topic, Comment and Article caches concurrently, preserves source identity and routes results back to forum topics or Reader articles without network access.
- Versioned workspace sessions restore the last workspace, forum source and global query. Mutex-protected partial updates prevent navigation, source and search writers from overwriting one another, while detail stacks remain stable after settings writes.
- The source operations workspace combines live forum registrations, Reader subscriptions, cache counts and refresh diagnostics into ready, disabled, refreshing, offline, authentication, rate-limit and degraded states with source-local retry and diagnostic clearing.
- Desktop stores its database in the application data directory instead of the launch working directory. A first-run upgrader copies a legacy database and WAL/SHM sidecars only when no target database exists.
- A product-graph JVM integration test covers all three search result types, health aggregation, authentication degradation, diagnostic clearing and concurrent session restoration; Desktop database-location tests prevent destructive upgrade regressions.

## 0.16.0 - 2026-07-17

Release-candidate interaction and bounded-performance release.

- Workspace destinations expose Ctrl/Command+1–9, feature sidebars expose Ctrl/Command+B, and Forum/Reader/Feed add contextual refresh, search, jump and scroll commands without intercepting ordinary text input.
- Focus ownership, pane semantics, selected-state descriptions, 48dp sidebar targets, IME search actions and Escape handling are centralized in the common UI shell and adaptive modal/detail boundaries.
- Reader retains an independent scroll position for every source/filter pair; Feed does the same for each source set, and both use stable Paging item keys so refreshes do not rebuild visible rows or move the viewport.
- Generic Forum navigation, subscriptions and image lookup now carry opaque String topic IDs end to end. Numeric compatibility constructors and lossy `toLong()` routing are removed for future connectors.
- Database schema v4 adds release query indexes for Reader articles, subscriptions, comments and topics. User-content cache queries include source identity, and deterministic query-plan tests prevent index regressions.
- Every production Pager uses one bounded common policy: two initial pages, half-page prefetch, no placeholders and a ten-page maximum window. This is the shared SQLDelight 2.3.2 + AndroidX Paging 3.4.1 policy on Android, iOS and Desktop.
- Material/Compose, kotlinx-datetime, XML, URL and coroutine deprecations are removed or isolated behind one cross-platform boundary; malformed bookmark rows now fail with field-specific diagnostics instead of null assertions.
- Desktop remains the sole release gate for this phase and verifies tests, compilation, isolated startup smoke and the distributable runtime image.
- The Linux release image joins its runtime classpath into one application JAR and explicitly includes the jdeps-required JDBC/HTTP/management modules, preventing long-classpath launcher corruption while preserving reflection-heavy WebView and connector code.
- Consolidated release service descriptors retain all Ktor engines, Sketch GIF/WebP/SVG/resource/network components and both XML serialization providers after the one-JAR merge; a JVM invariant prevents silent provider loss.

## 0.15.0 - 2026-07-16

Offline contract and paging-reliability release.

- Production Ktor clients can run against an injected engine; checked-in Discourse exchanges replay success, expired authentication, rate limits, oversized uploads and partial service failure without contacting a live forum.
- Non-success HTTP responses retain status, bounded response context and `Retry-After` metadata in one `HttpStatusException`, so retry policy, diagnostics and UI classify the same failure.
- Subscription storage is now its own ordered listing and uses SQLDelight's official `QueryPagingSource`; it no longer depends on unrelated channel `TopicListing` rows or infers remote pages from row counts.
- The unused home-grown offset/page/keyset PagingSource implementations and an unregistered Wire response-converter placeholder are deleted, leaving SQLDelight 2.3.2 plus AndroidX Paging 3.4.1 as the only pagination implementation.
- Database invariants cover failed-refresh rollback, atomic row/key refresh, subscription cache paging, recursive comments, image replacement and dynamic-source namespace deletion across a real Desktop restart.
- The packaged `--smoke-check` path now creates an isolated in-memory seed and resolves cached Forum and Reader content through the real DI graph without network or user-database mutation.
- Historical NMB names on generic Forum/image modules, the obsolete numeric ViewModel factory and the unused greeting-image feature chain are removed.
- The Desktop gate runs domain/data/UI/combination tests plus the full `composeApp` JVM compile; Android/iOS remain outside this phase as requested.

## 0.14.0 - 2026-07-16

Reliable reading-state and detail-workflow release.

- Paging presentation is now a tested policy: cached rows always remain readable during refresh failure, while initial failures, empty results and tail failures use distinct common states.
- Forum, Reader, Feed, bookmarks, subscriptions, search and user activity share one append-loading/error/end component instead of feature-local indicators.
- Refresh diagnostics identify offline and authentication failures, preserve successful sources, expose expandable per-source details and provide an in-context retry command.
- Topic lists/details, trends, article details, source initialization and Web login use the same adaptive detail hierarchy; the complete legacy top-app-bar family is deleted.
- Reader original-page mode now renders real URL or HTML content through the existing Compose Multiplatform WebView API instead of a placeholder.
- The unused parallel LoadableState/error/loading stack, dead capture infrastructure, commented image sample and non-functional screenshot action are removed.
- `:core-ui:jvmTest` covers cache-priority and error-vocabulary policy; `:composeApp:compileKotlinJvm` remains the Desktop product gate.

## 0.13.0 - 2026-07-16

Adaptive workflow completion release.

- Search, subscriptions, accounts, user activity, posting and source management now share one responsive secondary-workflow scaffold instead of unrelated mobile top bars.
- A common command surface drives search, filtering and bulk-selection layouts; bookmarks and history use the same responsive controls and live context metrics.
- Forum and Reader modal tasks switch between compact bottom sheets and wide focused dialogs for source editing, login, references, sub-comments, subscription IDs and data transfer.
- The composer uses a centered reading-width canvas, IME-aware content insets and a persistent editing toolbar without introducing platform-specific UI branches.
- Sync settings stack credentials on compact windows, use wrapping action groups and expose full-size portable JSON editors on larger windows.
- Unreachable legacy forum-list/list-detail experiments and the obsolete dedicated search app bar are deleted.
- Desktop JDK 21 `:composeApp:compileKotlinJvm` is the single verification gate for this release.

## 0.12.0 - 2026-07-16

Cross-platform adaptive workspace release.

- SQLDelight 2.3.2 and AndroidX Paging 3.4.1 are the explicit shared pagination baseline for Android, iOS and Desktop; obsolete Cash Paging catalog aliases are removed.
- A single common window capability model replaces page-local width checks and drives compact, medium, expanded and large layouts.
- Global navigation now becomes a bottom bar with an overflow sheet on phones, a compact rail on medium windows and a labelled workspace rail on desktop.
- Forum, Reader and Feed share one adaptive feature-sidebar scaffold; the old injected global drawer hierarchy and its unused components are deleted.
- Reader gains a large-window three-pane flow with inline article preview, responsive image summaries, search hero and a dedicated immersive reading canvas.
- Forum topic cards, timeline surfaces and thread detail share responsive padding, tag wrapping and the same focused reading canvas.
- `:composeApp:compileKotlinJvm` passes under JDK 21 as the single Desktop verification gate for this phase.

## 0.11.0 - 2026-07-16

Desktop workspace redesign release.

- A common Material 3 design system now defines the quiet indigo/cyan palette, typography, shape scale, spacing and responsive width tokens.
- Desktop uses a persistent global workspace rail plus feature sidebars for Forum, Reader and the unified timeline; compact windows retain a shared drawer.
- Forum timelines, trends, thread detail, Reader, article detail, bookmarks, history and sync settings now share centered content canvases and outlined surfaces.
- Empty, loading, diagnostics, page-header and navigation patterns are reusable `commonMain` components instead of page-specific mobile layouts.
- The Desktop window opens at a productive workspace size while keeping native window controls and package metadata.

## 0.10.0 - 2026-07-16

Forum depth and offline-readiness release.

- Tieba and Discourse image attachments now use real source upload protocols before publishing.
- Recent channel visits, bookmark tag intersections and card-level tag filtering are common flows.
- Reference images and sub-comment previews persist across Desktop restarts.
- Cached paging content remains usable during refresh and source failures, with an inline retry path.
- Desktop runtime images carry application metadata and are launch-tested with a headless startup probe.
- Database schema v3 adds source-local recent channel visits without discarding existing reader state.

## 0.9.0 - 2026-07-16

Desktop-first product baseline before the first public release.

- Runtime source catalog with built-in NMB/Tieba and multi-instance Discourse connectors.
- Unified forum, Reader and cross-source feed navigation with observable refresh diagnostics.
- Versioned local/WebDAV backup and restore for source descriptors, subscriptions, bookmarks,
  article state and user settings.
- Reader auto-refresh scheduling, cache policy, JSON/OPML import/export and RSS/Atom parsing.
- Source capability conformance gate and Desktop attachment picker for supported posting flows.
- JDK 21 Desktop CI covering composition smoke, database migration, domain/data tests and Debian
  package generation.
