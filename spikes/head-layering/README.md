# Head layering spike

> **NON_PRODUCTION** — S001/S002 disposable evidence. Nothing in this directory is converter production code.

This spike compares the same authorial `body -> neck -> head -> horn` fixture as:

- `single-anchor`: the hierarchy remains below the CPM `body` root;
- `root-partition`: `body -> neck` and `head -> horn` are independent CPM root branches, with the neutral head transform rebaked.

Run `python scripts/generate_and_analyze.py`. The generated projects are deterministic and are intended for structural/oracle checks plus the exact manual checks in `manual-checklist.md`.

