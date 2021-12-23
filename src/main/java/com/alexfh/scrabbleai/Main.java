package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.state.impl.ScrabbleBoardImpl;
import com.alexfh.scrabbleai.dictionary.impl.WordGraphDictionary;
import com.alexfh.scrabbleai.rule.impl.LetterScoreMapImpl;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        String gameFolder = "src/main/resources/games/game1/";
        ScrabbleGame scrabbleGame = new ScrabbleGame(
            LetterScoreMapImpl.fromFile(
                new File("src/main/resources/scoremap.txt")
            ),
            WordGraphDictionary.fromFile(
                new File("src/main/resources/nwl20.txt")
            ),
            ScrabbleBoardImpl.fromFiles(
                new File(gameFolder + "board.txt"),
                new File("src/main/resources/multipliers.txt")
            )
        );
    }

}
