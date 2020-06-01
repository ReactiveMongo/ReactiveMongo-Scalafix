package fix

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import akka.stream.Materializer

import reactivemongo.akkastream.AkkaStreamCursor

import reactivemongo.play.iteratees.PlayIterateesCursor

object Streaming {
  @silent def responseSrc(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    reactivemongo.api.bson.migrationRequired("Use bulkSource") /* c.responseSource(-1, reactivemongo.api.Cursor.FailOnError()) */

  @silent def responsePub(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    reactivemongo.api.bson.migrationRequired("Use bulkPublisher") /* c.responsePublisher() */

  @silent def responseEnum(c: PlayIterateesCursor[_])(implicit ec: ExecutionContext) =
    reactivemongo.api.bson.migrationRequired("Use bulkEnumerator") /* c.responseEnumerator() */
}
