package Entity;

import Core.PointUtils;
import TileEngine.TETile;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Hallways extends HashSet<Hallway> {
    /**
     * Add new hallway.
     * @param hallWay the hallway prepared for added.
     * @return True, if room is added to set, else false.
     */
    public boolean add (Hallway hallWay) {
        if (hallWay == null) {
            return false;
        }
        return super.add(hallWay);
    }

    /**
     * Fill the tiles with tiles of all hallways.
     * @param tiles the target to fill with.
     * @param blocks the set of tiles, when encounter these tiles, ignore to fill them.
     * @param floorTile tile for filling the floor of hallway.
     * @param wallTile tile for filling the wall of hallway.
     */
    public void fill(TETile[][] tiles, Set<TETile> blocks, TETile floorTile, TETile wallTile) {
        if (tiles == null) {
            throw new IllegalArgumentException("Cannot add tiles of hallways to null tiles.");
        }
        if (floorTile == null) {
            throw new IllegalArgumentException("Cannot fill null floor tile on tiles.");
        }
        if (wallTile == null) {
            throw new IllegalArgumentException("Cannot fill null wall tile on tiles.");
        }
        if (tiles.length == 0) {
            return;
        }
        if (tiles[0].length == 0) {
            return;
        }
        int minX = 0;
        int maxX = tiles.length - 1;
        int minY = 0;
        int maxY = tiles[0].length - 1;
        HashSet<Point> blockedPosSet = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                TETile tile = tiles[x][y];
                if (!blocks.contains(tile)) {
                    continue;
                }
                Point blockPos = new Point(x, y);
                blockedPosSet.add(blockPos);
            }
        }
        HashMap<Hallway, List<Point>> hallwayTraces = new HashMap<>();
        for (Hallway hallWay: this) {
            if (!PointUtils.inBoundary(hallWay.from, minX, minY, maxX, maxY)) {
                continue;
            }
            if (!PointUtils.inBoundary(hallWay.to, minX, minY, maxX, maxY)) {
                continue;
            }
            List<Point> trace = PointUtils.shortestTrace(hallWay.from, hallWay.to, blockedPosSet, minX, minY, maxX, maxY);
            if (trace == null) {
                System.out.println(TETile.toString(tiles));
                continue;
            }
            hallwayTraces.put(hallWay, trace);
        }
        // Fill floor of hallway.
        for (Hallway hallWay: hallwayTraces.keySet()) {
            List<Point> trace = hallwayTraces.get(hallWay);
            for (Point tracePos: trace) {
                tiles[tracePos.x][tracePos.y] = floorTile;
            }
        }
        // Fill wall of hallway.
        for (Hallway hallWay: hallwayTraces.keySet()) {
            List<Point> trace = hallwayTraces.get(hallWay);
            for (Point tracePos: trace) {
                Point[] surroundPosList = PointUtils.surroundNeighbors(tracePos);
                for (Point surroundPos: surroundPosList) {
                    if (!PointUtils.inBoundary(surroundPos, minX, minY, maxX, maxY)) {
                        continue;
                    }
                    if (blockedPosSet.contains(surroundPos)) {
                        continue;
                    }
                    TETile surroundPosTile = tiles[surroundPos.x][surroundPos.y];
                    if (surroundPosTile.equals(floorTile)) {
                        continue;
                    }
                    tiles[surroundPos.x][surroundPos.y] = wallTile;
                }
            }
        }
    }
}
