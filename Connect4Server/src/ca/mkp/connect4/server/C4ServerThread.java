package ca.mkp.connect4.server;

import java.io.IOException;
import java.net.Socket;

import ca.mkp.connect4.server.session.C4ServerSession;

/**
 * Runnable (threaded) class that creates a C4ServerSession
 * for the new client. 
 * 
 * @author Philip Kyres
 */
public class C4ServerThread implements Runnable 
{
	private Socket client;
	
	public C4ServerThread(Socket client)
	{
		if (client == null)
			throw new IllegalArgumentException("Socket cannot be null");
		
		this.client = client;
	}
	
	/**
	 * Starts the C4ServerSession which contains the game logic.
	 * Prints connection status log messages.
	 */
	@Override
	public void run() 
	{
		String infoMessage = "IP: " + client.getInetAddress().getHostAddress() + " at port: " + client.getPort();
		System.out.println("Client has connected. " + infoMessage);

		try
		{
			C4ServerSession session = new C4ServerSession(client);
			session.play();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}

		try
		{
			client.close();
			System.out.println("Disconnected from client. " + infoMessage);
		} 
		catch (IOException e) 
		{
			System.out.println("Lost connection to client. " + infoMessage);
		}
	}
}
