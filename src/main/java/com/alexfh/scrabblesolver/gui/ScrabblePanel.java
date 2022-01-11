package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.Main;
import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.CompoundRevertableAction;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.List;
import java.util.function.Consumer;

public class ScrabblePanel extends JPanel {

    public static final char backspaceChar = '}';

    private final Consumer<RevertableAction> onAction;
    private IScrabbleGameState gameState;
    private final ScrabbleGrid grid;
    private final PlayerTileGrid playerTileGrid;
    private final MoveScroller moveScroller;
    private boolean isVerticalLayout = false;
    private int currentTileSize = ScrabbleAnalyzer.defaultTileSize;
    private int updateNum = 0;
    private int lastUpdateReceived = 0;
    private Future<?> pendingUpdate;
    private final ScrabbleLayout layout;

    public ScrabblePanel(Consumer<RevertableAction> onAction, IScrabbleGameState gameState) {
        this.onAction = onAction;
        this.gameState = gameState;
        this.grid = new ScrabbleGrid(this.onAction, this.gameState, this::boardInvalidated);
        this.playerTileGrid = new PlayerTileGrid(this.onAction, this.gameState, this::playerTilesInvalidated);
        this.moveScroller = new MoveScroller(this::showMove, this::playMove);
        this.layout = new ScrabbleLayout();

        this.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) ScrabblePanel.this.requestFocusInWindow();
                }
            }
        );
        this.setLayout(this.layout);
        this.initializeLayout();
        this.updateMoves();
    }

    public void loadNewGame(IScrabbleGameState gameState) {
        this.gameState = gameState;

        this.grid.loadNewGame(this.gameState);
        this.playerTileGrid.loadNewGame(this.gameState);
        this.updateMoves();
    }

    public void clearBoard() {
        this.onAction.accept(
            CompoundRevertableAction.compoundActionOf(
                this.grid.clearGrid(),
                this.playerTileGrid.clearPlayerTileGrid()
            ).then(this::updateMoves)
        );
    }

    private void playMove(ScrabbleGame.Move move) {
        if (this.updateNum == this.lastUpdateReceived) {
            this.onAction.accept(
                CompoundRevertableAction.compoundActionOf(
                    this.grid.playMove(move),
                    this.playerTileGrid.playMove(move)
                ).then(this::updateMoves)
            );
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
        this.grid.clearSelectedMove();

        if (this.pendingUpdate != null) this.pendingUpdate.cancel(true);

        this.updateNum++;
        final IScrabbleGameState gameStateCopy = this.gameState.copyScrabbleGame();
        final int updateNumCopy = this.updateNum;
        this.pendingUpdate = ScrabbleGame.threadPool.submit(
            () -> {
                try {
                    this.getMoves(gameStateCopy, updateNumCopy);
                } catch (InterruptedException ignored) { }
            }
        );
    }

    private void getMoves(IScrabbleGameState gameStateCopy, int updateNumCopy) throws InterruptedException {
        ScrabbleGame game = new ScrabbleGame(
            LetterScoreMapImpl.defaultScoreMap,
            Main.dictionary,
            gameStateCopy
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