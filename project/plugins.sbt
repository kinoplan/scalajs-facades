addDependencyTreePlugin

addSbtPlugin("io.kinoplan.scalablytyped" % "sbt-converter" % "1.0.0-beta45-kp-2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
