package com.alexfh.scrabblesolver.gui.action;

import java.util.List;

public interface RevertableAction {

    void execute();

    void undo();

    default void redo() {
        this.execute();
    }

    default RevertableAction init() {
        this.execute();

        return this;
    }

    default RevertableAction then(final Runnable runAfter) {
        runAfter.run();

        return RevertableAction.ofNoInit(
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

    private static RevertableAction ofNoInit(final Runnable execute, final Runnable undo) {
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

    static RevertableAction of(final Runnable execute, final Runnable undo) {
        return RevertableAction.ofNoInit(execute, undo).init();
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

    static RevertableAction setCharAt(final char[][] char2DArray, final int r, final int c, final char toSet) {
        final char oldChar = char2DArray[r][c];

        return RevertableAction.of(
            () -> char2DArray[r][c] = toSet,
            () -> char2DArray[r][c] = oldChar
        );
    }

    static RevertableAction setBooleanAt(final boolean[][] bool2DArray, final int r, final int c, final boolean toSet) {
        final boolean oldBool = bool2DArray[r][c];

        return RevertableAction.of(
            () -> bool2DArray[r][c] = toSet,
            () -> bool2DArray[r][c] = oldBool
        );
    }

    static RevertableAction nullRevertableAction() {
        return RevertableAction.of(() -> {}, () -> {});
    }

}
