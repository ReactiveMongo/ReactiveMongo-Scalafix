package fix.gridfs

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import reactivemongo.api.gridfs.GridFS
import reactivemongo.api.bson.collection.BSONSerializationPack

object RequireMigration {
  @silent
  def save(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.save(???, ???, ???)(???, ???, ???, ???) // Consider: gridfs.writeFromInputStream(???, _streamNotEnumerator, ???)(???)

  @silent
  def saveWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.saveWithMD5(???, ???, ???)(???, ???, ???, ???) // Consider: gridfs.writeFromInputStream(???, _streamNotEnumerator, ???)(???)

  @silent
  def iteratee(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iteratee(???, ???)(???, ???, ???, ???) // Consider: gridfs.readToOutputStream or GridFS support in streaming modules

  @silent
  def iterateeWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iterateeWithMD5(???, ???)(???, ???, ???, ???) // Consider: gridfs.readToOutputStream or GridFS support in streaming modules
}
