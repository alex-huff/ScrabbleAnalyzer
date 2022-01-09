package com.alexfh.scrabblesolver.state;

public interface IScrabbleBoard {

    int getRows();

    int getCols();

    int getLetterMultiplierAt(int r, int c);

    int getWordMultiplierAt(int r, int c);

    char getCharAt(int r, int c);

    void setCharAt(int r, int c, char newChar);

    void removeCharAt(int r, int c);

    boolean isWildcardAt(int r, int c);

    void setWildcardAt(int r, int c, boolean isWild);

    default boolean isEmptyAt(int r, int c) {
        return this.getCharAt(r, c) == IScrabbleGameState.emptyMarker;
    }

    int getAnchorRow();

    int getAnchorCol();

    IScrabbleBoard copyBoard();

}
