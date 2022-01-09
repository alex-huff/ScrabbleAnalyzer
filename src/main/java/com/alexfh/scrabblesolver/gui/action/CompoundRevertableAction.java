package com.alexfh.scrabblesolver.gui.action;

import java.util.List;
import java.util.ListIterator;

public class CompoundRevertableAction implements RevertableAction {

    public static CompoundRevertableAction compoundActionOf(List<RevertableAction> revertableActions) {
        return new CompoundRevertableAction(revertableActions);
    }

    public static CompoundRevertableAction compoundActionOf(RevertableAction... revertableActions) {
        return new CompoundRevertableAction(List.of(revertableActions));
    }

    private final List<RevertableAction> revertableActions;

    public CompoundRevertableAction(List<RevertableAction> revertableActions) {
        this.revertableActions = revertableActions;
    }

    @Override
    public void execute() {
        ListIterator<RevertableAction> iterator = this.revertableActions.listIterator();
        RevertableAction revertableAction;

        while (iterator.hasNext()) {
            revertableAction = iterator.next();

            revertableAction.execute();
        }
    }

    @Override
    public void undo() {
        ListIterator<RevertableAction> iterator = this.revertableActions.listIterator(this.revertableActions.size());
        RevertableAction revertableAction;

        while (iterator.hasPrevious()) {
            revertableAction = iterator.previous();

            revertableAction.undo();
        }
    }

}
