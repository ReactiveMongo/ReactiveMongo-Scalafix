package fix

import scala.concurrent.Future

import reactivemongo.api.DB

object LinterUsage {
  val db1 = resolveDB 

  val db2: Future[DB] = db1

  private def resolveDB: Future[DB] = ???
}
