# Changelog

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
