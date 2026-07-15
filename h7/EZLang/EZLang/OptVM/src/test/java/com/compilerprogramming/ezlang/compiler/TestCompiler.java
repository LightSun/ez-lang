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
        Assertions.assertEquals("L0:\n" +
                        "    arg n\n" +
                        "    ret 1\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction2() {
        String src = "func foo(n: Int)->Int {\n" +
                "                    return -1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg n\n" +
                        "    ret -1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    ret n\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    %t1 = -n\n" +
                        "    ret %t1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    %t1 = n+1\n" +
                        "    ret %t1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    ret 2\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction7() {
        String src = "              func foo(n: Int)->Int {\n" +
                "                    return 1+1-1;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg n\n" +
                        "    ret 1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    ret 1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    ret 0\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    %t1 = n[0]\n" +
                        "    ret %t1\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    %t1 = n[0]\n" +
                        "    %t2 = n[1]\n" +
                        "    %t3 = %t1+%t2\n" +
                        "    ret %t3\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction12() {
        String src = "                func foo()->[Int] {\n" +
                "                    return new [Int] { 1, 2, 3 };\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    %t0 = New([Int], len=3)\n" +
                        "    %t0[0] = 1\n" +
                        "    %t0[1] = 2\n" +
                        "    %t0[2] = 3\n" +
                        "    ret %t0\n" +
                        "    goto  L1\n" +
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
                        "    arg n\n" +
                        "    %t1 = New([Int], len=1)\n" +
                        "    %t1[0] = n\n" +
                        "    ret %t1\n" +
                        "    goto  L1\n" +
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
                        "    arg x\n" +
                        "    arg y\n" +
                        "    %t2 = x+y\n" +
                        "    ret %t2\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction15() {
        String src = "                struct Person\n" +
                "                {\n" +
                "                    var age: Int\n" +
                "                    var children: Int\n" +
                "                }\n" +
                "                func foo(p: Person) -> Person {\n" +
                "                    p.age = 10;\n" +
                "                }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg p\n" +
                        "    p.age = 10\n" +
                        "    goto  L1\n" +
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
                        "    %t0 = New(Person)\n" +
                        "    %t0.age = 10\n" +
                        "    %t0.children = 0\n" +
                        "    ret %t0\n" +
                        "    goto  L1\n" +
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
                        "    arg array\n" +
                        "    array[0] = 1\n" +
                        "    array[1] = 2\n" +
                        "    goto  L1\n" +
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
                        "    arg x\n" +
                        "    arg y\n" +
                        "    %t2 = x<y\n" +
                        "    if %t2 goto L2 else goto L3\n" +
                        "L2:\n" +
                        "    ret x\n" +
                        "    goto  L1\n" +
                        "L1:\n" +
                        "L3:\n" +
                        "    ret y\n" +
                        "    goto  L1\n"
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
        Assertions.assertEquals("                L0:\n" +
                        "                    goto  L2\n" +
                        "                L2:\n" +
                        "                    if 1 goto L3 else goto L4\n" +
                        "                L3:\n" +
                        "                    goto  L1\n" +
                        "                L1:\n" +
                        "                L4:\n" +
                        "                    goto  L1\n"
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
        Assertions.assertEquals("                L0:\n" +
                        "                    goto  L2\n" +
                        "                L2:\n" +
                        "                    if 1 goto L3 else goto L4\n" +
                        "                L3:\n" +
                        "                    goto  L4\n" +
                        "                L4:\n" +
                        "                    goto  L1\n" +
                        "                L1:\n"
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
                        "    arg n\n" +
                        "    goto  L2\n" +
                        "L2:\n" +
                        "    %t1 = n>0\n" +
                        "    if %t1 goto L3 else goto L4\n" +
                        "L3:\n" +
                        "    %t2 = n-1\n" +
                        "    n = %t2\n" +
                        "    goto  L2\n" +
                        "L4:\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction22() {
        String src = "                func foo() {}\n" +
                "                func bar() { foo(); }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    goto  L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "    call foo\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction23() {
        String src = "                func foo(x: Int, y: Int) {}\n" +
                "                func bar() { foo(1,2); }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg x\n" +
                        "    arg y\n" +
                        "    goto  L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "    %t0 = 1\n" +
                        "    %t1 = 2\n" +
                        "    call foo params %t0, %t1\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction24() {
        String src = "                func foo(x: Int, y: Int)->Int { return x+y; }\n" +
                "                func bar()->Int { var t = foo(1,2); return t+1; }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg x\n" +
                        "    arg y\n" +
                        "    %t2 = x+y\n" +
                        "    ret %t2\n" +
                        "    goto  L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "    %t1 = 1\n" +
                        "    %t2 = 2\n" +
                        "    %t3 = call foo params %t1, %t2\n" +
                        "    t = %t3\n" +
                        "    %t4 = t+1\n" +
                        "    ret %t4\n" +
                        "    goto  L1\n" +
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
                        "    arg p\n" +
                        "    %t1 = p.age\n" +
                        "    ret %t1\n" +
                        "    goto  L1\n" +
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
                        "    arg p\n" +
                        "    %t1 = p.parent\n" +
                        "    %t2 = %t1.age\n" +
                        "    ret %t2\n" +
                        "    goto  L1\n" +
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
                        "    arg p\n" +
                        "    arg i\n" +
                        "    %t2 = p[i]\n" +
                        "    %t3 = %t2.parent\n" +
                        "    %t4 = %t3.age\n" +
                        "    ret %t4\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }

    @Test
    public void testFunction28() {
        String src = "                func foo(x: Int, y: Int)->Int { return x+y; }\n" +
                "                func bar(a: Int)->Int { var t = foo(a,2); return t+1; }";
        String result = compileSrc(src);
        Assertions.assertEquals("L0:\n" +
                        "    arg x\n" +
                        "    arg y\n" +
                        "    %t2 = x+y\n" +
                        "    ret %t2\n" +
                        "    goto  L1\n" +
                        "L1:\n" +
                        "L0:\n" +
                        "    arg a\n" +
                        "    %t2 = a\n" +
                        "    %t3 = 2\n" +
                        "    %t4 = call foo params %t2, %t3\n" +
                        "    t = %t4\n" +
                        "    %t5 = t+1\n" +
                        "    ret %t5\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
    }
}
