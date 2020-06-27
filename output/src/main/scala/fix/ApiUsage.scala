package fix

import scala.concurrent.{ ExecutionContext, Future }

import reactivemongo.api.MongoConnection


import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.bson.collection.BSONCollection

import reactivemongo.api.indexes.Index

import com.github.ghik.silencer.silent
import reactivemongo.api.{ AsyncDriver, CollectionStats, WriteConcern }
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.commands.CommandException
import reactivemongo.core.actors.Exceptions.{ ChannelNotFoundException, NodeSetNotReachableException }

object Commands {
  def collStats(drv: AsyncDriver, wc: WriteConcern): Future[CollectionStats] = ???

  def bulk: Future[Any /* MultiBulkWriteResult ~> anyCollection.MultiBulkWriteResult */] = ???

  def update: Future[Any /* UpdateWriteResult ~> anyCollection.MultiBulkWriteResult */] = ???

  def handle1(e: Exception): Unit = e match {
    case CommandException.Code(c) =>
      println(s"code = $c")

    case _ =>
  }

  def handle2(e: Exception): Unit = e match {
    case reactivemongo.api.commands.CommandException.Code(c) =>
      println(s"code = $c")

    case _ =>
  }

  def index1: Seq[Index.Default] = Seq(Index(expireAfterSeconds = Option.empty[Int], textIndexVersion = Option.empty[Int], languageOverride = Option.empty[String], bucketSize = Option.empty[Double], key = Seq.empty, weights = None, collation = None, max = Option.empty[Double], sphereIndexVersion = Option.empty[Int], wildcardProjection = None, name = Some("foo"), storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], bits = Option.empty[Int]))

  def index2: Index.Default = reactivemongo.api.indexes.Index(expireAfterSeconds = Option.empty[Int], textIndexVersion = Option.empty[Int], languageOverride = Option.empty[String], bucketSize = Option.empty[Double], key = Seq.empty, unique = false, weights = None, collation = None, max = Option.empty[Double], sphereIndexVersion = Option.empty[Int], wildcardProjection = None, name = Some("foo"), storageEngine = None, min = Option.empty[Double], defaultLanguage = Option.empty[String], background = false, bits = Option.empty[Int], version = Some(1))

  def withLastError1(arg: Option[WriteConcern]): Unit = println(arg.mkString)

  def lastError1(): reactivemongo.api.WriteConcern = ???
}

object Drv {
  def connect(d: AsyncDriver) = d.connect("mongodb://...")

  def closeCon(con: MongoConnection) = con.close()(null)

  @silent
  def conFromStr(uri: String)(implicit ec: ExecutionContext) =
    MongoConnection.fromString(uri)
}

object Coll {

  @silent def rename1(coll: GenericCollection[_])(
    implicit
    ec: ExecutionContext) = coll.db.connection.database("admin").flatMap(_.renameCollection(coll.db.name, coll.name, "foo"))

  @silent def rename2(coll: BSONCollection)(
    implicit
    ec: ExecutionContext) = coll.db.connection.database("admin").flatMap(_.renameCollection(coll.db.name, coll.name, "foo", false))

  def query1(coll: BSONCollection) = {
    val qry = coll.find(BSONDocument.empty, Some(BSONDocument("bar" -> 1))).
      allowPartialResults

    val b = qry.sort
    val c = qry.projection
    val d = qry.hint
    val e = qry.explain
    val f = qry.snapshot
    val g = qry.comment
    val h = qry.maxTimeMs

    if (System.currentTimeMillis() == -1) {
      println(s"b=$b,c=$c,d=$d,e=$e,f=$f,g=$g,h=$h")
    }
  }

  def query2(coll: BSONCollection) = coll.find(selector = BSONDocument.empty, projection = Some(BSONDocument("lorem" -> 1)))

  type QO1 = Nothing /* No longer exists: reactivemongo.api.QueryOpts */
  type QO2 = Nothing /* No longer exists: QueryOpts */

  def queryOpts1 = reactivemongo.api.bson.migrationRequired("Directly use query builder") /* QueryOpts(batchSizeN = 100) */

  def queryOpts2 = reactivemongo.api.bson.migrationRequired("Directly use query builder") /* reactivemongo.api.QueryOpts(skipN = 10) */

  def queryOpts3 = reactivemongo.api.bson.migrationRequired("Directly use query builder") /* reactivemongo.api.QueryOpts().skip(10) */

  @silent
  def agg1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.aggregateWith[BSONDocument](explain = true, true) { f =>
      import f._

      val m = Match(BSONDocument("foo" -> 1))
      val _ = m

      m -> List(Out("bar"))
    }

  def agg2(coll: BSONCollection) = coll.AggregationFramework

  def remove1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.delete.one(q = BSONDocument("foo" -> 1))

  def remove2(
    coll: BSONCollection,
    wc: WriteConcern)(implicit ec: ExecutionContext) = coll.delete(writeConcern = wc).one(q = BSONDocument("foo" -> 1), limit = {
  if (true) Some(1) else None
})

  def insert1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.insert.one(BSONDocument("bar" -> 1))

  def insert2(
    coll: BSONCollection,
    wc: WriteConcern)(implicit ec: ExecutionContext) =
    coll.insert(writeConcern = wc).one[BSONDocument](BSONDocument("bar" -> 1))

  def update1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.update.one[BSONDocument, BSONDocument](q = BSONDocument("foo" -> 1), u = BSONDocument("bar" -> "lorem"))

  def update2(coll: BSONCollection, wc: WriteConcern)(
    implicit
    ec: ExecutionContext) = coll.update(writeConcern = wc).one(q = BSONDocument("foo" -> 1), u = BSONDocument("bar" -> "lorem"), upsert = true, multi = false)

  @silent def unboxedCmd1[R <: Int, C <: reactivemongo.api.commands.Command with reactivemongo.api.commands.CommandWithResult[R]](coll: BSONCollection, cmd: C)(implicit ec: ExecutionContext) = coll.runner.apply[R, C](coll.db, cmd, reactivemongo.api.ReadPreference.primary)(???, ???, ec)

  type DeprecatedUnitBox = Unit

  def withUnitBox(a: Unit, b: Option[Unit]) = a -> b

  @silent def valueCmd1[R <: Int, C <: reactivemongo.api.commands.CollectionCommand with reactivemongo.api.commands.CommandWithResult[R]](coll: BSONCollection, cmd: C with reactivemongo.api.commands.CommandWithResult[R with Int])(implicit ec: ExecutionContext) = coll.runCommand(cmd, reactivemongo.api.ReadPreference.primary)(???, ???, ec)
}

object Core {

  type CNF = ChannelNotFoundException
  type NSNR = NodeSetNotReachableException

  def handle1(ex: Exception): Unit = ex match {
    case _: reactivemongo.core.actors.Exceptions.ChannelNotFoundException =>
      ex.printStackTrace()

    case _: reactivemongo.core.actors.Exceptions.NodeSetNotReachableException =>
      ex.printStackTrace()

    case _ =>
  }

  def handle2(ex: Exception): Unit = ex match {
    case _: ChannelNotFoundException | _: NodeSetNotReachableException =>
      ex.printStackTrace()

    case _ =>
  }
}
