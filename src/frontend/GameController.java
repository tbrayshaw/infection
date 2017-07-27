package frontend;

import ai.AI;
import javafx.animation.FillTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Move;
import model.State;

public class GameController {


    String[] aiName;          // Names of AI players on red and blue sides
    boolean squareSelected;   // Has the user made the first mouse click to choose a square?
    boolean gameOver;         // Has the game ended?
    boolean aiThinking;       // Is the AI thinking?
    int runtransitions;       // Number of animation transitions running
    State gameState;          // Current game state
    Move lastMove;            // Last move made
    AI[] ai;                  // References to AIs
    int[] score;              // Player's scores

    Rectangle marker;         // Reference to widget for piece selection cursor

    Circle[][] pieceForSpace; // Reference to widgets for placed pieces on board

    int fromx, fromy;         // Location of square selected by mouse

    Thread aiThread;          // Thread handle to run the AI in a separate thread to prevent it freezing the window

    // Standard strings
    final String FIRST_CLICK = "Click a piece to move.";
    final String BAD_FIRST_CLICK = "Move your own pieces.";
    final String SECOND_CLICK = "Click a square to move it to.";
    final String BAD_SECOND_CLICK = "Move to an empty square.";
    final String BAD_MOVE = "Illegal move. Try again.";

    // References to widgets in FXML file
    public Label p1scorelabel;
    public Label p2scorelabel;
    public Pane board;
    public Label p2namelabel;
    public Label p1namelabel;
    public Label instructions;


    // Invoked from FXML when board pane is clicked: calculates which square was clicked and passes it on
    public void boardClicked(MouseEvent event) {
        squareClicked((int)(event.getX()/40),(int)(event.getY()/40));
        event.consume();
    }


    void updateBoard() {
        runtransitions = 0;
        // Scan each board position
        for (int y=0; y<10; y++) {
            for (int x=0; x<10; x++) {
                int value = gameState.pieceAt(x,y);
                Color color;
                if (value == 1) color=Color.RED; else color=Color.BLUE;
                // If there is a piece here in the state, and there isn't one on screen, and there was a last move..
                if (value != 0) if (pieceForSpace[x][y] == null) if (lastMove != null) {
                    int oldx = lastMove.fromx;
                    int oldy = lastMove.fromy;
                    Circle piece;
                    // If there is no longer a piece on the square moved from, this is a move, so reuse that piece sprite.
                    if (gameState.pieceAt(lastMove.fromx, lastMove.fromy) == 0) {
                        piece = pieceForSpace[oldx][oldy];
                        pieceForSpace[oldx][oldy] = null;
                    } else {
                        // If there is still a piece on the square moved from, this is a replicate, so make a new sprite.
                        piece = new Circle((oldx * 40) + 20, (oldy * 40) + 20, 15, color);
                        board.getChildren().add(piece);
                    }
                    // Set up animation from the old location to the new location.
                    TranslateTransition tt = new TranslateTransition(Duration.millis(250), piece);
                    tt.setByX(((x * 40) + 20) - ((oldx * 40) + 20));
                    tt.setByY(((y * 40) + 20) - ((oldy * 40) + 20));
                    // Set up to call afterUpdate() after all animation threads finished.
                    tt.setOnFinished(e -> {
                        runtransitions--;
                        if (runtransitions <= 0) afterUpdate();
                    });
                    runtransitions++;
                    // Start animation thread.
                    tt.play();
                    pieceForSpace[x][y] = piece;
                } else {
                    // No last move - we are in initial setup.
                    // Create new sprite.
                    Circle newPiece = new Circle((x * 40) + 20, (y * 40) + 20, 15, color);
                    board.getChildren().add(newPiece);
                    pieceForSpace[x][y] = newPiece;
                }
                // Else, if there is a piece here in the state that's the opposite colour to the one on screen..
                else if (pieceForSpace[x][y].getFill() != color) {
                    // Create a color transition animation.
                    FillTransition ft = new FillTransition(Duration.millis(250), pieceForSpace[x][y], (Color) pieceForSpace[x][y].getFill(), color);
                    ft.setOnFinished(e -> {
                        runtransitions--;
                        if (runtransitions <= 0) afterUpdate();
                    });
                    runtransitions++;
                    ft.play();
                }
            }
        }
        // If no animations were started, run afterUpdate() directly since no animation thread exists to run it.
        if (runtransitions == 0) afterUpdate();
    }


    // Called after a move has been made and all board animations have been completed.
    void afterUpdate() {
        // If game was ended, report win.
        if (gameOver) {
            if (score[1] > score[0]) {
                instructions.setText(aiName[1] + " wins!");
            } else {
                if (score[1] == score[0]) {
                    instructions.setText("Draw!");
                } else {
                    instructions.setText(aiName[0] + " wins!");
                }
            }
        } else {
            // Get whose turn is next
            AI activeAI = ai[gameState.whoseTurn() - 1];
            if (activeAI == null) { // Human player, set to instructions then let dispatcher run
                squareSelected = false;
                instructions.setText(FIRST_CLICK);
            } else { // AI player, start thinking thread
                instructions.setText(aiName[gameState.whoseTurn() - 1] + "..");
                aiThinking = true;
                aiThread = new Thread(() -> { Move x = activeAI.nextMove(gameState); aiMoveDecided(x); });
                aiThread.run();
            }
        }
    }

    // Called from AI manager thread when AI has finished calculating its move
    void aiMoveDecided(Move nextMove) {
        aiThinking = false;
        assert gameState.moveIsValid(nextMove) : "AI tried to make invalid move" + nextMove;
        lastMove = nextMove;
        gameState = gameState.afterMove(lastMove);
        newTurn();
    }

    // Called immediately after a move is made, before screen is updated
    void newTurn() {

        score[0] = gameState.countPieces(1);
        score[1] = gameState.countPieces(2);

        if (gameState.validMoves().size() == 0) {
            gameOver = true;
            score[gameState.whoseNotTurn()-1] += gameState.countPieces(0);
        }

        p1scorelabel.setText(Integer.toString(score[0]));
        p2scorelabel.setText(Integer.toString(score[1]));
        updateBoard();
    }

    // Called when the user clicks on a square
    void squareClicked(int x, int y) {
        // If it's the AI's turn, do nothing
        if (aiThinking) return;
        // If the user hasn't selected a "from" square yet
        if (!squareSelected) {
            if (State.inBounds(x,y)) {
                // If they clicked on a square with their own piece, highlight it with the marker
                if (gameState.pieceAt(x,y) == gameState.whoseTurn()) {
                    fromx = x; fromy = y;
                    squareSelected = true;
                    marker.setVisible(true);

                    marker.setX(x * 40);
                    marker.setY(y*40);
                    instructions.setText(SECOND_CLICK);
                } else {
                    // Otherwise show error message
                    instructions.setText(BAD_FIRST_CLICK);
                }
            }
        } else { // If the user has selected a square before
            if (State.inBounds(x,y)) {
                if (gameState.pieceAt(x,y) == 0) {
                    // If they are clicking on an empty square, check if move is valid
                    Move sugMove = new Move(fromx,fromy,x,y);
                    if (gameState.moveIsValid(sugMove)) {
                        // If valid, apply it
                        lastMove = new Move(fromx,fromy,x,y);
                        gameState = gameState.afterMove(lastMove);
                        marker.setVisible(false);
                        newTurn();
                    } else {
                        // Give invalid move error
                        instructions.setText(BAD_MOVE);
                    }
                } else {
                    // They are clicking on a non-empty square
                    // If it's the previously selected "from" square assume they want to cancel selection
                    if ((x == fromx) && (y == fromy)) {
                        marker.setVisible(false);
                        squareSelected = false;
                        instructions.setText(FIRST_CLICK);
                    } else {
                        // Give empty square error
                        instructions.setText(BAD_SECOND_CLICK);
                    }

                }
            }
        }
    }

    public void setAIs(AI p1, String p1n, AI p2, String p2n) {
        ai = new AI[2];
        ai[0] = p1;
        ai[1] = p2;
        aiName = new String[2];
        aiName[0] = p1n;
        aiName[1] = p2n;
        p1namelabel.setText(aiName[0] + " score:");
        p2namelabel.setText(aiName[1] + " score:");
        score = new int[2];
        score[0] = 0;
        score[1] = 0;
        aiThinking = false;
        newTurn();

    }

    @SuppressWarnings("unused")
    public void initialize() {
        board.setMouseTransparent(false);
        for(int x=0; x<11; x++) {
            Line line = new Line(x*40,0,x*40,400);
            board.getChildren().add(line);
            line = new Line(0,x*40,400,x*40);
            board.getChildren().add(line);

        }

        marker = new Rectangle(40,40);
        marker.setFill(null);
        marker.setStroke(Color.RED);
        marker.setStrokeWidth(3.0);
        marker.setVisible(false);
        board.getChildren().add(marker);

        gameState = new State();
        gameState.setBoard(0,0,1);
        gameState.setBoard(0,9,2);
        gameState.setBoard(9, 0, 2);
        gameState.setBoard(9,9,1);
        gameOver = false;
        pieceForSpace = new Circle[10][10];
    }
}
