package fix.play

import scala.concurrent.Future




import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoComponents
}

import com.github.ghik.silencer.silent
import MongoController.GridFS
import reactivemongo.api.bson.BSONValue
import reactivemongo.api.bson.collection.{ BSONCollection, BSONSerializationPack }
import reactivemongo.play.json.compat._

trait Controller extends MongoController { self: ReactiveMongoComponents =>
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
}
