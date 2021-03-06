import java.util.concurrent.Semaphore;

// Takes 3 command line arguements
// GRIDSIZE     NUMWORKERS     ITERATIONS
public class JacobiBetterBarrier {
    public static void main(String[] args) {
        // EDIT THESE FOR TESTING
        final int N = Integer.parseInt(args[0]); // size of grid
        final int PR = Integer.parseInt(args[1]); // num processors/workers MUST BE A FACTOR OF N
        final int MAXITER = Integer.parseInt(args[2]); //number of iterations
        
        double maxDiff = 0.0;
        Grid grid = new Grid(N, PR);

        /*
         * EDIT AND ADD MULTIPLE OF THESE TO TEST GRID VALUES 
         * grid.grid[i][j] = NUMBER;
         * grid.newGrid[i][j] = NUMBER;
         */

        Thread[] workers = new Thread[PR];

        // START time
        long startTime = System.nanoTime();

        // Run Jacobi Algorithm
        for (int i = 0; i < PR; i++) {
            workers[i] = new Thread(new Worker(i + 1, grid, MAXITER));
            workers[i].start();
        }

        try {
            for (Thread thread : workers) {
                thread.join();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        // END time
        long endTime = System.nanoTime();

        for (double max : grid.maxDiff) {
            if (max > maxDiff) {
                maxDiff = max;
            }
        }

        long duration = (endTime - startTime);

        //Print out time results
        System.out.println("The maximum difference is: " + maxDiff);
        System.out.println("The duration is: " + duration / 1000 + " ms");

        //COMMENT OUT IF USING LARGE GRID
        // printArray(grid.grid);

    }

    private static void printArray(double a[][]){
        //Print out array
        for (double[] x : a) {
            for (double y : x) {
                System.out.print(y + " ");
            }
            System.out.println();
        }
    }

}

class Grid {
    // Grid variables
    double grid[][];
    double newGrid[][];
    double maxDiff[];
    int height;
    int n;

    // Barrier Variables
    Semaphore lock[];
    Semaphore mutex = new Semaphore(1);

    final int PR;
    int workers = 0;
    boolean wait[];

    // Initializing the grid
    Grid(int N, int pr) {
        grid = new double[N + 2][N + 2];
        newGrid = new double[N + 2][N + 2];
        PR = pr;
        wait = new boolean[PR];
        height = N / PR;
        maxDiff = new double[PR];
        n = N;
        lock = new Semaphore[PR];

        for (int i = 0; i < PR; i++) {
            lock[i] = new Semaphore(1);
        }

        for (int i = 0; i < N + 2; i++) {
            for (int j = 0; j < N + 2; j++) {
                grid[i][j] = 0.0;
                newGrid[i][j] = 0.0;
            }
        }

        for (int i = 0; i < N + 2; i++) {
            grid[i][0] = 1.0;
            newGrid[i][0] = 1.0;
            grid[i][N + 1] = 1.0;
            newGrid[i][N + 1] = 1.0;
        }

        for (int j = 0; j < N + 2; j++) {
            grid[0][j] = 1.0;
            newGrid[0][j] = 1.0;
            grid[N + 1][j] = 1.0;
            newGrid[N + 1][j] = 1.0;
        }
    }

    public void Barrier(int id) throws InterruptedException {
        lock[id-1].acquire();
        mutex.acquire();
        workers++;

        // Reset counter
        if (workers == PR) {
            workers = 0;
            for (int i = 0; i < PR; i++) {
                lock[i].release();
            }
        }
        mutex.release();
    }
}

class Worker implements Runnable {
    int firstRow;
    int lastRow;

    Grid grid;
    double newGrid[][];
    final int ID;
    final int maxIter;

    Worker(int id, Grid g, int maxIter) {
        firstRow = (id - 1) * g.height + 1;
        lastRow = firstRow + g.height - 1;
        grid = g;
        this.maxIter = maxIter;
        ID = id;
    }

    public void run() {
        try {
            grid.Barrier(ID);
            for (int iters = 1; iters <= maxIter; iters = iters + 2) {
                double myDiff = 0.0;

                for (int i = firstRow; i <= lastRow; i++) {
                    for (int j = 1; j <= grid.n; j++) {
                        grid.newGrid[i][j] = (grid.grid[i - 1][j] + grid.grid[i + 1][j] + grid.grid[i][j + 1]
                                + grid.grid[i][j - 1]) * 0.25;
                    }
                }

                grid.Barrier(ID);

                for (int i = firstRow; i <= lastRow; i++) {
                    for (int j = 1; j <= grid.n; j++) {
                        grid.grid[i][j] = (grid.newGrid[i - 1][j] + grid.newGrid[i + 1][j] + grid.newGrid[i][j + 1]
                                + grid.newGrid[i][j - 1]) * 0.25;
                    }
                }

                grid.Barrier(ID);

                for (int i = firstRow; i <= lastRow; i++) {
                    for (int j = 1; j <= grid.n; j++) {
                        myDiff = Math.max(myDiff, Math.abs(grid.grid[i][j] - grid.newGrid[i][j]));
                    }
                }

                grid.maxDiff[ID - 1] = myDiff;

                grid.Barrier(ID);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}