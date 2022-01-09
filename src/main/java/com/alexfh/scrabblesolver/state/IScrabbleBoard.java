package com.alexfh.scrabblesolver.state;

import com.alexfh.scrabblesolver.gui.action.RevertableAction;

public interface IScrabbleBoard {

    int getRows();

    int getCols();

    int getLetterMultiplierAt(int r, int c);

    int getWordMultiplierAt(int r, int c);

    char getCharAt(int r, int c);

    RevertableAction setCharAt(int r, int c, char newChar);

    RevertableAction removeCharAt(int r, int c);

    boolean isWildcardAt(int r, int c);

    RevertableAction setWildcardAt(int r, int c, boolean isWild);

    default boolean isEmptyAt(int r, int c) {
        return this.getCharAt(r, c) == IScrabbleGameState.emptyMarker;
    }

    int getAnchorRow();

    int getAnchorCol();

    IScrabbleBoard copyBoard();

}
