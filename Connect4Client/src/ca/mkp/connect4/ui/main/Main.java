package ca.mkp.connect4.ui.main;
	
import java.io.IOException;

import ca.mkp.connect4.ui.server_connection.ServerConnectionController;
import javafx.application.Application;

import javafx.stage.Stage;

import javafx.scene.Scene;

import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;

/**
 * 
 * @author Michael McMahon
 * @version 1.0
 * 
 * THis is the main application that drives the gui.
 * It allows for the user to connect to a server to play 1 or more games of connect 4
 */
public class Main extends Application 
{
	@Override
	public void start(Stage primaryStage) 
	{
		try 
		{	
			// starts the serverConnection gui which allows the user to enter a ip number.
			FXMLLoader serverConnection =  new FXMLLoader(getClass().getResource("/ca/mkp/connect4/ui/server_connection/C4UIServerConnection.fxml"));
			Pane serverConnectionRoot = (Pane)serverConnection.load();
			ServerConnectionController serverConnectionController =  (ServerConnectionController)serverConnection.getController();
						
			serverConnectionController.setStage(primaryStage);
			Scene serverConnectionScene = new Scene(serverConnectionRoot);			
			serverConnectionScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			primaryStage.setTitle("Connect to a server");
			primaryStage.setResizable(false);
			
			
			primaryStage.setScene(serverConnectionScene);
			primaryStage.show();
			
	
		} 
		catch(IOException e) 
		{
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) 
	{
		launch(args);
	}
	

	
}