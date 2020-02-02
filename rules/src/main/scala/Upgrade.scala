package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

class Upgrade extends SemanticRule("ReactiveMongoUpgrade") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    val transformer: PartialFunction[Tree, Patch] = gridfsUpgrade orElse {

      case _ =>
        //println(s"x = ${x.structure}")
        Patch.empty
    }

    Patch.fromIterable(doc.tree.collect(transformer))
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

      case gridfsRm @ Term.Apply(
        Term.Select(Term.Name(gt), Term.Name("remove")),
        List(Term.Name(ref))
        ) if (gridfsRm.symbol.owner.
        toString == "reactivemongo/api/gridfs/GridFS#") =>
        Patch.replaceTree(gridfsRm, s"${gt}.remove(${ref}.id)")

    }
  }
}
