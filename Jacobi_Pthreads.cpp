//Credit: textbook

//Compile with
//g++ -pthread -fpermissive ./Jacobi_Pthreads.cpp
//run with:
//./a.out  GRIDSIZE NUMWORKERS ITERATIONS

#include <pthread.h>
#include <semaphore.h>
#include <stdio.h>
#include <sys/times.h>
#include <cstdlib>

#define SHARED 1
#define MAXGRID 258
#define MAXWORKERS 16

void *Worker(void *);
void Barrier();

struct tms buffer; /* used for timing */
clock_t start, finish;

pthread_mutex_t barrier; // mutex semaphore for the barrier
pthread_cond_t go;       // condition variable
int numArrived = 0;      // number who have arrived

int gridSize, numWorkers, numIters, stripSize;
double maxDiff[MAXWORKERS];
double grid[MAXGRID][MAXGRID], newGrid[MAXGRID][MAXGRID];

int main(int argc, char *argv[])
{
    // thread ids and attributes
    pthread_t workerid[MAXWORKERS];
    pthread_attr_t attr;
    int i, j;
    double maxdiff = 0.0;

    // set global thread attributes
    pthread_attr_init(&attr);
    pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);

    // read command line
    // assume gridSize is a multiple of numWorkers
    gridSize = atoi(argv[1]);
    numWorkers = atoi(argv[2]);
    numIters = atoi(argv[3]);
    stripSize = gridSize / numWorkers;

    for (int i = 0; i <= gridSize + 1; i++)
        for (int j = 0; j <= gridSize + 1; j++)
        {
            grid[i][j] = 0.0;
            newGrid[i][j] = 0.0;
        }
    for (int i = 0; i <= gridSize + 1; i++)
    {
        grid[i][0] = 1.0;
        grid[i][gridSize + 1] = 1.0;
        newGrid[i][0] = 1.0;
        newGrid[i][gridSize + 1] = 1.0;
    }
    for (int j = 0; j <= gridSize + 1; j++)
    {
        grid[0][j] = 1.0;
        newGrid[0][j] = 1.0;
        grid[gridSize + 1][j] = 1.0;
        newGrid[gridSize + 1][j] = 1.0;
    }

    // mutex and condition for barrier
    pthread_mutex_init(&barrier, NULL);
    pthread_cond_init(&go, NULL);

    start = times(&buffer);
    /* create the workers, then wait for them to finish */
    for (i = 0; i < numWorkers; i++)
        pthread_create(&workerid[i], &attr, Worker, (void *)i);
    for (i = 0; i < numWorkers; i++)
        pthread_join(workerid[i], NULL);

    finish = times(&buffer);

    /* print the results */
    for (i = 0; i < numWorkers; i++)
        if (maxdiff < maxDiff[i])
        {
            maxdiff = maxDiff[i];
        }
    printf("Maximum difference:  %e\n", maxdiff);
    printf("Duration:  %d\n", finish - start);
}

void *Worker(void *arg)
{
    int myid = (int)arg;
    double mydiff, temp;
    int i, j, iters, firstRow, lastRow;

    // determine first and last rows of my strip of the grids
    firstRow = myid * stripSize + 1;
    lastRow = firstRow + stripSize - 1;

    for (iters = 1; iters <= numIters; iters++)
    {
        // update my points
        for (i = firstRow; i <= lastRow; i++)
        {
            for (j = 1; j <= gridSize; j++)
            {
                newGrid[i][j] = (grid[i - 1][j] + grid[i + 1][j] +
                                 grid[i][j - 1] + grid[i][j + 1]) *
                                0.25;
            }
        }
        Barrier();
        // update my points again
        for (i = firstRow; i <= lastRow; i++)
        {
            for (j = 1; j <= gridSize; j++)
            {
                grid[i][j] = (newGrid[i - 1][j] + newGrid[i + 1][j] +
                              newGrid[i][j - 1] + newGrid[i][j + 1]) *
                             0.25;
            }
        }
        Barrier();
    }
    // compute the maximum difference in my strip
   mydiff = 0.0;
  for (i = firstRow; i <= lastRow; i++) {
    for (j = 1; j <= gridSize; j++) {
      temp = grid[i][j]-newGrid[i][j];
      if (temp < 0)
        temp = -temp;
      if (mydiff < temp)
        mydiff = temp;
    }
  }
  maxDiff[myid] = mydiff;
}

void Barrier()
{
    pthread_mutex_lock(&barrier);
    numArrived++;
    if (numArrived == numWorkers)
    {
        numArrived = 0;
        pthread_cond_broadcast(&go);
    }
    else
        pthread_cond_wait(&go, &barrier);
    pthread_mutex_unlock(&barrier);
}