package src.models;

import java.util.Queue;

public class JobLoader implements Runnable {

  private static final int TOTAL_MEMORY_MB = 2048;
  private int availableMemory = TOTAL_MEMORY_MB;

  private final Queue<PCB> jobQueue;
  private final Queue<PCB> readyQueue;
  private final JobReader jobReader;

  private volatile boolean allProcessesDone = false;

  public JobLoader(Queue<PCB> jobQueue, Queue<PCB> readyQueue, JobReader jobReader) {
    this.jobQueue = jobQueue;
    this.readyQueue = readyQueue;
    this.jobReader = jobReader;
  }

  public synchronized void releaseMemory(int memoryMB) {
    this.availableMemory += memoryMB;
    notifyAll();
  }

  public void setAllProcessesDone(boolean done) {
    this.allProcessesDone = done;
  }

  @Override
  public void run() {
    try {
      while (true) {
        PCB nextJob = null;

        // peek at next job safely to make sure no 2 threads enter at the same time
        synchronized (jobQueue) {
          if (!jobQueue.isEmpty()) {
            nextJob = jobQueue.peek();
          }
        }

        // if no job is available, check for termination or sleep briefly
        if (nextJob == null) {
          if (jobReader.isFinished() && allProcessesDone) {
            break; // all jobs have loaded and finished so we terminate thread 2
          }
          Thread.sleep(50); // prevent busy-waiting while Thread 1 loads files
          continue;
        }

        // we wait until there is enough memory available for the next job
        synchronized (this) {
          while (nextJob.getMemoryRequired() > availableMemory) {
            wait(); // thread pauses here until releaseMemory() is called (it invokes notifyAll())
          }

          // safely remove the job from the job queue and allocate memory ensuring only 1
          // thread enters
          synchronized (jobQueue) {
            // double check the job is still there because of the time gap resulting from
            // wait()
            if (!jobQueue.isEmpty() && jobQueue.peek() == nextJob) {
              jobQueue.poll();
              availableMemory -= nextJob.getMemoryRequired();
            } else {
              continue;
            }
          }
        }

        // safely add the job to the ready queue
        nextJob.setState(PCB.State.READY);
        synchronized (readyQueue) {
          readyQueue.offer(nextJob);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}