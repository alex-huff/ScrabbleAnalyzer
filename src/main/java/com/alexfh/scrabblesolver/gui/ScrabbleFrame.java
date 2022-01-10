package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.action.RevertableAction;
import com.alexfh.scrabblesolver.gui.file.ScrabbleAnalyzerFileFilter;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.state.IScrabbleGameState;
import com.alexfh.scrabblesolver.state.impl.ScrabbleGameStateImpl;
import com.alexfh.scrabblesolver.state.impl.stream.SAInputStream;
import com.alexfh.scrabblesolver.state.impl.stream.SAOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

public class ScrabbleFrame extends JFrame {

    private static final Dimension screenWidth = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int defaultTileSize = (int) (ScrabbleFrame.screenWidth.getHeight() * .75F / 15);

    private final Stack<RevertableAction> undoStack = new Stack<>();
    private final Stack<RevertableAction> redoStack = new Stack<>();
    private IScrabbleGameState gameState;
    private final ScrabblePanel scrabblePanel;
    private File saveFile;

    public ScrabbleFrame() {
        this.gameState = ScrabbleGameStateImpl.defaultBlankScrabbleGameState();
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
        JMenuItem newFile = new JMenuItem("New File (Ctrl+N)");
        JMenuItem open = new JMenuItem("Open");
        JMenuItem save = new JMenuItem("Save (Ctrl+S)");
        JMenuItem saveAs = new JMenuItem("Save As (Shift+Ctrl+S)");
        JMenuItem clearBoard = new JMenuItem("Clear Board");
        JMenuItem undo = new JMenuItem("Undo (Ctrl+Z)");
        JMenuItem redo = new JMenuItem("Redo (Ctrl+R)");

        newFile.addActionListener(e -> this.newFile());
        open.addActionListener(e -> this.open());
        save.addActionListener(e -> this.save());
        saveAs.addActionListener(e -> this.saveAs());
        clearBoard.addActionListener(e -> this.scrabblePanel.clearBoard());
        undo.addActionListener(e -> this.undo());
        redo.addActionListener(e -> this.redo());
        fileMenu.add(newFile);
        fileMenu.add(open);
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
        inputMap.put(KeyStroke.getKeyStroke("ctrl N"), "newFile");
        actionMap.put(
            "newFile",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScrabbleFrame.this.newFile();
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

    private boolean isSaved() {
        return false;
    }

    private void onAction(RevertableAction revertableAction) {
        if (revertableAction.isNull()) return;

        this.redoStack.clear();
        this.undoStack.push(revertableAction);
    }

    private void undo() {
        if (this.undoStack.empty()) return;

        RevertableAction toUndo = this.undoStack.pop();

        toUndo.undo();
        this.redoStack.push(toUndo);
    }

    private void redo() {
        if (this.redoStack.empty()) return;

        RevertableAction toRedo = this.redoStack.pop();

        toRedo.redo();
        this.undoStack.push(toRedo);
    }

    private void newFile() {
        this.gameState = ScrabbleGameStateImpl.defaultBlankScrabbleGameState();
        this.saveFile = null;

        this.reloadGame();
    }

    private void open() {
        try {
            this.openChooser();
        } catch (IOException e) {
            this.fileOpenErrorDialog();
        }
    }

    private void openChooser() throws IOException {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileFilter(ScrabbleAnalyzerFileFilter.INSTANCE);
        fileChooser.setDialogTitle("Select a file to open");

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        this.openFromFile(this.getSelectedFileFromChooser(fileChooser));
    }

    private void openFromFile(File fileToOpen) throws IOException {
        this.gameState = this.readFromFile(fileToOpen);

        this.reloadGame();
    }

    private void reloadGame() {
        this.undoStack.clear();
        this.redoStack.clear();
        System.gc();
        this.scrabblePanel.loadNewGame(this.gameState);
    }

    private void save() {
        if (this.saveFile == null) this.saveAs();
        else this.tryToSaveToFile(this.saveFile);
    }

    private void tryToSaveToFile(File file) {
        try {
            this.saveToFile(file);
        } catch (IOException e) {
            this.fileSaveErrorDialog();
        }
    }

    private void fileOpenErrorDialog() {
        JOptionPane.showMessageDialog(this, "Could not open file");
    }

    private void fileSaveErrorDialog() {
        JOptionPane.showMessageDialog(this, "Could not save file");
    }

    private void saveAs() {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileFilter(ScrabbleAnalyzerFileFilter.INSTANCE);
        fileChooser.setDialogTitle("Select a file to save");

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File fileToSave = this.getSelectedFileFromChooser(fileChooser);

        this.tryToSaveToFile(fileToSave);
    }

    private File getSelectedFileFromChooser(JFileChooser fileChooser) {
        File selectedFile = fileChooser.getSelectedFile();

        if (selectedFile.exists() ||
            !(fileChooser.getFileFilter() instanceof ScrabbleAnalyzerFileFilter)
        ) return selectedFile;

        return new File(selectedFile.getAbsolutePath() + ScrabbleAnalyzerFileFilter.EXTENSION);
    }

    private void saveToFile(File file) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        SAOutputStream saOutputStream = new SAOutputStream(fileOut);

        saOutputStream.writeScrabbleGameState(gameState);
        saOutputStream.close();

        this.saveFile = file;
    }

    private IScrabbleGameState readFromFile(File file) throws IOException {
        FileInputStream fileIn = new FileInputStream(file);
        SAInputStream saInputStream = new SAInputStream(fileIn);
        IScrabbleGameState readGameState = saInputStream.readGameState();

        saInputStream.close();

        this.saveFile = file;

        return readGameState;
    }

}
