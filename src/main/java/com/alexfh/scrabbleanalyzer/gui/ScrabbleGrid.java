package com.alexfh.scrabbleanalyzer.gui;

import com.alexfh.scrabbleanalyzer.ScrabbleGame;
import com.alexfh.scrabbleanalyzer.gui.action.RevertableAction;
import com.alexfh.scrabbleanalyzer.gui.action.RevertableActionBuilder;
import com.alexfh.scrabbleanalyzer.gui.tile.TileProvider;
import com.alexfh.scrabbleanalyzer.gui.tile.TileStyle;
import com.alexfh.scrabbleanalyzer.state.IScrabbleBoard;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.state.impl.ScrabbleBoardImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ScrabbleGrid extends JPanel {

    private final Consumer<RevertableAction> onAction;
    private final TileLabel[][] labels = new TileLabel[15][15];
    private int tileSize = ScrabbleAnalyzer.defaultTileSize;
    private IScrabbleBoard board;
    private final Runnable onMovesInvalidated;
    private final TileStyle tileStyle;
    private int cursorR = 0;
    private int cursorC = 0;
    private boolean cursorJustSet = false;
    private boolean wasLastMovementForwardVert = false;
    private boolean wasLastMovementForwardHori = false;
    private final char[][] playedWordPreviewChars = ScrabbleBoardImpl.getNewEmptyBoard(15, 15);
    private ScrabbleGame.Move previewedMove;

    public ScrabbleGrid(Consumer<RevertableAction> onAction, IScrabbleBoard board, Runnable onMovesInvalidated, TileStyle tileStyle) {
        this.onAction = onAction;
        this.board = board;
        this.onMovesInvalidated = onMovesInvalidated;
        this.tileStyle = tileStyle;

        this.setPreferredSize(new Dimension(this.tileSize * 15, this.tileSize * 15));
        this.setLayout(new GridLayout(15, 15));
        this.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();

                    if (Character.isAlphabetic(c))
                        ScrabbleGrid.this.onCharPressed(Character.toLowerCase(c), e.isShiftDown());
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        ScrabbleGrid.this.onCharPressed(ScrabblePanel.backspaceChar, e.isShiftDown());
                    }
                }
            }
        );

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                final int finalR = r;
                final int finalC = c;
                TileLabel label = new TileLabel(
                    new ImageIcon(this.getTileAt(r, c)),
                    isLeft -> this.onTileClicked(finalR, finalC, isLeft)
                );
                labels[r][c] = label;

                this.add(label);
            }
        }
    }

    public void loadNewGame(IScrabbleBoard board) {
        this.board = board;

        this.clearSelectedMove();
        this.repaintGrid();
    }

    public void repaintGrid() {
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                this.updateTileAt(r, c);
            }
        }

        this.repaint();
    }

    public RevertableAction clearGrid() {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                final int finalR = r;
                final int finalC = c;

                actionBuilder.add(
                    this.board.removeCharAt(finalR, finalC).then(
                        () -> this.updateTileAt(finalR, finalC)
                    )
                );
            }
        }

        return actionBuilder.build().then(this::repaint);
    }

    public void showMove(ScrabbleGame.Move move) {
        this.clearSelectedMove();

        this.previewedMove = move;
        ScrabbleGame.Offset offset = this.previewedMove.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        int startRow = this.previewedMove.row();
        int startCol = this.previewedMove.col();

        for (int i = 0; i < this.previewedMove.playedTiles().length; i++) {
            char placedChar = this.previewedMove.playedTiles()[i];
            int spotInWord = this.previewedMove.tileSpotsInWord()[i];
            int newRow = offset.newRow(startRow, spotInWord);
            int newCol = offset.newCol(startCol, spotInWord);
            char toPlace = placedChar == IScrabbleGameState.wildCardTile ?
                this.previewedMove.playedWord().charAt(spotInWord) :
                Character.toUpperCase(placedChar);
            this.playedWordPreviewChars[newRow][newCol] = toPlace;

            this.updateAndRepaintTileAt(newRow, newCol);
        }
    }

    public RevertableAction playMove(ScrabbleGame.Move move) {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();
        ScrabbleGame.Offset offset = move.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        int startRow = move.row();
        int startCol = move.col();

        for (int i = 0; i < move.playedTiles().length; i++) {
            char placedChar = move.playedTiles()[i];
            int spotInWord = move.tileSpotsInWord()[i];
            final int newRow = offset.newRow(startRow, spotInWord);
            final int newCol = offset.newCol(startCol, spotInWord);
            RevertableAction placeTileAction = placedChar == IScrabbleGameState.wildCardTile ?
                RevertableAction.compoundActionOf(
                    this.board.setCharAt(newRow, newCol, move.playedWord().charAt(spotInWord)),
                    this.board.setWildcardAt(newRow, newCol, true)
                ) :
                this.board.setCharAt(newRow, newCol, placedChar);

            actionBuilder.add(
                placeTileAction.then(
                    () -> this.updateAndRepaintTileAt(newRow, newCol)
                )
            );
        }

        return actionBuilder.build();
    }

    public void clearSelectedMove() {
        if (this.previewedMove == null) return;

        ScrabbleGame.Offset offset = this.previewedMove.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        int startRow = this.previewedMove.row();
        int startCol = this.previewedMove.col();

        for (int i = 0; i < this.previewedMove.playedTiles().length; i++) {
            int spotInWord = this.previewedMove.tileSpotsInWord()[i];
            int newRow = offset.newRow(startRow, spotInWord);
            int newCol = offset.newCol(startCol, spotInWord);
            this.playedWordPreviewChars[newRow][newCol] = IScrabbleGameState.emptyMarker;

            this.updateAndRepaintTileAt(newRow, newCol);
        }

        this.previewedMove = null;
    }

    private void updateAndRepaintTileAtCursor() {
        this.updateAndRepaintTileAt(this.cursorR, this.cursorC);
    }

    private void updateAndRepaintTileAt(int r, int c) {
        this.updateTileAt(r, c);
        this.labels[r][c].repaint();
    }

    private void updateTileAt(int r, int c) {
        this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
    }

    private void doBackspace(boolean isShiftDown) {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        if (!this.cursorJustSet) {
            if (isShiftDown) {
                if (this.wasLastMovementForwardHori) {
                    actionBuilder.add(this.offsetColCursor(-1));
                } else {
                    actionBuilder.add(this.offsetRowCursor(-1));
                }
            } else {
                if (this.wasLastMovementForwardVert) {
                    actionBuilder.add(this.offsetRowCursor(-1));
                } else {
                    actionBuilder.add(this.offsetColCursor(-1));
                }
            }

            actionBuilder.add(this.setWasLastMovementForwardVert(false));
            actionBuilder.add(this.setWasLastMovementForwardHori(false));
        } else {
            actionBuilder.add(this.setJustSet(false));
        }

        actionBuilder.add(
            this.board.removeCharAt(this.cursorR, this.cursorC).then(
                () -> {
                    this.updateAndRepaintTileAtCursor();
                    this.onMovesInvalidated.run();
                }
            )
        );
        this.onAction.accept(actionBuilder.build().withDescription("Backspace in scrabble board"));
    }

    private void placeCharAtCursor(boolean isShiftDown, Character character) {
        RevertableActionBuilder actionBuilder = new RevertableActionBuilder();

        actionBuilder.add(this.setJustSet(false));

        if (isShiftDown) {
            if (this.wasLastMovementForwardHori) {
                actionBuilder.add(this.offsetRowCursor(1));
                actionBuilder.add(this.offsetColCursor(-1));
            }

            actionBuilder.add(this.setWasLastMovementForwardVert(true));
            actionBuilder.add(this.setWasLastMovementForwardHori(false));
        } else {
            if (this.wasLastMovementForwardVert) {
                actionBuilder.add(this.offsetRowCursor(-1));
                actionBuilder.add(this.offsetColCursor(1));
            }

            actionBuilder.add(this.setWasLastMovementForwardVert(false));
            actionBuilder.add(this.setWasLastMovementForwardHori(true));
        }

        actionBuilder.add(
            RevertableAction.compoundActionOf(
                this.board.setCharAt(this.cursorR, this.cursorC, character),
                this.board.setWildcardAt(this.cursorR, this.cursorC, false)
            ).then(
                () -> {
                    this.updateAndRepaintTileAtCursor();
                    this.onMovesInvalidated.run();
                }
            )
        );

        if (isShiftDown)
            actionBuilder.add(this.offsetRowCursor(1));
        else
            actionBuilder.add(this.offsetColCursor(1));

        this.onAction.accept(actionBuilder.build().withDescription("Type character on scrabble board"));
    }

    private void onCharPressed(Character character, boolean isShiftDown) {
        if (character == ScrabblePanel.backspaceChar) {
            if (this.cursorJustSet || (isShiftDown && this.cursorR > 0) || (!isShiftDown && this.cursorC > 0)) {
                this.doBackspace(isShiftDown);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            if (
                ((isShiftDown && this.cursorR < 15) || (!isShiftDown && this.cursorC < 15)) &&
                !(isShiftDown && this.wasLastMovementForwardHori && this.cursorR == 14) &&
                !(!isShiftDown && this.wasLastMovementForwardVert && this.cursorC == 14)
            ) {
                this.placeCharAtCursor(isShiftDown, character);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private void onTileClicked(final int r, final int c, boolean isLeft) {
        if (isLeft) {
            this.onAction.accept(
                RevertableAction.compoundActionOf(
                    this.setCursor(r, c),
                    this.setJustSet(true),
                    this.setWasLastMovementForwardVert(false),
                    this.setWasLastMovementForwardHori(false)
                ).withDescription("Set cursor in scrabble board at: " + (r + 1) + " " + (c + 1))
            );
            this.requestFocusInWindow();
        } else {
            if (!this.board.isEmptyAt(r, c)) {
                this.onAction.accept(
                    this.board.setWildcardAt(r, c, !this.board.isWildcardAt(r, c)).then(
                        () -> {
                            this.updateAndRepaintTileAt(r, c);
                            this.onMovesInvalidated.run();
                        }
                    ).withDescription("Toggle tile point value at: " + (r + 1) + " " + (c + 1))
                );
            }
        }
    }

    private RevertableAction setWasLastMovementForwardVert(final boolean lastMovementFV) {
        final boolean wasLastMovementFV = this.wasLastMovementForwardVert;

        if (wasLastMovementFV == lastMovementFV) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.wasLastMovementForwardVert = lastMovementFV,
            () -> this.wasLastMovementForwardVert = wasLastMovementFV
        );
    }

    private RevertableAction setWasLastMovementForwardHori(final boolean lastMovementFH) {
        final boolean wasLastMovementFH = this.wasLastMovementForwardHori;

        if (wasLastMovementFH == lastMovementFH) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.wasLastMovementForwardHori = lastMovementFH,
            () -> this.wasLastMovementForwardHori = wasLastMovementFH
        );
    }

    private RevertableAction setJustSet(final boolean justSet) {
        final boolean wasJustSet = this.cursorJustSet;

        if (wasJustSet == justSet) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.cursorJustSet = justSet,
            () -> this.cursorJustSet = wasJustSet
        );
    }

    private RevertableAction setCursor(final int newRowPos, final int newColPos) {
        final int oldRowPos = this.cursorR;
        final int oldColPos = this.cursorC;

        if (oldRowPos == newRowPos && oldColPos == newColPos) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> {
                this.cursorR = newRowPos;
                this.cursorC = newColPos;
            },
            () -> {
                this.cursorR = oldRowPos;
                this.cursorC = oldColPos;
            }
        );
    }

    private RevertableAction offsetRowCursor(final int offset) {
        if (offset == 0) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.cursorR += offset,
            () -> this.cursorR -= offset
        );
    }

    private RevertableAction offsetColCursor(final int offset) {
        if (offset == 0) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> this.cursorC += offset,
            () -> this.cursorC -= offset
        );
    }

    private BufferedImage getTileAt(int r, int c) {
        if (this.board.isEmptyAt(r, c)) {
            char previewChar = this.playedWordPreviewChars[r][c];

            if (previewChar != IScrabbleGameState.emptyMarker) {
                char previewCharLower;
                boolean isWild;

                if (Character.isUpperCase(previewChar)) {
                    previewCharLower = Character.toLowerCase(previewChar);
                    isWild = false;
                } else {
                    previewCharLower = previewChar;
                    isWild = true;
                }

                return TileProvider.INSTANCE.getTile(
                    previewCharLower,
                    isWild,
                    this.tileStyle.getIso(),
                    true,
                    this.tileSize
                );
            }

            return TileProvider.INSTANCE.getBlankTile(
                this.board.getLetterMultiplierAt(r, c),
                this.board.getWordMultiplierAt(r, c),
                this.tileSize
            );
        }

        return TileProvider.INSTANCE.getTile(
            this.board.getCharAt(r, c),
            this.board.isWildcardAt(r, c),
            this.tileStyle.getIso(),
            false,
            this.tileSize
        );
    }

    public void newSize(int newTileSize) {
        this.tileSize = newTileSize;

        this.setPreferredSize(new Dimension(this.tileSize * 15, this.tileSize * 15));

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
            }
        }
    }

}