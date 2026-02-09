/**
*/
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map;

class Trie {
    private TrieNode root;

    Trie() {
        root = new TrieNode();
    }

    // Finds the most common next char for any given String
    // or returns underscore '_' if the starting string is not
    // in the Trie
    public char mostLikelyNextChar(String s) {
        TrieNode current = root;

        for (char l : s.toCharArray()) {
            TrieNode nxt = current.getChildren().get(l);
            if (nxt == null) return '_';
            current = nxt;
        }

        long max = 0;
        char maxChar = '_';

        for (Map.Entry<Character, TrieNode> entry : current.getChildren().entrySet()) {
            if (entry.getValue().passCount > max) {
                max = entry.getValue().passCount;
                maxChar = entry.getKey();
            }
        }
        return maxChar;
    }

    void insert(String word) {
        TrieNode current = root;

        for (char l : word.toCharArray()) {
            TrieNode child = current.children.get(l);
            if (child == null) {
                child = new TrieNode();
                current.children.put(l, child);
            }
            current = child;
            current.incrementPassCount();
        }

        current.incrementEndCount();
    }

    public TreeSet<LetterNum> mostFreqKids(String pre) {
        TrieNode current = root;

        for (int i = 0; i < pre.length(); i++) {
            char ch = pre.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) return null;
            current = node;
        }

        TreeSet<LetterNum> set = new TreeSet<>();

        long totalContinuations = current.getPassCount() - current.getEndCount();
        if (totalContinuations <= 0) return set;

        for (Map.Entry<Character, TrieNode> entry : current.getChildren().entrySet()) {
            long kidsForLetter = entry.getValue().getPassCount();
            set.add(new LetterNum(entry.getKey(), (int) (kidsForLetter * 100.0 / totalContinuations + 0.5)));
        }

        return set;
    }

    public TreeSet<WordNum> getWordSet(String str) {
        TrieNode current = root;

        // Traverse the Trie to the node corresponding to the prefix `str`
        for (char ch : str.toCharArray()) {
            TrieNode nxt = current.getChildren().get(ch);
            if (nxt == null) {
                return new TreeSet<>();
            }
            current = nxt;
        }

        TreeSet<WordNum> wordSet = new TreeSet<>();

        long totalWordsWithPrefix = current.getPassCount();
        if (totalWordsWithPrefix <= 0) return wordSet;

        collectWords(current, new StringBuilder(str), wordSet, totalWordsWithPrefix);
        return wordSet;
    }

    // Helper method to recursively collect words from a given TrieNode
    private void collectWords(TrieNode node, StringBuilder prefix, Set wordSet, long totalWordsWithPrefix) {
        if (node.isEndOfWord()) {
            wordSet.add(new WordNum(prefix.toString(),
                    (long) (node.getEndCount() * 100.0 / totalWordsWithPrefix + 0.5)));
        }

        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            prefix.append(entry.getKey());
            collectWords(entry.getValue(), prefix, wordSet, totalWordsWithPrefix);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    public boolean contains(String word) {
        TrieNode current = root;

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) return false;
            current = node;
        }
        return current.isEndOfWord();
    }

    boolean isEmpty() {
        return root == null;
    }

    /***********   INNER CLASS ****/
    class TrieNode {
        private final Map<Character, TrieNode> children;
        private long passCount;
        private long endCount;

        public TrieNode() {
            children = new HashMap<>();
            passCount = 0;
            endCount = 0;
        }

        private Map<Character, TrieNode> getChildren() {
            return children;
        }

        private void incrementPassCount() {
            passCount++;
        }

        private void incrementEndCount() {
            endCount++;
        }

        private long getPassCount() {
            return passCount;
        }

        private long getEndCount() {
            return endCount;
        }

        private boolean isEndOfWord() {
            return endCount > 0;
        }

        public String toString() {
            return "(pass=" + passCount + ") end=" + endCount;
        }
    }

    public String readFromFile(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            return null;
        }
    }

    public String format(String input) {
        return input.toLowerCase().replaceAll("-", " ").replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ").trim();
    }

    /**  INNER CLASSES **/

    class WordNum implements Comparable<WordNum>{
	    private String word;
	    private long num;

	    public WordNum(String w, long n){
	      word = w;
	      num = n;
	    }

	    // Reverse Natural order (greatest to least)
	    public int compareTo(WordNum other){
	      if (this.num < other.num)
	          return 1;
	      else
	          return -1;
	    }

	    public String toString(){
	      if (num < 1)
	        return word +"=<1%";
	      return word +"="+num+"%";
	    }
 	 }

	class LetterNum implements Comparable<LetterNum>{
		private Character character;
		private int num;

		public LetterNum(Character c, int n){
		  character = c;
		  num = n;
		}

		// Reverse Natural order (greatest to least)
		public int compareTo(LetterNum other){
			  if (this.num < other.num)
				  return 1;
			  else
				  return -1;
			}

			public String toString(){
			  if (num < 1)
				return character +"=<1%";
			  return character +"="+num+"%";
			}
		  }













    public static void main(String[] args) {
        Trie t = new Trie();

        /*
        String text = t.readFromFile("PrideAndPrejudice.txt");
        text = t.format(text);
        String[] words = text.split(" ");
        for (String w : words)
            t.insert(w);
        */

        t.insert("the");
        t.insert("the");
        t.insert("the");
        t.insert("them");
        t.insert("then");
        t.insert("there");
        t.insert("there");
        t.insert("these");
        t.insert("therefore");

        System.out.println(t.mostFreqKids("the"));
        System.out.println(t.getWordSet("the"));
    }
}
