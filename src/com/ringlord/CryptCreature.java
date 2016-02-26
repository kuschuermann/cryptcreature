package com.ringlord;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.ringlord.cryptcreature.GUI;
import com.ringlord.logging.LogFileHandler;
import com.ringlord.logging.LogFormatter;


public class CryptCreature
{
  public static final String VERSION = "1.1";
  public static final String VERDATE = "2015-10-15";
  public static final String CPYEARS = "2014,2015";


  public static final void main( final String[] args )
  {
    final Logger logger = createLogger();
    logCopyrightHeader( logger );

    SwingUtilities.invokeLater( new Runnable()
    {
      @Override
      public void run()
      {
        new GUI().setVisible( true );
      }
    } );
  }


  public static File storage()
  {
    return new File( System.getProperty( "user.home" ),
                     ".cryptcreature" );
  }


  private static Logger createLogger()
  {
    final File loggingDir = storage();
    if( !loggingDir.isDirectory() )
      {
        loggingDir.mkdirs();
      }

    final Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
    logger.setUseParentHandlers( false );
    logger.setLevel( Level.FINEST );
    try
      {
        if( loggingDir.isDirectory() )
          {
            final Handler handler = new LogFileHandler( loggingDir,
                                                        "cryptcreature-%u%g.log",
                                                        1024 * 1024,
                                                        10 );
            handler.setFormatter( new LogFormatter() );
            logger.addHandler( handler );
          }
        else
          {
            final Handler handler = new ConsoleHandler();
            handler.setFormatter( new LogFormatter() );
            logger.addHandler( handler );

            logger.severe( "Unable to create logging directory " + loggingDir );
          }
      }
    catch( SecurityException |
           IOException x )
      {
        // Cannot create the Formatter or LogFileHandler
        logger.severe( "Unable to setup intended logging in directory " + loggingDir );
      }
    return logger;
  }


  private static void logCopyrightHeader( final Logger logger )
  {
    logger.info( "CryptCreature " + VERSION + " (" + VERDATE + ")" );
    logger.info( "Copyright \u00a9 " + CPYEARS + " by Ringlord Technologies" );
    logger.info( "All rights reserved" );
  }
}
