package com.alexfh.scrabblesolver.state;

import com.alexfh.scrabblesolver.gui.action.RevertableAction;

import java.util.List;

public interface IPlayerTileRack {

    int getRackSize();

    int getNumTilesInRack();

    char getTileInRackAt(int i);

    RevertableAction setTileInRackAt(int i, char c);

    default RevertableAction setTileInRackWildcardAt(int i) {
        return this.setTileInRackAt(i, IScrabbleGameState.wildCardTile);
    }

    default RevertableAction removeTileInRackAt(int i) {
        return this.setTileInRackAt(i, IScrabbleGameState.emptyMarker);
    }

    default boolean isTileInRackWildcardAt(int i) {
        return this.getTileInRackAt(i) == IScrabbleGameState.wildCardTile;
    }

    default boolean isTileInRackEmptyAt(int i) {
        return this.getTileInRackAt(i) == IScrabbleGameState.emptyMarker;
    }

    List<Character> getTilesInRack();

    IPlayerTileRack copyRack();

}
