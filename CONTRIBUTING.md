# Contributing

## Code of Conduct

Current project supports the [Scala code of conduct][code-of-conduct] and
wants all of its channels (GitHub, etc.) to be inclusive environments.

## Requirements

You will need the following tools:

* [Git][git]
* [Java 17][java-17]
* [SBT][sbt]
* [Node.js][node] 22 and [Yarn][yarn] 1.x (the converter installs the npm packages to read their types)
* [Python][python] 3 (only for `sbin/check-js-imports.py`)

## Project structure

| Path                      | What it is                                                                       |
|---------------------------|----------------------------------------------------------------------------------|
| `build.sbt`               | the `sjrFacades` generator: npm packages, versions, minimization                 |
| `project/ProjectSettings.scala` | settings shared by the generator and the modules, Scala versions            |
| `project/FacadeSplitter.scala`  | splits the generated sources into modules                                   |
| `sjr-facades/`            | working directory of the generator (generated sources, `node_modules`)           |
| `sjr-modules/`            | the generated modules, one per package (not committed)                           |
| `facade-modules.sbt`      | the generated sbt projects of the modules (not committed)                        |
| `sbin/check-js-imports.py`| checks that the imported JavaScript modules exist in `node_modules`              |

Everything generated is in `.gitignore` and is produced on every build, so never edit it by hand.

## Workflow

1. Create your own [fork][fork-and-pull] of the repository
   and work in a local branch based on `main`
2. Change `build.sbt` (see [Add or update a package](#add-or-update-a-package))
3. Run `sbt genFacades` and `sbt compileFacades`
4. [Check the facades in a real project](#check-in-a-real-project)
5. Run `sbt fmt` before creating the pull request
6. [Submit a pull request](#submit-a-pull-request)

## Add or update a package

Everything is configured in the `sjrFacades` project in `build.sbt`:

* add the package (and the `@types/*` it needs) with its version to `npmDependencies`,
* add the package to `stMinimize := Selection.AllExcept(...)`. The packages listed there are generated completely,
  the rest only as far as they are referenced, which keeps the transitive packages small,
* if a project that uses the facades needs something that minimization has removed (usually an implicit class
  in a companion or a member of a global object), add it to `stMinimizeKeep`, e.g. `std.PromiseLike.PromiseLikeOps`.
  The names are the names in the generated code without the `io.kinoplan.sjr` prefix.

Then regenerate and compile everything:

```bash
$ sbt genFacades        # generate the sources and split them into modules, for all Scala versions
$ sbt compileFacades    # compile all the modules for all Scala versions
```

`genFacades` takes a few minutes. The module graph is built from the references between the generated packages;
if two packages start to reference each other it fails and tells you which ones, merge them into one module then.

### Tree shaking

`stReactEnableTreeShaking` makes the components be imported by the long module path (`antd/es/checkbox`)
instead of the package entry point. It reduces the bundle size, but the path must exist at runtime, so a package
whose d.ts files have no JavaScript counterparts, or which restricts deep imports with `exports`, must be excluded
in `ProjectSettings.scala`. 

For example:

- `react-svg` ships only a single bundle, so `react-svg/dist/ReactSVG` from the d.ts files does not exist at runtime.
- `react-use-websocket-lite` restricts deep imports with `exports`, so `react-use-websocket-lite/dist/lib/use-websocket`
  is rejected by the bundler.

That is why both are excluded from `stReactEnableTreeShaking`.

Run `sbin/check-js-imports.py` against the linked output of a project that uses the facades to find such packages
(compilation never notices it, the bundler fails on the first request):

```bash
$ python3 sbin/check-js-imports.py --node-modules <project>/client/node_modules \
    --main-js <project>/client/target/scala-2.13/client-fastopt/main.js
```

### Minimization

If a project reports `value X is not a member of ...` for something that exists in the npm package,
the symbol was removed by minimization, add it to `stMinimizeKeep` or the package to `stMinimize`'s `AllExcept`.
A package that is listed in `AllExcept` is generated completely, so check the size of the module first.

## Check in a real project

Publish the facades locally and use them from the project:

```bash
$ sbt 'set ThisBuild / version := "0.0.1-SNAPSHOT"' +publishLocal
```

```scala
"io.kinoplan.scalajs-facades" %%% "sjr-antd" % "0.0.1-SNAPSHOT"
```

Without `set` the version is taken from git and includes the commit hash.
`sbt +publishLocal` does not generate anything, run `sbt genFacades` first.

## Formatting

### Scalafmt

We use [scalafmt][scalafmt] to format the sbt files and the build definition according to the rules
described in `.scalafmt.conf`. Make sure to run `sbt fmt` to ensure code formatting.
The generated sources are not formatted.

## Submit a pull request

* Pull requests should be submitted from a separate branch (e.g. using
  `git checkout -b "username/fix-123"`).
* In general we discourage force pushing to an active pull-request branch that other people are
  commenting on or contributing to, and suggest using `git merge master` during development. Once
  development is complete, use `git rebase master` and force push to [clean up the history][squash].
* The first line of a commit message should be no more than 72 characters long (to accommodate
  formatting in various environments).
* Commit messages should general use the present tense, normal sentence capitalization, and no final
  punctuation.
* If a pull request decreases code coverage more than by 5%, please file an issue to make sure that
  tests get added.

## Publish a Release (note for maintainers)

Push a Git tag into `main` branch:

```bash
$ git tag v0.0.1
$ git push origin v0.0.1
```

If release build fails, delete the tag from `main` branch

```bash
$ git tag -d v0.0.1
$ git push origin :refs/tags/v0.0.1
```

then make the corrections and try again.

[code-of-conduct]: https://www.scala-lang.org/conduct/

[fork-and-pull]: https://help.github.com/articles/using-pull-requests/

[git]: https://git-scm.com/

[java-17]: https://adoptium.net/temurin/releases/?version=17

[node]: https://nodejs.org/

[python]: https://www.python.org/

[sbt]: http://www.scala-sbt.org/

[scalafmt]: https://scalameta.org/scalafmt/

[squash]: http://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html

[yarn]: https://classic.yarnpkg.com/
