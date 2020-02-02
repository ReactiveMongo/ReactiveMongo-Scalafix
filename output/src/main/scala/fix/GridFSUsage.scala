package fix

import scala.concurrent.ExecutionContext

import reactivemongo.bson.BSONValue

import reactivemongo.api.{ BSONSerializationPack, DefaultDB }
import reactivemongo.api.gridfs.{ GridFS, ReadFile }

object GridFSUsage {
  def resolve1(db: DefaultDB) =
    db.gridfs

  def resolve2(db: DefaultDB) =
    db.gridfs("foo")

  // ---

  // TODO: import reactivemongo.bson
  // TODO: import reactivemongo.api.BSONSerialization =>

  def remove(
    gridfs: GridFS[BSONSerializationPack.type],
    file: ReadFile[BSONValue, reactivemongo.api.bson.BSONDocument])(implicit ec: ExecutionContext) =
    gridfs.remove(file.id)

}
