package ca.mkp.connect4.server.application;

import java.io.IOException;

import ca.mkp.connect4.server.C4Server;

/**
 * Server application with main method
 * 
 * @author Philip Kyres
 */
public class C4ServerApp
{
	public static void main(String[] args) 
	{
		try
		{
			// Hard-coded port number
			C4Server server = new C4Server(50000);
			server.run();
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
}
