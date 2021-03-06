package com.ringlord.cryptcreature;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.ringlord.Transformations;
import com.ringlord.mime.Base64;


public class CipherTextOutput
  extends JTextArea
{
  public CipherTextOutput( final CipherParameterPane cipherParameters,
                           final JTextComponent plainTextInput,
                           final JLabel inputPictureLabel,
                           final JLabel outputPictureLabel )
  {
    super();
    setPreferredSize( new Dimension( 320,
                                     80 ) );

    logger = Logger.getLogger( java.util.logging.Logger.GLOBAL_LOGGER_NAME );

    algorithm = cipherParameters.getChosenAlgorithm();
    iv = cipherParameters.getChosenInitVector();
    key = cipherParameters.getChosenSecretKey();

    cipherParameters.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent e )
      {
        switch( e.getActionCommand() )
          {
          case CipherParameterPane.ACTION_ALGORITHM_CHANGED:
            algorithm = cipherParameters.getChosenAlgorithm();
            if( algorithm == null )
              {
                logger.info( "Algorithm cleared (this shouldn't happen!)" );
              }
            else
              {
                logger.info( "Algorithm selected: " + algorithm.spec() );
              }
            break;

          case CipherParameterPane.ACTION_INITVECTOR_CHANGED:
            iv = cipherParameters.getChosenInitVector();
            if( iv == null )
              {
                logger.info( "Initialization Vector cleared (good, let the cipher create new ones each time)" );
              }
            else
              {
                logger.info( "Initialization Vector fixed (bad, this will help cryptanalysts reverse the secret key)" );
              }
            break;

          case CipherParameterPane.ACTION_SECRET_KEY_CHANGED:
            key = cipherParameters.getChosenSecretKey();
            if( key == null )
              {
                logger.info( "Secret key cleared" );
              }
            else
              {
                logger.info( "Secret Key generated" );
              }
            break;

          default:
          }
        updateImage( cipherParameters,
                     inputPictureLabel,
                     outputPictureLabel );
        modified( plainTextInput.getText(),
                  cipherParameters );
      }
    } );

    plainTextInput.getDocument().addDocumentListener( new DocumentListener()
    {
      @Override
      public void changedUpdate( final DocumentEvent e )
      {
      }


      @Override
      public void insertUpdate( final DocumentEvent e )
      {
        modified( plainTextInput.getText(),
                  cipherParameters );
      }


      @Override
      public void removeUpdate( final DocumentEvent e )
      {
        modified( plainTextInput.getText(),
                  cipherParameters );
      }
    } );

    setToolTipText( "<html>"
                    + "The first two items (cipher text and initialization vector, if not empty)<br>"
                    + "are required. The third item serves to verify that the original plaintext<br>"
                    + "was recovered intact; in many cases it may be unnecessary. Items<br>"
                    + "can be separated from each other with whitespace, comma (,),<br>"
                    + "semicolon (;), or a pipe (|).<br>"
                    + "<br>"
                    + "Try modifying this to see what happens during decryption!<br>"
                    + "<br>"
                    + "Nothing shows here unless you have a secret key defined above." );
  }


  private void modified( final String plainTextInput,
                         final CipherParameterPane cipherParameters )
  {
    if( (algorithm != null) && (key != null) )
      {
        new SwingWorker<Void,Void>()
        {
          @Override
          public Void doInBackground()
          {
            try
              {
                final Cipher c = Cipher.getInstance( algorithm.spec() );
                byte[] iv = cipherParameters.getChosenInitVector();
                if( iv == null )
                  {
                    c.init( Cipher.ENCRYPT_MODE,
                            key );
                  }
                else
                  {
                    final IvParameterSpec ivSpec = new IvParameterSpec( iv );
                    c.init( Cipher.ENCRYPT_MODE,
                            key,
                            ivSpec );
                  }
                final byte[] plainText = plainTextInput.getBytes( UTF8 );
                final byte[] cipherText = c.doFinal( plainText );
                logger.info( "Cipher Text = " + Transformations.toString( cipherText ) );
                if( iv == null )
                  {
                    iv = c.getIV();
                    logger.info( "Randomly generated InitVector = " + Transformations.toString( iv ) );
                  }
                else
                  {
                    iv = c.getIV();
                    logger.info( "Fixed InitVector (bad idea!) = " + Transformations.toString( iv ) );
                  }

                final MessageDigest md = MessageDigest.getInstance( "SHA-256" );
                final byte[] digest = md.digest( plainText );
                logger.info( "Optional SHA-256 digest of plain text = " + Transformations.toString( digest ) );

                synchronized( CipherTextOutput.this )
                  {
                    setText( new String( Base64.encode( cipherText ) ) + "\n" + (iv == null
                        ? ""
                        : new String( Base64.encode( iv ) )) + "\n" + new String( Base64.encode( digest ) ) );
                  }
              }
            catch( final NoSuchAlgorithmException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" +
                                                   "The algorithm '" +
                                                   cipherParameters.getChosenAlgorithm().name() +
                                                   "' is unavailable!",
                                               "No Such Algorithm",
                                               JOptionPane.ERROR_MESSAGE );

              }
            catch( final NoSuchPaddingException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" +
                                                   "The padding '" +
                                                   cipherParameters.getChosenAlgorithm().padding() +
                                                   "' is unavailable!",
                                               "No Such Padding",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final InvalidKeyException x )
              {
                final String algorithmName = key.getAlgorithm();
                if( algorithm.name().equals( algorithmName ) )
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
                                                   "<html>" +
                                                       "The " +
                                                       (8 * key.getEncoded().length) +
                                                       "-bit SecretKey " +
                                                       " generated<br>" +
                                                       "for the cryptographic cipher " +
                                                       algorithmName +
                                                       ",<br>" +
                                                       "is invalid and cannot be used.",
                                                   "Invalid Key",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                else
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
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
              }
            catch( final InvalidAlgorithmParameterException e )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" + "Parameters (key and/or IV) are not valid",
                                               "Invalid Algorithm Parameter",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final IllegalBlockSizeException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" + "The block size is not valid",
                                               "Illegal Block Size",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final BadPaddingException x )
              {
                final String padding = cipherParameters.getChosenAlgorithm().padding();
                if( "NoPadding".equalsIgnoreCase( padding ) )
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
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
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
                                                   x.getMessage(),
                                                   "Bad Padding",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
              }
            return null;
          }
        }.execute();
      }
  }


  private void updateImage( final CipherParameterPane cipherParameters,
                            final JLabel inputPictureLabel,
                            final JLabel outputPictureLabel )
  {
    if( (algorithm != null) && (key != null) )
      {
        new SwingWorker<Void,Void>()
        {
          @Override
          public Void doInBackground()
          {
            try
              {
                final Cipher c = Cipher.getInstance( algorithm.spec() );
                final byte[] iv = cipherParameters.getChosenInitVector();

                final BufferedImage img = (BufferedImage)((ImageIcon)inputPictureLabel.getIcon()).getImage();
                final int wide = img.getWidth( null );
                final int high = img.getHeight( null );
                final byte[] rgba = new byte[wide * high * 4];
                int i = 0;
                for( int w = 0; w < wide; w++ )
                  {
                    for( int h = 0; h < high; h++ )
                      {
                        final int pixel = img.getRGB( w,
                                                      h );
                        rgba[i++] = (byte)(pixel >> 24);
                        rgba[i++] = (byte)((pixel >> 16) & 0xff);
                        rgba[i++] = (byte)((pixel >> 8) & 0xff);
                        rgba[i++] = (byte)(pixel & 0xff);
                      }
                  }

                if( iv == null )
                  {
                    c.init( Cipher.ENCRYPT_MODE,
                            key );
                  }
                else
                  {
                    final IvParameterSpec ivSpec = new IvParameterSpec( iv );
                    c.init( Cipher.ENCRYPT_MODE,
                            key,
                            ivSpec );
                  }
                final byte[] cRGBA = c.doFinal( rgba );

                final BufferedImage buf = new BufferedImage( wide,
                                                             high,
                                                             img.getType() );
                i = 0;
                for( int w = 0; w < wide; w++ )
                  {
                    for( int h = 0; h < high; h++ )
                      {
                        final int pixel = (cRGBA[i++] << 24) | (cRGBA[i++] << 16) | (cRGBA[i++] << 8) | cRGBA[i++];
                        buf.setRGB( w,
                                    h,
                                    pixel );
                      }
                  }

                outputPictureLabel.setIcon( new ImageIcon( buf ) );
              }
            catch( final NoSuchAlgorithmException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" +
                                                   "The algorithm '" +
                                                   cipherParameters.getChosenAlgorithm().name() +
                                                   "' is unavailable!",
                                               "No Such Algorithm",
                                               JOptionPane.ERROR_MESSAGE );

              }
            catch( final NoSuchPaddingException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" +
                                                   "The padding '" +
                                                   cipherParameters.getChosenAlgorithm().padding() +
                                                   "' is unavailable!",
                                               "No Such Padding",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final InvalidKeyException x )
              {
                final String algorithmName = key.getAlgorithm();
                if( algorithm.name().equals( algorithmName ) )
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
                                                   "<html>" +
                                                       "The " +
                                                       (8 * key.getEncoded().length) +
                                                       "-bit SecretKey " +
                                                       " generated<br>" +
                                                       "for the cryptographic cipher " +
                                                       algorithmName +
                                                       ",<br>" +
                                                       "is invalid and cannot be used.",
                                                   "Invalid Key",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
                else
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
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
              }
            catch( final InvalidAlgorithmParameterException e )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" + "Parameters (key and/or IV) are not valid",
                                               "Invalid Algorithm Parameter",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final IllegalBlockSizeException x )
              {
                JOptionPane.showMessageDialog( CipherTextOutput.this,
                                               "<html>" + "The block size is not valid",
                                               "Illegal Block Size",
                                               JOptionPane.ERROR_MESSAGE );
              }
            catch( final BadPaddingException x )
              {
                final String padding = cipherParameters.getChosenAlgorithm().padding();
                if( "NoPadding".equalsIgnoreCase( padding ) )
                  {
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
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
                    JOptionPane.showMessageDialog( CipherTextOutput.this,
                                                   x.getMessage(),
                                                   "Bad Padding",
                                                   JOptionPane.ERROR_MESSAGE );
                  }
              }
            return null;
          }
        }.execute();
      }
  }

  private Algorithm algorithm;
  private byte[] iv;
  private Key key;
  private final Logger logger;
  private static final Charset UTF8 = Charset.forName( "UTF-8" );
  private static final long serialVersionUID = -4667318381366459940L;
}
