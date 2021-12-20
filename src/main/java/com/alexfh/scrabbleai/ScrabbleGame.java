package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.dictionary.IDictionary;

public class ScrabbleGame {

    private final LetterScoreMap letterScoreMap;
    private final IDictionary dictionary;

    public ScrabbleGame(LetterScoreMap letterScoreMap, IDictionary dictionary) {
        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
    }

}
