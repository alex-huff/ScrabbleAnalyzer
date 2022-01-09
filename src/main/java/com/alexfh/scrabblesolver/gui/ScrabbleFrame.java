package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.file.ScrabbleAnalyzerFileFilter;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Stack;

public class ScrabbleFrame extends JFrame {

    private static final Dimension screenWidth = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int defaultTileSize = (int) (ScrabbleFrame.screenWidth.getHeight() * .75F / 15);

    private final Stack<RevertableAction> undoStack = new Stack<>();
    private final Stack<RevertableAction> redoStack = new Stack<>();
    private final ScrabblePanel scrabblePanel;

    public ScrabbleFrame(IScrabbleGameState gameState) {
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
        this.registerKeybindings();
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (ScrabbleFrame.this.isSaved() ||
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
            this::onAction,
            gameState
        );
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenuItem save = new JMenuItem("Save (Ctrl+S)");
        JMenuItem saveAs = new JMenuItem("Save As (Shift+Ctrl+S)");
        JMenuItem clearBoard = new JMenuItem("Clear Board");
        JMenuItem undo = new JMenuItem("Undo (Ctrl+Z)");
        JMenuItem redo = new JMenuItem("Redo (Ctrl+R)");

        save.addActionListener(e -> System.out.println("Not yet implemented"));
        saveAs.addActionListener(e -> this.saveAs());
        clearBoard.addActionListener(e -> this.scrabblePanel.clearBoard());
        undo.addActionListener(e -> this.undo());
        redo.addActionListener(e -> this.redo());
        fileMenu.add(save);
        fileMenu.add(saveAs);
        editMenu.add(clearBoard);
        editMenu.add(undo);
        editMenu.add(redo);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        this.setJMenuBar(menuBar);
        this.add(this.scrabblePanel);
        this.pack();
        this.setVisible(true);
    }

    private void registerKeybindings() {
        JPanel contentPane = ((JPanel) this.getContentPane());
        InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = contentPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        actionMap.put(
            "undo",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScrabbleFrame.this.undo();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("ctrl R"), "redo");
        actionMap.put(
            "redo",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScrabbleFrame.this.redo();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), "save");
        actionMap.put(
            "save",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScrabbleFrame.this.save();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("shift ctrl S"), "saveAs");
        actionMap.put(
            "saveAs",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScrabbleFrame.this.saveAs();
                }
            }
        );
    }

    public boolean isSaved() {
        return false;
    }

    public void onAction(RevertableAction revertableAction) {
        revertableAction.execute();
        this.redoStack.clear();
        this.undoStack.push(revertableAction);
    }

    public void undo() {
        System.out.println("Undoing");
        if (this.undoStack.empty()) return;

        RevertableAction toUndo = this.undoStack.pop();

        toUndo.undo();
        this.redoStack.push(toUndo);
    }

    public void redo() {
        System.out.println("Redoing");
        if (this.redoStack.empty()) return;

        RevertableAction toRedo = this.redoStack.pop();

        toRedo.redo();
        this.undoStack.push(toRedo);
    }

    public void save() {
        System.out.println("Not yet implemented.");
    }

    public void saveAs() {
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

}
