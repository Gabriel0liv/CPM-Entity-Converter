# T304 — checklist humano de conformidade estática

Status inicial: **NOT RUN**. Preencher somente durante uma sessão humana real
no editor CPM 0.6.27.

Tester:  
Date:  
Operating system:  
Java:  
CPM version: 0.6.27  
CPM commit: `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`  
Evidence root: `build/t304/manual-evidence/`

| Fixture | SHA-256 | Open | Texture/UV | Hierarchy/bind | Save/reopen | Screenshot/evidence path | Observed |
|---|---|---|---|---|---|---|---|
| A | `31fa2370af8586d2617dba955aadbfa4f52329dc61597f47609f1f6fda2b7d97` | NOT RUN | NOT RUN | NOT RUN | NOT RUN |  |  |
| B | `4390f540b001bc81f338984875b74f384f6bb0ad26f7f8972c31df4df4245da9` | NOT RUN | NOT RUN | NOT RUN | NOT RUN |  |  |
| C | `177d2f339e3877d18fa000b7ed122080e4f9af4598886ff908ca82e1c36336e3` | NOT RUN | NOT RUN | NOT RUN | NOT RUN |  |  |
| D | `82384684919efc06c4305115734a23ece90b612feae1dacb3a058fa164113695` | NOT RUN | NOT RUN | NOT RUN | NOT RUN |  |  |

## Procedure

1. Open each artifact from the bundle without modifying the source copy.
2. Confirm roots, hierarchy, element names, texture, UV, pivots and static
   orientation against `projectio-report.json`.
3. Use **Save As** to a temporary copy, close, reopen and repeat the checks.
4. Record literal popups/warnings/exceptions and attach screenshots under the
   corresponding `screenshots/<fixture>/` directory.
5. Leave all `Observed` and evidence fields blank until actually performed.

Visual validation is **NOT RUN** in this automated execution.
