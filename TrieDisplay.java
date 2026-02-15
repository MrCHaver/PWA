import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TrieDisplay extends JPanel implements KeyListener {
  private JFrame frame;
  private int width = 1400, height = 700;
  private Trie2 trie;
  private String word;
  private char likelyChar;
  private boolean wordsLoaded;
  private ArrayList<TypedWord> wordList;

  // Regency-inspired palette
  private static final Color PAPER = new Color(245, 235, 215);
  private static final Color INK = new Color(60, 47, 47);
  private static final Color BORDER = new Color(110, 78, 46);
  private static final Color ACCENT = new Color(176, 141, 87);
  private static final Color VALID = new Color(62, 92, 74);
  private static final Color INVALID = new Color(122, 62, 72);
  private static final Color SOFT_INK = new Color(92, 71, 71);

  private static class TypedWord {
    String txt;
    Color col;

    TypedWord(String txt, Color col) {
      this.txt = txt;
      this.col = col;
    }
  }

  public TrieDisplay() {

    frame = new JFrame("Pride & Prejudice Predictive Writer");
    frame.setSize(width, height);
    frame.add(this);
    frame.addKeyListener(this);
    frame.setResizable(false);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.setFocusTraversalKeysEnabled(false);

    // Default Settings
    word = "";
    likelyChar = ' ';
    wordList = new ArrayList<>();
    wordsLoaded = false;

    trie = new Trie2();
    int total = loadWordsIntoTrie(trie, "PrideAndPrejudice.txt");
    System.out.println("Loaded " + total + " words");
    wordsLoaded = true;
    repaint();
  }

  // All Graphics handled in this method.  Don't do calculations here
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // Page background
    g2.setColor(PAPER);
    g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());

    // Decorative inner border
    g2.setColor(BORDER);
    g2.drawRect(20, 20, frame.getWidth() - 60, frame.getHeight() - 80);
    g2.drawRect(24, 24, frame.getWidth() - 68, frame.getHeight() - 88);

    // Header
    g2.setFont(new Font("Serif", Font.BOLD, 34));
    g2.setColor(INK);
    g2.drawString("Pride & Prejudice Writing Desk", 40, 80);

    g2.setFont(new Font("Serif", Font.PLAIN, 24));
    g2.setColor(SOFT_INK);
    if (wordsLoaded)
      g2.drawString("Compose your letter:", 40, 130);
    else
      g2.drawString("Preparing the manuscript...", 40, 130);

    int leftPoint = 40;
    final int wordBaseline = 210;
    final int minGap = 12;

    g2.setFont(new Font("Serif", Font.PLAIN, 30));
    FontMetrics bodyMetrics = g2.getFontMetrics();
    int naturalGap = Math.max(minGap, bodyMetrics.charWidth(' '));

    for (TypedWord w : wordList) {
      g2.setColor(w.col);
      g2.drawString(w.txt, leftPoint, wordBaseline);
      leftPoint += bodyMetrics.stringWidth(w.txt) + naturalGap;
    }

    if (trie.contains(word))
      g2.setColor(VALID);
    else if (likelyChar == '_')
      g2.setColor(INVALID);
    else
      g2.setColor(INK);

    g2.setFont(new Font("Serif", Font.BOLD, 34));
    g2.drawString(word, leftPoint, wordBaseline);

    g2.setColor(ACCENT);
    g2.drawLine(40, 240, frame.getWidth() - 80, 240);

    g2.setFont(new Font("Serif", Font.PLAIN, 24));
    g2.setColor(INK);

    if (word.length() > 0) {
      java.util.List<String> topLetters = trie.topNextLettersWithPercent(word, 5);
      java.util.List<String> randomLetters = trie.randomNextLettersWithPercent(word, 5);
      java.util.List<String> topWords = trie.topNextWordsWithPercent(word, 5);
      java.util.List<String> randomWords = trie.randomNextWordsWithPercent(word, 5);

      g2.drawString("Most probable next letters -> " + String.join(", ", topLetters), 40, 310);
      g2.drawString("Alternative letters -> " + String.join(", ", randomLetters), 40, 360);
      g2.drawString("Most probable continuations -> " + String.join(", ", topWords), 40, 410);
      g2.drawString("Alternative turns of phrase -> " + String.join(", ", randomWords), 40, 460);
      g2.setColor(SOFT_INK);
      g2.drawString("Percentages are based on patterns observed in Pride and Prejudice.", 40, 520);
    } else {
      g2.drawString("Begin a word to view likely next letters and likely next words.", 40, 310);
      g2.setColor(SOFT_INK);
      g2.drawString("Tip: Space commits a word. TAB autocompletes. Backspace edits.", 40, 360);
    }
  }

  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    if (keyCode == KeyEvent.VK_SPACE) {
      if (trie.contains(word))
        wordList.add(new TypedWord(word, VALID));
      else
        wordList.add(new TypedWord(word, INVALID));
      word = "";
    }
    if (keyCode == KeyEvent.VK_BACK_SPACE) {
      if (word.length() > 0)
        word = word.substring(0, word.length() - 1);
      else if (!wordList.isEmpty())
        word = wordList.remove(wordList.size() - 1).txt;
    }
    if (keyCode == KeyEvent.VK_TAB) {
      if (!word.isEmpty()) {
        String completion = trie.mostLikelyNextWord(word);
        if (completion != null && completion.startsWith(word))
          word = completion;
      }
      e.consume();
    }
    if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
      word += KeyEvent.getKeyText(keyCode).toLowerCase();

    likelyChar = trie.mostLikelyNextChar(word);
    System.out.println("keyCode =>" + keyCode + ", word =>" + word + ", likelyChar =>" + likelyChar);
    repaint();
  }

  /*** empty methods needed for interfaces **/
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}
  public void actionPerformed(ActionEvent e) {}

  public static void main(String[] args) {
    TrieDisplay app = new TrieDisplay();
  }

  private static int loadWordsIntoTrie(Trie2 trie, String filename) {
    int count = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.replaceAll("[^A-Za-z]", " ").toLowerCase();
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
