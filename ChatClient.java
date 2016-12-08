import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

	// Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
	JFrame frame = new JFrame("Chat Client");
	private JTextField chatBox = new JTextField();
	private JTextArea chatArea = new JTextArea();
	// --- Fim das variáveis relacionadas coma interface gráfica

	// Se for necessário adicionar variáveis ao objecto ChatClient, devem
	// ser colocadas aqui


	Socket clientSocket = null; //socket do cliente
	PrintWriter tellServer = null; // printer pra escrever pro sv
	BufferedReader fromServer = null; //guarda msg que vem do sv
	String command = null; //comando (/join, /nick, etc)





	// Método a usar para acrescentar uma string à caixa de texto
	// * NÃO MODIFICAR *
	public void printMessage(final String message) {
		chatArea.append(message);
	}


	// Construtor
	public ChatClient(String server, int port) throws IOException {

		// Inicialização da interface gráfica --- * NÃO MODIFICAR *
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(chatBox);
		frame.setLayout(new BorderLayout());
		frame.add(panel, BorderLayout.SOUTH);
		frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
		frame.setSize(500, 300);
		frame.setVisible(true);
		chatArea.setEditable(false);
		chatBox.setEditable(true);
		chatBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					newMessage(chatBox.getText());
				} catch (IOException ex) {
				} finally {
					chatBox.setText("");
				}
			}
		});
		// --- Fim da inicialização da interface gráfica

		// Se for necessário adicionar código de inicialização ao
		// construtor, deve ser colocado aqui

		try{
			clientSocket = new Socket(server,port);
			tellServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}
		catch(IOException e){
			e.printStackTrace();
		}


	}// fim do chatClient


	// Método invocado sempre que o utilizador insere uma mensagem
	// na caixa de entrada
	public void newMessage(String message) throws IOException {
		// PREENCHER AQUI com código que envia a mensagem ao servidor
		tellServer.println(message); // imprime no sv
		command = message;
		String[] tokens = command.split("\\s"); // espaços em branco
		tellServer.flush();
	}


	// Método principal do objecto
	public void run() throws IOException {
		// PREENCHER AQUI

		while(true){
			// ler proxima msg que o sv ns manda
			String rcvMsg = fromServer.readLine();
			if(rcvMsg == null) break;
			// partir a msg em tokens, pelos espaços em branco
			String[] broken_msg = rcvMsg.split("\\s");
			if(broken_msg.length == 1){
				// executou-se um comando e correu bem
				if(broken_msg[0].equals("OK")){
					printMessage("All went well with command: "+command);
				}
				// um comando executado deu asneira
				else if(broken_msg[0].equals("ERROR")){
					printMessage("Hum... There's something wrong with "+command);
				}
				// saida com sucesso do chat
				else if(broken_msg[0].equals("BYE")){
					printMessage("Aaaaaaaaaaaaaaaaand i am outta here!");
				}
			}

			else{
				// mudança de nick
				if (broken_msg[0].equals("NEWNICK")){
					printMessage("Chat member known as "+broken_msg[1]+" is now named "+broken_msg[2]);
				}
				// aviso de um novo user na sala
				else if(broken_msg[0].equals("JOINED")){
					printMessage("A new user has entered the room, goes by: "+broken_msg[1]);
				}
				// aviso de saida de user da sala
				else if (broken_msg[0].equals("LEFT")) {
					printMessage("User "+broken_msg[1]+" has left the chat room");
				}
			}
		}
		
		clientSocket.close();
	}


	// Instancia o ChatClient e arranca-o invocando o seu método run()
	// * NÃO MODIFICAR *
	public static void main(String[] args) throws IOException {
		ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
		client.run();
	}

}
