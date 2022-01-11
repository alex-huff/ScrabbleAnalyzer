package com.alexfh.scrabbleanalyzer.gui;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class TileLabel extends JLabel {

    private final Consumer<Boolean> onClicked;
    private final ImageIcon icon;

    public TileLabel(ImageIcon icon, Consumer<Boolean> onClicked) {
        super(icon);

        this.icon = icon;
        this.onClicked = onClicked;

        this.setFocusable(false);
        this.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        TileLabel.this.onClicked.accept(true);
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        TileLabel.this.onClicked.accept(false);
                    }
                }
            }
        );
    }

    @Override
    public ImageIcon getIcon() {
        return this.icon;
    }

}