# Changelog

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
