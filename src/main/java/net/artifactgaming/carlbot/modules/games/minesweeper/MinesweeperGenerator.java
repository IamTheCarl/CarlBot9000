package net.artifactgaming.carlbot.modules.games.minesweeper;

public class MinesweeperGenerator {
    public static final int rows = 10;
    public static final int columns = 10;

    private static final int maximumMinesCount = 12;

    public static MinesweeperTile[][] generateField(){
        MinesweeperTile[][] playField = createEmptyField();

        return playField;
    }

    private static MinesweeperTile[][] createEmptyField(){
        MinesweeperTile[][] emptyField = new MinesweeperTile[rows][columns];

        for (int x = 0; x < rows; ++x){
            for (int y = 0; y < columns; ++y){
                emptyField[x][y] = new MinesweeperTile();
            }
        }
        return emptyField;
    }
}
