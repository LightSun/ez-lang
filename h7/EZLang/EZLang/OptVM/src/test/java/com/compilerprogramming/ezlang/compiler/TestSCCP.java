package com.compilerprogramming.ezlang.compiler;

import com.compilerprogramming.ezlang.types.Symbol;
import com.compilerprogramming.ezlang.types.TypeDictionary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.EnumSet;

public class TestSCCP {

    String compileSrc(String src) {
        Compiler compiler = new Compiler();
        TypeDictionary typeDict = compiler.compileSrc(src);
        StringBuilder sb = new StringBuilder();
        EnumSet<Options> options = Options.NONE;
        for (Symbol s : typeDict.bindings.values()) {
            if (s instanceof Symbol.FunctionTypeSymbol) {
                Symbol.FunctionTypeSymbol f = (Symbol.FunctionTypeSymbol) s;
                CompiledFunction functionBuilder = (CompiledFunction) f.code();
                new EnterSSA(functionBuilder, options);
                BasicBlock.toStr(sb, functionBuilder.entry, new BitSet(), false);
                //functionBuilder.toDot(sb, false);
                SparseConditionalConstantPropagation sccp = new SparseConditionalConstantPropagation()
                        .constantPropagation(functionBuilder);
                sb.append(sccp.toString());
                sccp.apply(options);
                sb.append("After SCCP changes:\n");
                functionBuilder.toStr(sb, false);
            }
        }
        return sb.toString();
    }

    @Test
    public void test1() {
        String src = "func foo()->Int {\n" +
                "    var i = 1\n" +
                "    if (i == 0)\n" +
                "        i = 2\n" +
                "    else\n" +
                "        i = 3\n" +
                "    return i\n" +
                "}      ";
        String actual = compileSrc(src);
        String expected = "L0:\n" +
                "    i_0 = 1\n" +
                "    %t1_0 = i_0==0\n" +
                "    if %t1_0 goto L2 else goto L3\n" +
                "L2:\n" +
                "    i_2 = 2\n" +
                "    goto  L4\n" +
                "L4:\n" +
                "    i_3 = phi(i_2, i_1)\n" +
                "    ret i_3\n" +
                "    goto  L1\n" +
                "L1:\n" +
                "L3:\n" +
                "    i_1 = 3\n" +
                "    goto  L4\n" +
                "Flow edges:\n" +
                "L0->L2=NOT Executable\n" +
                "L0->L3=Executable\n" +
                "L4->L1=Executable\n" +
                "L2->L4=NOT Executable\n" +
                "L3->L4=Executable\n" +
                "Lattices:\n" +
                "i_0=1\n" +
                "%t1_0=0\n" +
                "i_1=3\n" +
                "i_3=3\n" +
                "After SCCP changes:\n" +
                "L0:\n" +
                "    goto  L3\n" +
                "L3:\n" +
                "    goto  L4\n" +
                "L4:\n" +
                "    ret 3\n" +
                "    goto  L1\n" +
                "L1:\n";
        Assertions.assertEquals(expected, actual);
    }

    // 19.4 in MCIC Appel
    // Expected results are based on fig 19.13 page 456
    @Test
    public void test2() {
        String src = "func foo()->Int {\n" +
                "    var i = 1\n" +
                "    var j = 1\n" +
                "    var k = 0\n" +
                "    while (k < 100) {\n" +
                "        if (j < 20) {\n" +
                "            j = i\n" +
                "            k = k + 1\n" +
                "        }\n" +
                "        else {\n" +
                "            j = k\n" +
                "            k = k + 2\n" +
                "        }\n" +
                "    }\n" +
                "    return j\n" +
                "}";
        String actual = compileSrc(src);
        String expected = "L0:\n" +
                "    i_0 = 1\n" +
                "    j_0 = 1\n" +
                "    k_0 = 0\n" +
                "    goto  L2\n" +
                "L2:\n" +
                "    k_1 = phi(k_0, k_4)\n" +
                "    j_1 = phi(j_0, j_4)\n" +
                "    %t3_0 = k_1<100\n" +
                "    if %t3_0 goto L3 else goto L4\n" +
                "L3:\n" +
                "    %t4_0 = j_1<20\n" +
                "    if %t4_0 goto L5 else goto L6\n" +
                "L5:\n" +
                "    j_3 = i_0\n" +
                "    %t5_0 = k_1+1\n" +
                "    k_3 = %t5_0\n" +
                "    goto  L7\n" +
                "L7:\n" +
                "    k_4 = phi(k_3, k_2)\n" +
                "    j_4 = phi(j_3, j_2)\n" +
                "    goto  L2\n" +
                "L6:\n" +
                "    j_2 = k_1\n" +
                "    %t6_0 = k_1+2\n" +
                "    k_2 = %t6_0\n" +
                "    goto  L7\n" +
                "L4:\n" +
                "    ret j_1\n" +
                "    goto  L1\n" +
                "L1:\n" +
                "Flow edges:\n" +
                "L0->L2=Executable\n" +
                "L4->L1=Executable\n" +
                "L2->L3=Executable\n" +
                "L2->L4=Executable\n" +
                "L3->L5=Executable\n" +
                "L7->L2=Executable\n" +
                "L3->L6=NOT Executable\n" +
                "L5->L7=Executable\n" +
                "L6->L7=NOT Executable\n" +
                "Lattices:\n" +
                "j_3=1\n" +
                "%t5_0=varying\n" +
                "k_3=varying\n" +
                "k_4=varying\n" +
                "j_4=1\n" +
                "i_0=1\n" +
                "j_0=1\n" +
                "k_0=0\n" +
                "k_1=varying\n" +
                "j_1=1\n" +
                "%t3_0=varying\n" +
                "%t4_0=1\n" +
                "After SCCP changes:\n" +
                "L0:\n" +
                "    goto  L2\n" +
                "L2:\n" +
                "    k_1 = phi(0, k_4)\n" +
                "    %t3_0 = k_1<100\n" +
                "    if %t3_0 goto L3 else goto L4\n" +
                "L3:\n" +
                "    goto  L5\n" +
                "L5:\n" +
                "    %t5_0 = k_1+1\n" +
                "    k_3 = %t5_0\n" +
                "    goto  L7\n" +
                "L7:\n" +
                "    k_4 = phi(k_3)\n" +
                "    goto  L2\n" +
                "L4:\n" +
                "    ret 1\n" +
                "    goto  L1\n" +
                "L1:\n";
        Assertions.assertEquals(expected, actual);
    }
}
