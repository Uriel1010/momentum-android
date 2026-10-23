# Momentum 4 visual specification

Audit: the 3.2 interface uses muted green, medium-weight text, repeated rounded containers and standard filter chips. The new identity uses editorial typography and a single lime invitation as its main visual anchor. All domain logic and Room/backup schemas remain unchanged.

Palette: #D7FF3F action; #C9B8FF recovery and accomplishment; #FAF7F2 light canvas; #0F0F10 focus and dark canvas; #E6E6E6 inactive structure. Light-theme text links use charcoal, never lime on white. Accent surfaces use charcoal text. Dynamic system color remains opt-in but action/recovery colors stay semantic.

Typography: Heebo 900 display, 700 titles, 400/500 supporting text. Heebo is bundled under SIL OFL. Rubik was considered as another commercially usable OFL Hebrew family; Heebo retains established Hebrew metrics and gives a tighter editorial silhouette. The additional 900 weight is instantiated from Google Fonts’ Heebo variable font and bundled, with no runtime font service.

Components: solid lime ActionPanel with a small static offset shadow; accessible custom pressable PrimaryAction; outlined segmented choices; compact four-tab navigation; monochrome project rows with real completion bars; lavender achievement tiles; labelled seven-day activity bars; lavender recovery surface. No random category colors, streak rewards, invented statistics or decorative motion loops.

Now: one expressive heading, one dominant lime recommendation with time/next step/start, quiet alternatives, and an explicit blocker route. Resume context remains a compact entry instead of a competing large card.
Focus: charcoal in either device theme, minimal toolbar, next-action slab and lime completion control. Recovery uses a full lavender sheet with elapsed rest time, continue and finish-for-today actions. The optional focus timer excludes the rest interval.
Projects: bold title, outlined active/paused filters, clear next-step previews and completion fractions derived from current task rows.
Progress: real starts/advances/completions, real seven-day activity, no streak coercion; history remains available. Purple means acknowledgement, lime means action.

Validation: real Compose/device previews for four screens under light and dark system themes, Hebrew RTL and English LTR; Focus intentionally stays charcoal. Large text and reduced motion; device interaction/regression tests; visual review after capture. Native widgets and notifications retain behavior with matching brand colors.
