package com.alexfh.scrabblesolver.gui.action;

public interface Action {

    void undo();

    default void redo() {
        this.execute();
    }

    void execute();

}
