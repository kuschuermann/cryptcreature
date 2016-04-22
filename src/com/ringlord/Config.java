package com.ringlord;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ringlord.mime.Base64;


/**
 * <p>
 * Simple key-value pairs stored in a file, comparable to, but different from
 * Properties files. When created, the values are retrieved, when modified they
 * are maintained in memory and the Config is marked as "dirty" (needing to be
 * saved). The original settings can be {@link #reset() reloaded}; the config
 * must be {@link Closeable closed} for changes to be persisted. If changes are
 * to be discarded, it is recommended to reset/reload the Config to ensure that
 * in-memory alterations are not accidentally persisted.
 * </p>
 *
 * @author K. Udo Schuermann
 * @see Settings
 **/
public class Config
  extends Observable
  implements
    Closeable
{
  public Config( final File file )
  {
    this( file,
	  null );
  }


  public Config( final File file,
	         final String commentLine )
  {
    super();
    this.file = file;
    this.info = (commentLine == null
	? "Edit only with great care: If you break it, you get to keep both parts!"
	: commentLine);
    load( file,
	  prefs );

    try
      {
	final WatchService ws = FileSystems.getDefault().newWatchService();
	final Path parent = file.toPath().getParent();
	parent.register( ws,
	                 StandardWatchEventKinds.ENTRY_MODIFY );
	final Thread updated = new Thread()
	{
	  @Override
	  public void run()
	  {
	    Config.this.isReady = true;
	    while( Config.this.isReady )
	      {
		try
		  {
		    final WatchKey key = ws.take();
		    for( final WatchEvent<?> event : key.pollEvents() )
		      {
			final WatchEvent.Kind<?> kind = event.kind();
			if( kind == StandardWatchEventKinds.ENTRY_MODIFY )
			  {
			    @SuppressWarnings("unchecked") final Path path = parent.resolve( ((WatchEvent<Path>)event)
				.context() );
			    if( path.equals( file.toPath() ) )
			      {
				if( wasJustSaved )
				  {
				    wasJustSaved = false;
				    continue;
				  }
				final Map<String,String> updatedConfig = new HashMap<>();
				load( file,
				      updatedConfig );
				final boolean isModified = merge( prefs,
				                                  updatedConfig );
				if( isModified )
				  {
				    isDirty = true;
				    setChanged();
				    notifyObservers();
				  }
			      }
			  }
		      }
		    key.reset();
		  }
		catch( final InterruptedException x )
		  {
		    break;
		  }
	      }
	  }
	};
	updated.setDaemon( true );
	updated.start();
      }
    catch( final IOException x )
      {
	x.printStackTrace( System.err );
      }
  }


  /**
   * Merges changes from disk and changes in memory, with awareness of the
   * original data that was loaded from disk.
   *
   *
   * Entries are modified in memory only if the have not bee
   *
   * modified in the updated configuration, which remain unchanged in memory are
   * updated; Entries modified in the updated
   *
   * @param currentConfig
   * @param updatedConfig
   * @param originalConfig
   */
  private boolean merge( final Map<String,String> currentConfig,
	                 final Map<String,String> updatedConfig )
  {
    boolean isModified = false;

    // Add new keys from the updated configuration; update keys whose value has
    // changed
    for( final Map.Entry<String,String> e : updatedConfig.entrySet() )
      {
	if( currentConfig.containsKey( e.getKey() ) )
	  {
	    final String updatedValue = e.getValue();
	    final String currentValue = currentConfig.get( e.getKey() );
	    if( ((updatedValue == null) && (currentValue != null)) ||
		((updatedValue != null) && (currentValue == null)) ||
		!updatedValue.equals( currentValue ) )
	      {
		currentConfig.put( e.getKey(),
		                   e.getValue() );
		isModified = true;
	      }
	  }
	else
	  {
	    currentConfig.put( e.getKey(),
		               e.getValue() );
	    isModified = true;
	  }
      }

    // Remove keys from the current configuration if no longer present in the
    // updated configuration
    List<String> toBeRemoved = null;
    for( final Map.Entry<String,String> e : currentConfig.entrySet() )
      {
	if( !updatedConfig.containsKey( e.getKey() ) )
	  {
	    if( toBeRemoved == null )
	      {
		toBeRemoved = new ArrayList<>();
	      }
	    toBeRemoved.add( e.getKey() );
	  }
      }
    if( toBeRemoved != null )
      {
	for( final String key : toBeRemoved )
	  {
	    currentConfig.remove( key );
	  }
	isModified = true;
      }

    return isModified;
  }


  public File file()
  {
    return file;
  }


  public Collection<String> getKeys()
  {
    final List<String> tmp = new ArrayList<>();
    tmp.addAll( prefs.keySet() );
    Collections.sort( tmp );
    return Collections.unmodifiableList( tmp );
  }


  /**
   * Loads the file when the Config is first constructed.
   *
   * @param file
   */
  private void load( final File file,
	             final Map<String,String> prefs )
  {
    final File parent = file.getParentFile();
    if( parent != null )
      {
	parent.mkdirs();
      }
    prefs.clear();
    try( final BufferedReader f = new BufferedReader( new FileReader( file ) ) )
      {
	final Properties p = new Properties();
	p.load( f );
	for( final Object o : p.keySet() )
	  {
	    final String key = (String)o;
	    prefs.put( key,
		       p.getProperty( key ) );
	  }
      }
    catch( final FileNotFoundException x )
      {
	// It's fine, we will create it
      }
    catch( final IOException x )
      {
	x.printStackTrace();
      }
  }


  private void save( final File file )
  {
    wasJustSaved = true; // prevent reload/merge trigger
    try( final BufferedWriter f = new BufferedWriter( new FileWriter( file ) ) )
      {
	final List<String> keys = new ArrayList<>();
	keys.addAll( prefs.keySet() );
	Collections.sort( keys );

	final StringTokenizer t = new StringTokenizer( info,
	                                               "\n\r" );
	while( t.hasMoreTokens() )
	  {
	    final String s = t.nextToken();
	    if( !s.startsWith( "#" ) )
	      {
		f.write( "# " );
	      }
	    f.write( s );
	    f.write( EOLN );
	  }
	f.write( "# Last written on " + new Date() + EOLN );
	f.write( EOLN );
	for( final String key : keys )
	  {
	    f.write( escape( key ) + "=" + escape( prefs.get( key ) ) + EOLN );
	  }
	f.write( EOLN );
	f.write( "#eot" );
	f.write( EOLN );
	f.flush();
      }
    catch( final IOException x )
      {
	Logger.getGlobal().log( Level.SEVERE,
	                        "Failed to save config to file " + file,
	                        x );
      }
    isDirty = false;
    setChanged();
    notifyObservers();
  }


  public boolean isDirty()
  {
    return isDirty;
  }


  public void save()
  {
    if( isDirty || !file.exists() )
      {
	save( file );
      }
  }


  // ======================================================================

  public void reset()
  {
    load( file,
	  prefs );
    setChanged();
    notifyObservers();
  }


  @Override
  public void close()
    throws IOException
  {
    if( isDirty )
      {
	save();
      }
  }


  // ======================================================================

  /**
   * Stores each of the values in a sequentially numbered series of keys
   * (starting with 0).
   *
   * @param baseKey
   * @param value
   */
  public void put( final String baseKey,
	           final List<String> values )
  {
    int n = 0;
    for( final String val : values )
      {
	put( baseKey + "." + (n++),
	     val );
      }
    // Now remove all sequentially numbered keys beyond the size of this list
    while( true )
      {
	final String k = baseKey + "." + (n++);
	if( get( k,
	         (String)null ) == null )
	  {
	    break;
	  }
	put( k,
	     (String)null );
      }
    setChanged();
  }


  /**
   * Retrieves each value from a sequentially numbered series of keys (starting
   * with 0).
   *
   * @param baseKey
   * @param defaultValues
   * @return
   */
  public List<String> get( final String baseKey,
	                   final List<String> defaultValues )
  {
    List<String> result = null;
    int n = 0;
    while( true )
      {
	final String val = get( baseKey + "." + (n++),
	                        (String)null );
	if( val == null )
	  {
	    break;
	  }
	if( result == null )
	  {
	    result = new ArrayList<>();
	  }
	result.add( val );
      }
    return (result == null
	? defaultValues
	: result);
  }


  // ======================================================================

  /**
   * Stores one or more values in a single key using either a defined separator
   * (such as a comma), or if the separator is given as null, will use a comma
   * but Base64-encode each of the values.
   *
   * @param key
   * @param value
   * @param separator
   */
  public void put( final String key,
	           final List<String> value,
	           final String separator )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	final StringBuilder sb = new StringBuilder();
	boolean isFirst = true;
	if( separator != null )
	  {
	    for( final String s : value )
	      {
		if( isFirst )
		  {
		    isFirst = false;
		  }
		else
		  {
		    sb.append( separator );
		  }
		sb.append( s );
	      }
	  }
	else
	  {
	    for( final String s : value )
	      {
		if( isFirst )
		  {
		    isFirst = false;
		  }
		else
		  {
		    sb.append( "," );
		  }
		final byte[] bytes = Base64.encode( s.getBytes( utf8 ) );
		sb.append( new String( bytes ) );
	      }
	  }
	prefs.put( key,
	           sb.toString() );
      }
    isDirty = true;
    setChanged();
  }


  /**
   * Obtain a List of (String) items.
   *
   * @param key
   *          The key for the list
   * @param defaultValue
   *          A default List if the setting is undefined
   * @param separator
   *          An optional separator: If no separator is defined (null or
   *          zero-length), the items are considered Base64 encoded and
   *          separated with commas, otherwise the separator (which may be
   *          anything, including newline or even multiple characters) is used
   *          to break the value into multiple items.
   * @return
   */
  public List<String> get( final String key,
	                   final List<String> defaultValue,
	                   final String separator )
  {
    final String tmp = prefs.get( key );
    if( (tmp == null) || (tmp.length() == 0) )
      {
	return defaultValue;
      }
    final List<String> result = new ArrayList<>();
    int start = 0;
    int pos;
    if( (separator != null) && (separator.length() > 0) )
      {
	final int len = separator.length();
	while( (pos = tmp.indexOf( separator,
	                           start )) > -1 )
	  {
	    result.add( tmp.substring( start,
		                       pos ) );
	    start = pos + len;
	  }
	result.add( tmp.substring( start ) );
      }
    else
      {
	while( (pos = tmp.indexOf( ",",
	                           start )) > -1 )
	  {
	    final byte[] bytes = Base64.decode( tmp.substring( start,
		                                               pos ).getBytes() );
	    result.add( new String( bytes,
		                    utf8 ) );
	    start = pos + 1;
	  }
	final byte[] bytes = Base64.decode( tmp.substring( start ).getBytes() );
	result.add( new String( bytes,
	                        utf8 ) );
      }
    return result;
  }


  // ======================================================================

  public void put( final String key,
	           final String value )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           value );
      }
    isDirty = true;
    setChanged();
  }


  public String get( final String key,
	             final String defaultValue )
  {
    final String value = prefs.get( key );
    return (value == null
	? defaultValue
	: value);
  }


  // ======================================================================
  public void put( final String key,
	           final Rectangle rect )
  {
    if( rect == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           rect.x + "," + rect.y + "," + rect.width + "," + rect.height );
      }
    isDirty = true;
    setChanged();
  }


  public Rectangle get( final String key,
	                final Rectangle defaultValue )
  {
    final String value = prefs.get( key );
    if( value == null )
      {
	return defaultValue;
      }

    try
      {
	int prev, comma;

	comma = value.indexOf( ',' );
	final int x = Integer.parseInt( value.substring( 0,
	                                                 comma ) );

	prev = comma + 1;
	comma = value.indexOf( ',',
	                       prev );
	final int y = Integer.parseInt( value.substring( prev,
	                                                 comma ) );

	prev = comma + 1;
	comma = value.indexOf( ',',
	                       prev );
	final int w = Integer.parseInt( value.substring( prev,
	                                                 comma ) );

	prev = comma + 1;
	final int h = Integer.parseInt( value.substring( prev ) );

	return new Rectangle( x,
	                      y,
	                      w,
	                      h );
      }
    catch( final Exception x )
      {
	x.printStackTrace();
	return defaultValue;
      }
  }


  // ======================================================================
  public void put( final String key,
	           final Boolean value )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           String.valueOf( value ) );
      }
    isDirty = true;
    setChanged();
  }


  public boolean get( final String key,
	              final boolean defaultValue )
  {
    try
      {
	final String s = prefs.get( key );
	if( s == null )
	  {
	    return defaultValue;
	  }
	return Boolean.parseBoolean( s );
      }
    catch( final NullPointerException x )
      {
	return defaultValue;
      }
  }


  // ======================================================================
  public void put( final String key,
	           final Double value )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           String.valueOf( value ) );
      }
    isDirty = true;
    setChanged();
  }


  public double get( final String key,
	             final double defaultValue )
  {
    try
      {
	return Double.parseDouble( prefs.get( key ) );
      }
    catch( NullPointerException | NumberFormatException x )
      {
	return defaultValue;
      }
  }


  // ======================================================================

  public void put( final String key,
	           final Long value )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           String.valueOf( value ) );
      }
    isDirty = true;
    setChanged();
  }


  public long get( final String key,
	           final long defaultValue )
  {
    try
      {
	return Long.parseLong( prefs.get( key ) );
      }
    catch( NullPointerException | NumberFormatException x )
      {
	return defaultValue;
      }
  }


  // ======================================================================

  public void put( final String key,
	           final Integer value )
  {
    if( value == null )
      {
	prefs.remove( key );
      }
    else
      {
	prefs.put( key,
	           String.valueOf( value ) );
      }
    isDirty = true;
    setChanged();
  }


  public int get( final String key,
	          final int defaultValue )
  {
    try
      {
	return Integer.parseInt( prefs.get( key ) );
      }
    catch( NullPointerException | NumberFormatException x )
      {
	return defaultValue;
      }
  }


  // ======================================================================

  private static String escape( final String s )
  {
    final StringBuilder sb = new StringBuilder();
    final int len = s.length();
    for( int i = 0; i < len; i++ )
      {
	final char c = s.charAt( i );
	switch( c )
	  {
	  case '\\':
	  case ':':
	  case '=':
	  case ' ':
	    sb.append( '\\' );
	    break;

	  case '\t':
	    sb.append( "\\t" );
	    break;

	  case '\n':
	    sb.append( "\\n" );
	    continue;

	  case '\r':
	    sb.append( "\\r" );
	    continue;
	  }
	sb.append( c );
      }
    return sb.toString();
  }

  private boolean isDirty;
  private volatile boolean isReady;
  private volatile boolean wasJustSaved;
  //
  private final File file;
  private final String info;
  private final Map<String,String> prefs = new HashMap<>();
  //
  private static final Charset utf8 = Charset.forName( "UTF-8" );
  private static final String EOLN = System.getProperty( "line.separator" );
}
