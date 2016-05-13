import java.io.IOException;
import java.io.InputStream;
/**
 * BackgroundProcess runs the passed command in the background as a new thread
 * @author alan
 *
 */
public class BackgroundProcesses {

  private int numOfJobs;
  private Process p;
  private ProcessBuilder pb;
  private String tokens[];
  private String jobs[];
  private Process processes[];

  class MyTask implements Runnable {
    public void run() {
      
      //runs process, numOfJobs is the index of the latest job, it is incremented in Jsh if-block of '&'
      try {
        p = pb.start();
        processes[numOfJobs] = p;

      }
      catch (IOException e) {
        System.err.println("Unknown command " + "'" + tokens[0] + "'");
        return;
      }
      
      //adds the background job to the end of list. Indexed by numOfjobs.
      int i = 0;
      String strCommand = "";
      while (i < tokens.length && !tokens[i].equals("&")) {
        strCommand += " " + tokens[i];
        i++;
      }
      jobs[numOfJobs] = strCommand;
      

      InputStream istream = p.getInputStream();
      InputStream errstream = p.getErrorStream();

      PipeThread inputReader = new PipeThread(istream, System.out);
      PipeThread errReader = new PipeThread(errstream, System.err);

      try {
        int returnValue = p.waitFor();
        // System.err.println("Process completed with return value " + returnValue);
      }
      catch (InterruptedException e) {
        // Do nothing for now...this is about threads and will
        // be explained later.
      }

      // close streams
      try {
        istream.close();
        errstream.close();
      }
      catch (IOException e) {
        System.err.println("IO Error");
      }

      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        // Do nothing for now...this is about threads and will
        // be explained later
      }

      p.destroy();

    }

  }


  public BackgroundProcesses(int numOfJobs, ProcessBuilder pb, String tokens[], String jobs[], Process processes[]) {
    this.numOfJobs = numOfJobs;
    this.pb = pb;
    this.tokens = tokens;
    this.jobs = jobs;
    this.processes = processes;
    Thread t = new Thread(new MyTask());
    t.start();
  }
}
