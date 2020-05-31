/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.bson._
import reactivemongo.bson.BSONValue
import reactivemongo.bson.{ BSONDocument, BSONObjectID, BSONReader }

import reactivemongo.bson.DefaultBSONHandlers._

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

  type Reader1[T] = BSONReader[BSONValue, T]
  type Reader2[T] = reactivemongo.bson.BSONReader[BSONValue, T]

  def reader1[T](r: reactivemongo.bson.BSONReader[_ <: BSONValue, T]): Unit =
    println(s"r = $r")

  def reader2[T](r: BSONReader[BSONString, T]): Unit =
    println(s"r = $r")

  type Writer1[T] = BSONWriter[T, BSONValue]
  type Writer2[T] = reactivemongo.bson.BSONWriter[T, BSONValue]

  def writer1[T](w: reactivemongo.bson.BSONWriter[T, _ <: BSONValue]): Unit =
    println(s"w = $w")

  def writer2[T](w: BSONWriter[T, BSONInteger]): Unit =
    println(s"w = $w")

  type Handler1[T] = BSONHandler[BSONValue, T]
  type Handler2[T] = reactivemongo.bson.BSONHandler[BSONValue, T]

  def handler1[T](h: reactivemongo.bson.BSONHandler[_ <: BSONValue, T]): Unit =
    println(s"h = $h")

  def handler2[T](h: BSONHandler[BSONInteger, T]): Unit =
    println(s"h = $h")
}
