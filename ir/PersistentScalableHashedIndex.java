package ir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    public static final int GROUPSIZE = 120000;
    public static final int WEAK_CAPACITY = 120000;

    private final Semaphore groupSem = new Semaphore(0);
    private final CountDownLatch cleanupLatch = new CountDownLatch(1);
    private int currentIndexGroup = 0;

    PersistentScalableHashedIndex() {
        new Thread(this::workerThread).start();
    }

    @Override
    public void insert(String token, int docID, int offset) {
        if (index.size() == GROUPSIZE) {
            writeIndex();
            index.clear();
            free = 0;
            currentIndexGroup++;
            try {
                dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + "." + currentIndexGroup, "rw");
                dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + "." + currentIndexGroup, "rw");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.err.println("New index group: " + currentIndexGroup);
            groupSem.release();
        }
        super.insert(token, docID, offset);
    }

    @Override
    public void cleanup() {
        System.err.println("Start cleanup");
        writeIndex();
        index = new WeakHashMap<>(WEAK_CAPACITY);
        groupSem.release(2);
        try {
            cleanupLatch.await();
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        System.err.println("Index cleanup done");
    }

    private void workerThread() {
        int mergeGroup = 0;
        while (true) {
            try {
                groupSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mergeGroup <= currentIndexGroup) {
                if (mergeGroup > 0) {
                    mergeFiles(mergeGroup);
                    System.err.println("Merged group " + mergeGroup);
                }
                mergeGroup++;
            } else {
                break;
            }
        }
        cleanupLatch.countDown();
    }

    /**
     * Merges two index files into a new one.
     */
    private void mergeFiles(int mergeGroup) {
        var ndict1 = INDEXDIR + "/" + DICTIONARY_FNAME;
        var ndict2 = INDEXDIR + "/" + DICTIONARY_FNAME + "." + mergeGroup;
        var ndata1 = INDEXDIR + "/" + DATA_FNAME;
        var ndata2 = INDEXDIR + "/" + DATA_FNAME + "." + mergeGroup;
        var ntmpdict = INDEXDIR + "/" + DICTIONARY_FNAME + ".tmp";
        var ntmpdata = INDEXDIR + "/" + DATA_FNAME + ".tmp";
        try (var dict1 = new RandomAccessFile(ndict1, "r");
             var dict2 = new RandomAccessFile(ndict2, "r");
             var data1 = new RandomAccessFile(ndata1, "r");
             var data2 = new RandomAccessFile(ndata2, "r");
             var newDict = new RandomAccessFile(ntmpdict, "rw");
             var newData = new RandomAccessFile(ntmpdata, "rw")) {
            var merged = new boolean[(int) TABLESIZE];
            long newFree = 0;
            for (long i = 0; i < TABLESIZE; i++) {
                var entry1 = readEntry(dict1, i * ENTRYSIZE);
                if (entry1 == null) {
                    continue;
                }
                long j = Math.abs(entry1.token.hashCode()) % TABLESIZE;
                var entry2 = readEntry(dict2, j * ENTRYSIZE);
                while (entry2 != null) {
                    if (entry2.token.equals(entry1.token)) {
                        merged[(int) j] = true;
                        break;
                    }
                    j = (j + 1) % TABLESIZE;
                    entry2 = readEntry(dict2, j * ENTRYSIZE);
                }
                if (entry2 == null) {
                    writeEntry(newDict, new Entry(entry1.token, newFree, entry1.dataSize), i * ENTRYSIZE);
                    newFree += writeData(newData, readData(data1, entry1.dataPtr, entry1.dataSize), newFree);
                } else {
                    writeEntry(newDict, new Entry(entry1.token, newFree, entry1.dataSize + entry2.dataSize), i * ENTRYSIZE);
                    newFree += writeData(newData, readData(data1, entry1.dataPtr, entry1.dataSize), newFree);
                    newFree += writeData(newData, readData(data2, entry2.dataPtr, entry2.dataSize), newFree);
                }
            }
            for (long i = 0; i < TABLESIZE; i++) {
                if (merged[(int) i]) {
                    continue;
                }
                var entry2 = readEntry(dict2, i * ENTRYSIZE);
                if (entry2 == null) {
                    continue;
                }
                long j = Math.abs(entry2.token.hashCode()) % TABLESIZE;
                var entry3 = readEntry(newDict, j * ENTRYSIZE);
                while (entry3 != null) {
                    j = (j + 1) % TABLESIZE;
                    entry3 = readEntry(newDict, j * ENTRYSIZE);
                }
                writeEntry(newDict, new Entry(entry2.token, newFree, entry2.dataSize), j * ENTRYSIZE);
                newFree += writeData(newData, readData(data2, entry2.dataPtr, entry2.dataSize), newFree);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        new File(ndict1).delete();
        new File(ndict2).delete();
        new File(ndata1).delete();
        new File(ndata2).delete();
        new File(ntmpdict).renameTo(new File(ndict1));
        new File(ntmpdata).renameTo(new File(ndata1));
    }
}
