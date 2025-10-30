package bananadash;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class player {

    //Animation
    private Image[] idle;
    private Image[] walk;

    //Movement+Physics
    public int x, y;
    public int width = 32, height = 32;
    public int speed = 3;
    public double velY = 0;
    public int frame = 0, tick = 0;
    public boolean jumping = false,moving = false,toRight = true,onGround = false;
    private boolean isDead = false;
    private int iFrame = 0;
    
    //HP
    private final int maxHearts = 5;
    private int hearts = maxHearts;  
    public int getHearts(){
        return hearts;
    }
    public int getMaxHearts(){
        return maxHearts;
    }

    //Map
    private TiledMap map;

    //Banana
    private java.util.List<Point> bananas = new ArrayList<>();
    private int bananasCollected = 0; 

    public player(int x, int y, TiledMap map) {
        this.x = x;
        this.y = y;
        this.map = map;
        idle = new Image[]{
            new ImageIcon(getClass().getResource("/Asset/Player/MonkeyIdle1.png")).getImage(),
            new ImageIcon(getClass().getResource("/Asset/Player/MonkeyIdle2.png")).getImage(),
            new ImageIcon(getClass().getResource("/Asset/Player/MonkeyIdle3.png")).getImage()
        };
        walk = new Image[]{
            new ImageIcon(getClass().getResource("/Asset/Player/MonkeyWalk1.png")).getImage(),
            new ImageIcon(getClass().getResource("/Asset/Player/MonkeyWalk2.png")).getImage()
        };
    }

    //Update Logic
    public void update(boolean left, boolean right, boolean jump) {
        if(iFrame > 0){
            iFrame--;    
        }
        moving = false;
        if(left){
            x -= speed; moving = true; toRight = false;
        }
        if (right){
            x += speed; moving = true; toRight = true;
        }

        if (jump && onGround) {
            velY = -8;
            jumping = true;
            onGround = false;
        }
        velY += 0.5;
        y += velY;

        checkCollision();
        checkBananaCollision();
        clampToWorld();
        
        //frame
        tick++;
        if (tick > 10) {
            tick = 0;
            frame = (frame+1)%(moving ? walk.length : idle.length);
        }
    }

    //Collision
    private void checkCollision() {
        if (map == null) return;
        int tileSize = map.getTileSize();
        onGround = false;

        int leftTile = (x+4)/tileSize;
        int rightTile = (x+width-4)/tileSize;
        int topTile = y/tileSize;
        int bottomTile = (y+height-1)/tileSize;
        if (map.getMap(bottomTile,leftTile) > 0 || map.getMap(bottomTile,rightTile) > 0) {
            y = bottomTile * tileSize - height;
            velY = 0;
            onGround = true;
            jumping = false;
        }
        if (map.getMap(topTile,leftTile) > 0 || map.getMap(topTile,rightTile) > 0) {
            y = (topTile + 1) * tileSize;
            velY = 0;
        }
        topTile = (y+4)/tileSize;
        bottomTile = (y+height-4)/tileSize;
        leftTile = x/tileSize;
        rightTile = (x+width-1)/tileSize;

        if (map.getMap(topTile,leftTile) > 0 || map.getMap(bottomTile,leftTile) > 0){
            x = (leftTile + 1) * tileSize;
        }
        if (map.getMap(topTile,rightTile) > 0 || map.getMap(bottomTile,rightTile) > 0){
            x = rightTile * tileSize - width;
        }
        
        // spike
        int tileX = (x + width / 2) / tileSize;
        int tileY = (y + height / 2) / tileSize;
        if (tileX >= 0 && tileX < map.getMapWidthInTiles()
                && tileY >= 0 && tileY < map.getMapHeightInTiles()
                && map.isSpikeAt(tileY, tileX)){
                takeDamage(1);
        }
    }

    //Collect Banana
    public void checkBananaCollision() {
        bananas = map.getBananas();
        Iterator<Point> it = bananas.iterator();
        Rectangle playerBox = new Rectangle(x, y, width, height);

        while (it.hasNext()) {
            Point b = it.next();
            Rectangle bananaBox = new Rectangle(b.x, b.y - map.getTileSize(), map.getTileSize(), map.getTileSize());
            if (playerBox.intersects(bananaBox)) {
                bananasCollected++;
                System.out.println("Collected! total = " + bananasCollected);
                it.remove();   
            }
        }
    }
    //get banana
    public int getBananasCollected() {
        return bananasCollected;
    }
    
    //Bound
    public Rectangle getBounds(){
        return new Rectangle(x, y, width, height);
    }
    
    //Feet
    public Rectangle getFeetRect() {
        int h = 6;
        return new Rectangle(x + 4, y + height - h, width - 8, h);
    }
    
    //Stick to ground
    private void clampToWorld() {
        int worldW = map.getPixelWidth();
        int worldH = map.getPixelHeight();
        if (x < 0){
            x = 0;
        }
        if (x + width > worldW){
            x = worldW - width;
        }
        if (y < 0){
            y = 0; velY = 0;
        }
        if (y + height >= worldH) {
            y = worldH - height;
            velY = 0;
            onGround = true;
            jumping = false;
        }
    }

    //hp+damage
    public void takeDamage(int dmg) {
        if (iFrame > 0 || isDead) return;
        hearts -= dmg;
        if (hearts <= 0) {
            hearts = 0;
            die();
        } else {
            iFrame = 60;
        }
    }
    
    public void die() {
        System.out.println("Player died");
        isDead = true;
    }

    public void bounceAfterKill() {
        velY = -8;
        onGround = false;
    }

    //draw player
    public void draw(Graphics g, int camX, int camY) {
        int drawX = x - camX;
        int drawY = y - camY+6;
        if (iFrame > 0 && (iFrame/4)%2 == 0) return;
        Image current = moving ? walk[frame % walk.length] : idle[frame % idle.length];

        if (toRight)
            g.drawImage(current, drawX, drawY, width, height, null);
        else
            g.drawImage(current, drawX + width, drawY, -width, height, null);
    }
}
