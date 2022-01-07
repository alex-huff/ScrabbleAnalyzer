package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.List;

public class MoveScroller extends JScrollPane {

    private final Dimension size = new Dimension(ScrabbleFrame.defaultTileSize * 8, ScrabbleFrame.defaultTileSize * 14);
    private final JList<String> jList;
    private List<ScrabbleGame.Move> currentMoves;
    private final Consumer<ScrabbleGame.Move> onMoveSelected;
    private final Runnable onMoveUnselected;
    private final Runnable onPlayMove;

    public MoveScroller(Consumer<ScrabbleGame.Move> onMoveSelected, Runnable onMoveUnselected, Runnable onPlayMove) {
        this.onMoveSelected = onMoveSelected;
        this.onMoveUnselected = onMoveUnselected;
        this.onPlayMove = onPlayMove;
        this.jList = new JList<>();

        this.jList.setLayoutOrientation(JList.VERTICAL);
        this.jList.addFocusListener(
            new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    MoveScroller.this.jList.clearSelection();
                }
            }
        );
        this.jList.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        int index = MoveScroller.this.jList.getSelectedIndex();

                        if (index >= 0) {
                            MoveScroller.this.onPlayMove.run();
                        }
                    }
                }
            }
        );
        this.jList.addListSelectionListener(
            listSelectionEvent -> {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    int index = this.jList.getSelectedIndex();

                    if (index >= 0) {
                        this.onMoveSelected.accept(this.currentMoves.get(index));
                    } else {
                        this.onMoveUnselected.run();
                    }
                }
            }
        );
        this.setViewportView(this.jList);
        this.makeScrollbarsFocusList();
    }

    private void makeScrollbarsFocusList() {
        this.makeScrollbarFocusList(this.getVerticalScrollBar());
        this.makeScrollbarFocusList(this.getHorizontalScrollBar());
    }

    private void makeScrollbarFocusList(JScrollBar scrollBar) {
        scrollBar.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) MoveScroller.this.jList.requestFocusInWindow();
                }
            }
        );
    }

    public void createListForMoves(List<ScrabbleGame.Move> moves) {
        this.currentMoves = moves;
        String[] listData = new String[this.currentMoves.size()];

        for (int i = 0; i < this.currentMoves.size(); i++) {
            ScrabbleGame.Move move = this.currentMoves.get(i);
            listData[i] =
                i + 1 +
                    " Score: " + move.score() +
                    " Vert: " + move.isVertical() +
                    " Row: " + move.row() +
                    " Col: " + move.col() +
                    " Word: " + move.playedWord() +
                    " Tiles: " + Arrays.toString(move.playedTiles());
        }

        this.jList.setListData(listData);
    }

    public void newSize(int width, int height) {
        this.size.setSize(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

}