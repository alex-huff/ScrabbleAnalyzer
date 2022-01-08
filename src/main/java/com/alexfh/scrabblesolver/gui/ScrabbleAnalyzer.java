package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.gui.action.Action;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;

public class ScrabbleAnalyzer {

    private final ScrabbleFrame scrabbleFrame;

    public ScrabbleAnalyzer(IScrabbleBoard board, char[] playerTiles) {
        this.scrabbleFrame = new ScrabbleFrame(this, this::onAction, board, playerTiles);
    }

    public boolean isSaved() {
        return false;
    }

    public void onAction(Action action) {

    }

    public void undo() {
        System.out.println("Undoing");
    }

    public void redo() {
        System.out.println("Redoing");
    }

}
