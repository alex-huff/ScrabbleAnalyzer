package com.alexfh.scrabbleanalyzer.rule.impl;

import com.alexfh.scrabbleanalyzer.rule.ILetterScoreMap;
import com.alexfh.scrabbleanalyzer.util.ScrabbleUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

@SuppressWarnings("ClassCanBeRecord")
public class LetterScoreMapImpl implements ILetterScoreMap {

    public static final LetterScoreMapImpl defaultScoreMap = new LetterScoreMapImpl(
        new int[] {
            1,
            4,
            4,
            2,
            1,
            4,
            3,
            3,
            1,
            10,
            5,
            2,
            4,
            2,
            1,
            4,
            10,
            1,
            1,
            1,
            2,
            5,
            4,
            8,
            3,
            10
        }
    );
    
    public static LetterScoreMapImpl fromFile(File scoreMapFile) throws IOException {
        String scoreMapData = Files.readString(scoreMapFile.toPath(), StandardCharsets.UTF_8);
        int[] scoreMap = new int[26];

        Arrays.stream(scoreMapData.split(ScrabbleUtil.newLineRegex)).forEach(
            line -> {
                String[] keyValue = line.split(":");

                if (keyValue.length != 2 || keyValue[0].length() == 0) return;

                char c = keyValue[0].charAt(0);
                int score;

                try {
                    score = Integer.parseInt(keyValue[1]);
                    scoreMap[ScrabbleUtil.charToInt(c)] = score;
                } catch (NumberFormatException ignored) { }
            }
        );

        return new LetterScoreMapImpl(scoreMap);
    }

    private final int[] scoreMap;

    private LetterScoreMapImpl(int[] scoreMap) {
        this.scoreMap = scoreMap;
    }

    public int getScore(char c) {
        return this.scoreMap[ScrabbleUtil.charToInt(c)];
    }

}
