/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks.
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 3500000L;

    /** The dictionary hash table on disk can fit this many entries. */
    public static final int MAX_WORD_LENGTH = 60;

    /** The length of the longest word in the dictionary. */
    public static final int ENTRYSIZE = Long.BYTES + Integer.BYTES + Integer.BYTES + MAX_WORD_LENGTH;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    Map<String, PostingsList> index = new HashMap<>();


    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public static class Entry {
        String token;
        long dataPtr;
        int dataSize;

        Entry(String token, long dataPtr, int dataSize) {
            this.token = token;
            this.dataPtr = dataPtr;
            this.dataSize = dataSize;
        }
    }


    // ==================================================================


    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(RandomAccessFile file, String dataString, long ptr) {
        try {
            file.seek(ptr);
            byte[] data = dataString.getBytes();
            file.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Reads data from the data file
     */
    String readData(RandomAccessFile file, long ptr, int size) {
        try {
            file.seek(ptr);
            byte[] data = new byte[size];
            file.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(RandomAccessFile dict, Entry entry, long ptr) {
        try {
            dict.seek(ptr);
            dict.writeLong(entry.dataPtr);
            dict.writeInt(entry.dataSize);
            dict.writeInt(entry.token.length());
            dict.writeBytes(entry.token);
            var length = entry.token.length();
            if (length < MAX_WORD_LENGTH) {
                byte[] padding = new byte[MAX_WORD_LENGTH - length];
                dict.write(padding);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(RandomAccessFile dict, long ptr) {
        try {
            if (dict.length() <= ptr) {
                return null;
            }
            dict.seek(ptr);
            var dataPtr = dict.readLong();
            var dataSize = dict.readInt();
            var length = dict.readInt();
            if (length == 0) {
                return null;
            } else {
                var data = new byte[length];
                dict.readFully(data);
                return new Entry(new String(data), dataPtr, dataSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(Integer.parseInt(data[0]), data[1]);
                docLengths.put(Integer.parseInt(data[0]), Integer.parseInt(data[2]));
            }
        }
        freader.close();
    }


    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for (var entry : index.entrySet()) {
                var token = entry.getKey();
                var postingsList = entry.getValue();
                var dicIndex = Math.abs(token.hashCode()) % TABLESIZE;
                var dicPtr = dicIndex * ENTRYSIZE;
                var e = readEntry(dictionaryFile, dicPtr);
                while (e != null) {
                    collisions++;
                    dicIndex = (dicIndex + 1) % TABLESIZE;
                    dicPtr = dicIndex * ENTRYSIZE;
                    e = readEntry(dictionaryFile, dicPtr);
                }
                var data = postingsList.toString();
                writeEntry(dictionaryFile, new Entry(token, free, data.length()), dicPtr);
                free += writeData(dataFile, data, free);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }


    // ==================================================================


    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        if (index.containsKey(token)) {
            return index.get(token);
        }
        var dicIndex = Math.abs(token.hashCode()) % TABLESIZE;
        var dicPtr = dicIndex * ENTRYSIZE;
        PostingsList list = null;
        var e = readEntry(dictionaryFile, dicPtr);
        while (e != null) {
            if (e.token.equals(token)) {
                var data = readData(dataFile, e.dataPtr, e.dataSize);
                list = PostingsList.fromString(data);
                break;
            }
            dicIndex = (dicIndex + 1) % TABLESIZE;
            dicPtr = dicIndex * ENTRYSIZE;
            e = readEntry(dictionaryFile, dicPtr);
        }
        index.put(token, list);
        return list;
    }


    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        if (token.length() > MAX_WORD_LENGTH) {
            // We don't care about tokens that are too long
            return;
        }
        var list = index.computeIfAbsent(token, _ -> new PostingsList());
        list.add(docID, offset);
    }


    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        writeIndex();
        System.err.println("done!");
    }
}
