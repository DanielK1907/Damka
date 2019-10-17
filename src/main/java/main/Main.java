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

    private static final Color JUNGLE_GREEN = new Color(50, 155, 105);

    // recursion depths
    private static final int EASY = 4;
    private static final int MEDIUM = 6;
    private static final int HARD = 8;

    public static void main(String[] args) {

        //<editor-fold defaultstate="collapsed" desc="Settings JFrame">


        // Instantiate the comboboxes and radio buttons
        JFrame settingsFrame = new JFrame("Settings");
        JComboBox boardLengthComboBox = new JComboBox();
        JComboBox pawnRowsComboBox = new JComboBox();
        ButtonGroup difficulties = new ButtonGroup();
        JRadioButton easy = new JRadioButton("Easy");
        JRadioButton medium = new JRadioButton("Medium");
        JRadioButton hard = new JRadioButton("Hard", true);
        boardLengthComboBox.setBounds(80, 90, 60, 50);
        pawnRowsComboBox.setBounds(255, 90, 60, 50);
        easy.setBounds(120, 140, 140, 80);
        medium.setBounds(120, 200, 140, 80);
        hard.setBounds(120, 260, 140, 80);
        easy.setBackground(JUNGLE_GREEN);
        medium.setBackground(JUNGLE_GREEN);
        hard.setBackground(JUNGLE_GREEN);
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
                Damka d = getDamkaBoard(boardLengthComboBox, pawnRowsComboBox, easy, medium);
                d.isComputer = false;
                d.start();
                settingsFrame.dispose();
            }

        });

        JButton computerB = new JButton("Human Vs. Computer");
        computerB.setBounds(50, 40, 300, 20);
        computerB.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Damka d = getDamkaBoard(boardLengthComboBox, pawnRowsComboBox, easy, medium);
                d.isComputer = true;
                d.start();
                settingsFrame.dispose();
            }

        });

        for (int i = 4; i <= 12; i++)
            boardLengthComboBox.addItem(i);
        boardLengthComboBox.setSelectedItem(null);


        boardLengthComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                pawnRowsComboBox.removeAllItems();
                for (int i = 1; i < ((int)boardLengthComboBox.getSelectedItem()+1)/2; i++)
                    pawnRowsComboBox.addItem(i);
                pawnRowsComboBox.setSelectedItem(null);
            }
        });


        settingsFrame.add(boardLengthComboBox);
        settingsFrame.add(pawnRowsComboBox);
        settingsFrame.add(easy);
        settingsFrame.add(medium);
        settingsFrame.add(hard);
        settingsFrame.add(lengthLabel);
        settingsFrame.add(pawnRowsLabel);
        settingsFrame.setSize(400, 360);
        settingsFrame.add(humanB);
        settingsFrame.add(computerB);
        settingsFrame.setLayout(null);
        settingsFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        settingsFrame.setResizable(false);
        settingsFrame.setLocationRelativeTo(null); // Passing null centers the form!
        settingsFrame.setVisible(true);
        settingsFrame.getContentPane().setBackground(JUNGLE_GREEN);
//</editor-fold>

    }

    private static Damka getDamkaBoard(JComboBox boardLengthComboBox, JComboBox pawnRowsComboBox, JRadioButton easy, JRadioButton medium) {
        Damka d;
        if (boardLengthComboBox.getSelectedItem() != null && pawnRowsComboBox.getSelectedItem() != null)
            d = new Damka((int)boardLengthComboBox.getSelectedItem(), (int)pawnRowsComboBox.getSelectedItem());
        else // Go for the classic variation
            d = new Damka(8, 3);
        if (easy.isSelected())
            Computer.DEPTH_MAX = EASY;
        else if (medium.isSelected())
            Computer.DEPTH_MAX = MEDIUM;
        else
            Computer.DEPTH_MAX = HARD;

        Computer.comp.board = d;
        return d;
    }
}