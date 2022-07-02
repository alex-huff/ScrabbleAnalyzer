package com.alexfh.scrabbleanalyzer.gui.layout;

import com.alexfh.scrabbleanalyzer.gui.ScrabbleAnalyzerPanel;

import java.awt.*;

public
class ScrabbleAnalyzerPanelLayout extends GridBagLayout
{

    @Override
    protected
    void arrangeGrid(Container parent)
    {
        if (parent instanceof ScrabbleAnalyzerPanel scrabbleAnalyzerPanel)
        {
            Dimension dimension = scrabbleAnalyzerPanel.getSize();

            scrabbleAnalyzerPanel.onResize((int) dimension.getWidth(), (int) dimension.getHeight());
        }

        super.arrangeGrid(parent);
    }

}
