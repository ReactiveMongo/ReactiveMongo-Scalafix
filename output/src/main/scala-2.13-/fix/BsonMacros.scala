package fix


import reactivemongo.api.bson.Macros
import reactivemongo.api.bson.Macros.Annotations.{ Ignore, Key }

object BsonMacros {

  case class Foo(
    @Key("_name") name: String,
    @Ignore age: Int)
}
