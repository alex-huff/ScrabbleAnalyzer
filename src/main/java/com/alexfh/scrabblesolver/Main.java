package com.alexfh.scrabblesolver;

import com.alexfh.scrabblesolver.rule.ILetterScoreMap;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.state.impl.ScrabbleBoardImpl;
import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        ScrabbleUtil.timeIt(Main::start, "main");
        ScrabbleGame.threadPool.shutdown();
    }

    private static void start() {
        String gameFolder = "src/main/resources/games/game1/";
        ScrabbleGame scrabbleGame;
        ILetterScoreMap letterScoreMap;
        WordGraphDictionary dictionary;
        IScrabbleBoard board;
        char[] playerTiles;
        int handSize = 7;

        try {
            letterScoreMap = LetterScoreMapImpl.fromFile(
                new File("src/main/resources/scoremap.txt")
            );
            dictionary = WordGraphDictionary.fromFile(
                new File("src/main/resources/nwl20.txt")
            );
            board = ScrabbleBoardImpl.fromFiles(
                new File(gameFolder + "board.txt"),
                new File("src/main/resources/multipliers.txt")
            );
            playerTiles = ScrabbleUtil.readPlayerTiles(
                new File(gameFolder + "currentletters.txt")
            );
        } catch (IOException e) {
            e.printStackTrace();

            return;
        }

        scrabbleGame = new ScrabbleGame(letterScoreMap, dictionary, board, playerTiles, handSize);
        List<ScrabbleGame.Move> moves = ScrabbleUtil.timeRetrieval(scrabbleGame::findMoves, "findMoves");

        ScrabbleUtil.timeIt(() -> Collections.sort(moves), "sort");

        for (int i = 0; i < moves.size(); i++) {
            ScrabbleGame.Move move = moves.get(i);

            System.out.println(
                i + 1 +
                " Score: " + move.score() +
                " Vert: " + move.isVertical() +
                " Row: " + move.row() +
                " Col: " + move.col() +
                " Word: " + move.playedWord() +
                " Tiles: " + Arrays.toString(move.playedTiles())
            );
        }
    }

}
