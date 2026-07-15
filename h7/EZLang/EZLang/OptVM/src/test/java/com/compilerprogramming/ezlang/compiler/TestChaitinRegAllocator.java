package com.compilerprogramming.ezlang.compiler;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class TestChaitinRegAllocator {

    /* Test move does not interfere with uses */
    @Test
    public void test4() {
        CompiledFunction function = TestInterferenceGraph.buildTest4();
        InterferenceGraph graph = new InterferenceGraphBuilder().build(function);
        System.out.println(graph.generateDotOutput());
        Set<InterferenceGraph.Edge> edges = graph.getEdges();
        Assertions.assertEquals(2, edges.size());
        Assertions.assertTrue(edges.contains(new InterferenceGraph.Edge(0, 1)));
        Assertions.assertTrue(edges.contains(new InterferenceGraph.Edge(0, 2)));
        Map<Integer, Integer> regAssignments = new ChaitinGraphColoringRegisterAllocator().assignRegisters(
                function, 64, Options.OPT);
        String result = function.toStr(new StringBuilder(), false).toString();
        Assertions.assertEquals("L0:\n" +
                        "    a = 1\n" +
                        "    b = 2\n" +
                        "    t = b+a\n" +
                        "    goto  L1\n" +
                        "L1:\n"
        , result);
        Assertions.assertEquals(regAssignments.size(), 3);
        Assertions.assertEquals(regAssignments.values().stream().sorted().distinct().count(), 2);
    }
}