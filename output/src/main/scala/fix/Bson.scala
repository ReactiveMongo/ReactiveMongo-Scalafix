package fix

import reactivemongo.api.bson._
import reactivemongo.api.bson.BSONValue
import reactivemongo.api.bson.{ BSONDocument, BSONObjectID }

import reactivemongo.api.bson.collection.BSONSerializationPack
// TODO: Collection classes

object Bson {
  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack
}
