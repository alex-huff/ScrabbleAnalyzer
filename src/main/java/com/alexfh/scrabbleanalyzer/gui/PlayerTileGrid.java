package com.alexfh.scrabbleanalyzer.gui;

import com.alexfh.scrabbleanalyzer.ScrabbleGame;
import com.alexfh.scrabbleanalyzer.gui.action.RevertibleAction;
import com.alexfh.scrabbleanalyzer.gui.action.RevertibleActionBuilder;
import com.alexfh.scrabbleanalyzer.gui.tile.TileProvider;
import com.alexfh.scrabbleanalyzer.state.IPlayerTileRack;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public
class PlayerTileGrid extends JPanel
{

    private final Consumer<RevertibleAction> onAction;
    private final TileLabel[]                labels        = new TileLabel[7];
    private       int                        tileSize      = ScrabbleAnalyzer.defaultTileSize;
    private       IPlayerTileRack            tileRack;
    private final Runnable                   onMovesInvalidated;
    private       int                        cursor        = 0;
    private       boolean                    cursorJustSet = false;

    public
    PlayerTileGrid(Consumer<RevertibleAction> onAction, IPlayerTileRack tileRack, Runnable onMovesInvalidated)
    {
        this.onAction           = onAction;
        this.tileRack           = tileRack;
        this.onMovesInvalidated = onMovesInvalidated;

        this.setPreferredSize(new Dimension(this.tileSize * 7, this.tileSize));
        this.setLayout(new GridLayout(1, 7));
        this.addKeyListener(new KeyAdapter()
        {
            @Override
            public
            void keyTyped(KeyEvent e)
            {
                char c = e.getKeyChar();

                if (Character.isAlphabetic(c))
                {
                    PlayerTileGrid.this.onCharPressed(Character.toLowerCase(c));
                }
                else if (c == IScrabbleGameState.wildCardMarker)
                {
                    PlayerTileGrid.this.onCharPressed(IScrabbleGameState.wildCardTile);
                }
            }

            @Override
            public
            void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
                {
                    PlayerTileGrid.this.onCharPressed(ScrabblePanel.backspaceChar);
                }
            }
        });

        for (int i = 0; i < 7; i++)
        {
            final int finalI = i;
            TileLabel label = new TileLabel(new ImageIcon(this.getTileAt(i)),
                isLeft -> this.onTileClicked(finalI, isLeft));
            labels[i] = label;

            this.add(label);
        }
    }

    public
    void loadNewGame(IPlayerTileRack tileRack)
    {
        this.tileRack = tileRack;

        this.repaintGrid();
    }

    public
    void repaintGrid()
    {
        for (int i = 0; i < 7; i++)
        {
            this.updateTileAt(i);
        }

        this.repaint();
    }

    public
    RevertibleAction clearPlayerTileGrid()
    {
        RevertibleActionBuilder actionBuilder = new RevertibleActionBuilder();

        for (int i = 0; i < 7; i++)
        {
            final int finalI = i;

            actionBuilder.add(this.tileRack.removeTileInRackAt(finalI).then(() -> this.updateTileAt(finalI)));
        }

        return actionBuilder.build().then(this::repaint);
    }

    public
    RevertibleAction playMove(ScrabbleGame.Move move)
    {
        RevertibleActionBuilder actionBuilder = new RevertibleActionBuilder();
        List<Character> playedTiles = new String(move.playedTiles()).chars().mapToObj(c -> (char) c)
            .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < 7 && !playedTiles.isEmpty(); i++)
        {
            if (this.tileRack.isTileInRackEmptyAt(i))
            {
                continue;
            }

            char c = this.tileRack.getTileInRackAt(i);

            if (playedTiles.contains(c))
            {
                final int finalI = i;

                playedTiles.remove((Character) c);
                actionBuilder.add(
                    this.tileRack.removeTileInRackAt(finalI).then(() -> this.updateAndRepaintTileAt(finalI)));
            }
        }

        return actionBuilder.build();
    }

    private
    void doBackspace()
    {
        RevertibleActionBuilder actionBuilder = new RevertibleActionBuilder();

        if (!this.cursorJustSet)
        {
            actionBuilder.add(this.offsetCursor(-1));
        }
        else
        {
            actionBuilder.add(this.setJustSet(false));
        }

        actionBuilder.add(this.tileRack.removeTileInRackAt(this.cursor).then(() ->
        {
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }));
        this.onAction.accept(actionBuilder.build().withDescription("Backspace in tile rack"));
    }

    private
    void placeCharAtCursor(Character character)
    {
        RevertibleActionBuilder actionBuilder = new RevertibleActionBuilder();

        actionBuilder.add(this.setJustSet(false));
        actionBuilder.add(this.tileRack.setTileInRackAt(this.cursor, character).then(() ->
        {
            this.updateAndRepaintTileAtCursor();
            this.onMovesInvalidated.run();
        }));
        actionBuilder.add(this.offsetCursor(1));
        this.onAction.accept(actionBuilder.build().withDescription("Type character in tile rack"));
    }

    private
    void onCharPressed(Character character)
    {
        if (character == ScrabblePanel.backspaceChar)
        {
            if (this.cursorJustSet || this.cursor > 0)
            {
                this.doBackspace();
            }
            else
            {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        else
        {
            if (this.cursor < 7)
            {
                this.placeCharAtCursor(character);
            }
            else
            {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private
    void onTileClicked(int i, boolean isLeft)
    {
        if (isLeft)
        {
            this.onAction.accept(RevertibleAction.compoundActionOf(this.setCursor(i), this.setJustSet(true))
                .withDescription("Set cursor in tile rack at: " + (i + 1)));
            this.requestFocusInWindow();
        }
    }

    private
    RevertibleAction setJustSet(final boolean justSet)
    {
        final boolean wasJustSet = this.cursorJustSet;

        if (wasJustSet == justSet)
        {
            return RevertibleAction.NULL_REVERTIBLE_ACTION;
        }

        return RevertibleAction.of(() -> this.cursorJustSet = justSet, () -> this.cursorJustSet = wasJustSet);
    }

    private
    RevertibleAction setCursor(final int newPos)
    {
        final int oldPos = this.cursor;

        return RevertibleAction.of(() -> this.cursor = newPos, () -> this.cursor = oldPos);
    }

    private
    RevertibleAction offsetCursor(final int offset)
    {
        return RevertibleAction.of(() -> this.cursor += offset, () -> this.cursor -= offset);
    }

    private
    void updateAndRepaintTileAtCursor()
    {
        this.updateAndRepaintTileAt(this.cursor);
    }

    private
    void updateAndRepaintTileAt(int i)
    {
        this.updateTileAt(i);
        this.labels[i].repaint();
    }

    private
    void updateTileAt(int i)
    {
        this.labels[i].getIcon().setImage(this.getTileAt(i));
    }

    private
    BufferedImage getTileAt(int i)
    {
        if (this.tileRack.isTileInRackEmptyAt(i))
        {
            return TileProvider.INSTANCE.getDefaultBlankTile(this.tileSize);
        }

        if (this.tileRack.isTileInRackWildcardAt(i))
        {
            return TileProvider.INSTANCE.getWildcardTile(this.tileSize);
        }

        return TileProvider.INSTANCE.getTile(this.tileRack.getTileInRackAt(i), false, false, this.tileSize);
    }

    public
    void newSize(int newTileSize)
    {
        this.tileSize = newTileSize;

        this.setPreferredSize(new Dimension(this.tileSize * 7, this.tileSize));

        for (int i = 0; i < 7; i++)
        {
            this.labels[i].getIcon().setImage(this.getTileAt(i));
        }
    }

}