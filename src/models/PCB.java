package src.models;

public class PCB {

  public enum State {
    NEW, READY, RUNNING, TERMINATED
  }

  private int processId;
  private State state;
  private int cpuBurstTime;
  private int priority;
  private int memoryRequired;
  private int waitingTime;
  private int turnaroundTime;
  private int remainingBurst;

  public PCB(int processId, int cpuBurstTime, int priority, int memoryRequired) {
    this.processId = processId;
    this.cpuBurstTime = cpuBurstTime;
    this.priority = priority;
    this.memoryRequired = memoryRequired;

    this.state = State.NEW;
    this.waitingTime = 0;
    this.turnaroundTime = 0;
    this.remainingBurst = cpuBurstTime;
  }

  public int getProcessId() {
    return processId;
  }

  public void setProcessId(int processId) {
    this.processId = processId;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public int getCpuBurstTime() {
    return cpuBurstTime;
  }

  public void setCpuBurstTime(int cpuBurstTime) {
    this.cpuBurstTime = cpuBurstTime;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getMemoryRequired() {
    return memoryRequired;
  }

  public void setMemoryRequired(int memoryRequired) {
    this.memoryRequired = memoryRequired;
  }

  public int getWaitingTime() {
    return waitingTime;
  }

  public void setWaitingTime(int waitingTime) {
    this.waitingTime = waitingTime;
  }

  public int getTurnaroundTime() {
    return turnaroundTime;
  }

  public void setTurnaroundTime(int turnaroundTime) {
    this.turnaroundTime = turnaroundTime;
  }

  public int getRemainingBurst() {
    return remainingBurst;
  }

  public void setRemainingBurst(int remainingBurst) {
    this.remainingBurst = remainingBurst;
  }

  @Override
  public String toString() {
    return "PCB{ID=" + processId + ", Burst=" + cpuBurstTime +
        ", Priority=" + priority + ", Mem=" + memoryRequired + "MB, State=" + state + "}";
  }
}