import java.util.*;

/**
 * Trie2 (Deluxe)
 *  - Fast insert/contains/prefix checks
 *  - Cached "best word" at every node for O(prefix) mostLikelyNextWord
 *  - Cached "best next char" at every node for O(prefix) mostLikelyNextChar
 *  - Optional prefix cache for short prefixes (best word + best char)
 *  - Extra utility methods:
 *      containsPrefix, getFrequency, countWordsWithPrefix,
 *      topKWords, printPrefixWords, printAllWords, printStructure, stats,
 *      delete (supported; uses subtree recompute when cached best is disturbed)
 *
 * Notes:
 *  - This version assumes words are inserted as given; you can normalize externally if you want.
 *  - Deletion is correct but can be slower in rare cases because caches may need recomputation.
 */
public class Trie2 {

    private Node root;

    // Simple cache for very short prefixes (optional optimization)
    // Keeps "best word" + "best next char" for common prefixes
    private Map<String, CacheEntry> prefixCache;
    private int cacheMaxPrefixLen = 2; // cache prefixes length <= 2

    public Trie2() {
        root = new Node(null, '\0');
        prefixCache = new HashMap<>();
    }

    /* =========================
       Core Required Methods
       ========================= */

    public void insert(String word) {

        if (word == null || word.length() == 0)
            return;

        root.passCount++;
        Node curr = root;

        // track path nodes for possible cache updates or later logic
        // (not strictly required, but helpful)
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            // putIfAbsent style (but still AP-friendly)
            Node child = curr.children.get(c);
            if (child == null) {
                child = new Node(curr, c);
                curr.children.put(c, child);
            }

            curr.passCount++;
            curr = child;
        }

        // terminal node updates
        curr.endCount++;

        // cached best-word update (propagate along path again)
        // We need the final frequency of this word:
        long newFreq = curr.endCount;

        // Walk again down the word updating caches as needed
        Node n = root;
        updateBestWord(n, word, newFreq);
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Node child = n.children.get(c);

            // update bestNextChar at this node based on child's passCount
            updateBestNextChar(n, c, child.passCount);

            // move down and update best word cache at child node
            n = child;
            updateBestWord(n, word, newFreq);
        }

        // Update prefix cache for short prefixes
        updatePrefixCacheOnInsert(word, newFreq);
    }

    public boolean contains(String word) {
        if (word == null || word.length() == 0)
            return false;

        Node node = getNode(word);
        return node != null && node.isEndOfWord();
    }

    public char mostLikelyNextChar(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return '_';

        // small-prefix cache
        if (prefix.length() <= cacheMaxPrefixLen) {
            CacheEntry ce = prefixCache.get(prefix);
            if (ce != null)
                return ce.bestNextChar;
        }

        Node node = getNode(prefix);
        if (node == null)
            return '_';

        return node.bestNextChar;
    }

    public String mostLikelyNextWord(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return "";

        // small-prefix cache
        if (prefix.length() <= cacheMaxPrefixLen) {
            CacheEntry ce = prefixCache.get(prefix);
            if (ce != null)
                return ce.bestWord;
        }

        Node node = getNode(prefix);
        if (node == null)
            return "";

        return node.bestWord;
    }


    // Returns top K likely next letters for a prefix, formatted as "x (12.3%)".
    public List<String> topNextLettersWithPercent(String prefix, int k) {
        ArrayList<ScoredOption> options = getNextLetterOptions(prefix);
        return formatTopOptions(options, k);
    }

    // Returns random next-letter options (excluding the strongest predictions when possible),
    // formatted as "x (12.3%)".
    public List<String> randomNextLettersWithPercent(String prefix, int k) {
        ArrayList<ScoredOption> options = getNextLetterOptions(prefix);
        return formatRandomOptions(options, k);
    }

    // Returns top K likely next words for a prefix, formatted as "word (12.3%)".
    public List<String> topNextWordsWithPercent(String prefix, int k) {
        ArrayList<ScoredOption> options = getNextWordOptions(prefix);
        return formatTopOptions(options, k);
    }

    // Returns random next-word options (excluding the strongest predictions when possible),
    // formatted as "word (12.3%)".
    public List<String> randomNextWordsWithPercent(String prefix, int k) {
        ArrayList<ScoredOption> options = getNextWordOptions(prefix);
        return formatRandomOptions(options, k);
    }

    public void printWordFrequencies() {
        TreeSet<WordFreq> set = new TreeSet<>();
        collectWords(root, new StringBuilder(), set);
        for (WordFreq wf : set) {
            System.out.println(wf.word + ": " + wf.freq);
        }
    }

    /* =========================
       Suggested Extra Methods
       ========================= */

    // True if the prefix path exists (even if not a full word)
    public boolean containsPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return false;
        return getNode(prefix) != null;
    }

    // Frequency of an exact word (0 if not present as a full word)
    public long getFrequency(String word) {
        if (word == null || word.length() == 0)
            return 0;
        Node n = getNode(word);
        if (n == null)
            return 0;
        return n.endCount;
    }

    // How many inserted words begin with this prefix
    // This is fast because passCount approximates "traffic", but to match meaning:
    // we store at each node a "prefixCount" as passCount is updated at parent in your original pattern.
    // Here: node.passCount is "times this node was stepped through (arrived at)". That matches prefix usage.
    public long countWordsWithPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return 0;
        Node n = getNode(prefix);
        if (n == null)
            return 0;
        return n.passCount;
    }

    // Print all words beginning with prefix (alphabetical)
    public void printPrefixWords(String prefix) {
        if (prefix == null || prefix.length() == 0)
            return;

        Node start = getNode(prefix);
        if (start == null)
            return;

        ArrayList<String> out = new ArrayList<>();
        collectWordsAlpha(start, new StringBuilder(prefix), out);
        for (String w : out)
            System.out.println(w);
    }

    // Return top K words by frequency under a prefix (descending frequency).
    // Includes pruning: if a subtree passCount is too small to beat the current Kth, skip it.
    public List<String> topKWords(String prefix, int k) {
        ArrayList<String> result = new ArrayList<>();
        if (prefix == null || prefix.length() == 0 || k <= 0)
            return result;

        Node start = getNode(prefix);
        if (start == null)
            return result;

        // Keep best K words in a min-heap by frequency (smallest at top)
        PriorityQueue<WordFreq> pq = new PriorityQueue<>(new Comparator<WordFreq>() {
            public int compare(WordFreq a, WordFreq b) {
                if (a.freq != b.freq)
                    return Long.compare(a.freq, b.freq); // min-heap by freq
                return b.word.compareTo(a.word); // reverse alpha so the "worst" is removed first
            }
        });

        topKDFS(start, new StringBuilder(prefix), pq, k);

        // Extract in descending order
        ArrayList<WordFreq> temp = new ArrayList<>();
        while (!pq.isEmpty()) temp.add(pq.poll());
        // temp is ascending; reverse to descending
        for (int i = temp.size() - 1; i >= 0; i--)
            result.add(temp.get(i).word);

        return result;
    }

    // Print all words (alphabetical)
    public void printAllWords() {
        ArrayList<String> out = new ArrayList<>();
        collectWordsAlpha(root, new StringBuilder(), out);
        for (String w : out)
            System.out.println(w);
    }

    // Print structure info: each word + its terminal node pass/end
    public void printStructure() {
        printStructureDFS(root, new StringBuilder());
    }

    // Basic stats: nodes, distinct words, total inserts, max depth
    public void stats() {
        Stats s = new Stats();
        statsDFS(root, 0, s);
        System.out.println("Total inserts (root.passCount): " + root.passCount);
        System.out.println("Total nodes: " + s.nodes);
        System.out.println("Distinct words: " + s.distinctWords);
        System.out.println("Max depth: " + s.maxDepth);
    }

    // Delete one occurrence of a word (decrement frequency).
    // Returns true if something was deleted, false if word not present.
    public boolean delete(String word) {
        if (word == null || word.length() == 0)
            return false;

        Node terminal = getNode(word);
        if (terminal == null || terminal.endCount == 0)
            return false;

        // Decrement counts
        terminal.endCount--;

        // Decrement passCount along the path nodes that are "stepped through"
        // In this implementation, passCount increments on the node you are at before moving down.
        // We incremented root.passCount once per insert, and for each step we incremented curr.passCount before moving.
        // We'll mirror that:
        root.passCount--;
        Node curr = root;
        for (int i = 0; i < word.length(); i++) {
            curr.passCount--;
            curr = curr.children.get(word.charAt(i));
        }
        // curr is terminal; note: terminal.passCount was decremented as the last "curr.passCount--" happens at parent.
        // That matches insert's pattern.

        // Cleanup: remove nodes that become useless (no children, no endCount)
        pruneIfNeeded(terminal);

        // Because deletion can invalidate cached bestWord/bestNextChar,
        // recompute caches along the prefix path of the deleted word.
        // (This is the simpler, correct approach; insert-only tries don't need it.)
        refreshCachesAlongPath(word);

        // Update prefix cache for short prefixes
        rebuildPrefixCacheForShortPrefixes();

        return true;
    }

    /* =========================
       Internal Optimizations
       ========================= */

    // Cache bestWord/bestCount at a node (tie-break alphabetical)
    private void updateBestWord(Node node, String candidateWord, long candidateCount) {
        if (candidateCount > node.bestWordCount) {
            node.bestWordCount = candidateCount;
            node.bestWord = candidateWord;
        } else if (candidateCount == node.bestWordCount && candidateCount > 0) {
            // tie-break alphabetical
            if (node.bestWord.equals("") || candidateWord.compareTo(node.bestWord) < 0) {
                node.bestWord = candidateWord;
            }
        }
    }

    // Cache bestNextChar at a node based on child passCount (tie-break alphabetical)
    private void updateBestNextChar(Node node, char candidateChar, long candidatePass) {
        if (candidatePass > node.bestNextPass) {
            node.bestNextPass = candidatePass;
            node.bestNextChar = candidateChar;
        } else if (candidatePass == node.bestNextPass && candidatePass > 0) {
            // tie-break alphabetical
            if (node.bestNextChar == '_' || candidateChar < node.bestNextChar) {
                node.bestNextChar = candidateChar;
            }
        }
    }

    private void updatePrefixCacheOnInsert(String word, long newFreq) {
        // update entries for prefixes up to cacheMaxPrefixLen
        int max = Math.min(cacheMaxPrefixLen, word.length());
        for (int len = 1; len <= max; len++) {
            String p = word.substring(0, len);
            CacheEntry ce = prefixCache.get(p);
            if (ce == null) {
                ce = new CacheEntry();
                prefixCache.put(p, ce);
            }

            // best word
            if (newFreq > ce.bestWordCount || (newFreq == ce.bestWordCount && word.compareTo(ce.bestWord) < 0)) {
                ce.bestWord = word;
                ce.bestWordCount = newFreq;
            }

            // best next char (for that prefix)
            Node node = getNode(p);
            if (node != null) {
                ce.bestNextChar = node.bestNextChar;
            }
        }
    }

    private void rebuildPrefixCacheForShortPrefixes() {
        prefixCache.clear();
        // rebuild by traversing trie for prefixes length <= cacheMaxPrefixLen
        rebuildCacheDFS(root, new StringBuilder(), 0);
    }

    private void rebuildCacheDFS(Node node, StringBuilder path, int depth) {
        if (depth > 0 && depth <= cacheMaxPrefixLen) {
            String prefix = path.toString();
            CacheEntry ce = new CacheEntry();
            ce.bestWord = node.bestWord;
            ce.bestWordCount = node.bestWordCount;
            ce.bestNextChar = node.bestNextChar;
            prefixCache.put(prefix, ce);
        }

        if (depth == cacheMaxPrefixLen)
            return;

        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            path.append(e.getKey());
            rebuildCacheDFS(e.getValue(), path, depth + 1);
            path.deleteCharAt(path.length() - 1);
        }
    }

    private ArrayList<ScoredOption> getNextLetterOptions(String prefix) {
        ArrayList<ScoredOption> out = new ArrayList<>();
        if (prefix == null || prefix.length() == 0)
            return out;

        Node node = getNode(prefix);
        if (node == null || node.children.isEmpty())
            return out;

        long total = 0;
        for (Node child : node.children.values()) total += child.passCount;
        if (total <= 0)
            return out;

        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            double percent = (e.getValue().passCount * 100.0) / total;
            out.add(new ScoredOption(String.valueOf(e.getKey()), percent));
        }

        Collections.sort(out);
        return out;
    }

    private ArrayList<ScoredOption> getNextWordOptions(String prefix) {
        ArrayList<ScoredOption> out = new ArrayList<>();
        if (prefix == null || prefix.length() == 0)
            return out;

        Node node = getNode(prefix);
        if (node == null)
            return out;

        ArrayList<WordFreq> words = new ArrayList<>();
        collectWordCounts(node, new StringBuilder(prefix), words);

        long total = 0;
        for (WordFreq wf : words) total += wf.freq;
        if (total <= 0)
            return out;

        for (WordFreq wf : words) {
            double percent = (wf.freq * 100.0) / total;
            out.add(new ScoredOption(wf.word, percent));
        }

        Collections.sort(out);
        return out;
    }

    private void collectWordCounts(Node curr, StringBuilder path, ArrayList<WordFreq> out) {
        if (curr.endCount > 0) {
            out.add(new WordFreq(path.toString(), curr.endCount));
        }

        for (Map.Entry<Character, Node> entry : curr.children.entrySet()) {
            path.append(entry.getKey());
            collectWordCounts(entry.getValue(), path, out);
            path.deleteCharAt(path.length() - 1);
        }
    }

    private List<String> formatTopOptions(ArrayList<ScoredOption> options, int k) {
        ArrayList<String> out = new ArrayList<>();
        if (k <= 0)
            return out;

        int limit = Math.min(k, options.size());
        for (int i = 0; i < limit; i++) out.add(options.get(i).toDisplay());
        return out;
    }

    private List<String> formatRandomOptions(ArrayList<ScoredOption> options, int k) {
        ArrayList<String> out = new ArrayList<>();
        if (k <= 0 || options.isEmpty())
            return out;

        ArrayList<ScoredOption> pool = new ArrayList<>();
        int skipTop = Math.min(5, options.size());
        if (options.size() > skipTop) {
            for (int i = skipTop; i < options.size(); i++) pool.add(options.get(i));
        } else {
            pool.addAll(options);
        }

        Collections.shuffle(pool);
        int limit = Math.min(k, pool.size());
        for (int i = 0; i < limit; i++) out.add(pool.get(i).toDisplay());
        return out;
    }

    /* =========================
       Traversal Helpers
       ========================= */

    private void collectWords(Node curr, StringBuilder path, TreeSet<WordFreq> set) {
        if (curr.endCount > 0) {
            set.add(new WordFreq(path.toString(), curr.endCount));
        }
        for (Map.Entry<Character, Node> entry : curr.children.entrySet()) {
            path.append(entry.getKey());
            collectWords(entry.getValue(), path, set);
            path.deleteCharAt(path.length() - 1);
        }
    }

    // alphabetical collection (TreeMap ordering)
    private void collectWordsAlpha(Node curr, StringBuilder path, ArrayList<String> out) {
        if (curr.endCount > 0) {
            out.add(path.toString() + " (" + curr.endCount + ")");
        }
        for (Map.Entry<Character, Node> entry : curr.children.entrySet()) {
            path.append(entry.getKey());
            collectWordsAlpha(entry.getValue(), path, out);
            path.deleteCharAt(path.length() - 1);
        }
    }

    // Top-K DFS with pruning using passCount:
    // if we already have K candidates and this subtree's passCount <= kthFreq, it can't improve.
    private void topKDFS(Node node, StringBuilder path, PriorityQueue<WordFreq> pq, int k) {
        // pruning
        if (pq.size() == k) {
            WordFreq kth = pq.peek(); // current smallest among best
            if (kth != null && node.passCount <= kth.freq) {
                // This subtree was traversed <= kth frequency;
                // it's unlikely to contain a word with endCount > kth.freq.
                // (This is a heuristic prune; safe-ish for typical data. You can remove prune if you want strict.)
                // To keep it strictly correct, comment out this prune block.
                // return;
            }
        }

        if (node.endCount > 0) {
            WordFreq wf = new WordFreq(path.toString(), node.endCount);
            if (pq.size() < k) {
                pq.add(wf);
            } else {
                WordFreq smallest = pq.peek();
                if (isBetterThan(wf, smallest)) {
                    pq.poll();
                    pq.add(wf);
                }
            }
        }

        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            path.append(e.getKey());
            topKDFS(e.getValue(), path, pq, k);
            path.deleteCharAt(path.length() - 1);
        }
    }

    private boolean isBetterThan(WordFreq a, WordFreq b) {
        if (b == null) return true;
        if (a.freq != b.freq) return a.freq > b.freq;
        // tie-break alphabetical
        return a.word.compareTo(b.word) < 0;
    }

    private void printStructureDFS(Node node, StringBuilder path) {
        if (node.endCount > 0) {
            System.out.println(path.toString() + " -> " + node.toString()
                    + " bestWord=" + node.bestWord + " bestWordCount=" + node.bestWordCount
                    + " bestNextChar=" + node.bestNextChar);
        }
        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            path.append(e.getKey());
            printStructureDFS(e.getValue(), path);
            path.deleteCharAt(path.length() - 1);
        }
    }

    private static class Stats {
        long nodes = 0;
        long distinctWords = 0;
        int maxDepth = 0;
    }

    private void statsDFS(Node node, int depth, Stats s) {
        s.nodes++;
        if (node.endCount > 0) s.distinctWords++;
        if (depth > s.maxDepth) s.maxDepth = depth;
        for (Node child : node.children.values()) {
            statsDFS(child, depth + 1, s);
        }
    }

    /* =========================
       Node Navigation Helpers
       ========================= */

    // Returns the node at the end of the given string, or null if path missing
    private Node getNode(String s) {
        Node curr = root;
        for (int i = 0; i < s.length(); i++) {
            Node child = curr.children.get(s.charAt(i));
            if (child == null)
                return null;
            curr = child;
        }
        return curr;
    }

    /* =========================
       Deletion Helpers
       ========================= */

    private void pruneIfNeeded(Node node) {
        Node curr = node;
        while (curr != null && curr != root) {
            if (curr.endCount == 0 && curr.children.isEmpty()) {
                Node parent = curr.parent;
                if (parent != null) {
                    parent.children.remove(curr.letterFromParent);
                }
                curr = parent;
            } else {
                break;
            }
        }
    }

    private void refreshCachesAlongPath(String word) {
        // Recompute bestWord/bestNextChar for each node along this word's path
        // by scanning its children and (for bestWord) scanning its subtree.
        // This is the "simple + correct" approach for supporting delete.

        Node curr = root;
        recomputeNodeCaches(curr, "");

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Node child = curr.children.get(c);
            if (child == null) break; // path already pruned
            prefix.append(c);
            curr = child;
            recomputeNodeCaches(curr, prefix.toString());
        }
    }

    private void recomputeNodeCaches(Node node, String prefix) {
        // bestNextChar recompute from children
        node.bestNextChar = '_';
        node.bestNextPass = -1;
        for (Map.Entry<Character, Node> e : node.children.entrySet()) {
            updateBestNextChar(node, e.getKey(), e.getValue().passCount);
        }

        // bestWord recompute by scanning subtree (can be expensive; only used for delete maintenance)
        BestWord best = new BestWord();
        dfsMostLikelyWord(node, new StringBuilder(prefix), best);
        node.bestWord = best.word;
        node.bestWordCount = best.count;
    }

    /* =========================
       Original DFS for best word
       ========================= */

    private static class BestWord {
        String word = "";
        long count = 0;
    }

    private void dfsMostLikelyWord(Node node, StringBuilder path, BestWord best) {
        if (node.endCount > 0) {
            if (node.endCount > best.count) {
                best.count = node.endCount;
                best.word = path.toString();
            } else if (node.endCount == best.count && node.endCount > 0) {
                // tie-break alphabetical
                String candidate = path.toString();
                if (best.word.equals("") || candidate.compareTo(best.word) < 0) {
                    best.word = candidate;
                }
            }
        }

        for (Map.Entry<Character, Node> entry : node.children.entrySet()) {
            path.append(entry.getKey());
            dfsMostLikelyWord(entry.getValue(), path, best);
            path.deleteCharAt(path.length() - 1);
        }
    }

    private static class ScoredOption implements Comparable<ScoredOption> {
        String text;
        double percent;

        ScoredOption(String text, double percent) {
            this.text = text;
            this.percent = percent;
        }

        String toDisplay() {
            return text + " (" + String.format(Locale.US, "%.1f", percent) + "%)";
        }

        @Override
        public int compareTo(ScoredOption other) {
            int pctCmp = Double.compare(other.percent, this.percent);
            if (pctCmp != 0)
                return pctCmp;
            return this.text.compareTo(other.text);
        }
    }

    /* =========================
       printWordFrequencies sorting helper
       ========================= */

    private class WordFreq implements Comparable<WordFreq> {
        String word;
        long freq;

        WordFreq(String w, long f) {
            word = w;
            freq = f;
        }

        @Override
        public int compareTo(WordFreq other) {
            if (this.freq != other.freq)
                return Long.compare(other.freq, this.freq); // descending
            return this.word.compareTo(other.word); // tie-break alpha
        }
    }

    /* =========================
       Prefix Cache Entry
       ========================= */

    private static class CacheEntry {
        String bestWord = "";
        long bestWordCount = 0;
        char bestNextChar = '_';
    }

    /* =========================
       NODE -- Inner class
       ========================= */

    class Node {
        long passCount;                 // total # times node is traversed
        long endCount;                  // # times end of word
        Map<Character, Node> children;  // TreeMap keeps alphabetical order, helpful for prints/debug

        // Deluxe fields:
        Node parent;
        char letterFromParent;

        // cached bests
        String bestWord;
        long bestWordCount;

        char bestNextChar;
        long bestNextPass;

        Node(Node parent, char letterFromParent) {
            // TreeMap makes alphabetical traversal consistent
            children = new TreeMap<Character, Node>();

            passCount = 0;
            endCount = 0;

            this.parent = parent;
            this.letterFromParent = letterFromParent;

            bestWord = "";
            bestWordCount = 0;

            bestNextChar = '_';
            bestNextPass = -1;
        }

        boolean isEndOfWord() {
            return endCount > 0;
        }

        @Override
        public String toString() {
            return "(pass = " + passCount + ", end = " + endCount + ")";
        }
    }
}
