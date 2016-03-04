package com.ringlord.cryptcreature;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.ringlord.Config;
import com.ringlord.CryptCreature;
import com.ringlord.swing.Images;


public class GUI
  extends JFrame
{
  public GUI()
  {
    super( "CryptCreature " + CryptCreature.VERSION + " (" + CryptCreature.VERDATE + ")" );

    try
      {
        final String osName = System.getProperty( "os.name" ).toUpperCase();
        if( (osName.indexOf( "WINDOWS" ) > -1) || // Microsoft © Windows
            (osName.indexOf( "MAC OS X" ) > -1) ) // Apple© Mac OS/X
          {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
          }
        else
          {
            // For all other platforms, use Nimbus
            UIManager.setLookAndFeel( "javax.swing.plaf.nimbus.NimbusLookAndFeel" );
          }
      }
    catch( final Exception x )
      {
        // Stay with whatever default UI Java has picked
      }

    this.config = new Config( new File( CryptCreature.storage(),
                                        "cryptcreature.conf" ) );

    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( final WindowEvent e )
      {
        shutdown();
      }
    } );

    ToolTipManager.sharedInstance().setDismissDelay( ToolTipManager.sharedInstance().getDismissDelay() * 2 );

    final List<Image> imageIcons = new ArrayList<>();
    imageIcons.add( Images.getImage( "/img/logo-64.png" ) );
    imageIcons.add( Images.getImage( "/img/logo-32.png" ) );
    imageIcons.add( Images.getImage( "/img/logo-24.png" ) );
    imageIcons.add( Images.getImage( "/img/logo-16.png" ) );
    setIconImages( imageIcons );

    // PARAMETERS
    final CipherParameterPane cipherParameters = new CipherParameterPane( config );

    cipherParameters.setBorder( BorderFactory.createEmptyBorder( 12,
                                                                 12,
                                                                 4,
                                                                 12 ) );

    // PLAIN TEXT INPUT
    final JTextArea plainTextInput = new JTextArea();
    final JPanel plainTextInputPanel = new JPanel( new BorderLayout() );
    plainTextInputPanel.add( BorderLayout.NORTH,
                             new JLabel( "1. Plain Text Input \u2014 To encrypt, type/paste your secret message here:" ) );
    plainTextInputPanel.add( BorderLayout.WEST,
                             new JLabel( "      " ) );
    plainTextInputPanel.add( BorderLayout.CENTER,
                             new JScrollPane( plainTextInput ) );

    // CIPHER TEXT SPEC OUTPUT
    final CipherTextOutput cipherTextOutput = new CipherTextOutput( cipherParameters,
                                                                    plainTextInput );
    final JPanel cipherTextOutputPanel = new JPanel( new BorderLayout() );
    cipherTextOutputPanel.add( BorderLayout.NORTH,
                               new JLabel( "2. Base64 encoded \u2026 CipherText/InitVector/SHA-256 Hash \u2014 Send this or paste cipher text here:" ) );
    cipherTextOutputPanel.add( BorderLayout.WEST,
                               new JLabel( "      " ) );
    cipherTextOutputPanel.add( BorderLayout.CENTER,
                               new JScrollPane( cipherTextOutput ) );

    // PLAIN TEXT OUTPUT
    final PlainTextOutput plainTextOutput = new PlainTextOutput( cipherParameters,
                                                                 cipherTextOutput );
    final JPanel plainTextOutputPanel = new JPanel( new BorderLayout() );
    plainTextOutputPanel.add( BorderLayout.NORTH,
                              new JLabel( "3. Decrypted from (2) above, producing the Plain Text again:" ) );
    plainTextOutputPanel.add( BorderLayout.WEST,
                              new JLabel( "      " ) );
    plainTextOutputPanel.add( BorderLayout.CENTER,
                              new JScrollPane( plainTextOutput ) );

    final JPanel ioPanel = new JPanel( new GridLayout( 0,
                                                       1,
                                                       0,
                                                       6 ) );
    ioPanel.add( plainTextInputPanel );
    ioPanel.add( cipherTextOutputPanel );
    ioPanel.add( plainTextOutputPanel );

    final Border outer = BorderFactory.createEmptyBorder( 4,
                                                          12,
                                                          0,
                                                          12 );
    final Border inner = BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Encryption & Deciphering" ),
                                                             BorderFactory.createEmptyBorder( 0,
                                                                                              4,
                                                                                              4,
                                                                                              4 ) );
    final JLabel status = new JLabel( "Logging to a file in " +
                                      new File( System.getProperty( "user.home" ),
                                                ".cryptcreature" ) +
                                      System.getProperty( "file.separator" ) );
    status.setBorder( BorderFactory.createEmptyBorder( 4,
                                                       12,
                                                       6,
                                                       12 ) );

    final JPanel layout = new JPanel( new BorderLayout() );
    layout.add( BorderLayout.NORTH,
                cipherParameters );
    layout.add( BorderLayout.CENTER,
                ioPanel );
    layout.add( BorderLayout.SOUTH,
                status );
    ioPanel.setBorder( BorderFactory.createCompoundBorder( outer,
                                                           inner ) );

    setContentPane( layout );
    pack();

    final Rectangle xywh = config.get( "main-window-xywh",
                                       (Rectangle)null );
    if( xywh != null )
      {
        setBounds( xywh );
      }
  }


  private void shutdown()
  {
    setVisible( false );
    final Rectangle xywh = getBounds();
    config.put( "main-window-xywh",
                xywh );
    try
      {
        config.close();
      }
    catch( final IOException x )
      {
        final Logger logger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME );
        logger.log( Level.WARNING,
                    "Cannot close config file " + config.file(),
                    x );
      }
    dispose();
  }

  private final Config config;
  private static final long serialVersionUID = 6617907165125822034L;
}
