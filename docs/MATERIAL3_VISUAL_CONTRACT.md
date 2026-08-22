# Auralis Material 3 Visual Contract

## Product expression

Auralis is one local listening space with two deliberate contexts. **Music** should feel immediate, energetic, and collection-oriented. **Audiobooks** should feel calm, progress-aware, and long-form. The app shell, type, spacing, motion, colors, and brand mark are shared; library structures, actions, and listening tools are purpose-specific.

## Component-role matrix

| Role | Material 3 treatment | Auralis rule |
|---|---|---|
| Screen canvas | `colorSurface` | Keep the primary library canvas open. Do not wrap ordinary lists in cards. |
| Grouped content | `colorSurfaceContainerLow` or `colorSurfaceContainer` | Reserve tonal grouping for status, resume, actions, or deliberate collections. |
| Elevated transient content | `colorSurfaceContainerHigh` or `colorSurfaceContainerHighest` | Apply to mini-player, sheets, dialogs, and focused controls only. |
| Primary action | Filled or tonal button/icon button | Exactly one dominant action per context: Play/Shuffle for Music, Resume/Start for Audiobooks. |
| Secondary action | Text, outlined, or transparent icon button | Contextual actions must not compete with the primary action. |
| Navigational state | Material 3 selected container and label emphasis | The active Music/Audiobooks destination must be evident without depending only on color. |
| Status and progress | Primary/secondary container plus readable supporting text | Audiobook completion and current listening state must be readable at a glance. |
| Error and maintenance | Error roles, plain-language copy, one recovery action | Indexing and local-media errors must remain informative but visually contained. |

## Spatial and type rhythm

| Token | Contract |
|---|---|
| Compact control target | At least 48 dp in both dimensions. |
| List row rhythm | Use a consistent 8 dp increment and reserve generous start/end padding for titles and overflow actions. |
| Page margin | Use the existing medium spacing token as the default horizontal screen inset. |
| Section gap | Separate library groupings with one deliberate large gap rather than nested card margins. |
| Display type | Use only for the expanded player, major detail heroes, or a single page headline. |
| Title type | Use for library/section titles and media titles. Never substitute a label weight for primary media identity. |
| Supporting type | Use body/label roles for artist, author, progress, time, and state metadata; preserve contrast against every supported theme. |

## Motion and feedback

Material 3 shared-axis transitions remain appropriate for screen-level navigation. Playback sheet expansion, queue movement, selection entry, list presses, and domain changes must use short state-oriented transitions only. Each interaction needs a visible pressed/selected/loading/finished result; no motion may delay a listening action or make a playback-domain change ambiguous.

## Original Auralis icon contract

The Auralis symbol is an original geometric **open listening page**. Its two page arcs create a central aperture that implies a right-facing play/listening form. Three simple vertical rounded rhythm marks may appear inside the aperture in full-color artwork, but the symbol must remain recognizable when those marks are removed for monochrome themed icons.

| Asset | Required construction |
|---|---|
| Adaptive background | A solid deep indigo/navy Material-compatible field with no text, texture, or shadow. |
| Adaptive foreground | A vector-first teal/aqua open-page/play silhouette with generous system safe-zone padding. Warm amber is optional only in full-color artwork and must not carry meaning alone. |
| Monochrome | A single-color version of the foreground symbol with no background, gradients, or multi-color details. |
| Splash mark | Center the foreground symbol on the same quiet brand background; do not turn it into a full-screen illustration. |
| In-app glyph | Use only at small scale in About, branding moments, or non-content placeholder states; do not replace artwork or playback state icons. |

## Accessibility and theme gates

The system must retain readable contrast in light, dark, black, dynamic, and fixed-accent themes. Active state always combines color with an icon, weight, label, shape, or content change. No brand asset is allowed to depend on a transparent background or a launcher-specific mask. Text and touch target requirements take precedence over expressive styling.
