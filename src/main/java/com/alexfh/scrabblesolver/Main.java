package com.alexfh.scrabblesolver;

import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.gui.ScrabbleAnalyzer;
import com.alexfh.scrabblesolver.gui.tile.DocumentProvider;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.state.impl.ScrabbleBoardImpl;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Main {

    public static WordGraphDictionary dictionary;

    static {
        try {
            Main.dictionary = WordGraphDictionary.fromFile(
                new File("src/main/resources/nwl20.txt")
            );
        } catch (IOException e) {
            Main.dictionary = new WordGraphDictionary();

            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        DocumentProvider.INSTANCE.init();

        String gameFolder = "src/main/resources/games/game6/";
        IScrabbleBoard board = ScrabbleBoardImpl.fromFiles(
            new File(gameFolder + "board.txt"),
            new File("src/main/resources/multipliers.txt")
        );
        char[] playerTiles = ScrabbleUtil.readPlayerTiles(
            new File(gameFolder + "currentletters.txt")
        );
//        IScrabbleBoard board = ScrabbleBoardImpl.blankBoard();
//        char[] playerTiles = new char[0];

        SwingUtilities.invokeLater(() -> new ScrabbleAnalyzer(board, playerTiles));
    }

}
