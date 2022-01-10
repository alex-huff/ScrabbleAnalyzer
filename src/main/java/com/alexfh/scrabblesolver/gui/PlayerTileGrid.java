package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.CompoundRevertableAction;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.action.RevertableActionBuilder;
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
    private IPlayerTileRack tileRack;
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

    public void loadNewGame(IPlayerTileRack tileRack) {
        this.tileRack = tileRack;

        for (int i = 0; i < 7; i++) {
            this.updateAndRepaintTileAt(i);
        }
    }

    public RevertableAction clearPlayerTileGrid() {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        for (int i = 0; i < 7; i++) {
            final int finalI = i;

            actionBuilder.add(
                this.tileRack.removeTileInRackAt(finalI).then(
                    () -> this.updateAndRepaintTileAt(finalI)
                )
            );
        }

        return actionBuilder.build();
    }

    public RevertableAction playMove(ScrabbleGame.Move move) {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();
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
                final int finalI = i;

                playedTiles.remove((Character) c);
                actionBuilder.add(
                    this.tileRack.removeTileInRackAt(finalI).then(
                        () -> this.updateAndRepaintTileAt(finalI)
                    )
                );
            }
        }

        return actionBuilder.build();
    }

    private void doBackspace() {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        if (!this.cursorJustSet)
            actionBuilder.add(this.offsetCursor(-1));
        else
            actionBuilder.add(this.setJustSet(false));

        actionBuilder.add(
            this.tileRack.removeTileInRackAt(this.cursor).then(
                () -> {
                    this.updateAndRepaintTileAtCursor();
                    this.onMovesInvalidated.run();
                }
            )
        );
        this.onAction.accept(actionBuilder.build());
    }

    private void placeCharAtCursor(Character character) {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        actionBuilder.add(this.setJustSet(false));
        actionBuilder.add(
            this.tileRack.setTileInRackAt(this.cursor, character).then(
                () -> {
                    this.updateAndRepaintTileAtCursor();
                    this.onMovesInvalidated.run();
                }
            )
        );
        actionBuilder.add(this.offsetCursor(1));
        this.onAction.accept(actionBuilder.build());
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

    private void onTileClicked(int i, boolean isLeft) {
        if (isLeft) {
            this.onAction.accept(
                CompoundRevertableAction.compoundActionOf(
                    this.setCursor(i),
                    this.setJustSet(true)
                )
            );
            this.requestFocusInWindow();
        }
    }

    private RevertableAction setJustSet(final boolean justSet) {
        final boolean wasJustSet = this.cursorJustSet;

        if (wasJustSet == justSet) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.cursorJustSet = justSet,
            () -> this.cursorJustSet = wasJustSet
        );
    }

    private RevertableAction setCursor(final int newPos) {
        final int oldPos = this.cursor;

        return RevertableAction.of(
            () -> this.cursor = newPos,
            () -> this.cursor = oldPos
        );
    }

    private RevertableAction offsetCursor(final int offset) {
        return RevertableAction.of(
            () -> this.cursor += offset,
            () -> this.cursor -= offset
        );
    }

    private void updateAndRepaintTileAtCursor() {
        this.updateAndRepaintTileAt(this.cursor);
    }

    private void updateAndRepaintTileAt(int i) {
        this.labels[i].getIcon().setImage(this.getTileAt(i));
        this.labels[i].repaint();
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