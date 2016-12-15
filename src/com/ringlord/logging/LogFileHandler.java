package com.ringlord.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;


/**
 * Creates a {@link Handler} to manage log file creation and rotation. Example
 * usage for setting up a logger:
 *
 * <pre>
 * final File loggingDir = new File( System.getProperty( &quot;user.home&quot; ),
 *                                   &quot;.myapp&quot; );
 * if( !loggingDir.isDirectory() )
 *   {
 *     loggingDir.mkdirs();
 *   }
 * 
 * final Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
 * logger.setLevel( Level.FINEST );
 * try
 *   {
 *     final LogFormatter formatter = new LogFormatter();
 *     logger.setUseParentHandlers( false );
 *     if( loggingDir.isDirectory() )
 *       {
 *         final Handler handler = new LogFileHandler( loggingDir,
 *                                                     &quot;myapp-%u%g.log&quot;,
 *                                                     1024 * 1024,
 *                                                     10 );
 *         logger.addHandler( handler );
 *         handler.setFormatter( formatter );
 *       }
 *     else
 *       {
 *         final Handler handler = new ConsoleHandler();
 *         handler.setFormatter( formatter );
 *         logger.addHandler( handler );
 *         logger.severe( &quot;Unable to create logging directory &quot; + loggingDir );
 *       }
 *   }
 * catch( SecurityException |
 *        IOException x )
 *   {
 *     logger.severe( &quot;Unable to setup intended logging in directory &quot; + loggingDir );
 *   }
 * </pre>
 *
 * @author K Udo Schuermann
 */
public class LogFileHandler
  extends FileHandler
{
  /**
   * Creates a LogFormatter that writes log files to "~/unnamed-NN.log",
   * rotating through 10 logs, each with a maximum size of 1MB.
   *
   * @throws IOException
   * @throws SecurityException
   */
  public LogFileHandler()
    throws IOException,
      SecurityException
  {
    this( null,
          null,
          1024 * 1024,
          10 );
  }


  /**
   * Create a LogFileHandler
   *
   * @param path
   *          The directory where the log files are to be created. This defaults
   *          to your "${HOME}" directory.
   * @param pattern
   *          The pattern to be used for the log files themselves. The default
   *          is "unnamed-%u%g.log" where %u is a cyclical number, and %g is
   *          used to differentiate logs when multiple threads are writing logs
   *          to the same directory.
   * @param maxSize
   *          The maximum size of the log before it is forced to rotate.
   * @param maxRotations
   *          The maximum rotations (generations) to keep before the oldest is
   *          discarded.
   * @throws IOException
   * @throws SecurityException
   */
  public LogFileHandler( final File path,
                         final String pattern,
                         final int maxSize,
                         final int maxRotations )
    throws IOException,
      SecurityException
  {
    super( new File( (path == null
                         ? new File( System.getProperty( "user.home" ) )
                         : path),
                     (pattern == null
                         ? "unnamed-%u%g.log"
                         : pattern) ).toString(),
           maxSize,
           maxRotations );
  }
}
