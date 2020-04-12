package fix


import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{ BSONDocument, BSONObjectID, BSONValue, _ }
import reactivemongo.api.bson.collection.BSONSerializationPack

object Bson {
  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack

  def bar(doc: reactivemongo.api.bson.BSONDocument): Option[Int] =
    doc.getAsOpt[BSONNumberLike]("_i").flatMap { _.toInt.toOption }

  def lorem(doc: BSONDocument) = doc.getAsOpt[String]("...")

  def ipsum(doc: BSONDocument) = doc.getAsUnflattenedTry[reactivemongo.api.bson.BSONValue]("...")

  def dolor(arr: BSONArray) = arr.getAsOpt[Int](0)

  def bolo(arr: reactivemongo.api.bson.BSONArray) =
    arr.getAsOpt[BSONBooleanLike](0).flatMap { v => (v.toBoolean).toOption }

  def collName1(coll: BSONCollection): String = coll.name

  def collName2(coll: reactivemongo.api.bson.collection.BSONCollection): String = coll.name
}
