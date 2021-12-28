package com.alexfh.scrabblesolver.state;

public interface IScrabbleBoard {

    int getRows();

    int getCols();

    int getLetterMultiplierAt(int r, int c);

    int getWordMultiplierAt(int r, int c);

    char getCharAt(int r, int c);

    boolean isWildcardAt(int r, int c);

    boolean isEmptyAt(int r, int c);

    int getAnchorRow();

    int getAnchorCol();

}
