package src.main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import src.models.GanttSlice;
import src.models.JobLoader;
import src.models.JobReader;
import src.models.PCB;

public class App {
  private static final Scanner SCANNER = new Scanner(System.in);
  private static final int RR_TIME_QUANTUM_MS = 5;

  public static void main(String[] args) {
    Queue<PCB> jobQueue = new ArrayDeque<>();
    Queue<PCB> readyQueue = new ArrayDeque<>();

    JobReader task1 = new JobReader("job.txt", jobQueue);
    Thread jobReaderThread = new Thread(task1, "JobReader"); // Thread 1
    jobReaderThread.start();

    JobLoader task2 = new JobLoader(jobQueue, readyQueue, task1);
    Thread jobLoaderThread = new Thread(task2, "JobLoader"); // Thread 2
    jobLoaderThread.start();

    try {
      jobReaderThread.join();
      task2.setAllProcessesDone(true);
      jobLoaderThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Interrupted");
    }

    int choice = askUser();
    List<GanttSlice> executionOrder;
    switch (choice) {
      case 1:
        executionOrder = shortestJobFirst(readyQueue);
        break;
      case 2:
        executionOrder = roundRobin(readyQueue);
        break;
      case 3:
        executionOrder = priorityNonPreemptive(readyQueue);
        break;
      default:
        System.out.println("Invalid choice. Defaulting to Shortest Job First.");
        executionOrder = shortestJobFirst(readyQueue);
        break;
    }

    printGanttChart(executionOrder);
  }

  private static int askUser() {
    System.out.println("Select a scheduling algorithm:");
    System.out.println("1) Shortest Job First");
    System.out.println("2) Round Robin (RR)");
    System.out.println("3) Priority Scheduling (Non-Preemptive)");
    System.out.print("Enter choice (1-3): ");

    while (!SCANNER.hasNextInt()) {
      System.out.print("Please enter 1, 2, or 3: ");
      SCANNER.next();
    }

    int choice = SCANNER.nextInt();
    return choice;
  }

  private static List<GanttSlice> shortestJobFirst(Queue<PCB> readyQueue) {
    System.out.println("Running Shortest Job First scheduling");

    List<PCB> ordered = new ArrayList<>();
    while (!readyQueue.isEmpty()) {
      ordered.add(readyQueue.poll());
    }
    // sort PCB by burst time ascending
    ordered.sort((left, right) -> Integer.compare(left.getCpuBurstTime(), right.getCpuBurstTime()));

    List<GanttSlice> slices = new ArrayList<>();
    int currentTime = 0;
    for (PCB pcb : ordered) {
      int burst = pcb.getCpuBurstTime();
      int startTime = currentTime;
      int endTime = currentTime + burst;
      slices.add(new GanttSlice(pcb.getProcessId(), startTime, endTime, burst, 0));
      currentTime = endTime;
    }

    return slices;
  }

  private static List<GanttSlice> roundRobin(Queue<PCB> readyQueue) {
    System.out.println("Running Round Robin scheduling");

    List<GanttSlice> slices = new ArrayList<>();
    int currentTime = 0;

    while (!readyQueue.isEmpty()) {
      PCB pcb = readyQueue.poll();
      int remaining = pcb.getRemainingBurst();
      if (remaining <= 0) {
        continue;
      }

      int slice = Math.min(remaining, RR_TIME_QUANTUM_MS);
      int startTime = currentTime;
      int endTime = currentTime + slice;
      int startBurst = remaining;
      int endBurst = remaining - slice;

      slices.add(new GanttSlice(pcb.getProcessId(), startTime, endTime, startBurst, endBurst));
      currentTime = endTime;
      pcb.setRemainingBurst(endBurst);

      if (endBurst > 0) {
        readyQueue.offer(pcb);
      }
    }

    return slices;
  }

  private static List<GanttSlice> priorityNonPreemptive(Queue<PCB> readyQueue) {
    System.out.println("Running Priority (Non-Preemptive) scheduling");

    List<PCB> ordered = new ArrayList<>();
    while (!readyQueue.isEmpty()) {
      ordered.add(readyQueue.poll());
    }
    // sort PCB by priority ascending, tie break: by burst time ascending
    ordered.sort((left, right) -> {
      int priorityCompare = Integer.compare(left.getPriority(), right.getPriority());
      if (priorityCompare != 0) {
        return priorityCompare;
      }
      return Integer.compare(left.getCpuBurstTime(), right.getCpuBurstTime());
    });

    List<GanttSlice> slices = new ArrayList<>();
    int currentTime = 0;
    for (PCB pcb : ordered) {
      int burst = pcb.getCpuBurstTime();
      int startTime = currentTime;
      int endTime = currentTime + burst;
      slices.add(new GanttSlice(pcb.getProcessId(), startTime, endTime, burst, 0));
      currentTime = endTime;
    }

    return slices;
  }

  private static void printGanttChart(List<GanttSlice> executionOrder) {
    int totalTime = 0;
    int maxPid = 0;
    for (GanttSlice s : executionOrder) {
      totalTime = Math.max(totalTime, s.getEndTime());
      maxPid = Math.max(maxPid, s.getProcessId());
    }

    int width = Math.min(Math.max(totalTime, 1), 60);
    double scale = totalTime > width ? (double) width / totalTime : 1.0;

    Map<Integer, char[]> lanes = new HashMap<>();
    for (int pid = 1; pid <= maxPid; pid++) {
      char[] lane = new char[width];
      Arrays.fill(lane, ' ');
      lanes.put(pid, lane);
    }

    for (GanttSlice s : executionOrder) {
      int pid = s.getProcessId();
      int startCol = (int) Math.round(s.getStartTime() * scale);
      int endCol = (int) Math.round(s.getEndTime() * scale);
      if (endCol <= startCol) {
        endCol = startCol + 1;
      }
      char[] lane = lanes.get(pid);
      for (int c = startCol; c < endCol && c < width; c++) {
        lane[c] = '=';
      }
    }

    System.out.println("Gantt chart:");

    StringBuilder ruler = new StringBuilder();
    ruler.append("Time  : ");
    for (int c = 0; c < width; c++) {
      ruler.append((c % 5 == 0) ? '|' : '-');
    }
    System.out.println(ruler.toString());

    StringBuilder labels = new StringBuilder();
    labels.append("       ");
    for (int pos = 0; pos < width; pos += 5) {
      int time = (int) Math.round(pos / scale);
      labels.append(String.format("%5d", time));
    }
    System.out.println(labels.toString());

    for (int pid = 1; pid <= maxPid; pid++) {
      StringBuilder line = new StringBuilder();
      line.append(String.format("P%-3d: ", pid));
      line.append(new String(lanes.get(pid)));
      System.out.println(line.toString());
    }
  }
}
