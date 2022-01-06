package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class ScrabbleFrame extends JFrame {

    private static final Dimension screenWidth = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int defaultTileSize = (int) (ScrabbleFrame.screenWidth.getHeight() * .75F / 15);

    public ScrabbleFrame(IScrabbleBoard board, char[] playerTiles) {
        BufferedImage iconImage = TileProvider.INSTANCE.getTile(
            'a',
            true,
            false,
            false,
            50
        );

        this.setTitle("ScrabbleAnalyzer");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(400, 400));
        this.setIconImage(iconImage);
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    ScrabbleGame.threadPool.shutdownNow();
                }
            }
        );

        this.add(new ScrabblePanel(board, new ArrayList<>(Arrays.asList(new String(playerTiles).chars().mapToObj(i -> (char) i).toArray(Character[]::new)))));
        this.pack();
        this.setVisible(true);
    }

}
