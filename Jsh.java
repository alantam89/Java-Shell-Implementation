import java.io.*;
import java.util.*;
/**
 * @author alan
 *
 *
 * A java program that runs the command line programs as an external process.
 * handling of: ls, rm, mkdir, rmdir, grep, cat are already implemented by ProcessBuilder class.
 * handling of: cd, pwd, |, jobs, &, fg were newly implemented.
 * Exit code is ctrl+d.
 * 
 */
public class Jsh {

  private static String command;
  private String directory = System.getProperty("user.dir");
  private String userDir;
  private File file;
  private int numOfJobs = 0;
  private String jobs[] = new String[100];
  private Process processes[] = new Process[100];
  private Process proc[];


  public static void main(String[] args) throws IOException, InterruptedException {

    Jsh pbe = new Jsh(command);
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    //will loop until a valid input is given
    System.out.print("insert command:");
    while ((command = input.readLine()) != null) {
        command = command.trim();
      if (command.length() != 0) {
        pbe.runProcess();
      }
      System.out.print("insert command:");
    }
    System.out.println("\nexiting...");
     input.close();
  }

/**
 * 
 * @param s: the command you wish to run(e.g. cd, ls, pwd, etc...)
 */
  public Jsh(String s) {
    command = s;
  }

/**
 * starts the ProcessBuilder and runs the commands given from user input
 * 
 * @throws IOException
 * @throws InterruptedException
 */
  public void runProcess() throws IOException, InterruptedException {

    ProcessBuilder pb = null;
    Process p = null;
    command = command.trim();
    //System.out.println(procs[0]+procs[1]);
    
    //if-block to handle commands using pipes
    if (command.contains("|"))
    {
      //can't use cd with pipe
      if(command.contains("cd"))
      {
        System.err.println("invalid use of pipes.");
        return;
      }
      
      //split commands at pipe
      String[] procs = command.split("[|]");
      
      //wrong syntax for pipe
      if(procs.length <= 1 || procs[0].equals(""))
      {
        System.err.println("invalid use of pipes.");
        return;
      }
      
      //possible valid usage of pipe
      if (procs.length > 1)
      {
        proc = new Process[procs.length];
        for (int i = 0; i < procs.length;i++)
        {
          procs[i] = procs[i].trim();
          //can't pipe empty commands together
          if(procs[i].equals(""))
          {
            System.err.println("invalid use of pipes.");
            return;
          }
          //splits on tabs and new lines, then starts ProcessBuilder
          // ProcessBuilder is an object that knows how to create
          // external processes like ls
          // Pass ProcessBuilder the command as an array of strings
          String[] procsTokens = procs[i].split("[ \t\n]+");
          pb = new ProcessBuilder(procsTokens);
          file = new File(directory);
          pb.directory(file);
          try {
            proc[i] = pb.start();
          }
          catch (IOException e) {
          System.err.println("Unknown command " + "'" + procsTokens[0] + "'");
          return;
          }
          
          //error stream.
          InputStream err = proc[i].getErrorStream();
          Thread t = new Thread(new PipeThread(err, System.out));
          t.start();
        }
      
        //output stream gets piped to the input steam of next command
        InputStream in = PipeThread.pipe(proc);
        Thread t = new Thread(new PipeThread(in, System.out));
        t.start();
      
      }
      return;
    }// end-if command containing pipe '|'
    
    // Split the command into an array of strings
    String[] tokens = command.split("[ \t\n]+");
    //System.out.println(tokens[0] + tokens[1]);
    pb = new ProcessBuilder(tokens);    
    // sets the directory
    file = new File(directory);
    pb.directory(file);
    
 //if-block for & command. Runs processes in the background, if possible.
    if (tokens[tokens.length - 1].equals("&")) {
      //the required commands to check for that can't be put into background 
      if (tokens[0].equals("fg") || tokens[0].equals("jobs") || tokens[0].equals("cd")) {
        System.err.print("cannot put '" + tokens[0] + "' in the background\n");
      }
      else {
        command = command.substring(0, command.length() - 1);
        // System.out.println(command);
        tokens = command.split("[ \t\n]+");
        pb = new ProcessBuilder(tokens);
        file = new File(directory);
        pb.directory(file);
        BackgroundProcesses bp = new BackgroundProcesses(numOfJobs, pb, tokens, jobs, processes);
        numOfJobs++;
      }
    }//end-if & command
    
    // cd command
    else if (tokens[0].equals("cd")) {
      // displays syntax error for invalid use of cd
      if (tokens.length > 2) {
        System.err.print("Syntax Error.\n");
      }
      // sets back directory to where java was ran if user inputs "cd" or "cd /"
      else if (tokens.length == 1 || tokens[1].equals("/")) {
        directory = System.getProperty("user.dir");
        file = new File(directory);
        System.err.println("Starting process " + tokens[0] + "...");
        pb.directory(file);
        return;
      }
      //moves one directory up
      else if (tokens[1].equals("..")) {
        file = new File(directory);
        directory = file.getParent();
        System.err.println("Starting process " + tokens[0] + "...");
        pb.directory(file);
        return;
      }
      //error for trying to put cd as a background process
      else if (tokens[1].equals("&")) {
        System.err.print("cannot put '" + tokens[0] + "' in the background\n");
        return;
      }
      // sets the directory to the new directory if it exists.
      else {
        userDir = tokens[1];
        file = new File(userDir);
        if (file.exists() == true) {
          directory = file.getCanonicalPath();
          System.err.println("Starting process " + tokens[0] + "...");
        }
        else if (file.exists() == false) {
          System.err.print("Invalid directory " + tokens[1] + "\n");
        }
      }

    }//end-if command containing cd
    
    // checks for jobs, outputs the jobs selected to run in background.
    else if (tokens[0].equals("jobs")) {
      if (tokens.length == 1) {
        for (int i = 0; i < numOfJobs; i++) {
          if (jobs[i] != null) {
            System.out.print("[" + (i + 1) + "] " + jobs[i] + "\n");
          }
        }
      }
      else if (tokens.length > 2) {
        System.err.print("Syntax error.\n Usage: jobs 'n' where n is the job ID \n");
      }
      else if (tokens[1].equals("&")) {
        System.err.print("cannot put '" + tokens[0] + "' in the background\n");
        return;
      }
      else {
        System.err.print("Syntax error.\n");
      }
        
    }//end-if jobs command

    //if-block for fg command. Brings specified process to the foreground. shell is blocked until process completes.
    else if (tokens[0].equals("fg")) {
      try {
        //checks for wrong syntax
        if (tokens.length < 2 || tokens.length > 2) {
          System.err.print("Usage: fg 'n' where n is a number\n");
          return;
        }
        else if (tokens[1].equals("&")) {
          System.err.print("cannot put '" + tokens[0] + "' in the background\n");
          return;
        }
        
        int processNum = Integer.parseInt(tokens[1]);       
        //valid fg syntax
        if (tokens.length == 2) {
          //checks for invalid arguments
          if (processNum > numOfJobs || processNum <= 0 || jobs[processNum - 1] == null) {
            System.err.print("no such background command\n");
            return;
          }
          else if (tokens[1].equals("&")) {
            System.err.print("cannot put '" + tokens[0] + "' in the background\n");
            return;
          }
          
          //valid arguments
          else {
            try {
              processes[processNum - 1].waitFor();
              jobs[processNum - 1] = null;
              System.err.println("Starting process " + tokens[0] + "...");
            }
            catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }// end-if for valid fg syntax
      } catch (NumberFormatException e) {
        System.err.print("Syntax error.\n");
      }
    }//end-if fg command

    // starts rest of process commands
    else {
      System.err.println("Starting process " + tokens[0] + "...");
      try {
        p = pb.start();
      }
      catch (IOException e) {
        System.err.println("Unknown command " + "'" + tokens[0] + "'");
        return;
      }

      InputStream istream = p.getInputStream();
      InputStream errstream = p.getErrorStream();
      //OutputStream newInput = p.getOutputStream();
      
      Thread t = new Thread(new PipeThread(istream, System.out));
      t.start();
      Thread t2 = new Thread(new PipeThread(errstream, System.out));
      t2.start();
      

      // Wait for the process to terminate
      try {
        int returnValue = p.waitFor();
      }
      catch (InterruptedException e) {
      }

      // close streams
      try {
        istream.close();
        errstream.close();
      }
      catch (IOException e) {
        System.err.println("IO Error");
      }
      
      /**
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        // Do nothing for now...this is about threads and will
        // be explained later
      }
       **/
      p.destroy();

      return;
    }//end of else block
  }
}
