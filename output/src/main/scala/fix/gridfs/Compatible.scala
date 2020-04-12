package fix.gridfs

import scala.concurrent.ExecutionContext


import reactivemongo.api.gridfs.{ GridFS, ReadFile }
import reactivemongo.api.DB
import reactivemongo.api.bson.BSONValue
import reactivemongo.api.bson.collection.BSONSerializationPack

object Compatible {
  def resolve1(db: DB) =
    db.gridfs

  def resolve2(db: DB) =
    db.gridfs("foo")

  // ---

  def remove(
    gridfs: GridFS[BSONSerializationPack.type],
    file: ReadFile[BSONValue, reactivemongo.api.bson.BSONDocument])(implicit ec: ExecutionContext) =
    gridfs.remove(file.id)
}
