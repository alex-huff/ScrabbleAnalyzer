package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.List;
import java.util.stream.Stream;

public class RevertableAction
{

    public static final RevertableAction nullRevertableAction = new RevertableAction(() ->
                                                                                     {
                                                                                     }, () ->
                                                                                     {
                                                                                     })
    {

        @Override
        public boolean isNull()
        {
            return true;
        }

        @Override
        public RevertableAction withDescription(String description)
        {
            return this;
        }

    };

    public static RevertableAction compoundActionOf(RevertableAction... revertableActions)
    {
        return Stream.of(revertableActions).collect(RevertableActionBuilder.collector());
    }

    public static RevertableAction ofNoInit(final Runnable execute, final Runnable undo)
    {
        return new RevertableAction(execute, undo);
    }

    public static RevertableAction of(final Runnable execute, final Runnable undo)
    {
        return new RevertableAction(execute, undo).init();
    }

    public static <T> RevertableAction removeElementFromListByEquality(final List<T> listOfT, final T toRemove)
    {
        final int index = listOfT.indexOf(toRemove);

        if (index < 0) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> listOfT.remove(index),
            () -> listOfT.add(index, toRemove)
        );
    }

    public static <T> RevertableAction addToList(final List<T> listOfT, final T toAdd)
    {
        return RevertableAction.of(
            () -> listOfT.add(toAdd),
            () -> listOfT.remove(listOfT.size() - 1)
        );
    }

    public static RevertableAction setCharAt(final char[] chars, final int i, final char toSet)
    {
        final char oldChar = chars[i];

        if (oldChar == toSet) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> chars[i] = toSet,
            () -> chars[i] = oldChar
        );
    }

    public static RevertableAction setCharAt(final char[][] char2DArray, final int r, final int c, final char toSet)
    {
        final char oldChar = char2DArray[r][c];

        if (oldChar == toSet) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> char2DArray[r][c] = toSet,
            () -> char2DArray[r][c] = oldChar
        );
    }

    public static RevertableAction setBooleanAt(
        final boolean[][] bool2DArray, final int r, final int c, final boolean toSet
    )
    {
        final boolean oldBool = bool2DArray[r][c];

        if (oldBool == toSet) return RevertableAction.nullRevertableAction;

        return RevertableAction.of(
            () -> bool2DArray[r][c] = toSet,
            () -> bool2DArray[r][c] = oldBool
        );
    }

    private String description = "Untitled action";
    private Runnable execute;
    private Runnable undo;

    private RevertableAction(Runnable execute, Runnable undo)
    {
        this.execute = execute;
        this.undo    = undo;
    }

    public void execute()
    {
        this.execute.run();
    }

    public void undo()
    {
        this.undo.run();
    }

    public void redo()
    {
        this.execute();
    }

    public RevertableAction init()
    {
        this.execute();

        return this;
    }

    public boolean isNull()
    {
        return false;
    }

    public RevertableAction then(final Runnable runAfter)
    {
        if (this.isNull()) return this;

        runAfter.run();

        final Runnable oldExecute = this.execute;
        final Runnable oldUndo    = this.undo;
        this.execute = () ->
        {
            oldExecute.run();
            runAfter.run();
        };
        this.undo    = () ->
        {
            oldUndo.run();
            runAfter.run();
        };

        return this;
    }

    public RevertableAction withDescription(String description)
    {
        if (description == null) return this;

        this.description = description;

        return this;
    }

    public String getDescription()
    {
        return this.description;
    }

}
