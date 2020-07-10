/*
rule = ReactiveMongoUpgrade
*/
package fix.play

import scala.concurrent.{ ExecutionContext, Future }

import play.api.libs.json._

import reactivemongo.bson.BSONValue

import reactivemongo.play.json.{ JSONSerializationPack, BSONFormats }

import reactivemongo.play.json.collection.JSONCollection

import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoComponents
}, MongoController.JsGridFS

import com.github.ghik.silencer.silent

trait Controller extends MongoController { self: ReactiveMongoComponents =>
  @silent def unused = {
    import MongoController.readFileReads
    ()
  }

  @silent
  def foo(gfs: JsGridFS) = gridFSBodyParser(gfs)(null, null, null)

  @silent
  def bar(gfs: JsGridFS) = gridFSBodyParser(gfs, null)(null, null, null, null)

  def lorem(gfs: Future[JsGridFS]) =
    gridFSBodyParser(gfs, null)(null, null, null)

  def json1(coll: JSONCollection) = coll.name

  def json2(pack: JSONSerializationPack.type) = pack.toString

  def toJson(v: BSONValue) =
    reactivemongo.play.json.BSONFormats.toJSON(v)

  def toBson(v: play.api.libs.json.JsValue) = BSONFormats.toBSON(v)

  type JP = reactivemongo.play.json.JSONSerializationPack.type

  def jp = reactivemongo.play.json.JSONSerializationPack

  def inline = CONTENT_DISPOSITION_INLINE

  @silent(".*dead\\ code.*")
  def fs1 = serve[JsString, reactivemongo.api.gridfs.ReadFile[JSONSerializationPack.type, JsString]](???)(???)(???)

  @silent
  def fs2(id: String, fs: JsGridFS)(implicit m: akka.stream.Materializer) = {
    import m.executionContext
    serve[JsString, MongoController.JsReadFile[JsString]](fs)(???)
  }
}

object PlayGridFS {
  import reactivemongo.api.gridfs.GridFS
  import reactivemongo.play.json.collection._

  def resolve(database: Future[reactivemongo.api.DefaultDB])(
    implicit
    ec: ExecutionContext): Future[GridFS[_]] =
    database.map(db =>
      GridFS[JSONSerializationPack.type](db))
}
