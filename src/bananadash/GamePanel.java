package bananadash;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private KeyHandler key;
    private player p;
    private TiledMap map;
    private Timer timer;
    
    //frame
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    
    //focus
    private int cameraX = 0;
    private int cameraY = 0;
    
    //scale
    private double scale = 1.5;
    
    //game state
    enum GameState { TITLE, PLAY, GAME_OVER, GAME_END }
    private GameState state = GameState.TITLE;
    private Image titleImg;
    private Image winImg;
    private Image gameOverImg;
    
    public void startGame() {
        state = GameState.PLAY;
    }
    
    public GameState getGameState(){
        return state;
    }
    
    public void gameOver() {
        state = GameState.GAME_OVER;
    }
    
    public void returnToTitle() {
        state = GameState.TITLE;
        map = new TiledMap("/Asset/map/level1_final.json", "/Asset/Tileset/TilesetGrass.png");
        p = new player(64, 64, map);
        cameraX = 0;
        cameraY = 0;
        repaint();
    }


    //sky bg
    private Image skyImg, cloudsBack, cloudsMid, cloudsFront;
    private Image loadBG(String... candidates) {
        for (String p : candidates) {
            if (p == null) continue;
            java.net.URL url = getClass().getResource(p);
            if (url != null) return new ImageIcon(url).getImage();
        }
        return null;
    }

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        requestFocus();
        addKeyListener(key);
        key = new KeyHandler(this);
        addKeyListener(key);
        //state pic
        titleImg = new ImageIcon(getClass().getResource("/Asset/picture/gameCover.jpg")).getImage();
        winImg = new ImageIcon(getClass().getResource("/Asset/picture/gameWin.png")).getImage();
        gameOverImg = new ImageIcon(getClass().getResource("/Asset/picture/gameOver.png")).getImage();
        //background pic
        skyImg      = loadBG("/Asset/Background/SkyBackground.png");
        cloudsBack  = loadBG("/Asset/Background/Cludsbackgroundback.png");
        cloudsMid   = loadBG("/Asset/Background/Cludsbackgroundmiddle.png");
        cloudsFront = loadBG("/Asset/Background/Cludsbackgroundfront.png");
        //map
        map = new TiledMap("/Asset/map/level1_final.json", "/Asset/Tileset/TilesetGrass.png");
        p = new player(64, 64, map);

        timer = new Timer(16, e -> {
            updateGame();
            repaint();
        });
        timer.start();
    }

    private void updateGame() {
        p.update(key.left, key.right, key.jump);
        //scale
        int scaledWidth = (int)(WIDTH / scale);
        int scaledHeight = (int)(HEIGHT / scale);

        cameraX = Math.max(0, Math.min(p.x - scaledWidth / 2,
                map.getMapWidthInTiles() * 32 - scaledWidth));
        int mapPixelHeight = map.getMapHeightInTiles() * 32;
        cameraY = Math.max(0, Math.min(p.y - scaledHeight/2, mapPixelHeight - scaledHeight));

        if (mapPixelHeight < scaledHeight) {
            cameraY = 0;
        }

        map.updateEnemies();
        map.checkPlayerEnemyCollision(p);
        if (state == GameState.PLAY && p.getHearts() <= 0) {
            gameOver();
            return;
        }

        if (map.getGoal() != null) {
            TiledMap.Goal g = map.getGoal();
            Rectangle goalRect = new Rectangle(g.x, g.y - g.h, g.w, g.h);
            Rectangle playerRect = new Rectangle(p.x,p.y,p.width,p.height);
            //Check goal
            if (playerRect.intersects(goalRect)) {
                boolean bananaOK = p.getBananasCollected() >= g.requiredBanana;
                boolean enemyOK = !g.requiredEnemyCleared || map.getEnemies().stream().noneMatch(Enemy::isAlive);
                if (bananaOK && enemyOK) {
                    if ("end".equalsIgnoreCase(g.next)) {
                        state = GameState.GAME_END;
                    } else {
                        loadNextLevel(g.next);
                    }
                }
            }
        }
    }
    
    //map level2
    public void loadNextLevel(String path) {
        try {           
           if ("win".equalsIgnoreCase(path)) {
                state = GameState.GAME_OVER;
                return;
            }
            System.out.println("Loading next level: " + path);
            map = new TiledMap("/Asset/map/level2.json", "/Asset/Tileset/TilesetGrass.png");
            p = new player(64,400, map);
            cameraX = 0;
            cameraY = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //scale x1.5
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.scale(scale, scale);
        drawBackground(g2);
        //game state
        if (state == GameState.TITLE) {
            g.drawImage(titleImg, 0, 0, getWidth(), getHeight(), null);
            long time = System.currentTimeMillis() / 800; 
            if (time % 2 == 0) {
               g.setFont(new Font("Monospaced", Font.BOLD, 28));
               g.setColor(Color.WHITE);
               String text = "Press ENTER to Start";
               int textWidth = g.getFontMetrics().stringWidth(text);
               g.drawString(text, (getWidth() - textWidth) / 2, getHeight() - 100);
           }
            g.dispose();
            return;
        }
        if (state == GameState.GAME_END) {
            g.drawImage(winImg, 0, 0, getWidth(), getHeight(), null);
            long time = System.currentTimeMillis() / 800; 
            if (time % 2 == 0) {
                g.setFont(new Font("Monospaced", Font.BOLD, 28));
                g.setColor(Color.WHITE);
                String msg = "Press ENTER to Play Again!";
                int w = g.getFontMetrics().stringWidth(msg);
                g.drawString(msg, (getWidth() - w) / 2, getHeight() - 100);
            }
            return;
        }
        if (state == GameState.GAME_OVER) {
            g.drawImage(gameOverImg, 0, 0, getWidth(), getHeight(), null);
            long time = System.currentTimeMillis() / 800; 
            if (time % 2 == 0) {
                g.setFont(new Font("Monospaced", Font.BOLD, 28));
                g.setColor(Color.WHITE);
                String text = "Press ENTER to Return to Title";
                int textWidth = g.getFontMetrics().stringWidth(text);
                g.drawString(text, (getWidth() - textWidth) / 2, getHeight() - 100);
            }
            return;
        }
        map.draw(g2, -cameraX, -cameraY);//draw map
        map.drawEnemies(g2, cameraX, cameraY);//draw enemy
        map.drawGoal(g2, -cameraX, -cameraY);//draw goal
        p.draw(g2, cameraX, cameraY);//draw player
        drawBananaHUD(g);
        drawHeartsHUD(g);
        g2.dispose();
    }

    //Draw Hearts
    private void drawHeartsHUD(Graphics g) {
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        int x0 = 20, y0 = 40, gap = 40;
        int hearts = p.getHearts();
        int maxHearts = p.getMaxHearts();

        for (int i = 0; i < maxHearts; i++) {
            if (i < hearts) {
                g.setColor(Color.RED);
                g.drawString("❤️", x0 + i * gap, y0);
            } else {
                g.setColor(Color.GRAY);
                g.drawString("🤍", x0 + i * gap, y0);
            }
        }
    }
    
    //Draw Banana
    private void drawBananaHUD(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Image bananaIcon = new ImageIcon(getClass().getResource("/Asset/Items/Banana1.png")).getImage();

        int x = 250, y = 15;
        int iconSize = 32;
        g2.drawImage(bananaIcon, x, y, iconSize, iconSize, null);
        g2.setFont(new Font("Monospaced",Font.BOLD,24));
        g2.setColor(Color.WHITE);
        g2.drawString("x " + p.getBananasCollected(), x + iconSize + 10, y + 26);
    }
    
    //Draw Background
    private void drawBackground(Graphics g) {
        int viewW = getWidth();
        int viewH = getHeight();

        double skySpeed = 0.10;
        double backSpeed = 0.25;
        double midSpeed  = 0.45;
        double frontSpeed= 0.70;

        java.util.function.BiConsumer<Image, Double> drawLayer = (img, speed) -> {
            if (img == null) return;
            int imgW = img.getWidth(null);
            int imgH = img.getHeight(null);
            if(imgW <= 0 || imgH <= 0) return;

            int offsetX = (int)Math.floor((-cameraX * speed) % imgW);
            if (offsetX > 0) offsetX -= imgW;

            int y;
            if (speed == skySpeed) {
                y = 0;
                for (int x = offsetX; x < viewW; x += imgW) {
                    g.drawImage(img, x, 0, imgW, viewH, null);
                }
                return;
            } else if (speed == backSpeed) {
                y = viewH - imgH - 160;
            } else if (speed == midSpeed) {
                y = viewH - imgH - 100;
            } else {
                y = viewH - imgH - 60;
            }

            for (int x = offsetX; x < viewW; x += imgW) {
                g.drawImage(img, x, y, null);
            }
        };
        
        drawLayer.accept(skyImg,skySpeed);
        drawLayer.accept(cloudsBack,backSpeed);
        drawLayer.accept(cloudsMid,midSpeed);
        drawLayer.accept(cloudsFront,frontSpeed);
    }
}
