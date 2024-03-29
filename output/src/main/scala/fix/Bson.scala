package fix



import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{ BSONDocument, BSONObjectID, BSONReader, BSONValue, _ }
import reactivemongo.api.bson.BSONObjectID.{generate => generateId}
import reactivemongo.api.bson.collection.BSONSerializationPack

object Bson {
  def defaultHandlers = reactivemongo.api.bson.migrationRequired("DefaultBSONHandlers is no longer public; Rather use implicit resolution.") /* DefaultBSONHandlers.BSONBinaryHandler */

  def afterWrite1[T](implicit w1: BSONWriter[T]): BSONWriter[T] = w1.afterWrite { case bson =>
    (BSONDocument(f"$$string" -> bson) ++ ("_hash" -> bson.hashCode))
  }

  def afterWrite2[T](implicit w2: BSONWriter[T]) =
    w2.afterWrite { case bson =>
      BSONDocument(f"$$value" -> bson)
    }

  def afterWrite3[T](implicit w3: BSONDocumentWriter[T]) =
    w3.afterWrite { doc =>
      (doc ++ ("_hash" -> doc.hashCode))
    }

  def afterWrite4[T](implicit w4: BSONWriter[T]): BSONWriter[T] = w4.afterWrite {
    case str @ BSONString(v) =>
      (BSONDocument(f"$$string" -> str) ++ ("_len" -> v.size))

    case int @ BSONInteger(_) =>
      BSONDocument(f"$$int" -> int)

    case _ =>
      BSONDocument(f"$$undefined" -> 1)
  }

  def asStr(in: BSONString) = in.asOpt[String]

  @com.github.ghik.silencer.silent
  def foo(n: BSONDouble, v: BSONValue, d: BSONDocument, i: BSONObjectID) = BSONSerializationPack

  def oid(): BSONObjectID = generateId()

  def bar(barDoc: reactivemongo.api.bson.BSONDocument): Option[Int] =
    ((BSONDocument("foo" -> 0) ++ barDoc)).getAsOpt[BSONNumberLike]("_i").flatMap(_.toInt.toOption)

  def lorem(loremDoc: BSONDocument) =
    ((loremDoc ++ ("foo" -> 1))).getAsOpt[String]("...")

  def ipsum(doc: BSONDocument) = doc.getAsUnflattenedTry[reactivemongo.api.bson.BSONValue]("...")

  def dolor(arr: BSONArray) = arr.getAsOpt[Int](0)

  def bolo(arr: reactivemongo.api.bson.BSONArray) =
    arr.getAsOpt[BSONBooleanLike](0).flatMap { v => v.toBoolean.toOption }

  def collName1(coll: BSONCollection): String = coll.name

  def collName2(coll: reactivemongo.api.bson.collection.BSONCollection): String = coll.name

  type Reader1[T] = BSONReader[T]
  type Reader2[T] = reactivemongo.api.bson.BSONReader[T]

  def reader1[T](r: reactivemongo.api.bson.BSONReader[T]): Unit =
    println(s"r = $r")

  def reader2[T](r: BSONReader[T]): Unit = {
    val v = r.readTry(BSONString("foo"))
    println(s"v = $v")
  }

  def reader3[T](f: BSONValue => T) = BSONReader[T](f)

  def reader4[T](f: BSONValue => T) =
    reactivemongo.api.bson.BSONReader[T](f)

  type Writer1[T] = BSONWriter[T]
  type Writer2[T] = reactivemongo.api.bson.BSONWriter[T]

  def writer1[T](w: reactivemongo.api.bson.BSONWriter[T]): Unit =
    println(s"w = $w")

  def writer2[T](w: BSONWriter[T], v: T): Unit = {
    val b = w.writeTry(v)
    println(s"b = $b")
  }

  def writer3[T](f: T => BSONValue) = BSONWriter[T](f)

  def writer4[T](f: T => BSONValue) =
    reactivemongo.api.bson.BSONWriter[T](f)

  type Handler1[T] = BSONHandler[T]
  type Handler2[T] = reactivemongo.api.bson.BSONHandler[T]

  def handler1[T](h: reactivemongo.api.bson.BSONHandler[T]): Unit =
    println(s"h = $h")

  def handler2[T](h: BSONHandler[T], v: T): Unit = {
    val b = h.writeTry(v)
    val r = h.readTry(BSONInteger(1))
    println(s"b = $b, r = $r")
  }

  def handler3[T](r: BSONValue => T, w: T => BSONValue) =
    BSONHandler[T](r, w)

  def handler4[T](r: BSONValue => T, w: T => BSONValue) =
    reactivemongo.api.bson.BSONHandler[T](r, w)

  def handler5[T](
    implicit
    r: BSONDocumentReader[T], w: BSONDocumentWriter[T]) =
    reactivemongo.api.bson.BSONDocumentHandler.from[T](r.readTry, w.writeTry)

  def handler6[T](
    implicit
    r: BSONDocumentReader[T], w: BSONDocumentWriter[T]) =
    BSONDocumentHandler.from(r.readTry, w.writeTry)

  def handler7[T, B <: BSONValue](
    implicit
    r: BSONReader[T], w: BSONWriter[T]) =
    BSONHandler.from(r.readTry, w.writeTry)

  type NonEmptyList[T] = ::[T]

  object NonEmptyList {
    def of[T](head: T, tail: T*): NonEmptyList[T] = ::(head, tail.toList)
  }

  def nonEmptyListHandler[T](
    implicit
    aHandler: BSONHandler[T]): BSONHandler[NonEmptyList[T]] = {
    val reader = implicitly[BSONReader[List[T]]].afterRead[NonEmptyList[T]] {
      case head :: tail => NonEmptyList.of(head, tail: _*)
      case _ => throw new Exception("Expected a non empty list.")
    }

    def writer: BSONWriter[NonEmptyList[T]] = ???

    BSONHandler.from[NonEmptyList[T]](reader.readTry, writer.writeTry)
  }
}
