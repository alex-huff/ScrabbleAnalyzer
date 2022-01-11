package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.LinkedList;
import java.util.List;

public class RevertableActionBuilder {

    List<RevertableAction> revertableActions = new LinkedList<>();

    public RevertableActionBuilder add(RevertableAction action) {
        if (!action.isNull()) this.revertableActions.add(action);

        return this;
    }

    public RevertableAction build() {
        return this.revertableActions.isEmpty() ?
            RevertableAction.nullRevertableAction :
            new CompoundRevertableAction(this.revertableActions);
    }

}
