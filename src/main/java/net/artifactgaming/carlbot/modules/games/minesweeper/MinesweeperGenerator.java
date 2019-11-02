package net.artifactgaming.carlbot.modules.games.minesweeper;

public class MinesweeperGenerator {
    public static final int rows = 10;
    public static final int columns = 10;

    private static final int guaranteedMinesCount = 14;

    public static MinesweeperTile[][] generateField(){

        MinesweeperTile[][] playField = createEmptyField();

        for (int mineCreated = 0; mineCreated < guaranteedMinesCount; ++ mineCreated){
            int xPosToPlaceMine;
            int yPosToPlaceMine;
            // Generate a new position to place a mine.
            // (ensure that the position does not have a mine currently.)
            do {
                xPosToPlaceMine = (int) (Math.random() * rows);
                yPosToPlaceMine = (int) (Math.random() * columns);
            } while (!playField[xPosToPlaceMine][yPosToPlaceMine].isSafe());

            UpdateTileWithMine(playField, xPosToPlaceMine, yPosToPlaceMine);
        }

        return playField;
    }

    private static void UpdateTileWithMine(MinesweeperTile[][] tiles, int x, int y){
        tiles[x][y].setSafe(false);

        // Update surrounding tiles
        for (int row = -1; row <= 1; ++row){
            for (int column = -1; column <= 1; ++column){
                int currentX = row + x;
                int currentY = column + y;

                // Ignore self or out-of-bounds.
                if (currentX < 0 || currentY < 0 || (currentY == y && currentX == x) || currentX >= rows || currentY >= columns ){
                    continue;
                } else {
                    // Increase mine count on surrounding tile.
                    int newMineCount = tiles[currentX][currentY].getSurroundingMinesCount() + 1;
                    tiles[currentX][currentY].setSurroundingMinesCount(newMineCount);
                }
            }
        }
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
