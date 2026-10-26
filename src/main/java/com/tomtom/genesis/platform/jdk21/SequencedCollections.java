package com.tomtom.genesis.platform.jdk21;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.TreeMap;

/**
 * Java 21 (JEP 431) - sequenced collections.
 *
 * Three new interfaces - SequencedCollection, SequencedSet, SequencedMap -
 * retrofitted into the collections hierarchy to give every ordered collection
 * a uniform way to ask for its first element, its last element, and its reverse.
 */
public class SequencedCollections {

    public static void main(String[] args) {
        theInconsistency();
        theNewInterfaces();
        reversedViews();
        maps();
        caveats();
    }

    private static void theInconsistency() {
        header("The inconsistency it fixes");

        System.out.println("Getting the first element, before Java 21:");
        System.out.println("  List          -> list.get(0)");
        System.out.println("  Deque         -> deque.getFirst()");
        System.out.println("  SortedSet     -> set.first()");
        System.out.println("  LinkedHashSet -> set.iterator().next()      <- no method at all");
        System.out.println();
        System.out.println("Getting the LAST element:");
        System.out.println("  List          -> list.get(list.size() - 1)");
        System.out.println("  Deque         -> deque.getLast()");
        System.out.println("  SortedSet     -> set.last()");
        System.out.println("  LinkedHashSet -> loop over the whole thing, keeping the last seen");
        System.out.println();
        System.out.println("Four spellings for one idea, and one case with no answer at all - even");
        System.out.println("though LinkedHashSet has had a well-defined order since Java 1.4.");
    }

    private static void theNewInterfaces() {
        header("One vocabulary for all of them");

        SequencedCollection<String> list = new ArrayList<>(List.of("a", "b", "c"));
        SequencedSet<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));

        System.out.println("ArrayList     first=" + list.getFirst() + " last=" + list.getLast());
        System.out.println("LinkedHashSet first=" + set.getFirst() + " last=" + set.getLast());

        list.addFirst("start");
        list.addLast("end");
        System.out.println("after addFirst/addLast: " + list);

        System.out.println("removeFirst() -> " + list.removeFirst() + ", now " + list);
        System.out.println("removeLast()  -> " + list.removeLast() + ", now " + list);

        System.out.println();
        System.out.println("The full interface is small and obvious:");
        System.out.println("  getFirst, getLast, addFirst, addLast, removeFirst, removeLast, reversed");
        System.out.println();
        System.out.println("Where the hierarchy now sits:");
        System.out.println("  Collection -> SequencedCollection -> List, Deque, SortedSet(via SequencedSet)");
        System.out.println("  Map        -> SequencedMap        -> LinkedHashMap, SortedMap");
        System.out.println();
        System.out.println("Nothing was broken to do it - the new interfaces were inserted above");
        System.out.println("existing types and the new methods have default implementations.");
    }

    private static void reversedViews() {
        header("reversed() is a VIEW, not a copy");

        List<String> original = new ArrayList<>(List.of("a", "b", "c"));
        List<String> reversed = original.reversed();

        System.out.println("original = " + original);
        System.out.println("reversed = " + reversed);

        original.add("d");
        System.out.println();
        System.out.println("after original.add(\"d\"):");
        System.out.println("  original = " + original);
        System.out.println("  reversed = " + reversed + "   <- the view followed the change");

        reversed.addFirst("z");
        System.out.println();
        System.out.println("after reversed.addFirst(\"z\"):");
        System.out.println("  reversed = " + reversed);
        System.out.println("  original = " + original + "   <- writing through the view works too");

        System.out.println();
        System.out.println("This matters. Collections.reverse(list) mutates the list in place and");
        System.out.println("costs O(n). reversed() allocates a lightweight view in O(1) and shares");
        System.out.println("storage - but that also means it is not a snapshot.");
        System.out.println();
        System.out.println("For a genuine copy: List.copyOf(list.reversed()).");
    }

    private static void maps() {
        header("SequencedMap");

        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        System.out.println("firstEntry()  = " + map.firstEntry());
        System.out.println("lastEntry()   = " + map.lastEntry());
        System.out.println("reversed()    = " + map.reversed());

        map.putFirst("zeroth", 0);
        System.out.println("after putFirst: " + map);
        System.out.println("pollFirstEntry() -> " + map.pollFirstEntry() + ", now " + map);

        System.out.println();
        System.out.println("The three view methods are sequenced too, which is the quiet win:");
        System.out.println("  sequencedKeySet()   = " + map.sequencedKeySet());
        System.out.println("  sequencedValues()   = " + map.sequencedValues());
        System.out.println("  sequencedEntrySet() first = " + map.sequencedEntrySet().getFirst());

        SequencedMap<String, Integer> sorted = new TreeMap<>(map);
        System.out.println();
        System.out.println("TreeMap is a SequencedMap as well: first=" + sorted.firstEntry()
                + " last=" + sorted.lastEntry());

        System.out.println();
        System.out.println("putFirst / putLast on a SORTED map throw UnsupportedOperationException -");
        System.out.println("the position is determined by the comparator, not by you.");
        try {
            sorted.putFirst("aaa", 0);
        } catch (UnsupportedOperationException e) {
            System.out.println("  sorted.putFirst(...) -> UnsupportedOperationException, as expected");
        }
    }

    private static void caveats() {
        header("Caveats");

        System.out.println("1. getFirst/getLast throw NoSuchElementException on an empty collection,");
        System.out.println("   they do not return null. That is intentional and matches Deque.");

        List<String> empty = new ArrayList<>();
        try {
            empty.getFirst();
        } catch (java.util.NoSuchElementException e) {
            System.out.println("   [] .getFirst() -> NoSuchElementException");
        }

        System.out.println();
        System.out.println("2. HashSet and HashMap are NOT sequenced - they have no order to expose.");
        System.out.println("   Only LinkedHashSet/LinkedHashMap, List, Deque and the sorted types are.");
        System.out.println();
        System.out.println("3. This is a source-compatible but not entirely binary-neutral change:");
        System.out.println("   a class implementing List now inherits new default methods. If your");
        System.out.println("   own class already declared an incompatible reversed() or getFirst(),");
        System.out.println("   it will fail to compile on 21. Rare, but it is the one real migration");
        System.out.println("   hazard of this JEP.");
        System.out.println();
        System.out.println("4. List.of(...) returns an immutable list, so the add/remove methods of");
        System.out.println("   the interface throw - as they always did.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
