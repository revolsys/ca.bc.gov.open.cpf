package ca.bc.gov.open.cpf.api.worker;

public class WorkerRunning {
  private static boolean running = true;

  public static boolean isRunning() {
    return running;
  }

  public static void stop() {
    running = false;
  }
}
