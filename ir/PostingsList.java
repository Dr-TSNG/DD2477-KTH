/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;

public class PostingsList {

    /** The postings list */
    private final HashMap<Integer, PostingsEntry> list = new HashMap<>();

    /** Fast lookup for docIDs */
    private final ArrayList<Integer> index = new ArrayList<>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(index.get(i));
    }

    public PostingsEntry getByDocID(int docID) {
        return list.get(docID);
    }

    public void add(int docID, int offset) {
        var entry = list.computeIfAbsent(docID, _ -> {
            index.add(docID);
            return new PostingsEntry(docID, 0);
        });
        entry.offsets.add(offset);
    }

    public PostingsList intersect(PostingsList other) {
        var result = new PostingsList();
        for (var docID : this.list.keySet()) {
            if (other.list.containsKey(docID)) {
                // We don't care about the actual offset values, so we can just add an empty PostingsEntry
                result.list.put(docID, new PostingsEntry(docID, 0));
                result.index.add(docID);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(list.size()).append(" ");
        for (var docID : index) {
            sb.append(docID).append(" ").append(list.get(docID)).append(" ");
        }
        return sb.toString();
    }

    public static PostingsList fromString(String s) {
        var list = new PostingsList();
        var data = s.split(" ");
        for (int i = 1; i < data.length; i += 2) {
            var docID = Integer.parseInt(data[i]);
            var entry = PostingsEntry.fromString(data[i + 1]);
            list.list.put(docID, entry);
            list.index.add(docID);
        }
        return list;
    }
}
