package com.alexfh.scrabbleai.dictionary;

import java.util.function.Consumer;

public interface IDictionary {

    boolean hasWord(String word);

    boolean hasPrefix(String prefix);

    void addWord(String word);

    void removeWord(String word);

    void forEach(Consumer<String> consumer);

}
