package com.alexfh.scrabblesolver;

import com.alexfh.scrabblesolver.dictionary.WordGraphDictionary;
import com.alexfh.scrabblesolver.gui.ScrabbleFrame;
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

        SwingUtilities.invokeLater(
            () -> {
//                try {
//                    UIManager.setLookAndFeel(new NimbusLookAndFeel());
//                } catch (UnsupportedLookAndFeelException e) {
//                    e.printStackTrace();
//                }

//                UIManager.put("control", new Color(128, 128, 128));
//                UIManager.put("info", new Color(128, 128, 128));
//                UIManager.put("nimbusBase", new Color(18, 30, 49));
//                UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
//                UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
//                UIManager.put("nimbusFocus", new Color(115, 164, 209));
//                UIManager.put("nimbusGreen", new Color(176, 179, 50));
//                UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
//                UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
//                UIManager.put("nimbusOrange", new Color(191, 98, 4));
//                UIManager.put("nimbusRed", new Color(169, 46, 34));
//                UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
//                UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
//                UIManager.put("text", new Color(230, 230, 230));

                new ScrabbleFrame(board, playerTiles);
            }
        );
    }

}
