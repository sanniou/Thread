# Changelog

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
