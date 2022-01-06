package com.alexfh.scrabblesolver.gui;

import com.alexfh.scrabblesolver.Main;
import com.alexfh.scrabblesolver.ScrabbleGame;
import com.alexfh.scrabblesolver.gui.tile.TileProvider;
import com.alexfh.scrabblesolver.rule.impl.LetterScoreMapImpl;
import com.alexfh.scrabblesolver.state.IScrabbleBoard;
import com.alexfh.scrabblesolver.util.ScrabbleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScrabbleFrame extends JFrame {

    private static final Dimension screenWidth = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int defaultTileSize = (int) (ScrabbleFrame.screenWidth.getHeight() * .75F / 15);

    public ScrabbleFrame(IScrabbleBoard board, char[] playerTiles) {
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
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    ScrabbleGame.threadPool.shutdownNow();
                }
            }
        );

        this.add(new ScrabblePanel(board, new ArrayList<>(Arrays.asList(new String(playerTiles).chars().mapToObj(i -> (char) i).toArray(Character[]::new)))));
        this.pack();
        this.setVisible(true);
    }

    private static class ScrabbleLayout extends GridBagLayout {

        @Override
        protected void arrangeGrid(Container parent) {
            if (parent instanceof ScrabblePanel scrabblePanel) {
                Dimension dimension = parent.getSize();

                scrabblePanel.onResize((int) dimension.getWidth(), (int) dimension.getHeight());
            }

            super.arrangeGrid(parent);
        }

    }

    private static class ScrabblePanel extends JPanel {

        private static final char backspaceChar = '}';

        private final ScrabbleGrid grid;
        private final PlayerTileGrid playerTileGrid;
        private final MoveScroller moveScroller;
        private boolean isVerticalLayout = false;
        private int currentTileSize = defaultTileSize;
        private int updateNum = 0;
        private int lastUpdateReceived = 0;
        private Future<?> pendingUpdate;

        private ScrabblePanel(IScrabbleBoard board, java.util.List<Character> playerTiles) {
            this.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) ScrabblePanel.this.requestFocusInWindow();
                    }
                }
            );

            this.grid = new ScrabbleGrid(board, this::boardInvalidated);
            this.playerTileGrid = new PlayerTileGrid(playerTiles, this::playerTilesInvalidated);
            this.moveScroller = new MoveScroller(this.grid::showMove, this.grid::clearSelectedMove);

            this.setLayout(new ScrabbleLayout());
            this.initializeLayout();
            this.updateMoves();
        }

        private void boardInvalidated() {
            this.updateMoves();
        }

        private void playerTilesInvalidated() {
            this.updateMoves();
        }

        private void updateMoves() {
            if (this.pendingUpdate != null) this.pendingUpdate.cancel(true);

            IScrabbleBoard boardCopy = this.grid.getBoardCopy();
            char[] playerTilesCopy = this.playerTileGrid.getPlayerTilesCopy();
            this.updateNum++;
            int updateNumCopy = this.updateNum;
            this.pendingUpdate = ScrabbleGame.threadPool.submit(
                () -> {
                    try {
                        this.getMoves(boardCopy, playerTilesCopy, updateNumCopy);
                    } catch (InterruptedException ignored) { }
                }
            );
        }

        private void getMoves(IScrabbleBoard boardCopy, char[] playerTilesCopy, int updateNumCopy) throws InterruptedException {
            ScrabbleGame game = new ScrabbleGame(
                LetterScoreMapImpl.defaultScoreMap,
                Main.dictionary,
                boardCopy,
                playerTilesCopy,
                7
            );
            List<ScrabbleGame.Move> moves = game.findMoves();

            Collections.sort(moves);
            ScrabbleUtil.checkInterrupted();
            SwingUtilities.invokeLater(
                () -> {
                    if (this.lastUpdateReceived > updateNumCopy) return;

                    this.lastUpdateReceived = updateNumCopy;

                    this.moveScroller.createListForMoves(moves);
                }
            );
        }

        private void onResize(int width, int height) {
            float aspectRatio = width * 1.0F / height;
            int tileSize;
            float layoutThreshold = 23 / 19F; // exact aspect ratio where vert/hori layouts equal in tile size

            if ((!this.isVerticalLayout && aspectRatio < layoutThreshold) ||
                (this.isVerticalLayout && aspectRatio > layoutThreshold)
            ) {
                this.isVerticalLayout = !this.isVerticalLayout;
                this.initializeLayout();
            }

            if (this.isVerticalLayout) {
                if (aspectRatio > 15 / 19F) {
                    tileSize = height / 19;
                } else {
                    tileSize = width / 15;
                }
            } else {
                if (aspectRatio > 23 / 15F) {
                    tileSize = height / 15;
                } else {
                    tileSize = width / 23;
                }
            }

            this.moveScroller.newSize(
                this.isVerticalLayout ? width : (width - tileSize * 15),
                this.isVerticalLayout ? (height - tileSize * 16) : (height - tileSize)
            );

            if (tileSize != this.currentTileSize) {
                TileProvider.INSTANCE.clearCache();
                this.grid.newSize(tileSize);
                this.playerTileGrid.newSize(tileSize);
                System.gc();
            }

            this.currentTileSize = tileSize;
        }

        private void initializeLayout() {
            ScrabbleLayout layout = (ScrabbleLayout) this.getLayout();
            boolean wasInitialized = this.getComponents().length > 0;

            if (wasInitialized) {
                layout.removeLayoutComponent(this.grid);
                layout.removeLayoutComponent(this.playerTileGrid);
                layout.removeLayoutComponent(this.moveScroller);
            }

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = this.isVerticalLayout ? 1 : 2;

            if (wasInitialized)
                layout.addLayoutComponent(this.grid, c);
            else
                this.add(this.grid, c);

            c = new GridBagConstraints();
            c.gridx = this.isVerticalLayout ? 0 : 1;
            c.gridy = this.isVerticalLayout ? 1 : 0;

            if (wasInitialized)
                layout.addLayoutComponent(this.moveScroller, c);
            else
                this.add(this.moveScroller, c);

            c = new GridBagConstraints();
            c.gridx = this.isVerticalLayout ? 0 : 1;
            c.gridy = this.isVerticalLayout ? 2 : 1;

            if (wasInitialized)
                layout.addLayoutComponent(this.playerTileGrid, c);
            else
                this.add(this.playerTileGrid, c);
        }

    }

    private static class TileLabel extends JLabel {

        private final Consumer<Boolean> onClicked;
        private final BiConsumer<Character, Boolean> letterPressed;
        private final ImageIcon icon;

        public TileLabel(ImageIcon icon, Consumer<Boolean> onClicked, BiConsumer<Character, Boolean> letterPressed) {
            super(icon);

            this.icon = icon;
            this.onClicked = onClicked;
            this.letterPressed = letterPressed;

            this.setFocusable(true);
            this.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            TileLabel.this.requestFocusInWindow();
                            TileLabel.this.onClicked.accept(true);
                        } else if (e.getButton() == MouseEvent.BUTTON3) {
                            TileLabel.this.onClicked.accept(false);
                        }
                    }
                }
            );
            this.addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        char c = e.getKeyChar();

                        if (Character.isAlphabetic(c) || c == ScrabbleUtil.wildCardMarker)
                            TileLabel.this.letterPressed.accept(Character.toLowerCase(c), e.isShiftDown());
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                            TileLabel.this.letterPressed.accept(ScrabblePanel.backspaceChar, e.isShiftDown());
                        }
                    }
                }
            );
        }

        @Override
        public ImageIcon getIcon() {
            return this.icon;
        }

    }

    private static class MoveScroller extends JScrollPane {

        private final Dimension size = new Dimension(ScrabbleFrame.defaultTileSize * 8, ScrabbleFrame.defaultTileSize * 14);
        private final JList<String> jList;
        private List<ScrabbleGame.Move> currentMoves;
        private final Consumer<ScrabbleGame.Move> onMoveSelected;
        private final Runnable onMoveUnselected;

        private MoveScroller(Consumer<ScrabbleGame.Move> onMoveSelected, Runnable onMoveUnselected) {
            this.onMoveSelected = onMoveSelected;
            this.onMoveUnselected = onMoveUnselected;
            this.jList = new JList<>();

            this.jList.setLayoutOrientation(JList.VERTICAL);
            this.jList.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        MoveScroller.this.jList.clearSelection();
                    }
                }
            );
            this.jList.addListSelectionListener(
                listSelectionEvent -> {
                    if (!listSelectionEvent.getValueIsAdjusting()) {
                        int index = this.jList.getSelectedIndex();

                        if (index >= 0) {
                            this.onMoveSelected.accept(this.currentMoves.get(index));
                        } else {
                            this.onMoveUnselected.run();
                        }
                    }
                }
            );
            this.setViewportView(this.jList);
            this.makeScrollbarsFocusList();
        }

        private void makeScrollbarsFocusList() {
            this.makeScrollbarFocusList(this.getVerticalScrollBar());
            this.makeScrollbarFocusList(this.getHorizontalScrollBar());
        }

        private void makeScrollbarFocusList(JScrollBar scrollBar) {
            scrollBar.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) MoveScroller.this.jList.requestFocusInWindow();
                    }
                }
            );
        }

        private void createListForMoves(List<ScrabbleGame.Move> moves) {
            this.currentMoves = moves;
            String[] listData = new String[this.currentMoves.size()];

            for (int i = 0; i < this.currentMoves.size(); i++) {
                ScrabbleGame.Move move = this.currentMoves.get(i);
                listData[i] =
                    i + 1 +
                    " Score: " + move.score() +
                    " Vert: " + move.isVertical() +
                    " Row: " + move.row() +
                    " Col: " + move.col() +
                    " Word: " + move.playedWord() +
                    " Tiles: " + Arrays.toString(move.playedTiles());
            }

            this.jList.setListData(listData);
        }

        private void newSize(int width, int height) {
            this.size.setSize(width, height);
        }

        @Override
        public Dimension getPreferredSize() {
            return this.size;
        }

    }

    private static class PlayerTileGrid extends JPanel {

        private final TileLabel[] labels = new TileLabel[7];
        private int tileSize = ScrabbleFrame.defaultTileSize;
        private final Dimension size = new Dimension(this.tileSize * 7, this.tileSize);
        private final java.util.List<Character> playerTiles;
        private final char[] placedTiles;
        private final Runnable onMovesInvalidated;
        private int cursor = 0;
        private boolean cursorJustSet = false;

        private PlayerTileGrid(List<Character> playerTiles, Runnable onMovesInvalidated) {
            this.playerTiles = playerTiles;
            this.placedTiles = new char[7];

            for (int i = 0; i < 7; i++) {
                this.placedTiles[i] = (i < this.playerTiles.size()) ? this.playerTiles.get(i) : IScrabbleBoard.emptyMarker;
            }

            this.onMovesInvalidated = onMovesInvalidated;

            this.setLayout(new GridLayout(1, 7));

            for (int i = 0; i < 7; i++) {
                int finalI = i;
                TileLabel label = new TileLabel(
                    new ImageIcon(this.getTileAt(i)),
                    isLeft -> this.onTileClicked(finalI, isLeft),
                    this::onCharPressed
                );
                labels[i] = label;

                this.add(label);
            }
        }

        private void onCharPressed(Character character, boolean isShiftDown) {
            if (character == ScrabblePanel.backspaceChar) {
                if (this.cursorJustSet || this.cursor > 0) {
                    if (!this.cursorJustSet) this.cursor--;

                    char oldChar = this.placedTiles[this.cursor];

                    if (oldChar != IScrabbleBoard.emptyMarker) {
                        this.placedTiles[this.cursor] = IScrabbleBoard.emptyMarker;

                        this.updateAndRepaintTileAtCursor();

                        this.playerTiles.remove((Character) oldChar);
                        this.onMovesInvalidated.run();
                    }
                }
            } else {
                if (this.cursor < 7) {
                    char oldChar = this.placedTiles[this.cursor];

                    if (character == ScrabbleUtil.wildCardMarker) {
                        if (oldChar != ScrabbleUtil.wildCardTile) {
                            this.placedTiles[this.cursor] = ScrabbleUtil.wildCardTile;

                            this.updateAndRepaintTileAtCursor();

                            if (oldChar != IScrabbleBoard.emptyMarker) {
                                this.playerTiles.remove((Character) oldChar);
                            }

                            this.playerTiles.add(ScrabbleUtil.wildCardTile);
                            this.onMovesInvalidated.run();
                        }
                    } else if (oldChar != character) {
                        this.placedTiles[this.cursor] = character;

                        this.updateAndRepaintTileAtCursor();

                        if (oldChar != IScrabbleBoard.emptyMarker) {
                            this.playerTiles.remove((Character) oldChar);
                        }

                        this.playerTiles.add(character);
                        this.onMovesInvalidated.run();
                    }

                    this.cursor++;
                }
            }

            this.cursorJustSet = false;
        }

        private void updateAndRepaintTileAtCursor() {
            this.labels[this.cursor].getIcon().setImage(this.getTileAt(this.cursor));
            this.labels[this.cursor].repaint();
        }

        private void onTileClicked(int i, boolean isLeft) {
            if (isLeft) {
                this.cursor = i;
                this.cursorJustSet = true;
            }
        }

        private char[] getPlayerTilesCopy() {
            return this.playerTiles.stream().map(String::valueOf).collect(Collectors.joining()).toCharArray();
        }

        private BufferedImage getTileAt(int i) {
            char c = this.placedTiles[i];

            if (c == ScrabbleUtil.wildCardTile) {
                return TileProvider.INSTANCE.getWildcardTile(true, this.tileSize);
            } else if (c == IScrabbleBoard.emptyMarker) {
                return TileProvider.INSTANCE.getDefaultBlankTile(this.tileSize);
            } else {
                return TileProvider.INSTANCE.getTile(c, false, true, false, this.tileSize);
            }
        }

        private void newSize(int newTileSize) {
            this.tileSize = newTileSize;

            this.size.setSize(this.tileSize * 7, this.tileSize);

            for (int i = 0; i < 7; i++) {
                this.labels[i].getIcon().setImage(this.getTileAt(i));
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return this.size;
        }

    }

    private static class ScrabbleGrid extends JPanel {

        private final TileLabel[][] labels = new TileLabel[15][15];
        private int tileSize = ScrabbleFrame.defaultTileSize;
        private final Dimension size = new Dimension(this.tileSize * 15, this.tileSize * 15);
        private final IScrabbleBoard board;
        private final Runnable onMovesInvalidated;
        private int cursorR = 0;
        private int cursorC = 0;
        private boolean cursorJustSet = false;
        private boolean wasLastMovementForwardVert = false;
        private boolean wasLastMovementForwardHori = false;
        private final char[][] playedWordPreviewChars = IScrabbleBoard.getNewEmptyBoard(15, 15);
        private ScrabbleGame.Move previewedMove;

        private ScrabbleGrid(IScrabbleBoard board, Runnable onMovesInvalidated) {
            this.board = board;
            this.onMovesInvalidated = onMovesInvalidated;

            this.setLayout(new GridLayout(15, 15));

            for (int r = 0; r < 15; r++) {
                for (int c = 0; c < 15; c++) {
                    int finalR = r;
                    int finalC = c;
                    TileLabel label = new TileLabel(
                        new ImageIcon(this.getTileAt(r, c)),
                        isLeft -> this.onTileClicked(finalR, finalC, isLeft),
                        this::onCharPressed
                    );
                    labels[r][c] = label;

                    this.add(label);
                }
            }
        }

        private void showMove(ScrabbleGame.Move move) {
            this.clearSelectedMove();

            this.previewedMove = move;
            ScrabbleGame.Offset offset = this.previewedMove.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
            int startRow = this.previewedMove.row();
            int startCol = this.previewedMove.col();

            for (int i = 0; i < this.previewedMove.playedTiles().length; i++) {
                char placedChar = this.previewedMove.playedTiles()[i];
                int spotInWord = this.previewedMove.tileSpotsInWord()[i];
                int newRow = offset.newRow(startRow, spotInWord);
                int newCol = offset.newCol(startCol, spotInWord);
                char toPlace;

                if (placedChar == ScrabbleUtil.wildCardTile) {
                    toPlace = this.previewedMove.playedWord().charAt(spotInWord);
                } else {
                    toPlace = Character.toUpperCase(placedChar);
                }

                this.playedWordPreviewChars[newRow][newCol] = toPlace;

                this.updateAndRepaintTileAt(newRow, newCol);
            }
        }

        private void clearSelectedMove() {
            if (this.previewedMove == null) return;

            ScrabbleGame.Offset offset = this.previewedMove.isVertical() ? ScrabbleGame.vertOffset : ScrabbleGame.horiOffset;
            int startRow = this.previewedMove.row();
            int startCol = this.previewedMove.col();

            for (int i = 0; i < this.previewedMove.playedTiles().length; i++) {
                int spotInWord = this.previewedMove.tileSpotsInWord()[i];
                int newRow = offset.newRow(startRow, spotInWord);
                int newCol = offset.newCol(startCol, spotInWord);
                this.playedWordPreviewChars[newRow][newCol] = IScrabbleBoard.emptyMarker;

                this.updateAndRepaintTileAt(newRow, newCol);
            }
        }

        private void updateAndRepaintTileAtCursor() {
            this.updateAndRepaintTileAt(this.cursorR, this.cursorC);
        }

        private void updateAndRepaintTileAt(int r, int c) {
            this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
            this.labels[r][c].repaint();
        }

        private void onCharPressed(Character character, boolean isShiftDown) {
            if (character == ScrabbleUtil.wildCardMarker) return;

            if (character == ScrabblePanel.backspaceChar) {
                if (this.cursorJustSet || (isShiftDown && this.cursorR > 0) || (!isShiftDown && this.cursorC > 0)) {
                    if (!this.cursorJustSet) {
                        if (isShiftDown) {
                            if (this.wasLastMovementForwardHori) {
                                this.cursorC--;
                            } else {
                                this.cursorR--;
                            }
                        } else {
                            if (this.wasLastMovementForwardVert) {
                                this.cursorR--;
                            } else {
                                this.cursorC--;
                            }
                        }

                        this.wasLastMovementForwardVert = false;
                        this.wasLastMovementForwardHori = false;
                    }

                    if (!this.board.isEmptyAt(this.cursorR, this.cursorC)) {
                        this.board.removeCharAt(this.cursorR, this.cursorC);
                        this.updateAndRepaintTileAtCursor();
                        this.onMovesInvalidated.run();
                    }
                }
            } else {
                if ((isShiftDown && this.cursorR < 15) || (!isShiftDown && this.cursorC < 15)) {
                    if (isShiftDown) {
                        if (this.wasLastMovementForwardHori) {
                            if (this.cursorR == 14) return;

                            this.cursorR++;
                            this.cursorC--;
                        }

                        this.wasLastMovementForwardVert = true;
                        this.wasLastMovementForwardHori = false;
                    } else {
                        if (this.wasLastMovementForwardVert) {
                            if (this.cursorC == 14) return;

                            this.cursorR--;
                            this.cursorC++;
                        }

                        this.wasLastMovementForwardHori = true;
                        this.wasLastMovementForwardVert = false;
                    }

                    if (
                        !(
                            !this.board.isEmptyAt(this.cursorR, this.cursorC) &&
                            (this.board.getCharAt(this.cursorR, this.cursorC) == character) &&
                            (!this.board.isWildcardAt(this.cursorR, this.cursorC))
                        )
                    ) {
                        this.board.setCharAt(this.cursorR, this.cursorC, character);
                        this.board.setWildcardAt(this.cursorR, this.cursorC, false);
                        this.updateAndRepaintTileAtCursor();
                        this.onMovesInvalidated.run();
                    }

                    if (isShiftDown)
                        this.cursorR++;
                    else
                        this.cursorC++;
                }
            }

            this.cursorJustSet = false;
        }

        private void onTileClicked(int r, int c, boolean isLeft) {
            if (isLeft) {
                this.cursorR = r;
                this.cursorC = c;
                this.cursorJustSet = true;
                this.wasLastMovementForwardVert = false;
                this.wasLastMovementForwardHori = false;
            } else {
                if (!this.board.isEmptyAt(r, c)) {
                    this.board.setWildcardAt(r, c, !this.board.isWildcardAt(r, c));
                    this.updateAndRepaintTileAt(r, c);
                    this.onMovesInvalidated.run();
                }
            }
        }

        private IScrabbleBoard getBoardCopy() {
            return this.board.copy();
        }

        private BufferedImage getTileAt(int r, int c) {
            if (this.board.isEmptyAt(r, c)) {
                char previewChar = this.playedWordPreviewChars[r][c];

                if (previewChar != IScrabbleBoard.emptyMarker) {
                    char previewCharLower;
                    boolean isWild;

                    if (Character.isUpperCase(previewChar)) {
                        previewCharLower = Character.toLowerCase(previewChar);
                        isWild = false;
                    } else {
                        previewCharLower = previewChar;
                        isWild = true;
                    }

                    return TileProvider.INSTANCE.getTile(
                        previewCharLower,
                        isWild,
                        true,
                        true,
                        this.tileSize
                    );
                }

                return TileProvider.INSTANCE.getBlankTile(
                    this.board.getLetterMultiplierAt(r, c),
                    this.board.getWordMultiplierAt(r, c),
                    this.tileSize
                );
            }

            return TileProvider.INSTANCE.getTile(
                this.board.getCharAt(r, c),
                this.board.isWildcardAt(r, c),
                true,
                false,
                this.tileSize
            );
        }

        private void newSize(int newTileSize) {
            this.tileSize = newTileSize;

            this.size.setSize(this.tileSize * 15, this.tileSize * 15);

            for (int r = 0; r < 15; r++) {
                for (int c = 0; c < 15; c++) {
                    this.labels[r][c].getIcon().setImage(this.getTileAt(r, c));
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return this.size;
        }

    }

}
