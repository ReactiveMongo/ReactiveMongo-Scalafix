package fix.play

import scala.concurrent.Future

import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoComponents
}

import com.github.ghik.silencer.silent
import MongoController.GridFS

trait Controller extends MongoController { self: ReactiveMongoComponents =>
  @silent
  def foo(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  @silent
  def bar(gfs: GridFS) = gridFSBodyParser(Future.successful(gfs))(null)

  def lorem(gfs: Future[GridFS]) =
    gridFSBodyParser(gfs)(null)
}
