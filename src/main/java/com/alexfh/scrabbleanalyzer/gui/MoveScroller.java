package com.alexfh.scrabbleanalyzer.gui;

import com.alexfh.scrabbleanalyzer.ScrabbleGame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import java.util.List;

public class MoveScroller extends JScrollPane {

    private static final Font FONT = new Font("Courier New", Font.BOLD, 16);

    private final JTable table;
    private List<ScrabbleGame.Move> currentMoves;
    private final Consumer<ScrabbleGame.Move> onMoveSelected;
    private final Consumer<ScrabbleGame.Move> onPlayMove;
    private final String[] colNames = new String[] { "Number", "Score", "Word" };

    public MoveScroller(Consumer<ScrabbleGame.Move> onMoveSelected, Consumer<ScrabbleGame.Move> onPlayMove) {
        this.onMoveSelected = onMoveSelected;
        this.onPlayMove = onPlayMove;
        this.table = new JTable(this.getModelFromData(new String[0][0]));

        this.setPreferredSize(new Dimension(ScrabbleAnalyzer.defaultTileSize * 8, ScrabbleAnalyzer.defaultTileSize * 14));
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setFont(MoveScroller.FONT);
        this.table.getTableHeader().setFont(MoveScroller.FONT);
        this.table.setFillsViewportHeight(true);
        this.table.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        int index = MoveScroller.this.table.getSelectedRow();

                        if (index >= 0) {
                            MoveScroller.this.onPlayMove.accept(MoveScroller.this.currentMoves.get(index));
                        }
                    }
                }
            }
        );
        this.table.getSelectionModel().addListSelectionListener(
            listSelectionEvent -> {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    int index = this.table.getSelectedRow();

                    if (index >= 0) {
                        this.onMoveSelected.accept(this.currentMoves.get(index));
                    }
                }
            }
        );
        this.setViewportView(this.table);
        this.makeScrollbarsFocusTable();
    }

    private void makeScrollbarsFocusTable() {
        this.makeScrollbarFocusTable(this.getVerticalScrollBar());
        this.makeScrollbarFocusTable(this.getHorizontalScrollBar());
    }

    private void makeScrollbarFocusTable(JScrollBar scrollBar) {
        scrollBar.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) MoveScroller.this.table.requestFocusInWindow();
                }
            }
        );
    }

    public void createListForMoves(List<ScrabbleGame.Move> moves) {
        this.currentMoves = moves;
        String[][] tableData = new String[this.currentMoves.size()][];

        for (int i = 0; i < this.currentMoves.size(); i++) {
            ScrabbleGame.Move move = this.currentMoves.get(i);
            tableData[i] = new String[] {
                String.valueOf(i + 1),
                String.valueOf(move.score()),
                move.playedWord()
            };
        }

        this.table.setModel(this.getModelFromData(tableData));
    }

    private TableModel getModelFromData(String[][] data) {
        return new DefaultTableModel(data, this.colNames) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

}