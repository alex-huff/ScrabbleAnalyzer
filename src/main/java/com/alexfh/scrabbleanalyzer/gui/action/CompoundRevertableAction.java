package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompoundRevertableAction extends RevertableAction {

    public static RevertableAction compoundActionOf(RevertableAction... revertableActions) {
        List<RevertableAction> actions = Stream.of(revertableActions).filter(
            action -> !action.isNull()
        ).collect(Collectors.toList());

        return actions.isEmpty() ? RevertableAction.nullRevertableAction : new CompoundRevertableAction(
            actions
        );
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
