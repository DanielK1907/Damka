package main;

import java.util.ArrayList;
import java.util.Stack;

/**
 * AI for playing Shashki (Russian Checkers)
 * @author Daniel Kanevsky
 */
class Computer {
    
    private Computer(){}
    
    static class Move
    {
        int fromRow; // Row index of origin tile
        int fromCol; // Col index of origin tile
        int toRow; // Row index of destination tile
        int toCol; // Col index of destination tile
        int movesWithoutProgress; // Moves without progress before this move was played
        boolean isCapture; // Is this move a capture?
        TileColor capturedColor; // Color of captured piece
        int capturedRow; // Row of captured piece
        int capturedCol; // Col of captured piece
        boolean wasOnStreak; // Was this move made as the part of streak
        boolean wasPremotion; // Was this a premotion to a queen
        boolean turn; // Whose turn is it? false - white, true - black
        
        Move(int fromRow, int fromCol, int toRow, int toCol, int movesWithoutProgress,
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
    
    static int DEPTH_MAX;
    static final float MIN_POS_VAL = -10000;
    static final float MAX_POS_VAL =  10000;
    
    // Singleton instance
    static Computer comp = new Computer();

    Damka board;
    private Move moveToPlay; // The move which will be played
    
    Stack<Move> movesStack = new Stack<>();
    
    // Generate all possible moves in the current position
    private ArrayList<Move> generateMoves ()
    {
        ArrayList<Move> moves = new ArrayList<>();
        TileColor playing1, playing2;
        boolean isCapture = board.isForced;
        
        // If the board is on streak, generate the moves with the pawn/queen on streak.
        if (board.isOnStreak)
        {
            for (int i = 0; i < board.LENGTH; i++)
            {
                for (int j = 1 - i%2; j < board.LENGTH; j+= 2)
                {
                    if (board.tiles[i][j].color == TileColor.RED)
                    {
                        moves.add(new Move(board.chosenPawnRow, board.chosenPawnCol, i, j, 0, true, true, board.turn));
                    }
                }
            }
            
            return moves;
        }
        
        if (board.turn)
        {
            playing1 = TileColor.BLACK_PAWN;
            playing2 = TileColor.BLACK_QUEEN;
        }
        else
        {
            playing1 = TileColor.WHITE_PAWN;
            playing2 = TileColor.WHITE_QUEEN;
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
                            board.tiles[i][j].setColor(TileColor.BLACK_PAWN_CHOSEN);
                        else
                            board.tiles[i][j].setColor(TileColor.WHITE_PAWN_CHOSEN);
                        board.turnRedPawnSquaresOn();
                        if (board.turn)
                            board.tiles[i][j].setColor(TileColor.BLACK_PAWN);
                        else
                            board.tiles[i][j].setColor(TileColor.WHITE_PAWN);
                    }
                    else
                    {
                        if (board.turn)
                            board.tiles[i][j].setColor(TileColor.BLACK_QUEEN_CHOSEN);
                        else
                            board.tiles[i][j].setColor(TileColor.WHITE_QUEEN_CHOSEN);
                        board.turnRedQueenSquaresOn();
                        if (board.turn)
                            board.tiles[i][j].setColor(TileColor.BLACK_QUEEN);
                        else
                            board.tiles[i][j].setColor(TileColor.WHITE_QUEEN);
                    }
                        
                    // Add the moves for the current tile
                    for (int k = 0; k < board.LENGTH; k++)
                    {
                        for (int l = 1 - k%2; l < board.LENGTH; l += 2)
                        {
                            if (board.tiles[k][l].color == TileColor.RED)
                            {
                                board.tiles[k][l].setColor(TileColor.BLACK);
                                moves.add(new Move(i, j, k, l, board.movesWithoutProgress, isCapture, false, board.turn));
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
    float miniMaxAlphaBeta(int currentDepth, boolean Max, float alpha, float beta)
    {
        if (board.movesWithoutProgress == Damka.MOVES_FOR_DRAW)
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
            // Alpha-Beta Purning!!!
            if (alpha >= beta)
               break;
        }
        
        return positionValue;
    }

    private void makeMove(Move move)
    {
        board.chosenPawnRow = move.fromRow;
        board.chosenPawnCol = move.fromCol;
        DamkaPawn originTile = board.tiles[move.fromRow][move.fromCol];
        DamkaPawn destinationTile = board.tiles[move.toRow][move.toCol];
        
        // set the pawn/queen as chosen if isn't already
        if (originTile.color == TileColor.WHITE_PAWN)
            originTile.setColor(TileColor.WHITE_PAWN_CHOSEN);
        else if (originTile.color == TileColor.BLACK_PAWN)
            originTile.setColor(TileColor.BLACK_PAWN_CHOSEN);
        else if (originTile.color == TileColor.WHITE_QUEEN)
            originTile.setColor(TileColor.WHITE_QUEEN_CHOSEN);
        else if (originTile.color == TileColor.BLACK_QUEEN)
            originTile.setColor(TileColor.BLACK_QUEEN_CHOSEN);
        
        if (!move.isCapture)
        {
            if (originTile.color == TileColor.WHITE_PAWN_CHOSEN ||
                    originTile.color == TileColor.BLACK_PAWN_CHOSEN)
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
        TileColor deadTileColor;
        board.turnRedSquaresOff();
        if (!board.isOnStreak && move.wasOnStreak)
        {
            
            if (move.turn)
                deadTileColor = TileColor.DEAD_WHITE;
            else
                deadTileColor = TileColor.DEAD_BLACK;
            
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
        DamkaPawn originTile = board.tiles[move.fromRow][move.fromCol];
        DamkaPawn destinationTile = board.tiles[move.toRow][move.toCol];
        int destColor = destinationTile.color.ordinal();
        
        if (!board.isOnStreak && move.wasOnStreak)
            destinationTile.setColor(TileColor.RED);
        else
            destinationTile.setColor(TileColor.BLACK);
        
        
        board.isOnStreak = move.wasOnStreak;
        board.isForced = move.isCapture;
        board.turn = move.turn;
        
        if (!move.wasOnStreak &&
            (destColor == TileColor.WHITE_PAWN_CHOSEN.ordinal() ||
            destColor == TileColor.BLACK_PAWN_CHOSEN.ordinal() ||
            destColor == TileColor.WHITE_QUEEN_CHOSEN.ordinal() ||
            destColor == TileColor.BLACK_QUEEN_CHOSEN.ordinal()))
            destColor -= 2;
        if (move.wasOnStreak &&
            (destColor == TileColor.WHITE_PAWN.ordinal() ||
            destColor == TileColor.BLACK_PAWN.ordinal() ||
            destColor == TileColor.WHITE_QUEEN.ordinal() ||
            destColor == TileColor.BLACK_QUEEN.ordinal()))
            destColor += 2;
        originTile.setColor(TileColor.values()[destColor]);
        
        if (move.wasPremotion)
        {
            if (originTile.color == TileColor.WHITE_QUEEN ||
                originTile.color == TileColor.WHITE_QUEEN_CHOSEN)
            {
                board.whiteQueens--;
                originTile.setColor(TileColor.WHITE_PAWN);
            }
            else
            {
                board.blackQueens--;
                originTile.setColor(TileColor.BLACK_PAWN);
            }
        }
    
        if (move.isCapture)
        {
            board.tiles[move.capturedRow][move.capturedCol].setColor(move.capturedColor);
            if (move.turn)
            {
                board.whitePawnsLeft++;
                if (move.capturedColor == TileColor.WHITE_QUEEN)
                    board.whiteQueens++;
            }
                
            else
            {
                board.blackPawnsLeft++;
                if (move.capturedColor == TileColor.BLACK_QUEEN)
                    board.blackQueens++;
            }
        }
        
        
        if (move.wasOnStreak)
        {
            board.isPawnChosen = true;
            board.chosenPawnRow = move.fromRow;
            board.chosenPawnCol = move.fromCol;
            
            if (originTile.color == TileColor.BLACK_PAWN ||
                originTile.color == TileColor.WHITE_PAWN)
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
                if (board.whitePawnsLeft <= Damka.MAX_PAWNS_IN_STALEMATE &&
                !board.canPlay(TileColor.BLACK_PAWN,
                         TileColor.BLACK_QUEEN))
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
                if (board.whitePawnsLeft <= Damka.MAX_PAWNS_IN_STALEMATE &&
                !board.canPlay(TileColor.WHITE_PAWN,
                         TileColor.WHITE_QUEEN))
                    return MAX_POS_VAL;
            }
        }
        
        // check for black pawns in white territory
        for (int i = board.LENGTH - 3; i < board.LENGTH - 1; i++) {
            for (int j = 1 - i % 2; j < board.LENGTH; j += 2) {
                if (board.tiles[i][j].color == TileColor.BLACK_PAWN)
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
                if (board.tiles[i][j].color == TileColor.WHITE_PAWN)
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
                if (board.tiles[0][i].color == TileColor.BLACK_PAWN)
                    posVal += 0.2;
            
            for (int i = board.LENGTH % 2; i < board.LENGTH; i+= 2)
                if (board.tiles[board.LENGTH - 1][i].color == TileColor.WHITE_PAWN)
                    posVal -= 0.2;
        }
        
        return posVal;
    }
    
    /**
     * Find the best move in the position for the computer (black)
     * And make it
     */
    void play()
    {
        moveToPlay = null;
        board.isComputerPlaying = true;
        //long start = System.nanoTime();
        findBestMove();
        //long result = System.nanoTime() - start;
        //board.setTitle(Long.toString(result));
        board.isComputerPlaying = false;
        if (moveToPlay != null)
            makeMove(moveToPlay);
        else
            board.endGame("User Wins!!!");
        
    }
}