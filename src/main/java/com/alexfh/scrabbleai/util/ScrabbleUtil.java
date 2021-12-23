package com.alexfh.scrabbleai.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ScrabbleUtil {

    private static boolean isValidTiles(String s) {
        return s.matches("^[a-zA-Z0]*$");
    }

    public static int charToInt(char c) {
        return (int) c - 97;
    }

    public static char[] readPlayerTiles(File file) throws IOException {
        String playerTiles = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        if (!ScrabbleUtil.isValidTiles(playerTiles)) throw new IllegalStateException("Invalid player tiles");

        return playerTiles.toCharArray();
    }

    public static char[][] readRectangularBoardText(File file) throws IOException {
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

        return boardChars;
    }

}
