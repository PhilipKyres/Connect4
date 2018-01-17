package ca.mkp.connect4.server.session;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import ca.mkp.connect4.common.packet.C4PacketManager;

/**
 * This Class is responsible for the session between a user and the server. The
 * play() method allows the user to play until he closes the connection
 * 
 * @author Mark Parenteau
 *
 */
public class C4ServerSession{
	private C4PacketManager serverPacket;
	private final int TOTAL_TOKENS = 21;
	private int playerTokens;
	private int serverTokens;
	private int[][] board;
	private final int PLAYER_IDENTIFIER = 1;
	private final int SERVER_IDENTIFIER = 2;
	private final int MAX_ROW = 9;
	private final int MIN_ROW = 3;
	private final int MAX_COLUMN = 10;
	private final int MIN_COLUMN = 3;
	private final int COL_OFFSET = 3;

	/**
	 * Constructor
	 * 
	 * @param socket
	 * @throws IOException
	 */
	public C4ServerSession(Socket client) throws IOException {
		playerTokens = TOTAL_TOKENS;
		serverTokens = TOTAL_TOKENS;
		serverPacket = new C4PacketManager(client.getInputStream(), client.getOutputStream());
		board = new int[12][13];
	}

	/**
	 * This method is responsible for handling the game session until the user
	 * closes it
	 * 
	 * @throws IOException
	 * @throws SocketException
	 *             **
	 */
	public void play() throws SocketException, IOException {
		int msgType = -1;
		byte[] packet = null;
		boolean playAgain = true;
		boolean gameOver = true;

		// playAgain is true until the user closes the connection
		while (playAgain) {
			packet = serverPacket.receivePacket();
			msgType = packet[0];

			resetBoard();

			switch (msgType) {
			case C4PacketManager.GAME_STARTED:
				setUpGame();
				gameOver = false;
				serverPacket.sendPacket((byte) 0, C4PacketManager.GAME_STARTED);
				System.out.println("Game started");
				break;

			case C4PacketManager.CLIENT_DISCONNECTED:
				gameOver = true;
				playAgain = false;
				break;

			}

			while (!gameOver) {
				packet = serverPacket.receivePacket();
				msgType = packet[0];

				switch (msgType) {
				case C4PacketManager.PLAYER_MOVE:
					// because of the 3 cells used as left padding, we add 3 to
					// the column received
					int column = packet[1] + COL_OFFSET;

					if (validateMove(column)) {
						// place the player's move
						int move = playTurn(column, true);

						// if player wins on this move
						if (checkForWin(move)) {
							serverPacket.sendPacket((byte) move, C4PacketManager.GAME_WON);
							System.out.println("AI lost game");
							resetBoard();
							setUpGame();
							gameOver = true;
						} else
							serverPacket.sendPacket((byte) (move-3), C4PacketManager.PLAYER_MOVE);
					} else // invalid move
						serverPacket.sendPacket((byte) 0, C4PacketManager.INVALID_MOVE);
					break;
				case C4PacketManager.AI_MOVE:
					//using - COL_OFFSET here to prevent modifiying the variable
					//3 times when sending an answer back to the client
					int move = playTurn(findBestMove(), false) - COL_OFFSET;

					// if Server wins on this move
					if (checkForWin(move +COL_OFFSET)) {
						serverPacket.sendPacket((byte) (move), C4PacketManager.GAME_LOST);
						System.out.println("AI won game");
						resetBoard();
						setUpGame();
						gameOver = true;
					} else if (playerTokens == 0 && serverTokens == 0) {
						serverPacket.sendPacket((byte) move, C4PacketManager.GAME_TIE);
						System.out.println("Game tied");
						gameOver = true;
					} else {
						serverPacket.sendPacket((byte) move, C4PacketManager.AI_MOVE);
					}
					break;
				case C4PacketManager.CLIENT_DISCONNECTED:
					gameOver = true;
					playAgain = false;
					break;
				}
			}
		}
	}

	/**
	 * Initializes important variables when a game starts
	 */
	public void setUpGame() {
		playerTokens = TOTAL_TOKENS;
		serverTokens = TOTAL_TOKENS;
	}

	/**
	 * This method is responsible for checking for a possible win.
	 */
	private boolean checkForWin(int column) {
		boolean won = false;
		boolean found = false;
		int currentX = MIN_ROW;
		int id = 0;
		// Cycles through the rows to find the row where the token has just been
		// played
		for (int x = MIN_ROW; x < MAX_ROW && !found; x++) {
			// at this point it is impossible that the column is empty because a
			// move has just been played in there
			if (board[x][column] != 0) {
				currentX = x;
				found = true;
			}
		}
		// Sets the identifier to the current token's
		if (board[currentX][column] == PLAYER_IDENTIFIER) {
			id = PLAYER_IDENTIFIER;
		} else {
			id = SERVER_IDENTIFIER;
		}
		if (checkWinSides(currentX, column, id)) {
			won = true;
		} else if (checkWinLower(currentX, column, id)) {
			won = true;
		} else if (checkWinDiagonals(currentX, column, id)) {
			won = true;
		}

		return won;
	}

	/**
	 * This method is called between every game to ensure that the board is
	 * properly reset
	 */
	public void resetBoard() {
		for (int j = 0; j < board.length; j++) {
			for (int i = 0; i < board[0].length; i++) {
				board[j][i] = 0;
			}
		}
	}

	/**
	 * Validates a player moves to ensure that a move is not played beyond
	 * boundaries
	 * 
	 * @param column
	 * @return true if the move is valid, false otherwise
	 */
	private boolean validateMove(int column) {
		// if the uppermost row of the column is empty, it is a valid move
		if (column > 2 && column < 10 && board[MIN_ROW][column] == 0)
			return true;
		else
			return false;
	}

	/**
	 * This method will return an amount of points based on what is located to
	 * the sides of the current position
	 * 
	 * @param x
	 *            the row value on the board
	 * @param y
	 *            the column value on the board
	 * @return the amount of points for that move
	 */
	private int checkSides(int x, int y) {
		int pts = 0;
		int tokenLeft = 0;
		int playerTokenLeft = 0;
		int tokenRight = 0;
		int playerTokenRight = 0;
		// Goes through the 3 elements to the left of the current case
		for (int j = y - 3; j < y; j++) {
			// if the token is a server token
			if (board[x][j] == SERVER_IDENTIFIER) {
				playerTokenLeft = 0;
				tokenLeft++;
			}
			// if the token is a player token or no token at all(might consider
			// adding a bit here for no token)
			else {
				tokenLeft = 0;
				if (board[x][j] == PLAYER_IDENTIFIER) {
					playerTokenLeft++;
				}
			}
		}
		// Goes through the 3 elements to the right of the current case
		for (int j = y + 1; j <= y + 3; j++) {
			// token is a server token
			if (board[x][j] == SERVER_IDENTIFIER) {
				tokenRight++;
			}
			// token is player token or no token
			else {
				tokenRight = 0;
				if (board[x][j] == PLAYER_IDENTIFIER) {
					playerTokenRight++;
				}
			}
		}
		int playerTokenAmount = playerTokenLeft + playerTokenRight;
		int tokenAmount = tokenLeft + tokenRight;
		// if block is higher priority than score on this move
		if (playerTokenAmount > tokenAmount){
			tokenAmount = playerTokenAmount;
			//Give a higher priority to blocking, since you play second it prevents a few cases of free win
			pts += 10;
		}
		switch (tokenAmount) {
		case 0:// no "usable" token in the line
			pts += 10;
			break;
		case 1:// 1 in position
			pts += 20;
			break;
		case 2:// 2 in line
			pts += 30;
			break;
		default:// Win is possible
			pts += 500;
			break;
		}
		return pts;
	}

	/**
	 * This method will return an amount of points based on what is located in
	 * diagonal of the current position
	 * 
	 * @param x
	 *            the row value on the board
	 * @param y
	 *            the column value on the board
	 * @return the amount of points for that move
	 */
	private int checkDiagonals(int x, int y) {
		int pts = 0;
		int tokenBLeft = 0;
		int tokenBRight = 0;
		int tokenULeft = 0;
		int tokenURight = 0;
		int playerTokenBLeft = 0;
		int playerTokenBRight = 0;
		int playerTokenULeft = 0;
		int playerTokenURight = 0;
		// Goes through the 3 elements to the bottom left of the current case
		// j is the starting column, z is the starting row
		for (int j = y - 3, z = x + 3; j < y; j++, z--) {
			// if the token is a server token
			if (board[z][j] == SERVER_IDENTIFIER) {
				playerTokenBLeft = 0;
				tokenBLeft++;
			}
			// if the token is a player token or no token at all(might consider
			// adding a bit here for no token)
			else {
				tokenBLeft = 0;
				if (board[z][j] == PLAYER_IDENTIFIER) {
					playerTokenBLeft++;
				}
			}
		}
		// Goes through the 3 elements to the upper right of the current case
		for (int j = y + 1, z = x - 1; j <= y + 3; j++, z--) {
			// token is a server token
			if (board[z][j] == SERVER_IDENTIFIER) {
				playerTokenURight = 0;
				tokenURight++;
			}
			// token is player token or no token
			else {
				tokenURight = 0;
				if (board[z][j] == PLAYER_IDENTIFIER) {
					playerTokenURight++;
				}
			}
		}
		int playerTokenAmount = playerTokenBLeft + playerTokenURight;
		int tokenAmount = tokenBLeft + tokenURight;
		// if block is higher priority than score on this move
		if (playerTokenAmount > tokenAmount)
			tokenAmount = playerTokenAmount;
		switch (tokenAmount) {
		case 0:// no "usable" token in the line
			pts += 10;
			break;
		case 1:// 1 in position
			pts += 20;
			break;
		case 2:// 2 in line
			pts += 30;
			break;
		default:// Win is possible
			pts += 500;
			break;
		}
		// Now checking for the upperleft to lower right diagonal
		// Goes through the 3 elements to the upper left of the current case
		// j is the starting column, z is the starting row
		for (int j = y - 3, z = x - 3; j < y; j++, z++) {
			// if the token is a server token
			if (board[z][j] == SERVER_IDENTIFIER) {
				playerTokenULeft = 0;
				tokenULeft++;
			}
			// if the token is a player token or no token at all(might consider
			// adding a few pts here for no token)
			else {
				tokenULeft = 0;
				if (board[z][j] == PLAYER_IDENTIFIER) {
					playerTokenULeft++;
				}
			}
		}
		// Goes through the 3 elements to the bottom right of the current case
		for (int j = y + 1, z = x + 1; j <= y + 3; j++, z++) {
			// token is a server token
			if (board[z][j] == SERVER_IDENTIFIER) {
				playerTokenBRight = 0;
				tokenBRight++;
			}
			// token is player token or no token
			else {
				tokenBRight = 0;
				if (board[z][j] == PLAYER_IDENTIFIER) {
					playerTokenBRight++;
				}
			}
		}
		// stores the amount of token lined up with the current case
		tokenAmount = tokenULeft + tokenBRight;
		playerTokenAmount = playerTokenULeft + playerTokenBRight;
		// if player block has higher priority than score
		if (playerTokenAmount > tokenAmount)
			tokenAmount = playerTokenAmount;

		switch (tokenAmount) {
		case 0:// no "usable" token in the line
			pts += 10;
			break;
		case 1:// 1 in position
			pts += 20;
			break;
		case 2:// 2 in line
			pts += 30;
			break;
		default:// Win is possible
			pts += 500;
			break;
		}
		return pts;
	}

	

	/**
	 * This method will return an amount of points based on what is located
	 * under the current position
	 * 
	 * @param x
	 *            the row value on the board
	 * @param y
	 *            the column value on the board
	 * @return the amount of points for that move
	 */
	private int checkLower(int x, int y) {
		int pts = 0;
		int blockPts = 0;
		if (x != MAX_ROW) {
			while (x < MAX_ROW) {
				if (board[x][y] == SERVER_IDENTIFIER) {
					blockPts = 0;
					pts += 10;
					// 3 tokens, win is possible
					if (pts == 30) {
						pts = 500;
						break;
					}
				}
				// found a player token
				else if (board[x][y] == PLAYER_IDENTIFIER) {
					pts = 0;
					blockPts += 10;

					// if 3 player token, high blocking priority to prevent
					// the win
					if (blockPts == 30) {
						blockPts = 500;
						break;
					}
				}
				else{
					pts = 0;
					blockPts = 0;
				}
				x++;
			}
		}
		// if block points are higher than score points
		if (blockPts > pts)
			pts = blockPts;
		return pts;
	}

	/**
	 * Checks if move won within its column
	 * 
	 * @param x
	 *            the row value
	 * @param y
	 *            the column value
	 * @param id
	 *            the player/computer identifier
	 * @return true if the game is won, false if not
	 */
	private boolean checkWinLower(int x, int y, int id) {
		boolean won = false;
		int tokens = 0;

		while (x < MAX_ROW && !won) {
			if (board[x][y] == id) {
				tokens++;
			} else
				tokens = 0;
			if (tokens == 4) {
				won = true;
			}
			x++;
		}

		return won;
	}

	/**
	 * Checks if move won within its row
	 * 
	 * @param x
	 *            the row value
	 * @param y
	 *            the column value
	 * @param id
	 *            the player/computer identifier
	 * @return true if the game is won, false if not
	 */
	private boolean checkWinSides(int x, int y, int id) {
		boolean won = false;
		int tokens = 0;
		// Starts 3 column to the left, goes up to three column to the right
		for (int i = y - 3; i <= y + 3 && !won; i++) {

			if (board[x][i] == id) {
				tokens++;
			} else {

				tokens = 0;
			}

			if (tokens == 4) {
				won = true;
			}
		}
		return won;
	}

	/**
	 * Checks if move won within its diagonals
	 * 
	 * @param x
	 *            the row value
	 * @param y
	 *            the column value
	 * @param id
	 *            the player/computer identifier
	 * @return true if the game is won, false if not
	 */
	private boolean checkWinDiagonals(int x, int y, int id) {
		boolean won = false;
		int tokens = 0;
		for (int row = x - 3, column = y - 3; row <= x + 3 && !won; row++, column++) {
			if (board[row][column] == id) {
				tokens++;
			} else {
				tokens = 0;
			}
			if (tokens == 4) {
				won = true;
			}

		}
		// resets the token to prevent any issue with the first diagonal
		tokens = 0;
		for (int row = x + 3, column = y - 3; row >= x - 3 && !won; row--, column++) {
			if (board[row][column] == id) {
				tokens++;
			} else {
				tokens = 0;
			}
			if (tokens == 4) {
				won = true;
			}
		}

		return won;
	}

	/**
	 * This method will return the best column for the computer to play
	 * 
	 * @return the column where to play the move
	 */
	private int findBestMove() {
		ArrayList<Integer> columnList = new ArrayList<Integer>();
		int pts;
		// Cycles through the possible moves and assigns points to each
		for (int y = MIN_COLUMN; y < MAX_COLUMN; y++) {
			boolean ptsSet = false;
			pts = 0;
			// go through the rows until you can set the points for that move
			for (int x = MIN_ROW; x < MAX_ROW && !ptsSet; x++) {
				// This check can only be true for the uppermost case on the
				// board, it means the column is already full
				if (board[x][y] != 0) {
					pts = -1;
					break;
				}
				// If we are on the lowermost case
				else if (x == MAX_ROW - 1) {
					pts += checkSides(x, y);
					pts += checkDiagonals(x, y);
					ptsSet = true;
				}
				// if the postion under the current one already has a "token",
				// do the checks
				else if (board[x + 1][y] != 0) {
					pts += checkLower(x, y);
					pts += checkSides(x, y);
					pts += checkDiagonals(x, y);
					ptsSet = true;
				}
			}
			columnList.add(pts);
		}
		// Finds the highest points value and stores all possible plays
		ArrayList<Integer> possibleMoves = new ArrayList<Integer>();
		int max = Collections.max(columnList);
		for (int i = 0; i < columnList.size(); i++) {
			if (columnList.get(i) == max) {
				possibleMoves.add(i);
			}
		}
		//Chooses a random move from the possible choices
		Random randomGenerator = new Random();
		int move = randomGenerator.nextInt(possibleMoves.size());
		return possibleMoves.get(move) + 3;
	}

	/**
	 * 
	 * @param y
	 *            the column where the move is to be played
	 * @param isServer
	 *            true if it is the server turn
	 * @return the column where the move was played
	 */
	private int playTurn(int y, boolean isPlayer) {
		int id = 0;
		boolean varSet = false;
		if (isPlayer) {
			id = PLAYER_IDENTIFIER;
			playerTokens--;
		} else {
			id = SERVER_IDENTIFIER;
			serverTokens--;
		}
		// This loop is reponsible for finding in which row the token will go
		for (int x = MIN_ROW; x < MAX_ROW && !varSet; x++) {
			if (x == MAX_ROW - 1) {
				board[x][y] = id;
				varSet = true;
			} else if (board[x + 1][y] != 0) {
				board[x][y] = id;
				varSet = true;
			}
		}
		return y;
	}

}
