/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.bson._
import reactivemongo.bson.BSONValue
import reactivemongo.bson.{
  BSONDocument,
  BSONObjectID,
  BSONReader,
  DefaultBSONHandlers
}
import reactivemongo.bson.BSONObjectID.{ generate => generateId }

import reactivemongo.bson.DefaultBSONHandlers._

import Macros.Annotations, Annotations.Ignore
import Macros.Annotations.Key

import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.collections.bson.BSONCollection

object Bson {
  def defaultHandlers = DefaultBSONHandlers.BSONBinaryHandler

  def asStr(in: BSONString) = in.as[String]

  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack

  def oid(): BSONObjectID = generateId()

  def bar(doc: reactivemongo.bson.BSONDocument): Option[Int] =
    (("foo" -> 0) ~: doc).getAs[BSONNumberLike]("_i").map(_.toInt)

  def lorem(doc: BSONDocument) =
    (doc :~ ("foo" -> 1)).getAs[String]("...")

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

  def reader2[T](r: BSONReader[BSONString, T]): Unit = {
    val v = r.read(BSONString("foo"))
    println(s"v = $v")
  }

  def reader3[T](f: BSONValue => T) = BSONReader[BSONValue, T](f)

  def reader4[T](f: BSONValue => T) =
    reactivemongo.bson.BSONReader[BSONValue, T](f)

  type Writer1[T] = BSONWriter[T, BSONValue]
  type Writer2[T] = reactivemongo.bson.BSONWriter[T, BSONValue]

  def writer1[T](w: reactivemongo.bson.BSONWriter[T, _ <: BSONValue]): Unit =
    println(s"w = $w")

  def writer2[T](w: BSONWriter[T, BSONInteger], v: T): Unit = {
    val b = w.write(v)
    println(s"b = $b")
  }

  def writer3[T](f: T => BSONValue) = BSONWriter[T, BSONValue](f)

  def writer4[T](f: T => BSONValue) =
    reactivemongo.bson.BSONWriter[T, BSONValue](f)

  type Handler1[T] = BSONHandler[BSONValue, T]
  type Handler2[T] = reactivemongo.bson.BSONHandler[BSONValue, T]

  def handler1[T](h: reactivemongo.bson.BSONHandler[_ <: BSONValue, T]): Unit =
    println(s"h = $h")

  def handler2[T](h: BSONHandler[BSONInteger, T], v: T): Unit = {
    val b = h.write(v)
    val r = h.read(BSONInteger(1))
    println(s"b = $b, r = $r")
  }

  def handler3[T](r: BSONValue => T, w: T => BSONValue) =
    BSONHandler[BSONValue, T](r, w)

  def handler4[T](r: BSONValue => T, w: T => BSONValue) =
    reactivemongo.bson.BSONHandler[BSONValue, T](r, w)

  def handler5[T](
    implicit
    r: BSONDocumentReader[T], w: BSONDocumentWriter[T]) =
    reactivemongo.bson.BSONDocumentHandler[T](r.read, w.write)

  def handler6[T](
    implicit
    r: BSONDocumentReader[T], w: BSONDocumentWriter[T]) =
    BSONDocumentHandler(r.read, w.write)

  def handler7[T, B <: BSONValue](
    implicit
    r: BSONReader[B, T], w: BSONWriter[T, B]) =
    BSONHandler(r.read, w.write)

  type NonEmptyList[T] = ::[T]

  object NonEmptyList {
    def of[T](head: T, tail: T*): NonEmptyList[T] = ::(head, tail.toList)
  }

  def nonEmptyListHandler[T](
    implicit
    aHandler: BSONHandler[_ <: BSONValue, T]): BSONHandler[BSONArray, NonEmptyList[T]] = {
    val reader = implicitly[BSONReader[BSONArray, List[T]]].afterRead[NonEmptyList[T]] {
      case head :: tail => NonEmptyList.of(head, tail: _*)
      case _ => throw new Exception("Expected a non empty list.")
    }

    def writer: BSONWriter[NonEmptyList[T], BSONArray] = ???

    BSONHandler[BSONArray, NonEmptyList[T]](reader.read, writer.write)
  }

  case class Foo(
    @Key("_name") name: String,
    @Ignore age: Int)
}
