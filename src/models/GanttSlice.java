package src.models;

public class GanttSlice {
  private final int processId;
  private final int startTime;
  private final int endTime;
  private final int startBurst;
  private final int endBurst;

  public GanttSlice(int processId, int startTime, int endTime, int startBurst, int endBurst) {
    this.processId = processId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startBurst = startBurst;
    this.endBurst = endBurst;
  }

  public int getProcessId() {
    return processId;
  }

  public int getStartTime() {
    return startTime;
  }

  public int getEndTime() {
    return endTime;
  }

  public int getStartBurst() {
    return startBurst;
  }

  public int getEndBurst() {
    return endBurst;
  }
}
