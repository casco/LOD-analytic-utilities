package collector;

import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import javax.jcr.RepositoryException;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Created by alejandrofernandez on 5/28/14.
 */
public class Retriever {

    DocumentRepository repo;

    /**
     * Create a document retriever and set its local document repository to
     * be repoFolder
     * @param repoFolder
     */
    public Retriever(File repoFolder) {
        this.repo = new DocumentRepository(repoFolder);
    }

    /**
     * retrieve all documents in the urls in urlList file, from the start line
     * to the end line. Save the retrieve documents in the repo. Set their http status
     * @param urlList
     * @param start
     * @param end
     * @throws IOException
     * @throws RepositoryException
     */
    public void retrieveAndStore(File urlList, int start, int end) throws IOException, RepositoryException {
        FileInputStream fis = new FileInputStream(urlList);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
        String line;
        int ok = 0;
        int notOk= 0;
        int count = 0;
        repo.startSession();
        while ((line = br.readLine()) != null) {
            count++;

           if ((start <= count) & (count <= end)) {
                System.out.print("" + count + " - " + line);
                Response response = Request.Get(line).execute();
                int status = response.returnResponse().getStatusLine().getStatusCode();
                System.out.println(" (" + status + ")");
                repo.setOriginalStatus(new URL(line), new Integer(status).toString());
                if (status == HttpStatus.SC_OK) {
                    //TODO What if status is no longer OK?
                    response = Request.Get(line).execute();
                    repo.setOriginalContent(new URL(line), response.returnContent().asString());
                    ok++;
                } else {
                    notOk++;
                }
            }

        }
        repo.shutdown();
        br.close();
        System.out.println("Processed: " + ok + notOk + " (" + ok + " ok - " + notOk + " not ok)");
    }

    /**
     * List everithing we have in the repo
     * @throws RepositoryException
     */
    private void listAll() throws RepositoryException {
        repo.startSession();
        int totalBytes = 0;
        Iterator<URL> it = repo.documentsIterator();
        while (it.hasNext()) {
            URL url = it.next();
            int length = repo.getOriginalContent(url).length();
            totalBytes += length;
            System.out.println(url.toString() + " (status:" + repo.getOriginalStatus(url) +
                    " / " + length + " bytes)");
        }
        System.out.println("Downloaded " + totalBytes + " bytes in total");
    }



    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage:\n" +
                    "\tjava -jar retriever.jar repo-path url-list-path from to");
            System.exit(-1);
        }

        String repoPath = args[0];
        String urlListPath = args[1];
        long start = System.currentTimeMillis();

        downloadUrls(new File(repoPath), new File(urlListPath),
                Integer.parseInt(args[2]),Integer.parseInt(args[3]));

        //listEverythingOnTheDB(new File(repoPath));

        long elapsedTimeMillis = System.currentTimeMillis()-start;
        float elapsedTimeMin = elapsedTimeMillis/(60*1000F);
        System.out.println("It took: " + elapsedTimeMin + " minutes");
    }

    private static void downloadUrls(File repoPath, File urlList, int first, int last) {
        try {
            new Retriever(repoPath).retrieveAndStore(urlList, first, last);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private static void listEverythingOnTheDB(File repoPath) {
        try {
            new Retriever(repoPath).listAll();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
}
