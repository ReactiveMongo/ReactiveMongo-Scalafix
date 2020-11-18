/*
rule = ReactiveMongoUpgrade
*/
package fix.play

import scala.concurrent.{ ExecutionContext, Future }

import reactivemongo.play.json.JSONSerializationPack

import play.modules.reactivemongo.MongoController, MongoController.JsGridFS

import com.github.ghik.silencer.silent

trait ControllerGridFS { _: Controller =>

  @silent def unused = {
    import MongoController.readFileReads
    ()
  }

  type FS1 = JsGridFS
  type FS2 = MongoController.JsGridFS

  @silent
  def foo(gfs: JsGridFS) = gridFSBodyParser(gfs)(null, null, null)

  @silent
  def bar(gfs: JsGridFS) = gridFSBodyParser(gfs, null)(null, null, null, null)

  def lorem(gfs: Future[MongoController.JsGridFS]) =
    gridFSBodyParser(gfs, null)(null, null, null)

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
