package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.List;
import java.util.stream.Stream;

public
class RevertibleAction
{

    public static final RevertibleAction NULL_REVERTIBLE_ACTION = new RevertibleAction(() ->
    {
    }, () ->
    {
    })
    {

        @Override
        public
        boolean isNull()
        {
            return true;
        }

        @Override
        public
        RevertibleAction withDescription(String description)
        {
            return this;
        }

    };

    public static
    RevertibleAction compoundActionOf(RevertibleAction... revertibleActions)
    {
        return Stream.of(revertibleActions).collect(RevertibleActionBuilder.collector());
    }

    public static
    RevertibleAction ofNoInit(final Runnable execute, final Runnable undo)
    {
        return new RevertibleAction(execute, undo);
    }

    public static
    RevertibleAction of(final Runnable execute, final Runnable undo)
    {
        return new RevertibleAction(execute, undo).init();
    }

    public static
    <T> RevertibleAction removeElementFromListByEquality(final List<T> listOfT, final T toRemove)
    {
        final int index = listOfT.indexOf(toRemove);

        if (index < 0)
        {
            return RevertibleAction.NULL_REVERTIBLE_ACTION;
        }

        return RevertibleAction.of(() -> listOfT.remove(index), () -> listOfT.add(index, toRemove));
    }

    public static
    <T> RevertibleAction addToList(final List<T> listOfT, final T toAdd)
    {
        return RevertibleAction.of(() -> listOfT.add(toAdd), () -> listOfT.remove(listOfT.size() - 1));
    }

    public static
    RevertibleAction setCharAt(final char[] chars, final int i, final char toSet)
    {
        final char oldChar = chars[i];

        if (oldChar == toSet)
        {
            return RevertibleAction.NULL_REVERTIBLE_ACTION;
        }

        return RevertibleAction.of(() -> chars[i] = toSet, () -> chars[i] = oldChar);
    }

    public static
    RevertibleAction setCharAt(final char[][] char2DArray, final int r, final int c, final char toSet)
    {
        final char oldChar = char2DArray[r][c];

        if (oldChar == toSet)
        {
            return RevertibleAction.NULL_REVERTIBLE_ACTION;
        }

        return RevertibleAction.of(() -> char2DArray[r][c] = toSet, () -> char2DArray[r][c] = oldChar);
    }

    public static
    RevertibleAction setBooleanAt(final boolean[][] bool2DArray, final int r, final int c, final boolean toSet)
    {
        final boolean oldBool = bool2DArray[r][c];

        if (oldBool == toSet)
        {
            return RevertibleAction.NULL_REVERTIBLE_ACTION;
        }

        return RevertibleAction.of(() -> bool2DArray[r][c] = toSet, () -> bool2DArray[r][c] = oldBool);
    }

    private String   description = "Untitled action";
    private Runnable execute;
    private Runnable undo;

    private
    RevertibleAction(Runnable execute, Runnable undo)
    {
        this.execute = execute;
        this.undo    = undo;
    }

    public
    void execute()
    {
        this.execute.run();
    }

    public
    void undo()
    {
        this.undo.run();
    }

    public
    void redo()
    {
        this.execute();
    }

    public
    RevertibleAction init()
    {
        this.execute();

        return this;
    }

    public
    boolean isNull()
    {
        return false;
    }

    public
    RevertibleAction then(final Runnable runAfter)
    {
        if (this.isNull())
        {
            return this;
        }

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

    public
    RevertibleAction withDescription(String description)
    {
        if (description == null)
        {
            return this;
        }

        this.description = description;

        return this;
    }

    public
    String getDescription()
    {
        return this.description;
    }

}
