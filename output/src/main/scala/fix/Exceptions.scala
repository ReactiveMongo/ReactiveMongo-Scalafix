package fix

import reactivemongo.core.errors.{ DatabaseException, ReactiveMongoException }

object Exceptions {
  def handleDetailed1(cause: DatabaseException): Unit = ???

  def handleDetailed2(cause: reactivemongo.core.errors.DatabaseException): Unit = ???

  def handleGenericDB1(cause: DatabaseException): Unit = ???

  def handleGenericDB2(cause: reactivemongo.core.errors.DatabaseException): Unit = ???

  def handleConErr1(cause: ReactiveMongoException): Unit = ???

  def handleConErr2(cause: reactivemongo.core.errors.ReactiveMongoException): Unit = ???

  def handleNotInit1(cause: ReactiveMongoException): Unit = ???

  def handleNotInit2(cause: reactivemongo.core.errors.ReactiveMongoException): Unit = ???

  def handleDriverErr1(cause: ReactiveMongoException): Unit = ???

  def handleDriverErr2(cause: reactivemongo.core.errors.ReactiveMongoException): Unit = ???

  def handleGenericDriverErr1(cause: ReactiveMongoException): Unit = ???

  def handleGenericDriverErr2(cause: reactivemongo.core.errors.ReactiveMongoException): Unit = ???
}
