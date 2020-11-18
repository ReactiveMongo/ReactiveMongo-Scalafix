package fix

import reactivemongo.api.indexes.Index

object IndexCommands {
  def index1: Seq[Index.Default] = Seq(Index(expireAfterSeconds = Option.empty[Int], languageOverride = Option.empty[String], textIndexVersion = Option.empty[Int], max = Option.empty[Double], bits = Option.empty[Int], sphereIndexVersion = Option.empty[Int], weights = None, storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], wildcardProjection = None, name = Some("foo"), collation = None, bucketSize = Option.empty[Double], key = Seq.empty))

  def index2: Index.Default = reactivemongo.api.indexes.Index(expireAfterSeconds = Option.empty[Int], languageOverride = Option.empty[String], textIndexVersion = Option.empty[Int], max = Option.empty[Double], bits = Option.empty[Int], sphereIndexVersion = Option.empty[Int], weights = None, version = Some(1), storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], wildcardProjection = None, background = false, unique = false, name = Some("foo"), collation = None, bucketSize = Option.empty[Double], key = Seq.empty)
}
