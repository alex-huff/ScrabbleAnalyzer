package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.stream.Collector;

public
class RevertibleActionBuilder
{

    public  Runnable execute;
    public  Runnable undo;
    private boolean  isNull = true;

    public
    void add(final RevertibleAction revertibleAction)
    {
        if (revertibleAction.isNull())
        {
            return;
        }

        if (this.isNull)
        {
            this.isNull = false;
        }

        final Runnable oldExecute = this.execute;
        final Runnable oldUndo    = this.undo;

        this.execute = oldExecute == null ? revertibleAction::execute : () ->
        {
            oldExecute.run();
            revertibleAction.execute();
        };
        this.undo    = oldUndo == null ? revertibleAction::undo : () ->
        {
            revertibleAction.undo();
            oldUndo.run();
        };
    }

    public
    RevertibleActionBuilder addAll(RevertibleActionBuilder revertibleActionBuilder)
    {
        this.add(revertibleActionBuilder.build());

        return this;
    }

    public
    RevertibleAction build()
    {
        return this.isNull ? RevertibleAction.NULL_REVERTIBLE_ACTION : RevertibleAction.ofNoInit(this.execute, this.undo);
    }

    public static
    Collector<RevertibleAction, RevertibleActionBuilder, RevertibleAction> collector()
    {
        return Collector.of(RevertibleActionBuilder::new, RevertibleActionBuilder::add, RevertibleActionBuilder::addAll,
            RevertibleActionBuilder::build);
    }

}

