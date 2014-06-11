package utilities;

import repository.DocumentRepository;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

/**
 * I look for those documents in the repo that have the indicated original status
 * and try to retrieve them
 */
public class DownloadOriginalsOperation extends AbstractRepositoryOperation {


    String status = DocumentRepository.STATUS_MISSING;
    int countLimit = 0;
    long timeLimit = 0;

    /**
     * Look in the repo for all documents with the given status (using the constants defined
     * in DocumentRepository. Download those URLs and stored them again in the repo
     * @param repoFolder
     * @param status The status of the selected URLs
     * @param countLimit maximum number of URLs to process - 0 means all possible
     * @param timeLimit maximum time (in minutes) to work - 0 means all possible
     */
    public DownloadOriginalsOperation(File repoFolder, String status, int countLimit, long timeLimit) {
        super(repoFolder);
        this.status = status;
        this.countLimit = countLimit;
        this.timeLimit = timeLimit;

    }

    @Override
    public void run() {
        Iterator<URL> iterator = null;
        long count = 0;
        long start = System.currentTimeMillis();
        try {
            getRepo().startSession();
            iterator = getRepo().documentsByStatusIterator(status);
            while (iterator.hasNext() &
                    ((countLimit == 0) | (count <= countLimit)) &
                    ((timeLimit == 0) | ((System.currentTimeMillis() - start) < timeLimit * 60000))) {

                URL doc = iterator.next();
                retrieveOriginalDocument(doc.toString());
                count++;
            }
            getRepo().endSession();
        } catch (RepositoryException e) {
            getRepo().shutdown();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            getRepo().shutdown();
        }
    }

    public static void main(String[] args) {
        new DownloadOriginalsOperation(new File("data/repo"), DocumentRepository.STATUS_MISSING,0,1).run();
    }
}
