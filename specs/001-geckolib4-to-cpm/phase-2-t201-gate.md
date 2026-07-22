# Phase 2 T201 Gate

Date: 2026-07-22
Commit base: ee3cc5c2e5b408297c59776820043e6bc6c4a274
Implementation/evidence commit: bb60053bd97db09c976e5845961553fe4dd2c89d
Gate record commit: this commit
Independent review: review/t201-final-acceptance-v2 — PASS
Workflow run: 29946604478
Workflow HEAD: 07454937481501f735602a9c5264d1dbfb512d92
Ubuntu: PASS
Windows: PASS

## Acceptance evidence

- PNG success/failure and logical-path tests cover valid files, malformed
  signatures, missing files, hash/byte preservation and the local limits
  `maxBytes`, `maxWidth`, `maxHeight` and `maxPixels`.
- PNG diagnostics expose logical pointers and structured limit context.
- Box and per-face UV retain signed/fractional values, canonical face ordering
  and bounds warnings without clamping.
- Static assembly A–D is exercised with one texture, empty clips, validator
  execution and unchanged PNG bytes.
- Test-only `StaticModelSnapshot` compares complete expected trees for all four
  fixtures, including source, geometry, roots, textures, bones, bind transforms,
  provenance, cubes and box/per-face UV.
- Manifest check, S004 audit, reproducible build and GeckoLib regression pass;
  oracle result is 41/41 assertions with clips A/B `idle,walk`, C `idle`, D
  `walk`.

T201: **[x] PASS**

Deferred to T202: animation clips, tracks, keyframes and playback.
Deferred to T203: easing, Molang and related diagnostics.
Deferred to T204: hostile/fuzz matrix, differential oracle and broader limits.
Deferred to T300/T700: CPM projection, output and broad filesystem/locale matrix.
