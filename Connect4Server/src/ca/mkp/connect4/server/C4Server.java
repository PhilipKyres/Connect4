package ca.mkp.connect4.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Accepts incoming client connections indefinitely.
 * Spins off a new thread for each new client.
 * 
 * @author Philip Kyres
 */
public class C4Server
{
	private ServerSocket serverSocket;
	private int serverPort;

	public C4Server(int serverPort) throws IOException
	{
		this.serverPort = serverPort;
		this.serverSocket = new ServerSocket(serverPort);
	}
	
	/**
	 * Infinite loop that accepts client connections
	 * and creates a new thread for each.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException
	{
		System.out.println("Server Has Started.....");
		System.out.println("Server IP is: " +  InetAddress.getLocalHost().getHostAddress() + " at port: " + serverPort);
		
		while (true)
		{
			try
			{
				Socket client = serverSocket.accept();
				
				// Creates a new server thread.
				C4ServerThread server =  new C4ServerThread(client);
				Thread t =  new Thread(server);
				t.start();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
		//Never Reached
	}
}
