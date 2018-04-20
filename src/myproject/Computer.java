package myproject; 

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static myproject.Damka.MAX_PAWNS_IN_STALEMATE;
import static myproject.DamkaTile.TilePawn.BLACK;

/**
 * AI for playing Shashki
 * @author Daniel Kanevsky
 */
public class Computer {
    
    private Computer(){}
    
    class Move
    {
        public int fromRow; // Row index of origin tile
        public int fromCol; // Col index of origin tile
        public int toRow; // Row index of destination tile
        public int toCol; // Col index of destination tile
        public int movesWithoutProgress; // Moves without progress before this move was played
        public boolean isCapture; // Is this move a capture?
        public int capturedColor; // Color of captured piece
        public int capturedRow; // Row of captured piece
        public int capturedCol; // Col of captured piece
        public boolean wasOnStreak; // Was this move made as the part of streak
        public boolean wasPremotion; // Was this a premotion to a queen
        public boolean turn; // Whose turn is it? false - white, true - black
        ArrayList<Integer> deadTiles; // Dead tiles on board before making this move
        
        public Move(int fromRow, int fromCol, int toRow, int toCol, int movesWithoutProgress,
                boolean isCapture, boolean wasOnStreak, boolean turn)
        {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.movesWithoutProgress = movesWithoutProgress;
            this.isCapture = isCapture;
            this.wasOnStreak = wasOnStreak;
            this.turn = turn;
            wasPremotion = false;
        } 
    }
    
    private static final int DEPTH_MAX = 8;
    public static final float MIN_POS_VAL = -10000;
    public static final float MAX_POS_VAL =  10000;
    
    // Singleton instance
    public static Computer comp = new Computer();

    public Damka board;
    public Move moveToPlay;
    private static Random rand = new Random();
    
    Stack<Move> movesStack = new Stack<>();
    
    // Generate all possible moves in the current position
    private ArrayList<Move> generateMoves ()
    {
        ArrayList<Move> moves = new ArrayList<>();
        int playing1, playing2;
        boolean isCapture = board.isForced;
        
        // If the board is on strak, generate the moves whit the pawn/gueen on streak.
        if (board.isOnStreak)
        {
            for (int i = 0; i < board.LENGTH; i++)
            {
                for (int j = 1 - i%2; j < board.LENGTH; j+= 2)
                {
                    if (board.tiles[i][j].color == DamkaTile.TilePawn.RED.ordinal())
                    {
                        moves.add(new Move(board.chosenPawnRow, board.chosenPawnCol,i,j,0,true, true, board.turn));
                    }
                }
            }
            
            return moves;
        }
        
        if (board.turn)
        {
            playing1 = DamkaTile.TilePawn.BLACK_PAWN.ordinal();
            playing2 = DamkaTile.TilePawn.BLACK_QUEEN.ordinal();
        }
        else
        {
            playing1 = DamkaTile.TilePawn.WHITE_PAWN.ordinal();
            playing2 = DamkaTile.TilePawn.WHITE_QUEEN.ordinal();
        }
        
        
        
        for (int i = 0; i < board.LENGTH; i++) {
            for (int j = 1 - i % 2; j < board.LENGTH; j += 2) {
                if (board.tiles[i][j].color == playing1 ||
                    board.tiles[i][j].color == playing2) 
                {
                    board.chosenPawnRow = i;
                    board.chosenPawnCol = j;
                    
                    // Turn the red squares for the current tile
                    if (board.tiles[i][j].color == playing1)
                    {
                        if (board.turn)
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal());
                        else
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal());
                        board.turnRedPawnSquaresOn();
                        if (board.turn)
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal());
                        else
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
                    }
                    else
                    {
                        if (board.turn)
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal());
                        else
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal());
                        board.turnRedQueenSquaresOn();
                        if (board.turn)
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.BLACK_QUEEN.ordinal());
                        else
                            board.tiles[i][j].setColor(DamkaTile.TilePawn.WHITE_QUEEN.ordinal());
                    }
                        
                    // Add the moves for the current tile
                    for (int k = 0; k < board.LENGTH; k++)
                    {
                        for (int l = 1 - k%2; l < board.LENGTH; l += 2)
                        {
                            if (board.tiles[k][l].color == DamkaTile.TilePawn.RED.ordinal())
                            {
                                board.tiles[k][l].setColor(BLACK.ordinal());
                                moves.add(new Move(i, j, k, l,board.movesWithoutProgress ,isCapture, false, board.turn));
                            }
                        }
                    }
                }
            }
        }
        
        return moves;
    }
     
    private void findBestMove()
    {
        ArrayList<Move> possibilities = generateMoves();
        if (possibilities.isEmpty())
            return;
        
        // the Move to play is the first possible move as a default option
        moveToPlay = possibilities.get(0);
        
        // If there is only one move to make -> do it!
        if (possibilities.size() == 1)
            return;
        
        float moveValue, bestValue = MIN_POS_VAL;
        boolean isOver = false; // No need to look for more moves if a win found
        
        for (Move possibility : possibilities)
        {
            movesStack.push(possibility);
            makeMove(possibility);
            // evaluate the position with the move made, the best value so far is the alpha value
            moveValue = miniMaxAlphaBeta(1, board.isOnStreak, bestValue, MAX_POS_VAL);
            
            if (moveValue > bestValue)
            {
                bestValue = moveValue;
                moveToPlay = possibility;
                if (bestValue == MAX_POS_VAL)
                    isOver = true;
            }
            undoMove(movesStack.pop()); // return the board to it's previous state
            if (isOver)
                break;
        }
        
    }
    
    /**
     * Search for the best move using MiniMax DFS search with
     * Alpha-Beta pruning optimization.
     * @param currentDepth : the Depth reached by the recursive calls from the root node
     * @param Max : true -> Max is playing ~~~ false -> Min is playing
     * @param alpha : alpha value -> best position value guaranteed for Max in the current node
     * @param beta : beta value - > best position value guaranteed for Min in the current node
     * @return : position Value
     */
    public float miniMaxAlphaBeta(int currentDepth, boolean Max, float alpha, float beta)
    {
        if (board.movesWithoutProgress == board.MOVES_FOR_DRAW)
            return 0;
        if (currentDepth == DEPTH_MAX)
            return evaluatePosition();
        
        float positionValue; // value of position = best moveValue so far
        float moveValue; // used as a 'temp' variable' to determine value of each move
        ArrayList<Move> possibilities = generateMoves();
        int sign;
        
        if (Max)
        {
      
            positionValue = MIN_POS_VAL;
            sign = 1;
        }
        else
        {
            positionValue = MAX_POS_VAL;
            sign = -1;
        }
        
        // Choose the best move from the possible moves recursively
        for (Move possibility : possibilities)
        {
            movesStack.push(possibility);
            makeMove(possibility);
            /*board.gamePanel.update(board.gamePanel.getGraphics());
            try {
            Thread.sleep(50);
            } catch (InterruptedException ex) {
            Logger.getLogger(Computer.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            if (board.isOnStreak)
                moveValue = miniMaxAlphaBeta(currentDepth, Max, alpha, beta);
            else
                moveValue = miniMaxAlphaBeta(currentDepth+1, !Max, alpha, beta);
            if (Math.signum(moveValue - positionValue) == sign)
                positionValue = moveValue;
            
            if (Max && positionValue > alpha)
                alpha = positionValue;
            else if (!Max && positionValue < beta)
                beta = positionValue;
            
            undoMove(movesStack.pop());
            /*board.gamePanel.update(board.gamePanel.getGraphics());
            try {
            Thread.sleep(50);
            } catch (InterruptedException ex) {
            Logger.getLogger(Computer.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            // Alpha-Beta Purning!!!
            if (alpha >= beta)
                break;
        }
        
        return positionValue;
    }
    
    // Search for the best move via minimax without Alpha-Beta pruning
    private float miniMaxBestMove(int currentDepth, int sign)
    {
        if (board.movesWithoutProgress == Damka.MOVES_FOR_DRAW)
            return 0;
        if (currentDepth == DEPTH_MAX)
            return evaluatePosition();
        
        ArrayList<Move> possibilities = generateMoves();
        float positionValue = MIN_POS_VAL;
        float moveValue;
        boolean bestMoveFound = false;
        
        for (Move possibility : possibilities)
        {
            movesStack.push(possibility);
            makeMove(possibility);
            if (board.isOnStreak) 
                moveValue = miniMaxBestMove(currentDepth + 1, sign)*sign;
            else
                moveValue = miniMaxBestMove(currentDepth + 1, -sign)*sign;
            
            if (moveValue > positionValue)
            {
                positionValue = moveValue;
                if (currentDepth == 0)
                    moveToPlay = possibility;
                if (positionValue == MAX_POS_VAL)
                    bestMoveFound = true;
                
            }
                
            undoMove(movesStack.pop());
            if (bestMoveFound)
                break;
        }
        
        return positionValue*sign;
    }

    private void makeMove(Move move)
    {
        board.chosenPawnRow = move.fromRow;
        board.chosenPawnCol = move.fromCol;
        DamkaTile originTile = board.tiles[move.fromRow][move.fromCol];
        DamkaTile destinationTile = board.tiles[move.toRow][move.toCol];
        
        // set the pawn/queen as chosen if isn't already
        if (originTile.color == DamkaTile.TilePawn.WHITE_PAWN.ordinal())
            originTile.setColor(DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal());
        else if (originTile.color == DamkaTile.TilePawn.BLACK_PAWN.ordinal())
            originTile.setColor(DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal());
        else if (originTile.color == DamkaTile.TilePawn.WHITE_QUEEN.ordinal())
            originTile.setColor(DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal());
        else if (originTile.color == DamkaTile.TilePawn.BLACK_QUEEN.ordinal())
            originTile.setColor(DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal());
        
        if (!move.isCapture)
        {
            if (originTile.color == DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal() ||
                    originTile.color == DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal())
                board.movePawn(destinationTile);
            else
                board.moveQueen(destinationTile);
        }
        else // Forced to play = capture
            board.Capture(destinationTile);
        
        
    }
    
    private void undoMove(Move move)
    {
        board.movesWithoutProgress = move.movesWithoutProgress;
        int deadTileColor;
        board.turnRedSquaresOff();
        if (!board.isOnStreak && move.wasOnStreak)
        {
            
            if (move.turn)
                deadTileColor = DamkaTile.TilePawn.DEAD_WHITE.ordinal();
            else
                deadTileColor = DamkaTile.TilePawn.DEAD_BLACK.ordinal();
            
            Stack<Move> temp = new Stack<>();
            while(!movesStack.isEmpty() && movesStack.peek().isCapture && movesStack.peek().turn)
            {
                Move prevMove = movesStack.pop();
                temp.push(prevMove);
                board.tiles[prevMove.capturedRow][prevMove.capturedCol].setColor(deadTileColor);
            }
            
            while (!temp.isEmpty())
                movesStack.push(temp.pop());
        }
        DamkaTile originTile = board.tiles[move.fromRow][move.fromCol];
        DamkaTile destinationTile = board.tiles[move.toRow][move.toCol];
        int destColor = destinationTile.color;
        
        if (!board.isOnStreak && move.wasOnStreak)
            destinationTile.setColor(DamkaTile.TilePawn.RED.ordinal());
        else
            destinationTile.setColor(DamkaTile.TilePawn.BLACK.ordinal());
        
        
        board.isOnStreak = move.wasOnStreak;
        board.isForced = move.isCapture;
        board.turn = move.turn;
        
        if (!move.wasOnStreak &&
            (destColor == DamkaTile.TilePawn.WHITE_PAWN_CHOSEN.ordinal() ||
            destColor == DamkaTile.TilePawn.BLACK_PAWN_CHOSEN.ordinal() ||
            destColor == DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal() ||
            destColor == DamkaTile.TilePawn.BLACK_QUEEN_CHOSEN.ordinal()))
            destColor -= 2;
        if (move.wasOnStreak &&
            (destColor == DamkaTile.TilePawn.WHITE_PAWN.ordinal() ||
            destColor == DamkaTile.TilePawn.BLACK_PAWN.ordinal() ||
            destColor == DamkaTile.TilePawn.WHITE_QUEEN.ordinal() ||
            destColor == DamkaTile.TilePawn.BLACK_QUEEN.ordinal()))
            destColor += 2;
        originTile.setColor(destColor);
        
        if (move.wasPremotion)
        {
            if (originTile.color == DamkaTile.TilePawn.WHITE_QUEEN.ordinal() ||
                originTile.color == DamkaTile.TilePawn.WHITE_QUEEN_CHOSEN.ordinal())
            {
                board.whiteQueens--;
                originTile.setColor(DamkaTile.TilePawn.WHITE_PAWN.ordinal());
            }
            else
            {
                board.blackQueens--;
                originTile.setColor(DamkaTile.TilePawn.BLACK_PAWN.ordinal());
            }
        }
    
        if (move.isCapture)
        {
            board.tiles[move.capturedRow][move.capturedCol].setColor(move.capturedColor);
            if (move.turn)
            {
                board.whitePawnsLeft++;
                if (move.capturedColor == DamkaTile.TilePawn.WHITE_QUEEN.ordinal())
                    board.whiteQueens++;
            }
                
            else
            {
                board.blackPawnsLeft++;
                if (move.capturedColor == DamkaTile.TilePawn.BLACK_QUEEN.ordinal())
                    board.blackQueens++;
            }
        }
        
        
        if (move.wasOnStreak)
        {
            board.isPawnChosen = true;
            board.chosenPawnRow = move.fromRow;
            board.chosenPawnCol = move.fromCol;
            
            if (originTile.color == DamkaTile.TilePawn.BLACK_PAWN.ordinal() ||
                originTile.color == DamkaTile.TilePawn.WHITE_PAWN.ordinal())
                board.turnRedPawnSquaresOn();
            else
                board.turnRedQueenSquaresOn();
        }
        else
            board.isPawnChosen = false;
    }
    
    // Evaluate position in static manner.
    private float evaluatePosition()
    {
        boolean isStartGame = board.whitePawnsLeft + board.blackPawnsLeft > board.PAWN_ROWS*board.LENGTH/3;
        
        if (board.whitePawnsLeft == 0)
            return MAX_POS_VAL;
        if (board.blackPawnsLeft == 0)
            return MIN_POS_VAL;
        
        
        float posVal = board.blackPawnsLeft - board.whitePawnsLeft;
        posVal += 1.1*board.blackQueens;
        posVal -= 1.1*board.whiteQueens;
        
        // A queen has a higher value before the endgame
        if (isStartGame)
        {
            posVal += 0.5*board.blackQueens;
            posVal -= 0.5*board.whiteQueens;
        }
        
        if (board.turn)
        {
            posVal += 0.25;
            
            // if the position is forced than a capture exists, usually good
            if (board.isForced)
                posVal+= 0.35;
            // check for a loss
            else
            {
                if (board.whitePawnsLeft <= MAX_PAWNS_IN_STALEMATE &&
                !board.canPlay(DamkaTile.TilePawn.BLACK_PAWN.ordinal(),
                         DamkaTile.TilePawn.BLACK_QUEEN.ordinal()))
                    return MIN_POS_VAL;
            }
        }
        else
        {
            posVal -= 0.25;
            // if the position is forced than a capture exists, usually good (for white)
            if (board.isForced)
                posVal -= 0.35;
            // check for a loss (for white)
            else
            {
                if (board.whitePawnsLeft <= MAX_PAWNS_IN_STALEMATE &&
                !board.canPlay(DamkaTile.TilePawn.WHITE_PAWN.ordinal(),
                         DamkaTile.TilePawn.WHITE_QUEEN.ordinal()))
                    return MAX_POS_VAL;
            }
        }
        
        // check for black pawns in white territory
        for (int i = board.LENGTH - 3; i < board.LENGTH - 1; i++) {
            for (int j = 1 - i % 2; j < board.LENGTH; j += 2) {
                if (board.tiles[i][j].color == DamkaTile.TilePawn.BLACK_PAWN.ordinal())
                {
                    // bad in start of the game
                    if (isStartGame)
                        posVal -= 0.15;
                    // good in the end of the game
                    else
                        posVal += 0.25;
                }
            }
        }

        // check for white pawns in black territory
        for (int i = 1; i < 3; i++) {
            for (int j = 1 - (i % 2); j < board.LENGTH; j += 2) {
                if (board.tiles[i][j].color == DamkaTile.TilePawn.WHITE_PAWN.ordinal())
                {
                    // bad in start of the game (for white)
                    if (isStartGame)
                        posVal += 0.15;
                    // good in the end of the game (for white)
                    else
                        posVal -= 0.25;
                }
            }
        }
        
        // check for pawns in back-most and front-most rows at start-game
        if (isStartGame)
        {
            for (int i = 1; i < board.LENGTH; i+= 2)
                if (board.tiles[0][i].color == DamkaTile.TilePawn.BLACK_PAWN.ordinal())
                    posVal += 0.2;
            
            for (int i = board.LENGTH % 2; i < board.LENGTH; i+= 2)
                if (board.tiles[board.LENGTH - 1][i].color == DamkaTile.TilePawn.WHITE_PAWN.ordinal())
                    posVal -= 0.2;
        }
        
        return posVal;
    }
    
    /**
     * Find the best move in the position for the computer (black)
     * And make it
     */
    public void play()
    {
        moveToPlay = null;
        board.isComputerPlaying = true;
        //int movesWithoutProgress = board.movesWithoutProgress;
        findBestMove();
        board.isComputerPlaying = false;
        //board.movesWithoutProgress = movesWithoutProgress;
        if (moveToPlay != null)
            makeMove(moveToPlay);
        else
            board.endGame("User Wins!!!");
        
    }
}