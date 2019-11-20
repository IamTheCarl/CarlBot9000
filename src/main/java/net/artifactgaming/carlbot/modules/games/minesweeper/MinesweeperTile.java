package net.artifactgaming.carlbot.modules.games.minesweeper;

public class MinesweeperTile {
    private static final String BOMB_EMOTE = "\uD83D\uDCA3";

    /**
     * Use it when the tile itself is safe.
     * Index is equals to the number of bombs surrounding the tile.
     */
    private final String[] TILE_COUNT_EMOTE  = {
            "\uD83C\uDD97",
            ":one:",
            ":two:",
            ":three:",
            ":four:",
            ":five:",
            ":six:",
            ":seven:",
            ":eight:"
    };

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

    public String toEmoteString(){
        if (!isSafe){
            return BOMB_EMOTE;
        } else {
            return TILE_COUNT_EMOTE[surroundingMinesCount];
        }
    }
}
