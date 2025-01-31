package com.wavesplatform.lang.v1.compiler
import com.wavesplatform.common.utils.EitherExt2
import com.wavesplatform.lang.v1.FunctionHeader
import com.wavesplatform.lang.v1.FunctionHeader.{Native, User}
import com.wavesplatform.lang.v1.compiler.Terms._
import com.wavesplatform.lang.v1.evaluator.FunctionIds._

object CompilerMacro {
  def unwrapFold(limit: Int, list: EXPR, acc: EXPR, func: FunctionHeader): EXPR = {

    def call(id: Short, args: List[EXPR])        = FUNCTION_CALL(Native(id), args)
    def callUser(name: String, args: List[EXPR]) = FUNCTION_CALL(User(name), args)

    def step(last: Boolean) = FUNC(
      if (last) "2" else "1",
      List("$a", "$i"),
      IF(
        call(GE_LONG, List(REF("$i"), REF("$s"))),
        REF("$a"),
        if (last)
          call(THROW, List(CONST_STRING(s"List size exceeds $limit").explicitGet()))
        else
          FUNCTION_CALL(
            func,
            List(
              REF("$a"),
              call(GET_LIST, List(REF("$l"), REF("$i")))
            )
          )
      )
    )

    val callChain        = (0 until limit).foldLeft(REF("$acc0"): EXPR)((acc, i) => callUser("1", List(acc, CONST_LONG(i))))
    val callChainWithEnd = callUser("2", List(callChain, CONST_LONG(limit)))
    List(
      LET("$l", list),
      LET("$s", call(SIZE_LIST, List(REF("$l")))),
      LET("$acc0", acc),
      step(last = false),
      step(last = true)
    ).foldRight(callChainWithEnd: EXPR)((decl, acc) => BLOCK(decl, acc))
  }
}
