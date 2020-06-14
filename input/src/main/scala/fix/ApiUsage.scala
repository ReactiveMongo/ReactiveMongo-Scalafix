/*
rule = ReactiveMongoUpgrade
*/
package fix

import scala.concurrent.{ ExecutionContext, Future }

import reactivemongo.api.{ MongoDriver, MongoConnection, QueryOpts }
import reactivemongo.api.commands.{ CollStatsResult, WriteConcern }

import reactivemongo.api.commands.{
  CommandError,
  MultiBulkWriteResult,
  UpdateWriteResult
}

import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.collections.bson.BSONCollection

import reactivemongo.api.indexes.Index

import com.github.ghik.silencer.silent

object Commands {
  def collStats(drv: MongoDriver, wc: WriteConcern): Future[CollStatsResult] = ???

  def bulk: Future[MultiBulkWriteResult] = ???

  def update: Future[UpdateWriteResult] = ???

  def handle1(e: Exception): Unit = e match {
    case CommandError.Code(c) =>
      println(s"code = $c")

    case _ =>
  }

  def handle2(e: Exception): Unit = e match {
    case reactivemongo.api.commands.CommandError.Code(c) =>
      println(s"code = $c")

    case _ =>
  }

  def index1: Seq[Index] = Seq(Index(Seq.empty, name = Some("foo")))

  def index2: Index = reactivemongo.api.indexes.Index(
    Seq.empty, name = Some("foo"), false, false, false, version = Some(1))
}

object Drv {
  def connect(d: MongoDriver) = d.connection("mongodb://...")

  def closeCon(con: MongoConnection) = con.askClose()(null)

  @silent
  def conFromStr(uri: String)(implicit ec: ExecutionContext) =
    MongoConnection.parseURI(uri)
}

object Coll {
  import reactivemongo.bson.BSONDocument

  @silent def rename1(coll: GenericCollection[_])(
    implicit
    ec: ExecutionContext) = coll.rename("foo")

  @silent def rename2(coll: BSONCollection)(
    implicit
    ec: ExecutionContext) = coll.rename("foo", false)

  def query1(coll: BSONCollection) = {
    val qry = coll.find(BSONDocument.empty, BSONDocument("bar" -> 1)).
      partial

    val b = qry.sortOption
    val c = qry.projectionOption
    val d = qry.hintOption
    val e = qry.explainFlag
    val f = qry.snapshotFlag
    val g = qry.commentString
    val h = qry.maxTimeMsOption

    if (System.currentTimeMillis() == -1) {
      println(s"b=$b,c=$c,d=$d,e=$e,f=$f,g=$g,h=$h")
    }
  }

  def query2(coll: BSONCollection) = coll.find(
    projection = BSONDocument("lorem" -> 1),
    selector = BSONDocument.empty)

  type QO1 = reactivemongo.api.QueryOpts
  type QO2 = QueryOpts

  def queryOpts1 = QueryOpts(batchSizeN = 100)

  def queryOpts2 = reactivemongo.api.QueryOpts(skipN = 10)

  def queryOpts3 = reactivemongo.api.QueryOpts().skip(10)

  @silent
  def agg1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.aggregateWith1[BSONDocument](explain = true, true) { f =>
      import f._

      val m = Match(BSONDocument("foo" -> 1))
      val _ = m.makePipe

      m -> List(Out("bar"))
    }

  def agg2(coll: BSONCollection) = coll.BatchCommands.AggregationFramework

  def remove1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.remove(BSONDocument("foo" -> 1))

  def remove2(
    coll: BSONCollection,
    wc: WriteConcern)(implicit ec: ExecutionContext) = coll.remove(
    firstMatchOnly = true,
    selector = BSONDocument("foo" -> 1),
    writeConcern = wc)

  def insert1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.insert(BSONDocument("bar" -> 1))

  def insert2(
    coll: BSONCollection,
    wc: WriteConcern)(implicit ec: ExecutionContext) =
    coll.insert[BSONDocument](
      writeConcern = wc,
      document = BSONDocument("bar" -> 1))

  def update1(coll: BSONCollection)(implicit ec: ExecutionContext) =
    coll.update[BSONDocument, BSONDocument](
      BSONDocument("foo" -> 1), BSONDocument("bar" -> "lorem"))

  def update2(coll: BSONCollection, wc: WriteConcern)(
    implicit
    ec: ExecutionContext) = coll.update(
    writeConcern = wc,
    update = BSONDocument("bar" -> "lorem"),
    selector = BSONDocument("foo" -> 1),
    multi = false,
    upsert = true)

  @silent def unboxedCmd1[R <: reactivemongo.api.commands.BoxedAnyVal[Int], C <: reactivemongo.api.commands.Command with reactivemongo.api.commands.CommandWithResult[R]](coll: BSONCollection, cmd: C)(implicit ec: ExecutionContext) = coll.runner.unboxed[Int, R, C](coll.db, cmd, reactivemongo.api.ReadPreference.primary)(???, ???, ec)

  type DeprecatedUnitBox = reactivemongo.api.commands.UnitBox.type

  import reactivemongo.api.commands.{ BoxedAnyVal, UnitBox }
  def withUnitBox(a: UnitBox.type, b: Option[reactivemongo.api.commands.UnitBox.type]) = a -> b

  @silent def valueCmd1[R <: BoxedAnyVal[Int], C <: reactivemongo.api.commands.CollectionCommand with reactivemongo.api.commands.CommandWithResult[R]](coll: BSONCollection, cmd: C with reactivemongo.api.commands.CommandWithResult[R with reactivemongo.api.commands.BoxedAnyVal[Int]])(implicit ec: ExecutionContext) = coll.runValueCommand(cmd, reactivemongo.api.ReadPreference.primary)(???, ???, ec)
}

object Core {
  import reactivemongo.core.actors.Exceptions.{
    ChannelNotFound,
    NodeSetNotReachable
  }

  type CNF = ChannelNotFound
  type NSNR = NodeSetNotReachable

  def handle1(ex: Exception): Unit = ex match {
    case _: reactivemongo.core.actors.Exceptions.ChannelNotFound =>
      ex.printStackTrace()

    case _: reactivemongo.core.actors.Exceptions.NodeSetNotReachable =>
      ex.printStackTrace()

    case _ =>
  }

  def handle2(ex: Exception): Unit = ex match {
    case _: ChannelNotFound | _: NodeSetNotReachable =>
      ex.printStackTrace()

    case _ =>
  }
}
