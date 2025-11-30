import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameEngine extends JPanel implements Runnable, KeyListener {

    // --- CONFIGURATION ---
    final int tileSize = 32;

    // SCREEN SETTINGS
    final int maxScreenCol = 24;
    final int maxScreenRow = 18;
    final int screenWidth = tileSize * maxScreenCol;
    final int screenHeight = tileSize * maxScreenRow;

    // WORLD SETTINGS (computed from map)
    int maxWorldCol;
    int maxWorldRow;
    int worldWidth;
    int worldHeight;

    // --- GAME STATES ---
    final int STATE_TITLE = 0;
    final int STATE_STARTER_SELECT = 1;
    final int STATE_ROAMING = 2;
    final int STATE_TRANSITION = 3;
    final int STATE_BATTLE = 4;

    int gameState = STATE_TITLE;

    // --- SYSTEM ---
    Thread gameThread;
    Set<Integer> activeKeys = new HashSet<>();

    // --- ASSETS ---
    Image starterMapImage;
    Image titleScreenImage;
    Image pressEnterImage;
    BufferedImage battleBg;
    BufferedImage battleBase;

    // --- ANIMATION COUNTERS ---
    int titleCounter = 0;
    boolean showStartText = true;
    int transitionCounter = 0;
    boolean flashWhite = true;

    // --- ENTITIES ---
    Player player;
    Rooster playerCock;
    Rooster enemyCock;
    String battleMessage = "";
    int starterIndex = 0;

    // --- MUSIC ---
    MusicManager musicManager;

    // MAP DATA (25 x 25)
    int[][] newMapData = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,0,2,2,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,1,1,1,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,1,1,1,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,1,1,1,2,0,0,0,1},
            {1,2,2,2,2,1,0,0,0,2,2,2,2,2,2,0,0,0,0,0,2,0,0,0,1},
            {1,2,2,2,2,2,0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    public GameEngine() {
        // compute world dimensions from map
        if (newMapData != null && newMapData.length > 0 && newMapData[0] != null) {
            maxWorldRow = newMapData.length;
            maxWorldCol = newMapData[0].length;
        } else {
            // fallback defaults
            maxWorldRow = 24;
            maxWorldCol = 25;
        }
        worldWidth = tileSize * maxWorldCol;
        worldHeight = tileSize * maxWorldRow;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        loadImages();
    }

    public void setupGame() {
        this.addKeyListener(this);
        this.setFocusable(true);

        // Request focus after component hierarchy is realized
        SwingUtilities.invokeLater(this::requestFocusInWindow);

        player = new Player(tileSize * 11, tileSize * 9);
        musicManager = new MusicManager();
        musicManager.playMusic(GameConstants.MUSIC_TITLE, true);
        gameState = STATE_TITLE;
    }

    private void loadImages() {
        try {
            starterMapImage = loadImageSafe("/res/MapV2.png");
            titleScreenImage = loadImageSafe("/res/Title.png");

            // --- FIX: Enable Transparency for Start Button ---
            pressEnterImage = loadImageWithTransparency("/res/start.png", true);

            // --- FIX: Load Battle Assets with Explicit Check ---
            battleBg = toBuffered(loadImageSafe(GameConstants.PATH_BG_CITY));
            if (battleBg == null) System.out.println("❌ ERROR: Failed to load " + GameConstants.PATH_BG_CITY);

            battleBase = toBuffered(loadImageSafe(GameConstants.PATH_BASE));
            if (battleBase == null) System.out.println("❌ ERROR: Failed to load " + GameConstants.PATH_BASE);

        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }
    }

    // helper to convert Image -> BufferedImage safely
    private BufferedImage toBuffered(Image img) {
        if (img == null) return null;
        if (img instanceof BufferedImage) return (BufferedImage) img;
        BufferedImage buf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return buf;
    }

    private Image loadImageSafe(String path) {
        URL url = getClass().getResource(path);
        if (url != null) {
            return new ImageIcon(url).getImage();
        } else {
            System.out.println("⚠️ ERROR: Could not find " + path);
            return null;
        }
    }

    // --- FIX: RESTORED TRANSPARENCY LOGIC ---
    private Image loadImageWithTransparency(String path, boolean removeBlack) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) return null;

            BufferedImage raw = ImageIO.read(url);
            BufferedImage transparentImg = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = transparentImg.createGraphics();
            g2.drawImage(raw, 0, 0, null);
            g2.dispose();

            for (int row = 0; row < transparentImg.getHeight(); row++) {
                for (int col = 0; col < transparentImg.getWidth(); col++) {
                    int rgba = transparentImg.getRGB(col, row);
                    int alpha = (rgba >> 24) & 0xFF;
                    int r = (rgba >> 16) & 0xFF;
                    int g = (rgba >> 8) & 0xFF;
                    int b = rgba & 0xFF;

                    if (removeBlack) {
                        // If pixel is very dark (almost black), make it fully transparent
                        if (alpha > 0 && r < 30 && g < 30 && b < 30) {
                            // set fully transparent ARGB
                            transparentImg.setRGB(col, row, 0x00000000);
                        }
                    }
                }
            }
            return transparentImg;
        } catch (Exception e) {
            System.err.println("loadImageWithTransparency error: " + e.getMessage());
            return null;
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1_000_000_000.0 / 60.0;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        switch (gameState) {
            case STATE_TITLE -> {
                titleCounter++;
                if (titleCounter > 40) {
                    showStartText = !showStartText;
                    titleCounter = 0;
                }
            }
            case STATE_ROAMING -> {
                player.update(activeKeys, newMapData, tileSize);

                int playerCenterX = player.x + (tileSize / 2);
                int playerCenterY = player.y + (tileSize / 2);

                int col = playerCenterX / tileSize;
                int row = playerCenterY / tileSize;

                // use computed world dimensions
                if (col >= 0 && col < maxWorldCol && row >= 0 && row < maxWorldRow) {
                    int tileType = newMapData[row][col];
                    if (tileType == 2 && player.isMoving && new Random().nextInt(100) < 1) {
                        startTransition();
                    }
                }
            }
            case STATE_TRANSITION -> {
                transitionCounter++;
                if (transitionCounter % 5 == 0) flashWhite = !flashWhite;
                if (transitionCounter > 60) startBattle();
            }
        }
    }

    private void startTransition() {
        gameState = STATE_TRANSITION;
        transitionCounter = 0;
        activeKeys.clear();
    }

    private void startBattle() {
        gameState = STATE_BATTLE;
        enemyCock = RoosterFactory.createRandomRooster("Wild Chicken");
        battleMessage = "Wild " + enemyCock.getName() + " appeared!";
        if (musicManager != null) musicManager.playMusic(GameConstants.MUSIC_BATTLE, true);
        if (musicManager != null) musicManager.playSound(GameConstants.SOUND_ROOSTER);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create(); // create copy so we can dispose safely

        switch (gameState) {
            case STATE_TITLE -> drawTitleScreen(g2);
            case STATE_STARTER_SELECT -> drawStarterSelect(g2);
            case STATE_ROAMING -> drawRoaming(g2);
            case STATE_TRANSITION -> {
                drawRoaming(g2);
                drawTransition(g2);
            }
            case STATE_BATTLE -> drawBattle(g2);
        }
        g2.dispose();
    }

    private void drawTitleScreen(Graphics2D g2) {
        if (titleScreenImage != null) {
            g2.drawImage(titleScreenImage, 0, 0, screenWidth, screenHeight, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }
        if (showStartText && pressEnterImage != null) {
            int x = screenWidth / 2 - (pressEnterImage.getWidth(null) / 2);
            int y = screenHeight - 100;
            g2.drawImage(pressEnterImage, x, y, null);
        }
    }

    private void drawStarterSelect(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 60), 0, screenHeight, new Color(0, 0, 0));
        g2.setPaint(gp);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        String text = "CHOOSE YOUR STARTER";
        int x = getCenteredX(g2, text);
        g2.drawString(text, x, 100);

        drawStarterOption(g2, "MANOK NA PULA", 0, 200);
        drawStarterOption(g2, "MANOK NA ITIM", 1, 280);
        drawStarterOption(g2, "MANOK NA BATO", 2, 360);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g2.setColor(Color.GRAY);
        g2.drawString("[W/S] to Move   [ENTER] to Select   [M] Mute/Unmute", 200, 500);
    }

    private void drawStarterOption(Graphics2D g2, String name, int index, int y) {
        if (index == starterIndex) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 26));
            g2.drawString("> " + name + " <", 250, y);
        } else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            g2.drawString(name, 280, y);
        }
    }

    private void drawRoaming(Graphics2D g2) {
        int cameraX = player.x - screenWidth / 2 + tileSize / 2;
        int cameraY = player.y - screenHeight / 2 + tileSize / 2;

        if (cameraX < 0) cameraX = 0;
        if (cameraY < 0) cameraY = 0;
        if (worldWidth > screenWidth && cameraX > worldWidth - screenWidth) cameraX = worldWidth - screenWidth;
        if (worldHeight > screenHeight && cameraY > worldHeight - screenHeight) cameraY = worldHeight - screenHeight;

        g2.translate(-cameraX, -cameraY);
        if (starterMapImage != null) {
            g2.drawImage(starterMapImage, 0, 0, worldWidth, worldHeight, null);
        }
        player.draw(g2, tileSize);
        g2.translate(cameraX, cameraY);
    }

    private void drawTransition(Graphics2D g2) {
        if (flashWhite) g2.setColor(Color.WHITE);
        else g2.setColor(Color.BLACK);

        for (int i = 0; i < screenHeight; i += 40) {
            g2.fillRect(0, i, screenWidth, 20);
        }
    }

    private void drawBattle(Graphics2D g2) {
        // defensive null checks
        if (playerCock == null || enemyCock == null) {
            g2.setColor(Color.GRAY);
            g2.fillRect(0, 0, screenWidth, screenHeight);
            g2.setColor(Color.WHITE);
            g2.drawString("Loading battle...", 50, 50);
            return;
        }

        if (battleBg != null) {
            g2.drawImage(battleBg, 0, 0, screenWidth, screenHeight, null);
        } else {
            g2.setColor(Color.GRAY);
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        int enemyBaseX = 450, enemyBaseY = 130;
        int playerBaseX = 50, playerBaseY = 300;

        if (battleBase != null) {
            g2.drawImage(battleBase, enemyBaseX, enemyBaseY, 250, 100, null);
            g2.drawImage(battleBase, playerBaseX, playerBaseY, 250, 100, null);
        }

        if (enemyCock.getFrontSprite() != null) {
            g2.drawImage(enemyCock.getFrontSprite(), enemyBaseX + 50, enemyBaseY - 60, 150, 150, null);
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(enemyBaseX + 80, enemyBaseY - 50, 64, 64);
        }

        if (playerCock.getBackSprite() != null) {
            g2.drawImage(playerCock.getBackSprite(), playerBaseX + 50, playerBaseY - 60, 150, 150, null);
        } else {
            g2.setColor(Color.BLUE);
            g2.fillRect(playerBaseX + 80, playerBaseY - 50, 64, 64);
        }

        drawHPBar(g2, enemyCock, 50, 50);
        drawHPBar(g2, playerCock, 500, 320);

        int boxX = 0, boxY = 430, boxW = screenWidth, boxH = 150;
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(boxX, boxY, boxW, boxH);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(boxX + 5, boxY + 5, boxW - 10, boxH - 10);

        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.drawString(battleMessage, boxX + 30, boxY + 40);

        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.setColor(Color.YELLOW);
        List<Skill> skills = playerCock.getSkills();
        if (!skills.isEmpty()) g2.drawString("[1] " + skills.get(0).getName(), boxX + 30, boxY + 80);
        if (skills.size() > 1) g2.drawString("[2] " + skills.get(1).getName(), boxX + 30, boxY + 110);
        if (skills.size() > 2) g2.drawString("[3] " + skills.get(2).getName(), boxX + 300, boxY + 80);
    }

    private void drawHPBar(Graphics2D g2, Rooster r, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillRoundRect(x, y, 220, 70, 15, 15);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(r.getName(), x + 10, y + 25);

        g2.setColor(Color.GRAY);
        g2.fillRect(x + 10, y + 35, 200, 10);

        double hpPercent = (double) r.getHp() / r.getMaxHp();
        if (hpPercent > 0.5) g2.setColor(Color.GREEN);
        else if (hpPercent > 0.2) g2.setColor(Color.ORANGE);
        else g2.setColor(Color.RED);

        g2.fillRect(x + 10, y + 35, (int) (200 * Math.max(0, Math.min(1.0, hpPercent))), 10);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString(r.getHp() + "/" + r.getMaxHp(), x + 10, y + 60);
    }

    private int getCenteredX(Graphics2D g2, String text) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return screenWidth / 2 - length / 2;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        switch (gameState) {
            case STATE_TITLE -> {
                if (code == KeyEvent.VK_ENTER) {
                    if (musicManager != null) musicManager.playSound(GameConstants.SOUND_MENU_ACCEPT);
                    gameState = STATE_STARTER_SELECT;
                    if (musicManager != null) musicManager.playMusic(GameConstants.MUSIC_ADVENTURE, true);
                }
                if (code == KeyEvent.VK_M && musicManager != null) musicManager.toggleMute();
            }
            case STATE_STARTER_SELECT -> {
                if (code == KeyEvent.VK_W) {
                    if (musicManager != null) musicManager.playSound(GameConstants.SOUND_MENU_MOVE);
                    starterIndex--;
                    if (starterIndex < 0) starterIndex = 2;
                }
                if (code == KeyEvent.VK_S) {
                    if (musicManager != null) musicManager.playSound(GameConstants.SOUND_MENU_MOVE);
                    starterIndex++;
                    if (starterIndex > 2) starterIndex = 0;
                }
                if (code == KeyEvent.VK_ENTER) {
                    if (musicManager != null) {
                        musicManager.playSound(GameConstants.SOUND_MENU_ACCEPT);
                        musicManager.playSound(GameConstants.SOUND_ROOSTER);
                    }
                    playerCock = RoosterFactory.createRooster(starterIndex + 1, "My Manok");
                    gameState = STATE_ROAMING;
                }
                if (code == KeyEvent.VK_M && musicManager != null) musicManager.toggleMute();
            }
            case STATE_ROAMING -> {
                activeKeys.add(code);
                if (code == KeyEvent.VK_M && musicManager != null) musicManager.toggleMute();
            }
            case STATE_BATTLE -> {
                if (code == KeyEvent.VK_1) executeTurn(0);
                if (code == KeyEvent.VK_2) executeTurn(1);
                if (code == KeyEvent.VK_3) executeTurn(2);
                if (code == KeyEvent.VK_M && musicManager != null) musicManager.toggleMute();
            }
        }
    }

    private void executeTurn(int skillIndex) {
        if (playerCock == null || enemyCock == null) return;
        if (skillIndex >= playerCock.getSkills().size()) return;

        Skill skill = playerCock.getSkills().get(skillIndex);

        // Play skill-specific sound effect
        if (musicManager != null && skill.getSoundEffect() != null) {
            musicManager.playSound(skill.getSoundEffect());
        }

        int dmg = playerCock.attack(enemyCock, skill);
        if (musicManager != null) musicManager.playSound(GameConstants.SOUND_HIT);
        battleMessage = "Used " + skill.getName() + "! Dealt " + dmg + " dmg.";
        repaint();

        if (enemyCock.isFainted()) {
            battleMessage = "Enemy fainted! You Win!";
            if (musicManager != null) musicManager.playSound(GameConstants.SOUND_VICTORY);
            endBattle(true);
            return;
        }

        Timer enemyTimer = new Timer(1000, evt -> {
            List<Skill> eSkills = enemyCock.getSkills();
            Skill eSkill = eSkills.get(new Random().nextInt(eSkills.size()));

            // Play enemy skill sound effect
            if (musicManager != null && eSkill.getSoundEffect() != null) {
                musicManager.playSound(eSkill.getSoundEffect());
            }
            int eDmg = enemyCock.attack(playerCock, eSkill);
            if (musicManager != null) musicManager.playSound(GameConstants.SOUND_IMPACT);

            battleMessage = "Enemy used " + eSkill.getName() + "! (-" + eDmg + ")";
            repaint();

            if (playerCock.isFainted()) {
                battleMessage = "You fainted! Rushing home...";
                if (musicManager != null) musicManager.playSound(GameConstants.SOUND_DEFEAT);
                endBattle(false);
            }
        });
        enemyTimer.setRepeats(false);
        enemyTimer.start();
    }

    private void endBattle(boolean win) {
        Timer t = new Timer(2000, evt -> {
            if (!win) {
                player.x = tileSize * 11;
                player.y = tileSize * 9;
                if (playerCock != null) playerCock.healFull();
            }
            gameState = STATE_ROAMING;
            if (musicManager != null) musicManager.playMusic(GameConstants.MUSIC_ADVENTURE, true);
        });
        t.setRepeats(false);
        t.start();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        activeKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}