ThisBuild / scalaVersion := "2.12.15"

ThisBuild / crossScalaVersions := Seq(
  "2.11.12", scalaVersion.value, "2.13.7")

ThisBuild / crossVersion := CrossVersion.binary

ThisBuild / scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8",
  "-unchecked",
  "-feature",
  "-Xlint",
  "-g:vars"
)

inThisBuild(Seq(// scalafix
  addCompilerPlugin(scalafixSemanticdb),
  scalacOptions ++= Seq(
    "-Yrangepos",
    "-P:semanticdb:synthetics:on"
  )
))

ThisBuild / scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12") {
    Seq(
      "-Xmax-classfile-name", "128",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-infer-any",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-macros:after"
    )
  } else if (scalaBinaryVersion.value == "2.11") {
    Seq(
      "-Xmax-classfile-name", "128",
      "-Yopt:_", "-Ydead-code", "-Yclosure-elim", "-Yconst-opt")
  } else {
    Seq(
      "-explaintypes",
      "-Wnumeric-widen",
      "-Wdead-code",
      "-Wvalue-discard",
      "-Wextra-implicit",
      "-Wmacros:after",
      "-Wunused")
  }
}

ThisBuild / Compile / console / scalacOptions ~= {
  _.filterNot(o =>
    o.startsWith("-X") || o.startsWith("-Y") || o.startsWith("-P:silencer"))
}

Test / scalacOptions ~= {
  _.filterNot(_ == "-Xfatal-warnings")
}

Compile / console / scalacOptions ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

Test / console / scalacOptions ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

ThisBuild / libraryDependencies ++= {
  val silencerVersion = "1.7.2"

  Seq(
    compilerPlugin(("com.github.ghik" %% "silencer-plugin" % silencerVersion).
      cross(CrossVersion.full)),
    ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided).
      cross(CrossVersion.full)
  )
}
