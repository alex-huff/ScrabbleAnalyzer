package com.alexfh.scrabbleanalyzer.gui.layout;

import com.alexfh.scrabbleanalyzer.gui.ScrabblePanel;

import java.awt.*;

public class ScrabbleLayout extends GridBagLayout
{

    @Override
    protected void arrangeGrid(Container parent)
    {
        if (parent instanceof ScrabblePanel scrabblePanel)
        {
            Dimension dimension = scrabblePanel.getSize();

            scrabblePanel.onResize((int) dimension.getWidth(), (int) dimension.getHeight());
        }

        super.arrangeGrid(parent);
    }

}