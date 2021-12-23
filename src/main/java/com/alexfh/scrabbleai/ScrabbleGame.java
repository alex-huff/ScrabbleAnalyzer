package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.dictionary.IDictionary;
import com.alexfh.scrabbleai.rule.ILetterScoreMap;

public class ScrabbleGame {

    private final ILetterScoreMap letterScoreMap;
    private final IDictionary dictionary;
    private final IScrabbleBoard board;
    private final char[] playerTiles;

    public ScrabbleGame(ILetterScoreMap letterScoreMap, IDictionary dictionary, IScrabbleBoard board, char[] playerTiles) {
        this.letterScoreMap = letterScoreMap;
        this.dictionary = dictionary;
        this.board = board;
        this.playerTiles = playerTiles;
    }

}
