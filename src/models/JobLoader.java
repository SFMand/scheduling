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

        // Wait until there's a job available in the queue
        while (nextJob == null) {
          synchronized (jobQueue) {
            if (!jobQueue.isEmpty()) {
              nextJob = jobQueue.peek();
              break;
            }
          }

          // No job available - check termination or sleep
          if (jobReader.isFinished() && allProcessesDone) {
            return; // All jobs loaded and processed
          }

          Thread.sleep(50); // Brief sleep to prevent busy-waiting
        }

        // Wait for sufficient memory to load this job, then allocate and add to ready
        // queue
        // This entire operation must be atomic to prevent jobs from appearing out of
        // order
        synchronized (this) {
          while (nextJob.getMemoryRequired() > availableMemory) {
            wait(); // Pause until memory becomes available via releaseMemory()
          }

          // Memory is now available - allocate it and remove from job queue
          synchronized (jobQueue) {
            // Safe to poll since we're the only thread that removes from jobQueue
            if (!jobQueue.isEmpty() && jobQueue.peek() == nextJob) {
              jobQueue.poll();
              availableMemory -= nextJob.getMemoryRequired();
            } else {
              // Queue was cleared or modified unexpectedly - restart the loop
              nextJob = null;
              continue;
            }
          }
        }

        // Job is now allocated in memory - add to ready queue for scheduler
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