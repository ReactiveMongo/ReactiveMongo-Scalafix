/*
rule = ReactiveMongoUpgrade
*/
package fix.gridfs

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.GridFS

object RequireMigration {
  @silent
  def save(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.save(???, ???, ???)(???, ???, ???, ???)

  @silent
  def saveWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.saveWithMD5(???, ???, ???)(???, ???, ???, ???)

  @silent
  def iteratee(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iteratee(???, ???)(???, ???, ???, ???)

  @silent
  def iterateeWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iterateeWithMD5(???, ???)(???, ???, ???, ???)
}
