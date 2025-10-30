package bananadash;

import java.awt.*;
import javax.swing.ImageIcon;

public class Enemy {
    //Movement+Physics
    public int x, y;
    private int startX;
    private int width, height;
    private float range, speed;
    private boolean movingRight = true;
    
    //Status
    private boolean isDead = false;
    private boolean isAlive = true;
    private long deathTime = 0;
    
    //Animation
    private Image walkImg;
    private Image deadImg;

    public Enemy(int x, int y, float range, float speed, int width, int height) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.range = range;
        this.speed = speed;
        this.width = width;
        this.height = height;

        walkImg = new ImageIcon(getClass().getResource("/Asset/Enemy/Enemy/EnemyWalk1.png")).getImage();
        deadImg = new ImageIcon(getClass().getResource("/Asset/Enemy/Enemy/EnemyDead3.png")).getImage();
    }

    public void update() {
        if (!isAlive || isDead) return;
        if(movingRight){
            x += speed;
            if (x > startX + range){
                movingRight = false;
            }
        }
        else{
            x -= speed;
            if (x < startX - range){
                movingRight = true;
            }
        }
    }

    //status
    public void die() {
        if (!isDead && isAlive) {
            isDead = true;
            isAlive = false;
        }
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isAlive() {
        return isAlive;
    }
    
    //draw
    public void draw(Graphics g, int camX, int camY) {
        if (!isAlive && !isDead) return;

        int drawX = x - camX;
        int drawY = y - camY - height;

        Image current = isDead ? deadImg : walkImg;
        g.drawImage(current, drawX, drawY, width, height, null);

        if (isDead && System.currentTimeMillis() - deathTime > 800) {
            isDead = false;
            isAlive = false;
            width = 0;
            height = 0;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y - height, width, height);
    }

    public void reset() {
        isAlive = true;
        isDead = false;
        x = startX;
    }
}
