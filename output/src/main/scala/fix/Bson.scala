package fix



import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{ BSONDocument, BSONObjectID, BSONReader, BSONValue, _ }
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

  type Reader1[T] = BSONReader[T]
  type Reader2[T] = reactivemongo.api.bson.BSONReader[T]

  def reader1[T](r: reactivemongo.api.bson.BSONReader[T]): Unit =
    println(s"r = $r")

  def reader2[T](r: BSONReader[T]): Unit =
    println(s"r = $r")

  type Writer1[T] = BSONWriter[T]
  type Writer2[T] = reactivemongo.api.bson.BSONWriter[T]

  def writer1[T](w: reactivemongo.api.bson.BSONWriter[T]): Unit =
    println(s"w = $w")

  def writer2[T](w: BSONWriter[T]): Unit =
    println(s"w = $w")

  type Handler1[T] = BSONHandler[T]
  type Handler2[T] = reactivemongo.api.bson.BSONHandler[T]

  def handler1[T](h: reactivemongo.api.bson.BSONHandler[T]): Unit =
    println(s"h = $h")

  def handler2[T](h: BSONHandler[T]): Unit =
    println(s"h = $h")
}
