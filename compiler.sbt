ThisBuild / scalaVersion := "2.12.10"

ThisBuild / crossScalaVersions := Seq("2.11.12", scalaVersion.value)

ThisBuild / crossVersion := CrossVersion.binary

ThisBuild / scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xlint",
  "-g:vars",
  "-Xfatal-warnings"
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
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-infer-any",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-macros:after"
    )
  } else {
    Seq("-Yopt:_", "-Ydead-code", "-Yclosure-elim", "-Yconst-opt")
  }
}

ThisBuild / scalacOptions in (Compile, console) ~= {
  _.filterNot(o =>
    o.startsWith("-X") || o.startsWith("-Y") || o.startsWith("-P:silencer"))
}

scalacOptions in Test ~= {
  _.filterNot(_ == "-Xfatal-warnings")
}

scalacOptions in (Compile, console) ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

scalacOptions in (Test, console) ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

ThisBuild / libraryDependencies ++= {
  val silencerVersion = "1.4.4"

  Seq(
    compilerPlugin(("com.github.ghik" %% "silencer-plugin" % silencerVersion).
      cross(CrossVersion.full)),
    ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided).
      cross(CrossVersion.full)
  )
}
