package com.alexfh.scrabbleai.dictionary;

import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WordGraph implements IDictionary {

    public static WordGraph fromFile(File dictionaryFile) throws IOException {
        WordGraph dictionary = new WordGraph();
        BufferedReader reader = new BufferedReader(new FileReader(dictionaryFile, StandardCharsets.UTF_8));
        String line;

        while((line = reader.readLine()) != null) {
            dictionary.addWord(line.toLowerCase().strip());
        }

        reader.close();

        return dictionary;
    }

    private static class WGNode {

        private final WGNode[] nodes = new WGNode[26];
        public boolean wordHere;
        private final WGNode parent;
        private final List<Character> paths;
        private final String word;

        public WGNode(WGNode parent, String word) {
            this(false, parent, word);
        }

        public WGNode(boolean wordHere, WGNode parent, String word) {
            this.wordHere = wordHere;
            this.parent = parent;
            this.paths = new ArrayList<>();
            this.word = word;
        }

        public void removePath(char c) {
            int i = ScrabbleUtil.charToInt(c);
            WGNode path = this.nodes[i];

            if (path != null) {
                this.nodes[i] = null;

                this.paths.remove(Character.valueOf(c));
            }
        }

        public WGNode getPath(char c) {
            return this.nodes[ScrabbleUtil.charToInt(c)];
        }

        public WGNode getOrCreatePath(char c) {
            int i = ScrabbleUtil.charToInt(c);
            WGNode current = this.nodes[i];

            if (current == null) {
                this.nodes[i] = new WGNode(this, this.word.concat(String.valueOf(c)));

                this.paths.add(c);
            }

            return this.nodes[i];
        }

    }

    private WGNode root = null;

    private WGNode followPath(String path) {
        if (this.root == null) return null;

        WGNode current = this.root;

        for (int i = 0; i < path.length(); i++) {
            current = current.getPath(path.charAt(i));

            if (current == null) return null;
        }

        return current;
    }

    private WGNode getOrCreateRoot() {
        if (this.root == null) {
            this.root = new WGNode(null, "");
        }

        return this.root;
    }

    @Override
    public boolean hasWord(String word) {
        WGNode path = this.followPath(word);

        return path != null && path.wordHere;
    }

    @Override
    public boolean hasPrefix(String prefix) {
        return this.followPath(prefix) != null;
    }

    @Override
    public void addWord(String word) {
        WGNode current = this.getOrCreateRoot();

        for (int i = 0; i < word.length(); i++) {
            current = current.getOrCreatePath(word.charAt(i));
        }

        current.wordHere = true;
    }

    @Override
    public void removeWord(String word) {
        WGNode path = this.followPath(word);

        if (path == null || !path.wordHere) return;

        path.wordHere = false;

        for (int i = word.length() - 1; i >= 0; i--) {
            WGNode parent = path.parent;

            if (path.paths.size() == 0 && !path.wordHere) {
                parent.removePath(word.charAt(i));
            } else {
                return;
            }

            path = parent;
        }

        if (path.paths.size() == 0 && !path.wordHere) this.root = null;
    }

    @Override
    public void forEach(Consumer<String> consumer) {
        if (this.root == null) return;

        this.forEach(this.root, consumer);
    }

    private void forEach(WGNode node, Consumer<String> consumer) {
        if (node.wordHere) consumer.accept(node.word);

        node.paths.stream()
            .map(node::getPath)
            .forEach(wgNode -> this.forEach(wgNode, consumer));
    }

}