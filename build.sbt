ThisBuild / scalaVersion := ProjectSettings.scala2_13

lazy val splitFacades = taskKey[Unit]("Splits generated facade sources into per-package modules")

lazy val sjrFacades = project
  .in(file("sjr-facades"))
  .enablePlugins(ScalablyTypedConverterGenSourcePlugin)
  .configure(ProjectSettings.sjsProfile)
  .settings(name := "sjr-facades", publish / skip := true)
  .settings(
    splitFacades := {
      val _ = (Compile / managedSources).value
      val root = (ThisBuild / baseDirectory).value

      FacadeSplitter.split(
        generatedRoot = (Compile / sourceManaged).value / "scalablytyped",
        modulesDir = root / "sjr-modules",
        sbtFile = root / "facade-modules.sbt",
        scalaDir = s"scala-${scalaBinaryVersion.value}"
      )
    }
  )
  .settings(
    Compile / npmDependencies ++=
      Seq(
        "@ant-design/icons" -> "6.2.5",
        "@ant-design/pro-components" -> "3.1.12-0",
        "@monaco-editor/loader" -> "1.7.0",
        "@monaco-editor/react" -> "4.7.0",
        "@sentry/react" -> "10.55.0",
        "@types/react" -> "19.2.17",
        "@types/react-dom" -> "19.2.3",
        "@types/react-syntax-highlighter" -> "15.5.13",
        "@types/scheduler" -> "0.26.0",
        "antd" -> "6.4.3",
        "i18next" -> "26.3.0",
        "i18next-browser-languagedetector" -> "8.2.1",
        "monaco-editor" -> "0.55.1",
        "rc-virtual-list" -> "3.19.2",
        "react" -> "19.2.7",
        "react-country-flag" -> "3.1.0",
        "react-dom" -> "19.2.7",
        "react-i18next" -> "17.0.8",
        "react-svg" -> "17.2.4",
        "react-syntax-highlighter" -> "16.1.1",
        "react-use-websocket-lite" -> "1.1.0",
        "scheduler" -> "0.27.0"
      ),
    // minimized libraries lose their companions, keep what dependents use directly
    stMinimizeKeep ++=
      List(
        "std.Promise.PromiseOps",
        "std.PromiseLike.PromiseLikeOps",
        "std.global.Notification.^",
        "std.global.Notification.requestPermission",
        "sentryBrowser.mod.^",
        "sentryBrowser.mod.browserTracingIntegration",
        "sentryCore.buildTypesTypesOptionsMod.CoreOptions"
      ),
    stMinimize :=
      Selection.AllExcept(
        "@ant-design/icons",
        "@ant-design/pro-components",
        "@monaco-editor/loader",
        "@monaco-editor/react",
        "@sentry/react",
        "antd",
        "csstype",
        "i18next",
        "i18next-browser-languagedetector",
        "rc-virtual-list",
        "react",
        "react-country-flag",
        "react-dom",
        "react-i18next",
        "react-svg",
        "react-syntax-highlighter",
        "react-use-websocket-lite"
      )
  )

publish / skip := true
crossScalaVersions := ProjectSettings.scalaVersions

// format: off
inThisBuild(
  List(
    sonatypeCredentialHost := Sonatype.sonatype01,
    versionScheme := Some(VersionScheme.EarlySemVer),
    organization := "io.kinoplan.scalajs-facades",
    homepage := Some(url("https://github.com/kinoplan/scalajs-facades")),
    licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
    developers := List(Developer("kinoplan", "Kinoplan", "job@kinoplan.ru", url("https://kinoplan.tech"))),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/kinoplan/scalajs-facades"),
        "scm:git:git@github.com:kinoplan/scalajs-facades.git"
      )
    )
  )
)
// format: on

Global / onChangedBuildSource := ReloadOnSourceChanges
