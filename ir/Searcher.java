/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.IntStream;

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
            case PHRASE_QUERY -> searchPhraseQuery(query);
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

    private PostingsList searchPhraseQuery(Query query) {
        if (query.queryterm.isEmpty()) {
            return null;
        }
        var result = new PostingsList();
        var firstTerm = query.queryterm.getFirst();
        var postings = index.getPostings(firstTerm.term);
        for (int i = 0; i < postings.size(); i++) {
            var entry = postings.get(i);
            var followings = new ArrayList<TreeSet<Integer>>();
            if (query.queryterm.stream().skip(1)
                    .allMatch(q -> Optional.of(index.getPostings(q.term))
                            .map(p -> p.getByDocID(entry.docID))
                            .map(e -> e.offsets)
                            .map(followings::add)
                            .isPresent())) {
                entry.offsets.stream()
                        .filter(base -> IntStream.range(0, followings.size())
                                .allMatch(j -> followings.get(j).contains(base + j + 1)))
                        .findFirst()
                        .ifPresent(offset -> result.add(entry.docID, offset));
            }
        }
        return result;
    }
}
