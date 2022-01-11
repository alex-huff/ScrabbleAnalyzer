package com.alexfh.scrabbleanalyzer;

import com.alexfh.scrabbleanalyzer.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleanalyzer.gui.ScrabbleAnalyzer;
import com.alexfh.scrabbleanalyzer.gui.tile.DocumentProvider;

import javax.swing.*;
import java.io.IOException;

public class Main {

    public static WordGraphDictionary dictionary;

    static {
        Main.dictionary = WordGraphDictionary.fromInputStream(
            Main.class.getResourceAsStream("/nwl20.txt")
        );
    }

    public static void main(String[] args) throws IOException {
        DocumentProvider.INSTANCE.init();
        SwingUtilities.invokeLater(
            () -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }

                new ScrabbleAnalyzer();
            }
        );
    }

}
