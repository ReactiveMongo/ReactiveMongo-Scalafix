package fix.play

import scala.concurrent.{ ExecutionContext, Future }

import play.api.libs.json._


import reactivemongo.api.gridfs.ReadFile



import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoComponents
}

import com.github.ghik.silencer.silent
import MongoController.GridFS
import reactivemongo.api.bson.BSONValue
import reactivemongo.api.bson.collection.{ BSONCollection, BSONSerializationPack }
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson._

trait Controller extends MongoController { self: ReactiveMongoComponents =>
  @silent def unused = {
    ()
  }

  type FS1 = GridFS
  type FS2 = MongoController.GridFS

  @silent
  def foo(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  @silent
  def bar(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  def lorem(gfs: Future[MongoController.GridFS]) =
    gridFSBodyParser(gfs)(null)

  def json1(coll: BSONCollection) = coll.name

  def json2(pack: BSONSerializationPack.type) = pack.toString

  def toJson(v: BSONValue) =
    reactivemongo.play.json.compat.fromValue(v)

  def toBson(v: play.api.libs.json.JsValue) = ValueConverters.toValue(v)

  type JP = reactivemongo.api.bson.collection.BSONSerializationPack.type

  def jp = reactivemongo.api.bson.collection.BSONSerializationPack

  def inline = "inline"

  @silent(".*dead\\ code.*")
  def fs1 = serve[reactivemongo.api.bson.BSONValue](???)(???)(???)

  @silent
  def fs2(id: String, fs: GridFS)(implicit m: akka.stream.Materializer) = {
    import m.executionContext
    serve[reactivemongo.api.bson.BSONValue](fs)(???)
  }

  type JSONReadFile1 = ReadFile[reactivemongo.api.bson.BSONString, reactivemongo.api.bson.BSONDocument]

  type JSONReadFile2 = reactivemongo.api.gridfs.ReadFile[reactivemongo.api.bson.BSONString, reactivemongo.api.bson.BSONDocument]

}

object PlayGridFS {
  import reactivemongo.api.gridfs.GridFS

  def resolve(database: Future[reactivemongo.api.DB])(
    implicit
    ec: ExecutionContext): Future[GridFS[_]] =
    database.map(db =>
      db.gridfs)
}
