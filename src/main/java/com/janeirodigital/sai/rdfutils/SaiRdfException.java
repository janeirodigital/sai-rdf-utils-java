package com.janeirodigital.sai.rdfutils;

/**
 * General exception used to represent issues in rdf processing
 */
public class SaiRdfException extends Exception {
    public SaiRdfException(String message, Throwable cause) {
        super(message, cause);
    }
    public SaiRdfException(String message) {
        super(message);
    }
}
