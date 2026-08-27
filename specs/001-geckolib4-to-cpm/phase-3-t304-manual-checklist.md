# T304 CPM Editor manual checklist

visualValidation: NOT RUN

CPM Editor: `0.6.27`

Pinned CPM source commit used by the automated gate: `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`

Automated gate CI run: `33080416007`

Use only an artifact whose SHA-256 exactly matches the row below. These hashes supersede the earlier pre-hardening visual fixtures.

| Fixture | SHA-256 | Open | Texture/UV | Hierarchy/bind | Save/reopen | Screenshot path | Observations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `fixture-a-humanoid` | `cc43050bfd8a189e5bb5e2bf21202a6025f72933b90877df28e264e33985788b` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-a-humanoid/` | |
| `fixture-b-neck` | `b2a9b77d772ce44ffe712ba27b77bbf26d957042d7de6e35b39a5694a915b55a` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-b-neck/` | |
| `fixture-c-deep-hierarchy` | `bbbed5f0b287552cb52ce962b8bfc0b11b99bb2ebb3f7127bdb4556a289eb195` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-c-deep-hierarchy/` | |
| `fixture-d-quadruped` | `a9f52dca3b965efa64876080de5be906d5f8293c4ba4e841bbb0d31054279824` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-d-quadruped/` | |

## Procedure

1. Obtain the exact CI evidence artifact or regenerate locally and verify its SHA-256 against this checklist before opening it.
2. Open the source `.cpmproject` in CPM Editor 0.6.27.
3. Verify vanilla roots, generated hierarchy and element names; confirm no unexpected flattening, reparenting or missing generated element.
4. Verify the diagnostic silhouette/placement:
   - fixture A must read as a humanoid with separated torso, head, arms and legs;
   - fixture B must visibly preserve the `body -> neck -> head -> horn` chain;
   - fixture D must read as a quadruped with a long body, four legs, head and tail.
5. Verify texture assignment and UV placement. For fixture C, explicitly inspect `accessory#cube-0`: all six faces must exist, use distinct UV rectangles and be visually distinguishable rather than sampling the same patch.
6. Verify static pivots, local orientation and bind pose against the fixture intent; record any visible offset, rotation, scale or inheritance error. Fixture C must retain the non-trivial accessory rotation derived from `[12, 0, 27]`.
7. Use **Save As** to a separate temporary copy. Never replace or overwrite the source artifact used for the hash binding.
8. Close the editor, reopen the saved temporary copy, and repeat hierarchy/bind and texture/UV checks.
9. Record every warning/error literally and attach screenshots under the fixture path shown in the table.
10. Change a row from `NOT RUN` only after that exact check was actually performed on the exact hashed artifact.

T304 must remain `[~]` while any required visual row remains `NOT RUN` or has unresolved observations.
