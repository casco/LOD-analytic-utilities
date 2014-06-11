package examples;


import repository.DocumentRepository;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * First hop example. Logs in to a content repository and prints a
 * status message.
 */
public class UsingTheDocumentRepository {

    /**
     * The main entry point of the example application.
     *
     * @param args command line arguments (ignored)
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {

        //Create a repo using Jackrabbit's default configurations at the given folder
        DocumentRepository repo = new DocumentRepository(new File("data/test-repo"));

        //Star a repository session (everything should happen within sessions
        //This repo does not support multiple sessions and  therefore does not support
        //concurrent access - it is a over-simplified wrapper for Jackrabbit.
        repo.startSession();

        //Let's store some content. Everything we save refers to a URL
        //Every time we save something, the modificationDate of the URL is set
        URL exampleUrl = new URL("http://www.nothing.com/index.html");

        //Save a document/string under a given URL
        //URLs are used as keys to store document content and their properties
        //We store the original html document with setOriginalContent() and its semantically
        //extracted version with setExtractedContent()
        repo.setOriginalContent(exampleUrl, "<html>example<html>");

        //get it
        System.out.println(repo.getOriginalContent(exampleUrl));
        System.out.println(repo.getModificationDate(exampleUrl));

        //Save the extracted version of the document available at the given url. Make sure you store all
        //extracted documents using the same serialization mechanism (RDF ,n3, JSON).
        //Indicate the extractor you used
        repo.setExtractedContent(new URL("http://www.nothing.com/index.html"), "object rdfs:type class",
                "http://www.any23.org");

        //get it
        System.out.println(repo.getExtractedContent(exampleUrl));
        System.out.println(repo.getModificationDate(exampleUrl));
        //TODO: create a method to retrieveAndStore the extractor used


        //set a status code for the original document and the extracted document
        //we want to record what happened when we tried to retrieveAndStore it
        repo.setOriginalStatus(exampleUrl, DocumentRepository.STATUS_OK);
        repo.setExtractedStatus(exampleUrl, DocumentRepository.STATUS_404);

        //get them
        System.out.println(repo.getOriginalStatus(exampleUrl));
        System.out.println(repo.getExtractedStatus(exampleUrl));

        //Iterate over all URLs we have set some property to
        Iterator<URL> iterator = repo.documentsIterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }

    }

}
