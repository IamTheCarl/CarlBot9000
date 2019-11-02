package net.artifactgaming.carlbot.modules.games.minesweeper;

import java.util.function.BooleanSupplier;

public class MinesweeperGenerator {
    public static final int rows = 10;
    public static final int columns = 10;

    private static final int maximumMinesCount = 12;

    /**
     * Increasing this value will increase the chance of mines being generated on the next go.
     * (Increases the chance of a mine being generated on the next tile, if the previous tile had no mine)
     */
    private static final int mineGenerationBias = 5;

    /**
     * Generated number needs to exceed this value in order to generate a mine.
     * (Out of 100)
     */
    private static final int mineGenerationCap = 60;

    public static MinesweeperTile[][] generateField(){
        int minesCreated = 0;
        final int[] currentMineBias = {0};

        ///region Local_Function
        BooleanSupplier willHaveMineOnCurrentTile = () -> {
            if (minesCreated >= maximumMinesCount){
                return false;
            }

            int chanceToGenerateMine = (int)((Math.random() * 100 + 1) + currentMineBias[0]);

            if (chanceToGenerateMine >= mineGenerationCap){
                currentMineBias[0] = 0;
                return true;
            } else {
                currentMineBias[0] += mineGenerationBias;
                return false;
            }
        };
        ///endregion

        MinesweeperTile[][] playField = createEmptyField();

        for (int x = 0; x < rows; ++x){
            for (int y = 0; y < columns; ++y){
                if (willHaveMineOnCurrentTile.getAsBoolean()){
                    UpdateTileWithMine(playField, x, y);
                }
            }
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
