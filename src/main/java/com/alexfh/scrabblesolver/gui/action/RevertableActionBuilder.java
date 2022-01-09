package com.alexfh.scrabblesolver.gui.action;

import java.util.LinkedList;
import java.util.List;

public class RevertableActionBuilder {

    List<RevertableAction> revertableActions = new LinkedList<>();

    public RevertableActionBuilder add(RevertableAction revertableAction) {
        this.revertableActions.add(revertableAction);

        return this;
    }

    public RevertableAction build() {
        return new CompoundRevertableAction(this.revertableActions);
    }

}
