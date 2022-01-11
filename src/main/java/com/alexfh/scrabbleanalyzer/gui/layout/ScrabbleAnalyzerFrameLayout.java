package com.alexfh.scrabbleanalyzer.gui.layout;

import com.alexfh.scrabbleanalyzer.gui.ScrabbleAnalyzer;

import java.awt.*;

public class ScrabbleAnalyzerFrameLayout extends GridBagLayout {

    private final ScrabbleAnalyzer scrabbleAnalyzer;

    public ScrabbleAnalyzerFrameLayout(ScrabbleAnalyzer scrabbleAnalyzer) {
        this.scrabbleAnalyzer = scrabbleAnalyzer;
    }

    @Override
    protected void arrangeGrid(Container parent) {
        Dimension dimension = parent.getSize();

        this.scrabbleAnalyzer.onResize((int) dimension.getWidth(), (int) dimension.getHeight());

        super.arrangeGrid(parent);
    }

}
