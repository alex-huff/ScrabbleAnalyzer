package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.dictionary.IDictionary;
import com.alexfh.scrabbleai.rule.ILetterScoreMap;

public class ScrabbleGame {

    private final ILetterScoreMap letterScoreMap;
    private final IDictionary dictionary;
    private final IScrabbleBoard board;

    public ScrabbleGame(ILetterScoreMap letterScoreMap, IDictionary dictionary, IScrabbleBoard board) {
        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
        this.board = board;
    }

}
