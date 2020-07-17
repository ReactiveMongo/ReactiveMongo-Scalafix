# ReactiveMongo Scalafix

Scalafix rules for ReactiveMongo

## Motivation

[Scalafix](https://scalacenter.github.io/scalafix/) rules to upgrade code using ReactiveMongo and check good practice for such integration.

## Usage

These rules can be configured in a [SBT](https://www.scala-sbt.org/) build.

First [setup Scalafix](https://scalacenter.github.io/scalafix/docs/users/installation.html) in the SBT build.

Then configure the ReactiveMongo rules:

```scala
scalafixDependencies in ThisBuild ++= Seq(
  "org.reactivemongo" %% "reactivemongo-scalafix" % VERSION)
```

To run the rules in SBT:

```
test:scalafix ReactiveMongoUpgrade
test:scalafix ReactiveMongoLinter --check
```

[![Maven](https://img.shields.io/maven-central/v/org.reactivemongo/reactivemongo-scalafix_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Creactivemongo-scalafix)

## Build manually

ReactiveMongo BSON libraries can be built from this source repository.

    sbt publishLocal

To run the tests, use:

    sbt -Dreactivemongo.api.migrationRequired.nonFatal=yes test

[![CircleCI](https://circleci.com/gh/ReactiveMongo/ReactiveMongo-Scalafix.svg?style=svg)](https://circleci.com/gh/ReactiveMongo/ReactiveMongo-Scalafix)