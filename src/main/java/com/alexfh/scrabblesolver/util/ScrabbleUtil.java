package com.alexfh.scrabblesolver.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

public class ScrabbleUtil {

    public static final char wildCardMarker = '0';
    public static final char wildCardTile = '{';
    public static final String newLineRegex = "\\R";
    public static final String alpha = "abcdefghijklmnopqrstuvwxyz";
    public static final char[] alphaChars = ScrabbleUtil.alpha.toCharArray();

    public static <T> T timeRetrieval(Supplier<T> supplier, String message) {
        long start = System.nanoTime();

        T t = supplier.get();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");

        return t;
    }

    public static void timeIt(Runnable runnable, String message) {
        long start = System.nanoTime();

        runnable.run();

        long finish = System.nanoTime();

        System.out.println(message + " " + 1.0D * (finish - start) / 1000000000 + "s");
    }

    private static boolean isValidTiles(String s) {
        return s.matches("^[a-zA-Z" + ScrabbleUtil.wildCardTile + "]*$");
    }

    public static int charToInt(char c) {
        return c - 97;
    }

    public static char intToChar(int i) {
        return (char) (i + 97);
    }

    public static char[] readPlayerTiles(File file) throws IOException {
        String playerTiles = Files.readString(file.toPath(), StandardCharsets.UTF_8)
            .strip()
            .toLowerCase()
            .replaceAll(String.valueOf(ScrabbleUtil.wildCardMarker), String.valueOf(ScrabbleUtil.wildCardTile));

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
