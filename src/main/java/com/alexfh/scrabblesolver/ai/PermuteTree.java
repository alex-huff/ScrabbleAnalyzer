package com.alexfh.scrabblesolver.ai;

import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class PermuteTree {

    public static class PTNode {

        private final PTNode[] nodes = new PTNode[27];
        private final List<Character> paths;
        private final String permutation;

        public PTNode(String permutation) {
            this.paths = new LinkedList<>();
            this.permutation = permutation;
        }

        public List<Character> getPaths() {
            return this.paths;
        }

        public PTNode getPath(char c) {
            return this.nodes[ScrabbleUtil.charToInt(c)];
        }

        private void createPathIfNull(char c) {
            int i = ScrabbleUtil.charToInt(c);
            PTNode current = this.nodes[i];

            if (current == null) {
                this.nodes[i] = new PTNode(this.permutation.concat(String.valueOf(c)));

                this.paths.add(c);
            }

        }

        private PTNode createPath(char c) {
            int i = ScrabbleUtil.charToInt(c);
            this.nodes[i] = new PTNode(this.permutation.concat(String.valueOf(c)));

            this.paths.add(c);

            return this.nodes[i];
        }

    }

    private final PTNode root = new PTNode("");

    public PermuteTree(List<Character> toPermute) throws InterruptedException {
        if (toPermute.size() == 0) return;

        this.forAllPerm(this.root, toPermute, 0);
    }

    public PTNode getRoot() {
        return this.root;
    }

    private void forAllPerm(PTNode permuteNode, List<Character> toPermute, int i) throws InterruptedException {
        ScrabbleUtil.checkInterrupted();

        if (i == toPermute.size() - 1) {
            permuteNode.createPathIfNull(toPermute.get(i));

            return;
        }

        if (permuteNode.getPath(toPermute.get(i)) == null)
            forAllPerm(permuteNode.createPath(toPermute.get(i)), toPermute, i + 1);

        for (int w = i + 1; w < toPermute.size(); w++) {
            if (toPermute.get(i) != toPermute.get(w) && permuteNode.getPath(toPermute.get(w)) == null) {
                swap(toPermute, i, w);
                forAllPerm(permuteNode.createPath(toPermute.get(i)), toPermute, i + 1);
                swap(toPermute, w, i);
            }
        }
    }

    private void swap(List<Character> toSwap, int from, int to) {
        char temp = toSwap.get(to);

        toSwap.set(to, toSwap.get(from));
        toSwap.set(from, temp);
    }

    public void forEach(Consumer<String> consumer) {
        this.forEach(this.root, consumer);
    }

    private void forEach(PTNode node, Consumer<String> consumer) {
        consumer.accept(node.permutation);

        for (char c : node.paths) {
            this.forEach(node.getPath(c), consumer);
        }
    }

}
