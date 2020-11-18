package fix

import com.github.ghik.silencer.silent

import akka.stream.Materializer

import reactivemongo.akkastream.AkkaStreamCursor

object Streaming {
  @silent def responseSrc(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    reactivemongo.api.bson.migrationRequired("Use bulkSource") /* c.responseSource(-1, reactivemongo.api.Cursor.FailOnError()) */

  @silent def responsePub(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    reactivemongo.api.bson.migrationRequired("Use bulkPublisher") /* c.responsePublisher() */
}
