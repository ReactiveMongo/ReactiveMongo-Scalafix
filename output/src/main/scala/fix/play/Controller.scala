package fix.play

import scala.concurrent.{ ExecutionContext, Future }




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

  @silent
  def foo(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  @silent
  def bar(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  def lorem(gfs: Future[GridFS]) =
    gridFSBodyParser(gfs)(null)

  def json1(coll: BSONCollection) = coll.name

  def json2(pack: BSONSerializationPack.type) = pack.toString

  def toJson(v: BSONValue) =
    reactivemongo.play.json.compat.fromValue(v)

  def toBson(v: play.api.libs.json.JsValue) = ValueConverters.toValue(v)

  type JP = reactivemongo.api.bson.collection.BSONSerializationPack.type

  def jp = reactivemongo.api.bson.collection.BSONSerializationPack

  def inline = "inline"
}

object PlayGridFS {
  import reactivemongo.api.gridfs.GridFS

  def resolve(database: Future[reactivemongo.api.DB])(
    implicit
    ec: ExecutionContext): Future[GridFS[_]] =
    database.map(db =>
      db.gridfs)
}
