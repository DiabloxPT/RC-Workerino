import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
	
	private String nick;
	private String state;
	private SocketChannel sc;
	private chatRoom room;
	
	public User(SocketChannel sChannel) {
		this.nick = "";
		this.state = "init";
		this.sc = sChannel;
		this.room = null;
	}
	
	public String getNick(){return this.nick;}
	public String getState(){return this.state;}
	public SocketChannel getSocketChannel(){return this.sc;}
	public chatRoom getChatRoom(){return this.room;}
	
}

class chatRoom{
	
	private String nome;
	private HashMap<String,User> chatters;
	
	chatRoom(String nome){
		this.nome = nome;
		this.chatters = new HashMap<String,User>();
	}
	
	public String getRoomName(){return this.nome;}
	public HashMap<String,User> getAllClients(){return this.chatters;}
	
}

public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  // users and chat rooms
  static private HashMap<SocketChannel,User> chatters = new HashMap<SocketChannel,User>();
  static private HashMap<String,chatRoom> rooms = new HashMap<String,chatRoom>();

  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
            SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );

          } else if ((key.readyOps() & SelectionKey.OP_READ) ==
            SelectionKey.OP_READ) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc );

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SocketChannel sc ) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }

    // Decode and print the message to stdout
    String message = decoder.decode(buffer).toString();
    System.out.print( message );
    giveResponse(message,sc);
    
    return true;
  }
  
  // handle the message
  static private void giveResponse(String message,SocketChannel socket){
	  // se for comando
	  if(message.charAt(0)=='/'){
		  String[] msg_pieces = message.split("\\s");
		  switch(msg_pieces[0]){
		  case "/nick":
			  //init 	/nick nome && !disponível(nome)
			  //init 	/nick nome && disponível(nome)
			  //outside 	/nick nome && !disponível(nome)
			  //outside 	/nick nome && disponível(nome)
			  //inside 	/nick nome && !disponível(nome)
			  //inside 	/nick nome && disponível(nome)
			  break;
		  case "/join":
			  break;
		  case "/leave":
			  break;
		  case "/bye":
			  break;
		  default:
			  break;
		  }
	  }
	  // se nao for comando
	  else{
		  
	  }
  }	// end of method
  
}// end of ChatServer
