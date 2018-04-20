/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myproject;

/**
 *
 * @author Daniel Kanevsky
 */
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class DamkaTile extends JButton implements ActionListener{

    public enum TilePawn
    {
        WHITE,
        BLACK,
        WHITE_PAWN,
        BLACK_PAWN,
        WHITE_PAWN_CHOSEN,
        BLACK_PAWN_CHOSEN,
        RED,
        WHITE_QUEEN,
        BLACK_QUEEN,
        WHITE_QUEEN_CHOSEN,
        BLACK_QUEEN_CHOSEN,
        DEAD_WHITE,
        DEAD_BLACK
    }
    
    
    private static final int TOTAL_ICONS = 13;
    private final Damka board;
    public final int row;
    public final int col;
    public int color;
    
    public static Dimension buttonSize= new Dimension(Damka.TILE_SIZE, Damka.TILE_SIZE);
    public static ImageIcon[] Images = new ImageIcon[TOTAL_ICONS];
    
    /**
     * Initialize the images array with images.
     */
    public  void initialize()
    {
        //<editor-fold defaultstate="collapsed" desc="load content">
        Images[TilePawn.WHITE.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/white.png"));
        Images[TilePawn.BLACK.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/black.png"));
        Images[TilePawn.WHITE_PAWN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/white pawn.png"));
        Images[TilePawn.BLACK_PAWN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/black pawn.png"));
        Images[TilePawn.WHITE_PAWN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/white pawn chosen.png"));
        Images[TilePawn.BLACK_PAWN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/black pawn chosen.png"));
        Images[TilePawn.RED.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/red.png"));
        Images[TilePawn.WHITE_QUEEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/white queen.png"));
        Images[TilePawn.BLACK_QUEEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/black queen.png"));
        Images[TilePawn.WHITE_QUEEN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/white queen chosen.png"));
        Images[TilePawn.BLACK_QUEEN_CHOSEN.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/black queen chosen.png"));
        Images[TilePawn.DEAD_WHITE.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/dead white.png"));
        Images[TilePawn.DEAD_BLACK.ordinal()] =
                new ImageIcon(this.getClass().getResource("Images/dead black.png"));
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
    
    public DamkaTile(){row = -1; col = -1; board = null;} // Technical only
    public DamkaTile(int row, int col, Damka board)
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
        if (color == TilePawn.WHITE.ordinal() ||
            color == TilePawn.BLACK.ordinal() ||
            color == TilePawn.DEAD_BLACK.ordinal() ||
            color == TilePawn.DEAD_WHITE.ordinal())
            return;
        
        if (color == TilePawn.WHITE_PAWN.ordinal() ||
            color == TilePawn.BLACK_PAWN.ordinal())
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
        
        if (color == TilePawn.WHITE_PAWN_CHOSEN.ordinal() ||
            color == TilePawn.BLACK_PAWN_CHOSEN.ordinal())
        {
            if (board.isOnStreak)
                return;
            board.turnPawnOff();
            return;
        }
        
        if (color == TilePawn.WHITE_QUEEN_CHOSEN.ordinal() ||
            color == TilePawn.BLACK_QUEEN_CHOSEN.ordinal())
        {
            if (board.isOnStreak)
                return;
            board.turnQueenOff();
            return;
        }
        
        if (color == TilePawn.RED.ordinal())
        {
            if (!board.isForced)
            {
                int tileColor = board.tiles[board.chosenPawnRow][board.chosenPawnCol].color;
                if (tileColor == TilePawn.WHITE_QUEEN_CHOSEN.ordinal() ||
                    tileColor == TilePawn.BLACK_QUEEN_CHOSEN.ordinal())
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
    
    
    public void setColor(int color)
    {
        this.color = color;
        setIcon(Images[color]);
    }
}
