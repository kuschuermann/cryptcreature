package com.ringlord.cryptcreature;

import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;


/**
 * Seeks to discover and expose the valid algorithms and parameters.
 *
 * @author kusch211
 */
public class Algorithm
  implements
    Comparable<Algorithm>
{
  public static synchronized Algorithm[] all()
  {
    final Algorithm[] defensiveCopy = new Algorithm[all.length];
    for( int i = 0; i < all.length; i++ )
      {
        defensiveCopy[i] = all[i];
      }
    return defensiveCopy;
  }


  public static Algorithm find( final String name,
                                final String mode,
                                final String padding )
  {
    for( final Algorithm a : all )
      {
        if( a.name.equals( name ) && a.mode.equals( mode ) && a.padding.equals( padding ) )
          {
            return a;
          }
      }
    return null;
  }


  public static String[] allNames()
  {
    final List<String> result = new ArrayList<>();
    for( final Algorithm a : all )
      {
        if( !result.contains( a.name ) )
          {
            result.add( a.name );
          }
      }
    final String[] sortedResult = new String[result.size()];
    result.toArray( sortedResult );
    Arrays.sort( sortedResult );
    return sortedResult;
  }


  public static String[] allModesFor( final String algorithmName )
  {
    final List<String> result = new ArrayList<>();
    for( final Algorithm a : all )
      {
        if( a.name.equals( algorithmName ) )
          {
            if( !result.contains( a.mode ) )
              {
                result.add( a.mode );
              }
          }
      }
    final String[] sortedResult = new String[result.size()];
    result.toArray( sortedResult );
    Arrays.sort( sortedResult );
    return sortedResult;
  }


  public static String[] allPaddingsFor( final String algorithmName,
                                         final String mode )
  {
    final List<String> result = new ArrayList<>();
    for( final Algorithm a : all )
      {
        if( a.name.equals( algorithmName ) && a.mode.equals( mode ) )
          {
            if( !result.contains( a.padding ) )
              {
                result.add( a.padding );
              }
          }
      }
    final String[] sortedResult = new String[result.size()];
    result.toArray( sortedResult );
    Arrays.sort( sortedResult,
                 Collections.reverseOrder() );
    return sortedResult;
  }


  public String name()
  {
    return name;
  }


  public String mode()
  {
    return mode;
  }


  public String padding()
  {
    return padding;
  }


  public boolean requireInitVector()
  {
    return requireInitVector;
  }


  public int[] keySizes()
  {
    final int[] defensiveCopy = new int[keySizes.size()];
    int n = 0;
    for( final int keySize : keySizes )
      {
        defensiveCopy[n++] = keySize;
      }
    return defensiveCopy;
  }


  public String spec()
  {
    return name + "/" + mode + "/" + padding;
  }


  @Override
  public int compareTo( final Algorithm other )
  {
    int comparison;
    if( (comparison = name.compareTo( other.name )) != 0 )
      {
        return comparison;
      }
    if( (comparison = mode.compareTo( other.mode )) != 0 )
      {
        return comparison;
      }
    if( (comparison = padding.compareTo( other.padding )) != 0 )
      {
        return comparison;
      }
    return 0;
  }


  @Override
  public String toString()
  {
    return spec();
  }


  private Algorithm( final String name,
                     final String mode,
                     final String padding,
                     final boolean requireInitVector )
  {
    super();
    this.name = name;
    this.mode = mode;
    this.padding = padding;
    this.requireInitVector = requireInitVector;
  }

  private final String name;
  private final String mode;
  private final String padding;
  private final boolean requireInitVector;
  private final List<Integer> keySizes = new ArrayList<>();
  //
  private static final byte[] TEST_BYTES = "Testing!".getBytes();
  private static final Algorithm[] all;
  static
    {
      final Logger logger = Logger.getLogger( java.util.logging.Logger.GLOBAL_LOGGER_NAME );
      final List<Algorithm> result = new ArrayList<>();
      for( final String name : new String[]{"AES","Blowfish","DES","DESede","RSA"} )
        {
          for( final String mode : new String[]{"CBC","PCBC","CFB","OFB","CTR","ECB"} )
            {
              final boolean requireIV = !"ECB".equals( mode );
              for( final String padding : new String[]{"NoPadding",
                                                       "PKCS1Padding",
                                                       "PKCS2Padding",
                                                       "PKCS5Padding",
                                                       "OAEPWithSHA-1AndMGF1Padding",
                                                       "OAEPWithSHA-256AndMGF1Padding"} )
                {
                  try
                    {
                      final Algorithm a = new Algorithm( name,
                                                         mode,
                                                         padding,
                                                         requireIV );
                      final Cipher c = Cipher.getInstance( a.spec() );
                      // The algorithm, mode, and padding seem to be
                      // understood

                      for( final int keySize : new int[]{32,
                                                         56,
                                                         64,
                                                         96,
                                                         112,
                                                         128,
                                                         168,
                                                         192,
                                                         256,
                                                         320,
                                                         384,
                                                         448,
                                                         512,
                                                         1024,
                                                         2048,
                                                         3072,
                                                         4096} )
                        {
                          try
                            {
                              final KeyGenerator keyGenerator = KeyGenerator.getInstance( a.name() );
                              keyGenerator.init( keySize );
                              final Key key = keyGenerator.generateKey();
                              // This key size seems to be understood

                              c.init( Cipher.ENCRYPT_MODE,
                                      key );
                              c.doFinal( TEST_BYTES );
                              a.keySizes.add( keySize );
                            }
                          catch( final Throwable x )
                            {
                              logger.info( name +
                                           "/" +
                                           mode +
                                           "/" +
                                           padding +
                                           " size=" +
                                           keySize +
                                           " ==> " +
                                           x.getMessage() );
                            }
                        }
                      if( a.keySizes.size() > 0 )
                        {
                          result.add( a );
                        }
                    }
                  catch( final Throwable x )
                    {
                      logger.fine( name + "/" + mode + "/" + padding + " ==> " + x.getMessage() );
                    }
                }
            }
        }
      all = new Algorithm[result.size()];
      int n = 0;
      for( final Algorithm a : result )
        {
          all[n++] = a;
        }
    }
}
