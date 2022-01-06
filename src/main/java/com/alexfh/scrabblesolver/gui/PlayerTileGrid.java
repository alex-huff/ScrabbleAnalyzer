package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import java.util.List;

public class PlayerTileGrid extends JPanel {

    private final TileLabel[] labels = new TileLabel[7];
    private int tileSize = ScrabbleFrame.defaultTileSize;
    private final Dimension size = new Dimension(this.tileSize * 7, this.tileSize);
    private final List<Character> playerTiles;
    private final char[] placedTiles;
    private final Runnable onMovesInvalidated;
    private int cursor = 0;
    private boolean cursorJustSet = false;

    public PlayerTileGrid(List<Character> playerTiles, Runnable onMovesInvalidated) {
        this.playerTiles = playerTiles;
        this.placedTiles = new char[7];

        for (int i = 0; i < 7; i++) {
            this.placedTiles[i] = (i < this.playerTiles.size()) ? this.playerTiles.get(i) : IScrabbleBoard.emptyMarker;
        }

        this.onMovesInvalidated = onMovesInvalidated;

        this.setLayout(new GridLayout(1, 7));

        for (int i = 0; i < 7; i++) {
            int finalI = i;
            TileLabel label = new TileLabel(
                new ImageIcon(this.getTileAt(i)),
                isLeft -> this.onTileClicked(finalI, isLeft),
                this::onCharPressed
            );
            labels[i] = label;

            this.add(label);
        }
    }

    private void onCharPressed(Character character, boolean isShiftDown) {
        if (character == ScrabblePanel.backspaceChar) {
            if (this.cursorJustSet || this.cursor > 0) {
                if (!this.cursorJustSet) this.cursor--;

                char oldChar = this.placedTiles[this.cursor];

                if (oldChar != IScrabbleBoard.emptyMarker) {
                    this.placedTiles[this.cursor] = IScrabbleBoard.emptyMarker;

                    this.updateAndRepaintTileAtCursor();

                    this.playerTiles.remove((Character) oldChar);
                    this.onMovesInvalidated.run();
                }
            }
        } else {
            if (this.cursor < 7) {
                char oldChar = this.placedTiles[this.cursor];

                if (character == ScrabbleUtil.wildCardMarker) {
                    if (oldChar != ScrabbleUtil.wildCardTile) {
                        this.placedTiles[this.cursor] = ScrabbleUtil.wildCardTile;

                        this.updateAndRepaintTileAtCursor();

                        if (oldChar != IScrabbleBoard.emptyMarker) {
                            this.playerTiles.remove((Character) oldChar);
                        }

                        this.playerTiles.add(ScrabbleUtil.wildCardTile);
                        this.onMovesInvalidated.run();
                    }
                } else if (oldChar != character) {
                    this.placedTiles[this.cursor] = character;

                    this.updateAndRepaintTileAtCursor();

                    if (oldChar != IScrabbleBoard.emptyMarker) {
                        this.playerTiles.remove((Character) oldChar);
                    }

                    this.playerTiles.add(character);
                    this.onMovesInvalidated.run();
                }

                this.cursor++;
            }
        }

        this.cursorJustSet = false;
    }

    private void updateAndRepaintTileAtCursor() {
        this.labels[this.cursor].getIcon().setImage(this.getTileAt(this.cursor));
        this.labels[this.cursor].repaint();
    }

    private void onTileClicked(int i, boolean isLeft) {
        if (isLeft) {
            this.cursor = i;
            this.cursorJustSet = true;
        }
    }

    public char[] getPlayerTilesCopy() {
        return this.playerTiles.stream().map(String::valueOf).collect(Collectors.joining()).toCharArray();
    }

    private BufferedImage getTileAt(int i) {
        char c = this.placedTiles[i];

        if (c == ScrabbleUtil.wildCardTile) {
            return TileProvider.INSTANCE.getWildcardTile(true, this.tileSize);
        } else if (c == IScrabbleBoard.emptyMarker) {
            return TileProvider.INSTANCE.getDefaultBlankTile(this.tileSize);
        } else {
            return TileProvider.INSTANCE.getTile(c, false, true, false, this.tileSize);
        }
    }

    public void newSize(int newTileSize) {
        this.tileSize = newTileSize;

        this.size.setSize(this.tileSize * 7, this.tileSize);

        for (int i = 0; i < 7; i++) {
            this.labels[i].getIcon().setImage(this.getTileAt(i));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

}