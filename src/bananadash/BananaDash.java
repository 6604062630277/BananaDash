/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package bananadash;

import javax.swing.*;
import java.awt.*;

public class BananaDash {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Banana Dash");
        GamePanel panel = new GamePanel();   
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
