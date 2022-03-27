package com.janeirodigital.sai.rdfutils;

/**
 * Custom exception thrown when something cannot be found in an RDF graph
 */
public class SaiRdfNotFoundException extends Exception {
    public SaiRdfNotFoundException(String message) {
        super(message);
    }
}
