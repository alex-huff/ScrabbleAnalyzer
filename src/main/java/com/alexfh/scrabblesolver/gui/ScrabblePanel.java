package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.Main;
import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.List;

public class ScrabblePanel extends JPanel {

    public static final char backspaceChar = '}';

    private final ScrabbleGrid grid;
    private final PlayerTileGrid playerTileGrid;
    private final MoveScroller moveScroller;
    private boolean isVerticalLayout = false;
    private int currentTileSize = ScrabbleFrame.defaultTileSize;
    private int updateNum = 0;
    private int lastUpdateReceived = 0;
    private Future<?> pendingUpdate;
    private final ScrabbleLayout layout;

    public ScrabblePanel(IScrabbleBoard board, List<Character> playerTiles) {
        this.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) ScrabblePanel.this.requestFocusInWindow();
                }
            }
        );

        this.grid = new ScrabbleGrid(board, this::boardInvalidated);
        this.playerTileGrid = new PlayerTileGrid(playerTiles, this::playerTilesInvalidated);
        this.moveScroller = new MoveScroller(this::showMove, this.grid::clearSelectedMove, this::playMove);
        this.layout = new ScrabbleLayout();

        this.setLayout(this.layout);
        this.initializeLayout();
        this.updateMoves();
    }

    public void clearBoard() {
        this.grid.clearGrid();
        this.playerTileGrid.clearPlayerTileGrid();
        this.updateMoves();
    }

    private void playMove(ScrabbleGame.Move move) {
        if (this.updateNum == this.lastUpdateReceived) {
            this.grid.playMove(move);
            this.playerTileGrid.playMove(move);
            this.updateMoves();
        }
    }

    private void showMove(ScrabbleGame.Move move) {
        if (this.updateNum == this.lastUpdateReceived) this.grid.showMove(move);
    }

    private void boardInvalidated() {
        this.updateMoves();
    }

    private void playerTilesInvalidated() {
        this.updateMoves();
    }

    private void updateMoves() {
        if (this.pendingUpdate != null) this.pendingUpdate.cancel(true);

        IScrabbleBoard boardCopy = this.grid.getBoardCopy();
        char[] playerTilesCopy = this.playerTileGrid.getPlayerTilesCopy();
        this.updateNum++;
        int updateNumCopy = this.updateNum;
        this.pendingUpdate = ScrabbleGame.threadPool.submit(
            () -> {
                try {
                    this.getMoves(boardCopy, playerTilesCopy, updateNumCopy);
                } catch (InterruptedException ignored) { }
            }
        );
    }

    private void getMoves(IScrabbleBoard boardCopy, char[] playerTilesCopy, int updateNumCopy) throws InterruptedException {
        ScrabbleGame game = new ScrabbleGame(
            LetterScoreMapImpl.defaultScoreMap,
            Main.dictionary,
            boardCopy,
            playerTilesCopy,
            7
        );
        List<ScrabbleGame.Move> moves = game.findMoves();

        Collections.sort(moves);
        ScrabbleUtil.checkInterrupted();
        SwingUtilities.invokeLater(
            () -> {
                if (this.lastUpdateReceived > updateNumCopy) return;

                this.lastUpdateReceived = updateNumCopy;

                this.moveScroller.createListForMoves(moves);
            }
        );
    }

    public void onResize(int width, int height) {
        float aspectRatio = width * 1.0F / height;
        int tileSize;
        float layoutThreshold = 23 / 19F; // exact aspect ratio where vert/hori layouts equal in tile size

        if ((!this.isVerticalLayout && aspectRatio < layoutThreshold) ||
            (this.isVerticalLayout && aspectRatio > layoutThreshold)
        ) {
            this.isVerticalLayout = !this.isVerticalLayout;
            this.initializeLayout();
        }

        if (this.isVerticalLayout) {
            if (aspectRatio > 15 / 19F) {
                tileSize = height / 19;
            } else {
                tileSize = width / 15;
            }
        } else {
            if (aspectRatio > 23 / 15F) {
                tileSize = height / 15;
            } else {
                tileSize = width / 23;
            }
        }

        this.moveScroller.newSize(
            this.isVerticalLayout ? width : (width - tileSize * 15),
            this.isVerticalLayout ? (height - tileSize * 16) : (height - tileSize)
        );

        if (tileSize != this.currentTileSize) {
            TileProvider.INSTANCE.clearCache();
            this.grid.newSize(tileSize);
            this.playerTileGrid.newSize(tileSize);
            System.gc();
        }

        this.currentTileSize = tileSize;
    }

    private void initializeLayout() {
        boolean wasInitialized = this.getComponents().length > 0;

        if (wasInitialized) {
            this.layout.removeLayoutComponent(this.grid);
            this.layout.removeLayoutComponent(this.playerTileGrid);
            this.layout.removeLayoutComponent(this.moveScroller);
        }

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = this.isVerticalLayout ? 1 : 2;

        if (wasInitialized)
            this.layout.addLayoutComponent(this.grid, c);
        else
            this.add(this.grid, c);

        c = new GridBagConstraints();
        c.gridx = this.isVerticalLayout ? 0 : 1;
        c.gridy = this.isVerticalLayout ? 1 : 0;

        if (wasInitialized)
            this.layout.addLayoutComponent(this.moveScroller, c);
        else
            this.add(this.moveScroller, c);

        c = new GridBagConstraints();
        c.gridx = this.isVerticalLayout ? 0 : 1;
        c.gridy = this.isVerticalLayout ? 2 : 1;

        if (wasInitialized)
            this.layout.addLayoutComponent(this.playerTileGrid, c);
        else
            this.add(this.playerTileGrid, c);
    }

}