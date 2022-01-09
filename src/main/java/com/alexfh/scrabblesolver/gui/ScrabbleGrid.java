package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.state.impl.ScrabbleBoardImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ScrabbleGrid extends JPanel {

    private final Consumer<RevertableAction> onAction;
    private final TileLabel[][] labels = new TileLabel[15][15];
    private int tileSize = ScrabbleFrame.defaultTileSize;
    private final Dimension size = new Dimension(this.tileSize * 15, this.tileSize * 15);
    private final IScrabbleBoard board;
    private final Runnable onMovesInvalidated;
    private int cursorR = 0;
    private int cursorC = 0;
    private boolean cursorJustSet = false;
    private boolean wasLastMovementForwardVert = false;
    private boolean wasLastMovementForwardHori = false;
    private final char[][] playedWordPreviewChars = ScrabbleBoardImpl.getNewEmptyBoard(15, 15);
    private ScrabbleGame.Move previewedMove;

    public ScrabbleGrid(Consumer<RevertableAction> onAction, IScrabbleBoard board, Runnable onMovesInvalidated) {
        this.onAction = onAction;
        this.board = board;
        this.onMovesInvalidated = onMovesInvalidated;

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

    public void clearGrid() {
        this.clearSelectedMove();

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (this.board.isEmptyAt(r, c)) continue;

                this.board.removeCharAt(r, c);
                this.updateAndRepaintTileAt(r, c);
            }
        }
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
            char toPlace;

            if (placedChar == IScrabbleGameState.wildCardTile) {
                toPlace = this.previewedMove.playedWord().charAt(spotInWord);
            } else {
                toPlace = Character.toUpperCase(placedChar);
            }

            this.playedWordPreviewChars[newRow][newCol] = toPlace;

            this.updateAndRepaintTileAt(newRow, newCol);
        }
    }

    public void playMove(ScrabbleGame.Move move) {
        ScrabbleGame.Offset offset = move.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
        int startRow = move.row();
        int startCol = move.col();

        for (int i = 0; i < move.playedTiles().length; i++) {
            char placedChar = move.playedTiles()[i];
            int spotInWord = move.tileSpotsInWord()[i];
            int newRow = offset.newRow(startRow, spotInWord);
            int newCol = offset.newCol(startCol, spotInWord);

            if (placedChar == IScrabbleGameState.wildCardTile) {
                this.board.setCharAt(newRow, newCol, move.playedWord().charAt(spotInWord));
                this.board.setWildcardAt(newRow, newCol, true);
            } else {
                this.board.setCharAt(newRow, newCol, placedChar);
            }

            this.updateAndRepaintTileAt(newRow, newCol);
        }
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
        this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
        this.labels[r][c].repaint();
    }

    private void doBackspace(boolean isShiftDown) {
        if (!this.cursorJustSet) {
            if (isShiftDown) {
                if (this.wasLastMovementForwardHori) {
                    this.cursorC--;
                } else {
                    this.cursorR--;
                }
            } else {
                if (this.wasLastMovementForwardVert) {
                    this.cursorR--;
                } else {
                    this.cursorC--;
                }
            }

            this.wasLastMovementForwardVert = false;
            this.wasLastMovementForwardHori = false;
        } else {
            this.cursorJustSet = false;
        }

        if (!this.board.isEmptyAt(this.cursorR, this.cursorC)) {
            this.board.removeCharAt(this.cursorR, this.cursorC);
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }
    }

    private void placeCharAtCursor(boolean isShiftDown, Character character) {
        this.cursorJustSet = false;

        if (isShiftDown) {
            if (this.wasLastMovementForwardHori) {
                if (this.cursorR == 14) return;

                this.cursorR++;
                this.cursorC--;
            }

            this.wasLastMovementForwardVert = true;
            this.wasLastMovementForwardHori = false;
        } else {
            if (this.wasLastMovementForwardVert) {
                if (this.cursorC == 14) return;

                this.cursorR--;
                this.cursorC++;
            }

            this.wasLastMovementForwardHori = true;
            this.wasLastMovementForwardVert = false;
        }

        if (
            !(
                !this.board.isEmptyAt(this.cursorR, this.cursorC) &&
                    (this.board.getCharAt(this.cursorR, this.cursorC) == character) &&
                    (!this.board.isWildcardAt(this.cursorR, this.cursorC))
            )
        ) {
            this.board.setCharAt(this.cursorR, this.cursorC, character);
            this.board.setWildcardAt(this.cursorR, this.cursorC, false);
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }

        if (isShiftDown)
            this.cursorR++;
        else
            this.cursorC++;
    }

    private void onCharPressed(Character character, boolean isShiftDown) {
        if (character == ScrabblePanel.backspaceChar) {
            if (this.cursorJustSet || (isShiftDown && this.cursorR > 0) || (!isShiftDown && this.cursorC > 0)) {
                this.doBackspace(isShiftDown);
            }
        } else {
            if ((isShiftDown && this.cursorR < 15) || (!isShiftDown && this.cursorC < 15)) {
                this.placeCharAtCursor(isShiftDown, character);
            }
        }
    }

    private void onTileClicked(int r, int c, boolean isLeft) {
        if (isLeft) {
            this.cursorR = r;
            this.cursorC = c;
            this.cursorJustSet = true;
            this.wasLastMovementForwardVert = false;
            this.wasLastMovementForwardHori = false;
            this.requestFocusInWindow();
        } else {
            if (!this.board.isEmptyAt(r, c)) {
                this.board.setWildcardAt(r, c, !this.board.isWildcardAt(r, c));
                this.updateAndRepaintTileAt(r, c);
                this.onMovesInvalidated.run();
            }
        }
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
                    true,
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
            true,
            false,
            this.tileSize
        );
    }

    public void newSize(int newTileSize) {
        this.tileSize = newTileSize;

        this.size.setSize(this.tileSize * 15, this.tileSize * 15);

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return this.size;
    }

}