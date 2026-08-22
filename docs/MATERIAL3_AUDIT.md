# Auralis Material 3 Baseline Audit

## Purpose

This audit establishes the native Android baseline for the Auralis Material 3 experience transformation. It records observed implementation seams and preserves the behavioral boundaries that visual work must not alter.

## Observed implementation baseline

| Surface | Observed implementation | Material 3 implication |
|---|---|---|
| Theme | `Theme.Auralis` inherits from `Theme.Material3Expressive.DynamicColors.DayNight`; Auralis also supplies explicit Material color roles and surface-container tiers. | The work is a consistency and hierarchy transformation, not a dependency migration. |
| Shared shell | `fragment_main.xml` uses a coordinator shell with an explore host, mini-player, expanded playback sheet, and queue sheet. | Navigation and playback hierarchy must be redesigned as one surface system without moving player ownership. |
| Home | `fragment_home.xml` uses a custom toolbar, scrollable tabs, and a ViewPager. `HomeFragment` switches Music and Audiobooks through `HomeViewModel`. | Compact navigation must make the active library obvious while preserving independent restored domain state. |
| Playback | The shared player has compact and expanded surfaces. The expanded panel conditionally exposes domain-specific actions. | The same visual grammar must not create mixed Music/Audiobooks actions or queues. |
| Settings | Shared preferences and mode-specific preference screens already exist. | The presentation must clarify shared, Music, Audiobooks, and local-library choices rather than merge them. |
| Launcher | The adaptive icon references a detailed raster foreground as both foreground and monochrome layer. | Replace it with original vector-first adaptive foreground, background, and one-color monochrome assets. |

## High-priority experience seams

The first visual milestone will concentrate on the following seams because they establish the hierarchy seen on every listening session:

1. **App shell and active-domain orientation.** The listener must know whether they are browsing Music or Audiobooks before interacting with tabs, search, or the mini-player.
2. **Top app bar and action density.** Search, settings, domain switching, sort, selection, and overflow must be visually prioritized rather than presented as a collection of equally weighted custom buttons.
3. **Mini-player and sheets.** The compact player, expanded player, and queue must use a consistent Material 3 surface, shape, and motion model while retaining explicit `PlaybackDomain` semantics.
4. **Lists and state surfaces.** Music browsing, Audiobook lifecycle states, indexing, empty states, errors, and long titles require one readable content hierarchy.
5. **Auralis identity.** The launcher, splash, compact brand glyph, widget identity treatment, and About presentation require a single original Material 3-ready symbol that communicates listening plus long-form storytelling.

## Behavior that visual work must preserve

| Invariant | Required preservation rule |
|---|---|
| Local-first media | No streaming, accounts, online catalog, or network media behavior is introduced. |
| PlaybackDomain | Music and Audiobooks remain explicit domains, with independent persisted snapshots and domain-pure queues. |
| Music scope | Music remains oriented around songs, albums, artists, genres, playlists, and Music-specific playback. |
| Audiobooks scope | Audiobooks alone owns long-form progress, chapters, bookmarks, speed, sleep, auto-rewind, and local folder organization. |
| Shared playback infrastructure | One service/player/notification/widget remains permitted; visual work must not change its ownership or snapshot contracts. |
| Attribution | GPL and upstream provenance remain intact. The visual system must be Auralis-specific and original. |

## Phase-one acceptance checklist

The first implementation milestone can proceed only after the shared visual contract defines color-role usage, elevation/surface levels, type hierarchy, shape hierarchy, icon treatment, component/action roles, spacing, motion, theme variants, and adaptive launcher requirements. Each rule must map to a real existing surface and must avoid a behavior change unless separately justified and tested.
