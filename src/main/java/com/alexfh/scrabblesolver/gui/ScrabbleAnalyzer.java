package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.state.IScrabbleBoard;

public class ScrabbleAnalyzer {

    private final ScrabbleFrame scrabbleFrame;

    public ScrabbleAnalyzer(IScrabbleBoard board, char[] playerTiles) {
        this.scrabbleFrame = new ScrabbleFrame(this, board, playerTiles);
    }

    public boolean isSaved() {
        return false;
    }

}
