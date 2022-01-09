package com.alexfh.scrabblesolver.state;

import java.util.List;

public interface IPlayerTileRack {

    int getRackSize();

    int getNumTilesInRack();

    char getTileInRackAt(int i);

    void setTileInRackAt(int i, char c);

    default void setTileInRackWildcardAt(int i) {
        this.setTileInRackAt(i, IScrabbleGameState.wildCardTile);
    }

    default void removeTileInRackAt(int i) {
        this.setTileInRackAt(i, IScrabbleGameState.emptyMarker);
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
