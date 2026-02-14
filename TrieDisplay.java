import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TrieDisplay extends JPanel implements KeyListener {
  private JFrame frame;
  private int width = 1400, height = 600;
  private Trie2 trie;
  private String word;
  private char likelyChar;
  private boolean wordsLoaded;
  private ArrayList<TypedWord> wordList;

  private static class TypedWord {
    String txt;
    Color col;

    TypedWord(String txt, Color col) {
      this.txt = txt;
      this.col = col;
    }
  }

  public TrieDisplay() {

    frame = new JFrame("Trie Next");
    frame.setSize(width, height);
    frame.add(this);
    frame.addKeyListener(this);
    frame.setResizable(false);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);

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
    g2.setColor(Color.BLACK);
    g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());

    g2.setFont(new Font("Courier New", Font.BOLD, 30));
    g2.setColor(Color.WHITE);
    if (wordsLoaded)
      g2.drawString("Start Typing:", 40, 100);
    else
      g2.drawString("Loading... please wait", 40, 100);

    int leftPoint = 40, charWidth = 18;
    for (TypedWord w : wordList) {
      g2.setColor(w.col);
      g2.drawString(w.txt, leftPoint, 200);
      leftPoint += (w.txt.length() * charWidth) + charWidth;
    }

    g2.setFont(new Font("Courier New", Font.BOLD, 32));
    if (trie.contains(word))
      g2.setColor(Color.GREEN);
    else if (likelyChar == '_')
      g2.setColor(Color.RED);
    else
      g2.setColor(Color.WHITE);

    g2.drawString(word, leftPoint, 200);
    g2.setFont(new Font("Courier New", Font.BOLD, 24));

    g2.setColor(Color.YELLOW);

    if (word.length() > 0) {
      java.util.List<String> topLetters = trie.topNextLettersWithPercent(word, 5);
      java.util.List<String> randomLetters = trie.randomNextLettersWithPercent(word, 5);
      java.util.List<String> topWords = trie.topNextWordsWithPercent(word, 5);
      java.util.List<String> randomWords = trie.randomNextWordsWithPercent(word, 5);

      g2.drawString("Top next letters -> " + String.join(", ", topLetters), 40, 320);
      g2.drawString("Random letters -> " + String.join(", ", randomLetters), 40, 360);
      g2.drawString("Top next words -> " + String.join(", ", topWords), 40, 400);
      g2.drawString("Random words -> " + String.join(", ", randomWords), 40, 440);
    } else {
      g2.drawString("Type letters to view next-letter and next-word likelihoods.", 40, 320);
    }
  }

  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    if (keyCode == KeyEvent.VK_SPACE) {
      if (trie.contains(word))
        wordList.add(new TypedWord(word, Color.GREEN));
      else
        wordList.add(new TypedWord(word, Color.RED));
      word = "";
    }
    if (keyCode == KeyEvent.VK_BACK_SPACE) {
      if (word.length() > 0)
        word = word.substring(0, word.length() - 1);
      else if (!wordList.isEmpty())
        word = wordList.remove(wordList.size() - 1).txt;
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
