package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

/* TODO:
wrong number of type parameters for method serve: [Id <: reactivemongo.api.bson.BSONValue](gfs: play.modules.reactivemongo.MongoController.GridFS)(foundFile: reactivemongo.api.Cursor[gfs.ReadFile[Id]], dispositionMode: String)(implicit materializer: akka.stream.Materializer)scala.concurrent.Future[play.api.mvc.Result]
[error]         case _            => serve[JsString, JSONReadFile](fs)(file)

serve[BSONValue]
+ import BSONValue
 */

/* TODO: gridfs.find[JsObject, JSONReadFile] ~>
gridfs[BSONDocument, BSONValue]
+ import BSONDocument, BSONValue
 */

/* TODO: MongoController JSONReadFile
 */

/* TODO: value files is not a member of reactivemongo.api.gridfs.GridFS[ */

final class Upgrade extends SemanticRule("ReactiveMongoUpgrade") { self =>
  override def fix(implicit doc: SemanticDocument): Patch =
    Patch.fromIterable(doc.tree.children.map(transformer))

  // ---

  private def transformer(
    implicit
    doc: SemanticDocument): Tree => Patch = {

    val fixes = Seq[Fix](
      coreUpgrade,
      apiUpgrade, gridfsUpgrade, bsonUpgrade, streamingUpgrade, playUpgrade)

    val fixImport: PartialFunction[Importer, Patch] =
      (fixes.foldLeft(PartialFunction.empty[Importer, Patch]) {
        _ orElse _.`import`
      }).orElse({ case _ => Patch.empty })

    val pipeline = fixes.foldLeft[PartialFunction[Tree, PatchDirective]]({
      case Import(importers) =>
        Patch.fromIterable(importers.map(fixImport))

    }) {
      _ orElse _.refactor
    }

    { tree: Tree =>
      def recurse = Patch.fromIterable(tree.children.map(transformer))

      pipeline.lift(tree).map { p =>
        if (p.patch.nonEmpty) {
          p.patch
        } else if (p.recurse && tree.children.nonEmpty) {
          recurse
        } else {
          Patch.empty
        }
      }.getOrElse {
        //println(s"tree = ${tree.structure}")

        if (tree.children.nonEmpty) {
          recurse
        } else {
          Patch.empty
        }
      }
    }
  }

  private def streamingUpgrade(implicit doc: SemanticDocument) = Fix(
    refactor = {
      case t @ Term.Apply(
        Term.Select(c @ Term.Name(_), Term.Name("responseSource")), _) if (
        c.symbol.info.exists(
          _.signature.toString startsWith "AkkaStreamCursor")) =>
        migrationRequired(t, "Use bulkSource")

      case t @ Term.Apply(
        Term.Select(c @ Term.Name(_), Term.Name("responsePublisher")), _) if (
        c.symbol.info.exists(
          _.signature.toString startsWith "AkkaStreamCursor")) =>
        migrationRequired(t, "Use bulkPublisher")

      case t @ Term.Apply(
        Term.Select(c @ Term.Name(_), Term.Name("responseEnumerator")), _) if (
        c.symbol.info.exists(
          _.signature.toString startsWith "PlayIterateesCursor")) =>
        migrationRequired(t, "Use bulkEnumerator")

    })

  private def coreUpgrade(implicit doc: SemanticDocument) = Fix(
    `import` = {
      case Importer(
        s @ Term.Select(
          Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("core")),
            Term.Name("actors")), Term.Name("Exceptions")
          ), importees) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(n @ Name.Indeterminate(
            "ChannelNotFound" | "NodeSetNotReachable")) =>
            val nme = Name.Indeterminate(s"${n.syntax}Exception")

            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(s, List(importee"${nme}")))).atomic

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }

      case Importer(
        s @ Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("core")),
          Term.Name("errors")
          ),
        importees
        ) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(
            Name.Indeterminate("DetailedDatabaseException" |
              "GenericDatabaseException")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(s, List(importee"DatabaseException")))).atomic

          case i @ Importee.Name(
            Name.Indeterminate("ConnectionException" |
              "ConnectionNotInitialized" |
              "DriverException" | "GenericDriverException")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(s, List(importee"ReactiveMongoException")))).atomic

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
      case t @ Type.Name(n @ ("ChannelNotFound" | "NodeSetNotReachable")) if (
        testSym(t)(_ startsWith "reactivemongo/core/actors/Exceptions")) =>
        Patch.replaceTree(t, s"${n}Exception")

      case t @ Type.Name("DetailedDatabaseException" |
        "GenericDatabaseException") if (testSym(t)(
        _ startsWith "reactivemongo/core/errors/")) =>
        Patch.replaceTree(t, "DatabaseException")

      case t @ Type.Name("ConnectionException" |
        "ConnectionNotInitialized" |
        "DriverException" |
        "GenericDriverException") if (testSym(t)(
        _ startsWith "reactivemongo/core/errors/")) =>
        Patch.replaceTree(t, "ReactiveMongoException")
    })

  private object QueryBuilderNaming {
    private val mapping = Map(
      "partial" -> "allowPartialResults",
      "sortOption" -> "sort",
      "projectionOption" -> "projection",
      "hintOption" -> "hint",
      "explainFlag" -> "explain",
      "snapshotFlag" -> "snapshot",
      "commentString" -> "comment",
      "maxTimeMsOption" -> "maxTimeMs")

    def unapply(name: String): Option[String] = mapping.get(name)
  }

  private final class InsertExtractor(implicit doc: SemanticDocument) {
    def unapply(tree: Tree): Option[Tuple3[Option[Type], Term, List[Term]]] = tree match {
      case t @ Term.Apply(
        Term.Select(c, Term.Name("insert")), args) if (
        t.symbol.info.exists { i =>
          i.signature match {
            case MethodSignature(_, List(a, b) :: _, _) if {
              i.toString.startsWith(
                "reactivemongo/api/collections/GenericCollection") &&
                a.symbol.info.exists(_.displayName == "document") &&
                b.symbol.info.exists(_.displayName == "writeConcern")
            } => true

            case _ =>
              false
          }
        }) =>
        Some((Option.empty[Type], c, args))

      case t @ Term.Apply(Term.ApplyType(
        Term.Select(c, Term.Name("insert")), List(tpe)), args) if (
        t.symbol.info.exists { i =>
          i.signature match {
            case MethodSignature(_, List(a, b) :: _, _) if {
              i.toString.startsWith(
                "reactivemongo/api/collections/GenericCollection") &&
                a.symbol.info.exists(_.displayName == "document") &&
                b.symbol.info.exists(_.displayName == "writeConcern")
            } => true

            case _ =>
              false
          }
        }) =>
        Some((Some(tpe), c, args))

      case _ =>
        None
    }
  }

  private final class UpdateExtractor(implicit doc: SemanticDocument) {
    def unapply(tree: Tree): Option[Tuple3[List[Type], Term, List[Term]]] = tree match {
      case t @ Term.Apply(Term.ApplyType(
        Term.Select(c, Term.Name("update")), tpeParams), args) if (
        t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/collections/")) =>
        Some(Tuple3(tpeParams, c, args))

      case t @ Term.Apply(
        Term.Select(c, Term.Name("update")), args) if (
        t.symbol.info.exists { i =>
          i.signature match {
            case MethodSignature(_, funArgs :: _, _) =>
              (funArgs.size > 2 && i.toString.startsWith(
                "reactivemongo/api/collections/GenericCollection"))

            case _ =>
              false
          }
        }) =>
        Some(Tuple3(List.empty[Type], c, args))

      case _ =>
        None
    }
  }

  private def apiUpgrade(implicit doc: SemanticDocument) = Fix(
    `import` = {
      case Importer(
        cmdPkg @ Term.Select(
          apiPkg @ Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")
          ),
        importees
        ) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("CollStatsResult")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(importee"CollectionStats")))).atomic

          case i @ Importee.Name(Name.Indeterminate("CommandError")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(cmdPkg, List(importee"CommandException")))).atomic

          case i @ Importee.Name(Name.Indeterminate("LastError")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(cmdPkg, List(importee"WriteResult")))).atomic

          case i @ Importee.Name(Name.Indeterminate(
            "GetLastError" | "WriteConcern")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(importee"WriteConcern")))).atomic

          case i @ Importee.Name(
            Name.Indeterminate(
              "BoxedAnyVal" | "MultiBulkWriteResult" |
              "UpdateWriteResult" | "UnitBox")) =>
            patches += Patch.removeImportee(i)

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }

      case Importer(
        apiPkg @ Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
        importees
        ) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("BSONSerializationPack")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(
                Term.Select(Term.Select(
                  apiPkg,
                  Term.Name("bson")), Term.Name("collection")),
                List(importee"BSONSerializationPack")))).atomic

          case i @ Importee.Name(Name.Indeterminate(
            "DefaultDB" | "GenericDB")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(importee"DB")))).atomic

          case i @ Importee.Name(Name.Indeterminate("MongoDriver")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(importee"AsyncDriver")))).atomic

          case i @ Importee.Name(Name.Indeterminate("QueryOpts")) =>
            patches += Patch.removeImportee(i)

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
      val Insert = new InsertExtractor
      val Update = new UpdateExtractor

      {
        case t @ Term.Apply(n @ (Term.Name("Index") | Term.Select(
          Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
            Term.Name("indexes")
            ),
          Term.Name("Index")
          )), args) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/indexes/Index")) => {

          val orderedArgs = Seq(
            "key", "name", "unique", "background", "dropDups", "sparse",
            "version", "partialFilter", "options")

          val argMap = scala.collection.mutable.Map.empty[String, Term]

          args.zipWithIndex.foreach {
            case (Term.Assign(Term.Name(nme), a), _) =>
              argMap.put(nme, a)

            case (a, idx) =>
              argMap.put(orderedArgs(idx), a)
          }

          argMap.remove("dropDups")

          argMap ++= Map[String, Term](
            "expireAfterSeconds" -> q"Option.empty[Int]",
            "storageEngine" -> q"None",
            "weights" -> q"None",
            "defaultLanguage" -> q"Option.empty[String]",
            "languageOverride" -> q"Option.empty[String]",
            "textIndexVersion" -> q"Option.empty[Int]",
            "sphereIndexVersion" -> q"Option.empty[Int]",
            "bits" -> q"Option.empty[Int]",
            "min" -> q"Option.empty[Double]",
            "max" -> q"Option.empty[Double]",
            "bucketSize" -> q"Option.empty[Double]",
            "collation" -> q"None",
            "wildcardProjection" -> q"None")

          val argSyntax = argMap.map {
            case (nme, term) => s"${nme} = ${term.syntax}"
          }.mkString(", ")

          Patch.replaceTree(t, s"${n.syntax}($argSyntax)")
        }

        // ---

        case t @ Type.Select(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Type.Name("LastError")) =>
          Patch.replaceTree(t, "reactivemongo.api.commands.WriteResult")

        case t @ Type.Name("LastError") if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/LastError")) =>
          Patch.replaceTree(t, "WriteResult")

        // ---

        case t @ Type.Select(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Type.Name("GetLastError")) =>
          Patch.replaceTree(t, "reactivemongo.api.WriteConcern")

        case t @ Type.Name("GetLastError") if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/GetLastError")) =>
          Patch.replaceTree(t, "WriteConcern")

        // ---

        case t @ Type.Name("Index") if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/indexes/Index")) =>
          Patch.replaceTree(t, "Index.Default")

        case t @ Term.Name("CommandError") if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/CommandError")) =>
          Patch.replaceTree(t, "CommandException")

        // ---

        case t @ (Type.Name("QueryOpts") | Type.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("api")),
          Type.Name("QueryOpts"))) =>
          Patch.replaceTree(t, s"Nothing /* No longer exists: ${t.syntax} */")

        case t @ Term.Apply(_, _) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/QueryOpts")) =>
          migrationRequired(t, "Directly use query builder") -> false

        // ---

        case t @ Term.Apply(
          Term.Select(c, Term.Name("runValueCommand")), args) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/GenericCollectionWithCommands")) =>
          Patch.replaceTree(t, s"""${c.syntax}.runCommand(${args.map(_.syntax) mkString ", "})""")

        case t @ Type.Apply(Type.Select(Term.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Type.Name("BoxedAnyVal")),
          List(tpe)) =>
          Patch.replaceTree(t, tpe.syntax)

        case t @ Type.Apply(Type.Name("BoxedAnyVal"), List(tpe)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/api/commands/BoxedAnyVal")) =>
          Patch.replaceTree(t, tpe.syntax)

        case t @ Type.Singleton(Term.Name("UnitBox")) =>
          Patch.replaceTree(t, "Unit")

        case t @ Type.Singleton(Term.Select(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Term.Name("UnitBox"))) =>
          Patch.replaceTree(t, "Unit")

        case t @ Term.ApplyType(Term.Select(
          s, Term.Name("unboxed")), List(_, r, c)) if (
          t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/Command.CommandWithPackRunner")) =>
          Patch.replaceTree(t, s"${s.syntax}.apply[${r.syntax}, ${c.syntax}]")

        case t @ Update((tpeParams, c, args)) => {
          val appTArgs: String = tpeParams match {
            case q :: u :: Nil => s"[$q, $u]"
            case _ => ""
          }

          val orderedArgs = Seq(
            "selector", "update", "writeConcern", "upsert", "multi")

          val argMap = scala.collection.mutable.Map.empty[String, Term]

          args.zipWithIndex.foreach {
            case (Term.Assign(Term.Name(nme), a), _) =>
              argMap.put(nme, a)

            case (a, idx) =>
              argMap.put(orderedArgs(idx), a)
          }

          val builderApply: String = argMap.get("writeConcern").fold("") { wc =>
            s"(writeConcern = ${wc.syntax})"
          }

          val oneArgNames = Seq(
            "selector" -> "q",
            "update" -> "u",
            "upsert" -> "upsert",
            "multi" -> "multi")

          val oneArgs = oneArgNames.flatMap {
            case (n, r) => argMap.get(n).map { v =>
              s"$r = ${v.syntax}"
            }.toSeq
          }.mkString("(", ", ", ")")

          Patch.replaceTree(
            t, s"${c.syntax}.update${builderApply}.one${appTArgs}${oneArgs}")

        }

        // ---

        case t @ Insert((tpe, c, args)) => {
          val appTArg = tpe.fold("") { at => s"[${at}]" }

          args match {
            case document :: Nil => document match {
              case Term.Assign(_, doc) =>
                Patch.replaceTree(
                  t, s"${c.syntax}.insert.one${appTArg}($doc)")

              case _ =>
                Patch.replaceTree(
                  t, s"${c.syntax}.insert.one${appTArg}($document)")
            }

            case List(a, b) => {
              val (document, writeConcern) = a match {
                case Term.Assign(Term.Name("writeConcern"), wc) =>
                  b -> wc

                case _ => b match {
                  case Term.Assign(_, doc) =>
                    doc -> a

                  case _ =>
                    b -> a
                }
              }

              val docArg = document match {
                case Term.Assign(_, doc) => doc
                case _ => document
              }

              Patch.replaceTree(t, s"${c.syntax}.insert(writeConcern = $writeConcern).one${appTArg}($docArg)")
            }
          }
        }

        // ---

        case t @ Term.Apply(Term.Select(
          c, Term.Name("remove")), args) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/collections/GenericCollection")) => {
          val orderedArgs = Seq("selector", "writeConcern", "firstMatchOnly")

          val argMap = scala.collection.mutable.Map.empty[String, Term]

          args.zipWithIndex.foreach {
            case (Term.Assign(Term.Name(nme), a), _) =>
              argMap.put(nme, a)

            case (a, idx) =>
              argMap.put(orderedArgs(idx), a)
          }

          val delete = argMap.get("writeConcern") match {
            case Some(wc) =>
              s"${c.syntax}.delete(writeConcern = ${wc.syntax})"

            case _ =>
              s"${c.syntax}.delete"
          }

          val oneArgs = Seq.newBuilder[Term]

          argMap.get("selector").foreach { q =>
            oneArgs += q"q = ${q}"
          }

          argMap.get("firstMatchOnly").foreach { a =>
            oneArgs += q"limit = { if (${a}) Some(1) else None }"
          }

          Patch.replaceTree(
            t, s"${delete}.one(${oneArgs.result().map(_.syntax).mkString(", ")})")
        }

        // ---

        case t @ Term.Apply(
          Term.ApplyType(
            x @ Term.Select(Term.Name(c), Term.Name("aggregateWith1")),
            typeArgs
            ),
          args
          ) if (x.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/GenericCollection")) => {
          val refactored = Term.Apply(Term.ApplyType(
            Term.Select(Term.Name(c), Term.Name("aggregateWith")),
            typeArgs), args)

          Patch.replaceTree(t, refactored.syntax)
        }

        case t @ Term.Select(op @ Term.Name(_), Term.Name("makePipe")) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/AggregationFramework")) =>
          Patch.replaceTree(t, op.syntax)

        case t @ Term.Select(
          Term.Select(c, Term.Name("BatchCommands")),
          Term.Name("AggregationFramework")
          ) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/bson/BSONBatchCommands")) =>
          Patch.replaceTree(t, s"${c.syntax}.AggregationFramework")

        case t @ Term.Select(c, Term.Name("aggregationFramework")) =>
          Patch.replaceTree(t, s"${c.syntax}.AggregationFramework")

        // ---

        case t @ Term.Name(QueryBuilderNaming(newName)) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/GenericQueryBuilder")) =>
          Patch.replaceTree(t, newName)

        case t @ Term.Apply(
          Term.Select(c, Term.Name("find")),
          List(a, b)) if (t.symbol.info.exists { i =>
          val sym = i.toString

          sym.startsWith("reactivemongo/api/collections/GenericCollection") &&
            sym.indexOf("projection: J") != -1
        }) => {
          val (selector, projection): (Term, Term) = a match {
            case Term.Assign(n @ Term.Name("projection"), arg) =>
              b -> Term.Assign(n, q"Some(${arg})")

            case _ => a -> (b match {
              case Term.Assign(n @ Term.Name("projection"), arg) =>
                Term.Assign(n, q"Some(${arg})")

              case _ =>
                q"Some($b)"
            })
          }

          Patch.replaceTree(t, s"${c.syntax}.find($selector, $projection)")
        }

        // ---

        case t @ Term.Apply(
          Term.Select(c, Term.Name("rename")), args) if (
          t.symbol.info.exists {
            _.toString startsWith "reactivemongo/api/CollectionMetaCommands"
          }) => {
          val tn = Term.fresh("coll")

          Patch.replaceTree(t, s"""{ val ${tn.syntax} = ${c.syntax}; ${tn.syntax}.db.connection.database("admin").flatMap(_.renameCollection(${tn.syntax}.db.name, ${tn.syntax}.name, ${args.map(_.syntax).mkString(", ")})) }""")
        }

        // ---

        case t @ Term.Select(d,
          Term.Name("connect" | "connection")) if (t.symbol.info.exists { x =>
          val sym = x.toString
          sym.startsWith("reactivemongo/api/MongoDriver") || sym.startsWith(
            "reactivemongo/api/AsyncDriver")
        }) =>
          Patch.replaceTree(t, s"${d.syntax}.connect")

        // ---

        case t @ Term.Select(c, Term.Name(m)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/api/MongoConnection")) =>
          m match {
            case "askClose" =>
              Patch.replaceTree(t, s"${c.syntax}.close")

            case "parseURI" =>
              Patch.replaceTree(t, s"${c.syntax}.fromString")

            case _ =>
              Patch.empty
          }

        // ---

        case t @ Type.Name("CollStatsResult") if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/commands/")) =>
          Patch.replaceTree(t, "CollectionStats")

        // ---

        case t @ Type.Name("DefaultDB" | "GenericDB") if (
          t.symbol.info.exists(_.toString startsWith "reactivemongo/api/")) =>
          Patch.replaceTree(t, "DB")

        case t @ Type.Name("MongoDriver") if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/")) =>
          Patch.replaceTree(t, "AsyncDriver")

        case t @ Type.Name(n @ (
          "MultiBulkWriteResult" | "UpdateWriteResult")) if (
          t.symbol.toString startsWith "reactivemongo/api/commands/") =>
          Patch.replaceTree(t, s"Any /* ${n} ~> anyCollection.MultiBulkWriteResult */")
      }
    })

  private def playUpgrade(implicit doc: SemanticDocument) = Fix(
    `import` = {
      case Importer(
        x @ Term.Name("MongoController"), importees
        ) if (x.symbol.toString startsWith "play/modules/reactivemongo/") => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("JsGridFS")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(x, List(importee"GridFS")))).atomic

          case i @ Importee.Name(Name.Indeterminate("readFileReads")) =>
            patches += Patch.removeImportee(i)

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }

      case Importer(
        Term.Select(
          Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("play")),
            Term.Name("json")
            ),
          Term.Name("collection")
          ),
        importees) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("JSONCollection")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(
                q"reactivemongo.api.bson.collection",
                List(importee"BSONCollection")))).atomic

          case i =>
            patches += Patch.removeImportee(i)
        }

        Patch.fromIterable(patches.result())
      }

      case Importer(
        Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("play")),
          Term.Name("json")
          ), importees) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("JSONSerializationPack")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(
                q"reactivemongo.api.bson.collection",
                List(importee"BSONSerializationPack")))).atomic

          case i @ Importee.Name(Name.Indeterminate("BSONFormats")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(
                q"reactivemongo.play.json.compat",
                List(importee"_")))).atomic

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
      case t @ Init(Type.Name("MongoController"), _, _) if (t.symbol.info.exists(_.toString startsWith "play/modules/reactivemongo/MongoController")) =>
        (Patch.addGlobalImport(Importer(
          q"reactivemongo.play.json.compat", List(importee"_"))) + Patch.
          addGlobalImport(Importer(
            q"reactivemongo.play.json.compat.json2bson", List(importee"_")))).
          atomic

      case t @ Term.Name("CONTENT_DISPOSITION_INLINE") if (t.symbol.info.exists(_.toString startsWith "play/modules/reactivemongo/MongoController#CONTENT_DISPOSITION_INLINE")) =>
        Patch.replaceTree(t, """"inline"""")

      case n @ Type.Name("JSONCollection") if (n.symbol.info.exists(
        _.toString startsWith "reactivemongo/play/json/collection/")) =>
        (Patch.replaceTree(n, "BSONCollection") + Patch.addGlobalImport(
          Importer( // In case JSONCollection was coming from _ import
            q"reactivemongo.api.bson.collection",
            List(importee"BSONCollection")))).atomic

      case t @ Term.Select(Term.Select(Term.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("play")),
        Term.Name("json")), Term.Name("collection")),
        Term.Name("JSONCollection")) =>
        Patch.replaceTree(t, "reactivemongo.api.bson.collection.BSONCollection")

      case n @ Type.Singleton(Term.Name("JSONSerializationPack")) =>
        (Patch.replaceTree(
          n, "BSONSerializationPack.type") + Patch.addGlobalImport(
          Importer( // In case JSONSerializationPack was coming from _ import
            q"reactivemongo.api.bson.collection",
            List(importee"BSONSerializationPack")))).atomic

      case t @ Term.Select(Term.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("play")),
        Term.Name("json")), Term.Name("JSONSerializationPack")) =>
        Patch.replaceTree(
          t, "reactivemongo.api.bson.collection.BSONSerializationPack")

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
            p, s"gridFSBodyParser(Future.successful($gfs))($mat)")
        }
      }

      case p @ Term.Apply(
        Term.Apply(Term.Name("gridFSBodyParser"), gfs :: _ :: _),
        _ :: _ :: mat :: _ :: Nil) =>
        Patch.replaceTree(
          p, s"gridFSBodyParser(Future.successful($gfs))($mat)")

      // ---

      case t @ Term.Select(Term.Name("BSONFormats"), Term.Name(n)) if (
        t.symbol.info.exists(
          _.toString startsWith "reactivemongo/play/json")) => {
        if (n == "toJSON") {
          Patch.replaceTree(t, "ValueConverters.fromValue")
        } else if (n == "toBSON") {
          Patch.replaceTree(t, "ValueConverters.toValue")
        } else {
          Patch.empty
        }
      }

      case t @ Term.Select(Term.Select(Term.Select(Term.Select(Term.Name(
        "reactivemongo"), Term.Name("play")),
        Term.Name("json")), Term.Name("BSONFormats")), Term.Name(n)) => {
        if (n == "toJSON") {
          Patch.replaceTree(t, "reactivemongo.play.json.compat.fromValue")
        } else if (n == "toBSON") {
          Patch.replaceTree(t, "reactivemongo.play.json.compat.toValue")
        } else {
          Patch.empty
        }
      }
    })

  private class AfterWriteExtractor(implicit doc: SemanticDocument) {
    def unapply(tree: Tree): Option[Term] = {
      if (!tree.symbol.info.exists(_.toString startsWith "reactivemongo/bson/BSONWriter")) {
        None
      } else tree match {
        case Term.Select(w, Term.Name("afterWrite")) =>
          Some(w)

        case Term.ApplyType(Term.Select(w, Term.Name("afterWrite")), List(_)) =>
          Some(w)

        case _ =>
          None
      }
    }
  }

  private def bsonUpgrade(implicit doc: SemanticDocument) = {
    val apiPkg = Term.Select(Term.Select(
      Term.Name("reactivemongo"), Term.Name("api")), Term.Name("bson"))

    val AfterWrite = new AfterWriteExtractor

    Fix(
      `import` = {
        case i @ Importer(Term.Name("Macros"), is) if (
          i.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/Macros")) => {
          val m = Term.Select(apiPkg, Term.Name("Macros"))

          Patch.fromIterable(is.map {
            case i @ Importee.Name(Name.Indeterminate("Annotations")) =>
              Patch.removeImportee(i)

            case i =>
              (Patch.removeImportee(i) + Patch.addGlobalImport(
                Importer(m, List(i)))).atomic
          })
        }

        case i @ Importer(Term.Select(Term.Name("Macros"), m), is) if (
          i.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/Macros")) => {
          val pkg = Term.Select(Term.Select(apiPkg, Term.Name("Macros")), m)

          Patch.fromIterable(is.map { i =>
            (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(pkg, List(i)))).atomic
          })
        }

        case i @ Importer(n @ Term.Name("Annotations"), is) if (
          i.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/Macros.Annotations")) => {
          val m = Term.Select(Term.Select(apiPkg, Term.Name("Macros")), n)

          Patch.fromIterable(is.map { i =>
            (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(m, List(i)))).atomic
          })
        }

        // ---

        case Importer(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")), is) => {

          Patch.fromIterable(is.map {
            case i @ Importee.Name(Name.Indeterminate("DefaultBSONHandlers")) =>
              Patch.removeImportee(i)

            case i =>
              (Patch.removeImportee(i) + Patch.addGlobalImport(
                Importer(apiPkg, List(i)))).atomic
          })
        }

        case Importer(Term.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")),
          Term.Name("DefaultBSONHandlers")), is) => {

          Patch.fromIterable(is.map { i =>
            (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(i)))).atomic
          })
        }

        case Importer(Term.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")), x), is) => {

          val pkg = Term.Select(apiPkg, x)

          Patch.fromIterable(is.map { i =>
            (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(pkg, List(i)))).atomic
          })
        }
      },
      refactor = {
        case t @ Term.Select(companion @ Term.Select(Term.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Term.Name("WriteResult")
          ), Term.Name("lastError")) =>
          Patch.replaceTree(t, s"${companion.syntax}.Exception.unapply")

        case t @ Term.Select(Term.Name(
          "WriteResult"), Term.Name("lastError")) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/commands/WriteResult")) =>
          Patch.replaceTree(t, s"WriteResult.Exception.unapply")

        // ---

        case Term.Apply(aw @ AfterWrite(w),
          List(body @ Term.PartialFunction(_))) if (
          w.symbol.info.exists(
            _.toString.indexOf("BSONDocumentWriter") == -1)) => {
          val basePatch = Patch.replaceTree(aw, s"${w.syntax}.afterWrite")
          val bodyPatch = transformer(doc)(body)

          if (bodyPatch.nonEmpty) {
            (basePatch + bodyPatch).atomic
          } else {
            basePatch
          }
        }

        case Term.Apply(aw @ AfterWrite(w), List(Term.Block(
          List(Term.Function(List(p @ Term.Param(_, _, _, _)), body))))) if (
          w.symbol.info.exists(
            _.toString.indexOf("BSONDocumentWriter") == -1)) => {

          val basePatch = (Patch.replaceTree(
            aw, s"${w.syntax}.afterWrite") + Patch.replaceTree(
            p, s"case ${p.syntax}"))

          val bodyPatch = transformer(doc)(body)

          if (bodyPatch.nonEmpty) {
            (basePatch + bodyPatch).atomic
          } else {
            basePatch.atomic
          }
        }

        // ---

        case t @ Term.Select(v, Term.Name("as")) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/bson/BSON")) =>
          Patch.replaceTree(t, s"${v.syntax}.asOpt")

        // ---

        case t @ Term.Select(r, Term.Name("read")) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/bson/BSON")) =>
          Patch.replaceTree(t, s"${r.syntax}.readTry")

        case t @ Term.Select(r, Term.Name("write")) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/bson/BSON")) =>
          Patch.replaceTree(t, s"${r.syntax}.writeTry")

        // ---

        case Term.Apply(n @ (Term.Name("BSONHandler") | Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
          Term.Name("BSONHandler"))), List(r, w)) => {

          val safe = r.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONReader#read")

          val factory: String = n match {
            case Term.Name("BSONHandler") => "BSONHandler"
            case _ => "reactivemongo.api.bson.BSONHandler"
          }

          if (safe) {
            (transformer(doc)(r) + transformer(doc)(w) + Patch.replaceTree(
              n, s"${factory}.from")).atomic

          } else {
            Patch.replaceTree(n, s"${factory}")
          }
        }

        case Term.Apply(
          t @ Term.ApplyType(n @ (Term.Name("BSONHandler") | Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
            Term.Name("BSONHandler"))), List(_, tpe)), List(r, w)) => {

          val safe = r.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONReader#read")

          val factory: String = n match {
            case Term.Name("BSONHandler") => "BSONHandler"
            case _ => "reactivemongo.api.bson.BSONHandler"
          }

          if (safe) {
            (transformer(doc)(r) + transformer(doc)(w) + Patch.replaceTree(
              t, s"${factory}.from[${tpe}]")).atomic

          } else {
            Patch.replaceTree(t, s"${factory}[${tpe}]")
          }
        }

        case Term.Apply(
          t @ Term.ApplyType(n @ (Term.Name(
            "BSONDocumentHandler") | Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
            Term.Name("BSONDocumentHandler"))),
            List(tpe)), List(r, w)) => {

          val safe = r.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONReader#read")

          val factory: String = n match {
            case Term.Name("BSONDocumentHandler") => "BSONDocumentHandler"
            case _ => "reactivemongo.api.bson.BSONDocumentHandler"
          }

          if (safe) {
            (transformer(doc)(r) + transformer(doc)(w) + Patch.replaceTree(
              t, s"${factory}.from[${tpe}]")).atomic

          } else {
            Patch.replaceTree(t, s"${factory}[${tpe}]")
          }
        }

        case Term.Apply(n @ (Term.Name(
          "BSONDocumentHandler") | Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
          Term.Name("BSONDocumentHandler"))), List(r, w)) => {

          val safe = r.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONReader#read")

          val factory: String = n match {
            case Term.Name("BSONDocumentHandler") => "BSONDocumentHandler"
            case _ => "reactivemongo.api.bson.BSONDocumentHandler"
          }

          if (safe) {
            (transformer(doc)(r) + transformer(doc)(w) + Patch.replaceTree(
              n, s"${factory}.from")).atomic

          } else {
            Patch.replaceTree(n, factory)
          }
        }

        // ---

        case t @ Term.ApplyType(
          Term.Name("BSONReader"), List(_, tpe)) =>
          Patch.replaceTree(t, s"BSONReader[${tpe.syntax}]")

        case t @ Term.ApplyType(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
          Term.Name("BSONReader")), List(_, tpe)) =>
          Patch.replaceTree(
            t, s"reactivemongo.api.bson.BSONReader[${tpe.syntax}]")

        // ---

        case t @ Term.ApplyType(
          Term.Name("BSONWriter"), List(tpe, _)) =>
          Patch.replaceTree(t, s"BSONWriter[${tpe.syntax}]")

        case t @ Term.ApplyType(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
          Term.Name("BSONWriter")), List(tpe, _)) =>
          Patch.replaceTree(
            t, s"reactivemongo.api.bson.BSONWriter[${tpe.syntax}]")

        // ---

        case t @ Type.Apply(Type.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")),
          n @ Type.Name("BSONHandler" | "BSONReader")), List(_, tpe)) =>
          Patch.replaceTree(t, s"reactivemongo.api.bson.$n[${tpe.syntax}]")

        case t @ Type.Apply(Type.Name(
          n @ ("BSONHandler" | "BSONReader")), List(_, tpe)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSON")) =>
          Patch.replaceTree(t, s"${n}[${tpe.syntax}]")

        // ---

        case t @ Type.Apply(Type.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")),
          Type.Name(n @ "BSONWriter")), List(tpe, _)) =>
          Patch.replaceTree(t, s"reactivemongo.api.bson.${n}[${tpe.syntax}]")

        case t @ Type.Apply(n @ Type.Name("BSONWriter"),
          List(tpe, _)) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/bson/BSON")) =>
          Patch.replaceTree(t, s"${n}[${tpe.syntax}]")

        // ---

        case t @ Term.Select(Term.Name("DefaultBSONHandlers"), _) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/DefaultBSONHandlers")) =>
          migrationRequired(t, "DefaultBSONHandlers is no longer public; Rather use implicit resolution.")

        case t @ Term.Name("DefaultBSONHandlers") if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/DefaultBSONHandlers")) =>
          migrationRequired(t, "DefaultBSONHandlers is no longer public; Rather use implicit resolution.")

        // ---

        case t @ Type.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("bson")), Type.Name(n)
          ) if (!Seq("BSONReader", "BSONHandler", "BSONWriter").contains(n)) =>
          Patch.replaceTree(t, s"reactivemongo.api.bson.$n")

        case t @ Term.Select(
          Term.Select(
            Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
            Term.Name("collections")
            ), Term.Name("bson")) =>
          Patch.replaceTree(t, s"reactivemongo.api.bson.collection")

        case v @ Term.Apply(Term.Select(
          Term.Apply(
            Term.ApplyType(
              Term.Select(d, Term.Name("getAs")),
              List(t @ Type.Name("BSONNumberLike" | "BSONBooleanLike"))
              ),
            List(f)
            ),
          Term.Name("map")
          ),
          List(body)
          ) if (d.symbol.info.exists { i =>
          val t = i.toString
          val s = i.signature.toString

          s == "BSONDocument" || s == "BSONArray" ||
            t.startsWith("reactivemongo/bson/BSONDocument") ||
            t.startsWith("reactivemongo/bson/BSONArray")
        }) => {
          val b = body match {
            case Term.Select(_: Term.Placeholder, expr) =>
              s"_.${expr.syntax}.toOption"

            case Term.Block(List(Term.Function(List(param), b))) =>
              s"${param.syntax} => (${b.syntax}).toOption"

            case _ =>
              body.syntax
          }

          val pd = transformer(doc)(d)

          if (pd.nonEmpty) {
            (pd + Patch.replaceTree(
              v, s".getAsOpt[${t.syntax}](${f.syntax}).flatMap { $b }")).atomic
          } else {
            Patch.replaceTree(
              v, s"${d.syntax}.getAsOpt[${t.syntax}](${f.syntax}).flatMap { $b }")
          }
        }

        case getAs @ Term.Apply(Term.ApplyType(Term.Select(
          d, Term.Name("getAs")), List(t)), List(nme)) if (
          t != "BSONNumberLike" && t != "BSONBooleanLike" &&
          getAs.symbol.info.exists { i =>
            val s = i.toString

            s.startsWith("reactivemongo/bson/BSONDocument") || s.
              startsWith("reactivemongo/bson/BSONArray")
          }) => {
          val pd = transformer(doc)(d)

          if (pd.nonEmpty) {
            (pd + Patch.replaceTree(getAs, s".getAsOpt[${t}]($nme)")).atomic
          } else {
            Patch.replaceTree(getAs, s"${d.syntax}.getAsOpt[${t}]($nme)")
          }
        }

        case u @ Term.Apply(
          Term.Select(x, Term.Name("getUnflattenedTry")),
          List(f)
          ) if (x.symbol.info.exists(_.signature.toString == "BSONDocument")) =>
          Patch.replaceTree(u, s"${x.syntax}.getAsUnflattenedTry[reactivemongo.api.bson.BSONValue]($f)")

        case t @ Term.ApplyInfix(
          d, Term.Name(":~"), List(), List(arg)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONDocument")) =>
          Patch.replaceTree(t, s"(${d.syntax} ++ ${arg.syntax})")

        case t @ Term.ApplyInfix(
          arg, Term.Name("~:"), List(), List(d)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/bson/BSONDocument")) =>
          Patch.replaceTree(t, s"(BSONDocument(${arg.syntax}) ++ ${d.syntax})")

      })
  }

  private def gridfsUpgrade(implicit doc: SemanticDocument) = Fix(
    `import` = {
      case Importer(
        Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("gridfs")
          ), importees) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate(
            "DefaultFileToSave" | "DefaultReadFile" |
            "ComputedMetadata" | "BasicMetadata" | "CustomMetadata")) =>
            patches += Patch.removeImportee(i)

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }

      case Importer(
        Term.Select(
          Term.Select(Term.Name("play"), Term.Name("modules")),
          Term.Name("reactivemongo")
          ), importees) => {
        val patches = Seq.newBuilder[Patch]

        importees.foreach {
          case i @ Importee.Name(Name.Indeterminate("JSONFileToSave")) =>
            patches += Patch.removeImportee(i)

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
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
        case gridfsRes @ Term.Apply(Term.ApplyType(
          GridFSTermName(), List(_)), List(Term.Name(db))) =>
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

        case readFileTpe @ Type.Apply(ReadFileTypeLike(), List(_, idTpe)) =>
          Patch.replaceTree(
            readFileTpe,
            s"ReadFile[${idTpe.syntax}, reactivemongo.api.bson.BSONDocument]")

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

        case t @ Type.Name("DefaultFileToSave") if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/gridfs/")) =>
          Patch.replaceTree(t, "Unit /* Consider: reactivemongo.api.gridfs.GridFS.fileToSave */")

        case t @ Type.Apply(Type.Name(
          "BasicMetadata" | "CustomMetadata"), _) if (t.symbol.info.exists(
          _.toString startsWith "reactivemongo/api/gridfs/")) =>
          Patch.replaceTree(t, "Unit /* Consider: reactivemongo.api.gridfs.ReadFile */")

        case t @ Type.Name("DefaultReadFile" | "ComputedMetadata") if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/api/gridfs/")) =>
          Patch.replaceTree(t, "Unit /* Consider: reactivemongo.api.gridfs.ReadFile */")

        case t @ Type.Name("JSONFileToSave") if (t.symbol.info.exists(
          _.toString startsWith "play/modules/reactivemongo/")) =>
          Patch.replaceTree(t, "Unit /* Consider: reactivemongo.api.gridfs.ReadFile */")

      }
    })

  private def testSym(t: Tree)(f: String => Boolean)(
    implicit
    doc: SemanticDocument): Boolean =
    t.symbol.info.exists { i => f(i.toString) }

  @inline private def migrationRequired(t: Tree, msg: String): Patch = Patch.replaceTree(t, s"""reactivemongo.api.bson.migrationRequired("${msg}") /* ${t.syntax} */""")

  // ---

  private case class Fix(
    refactor: PartialFunction[Tree, PatchDirective],
    `import`: PartialFunction[Importer, Patch] = PartialFunction.empty[Importer, Patch])

  private case class PatchDirective(
    patch: Patch,
    recurse: Boolean)

  private object PatchDirective {
    import scala.language.implicitConversions

    implicit def default(p: Patch): PatchDirective = PatchDirective(p, true)

    implicit def full(spec: (Patch, Boolean)): PatchDirective =
      PatchDirective(spec._1, spec._2)
  }
}
