package com.alexfh.scrabbleanalyzer;

import com.alexfh.scrabbleanalyzer.dictionary.WordGraphDictionary;
import com.alexfh.scrabbleanalyzer.gui.ScrabbleAnalyzer;
import com.alexfh.scrabbleanalyzer.gui.tile.DocumentProvider;

import javax.swing.*;
import java.io.IOException;

public class Main {

    public static WordGraphDictionary dictionary;

    public static void main(String[] args) throws IOException {
        Main.dictionary = WordGraphDictionary.fromInputStream(
            Main.class.getResourceAsStream("/nwl20.txt")
        );
        DocumentProvider.INSTANCE.init();
        SwingUtilities.invokeLater(ScrabbleAnalyzer::new);
    }

}
