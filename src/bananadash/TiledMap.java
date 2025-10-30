package bananadash;

import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class TiledMap {
    //for enemy prop goal
    public static class SpawnInfo {
        public final int x,y,w,h;
        public float range;
        public String propName;
        public SpawnInfo(int x,int y,int w,int h,float range){
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.range = range; //for enemy
        }
    }
    
    //Goal+Goal Requirement
    public static class Goal{
        public int x, y, w, h;
        public String next;
        public int requiredBanana;
        public boolean requiredEnemyCleared;
        public String transition;
    }
    private Goal goal;
    private Image goalImg;
    public Goal getGoal(){
        return goal;
    }
    
    //read string propoties
    private String getStringProp(JSONObject obj, String key) {
        JSONArray props = obj.optJSONArray("properties");
        if (props == null) return null;

        for (int i = 0; i < props.length(); i++) {
            JSONObject p = props.getJSONObject(i);
            if (key.equals(p.optString("name"))) {
                return p.optString("value", null);
            }
        }
        return null;
    }
    
    //read int propoties
    private int getIntProp(JSONObject obj, String key, int defVal) {
        JSONArray props = obj.optJSONArray("properties");
        if (props == null) return defVal;
        for (int i = 0; i < props.length(); i++) {
            JSONObject p = props.getJSONObject(i);
            if (key.equals(p.optString("name"))) {
                Object v = p.opt("value");
                if (v instanceof Number) return ((Number) v).intValue();
                try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
            }
        }
        return defVal;
    }

    private boolean getBoolProp(JSONObject obj, String key, boolean defVal) {
        JSONArray props = obj.optJSONArray("properties");
        if (props == null) return defVal;
        for (int i = 0; i < props.length(); i++) {
            JSONObject p = props.getJSONObject(i);
            if (key.equals(p.optString("name"))) {
                Object v = p.opt("value");
                if (v instanceof Boolean){
                    return (Boolean) v;
                }
                if (v != null){
                    return Boolean.parseBoolean(String.valueOf(v));
                }
            }
        }
        return defVal;
    }
    
    //Prop
    private Map<String, Image> propImages = new HashMap<>();
    private java.util.List<SpawnInfo> props = new ArrayList<>();
    
    //All Layer
    private final Map<String, Image> imageCache = new HashMap<>();
    private java.util.List<int[][]> allLayersData;
    private int mapWidth;
    private int mapHeight;
    private final int tileSize = 32;
    private Image tileset;
    public int getPixelWidth(){
        return getMapWidthInTiles()*getTileSize();
    }
    public int getPixelHeight(){
        return getMapHeightInTiles()*getTileSize();
    }
    public int getMapWidthInTiles(){
        return mapWidth;
    }
    public int getMapHeightInTiles(){
        return mapHeight;
    }
    public int getTileSize(){
        return tileSize;
    }

    //banana
    private java.util.List<Point> bananas = new java.util.ArrayList<>();
    public java.util.List<Point> getBananas(){
        return bananas;
    }
    
    //enemy
    private java.util.List<SpawnInfo> enemySpawns = new java.util.ArrayList<>();
    private java.util.List<Enemy> enemies = new java.util.ArrayList<>();
    
    public java.util.List<Enemy> getEnemies(){
        return enemies;
    }

    public int getMap(int tileY, int tileX) {
        if (collisionLayer != null &&
            tileY >= 0 && tileY < mapHeight &&
            tileX >= 0 && tileX < mapWidth) {
            return collisionLayer[tileY][tileX];
        }
        return 0;
    }
    
    private int[][] collisionLayer;
    private int[][] spikeLayer;
    public boolean isSpikeAt(int tileY, int tileX) {
        if (spikeLayer == null) return false;
        if (tileY < 0 || tileY >= mapHeight || tileX < 0 || tileX >= mapWidth) return false;
        return spikeLayer[tileY][tileX] > 0;
    }

    public TiledMap(String mapPath, String tilesetPath) {
        allLayersData = new ArrayList<>();
        loadJSON(mapPath);
        tileset = new ImageIcon(getClass().getResource(tilesetPath)).getImage();
        spawnEnemies();
    }


    public void loadJSON(String path) {
        try (InputStream is = new FileInputStream("src/" + path)) {
            if (is == null) {
                System.err.println("❌ Map file not found: " + path);
                return;
            }

            String jsonTxt = new String(is.readAllBytes());
            JSONObject root = new JSONObject(jsonTxt);
            JSONArray layers = root.getJSONArray("layers");

            // ---------- Tile layers ----------
            for (int i = 0; i < layers.length(); i++) {
                JSONObject layer = layers.getJSONObject(i);
                if (!"tilelayer".equals(layer.getString("type"))) continue;

                mapWidth = layer.getInt("width");
                mapHeight = layer.getInt("height");
                int[][] layerData = new int[mapHeight][mapWidth];

                JSONArray data = layer.getJSONArray("data");
                int idx = 0;
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        layerData[y][x] = data.getInt(idx++);
                    }
                }

                allLayersData.add(layerData);
                if (layer.getString("name").equalsIgnoreCase("ground")) {
                    collisionLayer = layerData;
                }
            }

            // ---------- Object layers ----------
            for (int i = 0; i < layers.length(); i++) {
                JSONObject layer = layers.getJSONObject(i);
                if (!"objectgroup".equals(layer.getString("type"))) continue;

                String lname = layer.getString("name").toLowerCase();
                JSONArray objects = layer.optJSONArray("objects");
                if (objects == null) continue;
                //load spike
                if (lname.equals("spike")) {
                    spikeLayer = new int[mapHeight][mapWidth];
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        float x = (float) obj.optDouble("x", 0);
                        float y = (float) obj.optDouble("y", 0);
                        float height = (float) obj.optDouble("height", tileSize);

                        int tileX = (int) (x / tileSize);
                        int tileY = (int) ((y - height) / tileSize);

                        int spikeTiles = Math.max(1, Math.round(height / tileSize));
                        for (int k = 0; k < spikeTiles; k++) {
                            int yy = tileY + k;
                            if (tileX >= 0 && tileX < mapWidth && yy >= 0 && yy < mapHeight) {
                                spikeLayer[yy][tileX] = 1;
                            }
                        }
                    }
                    System.out.println("✅ Loaded " + objects.length() + " spikes");
                }
                //load banana
                else if (lname.equals("banana")) {
                    bananas.clear();
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        float x = (float) obj.optDouble("x", 0);
                        float y = (float) obj.optDouble("y", 0);
                        bananas.add(new Point((int)x, (int)y));
                    }
                    System.out.println("✅ Loaded " + objects.length() + " bananas");
                }
                //load enemy
                else if (lname.equals("enemy")) {
                    enemySpawns.clear();
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        float x = (float) obj.optDouble("x", 0);
                        float y = (float) obj.optDouble("y", 0);
                        float w = (float) obj.optDouble("width", tileSize);
                        float h = (float) obj.optDouble("height", tileSize);

                        float patrolRange = 10;
                        float speed = 1.5f;
                        JSONArray props = obj.optJSONArray("properties");
                        if (props != null) {
                            for (int p = 0; p < props.length(); p++) {
                                JSONObject prop = props.getJSONObject(p);
                                if ("patrolRange".equals(prop.optString("name"))) {
                                    patrolRange = (float) prop.optDouble("value", patrolRange);
                                    break;
                                }
                                else if ("speed".equals(prop.optString("name"))) {
                                    speed = (float) prop.optDouble("value", speed);
                                    break;
                                }
                                
                            }
                        }

                        boolean isTileObject = obj.has("gid");
                        int spawnX = Math.round(x);
                        int spawnY = Math.round(isTileObject ? y : (y + h));
                        enemySpawns.add(new SpawnInfo(spawnX, spawnY, Math.round(w), Math.round(h), patrolRange));

                        System.out.println("Enemy " + j + " → patrolRange=" + patrolRange);
                    }
                    System.out.println("✅ Loaded " + enemySpawns.size() + " enemy spawns");
                }
                //load prop
                else if (lname.equals("tree")) {
                    props.clear();

                    String basePath = "/Asset/Props/";
                    propImages.put("start", new ImageIcon(getClass().getResource(basePath + "Arrow.png")).getImage());
                    propImages.put("tree1", new ImageIcon(getClass().getResource(basePath + "tree1.png")).getImage());
                    propImages.put("flower1", new ImageIcon(getClass().getResource(basePath + "flower1.png")).getImage());
                    propImages.put("flower2", new ImageIcon(getClass().getResource(basePath + "flower2.png")).getImage());
                    propImages.put("rock", new ImageIcon(getClass().getResource(basePath + "Rock.png")).getImage());
                    propImages.put("grass", new ImageIcon(getClass().getResource(basePath + "Grass.png")).getImage());

                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        float x = (float) obj.optDouble("x", 0);
                        float y = (float) obj.optDouble("y", 0);
                        float w = (float) obj.optDouble("width", tileSize);
                        float h = (float) obj.optDouble("height", tileSize);

                        String name = "";
                        JSONArray propsArr = obj.optJSONArray("properties");
                        if (propsArr != null) {
                            for (int p = 0; p < propsArr.length(); p++) {
                                JSONObject prop = propsArr.getJSONObject(p);
                                if ("name".equalsIgnoreCase(prop.optString("name"))) {
                                    name = prop.optString("value", "");
                                }
                            }
                        }
                        SpawnInfo s = new SpawnInfo((int)x, (int)y, (int)w, (int)h, 0);
                        s.propName = name;
                        props.add(s);
                        System.out.println("Loaded prop: " + s.propName + " at (" + x + "," + y + ")");
                    }
                }
                //load goal
                else if (lname.equals("final")){
                    for (int j = 0; j < objects.length(); j++) {
                        JSONObject obj = objects.getJSONObject(j);
                        String nameProp = getStringProp(obj, "name");
                        if (nameProp != null && !nameProp.equalsIgnoreCase("goal")) continue;

                        goal = new Goal();
                        goal.x = (int) obj.optDouble("x", 0);
                        goal.y = (int) obj.optDouble("y", 0);
                        goal.w = (int) obj.optDouble("width", 32);
                        goal.h = (int) obj.optDouble("height", 32);

                        goal.next = getStringProp(obj, "next");
                        goal.requiredBanana       = getIntProp(obj, "requiredBanana", 0);
                        goal.requiredEnemyCleared = getBoolProp(obj, "requiredEnemyCleared", false);
                        goal.transition = getStringProp(obj, "transition");

                        if (goalImg == null) {
                            java.net.URL u = getClass().getResource("/Asset/Props/Arrow.png");
                            if (u != null) goalImg = new ImageIcon(u).getImage();
                        }

                        System.out.println("Goal loaded at ("+goal.x+","+goal.y+"), w="+goal.w+" h="+goal.h+", next="+goal.next);
                    }
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void checkPlayerEnemyCollision(player p) {
       Rectangle feet = p.getFeetRect();
       Rectangle body = p.getBounds();

       for (Enemy e : enemies) {
           if (!e.isAlive()) continue;

           Rectangle er = e.getBounds();
           if (!er.intersects(body)) continue;

           boolean stompFromTop =
               feet.intersects(er) && p.velY > 0 &&
               (feet.y + feet.height) - er.y <= 10;

           if (stompFromTop) {
               e.die();
               p.bounceAfterKill();
           } else {
               p.takeDamage(1);
           }
       }
   }
    
    //enemy
    public void buildEnemiesFromSpawns(java.util.List<SpawnInfo> spawns) {
        enemies.clear();
        for (SpawnInfo s : spawns) {
            enemies.add(new Enemy(s.x, s.y, s.range, 1.5f, s.w, s.h));
        }
    }
    public void spawnEnemies() {
        enemies.clear();
        for (SpawnInfo s : enemySpawns) {
            enemies.add(new Enemy(s.x, s.y, s.range, 1.5f, s.w, s.h));
            System.out.println("spawn enemy at ("+s.x+","+s.y+") range="+s.range);
        }
    }
    public void updateEnemies() {
        for (Enemy e : enemies) e.update();
    }

    public void drawEnemies(Graphics g, int camX, int camY) {
        for (Enemy e : enemies) e.draw(g, camX, camY);
    }
    
    //goal
    public void drawGoal(Graphics g, int offsetX, int offsetY) {
        if (goal == null || goalImg == null) return;
        int tileSize = getTileSize();
        int drawX = goal.x + offsetX;
        int drawY = goal.y - goal.h + offsetY - 2;
        g.drawImage(goalImg, drawX, drawY, goal.w, goal.h, null);
    }

    //draw map
    public void draw(Graphics g, int offsetX, int offsetY) {
        // --- Tile layers ---
        for (int[][] layerData : allLayersData) {
            for (int y = 0; y < mapHeight; y++) {
                for (int x = 0; x < mapWidth; x++) {
                    if (layerData[y][x] != 0) {
                        int tileId = layerData[y][x];
                        int tileSizeInTileset = 32;
                        int tilesetCols = tileset.getWidth(null) / tileSizeInTileset;

                        int tileIndex = tileId - 1;
                        int sourceX = (tileIndex % tilesetCols) * tileSizeInTileset;
                        int sourceY = (tileIndex / tilesetCols) * tileSizeInTileset;

                        int destX = x * tileSize + offsetX;
                        int destY = y * tileSize + offsetY;

                        g.drawImage(tileset,
                                destX, destY, destX + tileSize, destY + tileSize,
                                sourceX, sourceY, sourceX + tileSizeInTileset, sourceY + tileSizeInTileset,
                                null);
                    }
                }
            }
        }
        //spike
        if (spikeLayer != null) {
            Image spikeImg = new ImageIcon(getClass().getResource("/Asset/Props/Spikes.png")).getImage();
            for (int y = 0; y < mapHeight; y++) {
                for (int x = 0; x < mapWidth; x++) {
                    if (spikeLayer[y][x] > 0) {
                        int destX = x * tileSize + offsetX;
                        int destY = y * tileSize + offsetY + (int)(tileSize * 0.25f);
                        g.drawImage(spikeImg, destX, destY, tileSize, tileSize, null);
                    }
                }
            }
        }
        //banana
        if (!bananas.isEmpty()) {
            Image[] banana = new Image[]{
                new ImageIcon(getClass().getResource("/Asset/Items/Banana1.png")).getImage(),
                new ImageIcon(getClass().getResource("/Asset/Items/Banana2.png")).getImage(),
                new ImageIcon(getClass().getResource("/Asset/Items/Banana3.png")).getImage()
            };

            double scale = 0.6;
            int bananaW = (int)(tileSize * scale);
            int bananaH = (int)(tileSize * scale);

            int frame = (int)((System.currentTimeMillis()/175) % banana.length);
            Image bananaImg = banana[frame];

            for (Point b : bananas) {
                int destX = b.x + offsetX;
                int destY = b.y - tileSize + offsetY;

                int adjustX = destX + (tileSize - bananaW) / 2;
                int adjustY = destY + (tileSize - bananaH) - 2;

                g.drawImage(bananaImg, adjustX, adjustY, bananaW, bananaH, null);
            }
        }
        
        //prop
        if (!props.isEmpty()) {
            for (SpawnInfo p : props) {
                Image img = propImages.get(p.propName);
                if (img != null) {
                    int drawX = p.x + offsetX;
                    int drawY = p.y - p.h + offsetY;
                    g.drawImage(img, drawX, drawY - img.getHeight(null) + p.h, img.getWidth(null), img.getHeight(null), null);
                }
            }
        }
    }
}
