package UserInterfaceEngine;

import Core.Config;
import Entity.World;
import Input.InputDevice;
import Shape.Direction;
import TileEngine.TETile;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.*;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class WorldInterface extends BaseInterface {
    /**
     * Status bar height in row count.
     */
    private final int STATUS_BAR_ROW_COUNT = 1;
    /**
     * Key, if pressed, trigger the userInterface into query command mode.
     */
    private final char QUERY_COMMAND_KEY = ':';
    /**
     * Command for quitting game.
     */
    private final char QUIT_GAME_COMMAND = 'q';
    /**
     * Boundary coordinate of canvas.
     */
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    /**
     * Side size of tile(square) in pixel.
     */
    private final int tileSize;
    private final World world;
    private final TETile[][] tiles;
    private final HashMap<Character, Direction> directionKeyMap;
    private boolean inQueryCommandMode;
    private boolean inEndingDialogMode;
    private final StringBuilder inputCommandSB;

    private class RefreshTask extends TimerTask {
        private final Runnable command;

        public RefreshTask(Runnable command) {
            this.command = command;
        }

        @Override
        public void run () {
            if (command != null) {
                command.run();
            }
        }
    }

    public WorldInterface(Config config, World world) {
        super(config);
        if (world == null) {
            throw new IllegalArgumentException("Cannot instantiate world userInterface with null world.");
        }
        this.minX = 0;
        this.maxX = world.width();
        this.minY = 0;
        this.maxY = world.height() + STATUS_BAR_ROW_COUNT;
        this.tileSize = width() / world.width();
        this.world = world;
        this.tiles = world.tiles();
        this.directionKeyMap = new HashMap<>();
        this.inputCommandSB = new StringBuilder();
        initialize();
    }

    public WorldInterface(Config config, long seed) {
        super(config);
        this.minX = 0;
        this.maxX = config.worldWidth;
        this.minY = 0;
        this.maxY = config.worldHeight + STATUS_BAR_ROW_COUNT;
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        this.tileSize = Math.min(width() / sizeX, height() / sizeY);
        this.world = new World(config.worldWidth, config.worldHeight);
        this.world.randWorld(seed);
        this.tiles = world.tiles();
        this.directionKeyMap = new HashMap<>();
        this.inputCommandSB = new StringBuilder();
        initialize();
    }

    private void initialize() {
        directionKeyMap.put('w', Direction.TOP);
        directionKeyMap.put('d', Direction.RIGHT);
        directionKeyMap.put('s', Direction.BOTTOM);
        directionKeyMap.put('a', Direction.LEFT);
        inQueryCommandMode = false;
        inEndingDialogMode = false;
        inputCommandSB.setLength(0);
        if (!config.hideInterface) {
            StdDraw.setCanvasSize(width(), height());
            StdDraw.setXscale(minX, maxX);
            StdDraw.setYscale(minY, maxY);
        }
    }

    private void showStatus() {
        if (config.hideInterface) {
            return;
        }
        // Draw status bar background.
        final Color STATUS_BAR_COLOR = Color.BLACK;
        StdDraw.setPenColor(STATUS_BAR_COLOR);
        double statusBarWidth = maxX - minX + 1;
        double statusBarHeight = STATUS_BAR_ROW_COUNT;
        double statusBarCenterX = minX + (statusBarWidth / 2);
        double statusBarCenterY =  maxY - (statusBarHeight / 2);
        StdDraw.filledRectangle(statusBarCenterX, statusBarCenterY, statusBarWidth / 2, statusBarHeight / 2);
        // Draw status text
        final Color PEN_COLOR = Color.WHITE;
        StdDraw.setPenColor(PEN_COLOR);
        final Font STATUS_TEXT_FONT = new Font("Monaco", Font.BOLD, tileSize - 2);
        StdDraw.setFont(STATUS_TEXT_FONT);
        String statusText;
        if (inQueryCommandMode) {
            statusText = inputCommandSB.toString();
        } else {
            // Draw mouse currently pointing tile.
            int tileX = (int) Math.floor(StdDraw.mouseX());
            int tileY = (int) Math.floor(StdDraw.mouseY());
            if (0 < tiles.length && tileX < tiles.length && tileY < tiles[0].length) {
                statusText = tiles[tileX][tileY].description();
            } else {
                statusText = "";
            }
        }
        StdDraw.textLeft(minX, statusBarCenterY, statusText);
        // Draw bottom line of status bar.
        final double STATUS_BAR_BOTTOM_LINE_Y = maxY - STATUS_BAR_ROW_COUNT;
        StdDraw.line(minX, STATUS_BAR_BOTTOM_LINE_Y, maxX, STATUS_BAR_BOTTOM_LINE_Y);
        StdDraw.show();
    }

    private void showEndingDialog(String msg) {
        if (config.hideInterface) {
            return;
        }
        // Draw dialog background.
        final Color DIALOG_BK_COLOR = Color.BLACK;
        StdDraw.setPenColor(DIALOG_BK_COLOR);
        double dialogWidth = (double) (maxX - minX + 1) / 3;
        double dialogHeight = (double) (maxY - minY + 1) / 2;
        double dialogCenterX = minX + (maxX - minX + 1 - dialogWidth) / 2 + (dialogWidth / 2);
        double dialogCenterY = maxY - (maxY - minY + 1 - dialogHeight) / 2 - (dialogHeight / 2);
        StdDraw.filledRectangle(dialogCenterX, dialogCenterY, dialogWidth / 2, dialogHeight / 2);
        // Draw msg text
        final Color DIALOG_PEN_COLOR = Color.WHITE;
        StdDraw.setPenColor(DIALOG_PEN_COLOR);
        final Font DIALOG_TEXT_FONT = new Font("Monaco", Font.BOLD, tileSize - 2);
        StdDraw.setFont(DIALOG_TEXT_FONT);
        StdDraw.text(dialogCenterX, dialogCenterY, msg);
        // Draw exit button
        StdDraw.text(dialogCenterX, dialogCenterY - 1, "press \"" + QUIT_GAME_COMMAND + "\" to exit game");
        StdDraw.show();
        // Update status
        inEndingDialogMode = true;
    }

    private void showWorldTiles() {
        if (config.hideInterface) {
            return;
        }
        TETile[][] worldTiles = world.tiles();
        // Draw world tiles background.
        final Color WORLD_COLOR = Color.BLACK;
        StdDraw.setPenColor(WORLD_COLOR);
        double worldWidth = maxX - minX;
        double worldHeight = maxY - minY - STATUS_BAR_ROW_COUNT;
        double worldCenterX = (minX + maxX) / 2;
        double worldCenterY = (minY + maxY - STATUS_BAR_ROW_COUNT) / 2;
        StdDraw.filledRectangle(worldCenterX, worldCenterY, worldWidth / 2, worldHeight / 2);
        // Draw tiles of world.
        int worldXSize = worldTiles.length;
        if (worldWidth <= 0) {
            return;
        }
        int worldYSize = worldTiles[0].length;
        for (int x = 0; x < worldXSize; x += 1) {
            for (int y = 0; y < worldYSize; y += 1) {
                if (worldTiles[x][y] == null) {
                    throw new IllegalArgumentException("Tile at position x=" + x + ", y=" + y
                            + " is null.");
                }
                worldTiles[x][y].draw(x, y);
            }
        }
        StdDraw.show();
    }

    @Override
    public void show() {
        if (config.hideInterface) {
            return;
        }
        final Color BACKGROUND_COLOR = Color.BLACK;
        StdDraw.clear(BACKGROUND_COLOR);
        showStatus();
        showWorldTiles();
    }

    @Override
    public void start(InputDevice inputDevice) {
        if (inputDevice == null) {
            throw new IllegalArgumentException("No input to world interface.");
        }
        show();
        TimerTask refreshStatusTask = new RefreshTask(this::showStatus);
        Timer refreshTimer = new Timer();
        try {
            refreshTimer.scheduleAtFixedRate(refreshStatusTask, new Date(), 100);
            while (inputDevice.possibleNextInput()) {
                char gotKey = Character.toLowerCase(inputDevice.getNextKey());
                if (inQueryCommandMode) {
                    inputCommandSB.append(gotKey);
                    if (gotKey == QUIT_GAME_COMMAND) {
                        // Save and exit.
                        saveWorld();
                        System.out.println(TETile.toString(world.tiles()));
                        System.exit(0);
                    }
                } if (inEndingDialogMode) {
                    if (gotKey == QUIT_GAME_COMMAND) {
                        System.out.println(TETile.toString(world.tiles()));
                        System.exit(0);
                    }
                } else {
                    if (gotKey == QUERY_COMMAND_KEY) {
                        inQueryCommandMode = true;
                        inputCommandSB.append(gotKey);
                    } else if (directionKeyMap.containsKey(gotKey)) {
                        Direction moveDirection = directionKeyMap.get(gotKey);
                        world.moveUser(moveDirection);
                        if (world.foundGoal()) {
                            showEndingDialog("Congratulation! You find the treasure!");
                        } else {
                            showWorldTiles();
                        }
                    }
                }
            }
        } finally {
            refreshTimer.cancel();
        }
    }

    /**
     * Save current world object to file.
     */
    private void saveWorld() {
        File file = new File(config.FILE_NAME);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutStream = new FileOutputStream(file);
            ObjectOutputStream objectOutStream = new ObjectOutputStream(fileOutStream);
            objectOutStream.writeObject(world);
        }  catch (FileNotFoundException e) {
            System.out.println("file not found");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Current state of world's tiles.
     */
    public TETile[][] worldTiles() {
        return world.tiles();
    }
}
