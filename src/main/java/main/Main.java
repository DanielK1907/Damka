package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static javax.swing.JFrame.EXIT_ON_CLOSE;


/**
 * "Abstract strategy game"
 * @author Daniel Kanevsky
 */
public class Main {

    private static final Color MAGIC = new Color(50, 155, 105);
    private static final int EASY = 4;
    private static final int MEDIUM = 6;
    private static final int HARD = 8;

    public static void main(String[] args) {

        //<editor-fold defaultstate="collapsed" desc="Settings JFrame">


        // Instantiate the comboboxes and radio buttons
        JFrame f = new JFrame("Settings");
        JComboBox lengthBox = new JComboBox();
        JComboBox pawnRowsBox = new JComboBox();
        ButtonGroup difficulties = new ButtonGroup();
        JRadioButton easy = new JRadioButton("Easy");
        JRadioButton medium = new JRadioButton("Medium");
        JRadioButton hard = new JRadioButton("Hard", true);
        lengthBox.setBounds(80, 90, 60, 50);
        pawnRowsBox.setBounds(255, 90, 60, 50);
        easy.setBounds(120, 140, 140, 80);
        easy.setBackground(Color.yellow);
        medium.setBounds(120, 200, 140, 80);
        hard.setBounds(120, 260, 140, 80);
        easy.setBackground(MAGIC);
        medium.setBackground(MAGIC);
        hard.setBackground(MAGIC);
        difficulties.add(easy);
        difficulties.add(medium);
        difficulties.add(hard);

        // Instantiate the labels
        JLabel lengthLabel = new JLabel("Board Length :");
        JLabel pawnRowsLabel = new JLabel("Pawns Rows :");
        lengthLabel.setBounds(70, 60, 130, 30);
        pawnRowsLabel.setBounds(245, 60, 120, 30);

        // instantiate the buttons
        JButton humanB = new JButton("Human Vs. Human");
        humanB.setBounds(50, 10, 300, 20);
        humanB.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Damka d;
                if (lengthBox.getSelectedItem() != null && pawnRowsBox.getSelectedItem() != null)
                    d = new Damka((int)lengthBox.getSelectedItem(), (int)pawnRowsBox.getSelectedItem());
                else // Go for the classic variation
                    d = new Damka(8, 3);
                if (easy.isSelected())
                    Computer.DEPTH_MAX = EASY;
                else if (medium.isSelected())
                    Computer.DEPTH_MAX = MEDIUM;
                else
                    Computer.DEPTH_MAX = HARD;

                Computer.comp.board = d;
                d.isComputer = false;
                d.start();
                f.dispose();
            }

        });

        JButton computerB = new JButton("Human Vs. Computer");
        computerB.setBounds(50, 40, 300, 20);
        computerB.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Damka d;
                if (lengthBox.getSelectedItem() != null & pawnRowsBox.getSelectedItem() != null)
                    d = new Damka((int)lengthBox.getSelectedItem(), (int)pawnRowsBox.getSelectedItem());
                else // Go for the classic variation
                    d = new Damka(8, 3);
                if (easy.isSelected())
                    Computer.DEPTH_MAX = EASY;
                else if (medium.isSelected())
                    Computer.DEPTH_MAX = MEDIUM;
                else
                    Computer.DEPTH_MAX = HARD;

                Computer.comp.board = d;
                d.isComputer = true;
                d.start();
                f.dispose();
            }

        });

        for (int i = 4; i <= 12; i++)
            lengthBox.addItem(i);
        lengthBox.setSelectedItem(null);


        lengthBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                pawnRowsBox.removeAllItems();
                for (int i = 1; i < ((int)lengthBox.getSelectedItem()+1)/2; i++)
                    pawnRowsBox.addItem(i);
                pawnRowsBox.setSelectedItem(null);
            }
        });


        f.add(lengthBox);
        f.add(pawnRowsBox);
        f.add(easy);
        f.add(medium);
        f.add(hard);
        f.add(lengthLabel);
        f.add(pawnRowsLabel);
        f.setSize(400, 360);
        f.add(humanB);
        f.add(computerB);
        f.setLayout(null);
        f.setDefaultCloseOperation(EXIT_ON_CLOSE);
        f.setResizable(false);
        f.setLocationRelativeTo(null); // Passing null centers the form!
        f.setVisible(true);
        f.getContentPane().setBackground(MAGIC);
//</editor-fold>

    }
}