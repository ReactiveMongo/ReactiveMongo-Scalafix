package fix.play

import scala.concurrent.{ ExecutionContext, Future }


import play.modules.reactivemongo.MongoController

import com.github.ghik.silencer.silent
import MongoController.GridFS
import reactivemongo.api.bson.collection.BSONSerializationPack

trait ControllerGridFS { _: Controller =>

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

}

object PlayGridFS {
  import reactivemongo.api.gridfs.GridFS

  def resolve(database: Future[reactivemongo.api.DB])(
    implicit
    ec: ExecutionContext): Future[GridFS[_]] =
    database.map(db =>
      db.gridfs)
}
