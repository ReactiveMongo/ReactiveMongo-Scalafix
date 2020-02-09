/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.bson._
import reactivemongo.bson.BSONValue
import reactivemongo.bson.{ BSONDocument, BSONObjectID }

import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.collections.bson.BSONCollection

object Bson {
  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack

  def bar(doc: reactivemongo.bson.BSONDocument): Option[Int] =
    doc.getAs[BSONNumberLike]("_i").map(_.toInt)

  def lorem(doc: BSONDocument) = doc.getAs[String]("...")

  def ipsum(doc: BSONDocument) = doc.getUnflattenedTry("...")

  def dolor(arr: BSONArray) = arr.getAs[Int](0)

  def bolo(arr: reactivemongo.bson.BSONArray) =
    arr.getAs[BSONBooleanLike](0).map { v => v.toBoolean }

  def collName1(coll: BSONCollection): String = coll.name

  def collName2(coll: reactivemongo.api.collections.bson.BSONCollection): String = coll.name
}
