package fix

import scalafix.testkit.AbstractSemanticRuleSuite

class RuleSuite extends AbstractSemanticRuleSuite
  with org.scalatest.funsuite.AnyFunSuiteLike {

  runAllTests()
}
