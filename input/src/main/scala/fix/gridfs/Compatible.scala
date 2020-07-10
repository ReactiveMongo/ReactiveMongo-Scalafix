/*
rule = ReactiveMongoUpgrade
*/
package fix.gridfs

import scala.concurrent.ExecutionContext

import reactivemongo.bson.{ BSONDocument, BSONValue }

import reactivemongo.api.{ BSONSerializationPack, DefaultDB }
import reactivemongo.api.gridfs.{ GridFS, ReadFile }

object Compatible {
  def resolve1(db: DefaultDB) =
    GridFS[BSONSerializationPack.type](db)

  def resolve2(db: DefaultDB) =
    GridFS[BSONSerializationPack.type](db, "foo")

  // ---

  def remove(
    gridfs: GridFS[BSONSerializationPack.type],
    file: ReadFile[BSONSerializationPack.type, BSONValue])(implicit ec: ExecutionContext) =
    gridfs.remove(file)

  // ---

  @com.github.ghik.silencer.silent
  def find(gridfs: GridFS[BSONSerializationPack.type]) =
    gridfs.find[BSONDocument, ReadFile[BSONSerializationPack.type, BSONValue]](???)(???, ???, ???, ???)

}
