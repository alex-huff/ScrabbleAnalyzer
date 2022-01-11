package com.alexfh.scrabbleanalyzer.gui.action;

import java.util.stream.Collector;

public class RevertableActionBuilder {

    public Runnable execute;
    public Runnable undo;
    private boolean isNull = true;

    public void add(final RevertableAction revertableAction) {
        if (revertableAction.isNull()) return;

        if (this.isNull) this.isNull = false;

        final Runnable oldExecute = this.execute;
        final Runnable oldUndo = this.undo;

        this.execute = oldExecute == null ? revertableAction::execute : () -> {
            oldExecute.run();
            revertableAction.execute();
        };
        this.undo = oldUndo == null ? revertableAction::undo : () -> {
            revertableAction.undo();
            oldUndo.run();
        };
    }

    public RevertableActionBuilder addAll(RevertableActionBuilder revertableActionBuilder) {
        this.add(revertableActionBuilder.build());

        return this;
    }

    public RevertableAction build() {
        return this.isNull ?
            RevertableAction.nullRevertableAction :
            RevertableAction.ofNoInit(this.execute, this.undo);
    }

    public static Collector<RevertableAction, RevertableActionBuilder, RevertableAction> collector() {
        return Collector.of(
            RevertableActionBuilder::new,
            RevertableActionBuilder::add,
            RevertableActionBuilder::addAll,
            RevertableActionBuilder::build
        );
    }

}

