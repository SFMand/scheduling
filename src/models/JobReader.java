package src.models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Queue;

public class JobReader implements Runnable {

  private String fileName;
  private Queue<PCB> jobQueue;
  private volatile boolean finished;

  // Constructor يستقبل اسم الملف والطابور اللي بنحط فيه العمليات
  public JobReader(String fileName, Queue<PCB> jobQueue) {
    this.fileName = fileName;
    this.jobQueue = jobQueue;
    this.finished = false;
  }

  @Override
  public void run() {

    // هذا الجزء يحاول يفتح الملف ويقراه
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      String line;

      // بنقرا الملف سطر سطر لين ينتهي الملف
      while ((line = reader.readLine()) != null) {

        // بنشيل المسافات الزايدة من بداية ونهاية السطر
        line = line.trim();
        // إذا كان السطر فاضي نتجاهله ونكمل
        if (line.isEmpty()) {
          continue;
        }
        // شكل السطر في الملف يكون مثل:
        // 1:25:4;500
        // المعنى:
        // processId : cpuBurstTime : priority ; memoryRequired
        // نقسم السطر إلى جزئين:
        // الجزء الأول: 1:25:4
        // الجزء الثاني: 500
        String[] mainParts = line.split(";");

        // نقسم الجزء الأول إلى:
        // process id, burst time, priority
        String[] processParts = mainParts[0].split(":");

        // نحول القيم من String إلى int
        int processId = Integer.parseInt(processParts[0]);
        int cpuBurstTime = Integer.parseInt(processParts[1]);
        int priority = Integer.parseInt(processParts[2]);
        int memoryRequired = Integer.parseInt(mainParts[1]);

        // ننشئ PCB جديد يمثل العملية
        PCB process = new PCB(processId, cpuBurstTime, priority, memoryRequired);

        // نضيف العملية إلى jobQueue
        // استخدمنا synchronized لأن أكثر من Thread ممكن يستخدم نفس الطابور
        synchronized (jobQueue) {
          jobQueue.add(process);
        }

        // طباعة بسيطة نتأكد أن العملية انضافت
        System.out.println("\nJob loaded: " + process);
      }

      // نغلق الملف بعد الانتهاء
      reader.close();

    } catch (IOException e) {
      // هذا الخطأ يظهر إذا الملف غير موجود أو فيه مشكلة في القراءة
      System.out.println("Error: Cannot read the job file.");

    } catch (Exception e) {
      // هذا الخطأ يظهر إذا كان تنسيق البيانات داخل الملف غير صحيح
      System.out.println("Error: Invalid data format in job file.");
    }

    // نحدد أن Thread 1 انتهى من قراءة كل العمليات
    finished = true;
    System.out.println("JobReader finished reading all jobs.");
  }

  // هذه الدالة تساعد باقي الكلاسات تعرف هل انتهى JobReader أو لا
  public boolean isFinished() {
    return finished;
  }
}