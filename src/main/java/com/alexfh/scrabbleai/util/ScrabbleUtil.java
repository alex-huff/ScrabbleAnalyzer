package com.alexfh.scrabbleai.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ScrabbleUtil {

    public static final String newLineRegex = "\\R";
    public static final String alpha = "abcdefghijklmnopqrstuvwxyz";
    public static final char[] alphaChars = ScrabbleUtil.alpha.toCharArray();

    private static boolean isValidTiles(String s) {
        return s.matches("^[a-zA-Z0]*$");
    }

    public static int charToInt(char c) {
        return c - 97;
    }

    public static char intToChar(int i) {
        return (char) (i + 97);
    }

    public static char[] readPlayerTiles(File file) throws IOException {
        String playerTiles = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        if (!ScrabbleUtil.isValidTiles(playerTiles)) throw new IllegalStateException("Invalid player tiles");

        return playerTiles.toCharArray();
    }

    public static char[][] readRectangularBoardText(File file) throws IOException {
        String boardText = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        if (boardText.isEmpty() || boardText.isBlank()) throw new IllegalStateException("Empty board");

        String[] lines = boardText.split(ScrabbleUtil.newLineRegex);
        int rows = lines.length;
        int cols = lines[0].length();
        char[][] boardChars = new char[rows][cols];

        for (int r = 0; r < rows; r++) {
            String line = lines[r].strip();

            if (line.length() != cols) throw new IllegalStateException("Non-rectangular board");

            boardChars[r] = line.toCharArray();
        }

        return boardChars;
    }

}
