package com.ringlord.swing;

import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * Provides various static services for loading images and icons.
 *
 * @author K Udo Schuermann
 */
public class Images
{
  /**
   * <p>
   * Obtain an {@link BufferedImage} resources (from the classpath). Images
   * loaded through this method are cached to eliminate the need to load them
   * again on future calls, which makes use of this method quite efficient (at
   * the expense of memory resources).
   * </p>
   *
   * <p>
   * No exceptions are thrown, but a message is written to stderr to report on a
   * missing image.
   * </p>
   *
   * @param name
   *          The name of the resource. Resource names generally begin with a
   *          '/' character.
   * @return The BufferedImage or null if the image could not be found or
   *         loaded.
   */
  public static BufferedImage getImage( final String name )
  {
    BufferedImage result = images.get( name );
    if( result == null )
      {
	final InputStream f = Images.class.getResourceAsStream( name );
	if( f != null )
	  {
	    try
	      {
		result = ImageIO.read( f );
		images.put( name,
		            result );
	      }
	    catch( final IOException x )
	      {
		x.printStackTrace();
	      }
	    finally
	      {
		try
		  {
		    f.close();
		  }
		catch( final IOException e )
		  {
		    // cleanup failed, don't bother complaining
		  }
	      }
	  }
	else
	  {
	    Logger.getGlobal().warning( "Missing image: " + name );
	  }
      }
    return result;
  }


  /**
   * Obtains the ImageIcon version of a named Image. Icons are cached separately
   * from images, on the presumption that a cost saving is derived from this,
   * and the memory overhead is worthy. Note that this causes an image by the
   * same name to be loaded (and cached), too.
   *
   * @param name
   *          The name of the resource. Resource names generally begin with a
   *          '/' character.
   * @return The ImageIcon or null if the image could not be found or loaded.
   */
  public static Icon getIcon( final String name )
  {
    ImageIcon result = icons.get( name );
    if( result == null )
      {
	final BufferedImage image = getImage( name );
	if( image != null )
	  {
	    result = new ImageIcon( image );
	    icons.put( name,
		       result );
	  }
      }
    return result;
  }


  /**
   * A convenience method to assemble a {@link List} of images by their names,
   * and call {@link Window#setIconImages(List)} with them. Note that the "best"
   * image of the set is selected by the Window implementation. It is suggested
   * that the named images are rectangular, and somewhere in the range of 16 to
   * 64 pixels in size: One image may be used for the title bar icon, another
   * for the task switcher, so do not forget to offer both small and large
   * versions of the icons.
   *
   * @param window
   *          The window whose icon to set.
   * @param availableNames
   * @return 'true' if at least one icon was offered to the Window, false if
   *         none of the images could be loaded and no icons could be set on the
   *         Window.
   */
  public static boolean setWindowIcons( final Window window,
	                                final String... availableNames )
  {
    final List<BufferedImage> images = new ArrayList<>();
    for( final String name : availableNames )
      {
	final BufferedImage image = getImage( name );
	if( image != null )
	  {
	    images.add( image );
	  }
      }
    if( images.isEmpty() )
      {
	return false;
      }
    window.setIconImages( images );
    return true;
  }

  private static final Map<String,BufferedImage> images = new HashMap<>();
  private static final Map<String,ImageIcon> icons = new HashMap<>();
}
