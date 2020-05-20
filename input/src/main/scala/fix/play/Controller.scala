/*
rule = ReactiveMongoUpgrade
*/
package fix.play

import scala.concurrent.Future

import reactivemongo.play.json.JSONSerializationPack

import reactivemongo.play.json.collection.JSONCollection

import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoComponents
}, MongoController.JsGridFS

import com.github.ghik.silencer.silent

trait Controller extends MongoController { self: ReactiveMongoComponents =>
  @silent
  def foo(gfs: JsGridFS) = gridFSBodyParser(gfs)(null, null, null)

  @silent
  def bar(gfs: JsGridFS) = gridFSBodyParser(gfs, null)(null, null, null, null)

  def lorem(gfs: Future[JsGridFS]) =
    gridFSBodyParser(gfs, null)(null, null, null)

  def json1(coll: JSONCollection) = coll.name

  def json2(pack: JSONSerializationPack.type) = pack.toString
}
