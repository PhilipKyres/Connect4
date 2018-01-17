package ca.mkp.connect4.ui.server_connection;



import java.io.IOException;

import javax.swing.JOptionPane;

import ca.mkp.connect4.ui.main.C4UIMainController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * 
 * @author Michael McMahon
 * @version 1.0
 * 
 * 	This is the controller responsible for accepting a server ip number and
 * launching a the main gui.
 */
public class ServerConnectionController 
{


	private String validIp = "";
    @FXML
    private Button buttonServerConnect;
    private Stage stage;

    @FXML
    private TextField serverIPTextField;

    @FXML
    void onServerConnectButtonClick(ActionEvent event) throws IOException 
    {
    	// make sure we have a valid ip, if so start the main ui.
    	String ip =  serverIPTextField.getText().trim();
     	
    	boolean canLaunchMainUI = true;

        	
    	if(!isValidIpAddress(ip))
    	{
    		JOptionPane.showMessageDialog(null, "Server ip is not valid. The ip must be in the following format XXX.XXX.XXX.XXX", "Invalid Server ip", JOptionPane.OK_OPTION);
    		canLaunchMainUI = false;
    	}
    	    	
    	if(canLaunchMainUI)
    	{
    		validIp = ip;
    		stage.close();
    		launchMainUi();

    	}

    }
       
    private boolean isValidIpAddress(String address)
    {    	
        try {
            if ( address == null || address.isEmpty() ) 
            {
                return false;
            }

            String[] ipParts = address.split( "\\." );
            
            if ( ipParts.length != 4 ) 
            {
                return false;
            }

            for ( String i : ipParts ) 
            {
                int part = Integer.parseInt( i );
                
                if ( (part < 0) || (part > 255) )
                {
                    return false;
                }
            }
            return true;

        } 
        catch (NumberFormatException nfe) 
        {
            return false;
        }
    }
        
    public String getIPAddress()
    {
    	return validIp;
    }
       
    public void setStage(Stage stage)
    {
    	this.stage = stage;
    }
    
    private void launchMainUi() throws IOException
    {
		FXMLLoader mainUiLoader =  new FXMLLoader(getClass().getResource("/ca/mkp/connect4/ui/main/C4UIMain.fxml"));
		GridPane root = (GridPane)mainUiLoader.load();
		C4UIMainController controller =  (C4UIMainController)mainUiLoader.getController();
		// pass the ip to the main controller. and display the main gui.
		controller.setIP(validIp);
		controller.Initialize();
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/ca/mkp/connect4/ui/main/application.css").toExternalForm());
		stage.setTitle("Connect Four Game");
		stage.setResizable(false);
		
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() 
		{
			// if the x button is pressed then notify the server that the 
			// client has disconnected and close the client socket.
            public void handle(WindowEvent we) 
            {
            	try 
            	{
					controller.closeClient();
	            	stage.close();
				} 
            	catch (IOException e) 
            	{
            		JOptionPane.showMessageDialog(null, "Something went criticaly wrong when trying to close the program", "CRITICAL ERROR!!!", JOptionPane.OK_OPTION);
				}

            }
        });
		
		stage.setScene(scene);
		stage.show();
    }
}
