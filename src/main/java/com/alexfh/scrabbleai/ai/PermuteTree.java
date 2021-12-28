package com.alexfh.scrabbleai.ai;

import com.alexfh.scrabbleai.util.ScrabbleUtil;

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

        public void createPathIfNull(char c) {
            int i = ScrabbleUtil.charToInt(c);
            PTNode current = this.nodes[i];

            if (current == null) {
                this.nodes[i] = new PTNode(this.permutation.concat(String.valueOf(c)));

                this.paths.add(c);
            }

        }

        public PTNode createPath(char c) {
            int i = ScrabbleUtil.charToInt(c);
            this.nodes[i] = new PTNode(this.permutation.concat(String.valueOf(c)));

            this.paths.add(c);

            return this.nodes[i];
        }

    }

    private final PTNode root = new PTNode("");

    public PermuteTree(char[] toPermute) {
        this.forAllPerm(this.root, toPermute, 0);
    }

    public PTNode getRoot() {
        return this.root;
    }

    private void forAllPerm(PTNode permuteNode, char[] toPermute, int i) {
        if (i == toPermute.length - 1) {
            permuteNode.createPathIfNull(toPermute[i]);

            return;
        }

        if (permuteNode.getPath(toPermute[i]) == null)
            forAllPerm(permuteNode.createPath(toPermute[i]), toPermute, i + 1);

        for (int w = i + 1; w < toPermute.length; w++) {
            if (toPermute[i] != toPermute[w] && permuteNode.getPath(toPermute[w]) == null) {
                swap(toPermute, i, w);
                forAllPerm(permuteNode.createPath(toPermute[i]), toPermute, i + 1);
                swap(toPermute, w, i);
            }
        }
    }

    private void swap(char[] toSwap, int from, int to) {
        char temp = toSwap[to];
        toSwap[to] = toSwap[from];
        toSwap[from] = temp;
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
