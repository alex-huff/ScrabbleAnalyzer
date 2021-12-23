package com.alexfh.scrabbleai.state.impl;

import com.alexfh.scrabbleai.state.IScrabbleBoard;
import com.alexfh.scrabbleai.util.ImmutablePair;
import com.alexfh.scrabbleai.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScrabbleBoardImpl implements IScrabbleBoard {

    public static ScrabbleBoardImpl fromFiles(File gameFile, File multipliersFile) throws IOException {
        Pair<Pair<Integer, Integer>, Pair<char[][], boolean[][]>> gameData = ScrabbleBoardImpl.readGameFile(gameFile);
        Pair<Pair<Integer, Integer>, Pair<int[][], int[][]>> multipliersData = ScrabbleBoardImpl.readMultipliersFile(multipliersFile);
        Pair<Integer, Integer> gameDimensions = gameData.getLeft();
        Pair<Integer, Integer> multipliersDimensions = multipliersData.getLeft();
        int gameRows = gameDimensions.getLeft();
        int gameCols = gameDimensions.getRight();
        int multiplierRows = multipliersDimensions.getLeft();
        int multiplierCols = multipliersDimensions.getRight();

        if (gameRows != multiplierRows) throw new IllegalStateException("Game board rows are not equal to multiplier rows: " + gameRows + "!=" + multiplierRows);
        if (gameCols != multiplierCols) throw new IllegalStateException("Game board columns are not equal to multiplier columns: " + gameCols + "!=" + multiplierCols);

        Pair<char[][], boolean[][]> tileMaps = gameData.getRight();
        char[][] playedTiles = tileMaps.getLeft();
        boolean[][] wildcardTiles = tileMaps.getRight();
        Pair<int[][], int[][]> multiplierTiles = multipliersData.getRight();
        int[][] letterMultipliers = multiplierTiles.getLeft();
        int[][] wordMultipliers = multiplierTiles.getRight();

        return new ScrabbleBoardImpl(gameRows, gameCols, letterMultipliers, wordMultipliers, playedTiles, wildcardTiles);
    }

    private static Pair<Pair<Integer, Integer>, Pair<int[][], int[][]>> readMultipliersFile(File multipliersFile) throws IOException {
        Pair<Pair<Integer, Integer>, char[][]> rectangularBoardText;

        try {
            rectangularBoardText = ScrabbleBoardImpl.readRectangularBoardText(multipliersFile);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to read multipliers board", e);
        }

        Pair<Integer, Integer> dimensions = rectangularBoardText.getLeft();
        char[][] boardText = rectangularBoardText.getRight();
        int rows = dimensions.getLeft();
        int cols = dimensions.getRight();
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
                    if (b != '.') throw new IllegalStateException("Invalid character in multipliers board");

                    letterMultipliers[r][c] = 1;
                    wordMultipliers[r][c] = 1;
                }
            }
        }

        return new ImmutablePair<>(new ImmutablePair<>(rows, cols), new ImmutablePair<>(letterMultipliers, wordMultipliers));
    }

    private static Pair<Pair<Integer, Integer>, Pair<char[][], boolean[][]>> readGameFile(File gameFile) throws IOException {
        Pair<Pair<Integer, Integer>, char[][]> rectangularBoardText;

        try {
            rectangularBoardText = ScrabbleBoardImpl.readRectangularBoardText(gameFile);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to read game board", e);
        }

        Pair<Integer, Integer> dimensions = rectangularBoardText.getLeft();
        char[][] boardText = rectangularBoardText.getRight();
        int rows = dimensions.getLeft();
        int cols = dimensions.getRight();
        char[][] playedTiles = new char[rows][cols];
        boolean[][] wildcardTiles = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char b = boardText[r][c];
                boolean isAlpha = Character.isAlphabetic(b);
                boolean isUpper = Character.isUpperCase(b);

                if (!isAlpha && b != '.') throw new IllegalStateException("Invalid character: '" + b + "' in game board");

                playedTiles[r][c] = isAlpha ? (isUpper ? Character.toLowerCase(b) : b) : '.';
                wildcardTiles[r][c] = isAlpha && (!isUpper);
            }
        }

        return new ImmutablePair<>(new ImmutablePair<>(rows, cols), new ImmutablePair<>(playedTiles, wildcardTiles));
    }

    private static Pair<Pair<Integer, Integer>, char[][]> readRectangularBoardText(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
        List<String> boardText = new ArrayList<>();
        String line = reader.readLine();

        if (line == null) throw new IllegalStateException("Empty board");

        int cols = line.length();
        int rows = 1;

        boardText.add(line);

        while ((line = reader.readLine()) != null) {
            if (line.length() != cols) throw new IllegalStateException("Non-rectangular board");

            boardText.add(line);
            rows++;
        }

        reader.close();

        char[][] boardChars = new char[rows][cols];

        for (int r = 0; r < rows; r++) {
            boardChars[r] = boardText.get(r).toCharArray();
        }

        return new ImmutablePair<>(new ImmutablePair<>(rows, cols), boardChars);
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

}
