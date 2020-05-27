package fix

import scala.concurrent.{ ExecutionContext, Future }

import reactivemongo.api.MongoConnection


import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.bson.collection.BSONCollection

import com.github.ghik.silencer.silent
import reactivemongo.api.{ AsyncDriver, CollectionStats, WriteConcern }
import reactivemongo.api.bson.BSONDocument
import reactivemongo.core.actors.Exceptions.{ ChannelNotFoundException, NodeSetNotReachableException }

object Commands {
  def collStats(drv: AsyncDriver, wc: WriteConcern): Future[CollectionStats] = ???

  def bulk: Future[Any /* MultiBulkWriteResult ~> anyCollection.MultiBulkWriteResult */] = ???
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
