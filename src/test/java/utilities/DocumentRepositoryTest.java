package utilities;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repository.DocumentRepository;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentRepositoryTest {

    DocumentRepository repository;
    private final File repositoryFolder;

    public DocumentRepositoryTest() {
        repositoryFolder = new File("unit-test-repo");
    }

    @Before
    public void setUp() throws Exception {
        if (repositoryFolder.exists()) {
            FileUtils.deleteDirectory(repositoryFolder);
        }
        assert (! repositoryFolder.exists());
        repository = new DocumentRepository(repositoryFolder);

   }

    @After
    public void tearDown() throws IOException {
       repository.shutdown();
       assert (repositoryFolder.exists());
       FileUtils.deleteDirectory(repositoryFolder);
       assert (! repositoryFolder.exists());
    }

    @Test
    public void testStartSession() throws RepositoryException {
        repository.startSession();
        String thrown = "";

        try {
            repository.startSession();
        } catch (Exception e) {

            if (e instanceof RepositoryException) {
                thrown = e.getMessage();
            }
        }

        assertEquals("Session already started", thrown);
    }

    @Test
    public void testEndSession() throws Exception {
        String thrown = "";
        try {
            repository.endSession();
        } catch (Exception e) {
            if (e instanceof RepositoryException) {
                thrown = e.getMessage();
            }
        }
        assertEquals("There is no session to end", thrown);
        repository.startSession();
        repository.endSession();
    }

    @Test
    public void testStoreOriginalDocument() throws Exception {
        repository.startSession();
        String content = "Un documento corto";
        URL url = new URL("http://mentira.org/");
        repository.setOriginalContent(url, content);
        repository.endSession();
        repository.startSession();
        assertEquals(content, repository.getOriginalContent(url));
        assertTrue(repository.getModificationDate(url) instanceof Calendar);
        repository.endSession();
    }

    @Test
    public void testStoreExtractedDocument() throws Exception {
        repository.startSession();
        URL url = new URL("http://getschema.org/microdata2rdf/examples/example.html");
        String exampleHtml = getURLContentOrNil(url);
        repository.setOriginalContent(url, exampleHtml);
        URL extractorUrl = new URL("http://getschema.org/microdataextractor" +
                "?url=" + URLEncoder.encode(url.toString(), "UTF-8") + "&out=n3");
        String extractedContent = getURLContentOrNil(extractorUrl);
        repository.setExtractedContent(url, extractedContent, "http://getschema.org/microdataextractor");
        assertEquals(extractedContent, repository.getExtractedContent(url));
        repository.endSession();
    }

    @Test
    public void testDocumentIterator() throws RepositoryException, MalformedURLException {
        repository.startSession();
        for (int i = 1; i<=10; i++) {
            repository.setOriginalContent(new URL("http://www.google.com/" + i), "document" + i);
        }
        Iterator<URL> documentsIterator = repository.documentsIterator();

        int i = 0;
        while(documentsIterator.hasNext()) {
            i++;
            URL url = documentsIterator.next();
            assertEquals("http://www.google.com/" + i, url.toString());
            assertEquals("document"+i, repository.getOriginalContent(url));

        }
        assertEquals(10, i);
        repository.endSession();
    }

    @Test
    public void testSetGetOriginalStatus() throws RepositoryException, MalformedURLException {
        repository.startSession();
        URL url = new URL("http://mentira.org/");
        repository.setOriginalStatus(url, DocumentRepository.STATUS_404);
        assertEquals(DocumentRepository.STATUS_404, repository.getOriginalStatus(url));
        repository.endSession();
    }

    @Test
    public void testDocumentsByStatusIterator() throws RepositoryException, MalformedURLException {
        repository.startSession();
        URL url = new URL("http://mentira.org/");
        repository.setOriginalStatus(new URL("http://mentira.org/1"), DocumentRepository.STATUS_MISSING);
        repository.setOriginalStatus(new URL("http://mentira.org/2"), DocumentRepository.STATUS_404);
        repository.setOriginalStatus(new URL("http://mentira.org/3"), DocumentRepository.STATUS_404);
        repository.setOriginalStatus(new URL("http://mentira.org/4"), DocumentRepository.STATUS_TIMED_OUT);
        repository.setOriginalContent(new URL("http://mentira.org/5"),"without status - should be missing");
        int count = 0;
        Iterator<URL> iterator = repository.documentsByStatusIterator(DocumentRepository.STATUS_404);
        while (iterator.hasNext()) {
            count++;
            assertEquals(DocumentRepository.STATUS_404,repository.getOriginalStatus(iterator.next()));
        }
        assertEquals(2,count);
        count = 0;
        iterator = repository.documentsByStatusIterator(DocumentRepository.STATUS_MISSING);
        while (iterator.hasNext()) {
            count++;
            assertEquals(DocumentRepository.STATUS_MISSING,repository.getOriginalStatus(iterator.next()));
        }
        assertEquals(2,count);

        repository.endSession();
    }

    @Test
    public void testSetGetExtractedStatus() throws RepositoryException, MalformedURLException {
        repository.startSession();
        URL url = new URL("http://mentira.org/");
        repository.setExtractedStatus(url, DocumentRepository.STATUS_OK);
        assertEquals(DocumentRepository.STATUS_OK, repository.getExtractedStatus(url));
        repository.endSession();
    }


    private String getURLContentOrNil(URL url) throws IOException {
        return Request.Get(url.toString()).execute().returnContent().asString();
    }
}