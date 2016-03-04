package com.ringlord.cryptcreature;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.ringlord.Config;
import com.ringlord.mime.Base64;


/**
 * An input panel for cryptographic parameters.
 *
 * @author kusch211
 */
public class CipherParameterPane
  extends JPanel
{
  public static final String ACTION_SECRET_KEY_CHANGED = "KEY";
  public static final String ACTION_ALGORITHM_CHANGED = "ALG";
  public static final String ACTION_INITVECTOR_CHANGED = "IV";


  public CipherParameterPane( final Config config )
  {
    super( new BorderLayout( 6,
                             4 ) );

    final Model<String> algorithmModel = new Model<>();
    final Model<String> modesModel = new Model<>();
    final Model<String> paddingModel = new Model<>();
    final Model<Integer> keySizeModel = new Model<>();

    final JCheckBox forceFixedInitVector = new JCheckBox( "Force Initialization Vector: ",
                                                          false );
    forceFixedInitVector.setToolTipText( "<html>"
                                         + "<b>Do not enable this option, except for demonstration purposes!</b><br>"
                                         + "<br>"
                                         + "Initialization vectors must be unique for each encryption operation<br>"
                                         + "that uses the same key, otherwise the secret key can be discovered<br>"
                                         + "and compromised through cryptanalysis!" );
    this.fixedInitVector = new JTextField( 15 );
    final JButton randomInitVector = new JButton( "Random" );

    final JButton loadSecretKey = new JButton( "Load\u2026" );
    final JButton saveSecretKey = new JButton( "Save\u2026" );
    saveSecretKey.setEnabled( false );

    secretKey = new JTextField( 24 );
    secretKey.setForeground( new Color( 230,
                                        230,
                                        230,
                                        255 ) );
    secretKey.setBackground( new Color( 255,
                                        255,
                                        255,
                                        255 ) );
    secretKey.setSelectedTextColor( new Color( 255,
                                               255,
                                               255,
                                               255 ) );
    secretKey.setSelectionColor( new Color( 230,
                                            230,
                                            230,
                                            255 ) );
    final JButton randomSecretKey = new JButton( "Random" );

    final JTextField secretKeyHash = new JTextField( 24 );
    secretKeyHash.setEditable( false );
    secretKeyHash.setToolTipText( "<html>"
                                  + "To verify that someone else is in possession<br>"
                                  + "of the correct secret key, you can exchange<br>"
                                  + "this digest hash, instead, but <i>never transmit<br>"
                                  + "the </i>secret<i> key by insecure means!</i> Digests<br>"
                                  + "are one-way cryptographic functions, which<br>"
                                  + "reveal nothing about the original data, but<br>"
                                  + "provide a type of data signature." );

    final JComboBox<String> keyHashDigest = new JComboBox<>( new String[]{"SHA-256","SHA-1","MD5"} );
    keyHashDigest.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent e )
      {
        if( e.getStateChange() == ItemEvent.SELECTED )
          {
            updateSecretKeyDigest( secretKey,
                                   secretKeyHash,
                                   keyHashDigest );
          }
      }
    } );
    keyHashDigest.setToolTipText( "<html>"
                                  + "The digest algorithm used<br>"
                                  + "to create a hash of the<br>"
                                  + "secret key." );

    loadSecretKey.setToolTipText( "<html>"
                                  + "Load secret key from a file on disk.<br>"
                                  + "<b>Keep it secret, keep it safe!</b>" );
    saveSecretKey.setToolTipText( "<html>"
                                  + "Save current secret key to a file on disk.<br>"
                                  + "<b>Keep it secret, keep it safe!</b>" );
    randomSecretKey.addMouseListener( new MouseAdapter()
    {
      @Override
      public void mouseEntered( final MouseEvent e )
      {
        final String name = (String)algorithmNames.getSelectedItem();
        final String mode = (String)modeNames.getSelectedItem();
        final String padding = (String)paddingNames.getSelectedItem();
        final Algorithm a = Algorithm.find( name,
                                            mode,
                                            padding );

        try
          {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance( a.name() );
            String text = keyGenerator.getProvider().toString().trim();
            text = text.replaceAll( "&",
                                    "&amp;" );
            text = text.replaceAll( "<",
                                    "&lt;" );
            text = text.replaceAll( ">",
                                    "&gt;" );
            text = text.replaceAll( "'",
                                    "&apos;" );
            text = text.replaceAll( "\"",
                                    "&quot;" );
            randomSecretKey.setToolTipText( "<html>" +
                                            "Generate a secret key randomly, using the<br>" +
                                            "following provider for the <i>" +
                                            a.name() +
                                            "</i> algorithm:<br>" +
                                            "<pre style=\"margin-left:8px;\"><i>" +
                                            text +
                                            "</i></pre>" +
                                            "Note that the randomness of the key is of<br>" +
                                            "extreme importance: Compromised software<br>" +
                                            "and even compromised hardware exists. All<br>" +
                                            "randomness is not created equal!" );
          }
        catch( final Exception x )
          {
            randomSecretKey.setToolTipText( "<html>"
                                            + "Generate a secret key randomly,<br>"
                                            + "using an algorithm which does<br>"
                                            + "not supply provider information.<br>"
                                            + "You might want to distrust it." );
          }
      }
    } );

    keySizes = new JComboBox<>( keySizeModel );

    algorithmNames = new JComboBox<>( algorithmModel );
    algorithmNames.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent e )
      {
        if( e.getStateChange() == ItemEvent.SELECTED )
          {
            final String algorithm = (String)algorithmNames.getSelectedItem();
            modesModel.update( Algorithm.allModesFor( algorithm ) );

            final String mode = (String)modeNames.getSelectedItem();
            final String padding = (String)paddingNames.getSelectedItem();
            final Algorithm a = Algorithm.find( algorithm,
                                                mode,
                                                padding );
            if( a != null )
              {
                final int[] values = a.keySizes();
                final Integer[] items = new Integer[values.length];
                for( int i = 0; i < values.length; i++ )
                  {
                    items[i] = values[i];
                  }
                keySizeModel.update( items );

                fixedInitVector.setText( "" );
                secretKey.setText( "" );
                notifyActionListeners( ACTION_ALGORITHM_CHANGED );
              }
          }
      }
    } );
    algorithmNames.setToolTipText( "<html>"
                                   + "These are the names of algorithms that appear<br>"
                                   + "be be installed on your computer and accessible<br>"
                                   + "to your Java Virtual Machine. Pick one that you<br>"
                                   + "have researched and found to be suitable for<br>"
                                   + "your needs. Using the wrong algorithm can<br>"
                                   + "become a liability for you." );

    modeNames = new JComboBox<>( modesModel );
    modeNames.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent e )
      {
        if( e.getStateChange() == ItemEvent.SELECTED )
          {
            final String algorithm = (String)algorithmNames.getSelectedItem();
            final String mode = (String)modeNames.getSelectedItem();
            paddingModel.update( Algorithm.allPaddingsFor( algorithm,
                                                           mode ) );
            notifyActionListeners( ACTION_ALGORITHM_CHANGED );

            switch( mode )
              {
              case "CBC":
                modeNames.setToolTipText( "<html>"
                                          + "CBC (Cipher Block Chaining) mode is<br>"
                                          + "is generally considered suitable for<br>"
                                          + "use in production systems, and is<br>"
                                          + "recommended by Niels Ferguson and<br>"
                                          + "Bruce Schneier (two well-known<br>"
                                          + "cryptographers)." );
                break;

              case "PCBC":
                modeNames.setToolTipText( "<html>"
                                          + "PCBC (Propagating Cipher Block Chaining)<br>"
                                          + "mode causes small changes in the cipher<br>"
                                          + "text to propagate indefinitely." );
                break;

              case "CFB":
                modeNames.setToolTipText( "<html>"
                                          + "CFB (Cipher Feedback) mode is a<br>"
                                          + "self-synchronizing mode, which will<br>"
                                          + "recover if part of the ciphertext is<br>"
                                          + "lost (only parts of the message will<br>"
                                          + "be lost, in other words)" );

                break;

              case "CTR":
                modeNames.setToolTipText( "<html>"
                                          + "CTR (Counter) mode is generally considered<br>"
                                          + "suitable for use in production systems, and<br>"
                                          + "is recommended by Niels Ferguson and<br>"
                                          + "Bruce Schneier (two well-known<br>"
                                          + "cryptographers)" );
                break;

              case "ECB":
                modeNames.setToolTipText( "<html>"
                                          + "<font color='red'>Warning: ECB (Electronic Code Book) mode<br>"
                                          + "is not recommended for production systems</font>,<br>"
                                          + "as it causes patterns in the plaintext to show<br>"
                                          + "up in the ciphertext, thus offering a potential<br>"
                                          + "wealth of data to cryptanalysts!<br>" );
                break;

              case "OFB":
                modeNames.setToolTipText( "<html>"
                                          + "OFB (Output Feedback) mode is a<br>"
                                          + "completely symmetric mode." );
                break;

              default:
                modeNames.setToolTipText( "No information on this mode is available" );
              }
          }
      }
    } );

    paddingNames = new JComboBox<>( paddingModel );

    keySizes.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent e )
      {
        if( e.getStateChange() == ItemEvent.SELECTED )
          {
            fixedInitVector.setText( "" );
            secretKey.setText( "" );
            notifyActionListeners( ACTION_ALGORITHM_CHANGED );
          }
      }
    } );
    keySizes.setToolTipText( "<html>"
                             + "Supported key lengths. If you see<br>"
                             + "no keys of size 192 or larger, then<br>"
                             + "your JVM's security policy does not<br>"
                             + "allow for unlimited strength crypto.<br>"
                             + "<br>"
                             + "Longer keys require significantly<br>"
                             + "greater computational resources<br>"
                             + "for brute-force attacks. Example:<br>"
                             + "a 128-bit key has 2<sup>128</sup> variations<br>"
                             + "(a 38-digit decimal number); a<br>"
                             + "192-bit key has 2<sup>192</sup> variations (a<br>"
                             + "57-digit decimal number), which<br>"
                             + "is 18 446 744 073 709 551 612<br>"
                             + "times more difficult to break using<br>"
                             + "brute-force computations." );

    algorithmModel.update( Algorithm.allNames() );

    final JPanel p1 = new JPanel( new GridLayout( 2,
                                                  1 ) );
    p1.add( new JLabel( "Name" ) );
    p1.add( algorithmNames );

    final JPanel p2 = new JPanel( new GridLayout( 2,
                                                  1 ) );
    p2.add( new JLabel( "Mode" ) );
    p2.add( modeNames );

    final JPanel p3 = new JPanel( new GridLayout( 2,
                                                  1 ) );
    p3.add( new JLabel( "Padding" ) );
    p3.add( paddingNames );

    final JPanel p4 = new JPanel( new GridLayout( 2,
                                                  1 ) );
    p4.add( new JLabel( "Key Size (bits)" ) );
    p4.add( keySizes );

    final JPanel algorithmInfo = new JPanel( new GridLayout( 1,
                                                             0,
                                                             6,
                                                             0 ) );
    algorithmInfo.add( p1 );
    algorithmInfo.add( p2 );
    algorithmInfo.add( p3 );
    algorithmInfo.add( p4 );
    algorithmInfo.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Cryptographic Algorithm & Parameters" ),
                                                                 BorderFactory.createEmptyBorder( 0,
                                                                                                  4,
                                                                                                  4,
                                                                                                  4 ) ) );

    fixedInitVector.setEnabled( false );
    randomInitVector.setEnabled( false );
    final JPanel ivPanel = new JPanel( new BorderLayout() );
    ivPanel.add( BorderLayout.WEST,
                 forceFixedInitVector );
    ivPanel.add( BorderLayout.CENTER,
                 fixedInitVector );
    ivPanel.add( BorderLayout.EAST,
                 randomInitVector );
    final Color normalForeground = forceFixedInitVector.getForeground();
    forceFixedInitVector.addItemListener( new ItemListener()
    {
      @Override
      public void itemStateChanged( final ItemEvent e )
      {
        if( e.getStateChange() == ItemEvent.SELECTED )
          {
            fixedInitVector.setEnabled( true );
            randomInitVector.setEnabled( true );
            forceFixedInitVector.setForeground( Color.red );
          }
        else
          {
            fixedInitVector.setEnabled( false );
            randomInitVector.setEnabled( false );
            forceFixedInitVector.setForeground( normalForeground );
          }
      }
    } );
    randomInitVector.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent e )
      {
        final String name = (String)algorithmNames.getSelectedItem();
        final String mode = (String)modeNames.getSelectedItem();
        final String padding = (String)paddingNames.getSelectedItem();
        final Algorithm a = Algorithm.find( name,
                                            mode,
                                            padding );

        try
          {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance( a.name() );
            keyGenerator.init( (int)keySizes.getSelectedItem() );
            final Key key = keyGenerator.generateKey();

            final Cipher c = Cipher.getInstance( a.spec() );
            c.init( Cipher.ENCRYPT_MODE,
                    key );
            final byte[] iv = c.getIV();
            fixedInitVector.setText( new String( Base64.encode( iv ) ) );
            fixedInitVector.setToolTipText( "IV Length = " + (8 * iv.length) + " bits" );
          }
        catch( NoSuchAlgorithmException |
               NoSuchPaddingException |
               InvalidKeyException x )
          {
            x.printStackTrace();
          }
      }
    } );

    randomSecretKey.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent arg0 )
      {
        final String name = (String)algorithmNames.getSelectedItem();
        final String mode = (String)modeNames.getSelectedItem();
        final String padding = (String)paddingNames.getSelectedItem();
        final Algorithm a = Algorithm.find( name,
                                            mode,
                                            padding );

        try
          {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance( a.name() );
            keyGenerator.init( (int)keySizes.getSelectedItem() );
            final Key key = keyGenerator.generateKey();
            final byte[] keyBytes = key.getEncoded();
            secretKey.setText( new String( Base64.encode( keyBytes ) ) );
            secretKey.setToolTipText( "<html>" +
                                      "Secret Key Length = " +
                                      (8 * keyBytes.length) +
                                      " bits<br>" +
                                      "<b>Keep it secret, keep it safe!</b>" );
          }
        catch( final NoSuchAlgorithmException x )
          {
            x.printStackTrace();
          }
      }
    } );

    loadSecretKey.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent e )
      {
        if( keyFileChooser == null )
          {
            keyFileChooser = new JFileChooser();
            keyFileChooser.setCurrentDirectory( new File( config.get( "key-dir",
                                                                      System.getProperty( "user.home" ) ) ) );
            keyFileChooser.setMultiSelectionEnabled( false );
          }
        keyFileChooser.setDialogTitle( "Load Secret Key" );
        keyFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );
        if( keyFileChooser.showDialog( CipherParameterPane.this,
                                       "Load" ) == JFileChooser.APPROVE_OPTION )
          {
            final File selectedFile = keyFileChooser.getSelectedFile();
            config.put( "key-dir",
                        selectedFile.getParent() );

            try( InputStream f = new FileInputStream( selectedFile ) )
              {
                final ByteArrayOutputStream keyData = new ByteArrayOutputStream();
                final byte[] buffer = new byte[128];
                int inBuffer;
                while( (inBuffer = f.read( buffer )) > -1 )
                  {
                    keyData.write( buffer,
                                   0,
                                   inBuffer );
                  }
                keyData.flush();
                final byte[] key = keyData.toByteArray();
                final int keySizeBits = (key.length * 8);
                final int selectedSize = (int)keySizes.getSelectedItem();
                boolean ok;
                if( selectedSize == keySizeBits )
                  {
                    ok = true;
                  }
                else
                  {
                    ok = false;
                    final ComboBoxModel<Integer> keySizeModel = keySizes.getModel();
                    final int modelSize = keySizeModel.getSize();
                    for( int i = 0; i < modelSize; i++ )
                      {
                        final Integer size = keySizeModel.getElementAt( i );
                        if( size == keySizeBits )
                          {
                            keySizes.setSelectedItem( size );
                            ok = true;
                            break;
                          }
                      }
                  }
                if( ok )
                  {
                    secretKey.setText( new String( Base64.encode( key ) ) );
                    secretKey.setToolTipText( "Secret Key Length = " + keySizeBits + " bits" );
                  }
              }
            catch( final Exception x )
              {
                x.printStackTrace();
              }
          }
      }
    } );
    saveSecretKey.addActionListener( new ActionListener()
    {
      @Override
      public void actionPerformed( final ActionEvent e )
      {
        final Key key = getChosenSecretKey();
        if( key != null )
          {
            final byte[] keyBytes = key.getEncoded();

            if( keyFileChooser == null )
              {
                keyFileChooser = new JFileChooser();
                keyFileChooser.setCurrentDirectory( new File( config.get( "key-dir",
                                                                          System.getProperty( "user.home" ) ) ) );
                keyFileChooser.setMultiSelectionEnabled( false );
              }
            keyFileChooser.setDialogTitle( "Save Secret Key" );
            keyFileChooser.setDialogType( JFileChooser.SAVE_DIALOG );
            if( keyFileChooser.showDialog( CipherParameterPane.this,
                                           "Save" ) == JFileChooser.APPROVE_OPTION )
              {
                try( OutputStream f = new FileOutputStream( keyFileChooser.getSelectedFile() ) )
                  {
                    f.write( keyBytes );
                    f.flush();
                  }
                catch( final Exception x )
                  {
                    x.printStackTrace();
                  }
              }
          }
      }
    } );

    fixedInitVector.getDocument().addDocumentListener( new DocumentListener()
    {
      @Override
      public void changedUpdate( final DocumentEvent arg0 )
      {
      }


      @Override
      public void insertUpdate( final DocumentEvent arg0 )
      {
        modified();
      }


      @Override
      public void removeUpdate( final DocumentEvent arg0 )
      {
        modified();
      }


      private void modified()
      {
        notifyActionListeners( ACTION_INITVECTOR_CHANGED );
      }
    } );

    secretKey.getDocument().addDocumentListener( new DocumentListener()
    {
      @Override
      public void changedUpdate( final DocumentEvent arg0 )
      {
      }


      @Override
      public void insertUpdate( final DocumentEvent arg0 )
      {
        modified();
      }


      @Override
      public void removeUpdate( final DocumentEvent arg0 )
      {
        modified();
      }


      private void modified()
      {
        notifyActionListeners( ACTION_SECRET_KEY_CHANGED );
        saveSecretKey.setEnabled( secretKey.getText().length() > 0 );
        updateSecretKeyDigest( secretKey,
                               secretKeyHash,
                               keyHashDigest );
      }
    } );

    secretKey.setToolTipText( "<html>"
                              + "A Base64-encoded secret key to match<br>"
                              + "both the algorithm and key size. <i>Never<br>"
                              + "exchange a secret key over insecure<br>"
                              + "channels!</i>" );

    final JPanel keyButtons = new JPanel( new GridLayout( 1,
                                                          0,
                                                          4,
                                                          0 ) );
    keyButtons.add( loadSecretKey );
    keyButtons.add( saveSecretKey );
    keyButtons.add( randomSecretKey );

    final JPanel secretKeyPanel = new JPanel( new BorderLayout( 4,
                                                                0 ) );
    secretKeyPanel.add( BorderLayout.CENTER,
                        secretKey );
    secretKeyPanel.add( BorderLayout.EAST,
                        keyButtons );

    final JPanel secretHashPanel = new JPanel( new BorderLayout( 4,
                                                                 0 ) );
    secretHashPanel.add( BorderLayout.CENTER,
                         secretKeyHash );
    secretHashPanel.add( BorderLayout.EAST,
                         keyHashDigest );

    final JPanel keyLabels = new JPanel( new GridLayout( 0,
                                                         1,
                                                         0,
                                                         4 ) );
    final JPanel keyFields = new JPanel( new GridLayout( 0,
                                                         1,
                                                         0,
                                                         4 ) );
    keyLabels.add( new JLabel( "Secret key:" ) );
    keyLabels.add( new JLabel( "Key digest:" ) );
    keyFields.add( secretKeyPanel );
    keyFields.add( secretHashPanel );

    final JPanel keyPanel = new JPanel( new BorderLayout( 4,
                                                          0 ) );
    keyPanel.add( BorderLayout.WEST,
                  keyLabels );
    keyPanel.add( BorderLayout.CENTER,
                  keyFields );

    final JPanel ivKeyPanel = new JPanel( new BorderLayout( 0,
                                                            4 ) );
    ivKeyPanel.add( BorderLayout.NORTH,
                    ivPanel );
    ivKeyPanel.add( BorderLayout.SOUTH,
                    keyPanel );
    ivKeyPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Initialization Vector & Secret Key" ),
                                                              BorderFactory.createEmptyBorder( 0,
                                                                                               4,
                                                                                               4,
                                                                                               4 ) ) );

    final JPanel parametersPanel = new JPanel( new BorderLayout( 0,
                                                                 4 ) );
    parametersPanel.add( BorderLayout.NORTH,
                         algorithmInfo );
    parametersPanel.add( BorderLayout.SOUTH,
                         ivKeyPanel );

    algorithmInfo.setToolTipText( "<html>"
                                  + "The cipher, mode, and padding, along with the key size<br>"
                                  + "determine the methodology and strength of the encryption." );
    ivKeyPanel.setToolTipText( "<html>"
                               + "Most secure algorithms require a unique initialization vector<br>"
                               + "for each encryption operation, and which matches decryption;<br>"
                               + "and a secret key that is shared (secretly!) between both parties." );

    add( BorderLayout.NORTH,
         parametersPanel );
  }


  public void addActionListener( final ActionListener l )
  {
    actionListeners.add( l );
  }


  public void removeActionListener( final ActionListener l )
  {
    actionListeners.remove( l );
  }


  private void notifyActionListeners( final String actionName )
  {
    final Set<ActionListener> listeners = new HashSet<>();
    listeners.addAll( actionListeners );
    if( !listeners.isEmpty() )
      {
        final ActionEvent e = new ActionEvent( this,
                                               0,
                                               actionName );
        for( final ActionListener l : actionListeners )
          {
            l.actionPerformed( e );
          }
      }
  }


  public Algorithm getChosenAlgorithm()
  {
    final String name = (String)algorithmNames.getSelectedItem();
    final String mode = (String)modeNames.getSelectedItem();
    final String padding = (String)paddingNames.getSelectedItem();
    return Algorithm.find( name,
                           mode,
                           padding );
  }


  public int getChosenSecretKeySize()
  {
    return (int)keySizes.getSelectedItem();
  }


  /**
   *
   * @return The {@link Key} of a chosen secret key, or null if none has yet
   *         been chosen.
   *         <em>A Key must be available for encryption to work</em>
   */
  public Key getChosenSecretKey()
  {
    final byte[] keyBytes = secretKey.getText().trim().getBytes();
    if( keyBytes.length > 0 )
      {
        final byte[] secretKeyBytes = Base64.decode( keyBytes );
        final Key key = new SecretKeySpec( secretKeyBytes,
                                           (String)algorithmNames.getSelectedItem() );
        return key;
      }
    return null;
  }


  /**
   *
   * @return The bytes of a fixed init vector, or null if a random init vector
   *         is to be used. Note that random init vectors are generated
   *         automatically by {@link Cipher#init}.
   */
  public byte[] getChosenInitVector()
  {
    if( fixedInitVector.isEnabled() )
      {
        final byte[] ivBytes = fixedInitVector.getText().trim().getBytes();
        if( ivBytes.length > 0 )
          {
            return Base64.decode( ivBytes );
          }
      }
    return null;
  }


  private static void updateSecretKeyDigest( final JTextField secretKey,
                                             final JTextField secretKeyHash,
                                             final JComboBox<String> keyHashDigest )
  {
    final byte[] keyBytes = secretKey.getText().trim().getBytes();
    if( keyBytes.length > 0 )
      {
        final byte[] secretKeyBytes = Base64.decode( keyBytes );
        final String digestAlgorithm = "" + keyHashDigest.getSelectedItem();
        try
          {
            final MessageDigest md = MessageDigest.getInstance( digestAlgorithm );
            final byte[] hash = md.digest( secretKeyBytes );
            final String hash64 = new String( Base64.encode( hash ) );
            secretKeyHash.setText( hash64 );
            return;
          }
        catch( final NoSuchAlgorithmException x )
          {
            // TODO Auto-generated catch block
            x.printStackTrace();
          }
      }
    secretKeyHash.setText( "" );
  }


  private class Model<T>
    extends DefaultComboBoxModel<T>
  {
    final void update( final T[] values )
    {
      @SuppressWarnings("unchecked") final T current = (T)getSelectedItem();
      T reSelect = null;

      removeAllElements();
      for( final T item : values )
        {
          addElement( item );
          if( item.equals( current ) )
            {
              reSelect = item;
            }
        }
      if( reSelect != null )
        {
          setSelectedItem( reSelect );
        }
      fireContentsChanged( this,
                           0,
                           Integer.MAX_VALUE );
    }
    private static final long serialVersionUID = -8939982052355500812L;
  }

  private JFileChooser keyFileChooser;
  //
  private final JComboBox<String> algorithmNames;
  private final JComboBox<String> modeNames;
  private final JComboBox<String> paddingNames;
  private final JComboBox<Integer> keySizes;
  private final JTextField fixedInitVector;
  private final JTextField secretKey;
  private final Set<ActionListener> actionListeners = new HashSet<>();
  private static final long serialVersionUID = 2914728362631572790L;
}
