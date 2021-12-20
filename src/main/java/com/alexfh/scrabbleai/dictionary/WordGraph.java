package com.alexfh.scrabbleai.dictionary;

public class WordGraph implements IDictionary {

    private static class DAWGNode {

        private final DAWGNode[] nodes = new DAWGNode[26];
        public boolean wordHere;
        private int paths;
        private final DAWGNode parent;

        public DAWGNode(DAWGNode parent) {
            this(false, parent);
        }

        public DAWGNode(boolean wordHere, DAWGNode parent) {
            this.wordHere = wordHere;
            this.parent = parent;
            this.paths = 0;
        }

        public void removePath(char c) {
            int i = charToInt(c);
            DAWGNode path = this.nodes[i];

            if (path != null) {
                this.nodes[i] = null;
                this.paths--;
            }
        }

        public DAWGNode getPath(char c) {
            return this.nodes[charToInt(c)];
        }

        public DAWGNode getOrCreatePath(char c) {
            int i = charToInt(c);
            DAWGNode current = this.nodes[i];

            if (current == null) {
                this.nodes[i] = new DAWGNode(this);

                this.paths++;
            }

            return this.nodes[i];
        }

    }

    private static int charToInt(char c) {
        return (int) c - 97;
    }

    private DAWGNode root = null;

    private DAWGNode followPath(String path) {
        if (this.root == null) return null;

        DAWGNode current = this.root;

        for (int i = 0; i < path.length(); i++) {
            current = current.getPath(path.charAt(i));

            if (current == null) return null;
        }

        return current;
    }

    private DAWGNode getOrCreateRoot() {
        if (this.root == null) {
            this.root = new DAWGNode(null);
        }

        return this.root;
    }

    @Override
    public boolean hasWord(String word) {
        DAWGNode path = this.followPath(word);

        return path != null && path.wordHere;
    }

    @Override
    public boolean hasPrefix(String prefix) {
        return this.followPath(prefix) != null;
    }

    @Override
    public void addWord(String word) {
        DAWGNode current = this.getOrCreateRoot();

        for (int i = 0; i < word.length(); i++) {
            current = current.getOrCreatePath(word.charAt(i));
        }

        current.wordHere = true;
    }

    @Override
    public void removeWord(String word) {
        DAWGNode path = this.followPath(word);

        if (path == null || !path.wordHere) return;

        path.wordHere = false;

        for (int i = word.length() - 1; i >= 0; i--) {
            DAWGNode parent = path.parent;

            if (path.paths == 0 && !path.wordHere) {
                parent.removePath(word.charAt(i));
            }

            path = parent;
        }

        if (path.paths == 0 && !path.wordHere) this.root = null;
    }

}
