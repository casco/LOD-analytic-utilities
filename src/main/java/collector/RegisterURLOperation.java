package collector;

import javax.jcr.RepositoryException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by alejandrofernandez on 6/10/14.
 */
public class RegisterURLOperation extends AbstractRepositoryOperation {

    File urlList;

    public RegisterURLOperation(File repoFolder, File urlList) {
        super(repoFolder);
        this.urlList = urlList;
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        Logger logger = Logger.getLogger(this.getClass().toString());
        try {
            fis = new FileInputStream(urlList);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            String line;

            int count = 0;
            getRepo().startSession();
            long start = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                if (!getRepo().hasOriginalContent(new URL(line))) {
                    getRepo().setOriginalStatus(new URL(line), DocumentRepository.STATUS_MISSING);
                    count ++;
                }
                if ((System.currentTimeMillis() - start) >= 30000) {
                    start = System.currentTimeMillis();
                    logger.log(Level.INFO, "Already registered " + count + " urls");
                }

            }
            getRepo().shutdown();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        new RegisterURLOperation(new File("data/repo"), new File("data/all_urls.txt")).run();
    }
}
