/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.Serializable;
import java.util.TreeSet;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score;
    public TreeSet<Integer> offsets;

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     * <p>
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    public PostingsEntry(int docID, double score) {
        this.docID = docID;
        this.score = score;
        this.offsets = new TreeSet<>();
    }
}

