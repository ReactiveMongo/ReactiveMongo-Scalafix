# ReactiveMongo Scalafix

Scalafix rules for ReactiveMongo

## Motivation

[Scalafix](https://scalacenter.github.io/scalafix/) rules to upgrade code using ReactiveMongo and check good practice for such integration.

## Usage

These rules can be configured in a [SBT](https://www.scala-sbt.org/) build.

First update the `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.11")
```

Then in the `build.sbt`:

```scala
scalafixDependencies in ThisBuild += "org.reactivemongo" % "reactivemongo-scalafix" % VERSION

addCompilerPlugin(scalafixSemanticdb)

scalacOptions ++= List("-Yrangepos", "-P:semanticdb:synthetics:on")
```

To run the rules in SBT:

```
test:scalafix ReactiveMongoUpgrade
test:scalafix ReactiveMongoLinter
```

[![Maven](https://img.shields.io/maven-central/v/org.reactivemongo/reactivemongo-scalafix_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Creactivemongo-scalafix)

## Build manually

ReactiveMongo BSON libraries can be built from this source repository.

    sbt publishLocal

To run the tests, use:

    sbt test

[![CircleCI](https://circleci.com/gh/ReactiveMongo/ReactiveMongo-Scalafix.svg?style=svg)](https://circleci.com/gh/ReactiveMongo/ReactiveMongo-Scalafix)