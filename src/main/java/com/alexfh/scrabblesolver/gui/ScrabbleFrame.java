package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.Action;
import com.alexfh.scrabblesolver.gui.file.ScrabbleAnalyzerFileFilter;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ScrabbleFrame extends JFrame {

    private static final Dimension screenWidth = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int defaultTileSize = (int) (ScrabbleFrame.screenWidth.getHeight() * .75F / 15);

    private final ScrabbleAnalyzer scrabbleAnalyzer;
    private final ScrabblePanel scrabblePanel;

    public ScrabbleFrame(ScrabbleAnalyzer scrabbleAnalyzer, Consumer<Action> onAction, IScrabbleBoard board, char[] playerTiles) {
        this.scrabbleAnalyzer = scrabbleAnalyzer;
        BufferedImage iconImage = TileProvider.INSTANCE.getTile(
            'a',
            true,
            false,
            false,
            50
        );

        this.setTitle("ScrabbleAnalyzer");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(400, 400));
        this.setIconImage(iconImage);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (ScrabbleFrame.this.scrabbleAnalyzer.isSaved() ||
                        JOptionPane.showConfirmDialog(
                            ScrabbleFrame.this,
                            "Are you sure you want to close without saving?",
                            "Confirmation",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            new ImageIcon(iconImage)
                        ) == JOptionPane.YES_OPTION
                    ) {
                        ScrabbleGame.threadPool.shutdownNow();
                        System.exit(0);
                    }
                }
            }
        );

        JMenuBar menuBar = new JMenuBar();
        this.scrabblePanel = new ScrabblePanel(
            onAction,
            board,
            new ArrayList<>(
                Arrays.asList(
                    new String(playerTiles).chars().mapToObj(
                        i -> (char) i
                    ).toArray(Character[]::new)
                )
            )
        );
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem clearBoard = new JMenuItem("Clear Board");

        save.addActionListener(e -> System.out.println("Not yet implemented"));
        saveAs.addActionListener(
            e -> {
                JFileChooser fileChooser = new JFileChooser();

                fileChooser.setFileFilter(ScrabbleAnalyzerFileFilter.INSTANCE);
                fileChooser.setDialogTitle("Select a file to save");

                int selection = fileChooser.showSaveDialog(this);

                if (selection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();

                    System.out.println(
                        "Save as file: " + (fileChooser.getFileFilter() instanceof ScrabbleAnalyzerFileFilter ? fileToSave.getAbsolutePath() + ScrabbleAnalyzerFileFilter.EXTENSION : fileToSave.getAbsolutePath())
                    );
                }
            }
        );
        clearBoard.addActionListener(e -> this.scrabblePanel.clearBoard());
        fileMenu.add(save);
        fileMenu.add(saveAs);
        editMenu.add(clearBoard);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        this.setJMenuBar(menuBar);
        this.add(this.scrabblePanel);
        this.pack();
        this.setVisible(true);
    }

}
