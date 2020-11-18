/*
rule = ReactiveMongoUpgrade
*/
package fix.gridfs

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.{
  BasicMetadata,
  ComputedMetadata,
  CustomMetadata,
  DefaultFileToSave,
  DefaultReadFile,
  GridFS
}

import play.modules.reactivemongo.JSONFileToSave

object RequireMigration {
  @silent
  def save(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.save(???, ???, ???)(???, ???, ???, ???)

  @silent
  def saveWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.saveWithMD5(???, ???, ???)(???, ???, ???, ???)

  @silent
  def iteratee(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iteratee(???, ???)(???, ???, ???, ???)

  @silent
  def iterateeWithMD5(gridfs: GridFS[BSONSerializationPack.type])(implicit ec: ExecutionContext) = gridfs.iterateeWithMD5(???, ???)(???, ???, ???, ???)

  def foo(
    fileToSave: DefaultFileToSave,
    rf: DefaultReadFile,
    jf: JSONFileToSave,
    m1: BasicMetadata[_],
    m2: ComputedMetadata,
    m3: CustomMetadata[_]) = println(s"fileToSave = $fileToSave, $rf, $jf, $m1, $m2, $m3")

}
