package com.alexfh.scrabblesolver;

import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.gui.ScrabbleFrame;
import com.alexfh.scrabblesolver.gui.tile.DocumentProvider;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.state.impl.ScrabbleGameStateImpl;

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
//        IScrabbleGameState gameState = ScrabbleGameStateImpl.fromFiles(
//            new File(gameFolder + "board.txt"),
//            new File("src/main/resources/multipliers.txt"),
//            new File(gameFolder + "currentletters.txt")
//        );
        IScrabbleGameState gameState = ScrabbleGameStateImpl.defaultBlankScrabbleGameState();

        SwingUtilities.invokeLater(
            () -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }

                new ScrabbleFrame(gameState);
            }
        );
    }

}
