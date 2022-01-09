package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IPlayerTileRack;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerTileGrid extends JPanel {

    private final Consumer<RevertableAction> onAction;
    private final TileLabel[] labels = new TileLabel[7];
    private int tileSize = ScrabbleFrame.defaultTileSize;
    private final Dimension size = new Dimension(this.tileSize * 7, this.tileSize);
    private final IPlayerTileRack tileRack;
    private final Runnable onMovesInvalidated;
    private int cursor = 0;
    private boolean cursorJustSet = false;

    public PlayerTileGrid(Consumer<RevertableAction> onAction, IPlayerTileRack tileRack, Runnable onMovesInvalidated) {
        this.onAction = onAction;
        this.tileRack = tileRack;
        this.onMovesInvalidated = onMovesInvalidated;

        this.setLayout(new GridLayout(1, 7));
        this.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();

                    if (Character.isAlphabetic(c))
                        PlayerTileGrid.this.onCharPressed(Character.toLowerCase(c));
                    else if (c == IScrabbleGameState.wildCardMarker)
                        PlayerTileGrid.this.onCharPressed(IScrabbleGameState.wildCardTile);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        PlayerTileGrid.this.onCharPressed(ScrabblePanel.backspaceChar);
                    }
                }
            }
        );

        for (int i = 0; i < 7; i++) {
            final int finalI = i;
            TileLabel label = new TileLabel(
                new ImageIcon(this.getTileAt(i)),
                isLeft -> this.onTileClicked(finalI, isLeft)
            );
            labels[i] = label;

            this.add(label);
        }
    }

    public void clearPlayerTileGrid() {
        for (int i = 0; i < 7; i++) {
            if (this.tileRack.isTileInRackEmptyAt(i)) continue;

            this.tileRack.removeTileInRackAt(i);
            this.updateAndRepaintTileAt(i);
        }
    }

    public void playMove(ScrabbleGame.Move move) {
        List<Character> playedTiles = new String(
            move.playedTiles()
        ).chars().mapToObj(
            c -> (char) c
        ).collect(
            Collectors.toCollection(
                ArrayList::new
            )
        );

        for (int i = 0; i < 7 && !playedTiles.isEmpty(); i++) {
            if (this.tileRack.isTileInRackEmptyAt(i)) continue;

            char c = this.tileRack.getTileInRackAt(i);

            if (playedTiles.contains(c)) {
                playedTiles.remove((Character) c);
                this.tileRack.removeTileInRackAt(i);
                this.updateAndRepaintTileAt(i);
            }
        }
    }

    private void doBackspace() {
        if (!this.cursorJustSet)
            this.cursor--;
        else
            this.cursorJustSet = false;

        if (!this.tileRack.isTileInRackEmptyAt(this.cursor)) {
            this.tileRack.removeTileInRackAt(this.cursor);
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }
    }

    private void placeCharAtCursor(Character character) {
        this.cursorJustSet = false;

        if (this.tileRack.getTileInRackAt(this.cursor) != character) {
            this.tileRack.setTileInRackAt(this.cursor, character);
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }

        this.cursor++;
    }

    private void onCharPressed(Character character) {
        if (character == ScrabblePanel.backspaceChar) {
            if (this.cursorJustSet || this.cursor > 0) {
                this.doBackspace();
            }
        } else {
            if (this.cursor < 7) {
                this.placeCharAtCursor(character);
            }
        }
    }

    private void updateAndRepaintTileAtCursor() {
        this.updateAndRepaintTileAt(this.cursor);
    }

    private void updateAndRepaintTileAt(int i) {
        this.labels[i].getIcon().setImage(this.getTileAt(i));
        this.labels[i].repaint();
    }

    private void onTileClicked(int i, boolean isLeft) {
        if (isLeft) {
            this.cursor = i;
            this.cursorJustSet = true;

            this.requestFocusInWindow();
        }
    }

    private BufferedImage getTileAt(int i) {
        if (this.tileRack.isTileInRackEmptyAt(i))
            return TileProvider.INSTANCE.getDefaultBlankTile(this.tileSize);

        if (this.tileRack.isTileInRackWildcardAt(i))
            return TileProvider.INSTANCE.getWildcardTile(true, this.tileSize);

        return TileProvider.INSTANCE.getTile(
            this.tileRack.getTileInRackAt(i),
            false,
            true,
            false,
            this.tileSize
        );
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