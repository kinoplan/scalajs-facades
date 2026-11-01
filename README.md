# scalajs-facades

![build](https://github.com/kinoplan/scalajs-facades/workflows/build/badge.svg)

Scala.js facades for selected npm packages, generated with the [kinoplan fork][converter-fork] of [ScalablyTyped][scalablytyped]
and published as a set of separate libraries.

##### Contents

* [Usage](#usage)
* [Published packages](#published-packages)
* [Using together with sbt-converter](#using-together-with-sbt-converter)
* [How it works](#how-it-works)
* [Contributing](#contributing)
* [License](#license)

## Usage

The facades are built for Scala 2.13 and Scala 3, on the Scala.js platform only.

Add the modules you need to `libraryDependencies`. All of them share one version:

```scala
libraryDependencies ++= Seq(
  "io.kinoplan.scalajs-facades" %%% "sjr-antd" % ${version},
  "io.kinoplan.scalajs-facades" %%% "sjr-ant-design-icons" % ${version}
)
```

The facades describe the API only, the JavaScript itself comes from `node_modules`, so install the npm packages
(the versions are listed below) in your project as usual.

The types live in the `io.kinoplan.sjr` package, for example `io.kinoplan.sjr.antd`.

## Published packages

These packages are generated from the versions pinned in `build.sbt`:

| npm package                        | version  | artifact                               |
|------------------------------------|----------|----------------------------------------|
| `antd`                             | 6.4.3    | `sjr-antd`                             |
| `@ant-design/icons`                | 6.2.5    | `sjr-ant-design-icons`                 |
| `@ant-design/pro-components`       | 3.1.12-0 | `sjr-ant-design-pro-components`        |
| `@monaco-editor/loader`            | 1.7.0    | `sjr-monaco-editor-loader`             |
| `@monaco-editor/react`             | 4.7.0    | `sjr-monaco-editor-react`              |
| `@sentry/react`                    | 10.55.0  | `sjr-sentry-react`                     |
| `i18next`                          | 26.3.0   | `sjr-i18next`                          |
| `i18next-browser-languagedetector` | 8.2.1    | `sjr-i18next-browser-languagedetector` |
| `rc-virtual-list`                  | 3.19.2   | `sjr-rc-virtual-list`                  |
| `react`                            | 19.2.7   | `sjr-react`                            |
| `react-country-flag`               | 3.1.0    | `sjr-react-country-flag`               |
| `react-dom`                        | 19.2.7   | `sjr-react-dom`                        |
| `react-i18next`                    | 17.0.8   | `sjr-react-i18next`                    |
| `react-svg`                        | 17.2.4   | `sjr-react-svg`                        |
| `react-syntax-highlighter`         | 16.1.1   | `sjr-react-syntax-highlighter`         |
| `react-use-websocket-lite`         | 1.1.0    | `sjr-react-use-websocket-lite`         |

Their dependencies (`std`, `csstype`, `@rc-component/*`, `@sentry/core` and so on) are published as separate modules
and come in transitively.

To keep the artifacts small, only what the packages above actually reference is generated for those dependencies
("minimized"). If you need something outside of that, it may be missing, see
[CONTRIBUTING.md](/CONTRIBUTING.md#minimization).

## Using together with sbt-converter

You can keep the ScalablyTyped sbt plugin in your project for npm packages that are not published here yet.
Put the packages covered by these facades into `stIgnore`, so the plugin does not generate them a second time:

```scala
stIgnore ++= List("antd", "@ant-design/icons", "react", "react-dom")
```

If a package is in `stIgnore`, the plugin does not generate its types. So wherever a package that the plugin does
generate refers to them (e.g. `ReactNode` or `CSSProperties`), you get `js.Any` instead of the type from
`io.kinoplan.sjr`.

## How it works

The `sjrFacades` project converts the npm packages from `build.sbt` into Scala sources. Then `sbt genFacades` splits
them into one sbt project per package (see `sjr-modules/` and `facade-modules.sbt`), which are built and published
on release. The references between the generated packages become `dependsOn` between the projects, so transitive
dependencies are separate modules that come in transitively. The generated code is not committed.

The details are in [CONTRIBUTING.md](/CONTRIBUTING.md).

## Contributing

Missing a package or found a broken type? See [CONTRIBUTING.md](/CONTRIBUTING.md) for how to add or update one.

## License

This project is licensed under the terms of the [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0).

[scalablytyped]: https://scalablytyped.org/
[converter-fork]: https://github.com/kinoplan/ScalablyTypedConverter
