## Response Efficiency — CRITICAL

Minimize tokens in responses:
- No preamble: just do it, don't announce what you're about to do
- No summary: don't recap what you just did
- No meta-commentary: no "Great question!" or "Sure!"
- Unchanged code: abbreviate with `// ... existing code`
- Simple tasks = one sentence confirmation only

Minimize tokens in work, too:
- `genFacades` takes minutes and `sjr-modules/` is huge — never `cat`/`grep -r` it, look at one module or use `ls`/`wc`
- Read the sbt output tail (`| tail -40`), not the whole log
- Don't regenerate when only docs, CI or `project/*.scala` comments changed

---

# Scala.js Facades — AI Agent Context

> This file provides context for AI assistants working with this codebase.
>
> Developers can edit this and/or related files to improve AI assistant behavior within the project.

---

## 1. Project Overview

**scalajs-facades** publishes Scala.js facades for selected npm packages as a set of libraries, one per npm package.
The facades are generated with the [kinoplan fork](https://github.com/kinoplan/ScalablyTypedConverter) of
ScalablyTyped (`io.kinoplan.scalablytyped:sbt-converter`) and published to Maven Central under
`io.kinoplan.scalajs-facades` as `sjr-<kebab-package>`.

- **Flavour:** `sjr` = Scala.js React: everything is generated with `stFlavour := Flavour.ScalajsReact` ([scalajs-react](https://github.com/japgolly/scalajs-react))
- **Language:** Scala 2.13 / 3 (versions in [project/ProjectSettings.scala](project/ProjectSettings.scala)), Scala.js only
- **Build:** SBT, `ScalablyTypedConverterGenSourcePlugin`, `sbt-ci-release`, `sbt-dynver`
- **Generated code is not committed:** `sjr-modules/` and `facade-modules.sbt` are in `.gitignore`

**Key references:**
- Usage, published packages — [README.md](README.md)
- Workflow, adding a package, tree shaking, minimization — [CONTRIBUTING.md](CONTRIBUTING.md)
- npm packages, `stMinimize`, `stMinimizeKeep` — [build.sbt](build.sbt)
- Shared settings, module factory — [project/ProjectSettings.scala](project/ProjectSettings.scala)
- Splitting the generated sources into modules — [project/FacadeSplitter.scala](project/FacadeSplitter.scala)
- Commands — [alias.sbt](alias.sbt)
- Check of the JS imports — [sbin/check-js-imports.py](sbin/check-js-imports.py)

### Architecture

```
npmDependencies (build.sbt)
    ↓ sjrFacades                — ScalablyTyped converts .d.ts → Scala sources (one big source tree)
    ↓ sjrFacades/splitFacades   — FacadeSplitter: io.kinoplan.sjr.<pkg> → sjr-modules/<pkg>, facade-modules.sbt
    ↓ facadeModules             — one sbt project per package, `dependsOn` from the references between packages
    ↓ publishSigned             — release in CI (`sbt ci-release`)
```

| Piece | Purpose |
|-------|---------|
| `sjrFacades` | Converter project, `publish / skip := true`; the only place where npm packages are listed |
| `sjr-modules/<pkg>/src/main/scala-<2.13\|3>/` | Generated sources, Scala 2.13 and 3 outputs differ in every file |
| `facade-modules.sbt` | Generated list of `ProjectSettings.facadeModule("<pkg>").dependsOn(...)` and the `facadeModules` aggregate |
| `base` module | Files in the root `io.kinoplan.sjr` package (e.g. `StBuildingComponent`) |

### Commands

| Command | What it does |
|---------|--------------|
| `sbt genFacades` | Generate and split for Scala 2.13 and 3, then reload (needs Node 22, Yarn 1.x, takes minutes) |
| `sbt compileFacades` | Compile all modules for both Scala versions |
| `sbt format` | scalafmt for `project/*.scala` and sbt files |
| `sbt +publishLocal` | Publish all modules locally, after `genFacades` |

---

## 2. Patterns

| Concept | Pattern |
|---------|---------|
| Module name | `sjr-` + npm name in kebab-case without the scope (`@ant-design/icons` → `sjr-ant-design-icons`) |
| Package | `io.kinoplan.sjr.<pkg>` (`ProjectSettings.namespace`); a package becomes a module |
| Module graph | Computed from FQN references in the generated sources; `ensureAcyclic` fails on a cycle — merge those packages |
| Minimization | `stMinimize := Selection.AllExcept(...)` lists the packages that are generated in full; the rest only as far as they are referenced |
| Kept members | Anything removed by minimization that users still need goes to `stMinimizeKeep` |
| Tree shaking | `stReactEnableTreeShaking` imports components by the long path; packages without that path at runtime are excluded in `ProjectSettings` |
| Versioning | `sbt-dynver`; a tag `vX.Y.Z` makes a release, otherwise `0.0.0+N-<hash>-SNAPSHOT` |

---

## 3. AI Agent Rules

> Everything below is instructions for AI assistants.
>
> Developers can edit these rules to control AI behavior when working with this code.

### 3.1. Principles

1. **Never edit generated files** — `sjr-modules/` and `facade-modules.sbt` are overwritten by `genFacades`; fix the cause in `build.sbt`, `ProjectSettings.scala` or `FacadeSplitter.scala`.
2. **Facades are a public API** — a renamed or removed type breaks every user; changing `stMinimize`, `stMinimizeKeep` or tree shaking changes the artifacts.
3. **Both Scala versions** — a change must work for 2.13 and 3, generation and compilation run for both.
4. **`sjr` means scalajs-react** — don't mix flavours in these modules; another flavour needs its own prefix, namespace and modules, not a flag on the existing ones.
5. **Minimal dependencies** — only `scalablytyped-runtime` and `scalajs-react` core in modules; don't add more.
6. **Comments in English only**, in the style of the surrounding code.
7. **Formatting** — run `sbt format` before finishing.

### 3.2. Adding or Updating a Package

1. Add or change the npm package and version in `npmDependencies` in `build.sbt`.
2. If its types should be generated in full, add it to `stMinimize` (`Selection.AllExcept`).
3. Run `sbt genFacades`, then `sbt compileFacades`.
4. If something that users need disappeared, add it to `stMinimizeKeep` (the member and the companion/`^` object if needed).
5. Check the JS imports in a real project with `sbin/check-js-imports.py`, the compiler never notices a missing path.
6. Update the table in [README.md](README.md) (npm package, version, artifact).

### 3.3. Common Pitfalls

- **`%%%` inside `.map(...)` lambda** crashes the Scala 2.12 meta-build — use a single `Def.setting(Seq(...))`.
- **Scala 2.13 and 3 outputs differ** — never copy a generated file from one `scala-<ver>` directory to the other.
- **Minimization removes companions and members** (e.g. `PromiseLikeOps.toFuture`, `^` objects) — symptom is a compile error in the user project, fix with `stMinimizeKeep`.
- **Tree shaking paths** — a long path that does not exist at runtime fails only in the bundler (`react-svg`, `react-use-websocket-lite`).
- **csstype** — unions with the `(string & {})` fallback are generated as `| String` aliases, literal-only unions as traits with companion constants.
- **`stIgnore` in a user project** — ignored packages turn into `js.Any` in the plugin's own output, they do not interoperate with `io.kinoplan.sjr`.
- **sbt version** — the fork plugin `1.0.0-beta45-kp-2` is built for sbt 1.x; sbt 2 needs `_sbt2_3` plugin artifacts that are not published.
- **Fork plugin in CI** — it must be published (it is in `~/.ivy2/local` otherwise), or `genFacades` can't resolve it.
- **Version changes with every commit** (`dynver`) — publish and use the facades from the same commit.

### 3.4. Planning

**Create a plan for:**
- Changing the splitting algorithm or the module layout (`FacadeSplitter`)
- Changing `stMinimize` / tree shaking defaults for many packages
- Bumping Scala, the converter plugin or sbt
- Changes in the release or CI flow

**No plan needed for:**
- Adding or updating a single npm package
- `stMinimizeKeep` entries
- Documentation, comments, formatting

### 3.5. Checklist Before Finishing

1. **Self-check** — would a maintainer approve this on code review?
2. **Generation** — `sbt genFacades` passes if `build.sbt`, `ProjectSettings.scala` or `FacadeSplitter.scala` changed
3. **Compilation** — `sbt compileFacades` passes for Scala 2.13 and 3
4. **Formatting** — `sbt format` executed
5. **Docs** — [README.md](README.md) table and [CONTRIBUTING.md](CONTRIBUTING.md) updated if packages or workflow changed
6. **No generated files** in the diff (`git status` shows only sources)

### 3.6. Debugging

1. Generation fails — check Node/Yarn versions and the converter log tail, then the package in `npmDependencies`
2. Cycle error from `ensureAcyclic` — merge the packages it names into one module
3. Compile error in a module — usually minimization (`stMinimizeKeep`) or a missing `dependsOn`
4. Compile error in a user project — missing member (`stMinimizeKeep`) or `js.Any` from `stIgnore`
5. Runtime error in the bundler — unresolved import path, run `sbin/check-js-imports.py` and check tree shaking

**Root Cause Analysis:**
- Don't patch generated files — find which setting produced them
- Check how a similar package is configured in `build.sbt` first
