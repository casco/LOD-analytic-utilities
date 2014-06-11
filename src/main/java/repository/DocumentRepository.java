package repository;

import org.apache.jackrabbit.core.TransientRepository;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by alejandrofernandez on 5/28/14.
 */
public  class DocumentRepository {
    private static final String EXTRACTED_CONTENT = "extracted-content";
    private static final String EXTRACTOR = "extractor";
    private static final String UPDATED_ON = "updated-on";
    private static final String ORIGINAL_CONTENT = "content";
    private static final String UTF_8 = "UTF-8";
    private static final String DOCUMENTS_HOME = "documents_home";
    private static final String ORIGINAL_STATUS = "ORIGINAL_STATUS";
    private static final String EXTRACTED_STATUS = "EXTRACTED_STATUS";

    public static final String STATUS_404 = "404";
    public static final String STATUS_OK = "200";
    public static final String STATUS_TIMED_OUT = "408";
    public static final String STATUS_MISSING = "MISSING";

    TransientRepository repository;
    Session session;
    boolean sessionAvailable;

    /**
     * Sets up a repository based on the configuration available in
     * repository.xml in the working directory
     */
    public DocumentRepository(File repoFolder) {
        repository = new TransientRepository(repoFolder);
        sessionAvailable = false;
    }

    /**
     * Starts a repository session. All calls to set or get methods
     * should happen within sessions. Only one session is needed / possible.
     * @throws RepositoryException
     */
    public void startSession() throws RepositoryException {
        if (sessionAvailable) {
            throw new RepositoryException("Session already started");
        }
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        sessionAvailable = true;
    }

    /**
     * Ends the current session - only one session is needed / possible
     * @throws RepositoryException
     */
    public void endSession() throws RepositoryException {
        if (!sessionAvailable) {
             throw new RepositoryException("There is no session to end");
        }
        session.logout();
        sessionAvailable = false;
    }

    /**
     * Closes all open session - not really necessary if you remember
     * to end the session you opened.
     */
    public void shutdown() {
        repository.shutdown();
    }

    /**
     * Stores the string content for the URL. Sets the modification date to "now"
     * @param url The urls thar was used to retrieveAndStore "content"
     * @param content The string content of the document (html)
     * @throws RepositoryException
     */
    public void setOriginalContent(URL url, String content) throws RepositoryException {
        //TODO: what happens if original document exists?
        setStringProperty(url, ORIGINAL_CONTENT, content);
    }


    /**
     * Stores the string content that an extractor gave us for the url.
     * It is up to the caller to get the content of the url, call the extractor
     * and then call this method.
     * It is also up to the caller to make sure the format of the extracted
     * content is known (i.e., rdf, n3, json)
     *
     * @param url The url of the original document that the extractor extracted
     *            to give us extractedContent
     * @param extractedContent
     * @param extractor  URL as a String for the extractor / or any other id
     * @throws RepositoryException
     */
    public void setExtractedContent(URL url, String extractedContent, String extractor) throws RepositoryException {
        setStringProperty(url, EXTRACTED_CONTENT, extractedContent);
        setStringProperty(url, EXTRACTOR, extractedContent);
    }

    /**
     * Returns true if there is original content for the url
     * @param url
     * @return
     * @throws RepositoryException
     */
    public boolean hasOriginalContent(URL url) throws RepositoryException {
        String encodedUrl = encode(url);

        Node root = getDocumentsHomeNode();
        return root.hasNode(encodedUrl);
    }

    /**
     * Gets the string content for the URL (HTML).
     * @param url t
     * @return the string content for the URL (HTML)
     * @throws RepositoryException
     */
    public String getOriginalContent(URL url) throws RepositoryException {
        return getStringProperty(url, ORIGINAL_CONTENT);
    }

    /**
     * Stores the string content that an extractor gave us for the url.
     * @param url
     * @return The string content that an extractor gave us for the url
     * @throws RepositoryException
     */
    public String getExtractedContent(URL url) throws RepositoryException {
        return getStringProperty(url, EXTRACTED_CONTENT);
    }

    /**
     * Returns the date/time (an instance of Calendar) for the last modification
     * of anything related to url (Original content, extracted content, etc.)
     * @param url
     * @return
     * @throws RepositoryException
     */
    public Calendar getModificationDate(URL url) throws RepositoryException {
        if (! sessionAvailable) {
            throw new RepositoryException("No session available");
        }
        Node node = getDocumentNode(url);
        return node.getProperty(UPDATED_ON).getDate();

    }

    /**
     * Sets the http status for the call to retrieveAndStore the original document
     * Just to remember what happened when we tried to retrieveAndStore it.
     * @param url
     * @param status a String (one of the constants)
     * @throws RepositoryException
     */
    public void setOriginalStatus(URL url, String status) throws RepositoryException {
        setStringProperty(url, ORIGINAL_STATUS, status );
   }

    /**
     * Returns the  status for the call to retrieveAndStore the original document
     * @param url
     * @return
     * @throws RepositoryException
     */
    public String getOriginalStatus(URL url) throws RepositoryException {

        String stringProperty = getStringProperty(url, ORIGINAL_STATUS);
        return stringProperty != null ? stringProperty : STATUS_MISSING;
    }

    /**
     * Sets the  status for the call to extract content from the original document
     * Just to remember what happened when we tried to extract it
     * @param url
     * @param status a String (one of the constants)
     * @throws RepositoryException
     */
    public void setExtractedStatus(URL url, String status) throws RepositoryException {
        setStringProperty(url, EXTRACTED_STATUS, status );
   }

    /**
     *  Returns the  status for the call to extract content from the original document
     * @param url
     * @return
     * @throws RepositoryException
     */
    public String getExtractedStatus(URL url) throws RepositoryException {
        return getStringProperty(url, EXTRACTED_STATUS);
    }

    /**
     * Returns an Iterator over the URLs of all documents that had any attribute set (e.g, content,
     * status, etc.)
     * @return  an Iterator over the URLs of all documents that had any attribute set (e.g, content,
     * status, etc.)
     * @throws RepositoryException
     */
    public Iterator<URL> documentsIterator() throws RepositoryException {
        if (! sessionAvailable) {
            throw new RepositoryException("No session available");
        }
        Node documentsHomeNode = getDocumentsHomeNode();
        final NodeIterator nodeIterator = documentsHomeNode.getNodes();

        Iterator<URL> urlIterator = getIteratorWrapper(nodeIterator);
        return urlIterator;
    }


    /**
     * An iterator only for documents with a given status
     * @param status (original document)
     * @return
     * @throws RepositoryException
     */
    public Iterator<URL> documentsByStatusIterator(String status) throws RepositoryException {
        if (! sessionAvailable) {
            throw new RepositoryException("No session available");
        }

        javax.jcr.query.QueryManager queryManager = session.getWorkspace().getQueryManager();
        String selector = "(ORIGINAL_STATUS  ='" + status + "')";
        if (status.equals(STATUS_MISSING)) {
            selector = "((ORIGINAL_STATUS IS NULL) OR (ORIGINAL_STATUS = '" + STATUS_MISSING + "'))";
        }

       String expression = "SELECT * from nt:unstructured\n" +
                "where (jcr:path LIKE '/documents_home/%') AND " + selector ;

        javax.jcr.query.Query query = queryManager.createQuery(expression, Query.SQL);
        QueryResult result = query.execute();

        final NodeIterator nodeIterator = result.getNodes();

        Iterator<URL> urlIterator = getIteratorWrapper(nodeIterator);
        return urlIterator;
    }

    private Iterator<URL> getIteratorWrapper(final NodeIterator nodeIterator) {
        return new Iterator<URL>() {
            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public URL next() {
                Node node = nodeIterator.nextNode();
                try {
                    return new URL(URLDecoder.decode(node.getName(), UTF_8));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void remove() {
                nodeIterator.remove();
            }
        };
    }


    private void setStringProperty(URL url, String property, String content) throws RepositoryException {
        if (! sessionAvailable) {
            throw new RepositoryException("No session available");
        }
        Node docNode = getOrCreateDocumentNode(url);
        docNode.setProperty(property, content);
        docNode.setProperty(UPDATED_ON, Calendar.getInstance());
        session.save();
    }


    private String getStringProperty(URL url, String property) throws RepositoryException {
        if (! sessionAvailable) {
            throw new RepositoryException("No session available");
        }
        Node node = getDocumentNode(url);
        if (node.hasProperty(property)) {
            return node.getProperty(property).getString();
        } else {
            return null;
        }

    }

    private Node getDocumentNode(URL url) throws RepositoryException {
        String encodedUrl = encode(url);
        Node documentsHomeNode = getDocumentsHomeNode();
        return documentsHomeNode.getNode(encodedUrl);
    }

    private Node getOrCreateDocumentNode(URL url) throws RepositoryException {
        String encodedUrl = encode(url);
        Node documentsHomeNode = getDocumentsHomeNode();
        Node docNode = null;
        if (documentsHomeNode.hasNode(encodedUrl)) {
            docNode = documentsHomeNode.getNode(encodedUrl);
        }  else {
            docNode = documentsHomeNode.addNode(encodedUrl);
        }
        return docNode;
    }

    private Node getDocumentsHomeNode() throws RepositoryException {
        Node root = session.getRootNode();
        if (!root.hasNode(DOCUMENTS_HOME))    {
            root.addNode(DOCUMENTS_HOME);
        }
        return root.getNode(DOCUMENTS_HOME);
    }




    private String encode(URL url) throws RepositoryException {
        String encodedUrl = null;
        try {
            encodedUrl = URLEncoder.encode(url.toString(), UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Problems encoding...");
        }
        return encodedUrl;
    }
}
