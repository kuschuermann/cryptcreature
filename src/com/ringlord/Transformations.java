package com.ringlord;

public class Transformations
{
  public static String toString( final byte[] bytes )
  {
    final StringBuilder sb = new StringBuilder();
    if( bytes != null )
      {
        for( int i = 0; i < bytes.length; i++ )
          {
            sb.append( String.format( "%02x",
                                      bytes[i] ) );
            if( (i + 1) % 8 == 0 )
              {
                sb.append( "  " );
              }
            else if( (i + 1) % 2 == 0 )
              {
                sb.append( " " );
              }
          }
      }
    return sb.toString().trim();
  }


  public static String toList( final Object[] values )
  {
    final StringBuilder sb = new StringBuilder();
    if( values != null )
      {
        boolean isFirst = true;
        for( final Object item : values )
          {
            if( item != null )
              {
                if( isFirst )
                  {
                    isFirst = false;
                  }
                else
                  {
                    sb.append( ", " );
                  }
                sb.append( item.toString() );
              }
          }
      }
    return sb.toString();
  }


  private Transformations()
  {
  }
}
