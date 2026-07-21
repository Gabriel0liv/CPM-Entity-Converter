# Third-party notices

This repository currently contains documentation and disposable, non-production
spikes. No upstream production source code or third-party mod assets are copied
into the project.

## Custom Player Models

- Project: Custom Player Models
- Upstream: https://github.com/tom5454/CustomPlayerModels
- Commit examined and used as the CPM oracle: `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`
- Version reported by the examined core project: `0.6.27`
- Copyright: © 2021 tom5454
- License: MIT
- Use in this repository: format research, behavioral oracle and compatibility
  tests. The upstream source remains in the separate reference checkout and is
  not copied here.

## GeckoLib

- Project: GeckoLib
- Upstream: https://github.com/bernie-g/geckolib
- Normative commit examined: `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`
- Version: `4.4.9`
- Target: Minecraft `1.20.1`, Forge `47.3.5`
- License: MIT
- Use in this repository: format and runtime-semantics research only. No
  GeckoLib source code is copied here.

## Assets and future derivations

- Test fixtures and textures must be authored for this project or carry an
  explicitly compatible redistributable license.
- Assets from third-party Minecraft mods must not be copied into fixtures.
- Any future source derived substantially from CPM, GeckoLib, or another
  project must receive a specific entry here identifying file, commit,
  copyright, license and the nature of the derivation.
- Merely observing file formats and behavior does not remove the obligation to
  record copied or substantially adapted implementation code.
