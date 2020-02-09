ThisBuild / organization := "org.reactivemongo"

name := "reactivemongo-scalafix-root"

import _root_.scalafix.sbt.{ BuildInfo => SF }

ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("staging"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases"))

addCompilerPlugin(scalafixSemanticdb)

lazy val rules = project.in(file("rules")).settings(
  name := "reactivemongo-scalafix",
  moduleName := "scalafix",
  libraryDependencies ++= Seq(
    "ch.epfl.scala" %% "scalafix-core" % SF.scalafixVersion)
)

lazy val input = project.in(file("input")).settings(
  libraryDependencies ++= {
    val previousVer = "0.12.7"

    val deps = Seq[(String, String)](
      "reactivemongo" -> previousVer,
      "play2-reactivemongo" -> s"${previousVer}-play26").map {
      case (nme, ver) => organization.value %% nme % ver % Provided
    }

    deps ++: Seq(
      "com.typesafe.play" %% "play" % "2.6.6" % Provided)
  },
  skip in publish := true
)

lazy val output = project.in(file("output")).settings(
  skip in publish := true,
  sources in Compile ~= {
    _.filterNot(_.getName endsWith "RequireMigration.scala")
  },
  libraryDependencies ++= {
    val latestVer = "1.0.0-rc.1-SNAPSHOT"

    val deps = Seq[(String, String)](
      "reactivemongo" -> latestVer,
      "play2-reactivemongo" -> "1.0.0-rc.1-play28-SNAPSHOT").map {
      case (nme, ver) => organization.value %% nme % ver % Provided
    }

    deps ++: Seq(
      "com.typesafe.play" %% "play" % "2.8.0" % Provided)

  }
).disablePlugins(SbtScalariform)

lazy val tests = project.in(file("tests"))
  .settings(
    skip in publish := true,
      libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % SF.scalafixVersion % Test cross CrossVersion.full,
    compile.in(Compile) :=
      compile.in(Compile).dependsOn(compile.in(input, Compile)).value,
    scalafixTestkitOutputSourceDirectories :=
      sourceDirectories.in(output, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      sourceDirectories.in(input, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(input, Compile).value,
  )
  .dependsOn(rules, output)
  .enablePlugins(ScalafixTestkitPlugin)

lazy val root = (project in file(".")).settings(
  skip in publish := true
).aggregate(rules, tests)
