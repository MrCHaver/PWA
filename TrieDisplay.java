import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
//help me write a specification and rubric for an assignment that uses a Trie to help predict the next letter and
public class TrieDisplay extends JPanel implements KeyListener
{
  private JFrame frame;
  private int size = 30, width = 1400, height = 600;
  private Trie trie;
  private String word, fullText;      // Word you are trying to spell printed in large font
  private char likelyChar;      // Used for single most likely character
  private boolean wordsLoaded;    // Use this to make sure words are alll loaded before you start typing
  private ArrayList<Word> wordList;


  public TrieDisplay(){

    frame=new JFrame("Trie Next");
    frame.setSize(width,height);
    frame.add(this);
    frame.addKeyListener(this);
    frame.setResizable(false);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);

    // Default Settings
    word = fullText = "";
    likelyChar = ' ';  // Used for single most likely character
    wordList = new ArrayList<Word>();
    wordsLoaded = false;

    trie = new Trie();
    String text = trie.readFromFile("PrideAndPrejudice.txt");
    text = trie.format(text);
    String[] words = text.split(" ");
    for (String w : words)
      trie.insert(w);
    System.out.println("Loaded");
    wordsLoaded = true;   // Set flag to true indicating program is ready
    repaint();

  }

  // All Graphics handled in this method.  Don't do calculations here
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);                // Setup and Background
    Graphics2D g2=(Graphics2D)g;
    g2.setColor(Color.BLACK);
    g2.fillRect(0,0,frame.getWidth(),frame.getHeight());

    g2.setFont(new Font("Courier New",Font.BOLD,30));       // Header
    g2.setColor(Color.WHITE);
    if (wordsLoaded)
      g2.drawString("Start Typing:",40,100);
    else
      g2.drawString("Loading... please wait",40,100);
    System.out.println("Word -->"+fullText+word);

    int leftPoint = 40, charWidth = 18;
    for (Word w : wordList){
      g2.setColor(w.col);
      g2.drawString(w.txt,leftPoint,200);
      leftPoint += (w.txt.length()*charWidth)+charWidth;
    }


    g2.setFont(new Font("Courier New",Font.BOLD,32));      // Typed text:  White == valid partial word
    if (trie.contains(word))                               //              Red == invalid
      g2.setColor(Color.GREEN);                            //              Green == full word
    else
      if (likelyChar == '_')
        g2.setColor(Color.RED);
      else
        g2.setColor(Color.WHITE);

    g2.drawString(word,leftPoint,200);
    g2.setFont(new Font("Courier New",Font.BOLD,24));


    //  YOUR CODE HERE
    g2.setColor(Color.YELLOW);
    //System.out.println(word+","+trie.getWordSet(word));
    g2.drawString("Likely next char ->"+trie.mostLikelyNextChar(word),40,320);

    if (word.length() > 0){
       g2.drawString("Likely char freq ->"+trie.mostFreqKids(word),40,360);
       String lWords = "";
       TreeSet<WordNum> nextWords = trie.getWordSet(word);
       for (int i = 0; i < 5; i++){
         if (i < nextWords.size()-1)
          lWords += nextWords.pollFirst()+", ";
       }
       lWords += nextWords.pollFirst();
       //String lWords = trie.getWordSet(word).toString();
       g2.drawString("Likely words -> "+lWords,40,400);
       ArrayList<WordNum> randWords = new ArrayList(nextWords);
       String rWords = "";
       for (int i = 0; i < 5; i++){
         if (randWords.size()>1)
            rWords += randWords.remove( (int)(Math.random()*randWords.size()) )+", ";
       }
       if (randWords.size()>0)
          rWords += randWords.remove( (int)(Math.random()*randWords.size()) );
       g2.drawString("Other Options -> "+rWords,40,440);

    }
    // Draw String below here for next most likely letter / letters
    // If there ae no possible next letters write something like "no further possibilities"

  }


  public void keyPressed(KeyEvent e){              // This handles key press
    int keyCode = e.getKeyCode();
    if (keyCode == 32){
      //fullText +=word+" ";
      if (trie.contains(word))
        wordList.add(new Word(word,Color.GREEN));
      else
        wordList.add(new Word(word,Color.RED));
      word = "";
    }
    if (keyCode == 8){  // Backspace -> remove last letter
      if (word.length() >0)
        word = word.substring(0,word.length()-1);
      else {
        //System.out.println("------>"+wordList.get(wordList.size()-1).txt);
        word = wordList.remove(wordList.size()-1).txt;
      }
      //System.out.println(wordList);

    }
    if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)  // alphabetic key
          word += KeyEvent.getKeyText(keyCode).toLowerCase();
    likelyChar = trie.mostLikelyNextChar(word);
    System.out.println("keyCode =>"+keyCode+", word =>"+word+", likelyChar =>"+likelyChar);     // Uncomment to Debug
    repaint();
  }

  /*** empty methods needed for interfaces **/
  public void keyReleased(KeyEvent e){}
  public void keyTyped(KeyEvent e){}
  public void actionPerformed(ActionEvent e) {}

  public static void main(String[] args){
    TrieDisplay app=new TrieDisplay();
  }
}