import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;


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
	public void setNick(String s){this.nick = s;}

	public String getState(){return this.state;}
	public void setState(String s){this.state = s;}

	public SocketChannel getSocketChannel(){return this.sc;}
	public void setSocketChannel(SocketChannel sc){this.sc = sc;}

	public chatRoom getChatRoom(){return this.room;}
	public void setChatRoom(chatRoom c){this.room = c;}
}

class chatRoom{

	private String nome;
	private HashMap<String,User> chatters;

	chatRoom(String nome){
		this.nome = nome;
		this.chatters = new HashMap<String,User>();
	}

	public String getRoomName(){return this.nome;}
	public User[] getAllClients(){return this.chatters.values().toArray(new User[this.chatters.size()]);}
	public void addChatter(User chatter) {this.chatters.put(chatter.getNick(), chatter);}
	public void removeChatter(User chatter) {this.chatters.remove(chatter.getNick());}

}

public class ChatServer
{
	// A pre-allocated buffer for the received data
	static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

	// Decoder for incoming text -- assume UTF-8
	static private final Charset charset = Charset.forName("UTF8");
	static private final CharsetEncoder encoder = charset.newEncoder();
	static private final CharsetDecoder decoder = charset.newDecoder();

	// users and chat rooms
	static private HashMap<SocketChannel,User> chatters = new HashMap<SocketChannel,User>();
	static private HashMap<String,User> nicks = new HashMap<String,User>();
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
						chatters.put(sc, new User(sc));

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
		// mensagem de um user sem nick
		// aqui responde a mensagem, independentemente do estado ou user
		giveResponse(sc, message);


		return true;
	}

	// handle the message
	static private void giveResponse(SocketChannel socket, String message) throws IOException {
		User aux = chatters.get(socket);
		// se for comando
		if(message.charAt(0)=='/'){
			String[] msg_pieces = message.split("\\s");
			
			switch(msg_pieces[0]){
			case "/nick":
				//init
				if(aux.getState().equals("init")){
					///nick nome && !disponível(nome)  
					if(nicks.containsKey(msg_pieces[1])){
						// ERROR
						serverResponse(socket, "ERROR\n");
						return;
					}
					///nick nome && disponível(nome)
					else if(!nicks.containsKey(msg_pieces[1])){
						aux.setNick(msg_pieces[1]);
						aux.setState("outside");
						nicks.put(msg_pieces[1], aux);
						// OK
						serverResponse(socket, "OK\n");
						return;
					}
				}
				//outside
				else if(aux.getState().equals("outside")){
					///nick nome && !disponível(nome)
					if(nicks.containsKey(msg_pieces[1])){
						//ERROR
						//mantem nome antigo
						serverResponse(socket, "ERROR\n");
						return;
					}
					///nick nome && disponível(nome)
					else if(!nicks.containsKey(msg_pieces[1])){
						nicks.remove(aux.getNick());
						//OK
						aux.setNick(msg_pieces[1]);
						nicks.put(msg_pieces[1], aux);
						serverResponse(socket, "OK\n");
						return;
					}
				}
				//inside
				else if(aux.getState().equals("inside")){
					///nick nome && !disponível(nome)
					if(nicks.containsKey(msg_pieces[1])){
						//ERROR
						serverResponse(socket, "ERROR\n");
						return;
					}
					///nick nome && disponível(nome)
					else if (!chatters.containsKey(msg_pieces[1])){
						// OK para user que mudou 
						// NEWNICK antigo novo para os outros da sala
						nicks.remove(aux.getNick());
						aux.setNick(msg_pieces[1]);
						nicks.put(msg_pieces[1], aux);
						User[] roomUsers = aux.getChatRoom().getAllClients();
						System.out.println(roomUsers);
						serverResponse(socket, "OK\n");
						return;
					}
				}
				break;
			case "/join":
				// se a sala nao existir, tem de a criar
				if(!rooms.containsKey(msg_pieces)[1]) chatRoom aux = new chatRoom(msg_pieces[1]);
				//outside 	/join sala
				if(aux.getState().equals("outside")){
					aux.setChatRoom(rooms.get(msg_pieces[1]));
					aux.setState("inside");
				}
				//inside 	/join sala
				else if(aux.getState().equals("inside")){
					// definir salas nova e antiga
					chatRoom targetRoom = rooms.get(msg_pieces[1]);
					chatRoom oldRoom = aux.getChatRoom();
					// dizer a todos os clientes da sala nova que o aux se vai juntar
					aux.setChatRoom(rooms.get(msg_pieces[1]));
					for(User u : targetRoom.getAllClients()){
						if(!u.getNick().equals(aux.getNick()))
							serverResponse(u.getSocketChannel(),"JOINED "+aux.getNick()+"\n");
					}
					// anunciar aos clientes da sala antiga que aux saiu
					for(User u : oldRoom.getAllClients()){
						serverResponse(u.getSocketChannel(), "LEFT "+aux.getNick()+"\n");
					}
					
				}
				// se estiver em init, msg de erro a dizer que n pode juntar-se sem nick
				else{
					serverResponse(aux.getSocketChannel(), "ERROR\n");
				}
					break;
			case "/leave":
				// dentro de uma sala
				if(aux.getState().equals("inside")){
					chatRoom oldRoom = aux.getChatRoom(); 
					aux.setChatRoom(null); 
					for(User u : oldRoom.getAllClients()){
						 serverResponse(u.getSocketChannel(), "LEFT "+aux.getNick()+"\n");
					 }
					 aux.setState("outside");
					 serverResponse(aux.getSocketChannel(), "OK\n");
				}
				//fora de qq sala
				else{
					serverResponse(aux.getSocketChannel(), "ERROR\n");
				}
				break;
			case "/bye":
				//dentro de uma sala
				if(aux.getState().equals("inside")){
					// left para os outros clientes da sala
					chatRoom oldRoom = aux.getChatRoom();
					aux.setChatRoom(null);
					for(User u :oldRoom.getAllClients()){
						serverResponse(u.getSocketChannel(), "LEFT "+aux.getNick()+"\n");
					}
					// bye para o cliente que saiu
					serverResponse(aux.getSocketChannel(), "BYE\n");
					//fechar aqui conexao a quem saiu
					aux.getSocketChannel().close();
					
				}
				//fora de uma sala ou mesmo sem nick
				else{
					//BYE para o utilizador servidor fecha a conexão ao cliente
					serverResponse(aux.getSocketChannel(), "BYE\n");
					aux.getSocketChannel().close();
				}
				break;
			default:
				break;
			}
		}
		// se nao for comando
		else{
			if(aux.getState().equals("inside")){
				for(User u : aux.getChatRoom().getAllClients()){
					serverResponse(u.getSocketChannel(), "MESSAGE "+aux.getNick()+" "+message);
				}
			}
			else{
				serverResponse(aux.getSocketChannel(), "ERROR\n");
			}
		}
	}	// end of method
	
	private static void serverResponse(SocketChannel sc, String message) throws IOException {
		sc.write(encoder.encode(CharBuffer.wrap(message)));
	}

}// end of ChatServer
