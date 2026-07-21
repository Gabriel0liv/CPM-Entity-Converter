# CPM Entity Converter

Spec-driven project for converting Minecraft entity models authored for
GeckoLib 4.4.9 into editable Custom Player Models `.cpmproject` files.

## Current state

The project is in pre-production discovery and spike work. The approved target
is Minecraft Java 1.20.1, Forge, GeckoLib 4.4.9, geometry format 1.12.0 and CPM
project format V1. No production converter is implemented yet.

- Normative documentation: [`docs/`](docs/)
- Feature specification: [`specs/001-geckolib4-to-cpm/`](specs/001-geckolib4-to-cpm/)
- Disposable experiments: [`spikes/`](spikes/)
- Compatibility matrix: [`docs/compatibility.md`](docs/compatibility.md)

`CustomPlayerModels/` is a read-only reference checkout outside this repository
and must never be modified by project work.

## License

CPM Entity Converter is licensed under the MIT License. See [`LICENSE`](LICENSE)
and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
