/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.bson._
import reactivemongo.bson.BSONValue
import reactivemongo.bson.{ BSONDocument, BSONObjectID }

import reactivemongo.api.BSONSerializationPack
// TODO: Collection classes

object Bson {
  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack
}
