package bananadash;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyHandler extends KeyAdapter {
    public boolean left, right, jump;
    private final GamePanel game;
    public KeyHandler(GamePanel game) {
        this.game = game;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (game.getGameState() == GamePanel.GameState.TITLE && code == KeyEvent.VK_ENTER) {
            game.startGame();
        } 
        else if ((game.getGameState() == GamePanel.GameState.GAME_OVER || game.getGameState() == GamePanel.GameState.GAME_END) 
                && code == KeyEvent.VK_ENTER){
                game.returnToTitle();
        }
        if (code == KeyEvent.VK_A) left = true;
        if (code == KeyEvent.VK_D) right = true;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE) jump = true;
        game.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A) left = false;
        if (code == KeyEvent.VK_D) right = false;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE) jump = false;
    }
}
