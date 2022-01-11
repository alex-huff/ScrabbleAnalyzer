package com.alexfh.scrabblesolver.state;

import com.alexfh.scrabblesolver.gui.action.RevertableAction;

import java.util.List;
import java.util.stream.IntStream;

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

    default boolean isEqualTo(IPlayerTileRack otherRack) {
        return
            this.getRackSize() == otherRack.getRackSize() &&
            IntStream.range(
                0,
                this.getRackSize()
            ).allMatch(
                i -> this.getTileInRackAt(i) == otherRack.getTileInRackAt(i)
            ) &&
            this.getTilesInRack().equals(otherRack.getTilesInRack());
    }

}
