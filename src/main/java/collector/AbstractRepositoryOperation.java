package collector;

import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ConnectTimeoutException;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by alejandrofernandez on 6/10/14.
 */
public abstract class AbstractRepositoryOperation {

    private final DocumentRepository repo;

    public AbstractRepositoryOperation(File repoFolder) {
        this.repo = new DocumentRepository(repoFolder);
    }

    public DocumentRepository getRepo() {
        return repo;
    }

    public abstract void run();

    /**
     * Attempt to retrieve the document. Store the document and its status in the repo
     * @param line
     * @throws IOException
     * @throws RepositoryException
     */
    void retrieveOriginalDocument(String line) throws IOException, RepositoryException {
        Logger  logger = Logger.getLogger(this.getClass().toString());
        long start_time = System.currentTimeMillis();
        HttpClientResponseHandler handler = new HttpClientResponseHandler();
        int status = 0;
        try {
            logger.log(Level.INFO, "Retrieving " + line);
            Request.Get(line).connectTimeout(60000).socketTimeout(60000)
                    .execute().handleResponse(handler);
            status = handler.getStatusLine().getStatusCode();
        } catch (ConnectTimeoutException e) {
            //e.printStackTrace();
            status = 408;
        } catch (SocketTimeoutException e) {
            //e.printStackTrace();
            status = 408;
        }
        repo.setOriginalStatus(new URL(line), new Integer(status).toString());
        long duration = (System.currentTimeMillis() - start_time) / 1000;
        logger.log(Level.INFO, "    status " + status + " (" + duration + " seconds)");
        if (status == HttpStatus.SC_OK) {
            repo.setOriginalContent(new URL(line), handler.getContent());
        }
    }
}
