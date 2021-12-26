package com.alexfh.scrabbleai.state.impl;

import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.util.structure.ImmutablePair;
import com.alexfh.scrabbleai.util.structure.Pair;
import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.io.File;
import java.io.IOException;

public class ScrabbleBoardImpl implements IScrabbleBoard {

    private static final char emptyMarker = '.';

    public static ScrabbleBoardImpl fromFiles(File gameFile, File multipliersFile) throws IOException {
        Pair<char[][], boolean[][]> gameData = ScrabbleBoardImpl.readGameFile(gameFile);
        Pair<int[][], int[][]> multipliersData = ScrabbleBoardImpl.readMultipliersFile(multipliersFile);
        char[][] playedTiles = gameData.getLeft();
        boolean[][] wildcardTiles = gameData.getRight();
        int[][] letterMultipliers = multipliersData.getLeft();
        int[][] wordMultipliers = multipliersData.getRight();
        int gameRows = playedTiles.length;
        int gameCols = playedTiles[0].length;
        int multiplierRows = letterMultipliers.length;
        int multiplierCols = letterMultipliers[0].length;

        if (gameRows != multiplierRows) throw new IllegalStateException("Game board rows are not equal to multiplier rows: " + gameRows + "!=" + multiplierRows);
        if (gameCols != multiplierCols) throw new IllegalStateException("Game board columns are not equal to multiplier columns: " + gameCols + "!=" + multiplierCols);

        return new ScrabbleBoardImpl(gameRows, gameCols, letterMultipliers, wordMultipliers, playedTiles, wildcardTiles);
    }

    private static Pair<int[][], int[][]> readMultipliersFile(File multipliersFile) throws IOException {
        char[][] boardText;

        try {
            boardText = ScrabbleUtil.readRectangularBoardText(multipliersFile);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to read multipliers board", e);
        }

        int rows = boardText.length;
        int cols = boardText[0].length;
        int[][] letterMultipliers = new int[rows][cols];
        int[][] wordMultipliers = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char b = boardText[r][c];
                boolean isAlpha = Character.isAlphabetic(b);
                boolean isUpper = Character.isUpperCase(b);

                if (isAlpha) {
                    String asString = Character.toString(b);
                    int modifier;

                    if (asString.equalsIgnoreCase("d")) {
                        modifier = 2;
                    } else if (asString.equalsIgnoreCase("t")) {
                        modifier = 3;
                    } else {
                        throw new IllegalStateException("Invalid multiplier: '" + b + "'");
                    }

                    letterMultipliers[r][c] = !isUpper ? modifier : 1;
                    wordMultipliers[r][c] = isUpper ? modifier : 1;
                } else {
                    if (b != ScrabbleBoardImpl.emptyMarker) throw new IllegalStateException("Invalid character in multipliers board");

                    letterMultipliers[r][c] = 1;
                    wordMultipliers[r][c] = 1;
                }
            }
        }

        return new ImmutablePair<>(letterMultipliers, wordMultipliers);
    }

    private static Pair<char[][], boolean[][]> readGameFile(File gameFile) throws IOException {
        char[][] boardText;

        try {
            boardText = ScrabbleUtil.readRectangularBoardText(gameFile);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to read game board", e);
        }

        int rows = boardText.length;
        int cols = boardText[0].length;
        char[][] playedTiles = new char[rows][cols];
        boolean[][] wildcardTiles = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char b = boardText[r][c];
                boolean isAlpha = Character.isAlphabetic(b);
                boolean isUpper = Character.isUpperCase(b);

                if (!isAlpha && b != ScrabbleBoardImpl.emptyMarker) throw new IllegalStateException("Invalid character: '" + b + "' in game board");

                playedTiles[r][c] = isAlpha ? (isUpper ? Character.toLowerCase(b) : b) : ScrabbleBoardImpl.emptyMarker;
                wildcardTiles[r][c] = isAlpha && (!isUpper);
            }
        }

        return new ImmutablePair<>(playedTiles, wildcardTiles);
    }

    private final int rows;
    private final int cols;
    private final int[][] letterMultipliers;
    private final int[][] wordMultipliers;
    private final char[][] playedTiles;
    private final boolean[][] wildcardTiles;

    public ScrabbleBoardImpl(int rows, int cols, int[][] letterMultipliers, int[][] wordMultipliers, char[][] playedTiles, boolean[][] wildcardTiles) {
        this.rows = rows;
        this.cols = cols;
        this.letterMultipliers = letterMultipliers;
        this.wordMultipliers = wordMultipliers;
        this.playedTiles = playedTiles;
        this.wildcardTiles = wildcardTiles;
    }

    @Override
    public int getRows() {
        return this.rows;
    }

    @Override
    public int getCols() {
        return this.cols;
    }

    @Override
    public int getLetterMultiplierAt(int r, int c) {
        return this.letterMultipliers[r][c];
    }

    @Override
    public int getWordMultiplierAt(int r, int c) {
        return this.wordMultipliers[r][c];
    }

    @Override
    public char getCharAt(int r, int c) {
        return this.playedTiles[r][c];
    }

    @Override
    public boolean isWildcardAt(int r, int c) {
        return this.wildcardTiles[r][c];
    }

    @Override
    public boolean isEmptyAt(int r, int c) {
        return this.playedTiles[r][c] == ScrabbleBoardImpl.emptyMarker;
    }

}
