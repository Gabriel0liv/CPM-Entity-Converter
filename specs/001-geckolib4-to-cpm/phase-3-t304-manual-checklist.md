# T304 CPM Editor manual checklist

visualValidation: NOT RUN

CPM Editor: `0.6.27`

Pinned CPM source commit used by the automated gate: `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`

Automated gate CI run: `33061294958`

Use only an artifact whose SHA-256 exactly matches the row below.

| Fixture | SHA-256 | Open | Texture/UV | Hierarchy/bind | Save/reopen | Screenshot path | Observations |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `fixture-a-humanoid` | `6657fb5751841e84548a3646fea30b1d601feccbca9676099ee699c27466365a` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-a-humanoid/` | |
| `fixture-b-neck` | `3a2967955e00cc2a4a8c43cc48c0bf69829794e94d5566350ba9e3ea4ba87290` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-b-neck/` | |
| `fixture-c-deep-hierarchy` | `85e1eaa9f49d299a271d295e0c5200a1bf93e85e1128d156c1d23c3dad560dbe` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-c-deep-hierarchy/` | |
| `fixture-d-quadruped` | `badc7e44e6b81a1347c6a23af6d858a3129f23e1e35b30ce91e886db49a71625` | NOT RUN | NOT RUN | NOT RUN | NOT RUN | `screenshots/fixture-d-quadruped/` | |

## Procedure

1. Obtain the exact CI evidence artifact or regenerate locally and verify its SHA-256 against this checklist before opening it.
2. Open the source `.cpmproject` in CPM Editor 0.6.27.
3. Verify vanilla roots, generated hierarchy and element names; confirm no unexpected flattening, reparenting or missing generated element.
4. Verify texture assignment and UV placement. For fixture C, explicitly inspect the per-face UV accessory path.
5. Verify static pivots, local orientation and bind pose against the fixture intent; record any visible offset, rotation, scale or inheritance error.
6. Use **Save As** to a separate temporary copy. Never replace or overwrite the source artifact used for the hash binding.
7. Close the editor, reopen the saved temporary copy, and repeat hierarchy/bind and texture/UV checks.
8. Record every warning/error literally and attach screenshots under the fixture path shown in the table.
9. Change a row from `NOT RUN` only after that exact check was actually performed on the exact hashed artifact.

T304 must remain `[~]` while any required visual row remains `NOT RUN` or has unresolved observations.
