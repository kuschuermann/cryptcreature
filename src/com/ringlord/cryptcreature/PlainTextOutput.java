package com.ringlord.cryptcreature;

import java.awt.Color;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.ringlord.mime.Base64;


public class PlainTextOutput
  extends JTextArea
{
  public PlainTextOutput( final CipherParameterPane cipherParameters,
                          final CipherTextOutput cipherTextOutput )
  {
    super();
    setEditable( false );
    this.normalColor = getForeground();

    logger = Logger.getLogger( java.util.logging.Logger.GLOBAL_LOGGER_NAME );

    cipherTextOutput.getDocument().addDocumentListener( new DocumentListener()
    {
      @Override
      public void changedUpdate( final DocumentEvent e )
      {
      }


      @Override
      public void insertUpdate( final DocumentEvent e )
      {
        modified( cipherTextOutput.getText().trim(),
                  cipherParameters );
      }


      @Override
      public void removeUpdate( final DocumentEvent e )
      {
        modified( cipherTextOutput.getText().trim(),
                  cipherParameters );
      }
    } );
  }


  private void modified( final String text,
                         final CipherParameterPane cipherParameters )
  {
    final Key key = cipherParameters.getChosenSecretKey();
    if( key == null )
      {
        return;
      }
    if( text.length() > 0 )
      {
        logger.finer( "Deciphering:\n" + text );
        new SwingWorker<Void,Void>()
        {
          @Override
          public Void doInBackground()
          {
            byte[] cipherText = null;
            byte[] initVector = null;
            byte[] plainTextHash = null;

            int itemIndex = 0;
            final StringTokenizer t = new StringTokenizer( text,
                                                           " \t\n\r,;|" );
            try
              {
                while( t.hasMoreTokens() )
                  {
                    final byte[] bytes = Base64.decode( t.nextToken().getBytes() );
                    switch( itemIndex++ )
                      {
                      case 0:
                        cipherText = bytes;
                        logger.finest( "Cipher text is " + bytes.length + " bytes" );
                        break;

                      case 1:
                        initVector = bytes;
                        logger.finest( "Initialization vector is " + bytes.length + " bytes" );
                        break;

                      case 2:
                        plainTextHash = bytes;
                        logger.finest( "SHA-256 digest is " + bytes.length + " bytes" );
                        break;

                      default:
                        logger.finest( "Unexpected extra data (ignored) is " + bytes.length + " bytes" );
                        // too much data, ignore it
                        break;
                      }
                  }
              }
            catch( final Exception x )
              {
                setText( "" );
                logger.log( Level.SEVERE,
                            "Failed to parse encrypted spec:\n" + text,
                            x );
                return null;
              }

            if( (cipherText != null) && (initVector != null) )
              {
                Algorithm algorithm = null;
                try
                  {
                    algorithm = cipherParameters.getChosenAlgorithm();

                    final Cipher c = Cipher.getInstance( algorithm.spec() );
                    if( initVector.length == 0 )
                      {
                        c.init( Cipher.DECRYPT_MODE,
                                key );
                      }
                    else
                      {
                        final IvParameterSpec ivSpec = new IvParameterSpec( initVector );
                        c.init( Cipher.DECRYPT_MODE,
                                key,
                                ivSpec );
                      }
                    final byte[] plainText = c.doFinal( cipherText );
                    logger.fine( "Deciphered: " + new String( plainText,
                                                              UTF8 ) );
                    if( plainTextHash != null )
                      {
                        boolean okay = true;
                        final MessageDigest md = MessageDigest.getInstance( "SHA-256" );
                        final byte[] digest = md.digest( plainText );
                        if( plainTextHash.length == digest.length )
                          {
                            for( int i = 0; i < plainTextHash.length; i++ )
                              {
                                if( plainTextHash[i] != digest[i] )
                                  {
                                    setText( "" );
                                    setToolTipText( "Digest mismatch" );
                                    okay = false;
                                    break;
                                  }
                              }
                          }
                        else
                          {
                            setToolTipText( "Digest wrong length" );
                            okay = false;
                            setForeground( Color.red );
                          }

                        if( okay )
                          {
                            setForeground( normalColor );
                            setToolTipText( null );
                          }
                        else
                          {
                            setForeground( Color.red );
                          }
                      }
                    else
                      {
                        setForeground( Color.orange );
                        setToolTipText( "No digest, cannot validate" );
                      }

                    setText( new String( plainText,
                                         UTF8 ) );
                  }
                catch( final NoSuchAlgorithmException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                   "<html>" +
                                                       "The algorithm '" +
                                                       cipherParameters.getChosenAlgorithm().name() +
                                                       "' is unavailable!",
                                                   "No Such Algorithm",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                catch( final NoSuchPaddingException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                   "<html>" +
                                                       "The padding '" +
                                                       cipherParameters.getChosenAlgorithm().padding() +
                                                       "' is unavailable!",
                                                   "No Such Padding",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                catch( final InvalidKeyException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    final String algorithmName = key.getAlgorithm();
                    JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                   "<html>" +
                                                       "The " +
                                                       (8 * key.getEncoded().length) +
                                                       "-bit SecretKey was " +
                                                       " generated<br>" +
                                                       "for the cryptographic cipher " +
                                                       algorithmName +
                                                       ",<br>" +
                                                       "which is not compatible with " +
                                                       algorithm.name() +
                                                       ".",
                                                   "Invalid Key",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                catch( final InvalidAlgorithmParameterException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                   "<html>" + "Parameters (key and/or IV) are not valid",
                                                   "Invalid Algorithm Parameter",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                catch( final IllegalBlockSizeException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                   "<html>" + "The block size is not valid",
                                                   "Illegal Block Size",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                catch( final BadPaddingException x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                    setText( "" );
                    final String padding = algorithm.padding();
                    if( "NoPadding".equalsIgnoreCase( padding ) )
                      {
                        JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                       "<html>" +
                                                           "For the chosen '" +
                                                           padding +
                                                           "' option, the length of your<br>" +
                                                           "plain text input must perfectly match the algorithm's" +
                                                           "block size. This is not the case. The easiest fix is" +
                                                           "to pick one of the cryptographically secure padding" +
                                                           "options, rather than 'NoPadding'",
                                                       "Bad Padding",
                                                       JOptionPane.ERROR_MESSAGE );
                      }
                    else
                      {
                        JOptionPane.showMessageDialog( PlainTextOutput.this,
                                                       x.getMessage(),
                                                       "Bad Padding",
                                                       JOptionPane.ERROR_MESSAGE );
                      }
                  }
                catch( final Throwable x )
                  {
                    logger.log( Level.SEVERE,
                                "Failed to decipher",
                                x );
                  }
              }
            else
              {
                logger.finer( "Nothing to decipher (require at least cipher text & init vector" );
              }
            return null;
          }
        }.execute();
      }
    else
      {
        logger.finest( "No text to decipher" );
      }
  }

  private final Color normalColor;
  private final Logger logger;

  private static final Charset UTF8 = Charset.forName( "UTF-8" );
  private static final long serialVersionUID = 242742521890866305L;
}
