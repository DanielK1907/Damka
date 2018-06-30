package myproject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * In this frame the game play takes place, including evaluation
 * @author Daniel Kanevsky
 */
public class Damka extends JFrame {
    
    //<editor-fold defaultstate="collapsed" desc="Constants">
    // Height and width of the board
    public final int LENGTH;
    // Number of rows filles with pawns for each side
    public final int PAWN_ROWS;
    
    // Tile length in pixels
    public static int TILE_SIZE;
    
    // The color purple - used in evaluation
    public static final Color PURPLE = new Color(250, 0, 250);
    
    // Number of moves without captures or pawn pushes needed for a draw
    public static final int MOVES_FOR_DRAW = 15;
    
    // Maximum amount of same-color pawns in a position where player can't move
    public static final int MAX_PAWNS_IN_STALEMATE = 5;

    /**
     * Direction vector for possile direction the queen can move
     */
    public static final int[][] QUEEN_DIRS = {
        {1, 1},
        {1, -1},
        {-1, 1},
        {-1, -1}
    };

    // Possible messages for evaluation
    private static final String[] STATES = new String[]
    {
        " Position is roughly equal",
        " White is slightly leading",
        " Black is slightly leading",
        " White is winning",
        " Black is winning"
    };
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Variables">
    // All the game board tiles
    public DamkaTile[][] tiles;
    
    public int whitePawnsLeft;
    public int blackPawnsLeft;
    public int whiteQueens = 0;
    public int blackQueens = 0;
    public int chosenPawnRow;
    public int chosenPawnCol;
    public int movesWithoutProgress = 0;
    public boolean isPawnChosen = false;
    public boolean isForced = false; // Is the player forced to make a capture
    public boolean isOnStreak = false; // Is the player in capture streak
    public boolean isComputer; // Does a computer play in this game
    public boolean turn = false; // false - white to play; true - black to play
    public boolean isComputerPlaying = false; // The computer isn't playing in the first move
    
    //<editor-fold defaultstate="collapsed" desc="JPanels">
    public final JPanel gamePanel = new JPanel(true); // Contains the board
    private final JPanel evalPanel = new JPanel(); // Contains the evaluation
    private final JPanel showEvalPanel = new JPanel(); // Contains the checkbox
    private final JPanel buffer = new JPanel(); // A buffer between "evalPanael" & "showEvalPanel"
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="Other JComponents">
    private final JCheckBox showEval =new JCheckBox("Show evaluation");
    
    private final JLabel evalMsg = new JLabel("Evaluation : ");
    private JLabel evaluation = new JLabel();
    private JLabel stateMsg = new JLabel();
    //</editor-fold>
    
    private Color winningColor = Color.WHITE; // Background color for evaluation
//</editor-fold>
    
    /**
     * Create a new Damka object (Board game as a frame)
     * Initalize the frame with panels a JComponents
     * @param length: Length of the board (width and height
     * @param pawnRows: Number of pawn rows each side has to begin with
     */
    public Damka(int length, int pawnRows) {
        super("Russian Checkers (Shashki)");
        LENGTH = length;
        PAWN_ROWS = pawnRows;
        TILE_SIZE = 600/LENGTH;
        
        tiles = new DamkaTile[LENGTH][LENGTH];
        whitePawnsLeft = LENGTH * PAWN_ROWS / 2;
        blackPawnsLeft = LENGTH * PAWN_ROWS / 2;
        
        setSize(TILE_SIZE * LENGTH, TILE_SIZE * LENGTH + 60);
        
        gamePanel.setLayout(new GridLayout(LENGTH, LENGTH, 0, 0));// default value is 0 anyway
        showEvalPanel.setBackground(Color.GRAY);
        
        showEval.setBackground(Color.GRAY);
        showEval.setSelected(true);
        showEval.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ie) {
                evalMsg.setVisible(!evalMsg.isVisible());
                evaluation.setVisible(!evaluation.isVisible());
                stateMsg.setVisible(!stateMsg.isVisible());
                if (ie.getStateChange() == 1)
                    evaluate();
                else
                    evalPanel.setBackground(Color.BLACK);
            }
        });
        
        // Add JComponents to panels
        evalPanel.add(evalMsg);
        evalPanel.add(evaluation);
        evalPanel.add(stateMsg);
        showEvalPanel.add(showEval);
        
        // Add panels to the frame
        add(buffer);
        add(evalPanel, BorderLayout.WEST);
        add(showEvalPanel, BorderLayout.EAST);
        add(gamePanel, BorderLayout.SOUTH);
    }

    /**
     * Initialize the game panel with pawns in the corrent colors
     */
    public void start()
    {
        //<editor-fold defaultstate="collapsed" desc="create tiles and add them to the game panel">
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < LENGTH; j++) {
                tiles[i][j] = new DamkaTile(i, j, this);
                gamePanel.add(tiles[i][j]);
            }
        }
        //</editor-fold>

        // Initialize Images array with the possible images
        (new DamkaTile()).initialize();

        //<editor-fold defaultstate="collapsed" desc="set pawn colors">
        // set white tales
        for (int i = 0; i < LENGTH; i++) {
            for (int j = i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(DamkaTile.TilePawn.WHITE.ordinal());
            }
        }
        
        // set the white pawns
        for (int i = LENGTH - PAWN_ROWS; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
            }
        }

        // set the black pawns
        for (int i = 0; i < PAWN_ROWS; i++) {
            for (int j = 1 - (i % 2); j < LENGTH; j += 2) {
                tiles[i][j].setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal());
            }
        }

        // set the black remaining slots
        for (int i = PAWN_ROWS; i < LENGTH - PAWN_ROWS; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(DamkaTile.TilePawn.BLACK.ordinal());
            }
        }
        //</editor-fold>
        

        // Frame settings
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null); // center the form
        pack();
        setVisible(true);
        
        
        //evaluate(); // evaluate initial value of position
    }

    /**
     * Turn the chosen pawn off, as well as all the red squares.
     * If the pawn is A queen, call turnQueenOff()
     */
    public void turnPawnOff() {
        if (tiles[chosenPawnRow][chosenPawnCol].color == DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal() ||
            tiles[chosenPawnRow][chosenPawnCol].color == DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal())
        {
            turnQueenOff();
            return;
        }

        if (turn) 
            tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal()); 
        else 
            tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
        
        turnRedSquaresOff();
        isPawnChosen = false;
    }

    /**
     * Turn the chosen queen off, as well as all the red squares
     */
    public void turnQueenOff()
    {
        if (!turn) {
            tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
        } else {
            tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
        }

        turnRedSquaresOff();
        isPawnChosen = false;
    }
    
    /**
     * Turn a pawn on:
     * 1) set chosen pawn properties to tile properties
     * 2) change the pawn color
     * 3) turn on the red pawn squares
     * @param tile: the pawn to turn on
     */
    public void turnPawnOn(DamkaTile tile)
    {
        chosenPawnRow = tile.row;
        chosenPawnCol = tile.col;
        isPawnChosen = true;

        // turn the pawn on if it is the correct turn
        if (tile.color == DamkaTile.TilePawn.WHITE_PAWN.ordinal()) {
            if (!turn) {
                tile.setColor(DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal());
                turnRedPawnSquaresOn();
            } else 
                isPawnChosen = false;
        } else {
            if (turn) {
                tile.setColor(DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal());
                turnRedPawnSquaresOn();
            } else {
                isPawnChosen = false;
            }
        }
    }

    /**
     *
     * @param tile: the queen to turn on.
     * 1) set chosen pawn properties to tile properties
     * 2) change the pawn color
     * 3) turn on the red queen squares
     */
    public void turnQueenOn(DamkaTile tile)
    {
        chosenPawnRow = tile.row;
        chosenPawnCol = tile.col;
        isPawnChosen = true;

        // turn the queen on if it is the correct turn
        if (tile.color == DamkaTile.TilePawn.WHITE_QUEEN.ordinal()) {
            if (!turn) {
                tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal());
                turnRedQueenSquaresOn();
            } else {
                isPawnChosen = false;
            }
        } else {
            if (turn) {
                tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal());
                turnRedQueenSquaresOn();
            } else {
                isPawnChosen = false;
            }
        }
    }
    
    /**
     * Turn the red pawn squares according to the chosen pawn.
     * Function considers the state of the board (forced or not)
     */
    public void turnRedPawnSquaresOn()
    {
        if (!isForced) {
            int direction = (turn) ? 1 : -1;
            if (tiles[chosenPawnRow][chosenPawnCol].col != 0)
                if (tiles[chosenPawnRow + direction][chosenPawnCol - 1].color == 
                        DamkaTile.TilePawn.BLACK.ordinal())
                    tiles[chosenPawnRow + direction][chosenPawnCol - 1].setColor(DamkaTile.TilePawn.RED.ordinal());
            if (tiles[chosenPawnRow][chosenPawnCol].col != LENGTH - 1)
                if (tiles[chosenPawnRow + direction][chosenPawnCol + 1].color == 
                        DamkaTile.TilePawn.BLACK.ordinal())
                    tiles[chosenPawnRow + direction][chosenPawnCol + 1].setColor(DamkaTile.TilePawn.RED.ordinal());                
        }
        else
        {
            int colorEaten1;
            int colorEaten2;
            
            // Determine the colors that are searched
            if (tiles[chosenPawnRow][chosenPawnCol].color == DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal())
            {    
                colorEaten1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
                colorEaten2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
            }
            else
            {
                colorEaten1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
                colorEaten2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
            }
             
            // Make sure this index isn't out of bounds and compare the tile color to the colors searched
            if (tiles[chosenPawnRow][chosenPawnCol].col >= 2 && tiles[chosenPawnRow][chosenPawnCol].row >= 2)
                if (tiles[chosenPawnRow - 2][chosenPawnCol - 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                    ( tiles[chosenPawnRow - 1][chosenPawnCol - 1].color == colorEaten1  ||
                      tiles[chosenPawnRow - 1][chosenPawnCol - 1].color == colorEaten2))
                    tiles[chosenPawnRow - 2][chosenPawnCol - 2].setColor(DamkaTile.TilePawn.RED.ordinal());
            if (tiles[chosenPawnRow][chosenPawnCol].col >= 2 && tiles[chosenPawnRow][chosenPawnCol].row <= LENGTH - 3)
                if (tiles[chosenPawnRow + 2][chosenPawnCol - 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                    (tiles[chosenPawnRow + 1][chosenPawnCol - 1].color == colorEaten1 || 
                     tiles[chosenPawnRow + 1][chosenPawnCol - 1].color == colorEaten2))
                    tiles[chosenPawnRow + 2][chosenPawnCol - 2].setColor(DamkaTile.TilePawn.RED.ordinal());
            if (tiles[chosenPawnRow][chosenPawnCol].col <= LENGTH - 3 && tiles[chosenPawnRow][chosenPawnCol].row >= 2)
                if (tiles[chosenPawnRow - 2][chosenPawnCol + 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                    (tiles[chosenPawnRow - 1][chosenPawnCol + 1].color == colorEaten1 || 
                    tiles[chosenPawnRow - 1][chosenPawnCol + 1].color == colorEaten2))
                    tiles[chosenPawnRow - 2][chosenPawnCol + 2].setColor(DamkaTile.TilePawn.RED.ordinal());
            if (tiles[chosenPawnRow][chosenPawnCol].col <= LENGTH - 3 && tiles[chosenPawnRow][chosenPawnCol].row <= LENGTH - 3)
                if (tiles[chosenPawnRow + 2][chosenPawnCol + 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                    (tiles[chosenPawnRow + 1][chosenPawnCol + 1].color == colorEaten1 || 
                    tiles[chosenPawnRow + 1][chosenPawnCol + 1].color == colorEaten2))
                    tiles[chosenPawnRow + 2][chosenPawnCol + 2].setColor(DamkaTile.TilePawn.RED.ordinal());
        }
    }

    /**
     * Turn the red queen squares according to the chosen pawn.
     * Function considers the state of the board (forced or not)
     * Moreover, the function checks wheter A streak is possible.
     * If a streak is possible the function removes all red squares which
     * do not lead to a streak. (Based on the official rules)
     */
    public void turnRedQueenSquaresOn()
    {
        DamkaTile enemy = null; // The enemy tile in the current iteration
        int enemyColor = -999; // The color of the enemy pawn found (used as a 'temp' variable)
        int currentColor; // Current color index of tile checked
        int currentRowIndex; // Current row index of tile checked
        int currentColIndex; // Current col index of tile checked
        int colorEaten1; // Pawn color to eat
        int colorEaten2; // Queen color to eat
        boolean enemyFound; // Did we came across enemy tile in the current iteration
        boolean doubleFound; // Did we find double option in the current iteration
        
        // Determine the "pray" colors in case position is "must-capture"
        if (turn)
        {
            colorEaten1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
            colorEaten2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
        }
        else
        {
            colorEaten1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
            colorEaten2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
        }
        
        for (int i = 0; i < 4; i++)
        {
            doubleFound = false;
            enemyFound = false;
            currentRowIndex = chosenPawnRow + QUEEN_DIRS[i][0];
            currentColIndex = chosenPawnCol + QUEEN_DIRS[i][1];
            if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                currentColIndex == -1 || currentColIndex == LENGTH)
                continue;
            currentColor = tiles[currentRowIndex][currentColIndex].color;
            if ((currentColor == colorEaten1 || currentColor == colorEaten2) && isForced)
                {
                    enemyFound = true;
                    enemy = tiles[currentRowIndex][currentColIndex];
                    enemyColor = enemy.color;
                    currentRowIndex += QUEEN_DIRS[i][0];
                    currentColIndex += QUEEN_DIRS[i][1];
                     if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                        currentColIndex == -1 || currentColIndex == LENGTH)
                        continue;
                    currentColor = tiles[currentRowIndex][currentColIndex].color;
                }
            
            // Loop will be broken when there is no possibility of fiding a red square
            // in the current direction.
            while (true)
            {
                if (!isForced && currentColor != DamkaTile.TilePawn.BLACK.ordinal())
                    break;
                if (isForced && currentColor != DamkaTile.TilePawn.BLACK.ordinal() && enemyFound)
                    break;
                    
                // If The current color is black and either a capture exists and enemy found
                // or enemy isn't found and there is no capture (using XOR as boolean operator),
                // then the square is valid option, hence I turn that black square to red.
                if (!(isForced ^ enemyFound) && currentColor == DamkaTile.TilePawn.BLACK.ordinal())
                {
                    // Check if a double is possible
                    if (isForced && !doubleFound)
                    {
                        enemy.setColor(DamkaTile.TilePawn.BLACK.ordinal());
                        if (canQueenCapture(currentRowIndex, currentColIndex, colorEaten1, colorEaten2))
                            doubleFound = true;
                        enemy.setColor(enemyColor);
                    }
                    
                    tiles[currentRowIndex][currentColIndex].setColor(DamkaTile.TilePawn.RED.ordinal());
                }
                
                currentRowIndex += QUEEN_DIRS[i][0];
                currentColIndex += QUEEN_DIRS[i][1];
                if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                    currentColIndex == -1 || currentColIndex == LENGTH)
                    break;
                currentColor = tiles[currentRowIndex][currentColIndex].color;
                
                // Check for enemy
                if ((currentColor == colorEaten1 || currentColor == colorEaten2) && !enemyFound && isForced)
                {
                    enemyFound = true;
                    enemy = tiles[currentRowIndex][currentColIndex];
                    enemyColor = enemy.color;
                    currentRowIndex += QUEEN_DIRS[i][0];
                    currentColIndex += QUEEN_DIRS[i][1];
                    if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                        currentColIndex == -1 || currentColIndex == LENGTH)
                        break;
                    currentColor = tiles[currentRowIndex][currentColIndex].color;
                }
            }
            
            // If double found, remove red squares that do not lead to double
            if (doubleFound)
            {
                enemy.setColor(DamkaTile.TilePawn.BLACK.ordinal()); // will be returned
                if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                    currentColIndex == -1 || currentColIndex == LENGTH ||
                    tiles[currentRowIndex][currentColIndex].color != DamkaTile.TilePawn.RED.ordinal())
                {
                    currentRowIndex -= QUEEN_DIRS[i][0];
                    currentColIndex -= QUEEN_DIRS[i][1];
                }
                currentColor = tiles[currentRowIndex][currentColIndex].color;
            
                while (currentColor == DamkaTile.TilePawn.RED.ordinal())
                {
                        if (!canQueenCapture(currentRowIndex, currentColIndex, colorEaten1, colorEaten2))
                        tiles[currentRowIndex][currentColIndex].setColor(DamkaTile.TilePawn.BLACK.ordinal());

                    currentRowIndex -= QUEEN_DIRS[i][0];
                    currentColIndex -= QUEEN_DIRS[i][1];
                    currentColor = tiles[currentRowIndex][currentColIndex].color;
                }
                
                enemy.setColor(enemyColor);
            }
        }
    }
    
    /**
     * Turn all red squares to black.
     */
    public void turnRedSquaresOff()
    {
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                if (tiles[i][j].color == DamkaTile.TilePawn.RED.ordinal()) {
                    tiles[i][j].setColor(DamkaTile.TilePawn.BLACK.ordinal());
                }
            }
        }
    }
    
    /**
     * Move the chosen pawn to a new tile.
     * Premote the pawn to a queen if needed.
     * @param tile: tile to move to
     */
    public void movePawn(DamkaTile tile)
    {  
        movesWithoutProgress = 0;
        if (turn)
        {
            if (tile.row != LENGTH - 1)
                tile.setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal());
            else
            {
                tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
                if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())//changedd
                    Computer.comp.movesStack.peek().wasPremotion = true;
                blackQueens++;
            }
                
        }
        else
        {
            if (tile.row != 0)
                tile.setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
            else
            {
                tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
                if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
                    Computer.comp.movesStack.peek().wasPremotion = true;
                whiteQueens++;
            }
                
        }
        
        tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.BLACK.ordinal());
        isPawnChosen = false;
        turnRedSquaresOff();
        changeTurn();
    }
    
    /**
     * Move the queen from one tile to another.
     * Announce a draw if needed.
     * @param tile: tile to move the queen to.
     */
    public void moveQueen(DamkaTile tile)
    {
        movesWithoutProgress++;
        if (movesWithoutProgress == MOVES_FOR_DRAW && !isComputerPlaying)
            endGame("Draw!!!");
        if (turn)
            tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
        else
            tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
        
        tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.BLACK.ordinal());
        isPawnChosen = false;
        turnRedSquaresOff();
        changeTurn();
    }
    
    /**
     * 1) Change the turn
     * 2) Check if the position is forced
     * 3) Make the computer play if it should
     * 4) Check if the game is over.
     * 5) reevaluate the position if needed
     */
    public void changeTurn()
    {
        //<editor-fold defaultstate="collapsed" desc="flip board">
        /*gamePanel.removeAll();
        gamePanel.repaint();
        gamePanel.
        
        if (turn)
        {
        for (int i = 0; i < LENGTH; i++)
        {
        for (int j = 0; j < LENGTH; j++)
        {
        gamePanel.add(tiles[i][j]);
        }
        }
        }
        else
        {
        for (int i = LENGTH - 1; i >= 0; i--)
        {
        for (int j = LENGTH - 1; j >= 0; j--)
        {
        gamePanel.add(tiles[i][j]);
        }
        }
        }
        
        gamePanel.repaint();
        gamePanel.*/
//</editor-fold>
        
        showEval.setEnabled(true);
        turn = !turn;
        isForced = DoesCaptureExist();
        
        if (isComputer && !isComputerPlaying && turn)
        {
            //gamePanel.update(gamePanel.getGraphics());
            Computer.comp.play();
            isForced = DoesCaptureExist();
        }
        else
        {
            if (isComputerPlaying)
                return;
            if (turn && !isForced && blackPawnsLeft <= MAX_PAWNS_IN_STALEMATE &&
                !canPlay(DamkaTile.TilePawn.BLACK_PAWN.ordinal(),
                         DamkaTile.TilePawn.BLACK_QUEEN.ordinal()))
                endGame("White Wins!!!");
            else if (!turn && !isForced && whitePawnsLeft <= MAX_PAWNS_IN_STALEMATE &&
                     !canPlay(DamkaTile.TilePawn.WHITE_PAWN.ordinal(),
                              DamkaTile.TilePawn.WHITE_QUEEN.ordinal()))
                endGame("Black Wins!!!");
            else if (showEval.isSelected())
                evaluate();
        }
        
    }
    
    private boolean DoesCaptureExist()
    {
        int colorEating1;
        int colorEating2;
        int colorEaten1;
        int colorEaten2;
        
        if (turn)
        {
            colorEating1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
            colorEating2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
            colorEaten1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
            colorEaten2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
        }
        else
        {
            colorEating1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
            colorEating2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
            colorEaten1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
            colorEaten2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
        }
        
        for (int i = 0; i < LENGTH; i++)
        {
            for (int j = 1 - i%2; j < LENGTH; j+= 2)
            {
                if (tiles[i][j].color == colorEating1)
                {
                    if (canPawnCapture(i, j, colorEaten1, colorEaten2))
                        return true;
                }
                else if (tiles[i][j].color == colorEating2)
                    if (canQueenCapture(i, j, colorEaten1, colorEaten2))
                        return true;
            }
        }
        
        return false;
    }
    
   private boolean canPawnCapture(int row, int col, int colorEaten1, int colorEaten2)
   {
       if (col >= 2 && row >= 2)
            if (tiles[row - 2][col - 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                (tiles[row - 1][col - 1].color == colorEaten1  ||
                tiles[row - 1][col - 1].color == colorEaten2))
                return true;
        if (col >= 2 && row <= LENGTH - 3)
            if (tiles[row + 2][col - 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                (tiles[row + 1][col - 1].color == colorEaten1 || 
                tiles[row + 1][col - 1].color == colorEaten2))
                return true;
        if (col <= LENGTH - 3 && row >= 2)
            if (tiles[row - 2][col + 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                (tiles[row - 1][col + 1].color == colorEaten1 || 
                tiles[row - 1][col + 1].color == colorEaten2))
                return true;
        if (col <= LENGTH - 3 && row <= LENGTH - 3)
            if (tiles[row + 2][col + 2].color == DamkaTile.TilePawn.BLACK.ordinal() &&
                (tiles[row + 1][col + 1].color == colorEaten1 || 
                tiles[row + 1][col + 1].color == colorEaten2))
                return true;
       return false;
   }
   
   private boolean canQueenCapture(int row, int col, int colorEaten1, int colorEaten2)
   {
        int currentColor;
        int currentRowIndex;
        int currentColIndex;
        boolean edgeReached;
        for (int i = 0; i < 4; i++)
        {
            edgeReached = false;
            currentRowIndex = row + QUEEN_DIRS[i][0];
            currentColIndex = col + QUEEN_DIRS[i][1];
            if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                currentColIndex == -1 || currentColIndex == LENGTH)
                continue;
            currentColor = tiles[currentRowIndex][currentColIndex].color;
            
            while (currentColor == DamkaTile.TilePawn.BLACK.ordinal() ||
                   currentColor == DamkaTile.TilePawn.RED.ordinal())
            {
                currentRowIndex += QUEEN_DIRS[i][0];
                currentColIndex += QUEEN_DIRS[i][1];
                if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                    currentColIndex == -1 || currentColIndex == LENGTH)
                {
                    edgeReached = true;
                    break;
                }
                currentColor = tiles[currentRowIndex][currentColIndex].color;
            }
            
            if (!edgeReached)
            {
                if (currentColor != colorEaten1 && currentColor != colorEaten2)
                    continue;
                currentRowIndex += QUEEN_DIRS[i][0];
                currentColIndex += QUEEN_DIRS[i][1];
                if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                    currentColIndex == -1 || currentColIndex == LENGTH)
                    continue;
                currentColor = tiles[currentRowIndex][currentColIndex].color;
                if (currentColor == DamkaTile.TilePawn.BLACK.ordinal())
                    return true;
            }
        }
       
       return false;
   }
   
    /**
     * Perform a capture.
     * Call the correct capture function.
     * @param tile: tile to move to.
     */
    public void Capture(DamkaTile tile)
   {
       showEval.setEnabled(false);
       movesWithoutProgress = 0;
       if (turn)
           whitePawnsLeft--;
       else
           blackPawnsLeft--;
           
       int eatingTileColor = tiles[chosenPawnRow][chosenPawnCol].color;
       tiles[chosenPawnRow][chosenPawnCol].setColor(DamkaTile.TilePawn.BLACK.ordinal());
       turnRedSquaresOff();
       if (eatingTileColor == DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal() ||
           eatingTileColor == DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal())
           CaptureWithPawn(tile);
       else
           CaptureWithQueen(tile);
       
   }
   
   private void CaptureWithPawn(DamkaTile tile)
   {
       DamkaTile current = tiles[chosenPawnRow][chosenPawnCol];
       DamkaTile deadTile;
       int colorToEat1, colorToEat2;
       int deadTileRow = (tile.row + current.row)/2;
       int deadTileCol = (tile.col + current.col)/2;
       if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
       {
           Computer.comp.movesStack.peek().capturedCol = deadTileCol;
           Computer.comp.movesStack.peek().capturedRow = deadTileRow;
           Computer.comp.movesStack.peek().capturedColor = tiles[deadTileRow][deadTileCol].color;
       }
       boolean wasPremoted = false;
       
       deadTile = tiles[deadTileRow][deadTileCol];
       if (turn)
       {
           if (deadTile.color == DamkaTile.TilePawn.WHITE_QUEEN.ordinal())
               whiteQueens--;
           colorToEat1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
           colorToEat2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
           deadTile.setColor(DamkaTile.TilePawn.DEAD_WHITE.ordinal());
           if (tile.row == LENGTH - 1)
           {
               tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
               wasPremoted = true;
               blackQueens++;
           }
           else
               tile.setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal());
       }
       else
       {
           if (deadTile.color == DamkaTile.TilePawn.BLACK_QUEEN.ordinal())
               blackQueens--;
           colorToEat1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
           colorToEat2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
           deadTile.setColor(DamkaTile.TilePawn.DEAD_BLACK.ordinal());
           if (tile.row == 0)
           {
               tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
               wasPremoted = true;
               whiteQueens++;
           }
           else   
               tile.setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
       }
       
       if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
            Computer.comp.movesStack.peek().wasPremotion = wasPremoted;
       
       // Check if Position is on streak
       if (!wasPremoted && canPawnCapture(tile.row, tile.col, colorToEat1, colorToEat2))
       {
           isOnStreak = true;
           if (turn)
               tile.setColor(DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal());
           else
               tile.setColor(DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal());
           
           isPawnChosen= true;
           chosenPawnRow = tile.row;
           chosenPawnCol = tile.col;
           turnRedPawnSquaresOn();
           
           if (isComputer && !isComputerPlaying && turn)
           {
               
               try {
                   gamePanel.update(gamePanel.getGraphics());
                   Thread.sleep(321);
               } catch (InterruptedException ex) {
                   Logger.getLogger(Damka.class.getName()).log(Level.SEVERE, null, ex);
               }
               Computer.comp.play();
           }
           
               
       }
       else if (wasPremoted && canQueenCapture(tile.row, tile.col, colorToEat1, colorToEat2))
       {
           isOnStreak = true;
           if (turn)
               tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal());
           else
               tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal());
           
           isPawnChosen= true;
           chosenPawnRow = tile.row;
           chosenPawnCol = tile.col;
           turnRedQueenSquaresOn();
           if (isComputer && !isComputerPlaying && turn)
           {
               
               try {
                   gamePanel.update(gamePanel.getGraphics());
                   Thread.sleep(321);
               } catch (InterruptedException ex) {
                   Logger.getLogger(Damka.class.getName()).log(Level.SEVERE, null, ex);
               }
               Computer.comp.play();
           }
               
       }
       else
       {
            removeDeadTiles();
            isOnStreak = false;
            isForced = false;
            isPawnChosen = false;
            changeTurn();
       }
       
   }
   
   private void CaptureWithQueen(DamkaTile tile)
   {
       DamkaTile current = tiles[chosenPawnRow][chosenPawnCol];
       DamkaTile deadTile;
       int colorToEat1, colorToEat2;
       int rowDir = (int)Math.signum(tile.row - current.row);
       int colDir = (int)Math.signum(tile.col - current.col);
       int deadTileRow = current.row + rowDir;
       int deadTileCol = current.col + colDir;
       
       if (turn)
       {
           tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
           colorToEat1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
           colorToEat2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
       }
       else
       {
           tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
           colorToEat1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
           colorToEat2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
       }
       
       deadTile = tiles[deadTileRow][deadTileCol];
       while (deadTile.color != colorToEat1 && deadTile.color != colorToEat2)
       {
           deadTileRow += rowDir;
           deadTileCol += colDir;
           deadTile = tiles[deadTileRow][deadTileCol];
       }
       
       if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
       {
           Computer.comp.movesStack.peek().capturedCol = deadTileCol;
           Computer.comp.movesStack.peek().capturedRow = deadTileRow;
           Computer.comp.movesStack.peek().capturedColor = tiles[deadTileRow][deadTileCol].color;
       }
       
       if (turn)
       {
           if (deadTile.color == DamkaTile.TilePawn.WHITE_QUEEN.ordinal())
               whiteQueens--;
           deadTile.setColor(DamkaTile.TilePawn.DEAD_WHITE.ordinal());
       }
       else
       {
           if (deadTile.color == DamkaTile.TilePawn.BLACK_QUEEN.ordinal())
               blackQueens--;

           deadTile.setColor(DamkaTile.TilePawn.DEAD_BLACK.ordinal());
       }
       
       // Check for strak
       if (canQueenCapture(tile.row, tile.col, colorToEat1, colorToEat2))
       {
           turnRedSquaresOff();
           isPawnChosen= true;
           isOnStreak = true;
           chosenPawnRow = tile.row;
           chosenPawnCol = tile.col;
           if (turn)
               tile.setColor(DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal());
           else
               tile.setColor(DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal());
           turnRedQueenSquaresOn();
           
           if (isComputer && !isComputerPlaying && turn)
           {
               //gamePanel.update(gamePanel.getGraphics());// has it's downside...
               Computer.comp.play();
           }
               
       }
       else
       {
           isPawnChosen = false;
           removeDeadTiles();
           isForced = false;
           isPawnChosen = false;
           isOnStreak = false;
           changeTurn();
       }
   }
   
   private void removeDeadTiles()
   {
       for (int i = 0; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                if (tiles[i][j].color == DamkaTile.TilePawn.DEAD_WHITE.ordinal() ||
                    tiles[i][j].color == DamkaTile.TilePawn.DEAD_BLACK.ordinal()) {
                    tiles[i][j].setColor(DamkaTile.TilePawn.BLACK.ordinal());
                }
            }
        }
   }
   
    /**
     * End the game with custom form.
     * @param message: message to display
     */
    public void endGame(String message)
   {
       setEnabled(false);
       
       JFrame gameOverF = new JFrame();
       JLabel gameOverL = new JLabel(message);
       JButton gameOverB = new JButton("OK!");
       
       gameOverL.setBounds(60, 95, 100, 20);
       gameOverB.setBounds(35, 20, 120, 40);
       
       gameOverB.addActionListener(new ActionListener()
       {
           @Override
           public void actionPerformed(ActionEvent ae) {
               gameOverF.dispose();
               dispose();
           }
       });
       
       gameOverF.add(gameOverL);
       gameOverF.add(gameOverB);
       
       gameOverF.setSize(200, 150);
       gameOverF.setLayout(null);
       gameOverF.setDefaultCloseOperation(EXIT_ON_CLOSE);
       gameOverF.setResizable(false);
       gameOverF.setLocationRelativeTo(null); // Passing null centers the form!
       gameOverF.setVisible(true);

   }
   
    /**
     * Checks if a move is possible in the current position
     * @param movingColor1: Color of moving pawn
     * @param movingColor2: Color of mivng queen
     * @return
     */
    public boolean canPlay(int movingColor1, int movingColor2)
   {
       int direction = (turn) ? 1 : -1;
       
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                if (tiles[i][j].color == movingColor1)// Check if pawn can move
                {
                    if (j > 0 && tiles[i + direction][j-1].color == DamkaTile.TilePawn.BLACK.ordinal())
                        return true;
                    if (j < LENGTH - 1 && tiles[i + direction][j+1].color == DamkaTile.TilePawn.BLACK.ordinal())
                        return true;
                }
                else if (tiles[i][j].color == movingColor2)// Check if queen can move
                {
                   for (int queen_i = 0; queen_i < 4; queen_i++)
                   {
                       int rowIndex = i + QUEEN_DIRS[queen_i][0];
                       int colIndex = j + QUEEN_DIRS[queen_i][1];
                       if (rowIndex >= 0 && rowIndex <= LENGTH - 1 &&
                           colIndex >= 0 && colIndex <= LENGTH - 1 &&
                           tiles[rowIndex][colIndex].color == DamkaTile.TilePawn.BLACK.ordinal())
                           return true;
                   }
                }
            }
        }
       
       return false;
   }
   
   // Evaluate the position using the In-Place Minimax DFS with Alpha Beta pruning algorithm
   // in the Computer class.
   // Update the evaluation panel according to the evaluation
   private void evaluate()
   {
       if (!isOnStreak && isPawnChosen)
           turnPawnOff();
       isComputerPlaying = true;
       int temp = movesWithoutProgress, msgIndex;
       float posValForWhite =
               -Computer.comp.miniMaxAlphaBeta(0, turn, Computer.MIN_POS_VAL, Computer.MAX_POS_VAL);
       evaluation.setText("" + posValForWhite);
       if (posValForWhite > 1.5)
       {
           msgIndex = 3;
           winningColor = Color.YELLOW;
       }
       else if (posValForWhite > 0.6)
       {
           msgIndex = 1;
           winningColor = Color.ORANGE;
       }
       else if (posValForWhite > -0.6)
       {
           msgIndex = 0;
           winningColor = Color.WHITE;
       }
       else if (posValForWhite > -1.5)
       {
           winningColor = Color.CYAN;
           msgIndex = 2;
       }
       else
       {
           winningColor = PURPLE;
           msgIndex = 4;
       }
       stateMsg.setText(STATES[msgIndex]);
       evalPanel.setBackground(winningColor);
       evaluation.setBackground(winningColor);
       evalMsg.setBackground(winningColor);
       
       isComputerPlaying = false;
       movesWithoutProgress = temp;
   }
}