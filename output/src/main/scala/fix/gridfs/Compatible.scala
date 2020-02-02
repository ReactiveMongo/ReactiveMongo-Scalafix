package fix.gridfs

import scala.concurrent.ExecutionContext

import reactivemongo.api.bson.BSONValue

import reactivemongo.api.bson.collection.BSONSerializationPack
import reactivemongo.api.DefaultDB
import reactivemongo.api.gridfs.{ GridFS, ReadFile }

object Compatible {
  def resolve1(db: DefaultDB) =
    db.gridfs

  def resolve2(db: DefaultDB) =
    db.gridfs("foo")

  // ---

  def remove(
    gridfs: GridFS[BSONSerializationPack.type],
    file: ReadFile[BSONValue, reactivemongo.api.bson.BSONDocument])(implicit ec: ExecutionContext) =
    gridfs.remove(file.id)
}
