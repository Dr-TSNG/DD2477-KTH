/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */


package ir;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;


/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private final HashMap<String, PostingsList> index = new HashMap<>();


    /**
     * Inserts this token in the hashtable.
     */
    public void insert(String token, int docID, int offset) {
        var list = index.computeIfAbsent(token, _ -> new PostingsList());
        list.add(docID, offset);
    }


    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    @Nullable
    public PostingsList getPostings(String token) {
        return index.get(token);
    }


    /**
     * No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
