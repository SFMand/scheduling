package src.main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import src.models.GanttSlice;
import src.models.JobLoader;
import src.models.JobReader;
import src.models.PCB;

public class App {
  private static final Scanner SCANNER = new Scanner(System.in);
  private static final int RR_TIME_QUANTUM_MS = 5;
  private static final int PRIORITY_AGING_INTERVAL_MS = 4;
  private static final int PRIORITY_STARVATION_UNIT_MS = 5;
  private static final int PRIORITY_MIN_LEVEL = 1;

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

    List<PCB> waiting = new ArrayList<>();
    while (!readyQueue.isEmpty()) {
      waiting.add(readyQueue.poll());
    }

    List<GanttSlice> slices = new ArrayList<>();
    Set<Integer> starvedProcessesReported = new HashSet<>();
    int currentTime = 0;

    while (!waiting.isEmpty()) {
      reportStarvedProcesses(waiting, starvedProcessesReported);

      PCB pcb = selectNextPriorityProcess(waiting);
      int burst = pcb.getCpuBurstTime(); // 13
      int startTime = currentTime; // 0
      int endTime = currentTime + burst; // 0 + 13 = 13
      slices.add(new GanttSlice(pcb.getProcessId(), startTime, endTime, burst, 0));
      pcb.setWaitingTime(startTime); // 0
      currentTime = endTime; // 13

      if (!waiting.isEmpty()) {
        int agingRounds = burst / PRIORITY_AGING_INTERVAL_MS;
        if (agingRounds > 0) {
          ageWaitingProcesses(waiting, agingRounds);
        }
        updateWaitingTimes(waiting, burst);
        reportStarvedProcesses(waiting, starvedProcessesReported);
      }
    }

    return slices;
  }

  private static PCB selectNextPriorityProcess(List<PCB> waiting) {
    PCB selected = waiting.get(0);
    for (PCB pcb : waiting) {
      int priorityCompare = Integer.compare(pcb.getPriority(), selected.getPriority());
      if (priorityCompare < 0) {
        selected = pcb;
        continue;
      }
      if (priorityCompare == 0) {
        int burstCompare = Integer.compare(pcb.getCpuBurstTime(), selected.getCpuBurstTime());
        if (burstCompare < 0 || (burstCompare == 0 && pcb.getProcessId() < selected.getProcessId())) {
          selected = pcb;
        }
      }
    }

    waiting.remove(selected);
    return selected;
  }

  private static void ageWaitingProcesses(List<PCB> waiting, int agingRounds) {
    for (int round = 0; round < agingRounds; round++) {
      for (PCB pcb : waiting) {
        pcb.setPriority(Math.max(PRIORITY_MIN_LEVEL, pcb.getPriority() - 1));
      }
    }

    if (agingRounds > 0) {
      System.out.printf("Aging applied: waiting processes improved after %d ms of CPU time.%n",
          agingRounds * PRIORITY_AGING_INTERVAL_MS);
    }
  }

  private static void updateWaitingTimes(List<PCB> waiting, int elapsedTime) {
    for (PCB pcb : waiting) {
      pcb.setWaitingTime(pcb.getWaitingTime() + elapsedTime);
    }
  }

  private static void reportStarvedProcesses(List<PCB> waiting, Set<Integer> starvedProcessesReported) {
    if (waiting.isEmpty()) {
      return;
    }

    int readyQueueSize = waiting.size();
    int starvationThreshold = readyQueueSize * PRIORITY_STARVATION_UNIT_MS;
    for (PCB pcb : waiting) {
      if (pcb.getWaitingTime() > starvationThreshold && starvedProcessesReported.add(pcb.getProcessId())) {
        System.out.printf(
            "Starvation detected: P%d waited %d ms with %d process(es) in the ready queue.%n",
            pcb.getProcessId(), pcb.getWaitingTime(), readyQueueSize);
      }
    }
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

    final String timelinePrefix = "Time  : ";

    StringBuilder ruler = new StringBuilder();
    ruler.append(timelinePrefix);
    for (int c = 0; c < width; c++) {
      ruler.append((c % 5 == 0) ? '|' : '-');
    }
    System.out.println(ruler.toString());

    StringBuilder labels = new StringBuilder();
    labels.append(timelinePrefix);
    for (int pos = 0; pos < width; pos += 5) {
      int time = (int) Math.round(pos / scale);
      labels.append(String.format("%-5d", time));
    }
    System.out.println(labels.toString());

    for (int pid = 1; pid <= maxPid; pid++) {
      StringBuilder line = new StringBuilder();
      line.append(String.format("P%-3d  : ", pid));
      line.append(new String(lanes.get(pid)));
      System.out.println(line.toString());
    }
  }
}
