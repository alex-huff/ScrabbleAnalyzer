package com.alexfh.scrabbleanalyzer.gui;

import com.alexfh.scrabbleanalyzer.ScrabbleGame;
import com.alexfh.scrabbleanalyzer.gui.action.RevertableAction;
import com.alexfh.scrabbleanalyzer.gui.file.ScrabbleAnalyzerFileFilter;
import com.alexfh.scrabbleanalyzer.gui.font.ScrabbleFonts;
import com.alexfh.scrabbleanalyzer.gui.tile.TileProvider;
import com.alexfh.scrabbleanalyzer.gui.tile.TileStyle;
import com.alexfh.scrabbleanalyzer.state.IScrabbleGameState;
import com.alexfh.scrabbleanalyzer.state.impl.ScrabbleGameStateImpl;
import com.alexfh.scrabbleanalyzer.state.impl.stream.SAInputStream;
import com.alexfh.scrabbleanalyzer.state.impl.stream.SAOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

public class ScrabbleAnalyzer extends JFrame
{

    private static final Dimension screenWidth     = Toolkit.getDefaultToolkit().getScreenSize();
    public static final  int       defaultTileSize = (int) (ScrabbleAnalyzer.screenWidth.getHeight() * .75F / 15);

    private final Stack<RevertableAction> undoStack  = new Stack<>();
    private final Stack<RevertableAction> redoStack  = new Stack<>();
    private final ScrabbleAnalyzerPanel   scrabbleAnalyzerPanel;
    private final BufferedImage           iconImage;
    private final String                  title      = "ScrabbleAnalyzer";
    private final String                  undoPrefix = "Undo (Ctrl+Z)";
    private final String                  redoPrefix = "Redo (Ctrl+R)";
    private final JMenuItem               undo;
    private final JMenuItem               redo;
    private       IScrabbleGameState      gameState;
    private       IScrabbleGameState      lastSaveState;
    private       File                    saveFile;

    public ScrabbleAnalyzer()
    {
        this.gameState = ScrabbleGameStateImpl.defaultBlankScrabbleGameState();

        this.setSaveFile();
        this.setLastSaveState();

        this.scrabbleAnalyzerPanel = new ScrabbleAnalyzerPanel(this.gameState, this::onAction);
        this.iconImage             = TileProvider.INSTANCE.getTile(
            'a',
            true,
            false,
            false,
            50
        );

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(450, 450));
        this.setIconImage(iconImage);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.registerKeybindings();
        this.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    if (ScrabbleAnalyzer.this.confirmationIfNotSaved("Are you sure you want to close without saving?"))
                    {
                        ScrabbleGame.threadPool.shutdownNow();
                        System.exit(0);
                    }
                }
            }
        );

        Font      menuFont   = ScrabbleFonts.courierNewBold.deriveFont(15F);
        JMenuBar  menuBar    = new JMenuBar();
        JMenu     fileMenu   = new JMenu("File");
        JMenu     editMenu   = new JMenu("Edit");
        JMenu     viewMenu   = new JMenu("View");
        JMenuItem newFile    = new JMenuItem("New File (Ctrl+N)");
        JMenuItem open       = new JMenuItem("Open");
        JMenuItem save       = new JMenuItem("Save (Ctrl+S)");
        JMenuItem saveAs     = new JMenuItem("Save As (Shift+Ctrl+S)");
        JMenuItem clearBoard = new JMenuItem("Clear Board");
        this.undo = new JMenuItem(this.undoPrefix);
        this.redo = new JMenuItem(this.redoPrefix);
        JMenu                tileStyle      = new JMenu("Tile Style");
        JRadioButtonMenuItem iso            = new JRadioButtonMenuItem("Isometric");
        JRadioButtonMenuItem flat           = new JRadioButtonMenuItem("Flat");
        ButtonGroup          tileStyleGroup = new ButtonGroup();

        fileMenu.setFont(menuFont);
        editMenu.setFont(menuFont);
        viewMenu.setFont(menuFont);
        tileStyle.setFont(menuFont);
        newFile.setFont(menuFont);
        open.setFont(menuFont);
        save.setFont(menuFont);
        saveAs.setFont(menuFont);
        clearBoard.setFont(menuFont);
        iso.setFont(menuFont);
        flat.setFont(menuFont);
        this.undo.setFont(menuFont);
        this.redo.setFont(menuFont);
        iso.setSelected(TileStyle.INSTANCE.getIso());
        flat.setSelected(!TileStyle.INSTANCE.getIso());
        tileStyle.add(iso);
        tileStyleGroup.add(iso);
        tileStyle.add(flat);
        tileStyleGroup.add(flat);
        this.undo.setEnabled(false);
        this.redo.setEnabled(false);
        newFile.addActionListener(e -> this.newFile());
        open.addActionListener(e -> this.open());
        save.addActionListener(e -> this.save());
        saveAs.addActionListener(e -> this.saveAs());
        clearBoard.addActionListener(e -> this.scrabbleAnalyzerPanel.clearBoard());
        undo.addActionListener(e -> this.undo());
        redo.addActionListener(e -> this.redo());
        iso.addActionListener(e -> this.setTileStyleAndRepaint(true));
        flat.addActionListener(e -> this.setTileStyleAndRepaint(false));
        fileMenu.add(newFile);
        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        editMenu.add(clearBoard);
        editMenu.add(undo);
        editMenu.add(redo);
        viewMenu.add(tileStyle);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        this.setJMenuBar(menuBar);
        this.add(this.scrabbleAnalyzerPanel);
        this.pack();
        this.setVisible(true);
    }

    private void setTileStyleAndRepaint(boolean isIso)
    {
        if (TileStyle.INSTANCE.getIso() == isIso) return;

        TileProvider.INSTANCE.clearCache();
        TileStyle.INSTANCE.setIso(isIso);
        this.scrabbleAnalyzerPanel.setNotification("Tile style changed to " + (isIso ? "isometric" : "flat"));
        this.scrabbleAnalyzerPanel.repaintGrids();
        System.gc();
    }

    private void registerKeybindings()
    {
        JPanel    contentPane = ((JPanel) this.getContentPane());
        InputMap  inputMap    = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap   = contentPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        actionMap.put(
            "undo",
            new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ScrabbleAnalyzer.this.undo();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("ctrl R"), "redo");
        actionMap.put(
            "redo",
            new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ScrabbleAnalyzer.this.redo();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("ctrl N"), "newFile");
        actionMap.put(
            "newFile",
            new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ScrabbleAnalyzer.this.newFile();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), "save");
        actionMap.put(
            "save",
            new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ScrabbleAnalyzer.this.save();
                }
            }
        );
        inputMap.put(KeyStroke.getKeyStroke("shift ctrl S"), "saveAs");
        actionMap.put(
            "saveAs",
            new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ScrabbleAnalyzer.this.saveAs();
                }
            }
        );
    }

    private boolean confirmationIfNotSaved(String message)
    {
        return
            this.isSaved() ||
            JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirmation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                new ImageIcon(this.iconImage)
            ) == JOptionPane.YES_OPTION;
    }

    private boolean isSaved()
    {
        return this.gameState.isEqualTo(this.lastSaveState);
    }

    private void onAction(RevertableAction action)
    {
        if (action.isNull()) return;

        this.scrabbleAnalyzerPanel.setNotification(action.getDescription());
        this.clearRedo();
        this.pushOntoUndo(action);
    }

    private void undo()
    {
        RevertableAction toUndo;

        if ((toUndo = this.popOffUndo()) == null) return;

        toUndo.undo();
        this.scrabbleAnalyzerPanel.setNotification("Undid: " + toUndo.getDescription());
        this.pushOntoRedo(toUndo);
    }

    private void redo()
    {
        RevertableAction toRedo;

        if ((toRedo = this.popOffRedo()) == null) return;

        toRedo.redo();
        this.scrabbleAnalyzerPanel.setNotification("Redid: " + toRedo.getDescription());
        this.pushOntoUndo(toRedo);
    }

    private void clearUndo()
    {
        this.undoStack.clear();
        this.undo.setText(this.undoPrefix);

        if (this.undo.isEnabled()) this.undo.setEnabled(false);
    }

    private void clearRedo()
    {
        this.redoStack.clear();
        this.redo.setText(this.redoPrefix);

        if (this.redo.isEnabled()) this.redo.setEnabled(false);
    }

    private RevertableAction popOffUndo()
    {
        if (this.undoStack.isEmpty()) return null;

        RevertableAction action = this.undoStack.pop();

        if (this.undoStack.isEmpty())
        {
            this.undo.setText(this.undoPrefix);

            if (this.undo.isEnabled()) this.undo.setEnabled(false);
        }
        else
        {
            this.undo.setText(this.undoPrefix + " " + this.undoStack.peek().getDescription());
        }

        return action;
    }

    private RevertableAction popOffRedo()
    {
        if (this.redoStack.isEmpty()) return null;

        RevertableAction action = this.redoStack.pop();

        if (this.redoStack.isEmpty())
        {
            this.redo.setText(this.redoPrefix);

            if (this.redo.isEnabled()) this.redo.setEnabled(false);
        }
        else
        {
            this.redo.setText(this.redoPrefix + " " + this.redoStack.peek().getDescription());
        }

        return action;
    }

    private void pushOntoUndo(RevertableAction action)
    {
        this.undoStack.push(action);
        this.undo.setText(this.undoPrefix + " " + action.getDescription());

        if (!this.undo.isEnabled()) this.undo.setEnabled(true);
    }

    private void pushOntoRedo(RevertableAction action)
    {
        this.redoStack.push(action);
        this.redo.setText(this.redoPrefix + " " + action.getDescription());

        if (!this.redo.isEnabled()) this.redo.setEnabled(true);
    }

    private void setLastSaveState()
    {
        this.lastSaveState = this.gameState.copyScrabbleGame();
    }

    private void newFile()
    {
        if (!this.confirmationIfNotSaved("Are you sure you want to create a new file without saving?"))
            return;

        this.gameState = ScrabbleGameStateImpl.defaultBlankScrabbleGameState();

        this.setSaveFile();
        this.setLastSaveState();
        this.reloadGame();
        this.scrabbleAnalyzerPanel.setNotification("Created new file");
    }

    private void open()
    {
        if (!this.confirmationIfNotSaved("Are you sure you want to open a new file without saving?"))
            return;

        try
        {
            this.openChooser();

            if (this.saveFile != null)
                this.scrabbleAnalyzerPanel.setNotification("Opened new file: " + this.saveFile.getName());
        }
        catch (IOException ignored)
        {
            this.fileOpenErrorDialog();
        }
        catch (UnsupportedBoardException ignored)
        {
            this.fileOpenErrorDialog("Unsupported board");
        }
    }

    private void openChooser() throws IOException, UnsupportedBoardException
    {
        JFileChooser fileChooser = new JFileChooser();

        if (this.saveFile != null) fileChooser.setCurrentDirectory(this.saveFile.getParentFile());
        fileChooser.setFileFilter(ScrabbleAnalyzerFileFilter.INSTANCE);
        fileChooser.setDialogTitle("Select a file to open");

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        this.loadGameFromFile(this.getSelectedFileFromChooser(fileChooser));
    }

    private void loadGameFromFile(File fileToOpen) throws IOException, UnsupportedBoardException
    {
        this.readFromFile(fileToOpen);
        this.reloadGame();
    }

    private void reloadGame()
    {
        this.clearUndo();
        this.clearRedo();
        System.gc();
        this.scrabbleAnalyzerPanel.loadNewGame(this.gameState);
    }

    private void save()
    {
        if (this.saveFile == null) this.saveAs();
        else this.tryToSaveToFile(this.saveFile);
    }

    private void tryToSaveToFile(File file)
    {
        try
        {
            this.saveToFile(file);

            if (this.saveFile != null)
                this.scrabbleAnalyzerPanel.setNotification("Saved to file: " + this.saveFile.getName());
        }
        catch (IOException e)
        {
            this.fileSaveErrorDialog();
        }
    }

    private void fileOpenErrorDialog()
    {
        this.fileOpenErrorDialog(null);
    }

    private void fileOpenErrorDialog(String message)
    {
        String errorMessage = message == null ? "Could not open file" : "Could not open file: " + message;

        JOptionPane.showMessageDialog(this, errorMessage);
    }

    private void fileSaveErrorDialog()
    {
        JOptionPane.showMessageDialog(this, "Could not save file");
    }

    private void saveAs()
    {
        JFileChooser fileChooser = new JFileChooser();

        if (this.saveFile != null) fileChooser.setCurrentDirectory(this.saveFile.getParentFile());
        fileChooser.setFileFilter(ScrabbleAnalyzerFileFilter.INSTANCE);
        fileChooser.setDialogTitle("Select a file to save");

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File fileToSave = this.getSelectedFileFromChooser(fileChooser);

        this.tryToSaveToFile(fileToSave);
    }

    private File getSelectedFileFromChooser(JFileChooser fileChooser)
    {
        File selectedFile = fileChooser.getSelectedFile();

        if (selectedFile.exists() ||
            !(fileChooser.getFileFilter() instanceof ScrabbleAnalyzerFileFilter)
        ) return selectedFile;

        return new File(selectedFile.getAbsolutePath() + ScrabbleAnalyzerFileFilter.EXTENSION);
    }

    private void setSaveFile()
    {
        this.saveFile = null;

        this.setTitle(this.title + " [" + "Untitled]");
    }

    private void setSaveFile(File file)
    {
        this.saveFile = file;

        this.setTitle(this.title + " [" + file.getName() + "]");
    }

    private void saveToFile(File file) throws IOException
    {
        FileOutputStream fileOut        = new FileOutputStream(file);
        SAOutputStream   saOutputStream = new SAOutputStream(fileOut);

        saOutputStream.writeScrabbleGameState(this.gameState);
        saOutputStream.close();
        this.setLastSaveState();
        this.setSaveFile(file);
    }

    private void readFromFile(File file) throws IOException, UnsupportedBoardException
    {
        FileInputStream    fileIn        = new FileInputStream(file);
        SAInputStream      saInputStream = new SAInputStream(fileIn);
        IScrabbleGameState gameState     = saInputStream.readGameState();

        if (
            !(gameState.getRows() == 15 && gameState.getCols() == 15) ||
            !(gameState.getRackSize() == 7)
        )
        {
            throw new UnsupportedBoardException();
        }

        this.gameState = gameState;

        saInputStream.close();
        this.setLastSaveState();
        this.setSaveFile(file);
    }

    private static class UnsupportedBoardException extends Exception
    {

    }

}
