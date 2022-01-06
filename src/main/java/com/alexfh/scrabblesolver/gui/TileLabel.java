package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TileLabel extends JLabel {

    private final Consumer<Boolean> onClicked;
    private final BiConsumer<Character, Boolean> letterPressed;
    private final ImageIcon icon;

    public TileLabel(ImageIcon icon, Consumer<Boolean> onClicked, BiConsumer<Character, Boolean> letterPressed) {
        super(icon);

        this.icon = icon;
        this.onClicked = onClicked;
        this.letterPressed = letterPressed;

        this.setFocusable(true);
        this.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        TileLabel.this.requestFocusInWindow();
                        TileLabel.this.onClicked.accept(true);
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        TileLabel.this.onClicked.accept(false);
                    }
                }
            }
        );
        this.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();

                    if (Character.isAlphabetic(c) || c == ScrabbleUtil.wildCardMarker)
                        TileLabel.this.letterPressed.accept(Character.toLowerCase(c), e.isShiftDown());
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        TileLabel.this.letterPressed.accept(ScrabblePanel.backspaceChar, e.isShiftDown());
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