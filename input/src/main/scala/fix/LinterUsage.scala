/*
rule = ReactiveMongoLinter
*/
package fix

import scala.concurrent.Future

import reactivemongo.api.{ DB, DefaultDB }

object LinterUsage {
  val db1 = resolveDB // assert: ReactiveMongoLinter

  val db2: Future[DB] = db1

  private def resolveDB: Future[DefaultDB] = ???
}
