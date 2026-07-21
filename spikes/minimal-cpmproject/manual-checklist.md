# S003 editor checklist

> **NON_PRODUCTION** — do not convert these steps into an automatic PASS.

Record OS, Java, CPM mod/editor version, CPM commit/build provenance, date and tester. For M2, M3, M4 and M5:

1. Start the CPM editor without modifying the reference checkout.
2. Open the exact artifact and record any popup, console warning or exception verbatim.
3. Verify the tree: M2 body only; M3 body + `Spike Cube`; M4 body + head; M5 body + cube.
4. For M3/M5, verify the checker texture, UV location and cube dimensions.
5. For M5, select standing, play at least three loops and verify the referenced cube animates.
6. Save As to a new temporary path, close, reopen and repeat steps 3–5.
7. Unzip the saved copy, record entries and normalized JSON diff against the input. Writer-added conservative fields are not loader requirements unless separately demonstrated.
8. Confirm original artifact SHA-256 remains equal to `artifacts/manifest.json`; delete only the manually created temporary copy.

Also try M0/M1 only to confirm the editor surfaces a controlled error; do not infer that a UI error message changes the already observed `ProjectIO` failure.

