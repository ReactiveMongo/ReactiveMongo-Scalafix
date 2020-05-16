package fix

import scala.concurrent.ExecutionContext

import akka.stream.Materializer

import reactivemongo.akkastream.AkkaStreamCursor

import reactivemongo.play.iteratees.PlayIterateesCursor

object Streaming {
  def responseSrc(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    ??? /* c.responseSource(-1, reactivemongo.api.Cursor.FailOnError()): Use bulkSource */

  def responsePub(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    ??? /* c.responsePublisher(): Use bulkPublisher */

  def responseEnum(c: PlayIterateesCursor[_])(implicit ec: ExecutionContext) =
    ??? /* c.responseEnumerator(): Use bulkEnumerator */
}
