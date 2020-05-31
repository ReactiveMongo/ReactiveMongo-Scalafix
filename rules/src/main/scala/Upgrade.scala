package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

final class Upgrade extends SemanticRule("ReactiveMongoUpgrade") { self =>
  override def fix(implicit doc: SemanticDocument): Patch = {
    val fixes = Seq[Fix](
      coreUpgrade,
      apiUpgrade, gridfsUpgrade, bsonUpgrade, streamingUpgrade, playUpgrade)

    val fixImport: PartialFunction[Importer, Patch] =
      (fixes.foldLeft(PartialFunction.empty[Importer, Patch]) {
        _ orElse _.`import`
      }).orElse({ case _ => Patch.empty })

    val pipeline = fixes.foldLeft[PartialFunction[Tree, Patch]]({
      case Import(importers) => Patch.fromIterable(importers.map(fixImport))
    }) {
      _ orElse _.refactor
    }

    val transformer: PartialFunction[Tree, Patch] =
      pipeline orElse {
        case _ =>
          //println(s"x = ${x.structure}")
          Patch.empty
      }

    Patch.fromIterable(doc.tree.collect(transformer))
  }

  // ---

  private def streamingUpgrade(implicit doc: SemanticDocument) = Fix(
    refactor = {
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
        t.symbol.info.exists(
          _.toString startsWith "reactivemongo/core/actors/Exceptions")) =>
        Patch.replaceTree(t, s"${n}Exception")

      case t @ Type.Name("DetailedDatabaseException" |
        "GenericDatabaseException") if (t.symbol.info.exists(
        _.toString startsWith "reactivemongo/core/errors/")) =>
        Patch.replaceTree(t, "DatabaseException")

      case t @ Type.Name("ConnectionException" |
        "ConnectionNotInitialized" |
        "DriverException" |
        "GenericDriverException") if (t.symbol.info.exists(
        _.toString startsWith "reactivemongo/core/errors/")) =>
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
    def unapply(tree: Tree): Option[Tuple3[Option[Type], String, List[Term]]] = tree match {
      case t @ Term.Apply(
        Term.Select(Term.Name(c), Term.Name("insert")), args) if (
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
        Term.Select(Term.Name(c), Term.Name("insert")), List(tpe)), args) if (
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
    def unapply(tree: Tree): Option[Tuple3[List[Type], String, List[Term]]] = tree match {
      case Term.Apply(Term.ApplyType(
        Term.Select(Term.Name(c), Term.Name("update")), tpeParams), args) =>
        Some(Tuple3(tpeParams, c, args))

      case t @ Term.Apply(
        Term.Select(Term.Name(c), Term.Name("update")), args) if (
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
        Term.Select(
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

          case i @ Importee.Name(Name.Indeterminate("WriteConcern")) =>
            patches += (Patch.removeImportee(i) + Patch.addGlobalImport(
              Importer(apiPkg, List(importee"WriteConcern")))).atomic

          case i @ Importee.Name(
            Name.Indeterminate(
              "BoxedAnyVal" | "MultiBulkWriteResult" | "UnitBox")) =>
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

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
      val Insert = new InsertExtractor
      val Update = new UpdateExtractor

      {
        case t @ Term.Apply(
          Term.Select(c, Term.Name("runValueCommand")), args) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/GenericCollectionWithCommands")) =>
          Patch.replaceTree(t, s"""${c.syntax}.runCommand(${args.map(_.syntax) mkString ", "})""")

        case t @ Type.Apply(Type.Select(Term.Select(Term.Select(
          Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Type.Name("BoxedAnyVal")),
          List(Type.Name(v))) =>
          Patch.replaceTree(t, v)

        case t @ Type.Apply(Type.Name("BoxedAnyVal"), List(Type.Name(v))) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/api/commands/BoxedAnyVal")) =>
          Patch.replaceTree(t, v)

        case t @ Type.Singleton(Term.Name("UnitBox")) =>
          Patch.replaceTree(t, "Unit")

        case t @ Type.Singleton(Term.Select(Term.Select(
          Term.Select(Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("commands")), Term.Name("UnitBox"))) =>
          Patch.replaceTree(t, "Unit")

        case t @ Term.ApplyType(Term.Select(
          s, Term.Name("unboxed")), List(_, Type.Name(r), Type.Name(c))) if (
          t.symbol.info.exists(_.toString startsWith "reactivemongo/api/commands/Command.CommandWithPackRunner")) =>
          Patch.replaceTree(t, s"${s.syntax}.apply[${r}, ${c}]")

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
            t, s"${c}.update${builderApply}.one${appTArgs}${oneArgs}")

        }

        // ---

        case t @ Insert((tpe, c, args)) => {
          val appTArg = tpe.fold("") { at => s"[${at}]" }

          args match {
            case document :: Nil => document match {
              case Term.Assign(_, doc) =>
                Patch.replaceTree(
                  t, s"${c}.insert.one${appTArg}($doc)")

              case _ =>
                Patch.replaceTree(
                  t, s"${c}.insert.one${appTArg}($document)")
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

              Patch.replaceTree(t, s"${c}.insert(writeConcern = $writeConcern).one${appTArg}($docArg)")
            }
          }
        }

        // ---

        case t @ Term.Apply(Term.Select(
          Term.Name(c), Term.Name("remove")), args) if (t.symbol.info.exists(
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
              s"${c}.delete(writeConcern = ${wc.syntax})"

            case _ =>
              s"${c}.delete"
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
          Term.Select(Term.Name(c), Term.Name("BatchCommands")),
          Term.Name("AggregationFramework")
          ) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/bson/BSONBatchCommands")) =>
          Patch.replaceTree(t, s"${c}.AggregationFramework")

        case t @ Term.Select(Term.Name(c), Term.Name("aggregationFramework")) =>
          Patch.replaceTree(t, s"${c}.AggregationFramework")

        // ---

        case t @ Term.Name(QueryBuilderNaming(newName)) if (t.symbol.info.exists(_.toString startsWith "reactivemongo/api/collections/GenericQueryBuilder")) =>
          Patch.replaceTree(t, newName)

        case t @ Term.Apply(
          Term.Select(c @ Term.Name(_), Term.Name("find")),
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

          Patch.replaceTree(t, s"${c}.find($selector, $projection)")
        }

        // ---

        case t @ Term.Apply(
          Term.Select(Term.Name(c), Term.Name("rename")), args) if (
          t.symbol.info.exists {
            _.toString startsWith "reactivemongo/api/CollectionMetaCommands"
          }) =>
          Patch.replaceTree(t, s"""${c}.db.connection.database("admin").flatMap(_.renameCollection(${c}.db.name, ${c}.name, ${args.map(_.syntax).mkString(", ")}))""")

        // ---

        case t @ Term.Select(Term.Name(d),
          Term.Name("connect" | "connection")) if (t.symbol.info.exists { x =>
          val sym = x.toString
          sym.startsWith("reactivemongo/api/MongoDriver") || sym.startsWith(
            "reactivemongo/api/AsyncDriver")
        }) =>
          Patch.replaceTree(t, s"${d}.connect")

        // ---

        case t @ Term.Select(Term.Name(c), Term.Name(m)) if (
          t.symbol.info.exists(
            _.toString startsWith "reactivemongo/api/MongoConnection")) =>
          m match {
            case "askClose" =>
              Patch.replaceTree(t, s"${c}.close")

            case "parseURI" =>
              Patch.replaceTree(t, s"${c}.fromString")

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

        case t @ Type.Name("MultiBulkWriteResult") if (
          t.symbol.toString startsWith "reactivemongo/api/commands/") =>
          Patch.replaceTree(t, "Any /* MultiBulkWriteResult ~> anyCollection.MultiBulkWriteResult */")
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

          case _ =>
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

          case _ =>
        }

        Patch.fromIterable(patches.result())
      }
    },
    refactor = {
      case n @ Type.Name("JSONCollection") if (n.symbol.info.exists(
        _.toString startsWith "reactivemongo/play/json/collection/")) =>
        Patch.replaceTree(n, "BSONCollection")

      case n @ Type.Singleton(Term.Name("JSONSerializationPack")) =>
        Patch.replaceTree(n, "BSONSerializationPack.type")

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

    })

  private def bsonUpgrade(implicit doc: SemanticDocument) = Fix(
    `import` = {
      case Importer(Term.Select(
        Term.Name("reactivemongo"), Term.Name("bson")), is) => {

        val apiPkg = Term.Select(Term.Select(
          Term.Name("reactivemongo"),
          Term.Name("api")), Term.Name("bson"))

        Patch.fromIterable(is.flatMap { i =>
          Seq((Patch.removeImportee(i) + Patch.addGlobalImport(
            Importer(apiPkg, List(i)))).atomic)
        })
      }

      case Importer(Term.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("bson")),
        Term.Name("DefaultBSONHandlers")), is) => {

        val apiPkg = Term.Select(Term.Select(
          Term.Name("reactivemongo"),
          Term.Name("api")), Term.Name("bson"))

        Patch.fromIterable(is.flatMap { i =>
          Seq((Patch.removeImportee(i) + Patch.addGlobalImport(
            Importer(apiPkg, List(i)))).atomic)
        })
      }

      case Importer(Term.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("bson")), x), is) => {

        val apiPkg = Term.Select(Term.Select(
          Term.Select(
            Term.Name("reactivemongo"), Term.Name("api")),
          Term.Name("bson")), x)

        Patch.fromIterable(is.flatMap { i =>
          Seq((Patch.removeImportee(i) + Patch.addGlobalImport(
            Importer(apiPkg, List(i)))).atomic)
        })
      }
    },
    refactor = {
      case t @ Type.Apply(Type.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("bson")),
        n @ Type.Name("BSONHandler" | "BSONReader")), List(_, tpe)) =>
        Patch.replaceTree(t, s"reactivemongo.api.bson.$n[${tpe.syntax}]")

      case t @ Type.Apply(Type.Select(Term.Select(
        Term.Name("reactivemongo"), Term.Name("bson")),
        Type.Name(n @ "BSONWriter")), List(tpe, _)) =>
        Patch.replaceTree(t, s"reactivemongo.api.bson.${n}[${tpe.syntax}]")

      case t @ Type.Apply(
        n @ Type.Name("BSONHandler" | "BSONReader"), List(_, tpe)) if (
        t.symbol.info.exists(
          _.toString startsWith "reactivemongo/bson/BSON")) =>
        Patch.replaceTree(t, s"${n.syntax}[${tpe.syntax}]")

      case t @ Type.Apply(n @ Type.Name("BSONWriter"),
        List(Type.Name(tpe), _)) if (t.symbol.info.exists(
        _.toString startsWith "reactivemongo/bson/BSON")) =>
        Patch.replaceTree(t, s"${n}[${tpe}]")

      case t @ Type.Select(
        Term.Select(Term.Name("reactivemongo"), Term.Name("bson")),
        Type.Name(n)
        ) if (n != "BSONReader" && n != "BSONHandler" && n != "BSONWriter") =>
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

    })

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
            Type.Singleton(Term.Name("BSONSerializationPack")), Type.Name(idTpe))
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

  // ---

  private case class Fix(
    refactor: PartialFunction[Tree, Patch],
    `import`: PartialFunction[Importer, Patch] = PartialFunction.empty[Importer, Patch])
}
