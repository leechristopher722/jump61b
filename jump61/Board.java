package jump61;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.ArrayDeque;
import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Christopher Lee
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _size = N;
        _board = new ArrayList<Square>();
        for (int i = 0; i < N * N; i++) {
            Square newSq = square(WHITE, 1);
            _board.add(newSq);
        }
        _history = new ArrayList<Board>();
        _history.add(null);
        _moveNum = 0;
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        copy(board0);
        _history.add(board0);
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        _board = new ArrayList<Square>();
        for (int i = 0; i < N * N; i++) {
            Square newSq = square(WHITE, 1);
            _board.add(newSq);
        }
        _history.clear();
        _history.add(null);
        _moveNum = 0;
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        internalCopy(board);
        _history.clear();
        _moveNum = 0;
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    public void internalCopy(Board board) {
        assert size() == board.size();
        for (int i = 0; i < size() * size(); i++) {
            internalSet(i, board.get((i)).getSpots(), board.get(i).getSide());
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return _board.get(n);
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int result = 0;
        for (Square eachSqr : _board) {
            result += eachSqr.getSpots();
        }
        return result;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        if (n < 0 || n >= size() * size() || getWinner() != null) {
            return false;
        }
        return (_board.get(n).getSide() == WHITE
                || _board.get(n).getSide() == player) && isLegal(player);
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return getWinner() == null && player == whoseMove();
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        boolean redWin = true;
        boolean blueWin = true;
        for (Square eachSqr : _board) {
            if (eachSqr.getSide() == RED) {
                blueWin = false;
            } else if (eachSqr.getSide() == BLUE) {
                redWin = false;
            } else {
                return null;
            }
        }
        if (redWin) {
            return RED;
        } else if (blueWin) {
            return BLUE;
        } else {
            return null;
        }
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int result = 0;
        for (Square eachSq : _board) {
            if (eachSq.getSide().equals(side)) {
                result += 1;
            }
        }
        return result;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        set(row(n), col(n), _board.get(n).getSpots() + 1, player);
        if (neighbors(n) < _board.get(n).getSpots() && getWinner() == null) {
            jump(n);
        }
        markUndo();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        int currSize = _board.size();
        _board.remove(n);
        if (num <= 0) {
            _board.add(n, square(WHITE, 1));
        } else {
            _board.add(n, square(player, num));
        }
        assert currSize == _board.size();
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_moveNum == 1 && _history.get(0) == null) {
            internalCopy(new Board(size()));
        } else {
            internalCopy(_history.get(_moveNum - 1));
        }
        _history.remove(_moveNum);
        _moveNum -= 1;
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        _moveNum += 1;
        Board newBoard = new Board(size());
        newBoard.internalCopy(this);
        _history.add(newBoard);
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        if (getWinner() == null) {
            Side player = get(S).getSide();
            int r = row(S);
            int c = col(S);
            internalSet(S, get(S).getSpots() - neighbors(S), player);

            if (r > 1) {
                simpleAdd(player, r - 1, c, 1);
                if (get(r - 1, c).getSpots() > neighbors(r - 1, c)) {
                    jump(sqNum(r - 1, c));
                }
            }
            if (c > 1) {
                simpleAdd(player, r, c - 1, 1);
                if (get(r, c - 1).getSpots() > neighbors(r, c - 1)) {
                    jump(sqNum(r, c - 1));
                }
            }
            if (r < size()) {
                simpleAdd(player, r + 1, c, 1);
                if (get(r + 1, c).getSpots() > neighbors(r + 1, c)) {
                    jump(sqNum(r + 1, c));
                }
            }
            if (c < size()) {
                simpleAdd(player, r, c + 1, 1);
                if (get(r, c + 1).getSpots() > neighbors(r, c + 1)) {
                    jump(sqNum(r, c + 1));
                }
            }
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===");
        for (int i = 1; i <= _board.size(); i++) {
            Square currSqr = _board.get(i - 1);
            if (i % size() == 1) {
                out.format("\n%s", "    ");
            }
            if (currSqr.getSide() == RED) {
                out.format("%d%s ", currSqr.getSpots(), "r");
            } else if (currSqr.getSide() == BLUE) {
                out.format("%d%s ", currSqr.getSpots(), "b");
            } else {
                out.format("%d%s ", currSqr.getSpots(), "-");
            }
        }
        out.format("\n===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            for (int i = 0; i < this.size() * this.size(); i++) {
                if (i >= B.size() * B.size()) {
                    return false;
                }
                if (this.get(i).getSide() != B.get(i).getSide()
                        || this.get(i).getSpots() != B.get(i).getSpots()) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** Represents the board of the game. */
    private ArrayList<Square> _board;

    /** Size of a row or column. */
    private int _size;

    /** History of the board so far. */
    private ArrayList<Board> _history;

    /** Number of moves done by players. */
    private int _moveNum;
}
