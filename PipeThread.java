import java.io.*;

/**
 * PipeThread clas developed to enable the pipe command implementation in Jsh.
 * Allows output to be sent to the input piped commands.
 * @author alan
 *
 */
public class PipeThread implements Runnable{

  private InputStream istream;
  private OutputStream ostream;

  
    public void run() {
      try {
        byte[] buffer = new byte[1024];
        int read = 1;
        //while istream hasn't reached end of file
        while (read > -1)
        {
          //read input stream into buffer, 
          read = istream.read(buffer, 0, buffer.length);
          if (read > -1)
          {
            //writes the buffer to output stream
            ostream.write(buffer, 0, read);
          }
        }
      }
      catch (IOException e) {
        // do nothing
      } finally {
        try {
          //closes stream if it isn't the System input stream
          if(istream != System.in)
          {
            istream.close();
          }
        } catch (Exception e) {
          
        }
        try {
          //closes stream is it isn't the System output stream
          if(ostream != System.out)
          {
            ostream.close();
          }
        } catch (Exception e) {
          
        }
      }
    }


  public PipeThread(InputStream istream, OutputStream ostream) {
    this.istream = istream;
    this.ostream = ostream;
    
  }
  
  /**
   * 
   * @param proc - array of processes that will have one process' output be put into the buffered input stream
   *               of the next piped process.
   * @return nothing.
   * @throws InterruptedException
   */
  public static InputStream pipe(Process[] proc) throws InterruptedException
  {
    Process p1;
    Process p2;
    
    for (int i = 0; i < proc.length; i++)
    {
      p1 = proc[i];
      
      //checks if there's an available process to pipe to
      if (i + 1 < proc.length)
      {
        p2 = proc[i+1];
        Thread t = new Thread(new PipeThread(p1.getInputStream(), p2.getOutputStream()));
        t.start();
      }
    }
    
    //waits for final thread to finish before returning
    Process last = proc[proc.length -1];
    last.waitFor();
    return last.getInputStream();
  }
}
