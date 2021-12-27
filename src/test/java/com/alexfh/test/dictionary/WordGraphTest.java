package com.alexfh.test.dictionary;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alexfh.scrabbleai.ai.PermuteTree;
import com.alexfh.scrabbleai.dictionary.WordGraphDictionary;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class WordGraphTest {

    @Test
    public void testInclusion() {
        WordGraphDictionary dictionary = new WordGraphDictionary();

        dictionary.addWord("foo");
        dictionary.addWord("foobar");
        assertTrue(dictionary.hasWord("foo"));
        assertFalse(dictionary.hasWord("bar"));
        assertTrue(dictionary.hasWord("foobar"));
        dictionary.removeWord("foobar");
        assertFalse(dictionary.hasWord("foobar"));
        assertTrue(dictionary.hasWord("foo"));
    }

    @Test
    public void testPrefix() {
        WordGraphDictionary dictionary = new WordGraphDictionary();
        List<String> testWords = List.of("prefix", "foo", "bar", "foobar", "a", "dictionary");

        assertFalse(dictionary.hasPrefix("")); // should be false if dictionary is empty
        testWords.forEach(dictionary::addWord);
        testWords.forEach(
            testWord -> {
                for (int i = testWord.length(); i > 0; i--) {
                    assertTrue(dictionary.hasPrefix(testWord.substring(0, i)));
                }
            }
        );
        assertTrue(dictionary.hasPrefix("")); // should be true if dictionary contains any words
        assertFalse(dictionary.hasPrefix("refix"));
        assertFalse(dictionary.hasPrefix("ar"));
        assertFalse(dictionary.hasPrefix("i"));
        dictionary.removeWord("foobar");
        assertFalse(dictionary.hasPrefix("fooba"));
        testWords.forEach(dictionary::removeWord);
        assertFalse(dictionary.hasPrefix("")); // should be false if dictionary is empty
    }

    @Test
    public void nwl20() throws IOException {
        WordGraphDictionary dictionary = WordGraphDictionary.fromFile(new File("src/test/resources/nwl20.txt"));
        List<String> words = new ArrayList<>();

        dictionary.forEach(words::add);
        assertEquals(words.size(), 191852);
        words.forEach(word -> assertTrue(dictionary.hasWord(word)));
        words.forEach(dictionary::removeWord);
        assertFalse(dictionary.hasPrefix(""));
    }

}
