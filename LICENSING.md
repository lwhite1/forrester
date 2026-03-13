# Licensing

This project uses a layered licensing model. The executable source code,
the original demo models, and imported third-party models each carry a
separate license.

## Project Source Code — AGPL-3.0

All Java source code in `courant-engine`, `courant-ui`, `courant-app`,
and `courant-tools` is licensed under the
**GNU Affero General Public License v3.0**.

See [`LICENSE`](LICENSE) in the project root.

## Demo Models & Code — CC-BY-SA-4.0

Original demo models and their Java source files in `courant-demos` are
licensed under **Creative Commons Attribution-ShareAlike 4.0 International**.
This covers both the model content (equations, parameters, structure) and
the hand-written Java code that constructs them.

See [`courant-demos/LICENSE`](courant-demos/LICENSE).

## Third-Party Models — CC-BY-NC-SA-4.0

Imported models from TU Delft (Pruyt, 2013) are licensed under
**Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International**.
This applies to the JSON model definitions under `courant-app/src/main/resources/models/`
and any generated Java demo classes derived from them.

See [`THIRD-PARTY-LICENSES`](THIRD-PARTY-LICENSES) for the full list of
third-party models and their attribution details.

## How the Layers Interact

The Creative Commons licenses cover the **model content** — the system
dynamics equations, parameters, variable names, and structural relationships
that define each model. The AGPL-3.0 governs the **simulation engine and
tooling** that compiles and executes those models.

When a CC-licensed model is compiled and run by the AGPL-licensed engine,
the model content retains its CC license while the engine code remains
under AGPL-3.0. Generated Java classes that reconstruct a model carry
the license of the underlying model content, not the engine.

| Component                | License          | File                        |
|--------------------------|------------------|-----------------------------|
| Engine, UI, app, tools   | AGPL-3.0         | `LICENSE`                   |
| Original demo models     | CC-BY-SA-4.0     | `courant-demos/LICENSE`   |
| TU Delft / Pruyt models  | CC-BY-NC-SA-4.0  | `THIRD-PARTY-LICENSES`      |

## Viewing Model Metadata

Each model stores its license, author, source, and URL in its
`ModelMetadata`. To view or edit this information in the application,
open **File > Model Data**. The dialog shows all metadata fields
for the currently loaded model.
