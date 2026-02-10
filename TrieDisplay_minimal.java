import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Minimal Trie sanity-check UI.
 *
 * Uses ONLY these Trie methods:
 *   - insert(String word)
 *   - contains(String word)                 // not required by the UI, but part of the required API
 *   - mostLikelyNextChar(String prefix)
 *   - mostLikelyNextWord(String prefix)
 *
 * How to run:
 *   1) Make sure Trie.java is on the classpath and implements the methods above.
 *   2) (Optional) Provide a training text file as args[0]. The file should contain whitespace-separated words.
 *      Example: java TrieDisplay_minimal words.txt
 *
 * Controls:
 *   - Type letters to build a prefix (the current token after the last space).
 *   - SPACE adds a space to the text (starts a new word).
 *   - BACKSPACE deletes.
 */
public class TrieDisplay_minimal extends JPanel implements KeyListener {

    private final Trie trie;

    // Full text the user has typed so far
    private final StringBuilder typed = new StringBuilder();

    // Cached display values (recomputed on each key press)
    private String currentPrefix = "";
    private char nextChar = '_';
    private String nextWord = "";

    public TrieDisplay_minimal(Trie trie) {
        this.trie = trie;

        setPreferredSize(new Dimension(900, 500));
        setBackground(Color.WHITE);

        setFocusable(true);
        addKeyListener(this);

        updatePredictions();
    }

    /** Recomputes currentPrefix, nextChar, and nextWord from the current typed text. */
    private void updatePredictions() {
        currentPrefix = getCurrentPrefix(typed.toString());

        if (currentPrefix.isEmpty()) {
            nextChar = '_';
            nextWord = "";
        } else {
            nextChar = trie.mostLikelyNextChar(currentPrefix);
            nextWord = trie.mostLikelyNextWord(currentPrefix);
        }
        repaint();
    }

    /** Returns the current "token" after the last space (letters only, lowercased). */
    private static String getCurrentPrefix(String text) {
        int lastSpace = text.lastIndexOf(' ');
        String token = (lastSpace >= 0) ? text.substring(lastSpace + 1) : text;

        // Keep it simple: letters only
        token = token.replaceAll("[^A-Za-z]", "").toLowerCase();
        return token;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

		// NOTE: a lot of these hardcoded values work relative to one another
		//       change with caution and ensure they work together
        g.setFont(new Font("Consolas", Font.PLAIN, 18));

        int x = 30;
        int y = 40;

        g.setColor(Color.BLACK);
        g.drawString("Typed text:", x, y);
        y += 26;

        // Draw the typed text (very simple wrap)
        String[] lines = wrap(typed.toString(), 80);
        for (String line : lines) {
            g.drawString(line, x, y);
            y += 22;
        }

        y += 18;
        g.drawLine(x, y, x + 820, y);
        y += 30;

        g.setColor(Color.DARK_GRAY);
        g.drawString("Current prefix: " + (currentPrefix.isEmpty() ? "(none)" : currentPrefix), x, y);
        y += 28;

        g.drawString("Most likely next char: " + nextChar, x, y);
        y += 28;

        g.drawString("Most likely next word: " + (nextWord.isEmpty() ? "(none)" : nextWord), x, y);
        y += 28;

        g.setColor(Color.GRAY);
        g.drawString("Tip: type a prefix (letters). Backspace deletes. Space starts a new word.", x, y);
    }

    /** Very small, dumb word wrap for display only. */
    private static String[] wrap(String s, int maxChars) {
        if (s.length() <= maxChars) return new String[]{s};

        ArrayList<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(i + maxChars, s.length());
            out.add(s.substring(i, end));
            i = end;
        }
        return out.toArray(new String[0]);
    }

    // ---- KeyListener --------------------------------------------------------

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();

        if (c == KeyEvent.CHAR_UNDEFINED) return;

        // Backspace is handled in keyPressed (more reliable)
        if (c == '\b') return;

        // Allow space OR treat enter / return like a space
        if (c == ' ' || c == '\n' || c == '\r') {
            typed.append(' ');
            updatePredictions();
            return;
        }

        // Allow any non-control character
        // (things that are not like tabs, return, etc..)
        if (!Character.isISOControl(c)) {
            typed.append(c);
            updatePredictions();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Backspace here
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (typed.length() > 0) {
                typed.deleteCharAt(typed.length() - 1);
                updatePredictions();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // not used (included to complete interface)
    }

    /*********  Main *********************/

    public static void main(String[] args) {
        Trie trie = new Trie();
        int numWords = loadWordsIntoTrie(trie,"PrideAndPrejudice.txt");
        System.out.println("Loaded "+String.format("%,d", numWords)+" words from Pride and Prejudice");

        JFrame frame = new JFrame("TrieDisplay (Minimal)");
        TrieDisplay_minimal panel = new TrieDisplay_minimal(trie);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Give the panel focus so it receives key events
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

   private static int loadWordsIntoTrie(Trie trie, String filename) {
       int count = 0;

       try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
           String line = "";
           while ((line = br.readLine()) != null) {

               // Normalize line: lowercase, letters only, spaces preserved
               line = line.replaceAll("[^A-Za-z]", " ").toLowerCase();

               // Split into words
               String[] words = line.split("\\s+");

               for (String w : words) {
                   if (!w.isEmpty()) {
                       trie.insert(w);
                       count++;
                   }
               }
           }

       } catch (IOException e) {
           System.out.println("Could not read file: " + filename);
       }

       return count;
   }

}
