/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PostingsList {

    /** The postings list */
    private final ArrayList<PostingsEntry> list = new ArrayList<>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    @NotNull
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public void add(@NotNull PostingsEntry entry) {
        if (list.isEmpty() || list.getLast().docID != entry.docID) {
            list.add(entry);
        }
    }

    @NotNull
    public PostingsList intersect(@NotNull PostingsList other) {
        var result = new PostingsList();
        var i = 0;
        var j = 0;
        while (i < list.size() && j < other.size()) {
            var entry = list.get(i);
            var otherEntry = other.get(j);
            if (entry.docID == otherEntry.docID) {
                result.add(entry);
                i++;
                j++;
            } else if (entry.docID < otherEntry.docID) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }
}
