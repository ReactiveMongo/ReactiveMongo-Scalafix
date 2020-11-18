package fix

import com.github.ghik.silencer.silent

import scala.concurrent.ExecutionContext

import reactivemongo.play.iteratees.PlayIterateesCursor

object StreamingIteratees {
  @silent def responseEnum(c: PlayIterateesCursor[_])(implicit ec: ExecutionContext) =
    reactivemongo.api.bson.migrationRequired("Use bulkEnumerator") /* c.responseEnumerator() */
}
