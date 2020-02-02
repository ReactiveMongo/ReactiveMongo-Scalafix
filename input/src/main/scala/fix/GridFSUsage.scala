/*
rule = ReactiveMongoUpgrade
*/
package fix

import scala.concurrent.ExecutionContext

import reactivemongo.bson.BSONValue

import reactivemongo.api.{ BSONSerializationPack, DefaultDB }
import reactivemongo.api.gridfs.{ GridFS, ReadFile }

object GridFSUsage {
  def resolve1(db: DefaultDB) =
    GridFS[BSONSerializationPack.type](db)

  def resolve2(db: DefaultDB) =
    GridFS[BSONSerializationPack.type](db, "foo")

  // ---

  // TODO: import reactivemongo.bson
  // TODO: import reactivemongo.api.BSONSerialization =>

  def remove(
    gridfs: GridFS[BSONSerializationPack.type],
    file: ReadFile[BSONSerializationPack.type, BSONValue])(implicit ec: ExecutionContext) =
    gridfs.remove(file)

}
