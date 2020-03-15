package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

final class Upgrade extends SemanticRule("ReactiveMongoUpgrade") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    val transformer: PartialFunction[Tree, Patch] =
      coreUpgrade orElse gridfsUpgrade orElse bsonUpgrade orElse streamingUpgrade orElse playUpgrade orElse {
        case _ =>
          //println(s"x = ${x.structure}")
          Patch.empty
      }

    Patch.fromIterable(doc.tree.collect(transformer))
  }

  /* TODO:
   collection.remove[S](selector: S, writeConcern: WriteConcern = writeConcern, firstMatchOnly: Boolean = false)(implicit swriter: pack.Writer[S], ec: ExecutionContext): Future[WriteResult]

update[S, T](selector: S, update: T, writeConcern: WriteConcern = writeConcern, upsert: Boolean = false, multi: Boolean = false)(implicit swriter: pack.Writer[S], writer: pack.Writer[T], ec: ExecutionContext): Future[UpdateWriteResult]

def insert[T](document: T, writeConcern: WriteConcern = writeConcern)(implicit writer: pack.Writer[T], ec: ExecutionContext): Future[WriteResult]

find[S, J](selector: S, projection: J)(implicit swriter: pack.Writer[S], pwriter: pack.Writer[J]) ~> Some(projection)

def aggregateWith1[T](explain: Boolean = false, allowDiskUse: Boolean = false, bypassDocumentValidation: Boolean = false, readConcern: Option[ReadConcern] = None, readPreference: ReadPreference = ReadPreference.primary, batchSize: Option[Int] = None)(f: AggregationFramework => AggregationPipeline)(implicit ec: ExecutionContext, reader: pack.Reader[T], cf: CursorFlattener[Cursor], cp: CursorProducer[T]) ~> aggregateWith

aggregatorContext[T](firstOperator: PipelineOperator, otherOperators: List[PipelineOperator], explain: Boolean, allowDiskUse: Boolean, bypassDocumentValidation: Boolean, readConcern: Option[ReadConcern], readPreference: ReadPreference, batchSize: Option[Int])(implicit reader: pack.Reader[T]): AggregatorContext[T]

AsyncDriver: def connect(nodes: Seq[String], options: MongoConnectionOptions = MongoConnectionOptions.default, authentications: Seq[Authenticate] = Seq.empty, name: Option[String] = None): Future[MongoConnection]

MongoDriver => AsyncDriver

MongoConnection.askClose => close

MongoConnection.parseURL => fromString

MongoConnection.def authenticate(db: String, user: String, password: String): Future[SuccessfulAuthentication] + failoverStrategy

Collection.rename => coll.db.renameCollection(coll.name, ...)

DefaultDB => DB
GenericDB => DB
reactivemongo.api.commands.CollStatsResult => CollectionStats

(QueryOps|GenericQueryBuilder).partial ~> GenericQueryBuilder.allowPartialResults

collection.aggregationFramework => AggregationFramework
   */

  // ---

  private def streamingUpgrade(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    case t @ Term.Apply(
      Term.Select(c @ Term.Name(_), Term.Name("responseSource")), _) if (
      c.symbol.info.exists(
        _.signature.toString startsWith "AkkaStreamCursor")) =>
      Patch.replaceTree(t, s"??? /* ${t.syntax}: Use bulkSource */")

    case t @ Term.Apply(
      Term.Select(c @ Term.Name(_), Term.Name("responsePublisher")), _) if (
      c.symbol.info.exists(
        _.signature.toString startsWith "AkkaStreamCursor")) =>
      Patch.replaceTree(t, s"??? /* ${t.syntax}: Use bulkPublisher */")

    case t @ Term.Apply(
      Term.Select(c @ Term.Name(_), Term.Name("responseEnumerator")), _) if (
      c.symbol.info.exists(
        _.signature.toString startsWith "PlayIterateesCursor")) =>
      Patch.replaceTree(t, s"??? /* ${t.syntax}: Use bulkEnumerator */")

  }

  private def coreUpgrade(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    case im @ Importer(
      Term.Select(
        Term.Select(Term.Name("reactivemongo"), Term.Name("core")),
        Term.Name("errors")
        ),
      importees
      ) => {
      val is = importees.map {
        case Importee.Name(
          Name.Indeterminate("DetailedDatabaseException" |
            "GenericDatabaseException")) =>
          "DatabaseException"

        case Importee.Name(
          Name.Indeterminate("ConnectionException" |
            "ConnectionNotInitialized" |
            "DriverException" | "GenericDriverException")) =>
          "ReactiveMongoException"

        case i =>
          i.syntax
      }.distinct.sorted

      val upd = if (is.size > 1) {
        is.mkString("{ ", ", ", " }")
      } else is.mkString

      Patch.replaceTree(im, s"reactivemongo.core.errors.$upd")
    }

    case t @ Type.Name("DetailedDatabaseException" |
      "GenericDatabaseException") if (t.symbol.info.exists(
      _.toString startsWith "reactivemongo/core/errors/")) => {

      Patch.replaceTree(t, "DatabaseException")
    }

    case t @ Type.Name("ConnectionException" |
      "ConnectionNotInitialized" |
      "DriverException" |
      "GenericDriverException") if (t.symbol.info.exists(
      _.toString startsWith "reactivemongo/core/errors/")) => {

      Patch.replaceTree(t, "ReactiveMongoException")
    }
  }

  private def playUpgrade(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    case i @ Importer(
      Term.Name("MongoController"),
      List(Importee.Name(Name.Indeterminate("JsGridFS")))
      ) => Patch.replaceTree(i, "MongoController.GridFS")

    case n @ Type.Name("JsGridFS") =>
      Patch.replaceTree(n, "GridFS")

    case p @ Term.Apply(
      Term.Apply(Term.Name("gridFSBodyParser"), gfs :: _),
      _ :: _ :: mat :: Nil) => {
      if (gfs.symbol.info.map(
        _.signature.toString).exists(_ startsWith "Future[")) {
        Patch.replaceTree(p, s"""gridFSBodyParser($gfs)($mat)""")
      } else {
        Patch.replaceTree(
          p, s"""gridFSBodyParser(Future.successful($gfs))($mat)""")
      }
    }

    case p @ Term.Apply(
      Term.Apply(Term.Name("gridFSBodyParser"), gfs :: _ :: _),
      _ :: _ :: mat :: _ :: Nil) =>
      Patch.replaceTree(
        p, s"""gridFSBodyParser(Future.successful($gfs))($mat)""")

  }

  def bsonUpgrade(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    case i @ Importer(
      Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
      _
      ) => Patch.replaceTree(i, i.syntax.replace(
      "reactivemongo.bson", "reactivemongo.api.bson"))

    case i @ Import(List(Importer(
      s @ Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
      importees
      ))) => {
      var changed = false
      val is = importees.filterNot { i =>
        val n = i.toString
        if (n == "BSONSerializationPack" || n == "_") {
          changed = true

          true
        } else false
      }

      val up = if (is.nonEmpty) {
        s"""import reactivemongo.api.bson.collection.BSONSerializationPack
import ${Importer(s, is).syntax}"""
      } else {
        "import reactivemongo.api.bson.collection.BSONSerializationPack"
      }

      Patch.replaceTree(i, up)
    }

    case t @ Type.Select(
      Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
      Type.Name(n)
      ) =>
      Patch.replaceTree(t, s"reactivemongo.api.bson.$n")

    case t @ Term.Select(
      Term.Select(
        Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
        Term.Name("collections")
        ),
      Term.Name("bson")
      ) => Patch.replaceTree(t, s"reactivemongo.api.bson.collection")

    case v @ Term.Apply(Term.Select(
      Term.Apply(
        Term.ApplyType(
          Term.Select(d @ Term.Name(n), Term.Name("getAs")),
          List(t @ Type.Name("BSONNumberLike" | "BSONBooleanLike"))
          ),
        List(f)
        ),
      Term.Name("map")
      ),
      List(body)
      ) if (d.symbol.info.exists { s =>
      val t = s.signature.toString
      t == "BSONDocument" || t == "BSONArray"
    }) => {
      val b = body match {
        case Term.Select(_: Term.Placeholder, expr) =>
          s"_.${expr.syntax}.toOption"

        case Term.Block(List(Term.Function(List(param), b))) =>
          s"${param.syntax} => (${b.syntax}).toOption"

        case _ =>
          body.syntax
      }

      Patch.replaceTree(v, s"${n}.getAsOpt[${t.syntax}](${f.syntax}).flatMap { $b }")
    }

    case getAs @ Term.Apply(
      Term.ApplyType(
        Term.Select(x @ Term.Name(a), Term.Name("getAs")),
        List(Type.Name(t))
        ),
      List(f)
      ) if (t != "BSONNumberLike" && t != "BSONBooleanLike" &&
      x.symbol.info.exists { i =>
        val s = i.signature.toString

        s == "BSONDocument" || s == "BSONArray"
      }) =>
      Patch.replaceTree(getAs, s"${a}.getAsOpt[${t}]($f)")

    case u @ Term.Apply(
      Term.Select(x @ Term.Name(a), Term.Name("getUnflattenedTry")),
      List(f)
      ) if (x.symbol.info.exists(_.signature.toString == "BSONDocument")) =>
      Patch.replaceTree(u, s"${a}.getAsUnflattenedTry[reactivemongo.api.bson.BSONValue]($f)")
  }

  private def gridfsUpgrade(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    // Extractors

    object GridFSTermName {
      def unapply(t: Term): Boolean = t match {
        case g @ Term.Name("GridFS") =>
          g.symbol.owner.toString == "reactivemongo/api/gridfs/"

        case _ =>
          false
      }
    }

    // ---

    object ReadFileTypeLike {
      def unapply(t: Type): Boolean = t match {
        case Type.Name("BasicMetadata" |
          "ComputedMetadata" | "CustomMetadata" | "ReadFile") =>
          t.symbol.owner.toString == "reactivemongo/api/gridfs/"

        case _ =>
          false
      }
    }

    {
      case gridfsRes @ Term.Apply(
        Term.ApplyType(
          GridFSTermName(),
          List(Type.Singleton(Term.Name("BSONSerializationPack")))
          ),
        List(Term.Name(db))
        ) =>
        Patch.replaceTree(gridfsRes, s"${db}.gridfs")

      case gridfsRes @ Term.Apply(
        Term.ApplyType(
          GridFSTermName(),
          List(Type.Singleton(Term.Name("BSONSerializationPack")))
          ),
        List(Term.Name(db), prefix)
        ) =>
        Patch.replaceTree(gridfsRes, s"${db}.gridfs($prefix)")

      // ---

      case readFileTpe @ Type.Apply(
        ReadFileTypeLike(),
        List(
          Type.Singleton(Term.Name("BSONSerializationPack")),
          Type.Name(idTpe)
          )
        ) =>
        Patch.replaceTree(
          readFileTpe, s"ReadFile[$idTpe, reactivemongo.api.bson.BSONDocument]")

      case save @ Term.Apply(
        Term.Apply(
          Term.Select(g, Term.Name("save" | "saveWithMD5")),
          List(Term.Name(_), Term.Name(file), Term.Name(chunkSize))
          ),
        List(Term.Name(_), Term.Name(ec), Term.Name(_), Term.Name(_))
        ) if (save.symbol.owner.
        toString == "reactivemongo/api/gridfs/GridFS#") =>
        Patch.addRight(save, s" // Consider: ${g}.writeFromInputStream(${file}, _streamNotEnumerator, $chunkSize)($ec)")

      case iteratee @ Term.Apply(
        Term.Apply(
          Term.Select(g, Term.Name("iteratee" | "iterateeWithMD5")),
          List(Term.Name(_), Term.Name(_))
          ),
        List(Term.Name(_), Term.Name(_), Term.Name(_), Term.Name(_))
        ) if (iteratee.symbol.owner.
        toString == "reactivemongo/api/gridfs/GridFS#") =>
        Patch.addRight(iteratee, s" // Consider: ${g}.readToOutputStream or GridFS support in streaming modules")

      case gridfsRm @ Term.Apply(
        Term.Select(Term.Name(gt), Term.Name("remove")),
        List(Term.Name(ref))
        ) if (gridfsRm.symbol.owner.
        toString == "reactivemongo/api/gridfs/GridFS#") =>
        Patch.replaceTree(gridfsRm, s"${gt}.remove(${ref}.id)")

    }
  }
}
