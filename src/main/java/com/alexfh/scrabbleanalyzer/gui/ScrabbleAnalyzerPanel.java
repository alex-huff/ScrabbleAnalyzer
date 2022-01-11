package com.alexfh.scrabbleanalyzer.gui;

import com.alexfh.scrabbleanalyzer.gui.action.RevertableAction;
import com.alexfh.scrabbleanalyzer.gui.layout.ScrabbleAnalyzerPanelLayout;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ScrabbleAnalyzerPanel extends JPanel {

    private final ScrabblePanel scrabblePanel;
    private final JLabel notificationBar;
    private final ScrabbleAnalyzerPanelLayout layout;

    public ScrabbleAnalyzerPanel(IScrabbleGameState gameState, Consumer<RevertableAction> onAction) {
        this.scrabblePanel = new ScrabblePanel(
            onAction,
            gameState
        );
        this.notificationBar = new JLabel();
        this.layout = new ScrabbleAnalyzerPanelLayout();

        this.setNotification("Welcome");
        this.setLayout(this.layout);
        this.notificationBar.setFont(new Font("Courier New", Font.PLAIN, 14));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;

        this.add(this.scrabblePanel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 4, 4, 4);

        this.add(this.notificationBar, constraints);
    }

    public void setNotification(String notification) {
        this.notificationBar.setText(
            ZonedDateTime
                .now( ZoneId.systemDefault() )
                .format( DateTimeFormatter.ofPattern( "HH:mm:ss" ) ) + " " +
            notification
        );
    }

    public void onResize(int width, int height) {
        this.notificationBar.setPreferredSize(
            new Dimension(
                width - this.getInsetWidth(this.notificationBar),
                this.notificationBar.getMinimumSize().height
            )
        );
        this.scrabblePanel.setPreferredSize(
            new Dimension(
                width - this.getInsetWidth(this.scrabblePanel),
                height - (
                    this.notificationBar.getMinimumSize().height +
                    this.getInsetHeight(this.notificationBar) +
                    this.getInsetHeight(this.scrabblePanel)
                )
            )
        );
    }

    private int getInsetWidth(Component component) {
        Insets componentInsets = this.getInsets(component);

        return componentInsets.left + componentInsets.right;
    }

    private int getInsetHeight(Component component) {
        Insets componentInsets = this.getInsets(component);

        return componentInsets.top + componentInsets.bottom;
    }

    private Insets getInsets(Component component) {
        GridBagConstraints constraints = this.layout.getConstraints(component);

        return constraints.insets;
    }

    public void loadNewGame(IScrabbleGameState gameState) {
        this.scrabblePanel.loadNewGame(gameState);
    }

    public void clearBoard() {
        this.scrabblePanel.clearBoard();
    }

}
