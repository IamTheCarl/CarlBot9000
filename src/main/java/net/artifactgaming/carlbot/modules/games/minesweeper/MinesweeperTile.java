package net.artifactgaming.carlbot.modules.games.minesweeper;

public class MinesweeperTile {
    /**
     * True if there is no mine on this tile.
     */
    private boolean isSafe;

    /**
     * How many mines are surrounding this tile.
     */
    private int surroundingMinesCount;

    public MinesweeperTile() {
        isSafe = true;
        surroundingMinesCount = 0;
    }

    public boolean isSafe() {
        return isSafe;
    }

    public int getSurroundingMinesCount() {
        return surroundingMinesCount;
    }

    void setSafe(boolean safe) {
        isSafe = safe;
    }

    void setSurroundingMinesCount(int surroundingMinesCount) {
        this.surroundingMinesCount = surroundingMinesCount;
    }
}
