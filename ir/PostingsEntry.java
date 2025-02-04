/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score;
    public List<Integer> offsets;

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
        this.offsets = new ArrayList<>();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(docID).append(";").append(score).append(";");
        sb.append(String.join(",", offsets.stream().map(Object::toString).toArray(String[]::new)));
        return sb.toString();
    }

    public static PostingsEntry fromString(String s) {
        var parts = s.split(";");
        var docID = Integer.parseInt(parts[0]);
        var score = Double.parseDouble(parts[1]);
        var entry = new PostingsEntry(docID, score);
        for (var offset : parts[2].split(",")) {
            entry.offsets.add(Integer.parseInt(offset));
        }
        return entry;
    }
}

