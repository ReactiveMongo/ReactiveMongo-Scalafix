package fix

import reactivemongo.api.indexes.Index

object IndexCommands {
  def index1: Seq[Index.Default] = Seq(Index(expireAfterSeconds = Option.empty[Int], textIndexVersion = Option.empty[Int], languageOverride = Option.empty[String], bucketSize = Option.empty[Double], key = Seq.empty, weights = None, collation = None, max = Option.empty[Double], sphereIndexVersion = Option.empty[Int], wildcardProjection = None, name = Some("foo"), storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], bits = Option.empty[Int]))

  def index2: Index.Default = reactivemongo.api.indexes.Index(expireAfterSeconds = Option.empty[Int], textIndexVersion = Option.empty[Int], languageOverride = Option.empty[String], bucketSize = Option.empty[Double], key = Seq.empty, unique = false, weights = None, collation = None, max = Option.empty[Double], sphereIndexVersion = Option.empty[Int], wildcardProjection = None, name = Some("foo"), storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], background = false, bits = Option.empty[Int], version = Some(1))
}
