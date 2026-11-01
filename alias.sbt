addCommandAlias("fmt", "+scalafmtSbt;")
addCommandAlias("format", "fmt;")

addCommandAlias(
  "genFacades",
  s"++${ProjectSettings.scala2_13}; sjrFacades/splitFacades; ++${ProjectSettings.scala3}; sjrFacades/splitFacades; reload"
)

addCommandAlias(
  "compileFacades",
  s"++${ProjectSettings.scala2_13}; facadeModules/compile; ++${ProjectSettings.scala3}; facadeModules/compile"
)
