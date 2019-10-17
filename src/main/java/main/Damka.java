package main;

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
    final int LENGTH;
    // Number of rows filled with pawns for each side
    final int PAWN_ROWS;
    
    // Tile length in pixels
    static int TILE_SIZE;
    
    // The color purple - used in evaluation to express black is winning
    private static final Color PURPLE = new Color(250, 0, 250);
    
    // Number of moves without captures or pawn pushes needed for a draw
    static final int MOVES_FOR_DRAW = 15;
    
    // Maximum amount of same-color pawns in a position where player can't move
    static final int MAX_PAWNS_IN_STALEMATE = 5;

    /**
     * Direction vector for possible direction the queen can move
     */
    private static final int[][] QUEEN_DIRS = {
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
    public DamkaPawn[][] tiles;
    
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
     * Initialize the frame with panels a JComponents
     * @param length: Length of the board (width and height
     * @param pawnRows: Number of pawn rows each side has to begin with
     */
    public Damka(int length, int pawnRows) {
        super("Russian Checkers (Shashki)");
        LENGTH = length;
        PAWN_ROWS = pawnRows;
        TILE_SIZE = 600/LENGTH;
        
        tiles = new DamkaPawn[LENGTH][LENGTH];
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
                if (ie.getStateChange() == ItemEvent.SELECTED)
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
     * Initialize the game panel with pawns in the correct colors
     */
    void start()
    {
        //<editor-fold defaultstate="collapsed" desc="create tiles and add them to the game panel">
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 0; j < LENGTH; j++) {
                tiles[i][j] = new DamkaPawn(i, j, this);
                gamePanel.add(tiles[i][j]);
            }
        }
        //</editor-fold>

        // Initialize Images array with the possible images
        (new DamkaPawn()).initialize();

        //<editor-fold defaultstate="collapsed" desc="set pawn colors">
        // set white tales
        for (int i = 0; i < LENGTH; i++) {
            for (int j = i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(TileColor.WHITE);
            }
        }
        
        // set the white pawns
        for (int i = LENGTH - PAWN_ROWS; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(TileColor.WHITE_PAWN);
            }
        }

        // set the black pawns
        for (int i = 0; i < PAWN_ROWS; i++) {
            for (int j = 1 - (i % 2); j < LENGTH; j += 2) {
                tiles[i][j].setColor(TileColor.BLACK_PAWN);
            }
        }

        // set the black remaining slots
        for (int i = PAWN_ROWS; i < LENGTH - PAWN_ROWS; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                tiles[i][j].setColor(TileColor.BLACK);
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
    void turnPawnOff() {
        if (tiles[chosenPawnRow][chosenPawnCol].color == TileColor.BLACK_QUEEN_CHOSEN ||
            tiles[chosenPawnRow][chosenPawnCol].color == TileColor.WHITE_QUEEN_CHOSEN)
        {
            turnQueenOff();
            return;
        }

        if (turn) 
            tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.BLACK_PAWN);
        else 
            tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.WHITE_PAWN);
        
        turnRedSquaresOff();
        isPawnChosen = false;
    }

    /**
     * Turn the chosen queen off, as well as all the red squares
     */
    public void turnQueenOff()
    {
        if (!turn) {
            tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.WHITE_QUEEN);
        } else {
            tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.BLACK_QUEEN);
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
    public void turnPawnOn(DamkaPawn tile)
    {
        chosenPawnRow = tile.row;
        chosenPawnCol = tile.col;
        isPawnChosen = true;

        // turn the pawn on if it is the correct turn
        if (tile.color == TileColor.WHITE_PAWN) {
            if (!turn) {
                tile.setColor(TileColor.WHITE_PAWN_CHOSEN);
                turnRedPawnSquaresOn();
            } else 
                isPawnChosen = false;
        } else {
            if (turn) {
                tile.setColor(TileColor.BLACK_PAWN_CHOSEN);
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
    public void turnQueenOn(DamkaPawn tile)
    {
        chosenPawnRow = tile.row;
        chosenPawnCol = tile.col;
        isPawnChosen = true;

        // turn the queen on if it is the correct turn
        if (tile.color == TileColor.WHITE_QUEEN) {
            if (!turn) {
                tile.setColor(TileColor.WHITE_QUEEN_CHOSEN);
                turnRedQueenSquaresOn();
            } else {
                isPawnChosen = false;
            }
        } else {
            if (turn) {
                tile.setColor(TileColor.BLACK_QUEEN_CHOSEN);
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
                if (tiles[chosenPawnRow + direction][chosenPawnCol - 1].color == TileColor.BLACK)
                    tiles[chosenPawnRow + direction][chosenPawnCol - 1].setColor(TileColor.RED);
            if (tiles[chosenPawnRow][chosenPawnCol].col != LENGTH - 1)
                if (tiles[chosenPawnRow + direction][chosenPawnCol + 1].color == TileColor.BLACK)
                    tiles[chosenPawnRow + direction][chosenPawnCol + 1].setColor(TileColor.RED);
        }
        else
        {
            TileColor colorEaten1;
            TileColor colorEaten2;
            
            // Determine the colors that are searched
            if (tiles[chosenPawnRow][chosenPawnCol].color == TileColor.BLACK_PAWN_CHOSEN)
            {    
                colorEaten1 = TileColor.WHITE_PAWN;
                colorEaten2 = TileColor.WHITE_QUEEN;
            }
            else
            {
                colorEaten1 = TileColor.BLACK_PAWN;
                colorEaten2 = TileColor.BLACK_QUEEN;
            }
             
            // Make sure this index isn't out of bounds and compare the tile color to the colors searched
            if (tiles[chosenPawnRow][chosenPawnCol].col >= 2 && tiles[chosenPawnRow][chosenPawnCol].row >= 2)
                if (tiles[chosenPawnRow - 2][chosenPawnCol - 2].color == TileColor.BLACK &&
                    ( tiles[chosenPawnRow - 1][chosenPawnCol - 1].color == colorEaten1  ||
                      tiles[chosenPawnRow - 1][chosenPawnCol - 1].color == colorEaten2))
                    tiles[chosenPawnRow - 2][chosenPawnCol - 2].setColor(TileColor.RED);
            if (tiles[chosenPawnRow][chosenPawnCol].col >= 2 && tiles[chosenPawnRow][chosenPawnCol].row <= LENGTH - 3)
                if (tiles[chosenPawnRow + 2][chosenPawnCol - 2].color == TileColor.BLACK &&
                    (tiles[chosenPawnRow + 1][chosenPawnCol - 1].color == colorEaten1 || 
                     tiles[chosenPawnRow + 1][chosenPawnCol - 1].color == colorEaten2))
                    tiles[chosenPawnRow + 2][chosenPawnCol - 2].setColor(TileColor.RED);
            if (tiles[chosenPawnRow][chosenPawnCol].col <= LENGTH - 3 && tiles[chosenPawnRow][chosenPawnCol].row >= 2)
                if (tiles[chosenPawnRow - 2][chosenPawnCol + 2].color == TileColor.BLACK &&
                    (tiles[chosenPawnRow - 1][chosenPawnCol + 1].color == colorEaten1 || 
                    tiles[chosenPawnRow - 1][chosenPawnCol + 1].color == colorEaten2))
                    tiles[chosenPawnRow - 2][chosenPawnCol + 2].setColor(TileColor.RED);
            if (tiles[chosenPawnRow][chosenPawnCol].col <= LENGTH - 3 && tiles[chosenPawnRow][chosenPawnCol].row <= LENGTH - 3)
                if (tiles[chosenPawnRow + 2][chosenPawnCol + 2].color == TileColor.BLACK &&
                    (tiles[chosenPawnRow + 1][chosenPawnCol + 1].color == colorEaten1 || 
                    tiles[chosenPawnRow + 1][chosenPawnCol + 1].color == colorEaten2))
                    tiles[chosenPawnRow + 2][chosenPawnCol + 2].setColor(TileColor.RED);
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
        DamkaPawn enemy = null; // The enemy tile in the current iteration
        TileColor enemyColor = null; // The color of the enemy pawn found
        TileColor currentColor; // Current color index of tile checked
        int currentRowIndex; // Current row index of tile checked
        int currentColIndex; // Current col index of tile checked
        TileColor colorEaten1; // Pawn color to eat
        TileColor colorEaten2; // Queen color to eat
        boolean enemyFound; // Did we came across enemy tile in the current iteration
        boolean doubleFound; // Did we find double option in the current iteration
        
        // Determine the "pray" colors in case position is "must-capture"
        if (turn)
        {
            colorEaten1 = TileColor.WHITE_PAWN;
            colorEaten2 = TileColor.WHITE_QUEEN;
        }
        else
        {
            colorEaten1 = TileColor.BLACK_PAWN;
            colorEaten2 = TileColor.BLACK_QUEEN;
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
                if (!isForced && currentColor != TileColor.BLACK)
                    break;
                if (isForced && currentColor != TileColor.BLACK && enemyFound)
                    break;
                    
                // If The current color is black and either a capture exists and enemy found
                // or enemy isn't found and there is no capture (using XOR as boolean operator),
                // then the square is valid option, hence I turn that black square to red.
                if (isForced == enemyFound && currentColor == TileColor.BLACK)
                {
                    // Check if a double is possible
                    if (isForced && !doubleFound)
                    {
                        enemy.setColor(TileColor.BLACK);
                        if (canQueenCapture(currentRowIndex, currentColIndex, colorEaten1, colorEaten2))
                            doubleFound = true;
                        enemy.setColor(enemyColor);
                    }
                    
                    tiles[currentRowIndex][currentColIndex].setColor(TileColor.RED);
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
                enemy.setColor(TileColor.BLACK); // will be returned
                if (currentRowIndex == -1 || currentRowIndex == LENGTH ||
                    currentColIndex == -1 || currentColIndex == LENGTH ||
                    tiles[currentRowIndex][currentColIndex].color != TileColor.RED)
                {
                    currentRowIndex -= QUEEN_DIRS[i][0];
                    currentColIndex -= QUEEN_DIRS[i][1];
                }
                currentColor = tiles[currentRowIndex][currentColIndex].color;
            
                while (currentColor == TileColor.RED)
                {
                        if (!canQueenCapture(currentRowIndex, currentColIndex, colorEaten1, colorEaten2))
                        tiles[currentRowIndex][currentColIndex].setColor(TileColor.BLACK);

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
                if (tiles[i][j].color == TileColor.RED) {
                    tiles[i][j].setColor(TileColor.BLACK);
                }
            }
        }
    }
    
    /**
     * Move the chosen pawn to a new tile.
     * Premote the pawn to a queen if needed.
     * @param tile: tile to move to
     */
    public void movePawn(DamkaPawn tile)
    {  
        movesWithoutProgress = 0;
        if (turn)
        {
            if (tile.row != LENGTH - 1)
                tile.setColor(TileColor.BLACK_PAWN);
            else
            {
                tile.setColor(TileColor.BLACK_QUEEN);
                if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())//changedd
                    Computer.comp.movesStack.peek().wasPremotion = true;
                blackQueens++;
            }
                
        }
        else
        {
            if (tile.row != 0)
                tile.setColor(TileColor.WHITE_PAWN);
            else
            {
                tile.setColor(TileColor.WHITE_QUEEN);
                if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
                    Computer.comp.movesStack.peek().wasPremotion = true;
                whiteQueens++;
            }
                
        }
        
        tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.BLACK);
        isPawnChosen = false;
        turnRedSquaresOff();
        changeTurn();
    }
    
    /**
     * Move the queen from one tile to another.
     * Announce a draw if needed.
     * @param tile: tile to move the queen to.
     */
    public void moveQueen(DamkaPawn tile)
    {
        movesWithoutProgress++;
        if (movesWithoutProgress == MOVES_FOR_DRAW && !isComputerPlaying)
            endGame("Draw!!!");
        if (turn)
            tile.setColor(TileColor.BLACK_QUEEN);
        else
            tile.setColor(TileColor.WHITE_QUEEN);
        
        tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.BLACK);
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
                !canPlay(TileColor.BLACK_PAWN,
                         TileColor.BLACK_QUEEN))
                endGame("White Wins!!!");
            else if (!turn && !isForced && whitePawnsLeft <= MAX_PAWNS_IN_STALEMATE &&
                     !canPlay(TileColor.WHITE_PAWN,
                              TileColor.WHITE_QUEEN))
                endGame("Black Wins!!!");
            else if (showEval.isSelected())
                evaluate();
        }
        
    }
    
    private boolean DoesCaptureExist()
    {
        TileColor colorEating1;
        TileColor colorEating2;
        TileColor colorEaten1;
        TileColor colorEaten2;
        
        if (turn)
        {
            colorEating1 = TileColor.BLACK_PAWN;
            colorEating2 = TileColor.BLACK_QUEEN;
            colorEaten1 = TileColor.WHITE_PAWN;
            colorEaten2 = TileColor.WHITE_QUEEN;
        }
        else
        {
            colorEating1 = TileColor.WHITE_PAWN;
            colorEating2 = TileColor.WHITE_QUEEN;
            colorEaten1 = TileColor.BLACK_PAWN;
            colorEaten2 = TileColor.BLACK_QUEEN;
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
    
   private boolean canPawnCapture(int row, int col, TileColor colorEaten1, TileColor colorEaten2)
   {
       if (col >= 2 && row >= 2)
            if (tiles[row - 2][col - 2].color == TileColor.BLACK &&
                (tiles[row - 1][col - 1].color == colorEaten1  ||
                tiles[row - 1][col - 1].color == colorEaten2))
                return true;
        if (col >= 2 && row <= LENGTH - 3)
            if (tiles[row + 2][col - 2].color == TileColor.BLACK &&
                (tiles[row + 1][col - 1].color == colorEaten1 || 
                tiles[row + 1][col - 1].color == colorEaten2))
                return true;
        if (col <= LENGTH - 3 && row >= 2)
            if (tiles[row - 2][col + 2].color == TileColor.BLACK &&
                (tiles[row - 1][col + 1].color == colorEaten1 || 
                tiles[row - 1][col + 1].color == colorEaten2))
                return true;
        if (col <= LENGTH - 3 && row <= LENGTH - 3)
            return tiles[row + 2][col + 2].color == TileColor.BLACK &&
                    (tiles[row + 1][col + 1].color == colorEaten1 ||
                            tiles[row + 1][col + 1].color == colorEaten2);
       return false;
   }
   
   private boolean canQueenCapture(int row, int col, TileColor colorEaten1, TileColor colorEaten2)
   {
        TileColor currentColor;
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
            
            while (currentColor == TileColor.BLACK ||
                   currentColor == TileColor.RED)
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
                if (currentColor == TileColor.BLACK)
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
    public void Capture(DamkaPawn tile)
   {
       showEval.setEnabled(false);
       movesWithoutProgress = 0;
       if (turn)
           whitePawnsLeft--;
       else
           blackPawnsLeft--;
           
       TileColor eatingTileColor = tiles[chosenPawnRow][chosenPawnCol].color;
       tiles[chosenPawnRow][chosenPawnCol].setColor(TileColor.BLACK);
       turnRedSquaresOff();
       if (eatingTileColor == TileColor.WHITE_PAWN_CHOSEN ||
           eatingTileColor == TileColor.BLACK_PAWN_CHOSEN)
           CaptureWithPawn(tile);
       else
           CaptureWithQueen(tile);
       
   }
   
   private void CaptureWithPawn(DamkaPawn tile)
   {
       DamkaPawn current = tiles[chosenPawnRow][chosenPawnCol];
       DamkaPawn deadTile;
       TileColor colorToEat1, colorToEat2;
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
           if (deadTile.color == TileColor.WHITE_QUEEN)
               whiteQueens--;
           colorToEat1 = TileColor.WHITE_PAWN;
           colorToEat2 = TileColor.WHITE_QUEEN;
           deadTile.setColor(TileColor.DEAD_WHITE);
           if (tile.row == LENGTH - 1)
           {
               tile.setColor(TileColor.BLACK_QUEEN);
               wasPremoted = true;
               blackQueens++;
           }
           else
               tile.setColor(TileColor.BLACK_PAWN);
       }
       else
       {
           if (deadTile.color == TileColor.BLACK_QUEEN)
               blackQueens--;
           colorToEat1 = TileColor.BLACK_PAWN;
           colorToEat2 = TileColor.BLACK_QUEEN;
           deadTile.setColor(TileColor.DEAD_BLACK);
           if (tile.row == 0)
           {
               tile.setColor(TileColor.WHITE_QUEEN);
               wasPremoted = true;
               whiteQueens++;
           }
           else   
               tile.setColor(TileColor.WHITE_PAWN);
       }
       
       if (isComputerPlaying && !Computer.comp.movesStack.isEmpty())
            Computer.comp.movesStack.peek().wasPremotion = wasPremoted;
       
       // Check if Position is on streak
       if (!wasPremoted && canPawnCapture(tile.row, tile.col, colorToEat1, colorToEat2))
       {
           isOnStreak = true;
           if (turn)
               tile.setColor(TileColor.BLACK_PAWN_CHOSEN);
           else
               tile.setColor(TileColor.WHITE_PAWN_CHOSEN);
           
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
               tile.setColor(TileColor.BLACK_QUEEN_CHOSEN);
           else
               tile.setColor(TileColor.WHITE_QUEEN_CHOSEN);
           
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
   
   private void CaptureWithQueen(DamkaPawn tile)
   {
       DamkaPawn current = tiles[chosenPawnRow][chosenPawnCol];
       DamkaPawn deadTile;
       TileColor colorToEat1, colorToEat2;
       int rowDir = (int)Math.signum(tile.row - current.row);
       int colDir = (int)Math.signum(tile.col - current.col);
       int deadTileRow = current.row + rowDir;
       int deadTileCol = current.col + colDir;
       
       if (turn)
       {
           tile.setColor(TileColor.BLACK_QUEEN);
           colorToEat1 = TileColor.WHITE_PAWN;
           colorToEat2 = TileColor.WHITE_QUEEN;
       }
       else
       {
           tile.setColor(TileColor.WHITE_QUEEN);
           colorToEat1 = TileColor.BLACK_PAWN;
           colorToEat2 = TileColor.BLACK_QUEEN;
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
           if (deadTile.color == TileColor.WHITE_QUEEN)
               whiteQueens--;
           deadTile.setColor(TileColor.DEAD_WHITE);
       }
       else
       {
           if (deadTile.color == TileColor.BLACK_QUEEN)
               blackQueens--;

           deadTile.setColor(TileColor.DEAD_BLACK);
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
               tile.setColor(TileColor.BLACK_QUEEN_CHOSEN);
           else
               tile.setColor(TileColor.WHITE_QUEEN_CHOSEN);
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
                if (tiles[i][j].color == TileColor.DEAD_WHITE ||
                    tiles[i][j].color == TileColor.DEAD_BLACK) {
                    tiles[i][j].setColor(TileColor.BLACK);
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
       
       gameOverB.addActionListener(ae -> {
           gameOverF.dispose();
           dispose();
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
     * @param movingColor2: Color of moving queen
     * @return
     */
    public boolean canPlay(TileColor movingColor1, TileColor movingColor2)
   {
       int direction = (turn) ? 1 : -1;
       
        for (int i = 0; i < LENGTH; i++) {
            for (int j = 1 - i % 2; j < LENGTH; j += 2) {
                if (tiles[i][j].color == movingColor1)// Check if pawn can move
                {
                    if (j > 0 && tiles[i + direction][j-1].color == TileColor.BLACK)
                        return true;
                    if (j < LENGTH - 1 && tiles[i + direction][j+1].color == TileColor.BLACK)
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
                           tiles[rowIndex][colIndex].color == TileColor.BLACK)
                           return true;
                   }
                }
            }
        }
       
       return false;
   }
   
   // Evaluate the position using the In-Place Minimax DFS with Alpha Beta pruning algorithm
   // in the main.Computer class.
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