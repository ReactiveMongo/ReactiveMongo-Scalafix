resolvers ++= Seq(
  Resolver.url("typesafe-repo", url(
   "https://dl.bintray.com/typesafe/sbt-plugins/"))(Resolver.ivyStylePatterns))

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.23")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
