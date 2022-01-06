package com.alexfh.scrabblesolver.state;

public interface IScrabbleBoard {

    static char[][] getNewEmptyBoard(int row, int col) {
        char[][] emptyBoard = new char[row][col];

        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                emptyBoard[r][c] = IScrabbleBoard.emptyMarker;
            }
        }

        return emptyBoard;
    }

    char emptyMarker = '.';

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
        return this.getCharAt(r, c) == IScrabbleBoard.emptyMarker;
    }

    int getAnchorRow();

    int getAnchorCol();

    IScrabbleBoard copy();

}
