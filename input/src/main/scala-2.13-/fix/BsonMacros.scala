/*
rule = ReactiveMongoUpgrade
*/
package fix

import reactivemongo.bson.Macros

import Macros.Annotations, Annotations.Ignore
import Macros.Annotations.Key

object BsonMacros {

  case class Foo(
    @Key("_name") name: String,
    @Ignore age: Int)
}
