package com.alexfh.scrabblesolver.gui.action;

import java.util.List;

public interface RevertableAction {

    void execute();

    void undo();

    default void redo() {
        this.execute();
    }

    default RevertableAction then(final Runnable runAfter) {
        return RevertableAction.of(
            () -> {
                RevertableAction.this.execute();
                runAfter.run();
            },
            () -> {
                RevertableAction.this.undo();
                runAfter.run();
            }
        );
    }

    static RevertableAction of(final Runnable execute, final Runnable undo) {
        return new RevertableAction() {

            @Override
            public void execute() {
                execute.run();
            }

            @Override
            public void undo() {
                undo.run();
            }

        };
    }

    static <T> RevertableAction removeElementFromListByEquality(final List<T> listOfT, final T toRemove) {
        final int index = listOfT.indexOf(toRemove);

        if (index < 0) return RevertableAction.nullRevertableAction();

        return RevertableAction.of(
            () -> listOfT.remove(index),
            () -> listOfT.add(index, toRemove)
        );
    }

    static <T> RevertableAction addToList(final List<T> listOfT, final T toAdd) {
        return RevertableAction.of(
            () -> listOfT.add(toAdd),
            () -> listOfT.remove(listOfT.size() - 1)
        );
    }

    static RevertableAction setCharAt(final char[] chars, final int i, final char toSet) {
        final char oldChar = chars[i];

        return RevertableAction.of(
            () -> chars[i] = toSet,
            () -> chars[i] = oldChar
        );
    }

    static RevertableAction nullRevertableAction() {
        return RevertableAction.of(() -> {}, () -> {});
    }

}
