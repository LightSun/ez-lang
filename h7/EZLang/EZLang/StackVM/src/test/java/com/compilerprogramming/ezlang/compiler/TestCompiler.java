package com.compilerprogramming.ezlang.compiler;

import com.compilerprogramming.ezlang.types.TypeDictionary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCompiler {

    String compileSrc(String src) {
        Compiler compiler = new Compiler();
        TypeDictionary typeDict = compiler.compileSrc(src);
        return compiler.dumpIR(typeDict);
    }

    @Test
    public void testFunction1() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return 1;\n" +
                "                }";
        String result = compileSrc(src);
        System.out.println(result);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tjump L1\n" +
                        "L1:\n",  result);
    }

    @Test
    public void testFunction2() {
        String src = "   func foo(n: Int)->Int {\n" +
                "              return -1;\n" +
                "         }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tnegi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction3() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return n;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction4() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return -n;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tnegi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction5() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return n+1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction6() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return 1+1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tpushi 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction7() {
        String src = "               func foo(n: Int)->Int {\n" +
                "                    return 1+1-1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tpushi 1\n" +
                        "\taddi\n" +
                        "\tpushi 1\n" +
                        "\tsubi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction8() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return 2==2;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 2\n" +
                        "\tpushi 2\n" +
                        "\teq\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction9() {
        String src = "                func foo(n: Int)->Int {\n" +
                "                    return 1!=1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tpushi 1\n" +
                        "\tneq\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction10() {
        String src = "                func foo(n: [Int])->Int {\n" +
                "                    return n[0];\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tloadindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction11() {
        String src = "                func foo(n: [Int])->Int {\n" +
                "                    return n[0]+n[1];\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tloadindexed\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\tloadindexed\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction12() {
        String src = "          func foo()->[Int] {\n" +
                "                    return new [Int] { 1, 2, 3 };\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tnew [Int]\n" +
                        "\tpushi 0\n" +
                        "\tpushi 1\n" +
                        "\tstoreindexed\n" +
                        "\tpushi 1\n" +
                        "\tpushi 2\n" +
                        "\tstoreindexed\n" +
                        "\tpushi 2\n" +
                        "\tpushi 3\n" +
                        "\tstoreindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction13() {
        String src = "                func foo(n: Int) -> [Int] {\n" +
                "                    return new [Int] { n };\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tnew [Int]\n" +
                        "\tpushi 0\n" +
                        "\tload 0\n" +
                        "\tstoreindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction14() {
        String src = "                func add(x: Int, y: Int) -> Int {\n" +
                "                    return x+y;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tload 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction15() {
        String src = "               struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var children: Int\n" +
                "                }\n" +
                "                func foo(p: Person) -> Person {\n" +
                "                    p.age = 10;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tpushi 10\n" +
                        "\tstoreindexed\n" +
                        "\tpop\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction16() {
        String src = "                struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var children: Int\n" +
                "                }\n" +
                "                func foo() -> Person {\n" +
                "                    return new Person { age=10, children=0 };\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tnew Person\n" +
                        "\tpushi 0\n" +
                        "\tpushi 10\n" +
                        "\tstoreindexed\n" +
                        "\tpushi 1\n" +
                        "\tpushi 0\n" +
                        "\tstoreindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction17() {
        String src = "                func foo(array: [Int]) {\n" +
                "                    array[0] = 1\n" +
                "                    array[1] = 2\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tpushi 1\n" +
                        "\tstoreindexed\n" +
                        "\tpop\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\tpushi 2\n" +
                        "\tstoreindexed\n" +
                        "\tpop\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction18() {
        String src = "                func min(x: Int, y: Int) -> Int {\n" +
                "                    if (x < y)\n" +
                "                        return x;\n" +
                "                    return y;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tload 1\n" +
                        "\tlt\n" +
                        "\tcbr L2 L3\n" +
                        "L2:\n" +
                        "\tload 0\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L3:\n" +
                        "\tload 1\n" +
                        "\tjump L1\n"
        , result);
    }

    @Test
    public void testFunction19() {
        String src = "                func loop() {\n" +
                "                    while (1)\n" +
                "                        return;\n" +
                "                    return;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tjump L2\n" +
                        "L2:\n" +
                        "\tpushi 1\n" +
                        "\tcbr L3 L4\n" +
                        "L3:\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L4:\n" +
                        "\tjump L1\n"
        , result);
    }

    @Test
    public void testFunction20() {
        String src = "                func loop() {\n" +
                "                    while (1)\n" +
                "                        break;\n" +
                "                    return;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tjump L2\n" +
                        "L2:\n" +
                        "\tpushi 1\n" +
                        "\tcbr L3 L4\n" +
                        "L3:\n" +
                        "\tjump L4\n" +
                        "L4:\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction21() {
        String src = "                func loop(n: Int) {\n" +
                "                    while (n > 0) {\n" +
                "                        n = n - 1;\n" +
                "                    }\n" +
                "                    return;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tjump L2\n" +
                        "L2:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tgt\n" +
                        "\tcbr L3 L4\n" +
                        "L3:\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\tsubi\n" +
                        "\tstore 0\n" +
                        "\tjump L2\n" +
                        "L4:\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction22() {
        String src = "                func foo() {}\n" +
                "                func bar() { foo(); }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "\tloadfunc foo\n" +
                        "\tcall 0\n" +
                        "\tpop\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction23() {
        String src = "                func foo(x: Int, y: Int) {}\n" +
                "                func bar() { foo(1,2); }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "\tloadfunc foo\n" +
                        "\tpushi 1\n" +
                        "\tpushi 2\n" +
                        "\tcall 2\n" +
                        "\tpop\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction24() {
        String src = "                func foo(x: Int, y: Int)->Int { return x+y; }\n" +
                "                func bar()->Int { var t = foo(1,2); return t+1; }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tload 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "\tloadfunc foo\n" +
                        "\tpushi 1\n" +
                        "\tpushi 2\n" +
                        "\tcall 2\n" +
                        "\tstore 0\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction25() {
        String src = "                struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var children: Int\n" +
                "                }\n" +
                "                func foo(p: Person) -> Int {\n" +
                "                    return p.age;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 0\n" +
                        "\tloadindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction26() {
        String src = "                struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var parent: Person\n" +
                "                }\n" +
                "                func foo(p: Person) -> Int {\n" +
                "                    return p.parent.age;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tpushi 1\n" +
                        "\tloadindexed\n" +
                        "\tpushi 0\n" +
                        "\tloadindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction27() {
        String src = "                struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var parent: Person\n" +
                "                }\n" +
                "                func foo(p: [Person], i: Int) -> Int {\n" +
                "                    return p[i].parent.age;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tload 1\n" +
                        "\tloadindexed\n" +
                        "\tpushi 1\n" +
                        "\tloadindexed\n" +
                        "\tpushi 0\n" +
                        "\tloadindexed\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction28() {
        String src = "                func foo(x: Int, y: Int)->Int { return x+y; }\n" +
                "                func bar(a: Int)->Int { var t = foo(a,2); return t+1; }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tload 0\n" +
                        "\tload 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "\tloadfunc foo\n" +
                        "\tload 0\n" +
                        "\tpushi 2\n" +
                        "\tcall 2\n" +
                        "\tstore 1\n" +
                        "\tload 1\n" +
                        "\tpushi 1\n" +
                        "\taddi\n" +
                        "\tjump L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction104() {
        String src = "                func foo()->Int\n" +
                "                {\n" +
                "                    return 1 == 1 && 2 == 2\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "\tpushi 1\n" +
                        "\tpushi 1\n" +
                        "\teq\n" +
                        "\tcbr L2 L3\n" +
                        "L2:\n" +
                        "\tpushi 2\n" +
                        "\tpushi 2\n" +
                        "\teq\n" +
                        "\tjump L4\n" +
                        "L4:\n" +
                        "\tjump L1\n" +
                        "L1:\n" +
                        "L3:\n" +
                        "\tpushi 0\n" +
                        "\tjump L4\n"
        , result);
    }

}
