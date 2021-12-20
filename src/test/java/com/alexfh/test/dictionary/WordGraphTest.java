package com.alexfh.test.dictionary;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.alexfh.scrabbleai.dictionary.WordGraph;
import com.alexfh.scrabbleai.dictionary.IDictionary;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WordGraphTest {

    @Test
    public void testInclusion() {
        IDictionary dictionary = new WordGraph();

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
        IDictionary dictionary = new WordGraph();
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
        IDictionary dictionary = new WordGraph();
        BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/nwl20.txt", StandardCharsets.UTF_8));
        List<String> words = new ArrayList<>();
        String line;

        while((line = reader.readLine()) != null) {
            words.add(line.toLowerCase().strip());
        }

        words.forEach(dictionary::addWord);
        words.forEach(word -> assertTrue(dictionary.hasWord(word)));
        words.forEach(dictionary::removeWord);
        assertFalse(dictionary.hasPrefix(""));
    }

}
