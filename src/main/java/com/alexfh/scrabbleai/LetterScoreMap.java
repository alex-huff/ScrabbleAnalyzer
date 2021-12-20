package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.io.*;

public class LetterScoreMap {

    public static LetterScoreMap fromFile(File scoreMapFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(scoreMapFile));
        int[] scoreMap = new int[26];
        String line;

        while ((line = reader.readLine()) != null) {
            String[] keyValue = line.split(":");

            if (keyValue.length != 2 || keyValue[0].length() == 0) continue;

            char c = keyValue[0].charAt(0);
            int score;

            try {
                score = Integer.parseInt(keyValue[1]);
                scoreMap[ScrabbleUtil.charToInt(c)] = score;
            } catch (NumberFormatException ignored) { }
        }

        reader.close();

        return new LetterScoreMap(scoreMap);
    }

    private final int[] scoreMap;

    private LetterScoreMap(int[] scoreMap) {
        this.scoreMap = scoreMap;
    }

    public int getScore(char c) {
        return this.scoreMap[ScrabbleUtil.charToInt(c)];
    }

}
