/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.api.indexes.Index

object IndexCommands {
  def index1: Seq[Index] = Seq(Index(Seq.empty, name = Some("foo")))

  def index2: Index = reactivemongo.api.indexes.Index(
    Seq.empty, name = Some("foo"), false, false, false, version = Some(1))
}
