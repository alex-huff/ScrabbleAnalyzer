package com.alexfh.scrabbleanalyzer.dictionary;

import com.alexfh.scrabbleanalyzer.util.ScrabbleUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public
class WordGraphDictionary
{

    public static
    WordGraphDictionary fromInputStream(InputStream inputStream)
    {
        WordGraphDictionary dictionary = new WordGraphDictionary();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {
            WordGraphDictionary.populateDictionaryFromReader(dictionary, reader);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return dictionary;
    }

    public static
    WordGraphDictionary fromFile(File dictionaryFile) throws IOException
    {
        WordGraphDictionary dictionary = new WordGraphDictionary();

        try (BufferedReader reader = new BufferedReader(new FileReader(dictionaryFile, StandardCharsets.UTF_8)))
        {
            WordGraphDictionary.populateDictionaryFromReader(dictionary, reader);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return dictionary;
    }

    private static
    void populateDictionaryFromReader(WordGraphDictionary dictionary, BufferedReader reader) throws IOException
    {
        String line;

        while ((line = reader.readLine()) != null)
        {
            dictionary.addWord(line.toLowerCase());
        }
    }

    public static
    class WGNode
    {

        private final WGNode[]        nodes = new WGNode[26];
        private       boolean         wordHere;
        private final WGNode          parent;
        private final List<Character> paths;
        private final String          word;

        public
        WGNode(WGNode parent, String word)
        {
            this(false, parent, word);
        }

        public
        WGNode(boolean wordHere, WGNode parent, String word)
        {
            this.wordHere = wordHere;
            this.parent   = parent;
            this.paths    = new LinkedList<>();
            this.word     = word;
        }

        public
        String getWord()
        {
            return this.word;
        }

        public
        boolean isWordHere()
        {
            return this.wordHere;
        }

        public
        List<Character> getPaths()
        {
            return this.paths;
        }

        private
        void removePath(char c)
        {
            int    i    = ScrabbleUtil.charToInt(c);
            WGNode path = this.nodes[i];

            if (path != null)
            {
                this.nodes[i] = null;

                this.paths.remove(Character.valueOf(c));
            }
        }

        public
        WGNode getPath(char c)
        {
            return this.nodes[ScrabbleUtil.charToInt(c)];
        }

        private
        WGNode getOrCreatePath(char c)
        {
            int    i       = ScrabbleUtil.charToInt(c);
            WGNode current = this.nodes[i];

            if (current == null)
            {
                this.nodes[i] = new WGNode(this, this.word.concat(String.valueOf(c)));

                this.paths.add(c);
            }

            return this.nodes[i];
        }

    }

    private WGNode root = null;

    public
    WGNode getRoot()
    {
        return this.root;
    }

    private
    WGNode followPath(String path)
    {
        if (this.root == null)
        {
            return null;
        }

        WGNode current = this.root;

        for (int i = 0; i < path.length(); i++)
        {
            current = current.getPath(path.charAt(i));

            if (current == null)
            {
                return null;
            }
        }

        return current;
    }

    private
    WGNode getOrCreateRoot()
    {
        if (this.root == null)
        {
            this.root = new WGNode(null, "");
        }

        return this.root;
    }

    public
    boolean hasWord(String word)
    {
        WGNode path = this.followPath(word);

        return path != null && path.wordHere;
    }

    public
    boolean hasPrefix(String prefix)
    {
        return this.followPath(prefix) != null;
    }

    public
    void addWord(String word)
    {
        WGNode current = this.getOrCreateRoot();

        for (int i = 0; i < word.length(); i++)
        {
            current = current.getOrCreatePath(word.charAt(i));
        }

        current.wordHere = true;
    }

    public
    void removeWord(String word)
    {
        WGNode path = this.followPath(word);

        if (path == null || !path.wordHere)
        {
            return;
        }

        path.wordHere = false;

        for (int i = word.length() - 1; i >= 0; i--)
        {
            WGNode parent = path.parent;

            if (path.paths.size() == 0 && !path.wordHere)
            {
                parent.removePath(word.charAt(i));
            }
            else
            {
                return;
            }

            path = parent;
        }

        if (path.paths.size() == 0 && !path.wordHere)
        {
            this.root = null;
        }
    }

    public
    void forEach(Consumer<String> consumer)
    {
        if (this.root == null)
        {
            return;
        }

        this.forEach(this.root, consumer);
    }

    private
    void forEach(WGNode node, Consumer<String> consumer)
    {
        if (node.wordHere)
        {
            consumer.accept(node.word);
        }

        for (char c : node.paths)
        {
            this.forEach(node.getPath(c), consumer);
        }
    }

}
