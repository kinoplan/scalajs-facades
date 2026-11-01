import org.scalablytyped.converter.Flavour
import org.scalablytyped.converter.plugin.ScalablyTypedPluginBase.autoImport.*
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbt.*
import sbt.Keys.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*
import org.scalajs.sbtplugin.ScalaJSPlugin
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.useYarn

object ProjectSettings {

  private val namespace = "io.kinoplan.sjr"

  val scala2_13 = "2.13.18"
  val scala3 = "3.9.0"

  val scala2Versions: Seq[String] = Seq(scala2_13)
  val scalaVersions: Seq[String] = scala2Versions :+ scala3

  lazy val commonProfile: Project => Project = _.settings(
    crossScalaVersions := scalaVersions,
    scalaJSLinkerConfig ~= (_.withSourceMap(false)),
    stOutputPackage := namespace,
    // long module paths do not resolve at runtime: react-svg has no js files for its d.ts ones, react-use-websocket-lite restricts deep imports with exports
    stReactEnableTreeShaking := Selection.AllExcept("react-svg", "react-use-websocket-lite"),
    useYarn := true,
    Compile / doc / sources := Nil
  )

  lazy val sjsProfile: Project => Project =
    _.configure(commonProfile).settings(stFlavour := Flavour.ScalajsReact)

  /** A module of the split facades, see [[FacadeSplitter]]. */
  def facadeModule(module: String): Project =
    Project(FacadeSplitter.projectId(module), file(s"sjr-modules/$module"))
      .enablePlugins(ScalaJSPlugin)
      .settings(
        name := "sjr-" + module.replaceAll("([A-Z])", "-$1").toLowerCase,
        crossScalaVersions := scalaVersions,
        Compile / doc / sources := Nil,
        libraryDependencies ++=
          Seq(
            "com.olvind"                        %%% "scalablytyped-runtime" % "2.4.2",
            "com.github.japgolly.scalajs-react" %%% "core"                  % "4.0.0"
          )
      )

}
