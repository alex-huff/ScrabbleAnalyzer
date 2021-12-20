package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.dictionary.WordGraph;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ScrabbleGame scrabbleGame = new ScrabbleGame(
            LetterScoreMap.fromFile(
                new File("src/main/resources/scoremap.txt")
            ),
            WordGraph.fromFile(new File("src/test/resources/nwl20.txt"))
        );
    }

}
