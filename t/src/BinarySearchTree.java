//import.java//

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.Objects;

/* inspired by https://algs4.cs.princeton.edu/32bst/BST.java.html */
public class BinarySearchTree<K,V>
extends AbstractMap<K,V> implements SortedMap<K,V>
{
    public static class Entry<K,V>
    extends AbstractMap.SimpleEntry<K,V>
    {
        @java.io.Serial
        private static final long serialVersionUID =
            0x7424_c5aabaf6dd8eL;

        private Entry<K,V> left;
        private Entry<K,V> right;

        private Entry(K key, V value)
        {
            super(key, value);
        }
    }

    private Entry<K,V> root;
    private Comparator<? super K> comparator;

    private int size;

    private K lower;
    private K upper;

    public BinarySearchTree () { this(null); }
    public BinarySearchTree (Comparator<? super K> cmp)
    {
        comparator = cmp;
    }

    /* Comparison logic used by all (this bit of
     * casting magic comes from TreeMap) */
    @SuppressWarnings("unchecked")
    private int compare(Object key1, Object key2) {
        K kk1, kk2;
        Comparable<? super K> ck1;

        kk2 = (K) key2;
        if (comparator != null) {
            kk1 = (K) key1;
            return comparator.compare(kk1, kk2);
        }
        ck1 = (Comparable<? super K>) key1;
        return ck1.compareTo(kk2);
    }

    // SortedMap interface

    @Override public Comparator<? super K> comparator()
    {
        return comparator;
    }

    @Override public SortedMap<K,V> subMap(K fromKey, K toKey)
    {
        Objects.requireNonNull(fromKey);
        Objects.requireNonNull(toKey);
        if (compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException(fromKey + " > " + toKey);
        }
        return view(fromKey, toKey);
    }

    @Override public SortedMap<K,V> headMap(K toKey)
    {
        Objects.requireNonNull(toKey);
        return view(null, toKey);
    }

    @Override public SortedMap<K,V> tailMap(K fromKey)
    {
        Objects.requireNonNull(fromKey);
        return view(fromKey, null);
    }

    private SortedMap<K,V> view(K lower, K upper) {
        BinarySearchTree<K,V> view =
            new BinarySearchTree<K,V>(this.comparator);
        view.root = root;
        view.lower = lower;
        view.upper = upper;
        return view;
    }

    @Override public K firstKey()
    {
        if (root == null) {
            throw new NoSuchElementException();
        }
        Entry<K,V> node = root;
        while (node.left != null) {
            node = node.left;
        }
        return node.getKey();
    }

    @Override public K lastKey()
    {
        if (root == null) {
            throw new NoSuchElementException();
        }
        Entry<K,V> node = root;
        while (node.right != null) {
            node = node.right;
        }
        return node.getKey();
    }

    // AbstractMap interface

    @Override public V put(K key, V value)
    {
        Objects.requireNonNull(key);
        if (lower != null && compare(key, lower) < 0) {
            throw new IllegalArgumentException("Lower bound violated");
        }
        if (upper != null && compare(key, upper) >= 0) {
            throw new IllegalArgumentException("Upper bound violated");
        }
        if (root == null) {
            root = new Entry<K,V>(key, value);
            size = 1;
            return value;
        }

        /* node -> next (equals null) */
        Entry<K,V> next;
        Entry<K,V> node;
        int delta;
        next = root;
        do {
            node = next;
            delta = compare(key, node.getKey());
            if (delta == 0) {
                size++;
                return node.setValue(value);
            }
            else if (delta < 0) {
                next = node.left;
            }
            else {
                next = node.right;
            }
        } while (next != null);

        next = new Entry<K,V>(key, value);
        if (delta < 0) {
            node.left = next;
        }
        else {
            node.right = next;
        }
        size++;
        return null;
    }

    Set<Map.Entry<K,V>> entries;

    @Override public Set<Map.Entry<K,V>> entrySet()
    {
        if (entries == null) {
            entries = new EntrySet();
        }
        return entries;
    }

    public class EntrySet extends AbstractSet<Map.Entry<K,V>>
    {
        @Override public int size() {
            return BinarySearchTree.this.size;
        }

        @Override public boolean remove(Object o) {
            /* This is some black magic casting I won't even question */
            if (!(o instanceof Map.Entry<?,?> entry)) {
                return false;
            }

            Object key = entry.getKey();
            Objects.requireNonNull(key);

            /* last -> node */
            Entry<K,V> last;
            Entry<K,V> node;
            last = null;
            node = BinarySearchTree.this.root;
            while (node != null) {
                last = node;
                int delta = compare(key, node.getKey());
                if (delta == 0) {
                    break;
                }
                else if (delta < 0) {
                    node = last.left;
                }
                else {
                    node = last.right;
                }
            }

            if (node != null && Objects.equals(
                node.getValue(), entry.getValue()))
            {
                delete(last, node);
                size++;
                return true;
            }
            return false;
        }

        @Override public Iterator<Map.Entry<K,V>> iterator()
        {
            return new EntryIterator();
        }
    }

    public class EntryIterator implements Iterator<Map.Entry<K,V>>
    {
        Entry<K,V> root;
        K min, max;
        Stack<Entry<K,V>> stack = new Stack<>();

        private EntryIterator()
        {
            root = BinarySearchTree.this.root;
            min = BinarySearchTree.this.lower;
            max = BinarySearchTree.this.upper;

            /* descend the tree, get as small as we can */
            while ( root != null ) {
                stack.push(root);
                if ( min != null && - compare(min, root.getKey()) < 0 ) {
                    root = root.right;
                }
                else {
                    root = root.left;
                }
            }
            /* in the event we overshoot... */
            while (!stack.isEmpty()) {
                if ( min != null && compare(min, stack.peek().getKey()) > 0 ) {
                    stack.pop();
                }
                else {
                    break;
                }
            }
        }

        public boolean hasNext()
        {
            return adv (false) != null;
        }

        public Map.Entry<K,V> next()
        {
            try {
                return Objects.requireNonNull( adv (true) );
            }
            catch (NullPointerException e) {
                throw new NoSuchElementException();
            }
        }

        /* This bit of esoteric code is from my iterator homework
         * (proudly NOT following the template they gave us (which
         * is also to say, it shouldn't be encumbered by whatever
         * copyright they have over their mediocre course)) */

        /** Cache. */ Entry<K,V> c;

        /**
         * Advance in-order traversal.
         *
         * @param nxt if true, do not save the result for later.
         * @return the current node
         */
        private Entry<K,V> adv (boolean nxt)
        {
            Entry<K,V> ret, urn, now;
            if (c != null) {
                ret = c;
                if (nxt) {
                    c = null;
                }
            }
            else {
                ret = null;
                /*
                 * find a suitable node bounded by the upper bound.
                 * (a previous iteration should have checked it
                 * against the lower bound.)
                 *
                 * if it's too big, its right subtree will be bigger,
                 * so we can just skip the entire subtree from there.
                 */
                while (!stack.isEmpty()) {
                    urn = stack.pop();
                    if ( max == null || - compare(max, urn.getKey()) <= 0 ) {
                        ret = urn;
                        break;
                    }
                    now = urn;
                }
                if (!nxt) {
                    c = ret;
                }
                /* find the in-order successor of ret */
                if (ret != null) {
                    urn = ret.right;
                    while (urn != null) {
                        stack.push(urn);
                        urn = urn.left;
                    }
                }
                /*
                 * skip the overshot nodes; we should be end up with at
                 * least one node, the node being where we started.
                 */
                while (!stack.isEmpty()) {
                    if ( min != null && compare (min, stack.peek().getKey()) > 0 ) {
                        stack.pop();
                    }
                    else {
                        break;
                    }
                }
            }
            return ret;
        }
    }

    /* BST deletion (?,? wouldn't work because Java would otherwise
     * be very unhappy if i try to assign them to each other)  */
    private void delete(Entry<K,V> last, Entry<K,V> node)
    {
        while (node != null) {
            Entry<K,V> left = node.left;
            Entry<K,V> right = node.right;

            if (left == null && right == null) {
                DELETE(last, node);
                node = null;
            }
            else if (left == null) {
                REPLACE(last, node, right);
                node = null;
            }
            else if (right == null) {
                REPLACE(last, node, left);
                node = null;
            }
            else {
                // find successor
                Entry<K,V> succ = right;
                Entry<K,V> cuss = node;
                while (succ != null) {
                    cuss = succ;
                    succ = cuss.left;
                }
                REPLACE(last, node, succ);
                last = cuss;
                node = succ;
            }
        }
    }

    private void DELETE(Entry<K,V> parent, Entry<K,V> disowned)
    {
        if (parent.left == disowned)        parent.left = null;
        else if (parent.right == disowned)  parent.right = null;
        else throw new IllegalStateException();
    }

    private void REPLACE(Entry<K,V> parent,
        Entry<K,V> disowned, Entry<K,V> adoptee)
    {
        if (parent.left == disowned)        parent.left = adoptee;
        else if (parent.right == disowned)  parent.right = adoptee;
        else throw new IllegalStateException();
    }

    /**
     * You should declare the starting index of your subtest.
     * @return the starting index; must be at least 0.
     */
    public int start ()
    {
        return 1;
    }

    /**
     * You should declare how many subtests there are here.
     * @return the number of subtests
     */
    public int avail ()
    {
        return 1;
    }

    /**
     * You should declare the plan for each subtest here.
     * @param test the test number
     * @return plan of {@code test} if there is one; otherwise 0
     */
    public int subplan (int test)
    {
        return test == 1 ? 11 : 0;
    }

    /**
     * Main function.
     *
     * @param args passed to my homie {@link #exec}
     */
    public static void main(String[] args)
    {
        BinarySearchTree<String, Integer> self =
            new BinarySearchTree<String, Integer>();
        /* Here is a rant from the original BinarySearchTree verbatim: */
        /* NOTE: it might look like that I meant to say (Class<blah<bluh>>).
         * That is not possible.  I have to erase the generic entirely
         * and re-apply it.  (I guess Class<?> works too, but it doesn't
         * emphasize the idea that we are literally force-replacing the
         * generic type because the Java compiler is..... dumb. =_=) */
        @SuppressWarnings("unchecked")
        Class<BinarySearchTree<String, Integer>> type =
            (Class) self.getClass();
        System.exit(self.exec("java " + type.getName(), args, type));
    }

    /**
     * Stolen example.
     *
     * <pre>{@literal
     *  % more tinyST.txt
     *  S E A R C H E X A M P L E
     *
     *  % java BinarySearchTree < tinyST.txt
     *  A 8
     *  C 4
     *  E 12
     *  H 5
     *  L 11
     *  M 9
     *  P 10
     *  R 3
     *  S 0
     *  X 7
     * }</pre>
     */
    public boolean test1() {
        init_subtest(1);

        SortedMap<String, Integer> counter;
        counter = new BinarySearchTree<>();

        String[] stdin = "SEARCHEXAMPLE".split("");
        for (int i = 0; i < stdin.length; ++i) {
            counter.put(stdin[i], i);
        }

        String[] chrmap = "ACEHLMPRSX".split("");
        int[] expect = {8, 4, 12, 5, 11, 9, 10, 3, 0, 7};
        Iterator<Map.Entry<String, Integer>> iterator
            = counter.entrySet().iterator();
        for (int i = 0; i < expect.length; ++i) {
            String name = chrmap[i] + " " + expect[i];
	    Map.Entry<String, Integer> entry;
            try {
                entry = iterator.next();
            }
            catch (NoSuchElementException e) {
                ok (false, name + "\nIterator exhausted");
                continue;
            }
            is (entry, new AbstractMap.SimpleEntry<String, Integer>
                (chrmap[i], expect[i]), name);
        }
        boolean end = false;
        try {
            iterator.next();
        }
        catch (NoSuchElementException e) {
            end = true;
        }
        ok (end, "Iterator exhausted");
        return done_subtest();
    }

//embed.java//
}
