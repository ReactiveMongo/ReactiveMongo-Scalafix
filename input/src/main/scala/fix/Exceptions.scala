/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.core.errors.{
  DetailedDatabaseException,
  GenericDatabaseException,
  ConnectionException,
  DriverException,
  GenericDriverException,
  ConnectionNotInitialized
}

object Exceptions {
  def handleDetailed1(cause: DetailedDatabaseException): Unit = ???

  def handleDetailed2(cause: reactivemongo.core.errors.DetailedDatabaseException): Unit = ???

  def handleGenericDB1(cause: GenericDatabaseException): Unit = ???

  def handleGenericDB2(cause: reactivemongo.core.errors.GenericDatabaseException): Unit = ???

  def handleConErr1(cause: ConnectionException): Unit = ???

  def handleConErr2(cause: reactivemongo.core.errors.ConnectionException): Unit = ???

  def handleNotInit1(cause: ConnectionNotInitialized): Unit = ???

  def handleNotInit2(cause: reactivemongo.core.errors.ConnectionNotInitialized): Unit = ???

  def handleDriverErr1(cause: DriverException): Unit = ???

  def handleDriverErr2(cause: reactivemongo.core.errors.DriverException): Unit = ???

  def handleGenericDriverErr1(cause: GenericDriverException): Unit = ???

  def handleGenericDriverErr2(cause: reactivemongo.core.errors.GenericDriverException): Unit = ???
}
