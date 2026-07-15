package com.compilerprogramming.ezlang.compiler;

import java.util.*;

public class InterferenceGraph {
    private Map<Integer, Set<Integer>> edges = new HashMap<>();

    private Set<Integer> addNode(Integer node) {
        Set<Integer> set = edges.get(node);
        if (set == null) {
            set = new HashSet<>();
            edges.put(node, set);
        }
        return set;
    }

    public void addEdge(Integer from, Integer to) {
        if (from == to) {
            return;
        }
        Set<Integer> set1 = addNode(from);
        Set<Integer> set2 = addNode(to);
        set1.add(to);
        set2.add(from);
    }

    /**
     * Remove a node from the interference graph
     * deleting it from all adjacency lists
     */
    public InterferenceGraph subtract(Integer node) {
        edges.remove(node);
        for (Integer key : edges.keySet()) {
            Set<Integer> neighbours = edges.get(key);
            neighbours.remove(key);
        }
        return this;
    }

    /**
     * Duplicate an interference graph
     */
    public InterferenceGraph dup() {
        InterferenceGraph igraph = new InterferenceGraph();
        igraph.edges = new HashMap<>();
        for (Integer key : edges.keySet()) {
            Set<Integer> neighbours = edges.get(key);
            igraph.edges.put(key, new HashSet<>(neighbours));
        }
        return igraph;
    }

    public boolean interfere(Integer from, Integer to) {
        Set<Integer> set = edges.get(from);
        return set != null && set.contains(to);
    }

    /**
     * The source is replaced by target in the graph.
     * All nodes that interfered with source are made to interfere with target.
     */
    public void rename(Integer source, Integer target) {
        // Move all interferences
        Set<Integer> fromSet = edges.remove(source);
        if (fromSet == null) {
            // FIXME figure out why
            // Test case testSSA21 / when run using Boissinot SSA Destruction without coalescing
            // This is eq() function in mergesort test case
            return;
        }
        Set<Integer> toSet = edges.get(target);
        if (toSet == null) {
            //throw new RuntimeException("Cannot find edge " + target + " from " + source);
            return; // FIXME this is workaround to handle scenario where target is arg register but we need a better way
        }
        toSet.addAll(fromSet);
        // If any node interfered with from
        // it should now interfere with to
        for (Integer k: edges.keySet()) {
            Set<Integer> set = edges.get(k);
            if (set.contains(source)) {
                set.remove(source);
                if (k != target)
                    set.add(target);
            }
        }
    }

    /**
     * Get neighbours of the node
     * Chaitin: neighbors()
     */
    public Set<Integer> neighbors(Integer node) {
        Set<Integer> adjacents = edges.get(node);
        if (adjacents == null)
            adjacents = Collections.emptySet();
        return adjacents;
    }

    public static final class Edge {
        public final int from;
        public final int to;
        public Edge(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return (from == edge.from && to == edge.to)
                    || (from == edge.to && to == edge.from);
        }

        @Override
        public int hashCode() {
            return from+to;
        }
    }

    public Set<Edge> getEdges() {
        Set<Edge> all = new HashSet<>();
        for (Integer from: edges.keySet()) {
            Set<Integer> set  = edges.get(from);
            for (Integer to: set) {
                all.add(new Edge(from, to));
            }
        }
        return all;
    }

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph IGraph {\n");
        for (Edge edge: getEdges()) {
            sb.append(edge.from).append("->").append(edge.to).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

}
