/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myproject;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import static javax.swing.JFrame.EXIT_ON_CLOSE;


/**
 * "Abstract strategy game"
 * @author Daniel Kanevsky
 */
public class MyProject {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //<editor-fold defaultstate="collapsed" desc="Settings JFrame">
        
        // Instantiate the comboboxes
        JFrame f = new JFrame("Settings");
        JComboBox lengthBox = new JComboBox();
        JComboBox pawnRowsBox = new JComboBox();
        lengthBox.setBounds(80, 90, 60, 50);
        pawnRowsBox.setBounds(255, 90, 60, 50);      
        
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
                if (lengthBox.getSelectedItem() != null & pawnRowsBox.getSelectedItem() != null)
                    d = new Damka((int)lengthBox.getSelectedItem(), (int)pawnRowsBox.getSelectedItem());
                else // Go for the classic variation
                    d = new Damka(8, 3);
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
                Computer.comp.board = d;
                d.isComputer = true;
                d.start();
                f.dispose();
            }
            
        });
        
        for (int i = 4; i <= 12; i++)
            lengthBox.addItem(i);
        
        lengthBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                pawnRowsBox.removeAllItems();
                for (int i = 1; i < ((int)lengthBox.getSelectedItem()+1)/2; i++)
                    pawnRowsBox.addItem(i);
            }
        });
        
        
        f.add(lengthBox);
        f.add(pawnRowsBox);
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
        f.getContentPane().setBackground(new Color(50, 155, 105));
//</editor-fold>

        
    }
}