package com.alexfh.scrabbleai;

import com.alexfh.scrabbleai.state.impl.ScrabbleBoardImpl;
import com.alexfh.scrabbleai.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleai.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabbleai.util.ScrabbleUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        ScrabbleUtil.timeIt(Main::start, "main");
    }

    private static void start() {
        ScrabbleGame scrabbleGame;

        try {
            String gameFolder = "src/main/resources/games/game1/";
            scrabbleGame = new ScrabbleGame(
                LetterScoreMapImpl.fromFile(
                    new File("src/main/resources/scoremap.txt")
                ),
                WordGraphDictionary.fromFile(
                    new File("src/main/resources/nwl20.txt")
                ),
                ScrabbleBoardImpl.fromFiles(
                    new File(gameFolder + "board.txt"),
                    new File("src/main/resources/multipliers.txt")
                ),
                ScrabbleUtil.readPlayerTiles(
                    new File(gameFolder + "currentletters.txt")
                )
            );
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        List<ScrabbleGame.Move> moves = ScrabbleUtil.timeRetrieval(scrabbleGame::findMoves, "findMoves");

        System.out.println(moves.size());
        ScrabbleUtil.timeIt(() -> Collections.sort(moves), "sort");
        moves.forEach(
            move -> System.out.println(
                "Score: " + move.score() +
                    " Vert: " + move.isVertical() +
                    " Row: " + move.row() +
                    " Col: " + move.col() +
                    " Word: " + move.playedWord() +
                    " Tiles: " + Arrays.toString(move.playedTiles())
            )
        );
    }

}
