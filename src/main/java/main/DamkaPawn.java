package main;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * @author Daniel Kanevsky
 * @
 * A tile In the Damka Board
 */
public class DamkaPawn extends JButton implements ActionListener{
    private static final int TOTAL_ICONS = 13;
    private final Damka board;
    public final int row;
    public final int col;
    public TileColor color;
    
    public static Dimension buttonSize= new Dimension(Damka.TILE_SIZE, Damka.TILE_SIZE);
    public static ImageIcon[] Images = new ImageIcon[TOTAL_ICONS];
    
    /**
     * Initialize the images array with images.
     */
    public  void initialize()
    {
        //<editor-fold defaultstate="collapsed" desc="load content">
        Images[TileColor.WHITE.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/white.png"));
        Images[TileColor.BLACK.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/black.png"));
        Images[TileColor.WHITE_PAWN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/white pawn.png"));
        Images[TileColor.BLACK_PAWN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/black pawn.png"));
        Images[TileColor.WHITE_PAWN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/white pawn chosen.png"));
        Images[TileColor.BLACK_PAWN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/black pawn chosen.png"));
        Images[TileColor.RED.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/red.png"));
        Images[TileColor.WHITE_QUEEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/white queen.png"));
        Images[TileColor.BLACK_QUEEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/black queen.png"));
        Images[TileColor.WHITE_QUEEN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/white queen chosen.png"));
        Images[TileColor.BLACK_QUEEN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/black queen chosen.png"));
        Images[TileColor.DEAD_WHITE.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/dead white.png"));
        Images[TileColor.DEAD_BLACK.ordinal()] =
                new ImageIcon(this.getClass().getResource("/Images/dead black.png"));
//</editor-fold>
        
        Image buffer;
        
        // Strech the image across the button
        for (int i = 0; i < TOTAL_ICONS; i++)
        {
            buffer = Images[i].getImage();
            buffer = buffer.getScaledInstance(Damka.TILE_SIZE, Damka.TILE_SIZE, Image.SCALE_SMOOTH);
            Images[i].setImage(buffer);
        }
        
    }
    
    public DamkaPawn(){row = -1; col = -1; board = null;} // Technical only
    public DamkaPawn(int row, int col, Damka board)
    {
        setPreferredSize(buttonSize);
        this.board = board;
        this.row = row;
        this.col = col;
        
        addActionListener(this);
    }
      
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        if (color == TileColor.WHITE ||
            color == TileColor.BLACK ||
            color == TileColor.DEAD_BLACK ||
            color == TileColor.DEAD_WHITE)
            return;
        
        if (color == TileColor.WHITE_PAWN ||
            color == TileColor.BLACK_PAWN)
        {
            if (board.isOnStreak)
                return;
            if (board.isPawnChosen)
            {
                board.turnPawnOff();
            }
            board.turnPawnOn(this);
            return;
        }
         
        if (color == TileColor.WHITE_PAWN_CHOSEN ||
            color == TileColor.BLACK_PAWN_CHOSEN)
        {
            if (board.isOnStreak)
                return;
            board.turnPawnOff();
            return;
        }
        
        if (color == TileColor.WHITE_QUEEN_CHOSEN ||
            color == TileColor.BLACK_QUEEN_CHOSEN)
        {
            if (board.isOnStreak)
                return;
            board.turnQueenOff();
            return;
        }
        
        if (color == TileColor.RED)
        {
            if (!board.isForced)
            {
                TileColor tileColor = board.tiles[board.chosenPawnRow][board.chosenPawnCol].color;
                if (tileColor == TileColor.WHITE_QUEEN_CHOSEN ||
                    tileColor == TileColor.BLACK_QUEEN_CHOSEN)
                {
                    board.moveQueen(this);
                    return;
                }
                
                board.movePawn(this);
                return;
            }
            
            board.Capture(this);
            return;
        }
        
        // if reached here: color must be black or white queen
        if (board.isOnStreak)
            return;
        
        if (board.isPawnChosen)
            board.turnPawnOff();
        
        board.turnQueenOn(this);
    }
    
    
    public void setColor(TileColor color)
    {
        this.color = color;
        setIcon(Images[color.ordinal()]);
    }
}
