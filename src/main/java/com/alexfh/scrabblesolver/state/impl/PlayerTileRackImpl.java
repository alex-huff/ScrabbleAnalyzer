package com.alexfh.scrabblesolver.state.impl;

import com.alexfh.scrabblesolver.gui.action.CompoundRevertableAction;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.state.IPlayerTileRack;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlayerTileRackImpl implements IPlayerTileRack {

    public static IPlayerTileRack defaultBlankRack() {
        return new PlayerTileRackImpl(new char[] { '.', '.', '.', '.', '.', '.', '.' });
    }

    public static IPlayerTileRack fromFile(File rackFile) throws IOException {
        String playerTiles = Files.readString(rackFile.toPath(), StandardCharsets.UTF_8)
            .strip()
            .toLowerCase()
            .replaceAll(String.valueOf(IScrabbleGameState.wildCardMarker), String.valueOf(IScrabbleGameState.wildCardTile));

        if (!ScrabbleUtil.isValidTiles(playerTiles)) throw new IllegalStateException("Invalid player tiles");

        return new PlayerTileRackImpl(playerTiles.toCharArray());
    }

    private final int rackSize;
    private final char[] placedTiles;
    private final List<Character> playerTiles;

    public PlayerTileRackImpl(char[] placedTiles) {
        this.rackSize = placedTiles.length;
        this.placedTiles = placedTiles;
        this.playerTiles = new ArrayList<>(this.rackSize);

        for (int i = 0; i < this.rackSize; i++) {
            char currentChar = this.placedTiles[i];

            if (currentChar != IScrabbleGameState.emptyMarker) {
                this.playerTiles.add(currentChar);
            }
        }
    }

    private PlayerTileRackImpl(char[] placedTiles, List<Character> playerTiles) {
        this.rackSize = placedTiles.length;
        this.placedTiles = placedTiles;
        this.playerTiles = playerTiles;
    }

    @Override
    public int getRackSize() {
        return this.rackSize;
    }

    @Override
    public int getNumTilesInRack() {
        return this.playerTiles.size();
    }

    @Override
    public char getTileInRackAt(int i) {
        return this.placedTiles[i];
    }

    @Override
    public RevertableAction setTileInRackAt(final int i, final char c) {
        List<RevertableAction> revertableActions = new LinkedList<>();
        final char oldChar = this.placedTiles[i];

        if (!this.isTileInRackEmptyAt(i)) {
            if (oldChar != c) {
                revertableActions.add(RevertableAction.removeElementFromListByEquality(this.playerTiles, oldChar));
            }
        }

        if (c != IScrabbleGameState.emptyMarker) {
            revertableActions.add(RevertableAction.addToList(this.playerTiles, c));
        }

        this.placedTiles[i] = c; // redo
        this.placedTiles[i] = oldChar; // undo

        return CompoundRevertableAction.compoundActionOf(revertableActions);
    }

    @Override
    public List<Character> getTilesInRack() {
        return this.playerTiles;
    }

    @Override
    public IPlayerTileRack copyRack() {
        return new PlayerTileRackImpl(
            this.placedTiles.clone(),
            new ArrayList<>(this.playerTiles)
        );
    }

}
