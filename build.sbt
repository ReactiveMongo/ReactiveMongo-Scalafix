ThisBuild / organization := "org.reactivemongo"

name := "reactivemongo-scalafix-root"

import _root_.scalafix.sbt.{ BuildInfo => SF }

ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("staging"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases"))

addCompilerPlugin(scalafixSemanticdb)

lazy val extraOpts = Def.setting[Seq[String]] {
  val opts = Seq.newBuilder[String] += "-deprecation"

  if (scalaBinaryVersion.value != "2.13") {
    opts += "-Xfatal-warnings"
  } else {
    opts += "-Werror"
  }

  opts.result()
}

lazy val rules = project.in(file("rules")).settings(
  name := "reactivemongo-scalafix",
  moduleName := name.value,
  libraryDependencies ++= Seq(
    "ch.epfl.scala" %% "scalafix-core" % SF.scalafixVersion// cross CrossVersion.full
  ),
  scalacOptions ++= extraOpts.value
)

lazy val input = project.in(file("input")).settings(
  unmanagedSourceDirectories in Compile += {
    val base = (sourceDirectory in Compile).value

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n < 13 => base / "scala-2.13-"
      case _                      => base / "scala-2.13+"
    }
  },
  scalacOptions += "-deprecation",
  libraryDependencies ++= {
    val previousVer = {
      if (scalaBinaryVersion.value == "2.13") "0.20.9"
      else "0.12.7"
    }

    val (playPrefix, playVer) = {
      if (scalaBinaryVersion.value == "2.13") "27" -> "2.7.4"
      else "26" -> "2.6.6"
    }

    val deps = Seq.newBuilder[(String, String)] ++= Seq(
      "reactivemongo" -> previousVer,
      "reactivemongo-akkastream" -> previousVer,
      "play2-reactivemongo" -> s"${previousVer}-play${playPrefix}")

    if (scalaBinaryVersion.value != "2.13") {
      deps += "reactivemongo-iteratees" -> previousVer
    }

    (deps.result().map {
      case (nme, ver) => organization.value %% nme % ver % Provided
    }) ++: Seq(
      "com.typesafe.play" %% "play" % playVer % Provided)
  },
  skip in publish := true
)

lazy val output = project.in(file("output")).settings(
  skip in publish := true,
  sources in Compile ~= {
    _.filterNot(_.getName endsWith "RequireMigration.scala")
  },
  unmanagedSourceDirectories in Compile += {
    val base = (sourceDirectory in Compile).value

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n < 13 => base / "scala-2.13-"
      case _                      => base / "scala-2.13+"
    }
  },
  scalacOptions ++= extraOpts.value,
  scalacOptions += "-P:silencer:globalFilters=.*Unused\\ import.*",
  libraryDependencies ++= {
    val latestVer = "1.0.0-rc.2"
    val play2Ver = if (scalaBinaryVersion.value == "2.11") "7" else "8"
    val rmPlayVer = s"1.0.0-play2${play2Ver}-rc.2"

    val deps = Seq.newBuilder[(String, String)] ++= Seq(
      "reactivemongo" -> latestVer,
      "reactivemongo-play-json-compat" -> rmPlayVer,
      "play2-reactivemongo" -> rmPlayVer)

    if (scalaBinaryVersion.value != "2.13") {
      deps += "reactivemongo-iteratees" -> latestVer
    }

    (deps.result().map {
      case (nme, ver) => organization.value %% nme % ver % Provided
    }) ++: Seq(
      "com.typesafe.play" %% "play" % s"2.${play2Ver}.0" % Provided)

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
