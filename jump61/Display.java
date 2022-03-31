package jump61;

import ucb.gui2.TopLevel;
import ucb.gui2.LayoutSpec;

import java.util.concurrent.ArrayBlockingQueue;

import static jump61.Side.*;

/** The GUI controller for jump61.  To require minimal change to textual
 *  interface, we adopt the strategy of converting GUI input (mouse clicks)
 *  into textual commands that are sent to the Game object through a
 *  a Writer.  The Game object need never know where its input is coming from.
 *  A Display is an Observer of Games and Boards so that it is notified when
 *  either changes.
 *  @author Christopher Lee
 */
class Display extends TopLevel implements View, CommandSource, Reporter {

    /** A new window with given TITLE displaying GAME, and using COMMANDWRITER
     *  to send commands to the current game. */
    Display(String title) {
        super(title, true);

        addMenuButton("Game->Quit", this::quit);
        addMenuButton("Game->New Game", this::newGame);

        addSeparator("Game");
        addMenuButton("Game->Set size 2", this::setSize2);
        addMenuButton("Game->Set size 3", this::setSize3);
        addMenuButton("Game->Set size 4", this::setSize4);
        addMenuButton("Game->Set size 5", this::setSize5);
        addMenuButton("Game->Set size 6", this::setSize6);
        addMenuButton("Game->Set size 7", this::setSize7);
        addMenuButton("Game->Set size 8", this::setSize8);
        addMenuButton("Game->Set size 9", this::setSize9);
        addMenuButton("Game->Set size 10", this::setSize10);

        addMenuButton("Player->Red to AI", this::setRedAI);
        addMenuButton("Player->Red to Manual", this::setRedManual);
        addMenuButton("Player->Blue to AI", this::setBlueAI);
        addMenuButton("Player->Blue to manual", this::setBlueManual);

        _boardWidget = new BoardWidget(_commandQueue);
        add(_boardWidget, new LayoutSpec("y", 1, "width", 2));
        display(true);
    }

    /** Response to "Quit" button click. */
    void quit(String dummy) {
        System.exit(0);
    }

    /** Response to "New Game" button click. */
    void newGame(String dummy) {
        _commandQueue.offer("new");
    }

    /** Response to "size 3". */
    void setSize2(String dummy) {
        _commandQueue.offer("size 2");
    }

    /** Response to "size 3". */
    void setSize3(String dummy) {
        _commandQueue.offer("size 3");
    }

    /** Response to "size 4". */
    void setSize4(String dummy) {
        _commandQueue.offer("size 4");
    }

    /** Response to "size 5". */
    void setSize5(String dummy) {
        _commandQueue.offer("size 5");
    }

    /** Response to "size 6". */
    void setSize6(String dummy) {
        _commandQueue.offer("size 6");
    }

    /** Response to "size 7". */
    void setSize7(String dummy) {
        _commandQueue.offer("size 7");
    }

    /** Response to "size 8". */
    void setSize8(String dummy) {
        _commandQueue.offer("size 8");
    }

    /** Response to "size 9". */
    void setSize9(String dummy) {
        _commandQueue.offer("size 9");
    }

    /** Response to "size 10". */
    void setSize10(String dummy) {
        _commandQueue.offer("size 10");
    }

    /** Switch RED to AI. */
    void setRedAI(String dummy) {
        _commandQueue.offer("auto red");
    }

    /** Switch RED to manual. */
    void setRedManual(String dummy) {
        _commandQueue.offer("manual red");
    }

    /** Switch BLUE to AI. */
    void setBlueAI(String dummy) {
        _commandQueue.offer("auto blue");
    }

    /** Switch Blue to manual. */
    void setBlueManual(String dummy) {
        _commandQueue.offer("manual blue");
    }

    @Override
    public void update(Board board) {

        _boardWidget.update(board);
        pack();
        _boardWidget.repaint();
    }

    @Override
    public String getCommand(String ignored) {
        try {
            return _commandQueue.take();
        } catch (InterruptedException excp) {
            throw new Error("unexpected interrupt");
        }
    }

    @Override
    public void announceWin(Side side) {
        showMessage(String.format("%s wins!", side.toCapitalizedString()),
                    "Game Over", "information");
    }

    @Override
    public void announceMove(int row, int col) {
    }

    @Override
    public void msg(String format, Object... args) {
        showMessage(String.format(format, args), "", "information");
    }

    @Override
    public void err(String format, Object... args) {
        showMessage(String.format(format, args), "Error", "error");
    }

    /** Time interval in msec to wait after a board update. */
    static final long BOARD_UPDATE_INTERVAL = 50;

    /** The widget that displays the actual playing board. */
    private BoardWidget _boardWidget;
    /** Queue for commands going to the controlling Game. */
    private final ArrayBlockingQueue<String> _commandQueue =
        new ArrayBlockingQueue<>(5);
}
