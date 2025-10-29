/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package banana_dash;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Monkey + Parallax Background – Single file */
public class Banana_Dash{
    public static void main(String[] args) {
        {
            JFrame f = new JFrame("Banana Dash – Monkey & Parallax BG");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(new GamePanel());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        };
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    //Player sprites
    private static final String PLAYER_PATH =
        "C:\\Users\\User\\Documents\\NetBeansProjects\\Banana_Dash\\src\\Monkey Forest Asset Pack\\Player\\";
    //Background
    private static final String BG_PATH =
        "C:\\Users\\User\\Documents\\NetBeansProjects\\Banana_Dash\\src\\Monkey Forest Asset Pack\\Background\\";

    private final Timer timer = new Timer(16, this); // ~60 fps
    private final Monkey monkey = new Monkey();

    // world config
    private static final int WIDTH = 900, HEIGHT = 520;
    private static final int GROUND_Y = 450;
    private static final float GRAVITY = 0.7f;
    private static final float SPEED = 4.4f;
    private static final float JUMP_VY = -13.5f;

    // input
    private boolean left, right, up;

    // background images
    private BufferedImage sky, cloudsBack, cloudsMiddle, cloudsFront;

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 30, 40));
        setFocusable(true);
        addKeyListener(this);

        // --- load player frames ---
        monkey.idleFrames = new BufferedImage[] {
            loadPlayer("Monkey Idle 1.png"),
            loadPlayer("Monkey Idle 2.png"),
            loadPlayer("Monkey Idle 3.png")
        };
        monkey.walkFrames = new BufferedImage[] {
            loadPlayer("Monkey Walk 1.png"),
            loadPlayer("Monkey Walk 2.png")
        };
        // ถ้าไม่มีรูป jump แยก ใช้ idle เฟรมแรกแทนก่อนได้
        monkey.jumpFrames = new BufferedImage[] { monkey.idleFrames[0] };

        // start position
        monkey.x = 100;
        monkey.y = GROUND_Y - monkey.h;

        // --- load backgrounds ---
        sky         = loadBG("Sky background.png");
        cloudsBack  = loadBG("Cluds background back.png");
        cloudsMiddle= loadBG("Cluds background middle.png");
        cloudsFront = loadBG("Cluds background front.png");

        timer.start();
    }

    private BufferedImage loadPlayer(String name) {
        try {
            return ImageIO.read(new File(PLAYER_PATH + name));
        } catch (Exception e) {
            System.err.println("failed to load player image: " + name);
            return null;
        }
    }

    private BufferedImage loadBG(String name) {
        try {
            return ImageIO.read(new File(BG_PATH + name));
        } catch (Exception e) {
            System.err.println("failed to load background image: " + name);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // input → velocity
        if (left && !right) monkey.vx = -SPEED;
        else if (right && !left) monkey.vx = SPEED;
        else monkey.vx = 0;

        if (up && monkey.onGround(GROUND_Y)) {
            monkey.vy = JUMP_VY;
        }

        // physics
        monkey.vy += GRAVITY;
        monkey.x += monkey.vx;
        monkey.y += monkey.vy;

        // ground
        if (monkey.y + monkey.h > GROUND_Y) {
            monkey.y = GROUND_Y - monkey.h;
            monkey.vy = 0;
        }

        // screen bounds
        if (monkey.x < 0) monkey.x = 0;
        if (monkey.x + monkey.w > WIDTH) monkey.x = WIDTH - monkey.w;

        // state
        if (!monkey.onGround(GROUND_Y))      monkey.state = Monkey.State.JUMP;
        else if (Math.abs(monkey.vx) > 0.1f) monkey.state = Monkey.State.WALK;
        else                                  monkey.state = Monkey.State.IDLE;

        monkey.facingLeft = monkey.vx < 0;
        monkey.tickAnimation();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // ====== BACKGROUND LAYERS (จากไกล → ใกล้) ======
        // ฟ้า (คงที่)
        drawScaled(g2, sky, 0, 0, WIDTH, HEIGHT);

        // parallax offset (ยิ่งใกล้ยิ่งไหลเร็ว)
        int off = (int)(monkey.x * 0.20f);
        drawTiledX(g2, cloudsBack,   -(int)(off * 0.8f),  0, 1.00f);
        drawTiledX(g2, cloudsMiddle, -(int)(off * 1.2f),  0, 1.00f);
        drawTiledX(g2, cloudsFront,  -(int)(off * 1.6f),  0, 1.00f);

        // ====== GROUND ======
        g2.setColor(new Color(60, 80, 90));
        g2.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // ====== PLAYER ======
        monkey.draw(g2);
    }

    /** วาดภาพให้เต็มสัดส่วนเป้าหมาย (ถ้ารูปเป็น null จะข้าม) */
    private void drawScaled(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        if (img == null) return;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x, y, w, h, null);
    }

    /** วาดพื้นหลังแบบกวาดซ้ำแนวนอน (ง่าย ๆ) + scaleY */
    private void drawTiledX(Graphics2D g2, BufferedImage img, int startX, int y, float scaleY) {
        if (img == null) return;
        int h = (int)(HEIGHT * scaleY);
        int w = (int)(img.getWidth() * (h / (float)img.getHeight())); // scale ตามความสูงจอ
        int x = startX % w;
        if (x > 0) x -= w;
        for (; x < WIDTH; x += w) {
            g2.drawImage(img, x, 0, w, HEIGHT, null);
        }
    }

    // ====== input ======
    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_W, KeyEvent.VK_SPACE -> up = true;
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = false;
            case KeyEvent.VK_W, KeyEvent.VK_SPACE -> up = false;
        }
    }
    @Override public void keyTyped(KeyEvent e) {}
}

/** --------- Monkey (animation + draw) --------- */
class Monkey {
    enum State { IDLE, WALK, JUMP }

    // position/size
    int x = 0, y = 0, w = 64, h = 64;
    float vx = 0, vy = 0;
    boolean facingLeft = false;

    // frames
    BufferedImage[] idleFrames;
    BufferedImage[] walkFrames;
    BufferedImage[] jumpFrames;

    // state
    State state = State.IDLE;

    // animation counters
    private int frameIndex = 0;
    private int frameTick = 0;
    private int frameDelay = 8; // ค่ายิ่งมาก เฟรมเปลี่ยนช้าลง

    boolean onGround(int groundY) { return y + h >= groundY - 1; }

    void tickAnimation() {
        BufferedImage[] arr = currentFrames();
        if (arr == null || arr.length == 0) return;
        frameTick++;
        if (frameTick > frameDelay) {
            frameTick = 0;
            frameIndex = (frameIndex + 1) % arr.length;
        }
    }

    private BufferedImage[] currentFrames() {
        return switch (state) {
            case WALK -> (walkFrames != null && walkFrames.length > 0) ? walkFrames : idleFrames;
            case JUMP -> (jumpFrames != null && jumpFrames.length > 0) ? jumpFrames : idleFrames;
            default   -> idleFrames;
        };
    }

    void draw(Graphics2D g2) {
        BufferedImage[] arr = currentFrames();
        BufferedImage img = (arr != null && arr.length > 0) ? arr[Math.min(frameIndex, arr.length - 1)] : null;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int drawW = 128, drawH = 128;            // ขยายให้เห็นชัด
        int drawX = x, drawY = y - (drawH - h);  // ให้เท้าติดพื้นพอดี

        if (img != null) {
            if (facingLeft) {
                g2.drawImage(img, drawX + drawW, drawY, -drawW, drawH, null); // mirror
            } else {
                g2.drawImage(img, drawX, drawY, drawW, drawH, null);
            }
        } else {
            // placeholder ถ้ารูปยังไม่โหลด
            g2.setColor(new Color(255, 200, 70));
            g2.fillRoundRect(drawX, drawY, drawW, drawH, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawString("No Image", drawX + 10, drawY + drawH/2);
        }
    }
}
