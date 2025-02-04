/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PostingsList {

    /** The postings list */
    private final ArrayList<PostingsEntry> list = new ArrayList<>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public PostingsEntry searchDocID(int docID) {
        var i = Collections.binarySearch(
                list,
                new PostingsEntry(docID, 0),
                Comparator.comparingInt(e -> e.docID)
        );
        return i >= 0 ? list.get(i) : null;
    }

    public void add(int docID, int offset) {
        if (list.isEmpty() || list.getLast().docID != docID) {
            list.add(new PostingsEntry(docID, 0));
        }
        list.getLast().offsets.add(offset);
    }

    public PostingsList intersect(PostingsList other) {
        var result = new PostingsList();
        for (var entry : this.list) {
            if (other.searchDocID(entry.docID) != null) {
                // We don't care about the actual offset values, so we can just add an empty PostingsEntry
                result.list.add(new PostingsEntry(entry.docID, 0));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (var entry : list) {
            sb.append(entry).append(" ");
        }
        return sb.toString();
    }

    public static PostingsList fromString(String s) {
        var list = new PostingsList();
        var data = s.split(" ");
        for (var entry : data) {
            list.list.add(PostingsEntry.fromString(entry));
        }
        return list;
    }
}
