ThisBuild / dynverVTagPrefix := false

ThisBuild / version := {
  val Stable = """([0-9]+)\.([0-9]+)\.([0-9]+)""".r

  (ThisBuild / dynverGitDescribeOutput).value match {
    case Some(descr) => {
      if ((ThisBuild / isSnapshot).value) {
        (ThisBuild / previousStableVersion).value match {
          case Some(previousVer) => {
            val current = (for {
              Seq(maj, min, patch) <- Stable.unapplySeq(previousVer)
              nextPatch <- scala.util.Try(patch.toInt).map(_ + 1).toOption
            } yield {
              val suffix = descr.commitSuffix.sha
              s"${maj}.${min}.${nextPatch}-${suffix}-SNAPSHOT"
            }).getOrElse {
              println(
                s"Fails to determine qualified snapshot version: $previousVer")

              previousVer
            }

            current
          }

          case _ =>
            sys.error("Fails to determine previous stable version")
        }
      } else {
        descr.ref.value
      }
    }

    case _ =>
      sys.error("Fails to resolve Git information")
  }
}
