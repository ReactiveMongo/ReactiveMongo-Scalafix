package reactivemongo.scalafix

import scalafix.v1._
import scala.meta._

final class Linter extends SemanticRule("ReactiveMongoLinter") {
  override def fix(implicit doc: SemanticDocument): Patch =
    Patch.fromIterable(doc.tree.collect {
      case v @ Defn.Val(
        List(),
        List(Pat.Var(Term.Name(_))),
        None,
        _
        ) => {
        val m = v.symbol.info.map(_.signature).exists {
          case MethodSignature(_, _, TypeRef(
            NoType,
            future,
            List(TypeRef(NoType, db, Nil))
            )) =>
            future.value == "scala/concurrent/Future#" && (
              db.value == "reactivemongo/api/DB#" ||
              db.value == "reactivemongo/api/DefaultDB#")

          case _ =>
            false
        }

        if (!m) {
          Patch.empty
        } else {
          Patch.lint(ValDatabase(v))
        }
      }

      case _ =>
        //println(s"-> ${x.structure}")
        Patch.empty
    })
}

// ---

case class ValDatabase(`val`: Defn.Val) extends Diagnostic {
  override def position: Position = `val`.pos

  override def message: String =
    s"Assigning ReactiveMongo database as 'val' is discouraged"
}
