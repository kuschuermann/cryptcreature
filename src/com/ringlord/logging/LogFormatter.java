package com.ringlord.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * <p>
 * Implements the Java Logging framework's {@link Formatter formatting} for
 * {@link LogRecord}S, producing a custom log format that is similar to, but
 * also more extensively detailed, than that provided by the standard
 * FileHandler.
 * </p>
 *
 * <p>
 * The output lines are formatted with the following fields (separated by
 * spaces):
 * </p>
 *
 * <ol>
 * <li>Date (yyyy-MM-dd)</li>
 * <li>Time (HH:mm:ss.SSS)</li>
 * <li>Level (enclosed in [&nbsp;]'s such as [SEVERE], [WARNING], [INFO],
 * [CONFIG], [FINE], [FINER], or [FINEST])</li>
 * <li>Thread ID (enclosed in (&nbsp;)'s)</li>
 * <li>Class#Method (followed by a colon)</li>
 * <li>The message text up to the end of the line</li>
 * </ol>
 *
 * <p>
 * Lines not conforming to this format are part of the most recent line that
 * does conform; examples of this include Java exception stack traces and other
 * information that the software writes to the log. The exception trace is NOT
 * cut short, in other words there will be no '...and N more'
 * </p>
 *
 * @author K. Udo Schuermann
 **/
final public class LogFormatter
  extends Formatter
{
  @Override
  public synchronized String format( final LogRecord record )
  {
    final String message = record.getMessage();
    final StringBuilder sb = new StringBuilder();
    sb.append( dateFormat.format( new Date( record.getMillis() ) ) )
      .append( " [" )
      .append( record.getLevel().toString() )
      .append( "] (" )
      .append( record.getThreadID() + ") " )
      .append( record.getSourceClassName() )
      .append( '#' )
      .append( record.getSourceMethodName() )
      .append( ": " )
      .append( (message == null
          ? ""
          : message.trim()) )
      .append( EOLN );

    Throwable t = record.getThrown();
    while( t != null )
      {
        final String msg = t.getMessage();
        sb.append( t.getClass().getName() ).append( ": " ).append( (msg == null
            ? ""
            : msg.trim()) ).append( EOLN );
        final StackTraceElement[] trace = t.getStackTrace();
        for( final StackTraceElement e : trace )
          {
            sb.append( "\tat " ).append( e.getClassName() ).append( '#' ).append( e.getMethodName() );
            final String filename = e.getFileName();
            final int fileline = e.getLineNumber();
            final boolean isNative = e.isNativeMethod();
            if( filename != null )
              {
                sb.append( " (" ).append( filename );
                if( fileline >= 0 )
                  {
                    sb.append( ":" + fileline );
                  }
                sb.append( ')' ).append( EOLN );
              }
            else if( isNative )
              {
                sb.append( " (native)" ).append( EOLN );
              }
            else
              {
                sb.append( EOLN );
              }
          }
        if( (t = t.getCause()) != null )
          {
            sb.append( "Caused by " );
          }
      }

    return sb.toString();
  }

  private final static SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );
  private final static String EOLN = System.getProperty( "line.separator" );
}
