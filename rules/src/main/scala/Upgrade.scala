package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

class Upgrade extends SemanticRule("ReactiveMongoUpgrade") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    val transformer: PartialFunction[Tree, Patch] =
      gridfsUpgrade orElse bsonUpgrade orElse {

        case _ =>
          //println(s"x = ${x.structure}")
          Patch.empty
      }

    Patch.fromIterable(doc.tree.collect(transformer))
  }

  // ---

  /* TODO: play.modules.reactivemongo/MongoController

  def gridFSBodyParser(gfs: Future[JsGridFS])(implicit @deprecated("Unused", "0.19.0") readFileReader: Reads[JsReadFile[JsValue]] = null.asInstanceOf[Reads[JsReadFile[JsValue]]], materializer: Materializer): JsGridFSBodyParser[JsValue] = parser(gfs, { (n, t) => gfs.fileToSave(Some(n), t) })(materializer)

  @deprecated("Use `gridFSBodyParser` without `ir`", "0.17.0")
  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit readFileReader: Reads[JsReadFile[Id]], materializer: Materializer, ir: Reads[Id]): JsGridFSBodyParser[Id] = parser(gfs, fileToSave)

  def gridFSBodyParser[Id <: JsValue](gfs: Future[JsGridFS], fileToSave: (String, Option[String]) => JsFileToSave[Id])(implicit @deprecated("Unused", "0.19.0") readFileReader: Reads[JsReadFile[Id]], materializer: Materializer): JsGridFSBodyParser[Id] = parser(gfs, fileToSave)
   */

  private val bsonUpgrade: PartialFunction[Tree, Patch] = {
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
