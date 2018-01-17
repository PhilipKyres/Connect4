package ca.mkp.connect4.ui.main;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
/**
 * 
 * @author Michael McMahon
 * @version 1.0
 * 
 * 	This is the controller responsible for accepting user input
 * for the main play again gui.
 */
public class C4UIPlayAgainController 
{

    @FXML
    private Button buttonYes;

    @FXML
    private Button buttonNo;
    private Stage stage;
    private IntegerProperty playerStage =  new SimpleIntegerProperty(-1);
    public static final int STATE_YES = 1;
    public static final int STATE_NO = 2;
    @FXML
    private Label playAgainMsg;
    private String msg;
    
    public void initialize()
    {
    	playAgainMsg.setText(msg);
    }
    
    @FXML
    void buttonNoClick(ActionEvent event) 
    {
    	playerStage.set(STATE_NO);;
    	stage.close();
    }

    @FXML
    void buttonYesClick(ActionEvent event) 
    {
    	playerStage.set(STATE_YES);

    	stage.close();
    }
    
    public IntegerProperty getGameState()
    {
    	return playerStage;
    }
    
    public void setStage(Stage stage)
    {
    	this.stage = stage;
    }
    
    public void setMessage(String msg)
    {
    	this.msg =  msg;
    }
    

}