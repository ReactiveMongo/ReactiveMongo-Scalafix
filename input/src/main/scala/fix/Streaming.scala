/*
rule = ReactiveMongoUpgrade
*/
package fix

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import akka.stream.Materializer

import reactivemongo.akkastream.AkkaStreamCursor

import reactivemongo.play.iteratees.PlayIterateesCursor

object Streaming {
  @silent def responseSrc(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    c.responseSource(-1, reactivemongo.api.Cursor.FailOnError())

  @silent def responsePub(c: AkkaStreamCursor[_])(implicit m: Materializer) =
    c.responsePublisher()

  @silent def responseEnum(c: PlayIterateesCursor[_])(implicit ec: ExecutionContext) =
    c.responseEnumerator()
}
