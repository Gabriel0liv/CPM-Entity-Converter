# T304 CPM Editor manual checklist

visualValidation: PASS

CPM Editor: `0.6.27`

Pinned CPM source commit used by the automated gate: `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`

Automated technical gate CI run: `33080416007`

Final documentation-only CI run before manual closure: `33080857318`

Use only an artifact whose SHA-256 exactly matches the row below. These hashes supersede the earlier pre-hardening visual fixtures.

| Fixture | SHA-256 | Open | Texture/UV | Hierarchy/bind | Save/reopen | Evidence | Observations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `fixture-a-humanoid` | `cc43050bfd8a189e5bb5e2bf21202a6025f72933b90877df28e264e33985788b` | PASS | PASS | PASS | PASS | user screenshot, 2026-08-27 | Readable humanoid silhouette; torso, head, both arms and both legs remain separated and correctly parented. |
| `fixture-b-neck` | `b2a9b77d772ce44ffe712ba27b77bbf26d957042d7de6e35b39a5694a915b55a` | PASS | PASS | PASS | PASS | user screenshot, 2026-08-27 | Visible `body -> neck -> head -> horn` chain; no flattening or missing descendant. |
| `fixture-c-deep-hierarchy` | `bbbed5f0b287552cb52ce962b8bfc0b11b99bb2ebb3f7127bdb4556a289eb195` | PASS | PASS | PASS | PASS | user screenshots, 2026-08-27 | Deep hierarchy preserved; `accessory#cube-0` inspected with distinct diagnostic texture regions and non-trivial local rotation visible. |
| `fixture-d-quadruped` | `a9f52dca3b965efa64876080de5be906d5f8293c4ba4e841bbb0d31054279824` | PASS | PASS | PASS | PASS | user screenshot, 2026-08-27 | Readable quadruped silhouette with long body, four legs, head and tail; no unexpected transform corruption. |

## Procedure performed

1. The exact redesigned CI evidence bundle was opened in CPM Editor 0.6.27.
2. Vanilla roots, generated hierarchy and element names were inspected for A/B/C/D.
3. Diagnostic silhouettes and placement were inspected: A reads as a humanoid, B preserves the neck/head/horn chain, and D reads as a quadruped.
4. Texture assignment was visually inspected. Fixture C was additionally inspected with `accessory#cube-0` selected; the diagnostic per-face UV regions are visibly distinguishable instead of collapsing to the same source patch.
5. Static pivots/local orientation/bind inheritance were visually checked for obvious offset, scale, flattening or detached-child failures; none were observed.
6. Each redesigned project was saved with **Save As**, closed, reopened, and reported to remain visually unchanged.
7. No CPM Editor warning/error was reported during this manual session.

## Evidence note

The screenshots were supplied by the tester in the project conversation on 2026-08-27. They are manual human evidence bound to the exact redesigned hashes above; the source screenshots are not vendored into the repository.

T304 manual visual acceptance is **PASS** for the exact hashes above.
