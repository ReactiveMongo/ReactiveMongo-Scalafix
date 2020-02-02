import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Publish extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  private val repoName = env("PUBLISH_REPO_NAME")
  private val repoUrl = env("PUBLISH_REPO_URL")

  override def projectSettings = Seq(
    licenses := Seq(
      "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := Some(repoUrl).map(repoName at _),
    credentials += Credentials(repoName, env("PUBLISH_REPO_ID"),
      env("PUBLISH_USER"), env("PUBLISH_PASS")),
    pomIncludeRepository := { _ => false },
    homepage := Some(url("http://reactivemongo.org")),
    autoAPIMappings := true,
    pomExtra := (
      <scm>
        <url>git://github.com/ReactiveMongo/ReactiveMongo-Scalafix.git</url>
          <connection>scm:git://github.com/ReactiveMongo/ReactiveMongo-Scalafix.git</connection>
          </scm>
        <developers>
        <developer>
        <id>cchantep</id>
        <name>Cedric Chantepie</name>
        <url>https://github.com/cchantep</url>
          </developer>
        </developers>)
  )

  @inline private def env(n: String): String = sys.env.get(n).getOrElse(n)
}
