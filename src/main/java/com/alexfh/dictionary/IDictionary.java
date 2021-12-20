package com.alexfh.dictionary;

public interface IDictionary {

    boolean hasWord(String word);

    boolean hasPrefix(String prefix);

    void addWord(String word);

    void removeWord(String word);

}
