/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Searches the index for postings matching the query.
     *
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        return switch (queryType) {
            case INTERSECTION_QUERY -> searchIntersectionQuery(query);
            case PHRASE_QUERY -> null;
            case RANKED_QUERY -> null;
        };
    }

    private PostingsList searchIntersectionQuery(Query query) {
        PostingsList result = null;
        for (var queryTerm : query.queryterm) {
            var postings = index.getPostings(queryTerm.term);
            if (postings != null) {
                if (result == null) {
                    result = postings;
                } else {
                    result = result.intersect(postings);
                }
            }
        }
        return result;
    }
}
