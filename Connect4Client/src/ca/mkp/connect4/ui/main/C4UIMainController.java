package ca.mkp.connect4.ui.main;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


import javax.swing.JOptionPane;

import ca.mkp.connect4.common.packet.C4PacketManager;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * 
 * @author Michael McMahon
 * @version 1.0
 * 
 * This is the controller responsible for accepting user input
 * for the main gui.
 */
public class C4UIMainController 
{

    @FXML
    private Label labelScorePlayer;

    @FXML
    private Label labelScoreServer;

    @FXML
    private MenuItem menuItemExit;

    @FXML
    private Label labelPlayerChips;

    @FXML
    private Label labelServerChips;
    
    @FXML
    private GridPane boardGridPane;
    
    
    private final int port = 50000;
    private String ip = "";
    private Socket client;
    private static final int MAX_CHIPS = 21;
    private int playerChips = MAX_CHIPS;
    private int serverChips = MAX_CHIPS;
    private C4PacketManager packetManager;
    private int playerWins = 0;
    private int serverWins = 0;
    private IntegerProperty isPlayingAgain;
	FilteredList<Node> labels;
    
	/**
	 * Initialize the client/sever connection and makes sure that
	 * both of them are ready to play.
	 */
    public void Initialize()
    {
    	try 
    	{
    		// perform handshake agreement between client and server.
			client =  new Socket(ip, port);
			packetManager =  new C4PacketManager(client.getInputStream(), client.getOutputStream());
			packetManager.sendPacket((byte)0, C4PacketManager.GAME_STARTED);
			byte[] packet = packetManager.receivePacket();
			if(packet[0] == C4PacketManager.GAME_STARTED)
			{
				newGame();
			}
			else
			{
	    		JOptionPane.showMessageDialog(null, "Something has gone wrong on the server, please try again later", "SERVER ERROR", JOptionPane.OK_OPTION);
	    		closeClient();
			}
		} 
    	catch (UnknownHostException e) 
    	{
    		JOptionPane.showMessageDialog(null, "Could not connect to the ip: " + ip +  " with port: " + port, "Invalid  Host", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		}
    	catch (ConnectException e) 
    	{
    		JOptionPane.showMessageDialog(null, "Failed to connect to server, please try again", "Connection ERROR", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		}
    	catch (IOException e) 
    	{
    		JOptionPane.showMessageDialog(null, "An connection error has occured, please try again", "IO ERROR", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		}
    }
    
    /**
     *  called when user clicks on one of the red columns.
     * @param event - the mouse event object.
     */
    @FXML
    void onColumnClick(MouseEvent event) 
    {
    	
    	// get the column id and convert it to a byte.
    	Node selected =  event.getPickResult().getIntersectedNode();
    	
    	char column = selected.getId().charAt(1);

    	byte playerCol = Byte.parseByte(String.valueOf(column));
    	
		FilteredList<Node> columnLabels = labels.filtered(p -> p.getId().charAt(0) == column);
		
		int columnSize = columnLabels.size()- 1;
		
		// make sure the column is not filled.
		if(!columnLabels.get(0).getStyleClass().contains("emptyCell"))
		{
    		JOptionPane.showMessageDialog(null, "Could not play move, Column is full of chips ", "Invalid  Move", JOptionPane.OK_OPTION);
    		return;
		}
		
		// when placing a player move add it to the next free 
		// spot in the column.
		for (int i = columnSize; i >= 0; i--) 
		{
			Label l = (Label)columnLabels.get(i);
			if(l.getStyleClass().contains("emptyCell"))
			{
				l.getStyleClass().remove("emptyCell");
	    		l.getStyleClass().add("red");
				break;
			}
		}

		try 
		{
			// send the player move to the server.
			packetManager.sendPacket(playerCol, C4PacketManager.PLAYER_MOVE);
			playPlayerMove();
		}
		catch (IOException e) 
		{
			JOptionPane.showMessageDialog(null, "Could not send move to server", "CONNECTION ERROR!", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		}
    	
		
		// server sends back the player move if it is valid
		// then acknowledge it and tell the server it is its
		// turn to play.
		byte[] received = null;
		try 
		{
			received = packetManager.receivePacket();
			byte serverMsg = received[0];
			byte serverCol =  received[1];
	    	
			if(serverMsg == C4PacketManager.PLAYER_MOVE)
			{
				packetManager.sendPacket((byte)0, C4PacketManager.AI_MOVE);
				received = packetManager.receivePacket();
				serverMsg = received[0];
				serverCol =  received[1];			
			}
			
			updateBoard(serverMsg, serverCol) ;
		} 
		catch (SocketException e) 
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "CONNECTION ERROR!", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), "CONNECTION ERROR!", JOptionPane.OK_OPTION);
    		closeClientLastResort();
		}

    }
    /**
     * user to update the gameboard.
     * 
     * @param msg -  the server message.
     * @param col - the column on the board.
     * @throws IOException
     */
    public void updateBoard(byte msg, byte col) throws IOException
    {
    	// get the column , get the next free node in that coulumn and play the move for the ai.
		FilteredList<Node> chips = labels.filtered(p -> Byte.parseByte(String.valueOf(p.getId().charAt(0))) == col);
		Label l = null;
		for (int i = chips.size() -1; i >= 0; i--) 
		{
			l = (Label) chips.get(i);
			if(l.getStyleClass().contains("emptyCell"))
			{
				l.getStyleClass().remove("emptyCell");
	    		l.getStyleClass().add("black");
				break;
			}
		}
		
    	switch(msg)
    	{
    	case C4PacketManager.AI_MOVE:
    		playAiMove(l);
    		break;
    	case C4PacketManager.GAME_LOST:
    		playAiMove(l);
    		serverWins++;
    		labelScoreServer.setText(Integer.toString(serverWins));
    		launchPlayAgainUi("You have lost the game! Play again?");
    		break;
    	case C4PacketManager.GAME_WON:
    		playerWins++;
    		labelScorePlayer.setText(Integer.toString(playerWins));
    		launchPlayAgainUi("You have won the game! Play again?");
    		break;
    	case C4PacketManager.GAME_TIE:
    		playAiMove(l);
    		launchPlayAgainUi("The game has resulted in a tie! Play again?");
    		break;
    	case C4PacketManager.INVALID_MOVE:
    		l.getStyleClass().remove("red");
    		l.getStyleClass().add("emptyCell");
    		JOptionPane.showMessageDialog(null, "Your move was not played because it was not a valid move, re-plau your turn","INVALID MOVE!", JOptionPane.DEFAULT_OPTION);
    		playerChips++;
    		labelPlayerChips.setText(Integer.toString(playerChips));
    		break;
    	case C4PacketManager.SERVER_LOST_CONNECTION:
    		JOptionPane.showMessageDialog(null, "You have lost connection to the server, Game is closing!","LOST CONNECTION!", JOptionPane.DEFAULT_OPTION);
    		closeClientLastResort();
    		break;    		
    	}
    }
    
    // used for the last moves.
    private void playAiMove(Label l)
    {
		if(l != null)
		{
    		if(l.getStyleClass().contains("emptyCell"))
    		{
        		l.getStyleClass().remove("emptyCell");
    		}
    		l.getStyleClass().add("black");
		}
		serverChips--;
		labelServerChips.setText(Integer.toString(serverChips));
    }
    
    private void playPlayerMove()
    {
		playerChips--;
		labelPlayerChips.setText(Integer.toString(playerChips));
    }
    
    // launches the play again again ui.
    private void launchPlayAgainUi(String message) throws IOException
    {
    	boardGridPane.setMouseTransparent(true);
		FXMLLoader mainUiLoader =  new FXMLLoader(getClass().getResource("/ca/mkp/connect4/ui/main/C4UIPlayAgain.fxml"));
		GridPane root = (GridPane)mainUiLoader.load();
		C4UIPlayAgainController controller =  (C4UIPlayAgainController)mainUiLoader.getController();
		controller.setMessage(message);
		controller.initialize();
		Stage stage =  new Stage();
		controller.setStage(stage);		
		Scene scene = new Scene(root);
		stage.setAlwaysOnTop(true);
		scene.getStylesheets().add(getClass().getResource("/ca/mkp/connect4/ui/main/application.css").toExternalForm());
		stage.setTitle("Play again?");
		stage.setResizable(false);		
		stage.setScene(scene);
		
		isPlayingAgain =  new SimpleIntegerProperty();
		isPlayingAgain.bind(controller.getGameState());
		isPlayingAgain.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) 
			{
				//if we want to play again then send the server a game started message, wait for the ackknowledge
				// reset the game board
				if(newValue.intValue() == C4UIPlayAgainController.STATE_YES)
				{
					try 
					{
						packetManager.sendPacket((byte)0, C4PacketManager.GAME_STARTED);
						byte[] packet = packetManager.receivePacket();
						if(packet[0] == C4PacketManager.GAME_STARTED)
						{
							newGame();
					    	boardGridPane.setMouseTransparent(false);
							stage.close();
						}
					} 
					catch (IOException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// close the program.
				else if(newValue.intValue() == C4UIPlayAgainController.STATE_NO)
				{
					try 
					{
						closeClient();
						stage.close();
					}
					catch (IOException e) 
					{
						closeClientLastResort();
					}
				
				}
			}
		});
		stage.show();
    }
    
    // reset the game board and  player chips 
    private void newGame() throws IOException
    {
    	labels = boardGridPane.getChildren().filtered(p -> p instanceof Label);
    	playerChips = MAX_CHIPS;
    	serverChips = MAX_CHIPS;
		labelPlayerChips.setText(Integer.toString(MAX_CHIPS));
		labelServerChips.setText(Integer.toString(MAX_CHIPS));

		
		for (int i = 0; i < labels.size(); i++) 
		{
			Label label  = (Label)labels.get(i);
			label.setText("\u2B24");
			label.getStyleClass().removeAll("black");
			label.getStyleClass().removeAll("red");
			if(!label.getStyleClass().contains("emptyCell"))
			{
				label.getStyleClass().add("emptyCell");
			}
		}
    }

    @FXML
    void onMenuItemExitClick(ActionEvent event) throws IOException 
    {
    	closeClient();
    }
    
    /**
     * sets the server ip.
     * @param ip -  the ip for the server
     */
    public void setIP(String ip)
    {
    	this.ip = ip;
    }
    
    /**
     * sends a client disconnection packet to the server.
     * and close the client.
     * @throws IOException
     */
    public void closeClient() throws IOException
    {
		packetManager.sendPacket((byte)0, C4PacketManager.CLIENT_DISCONNECTED);
		client.close();
    	Platform.exit();
    }
    
    // last resort close method. its possible
    // that something went wrong so we try to close the program 
    // safely.
    private void closeClientLastResort()
    {
		try 
		{
			if(client != null)
			{
				client.close();
			}
		} 
		catch (IOException e) 
		{
		}
		finally
		{
			Platform.exit();
		}
    }
}
