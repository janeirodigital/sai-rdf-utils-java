package com.janeirodigital.sai.rdfutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Custom exception thrown when something cannot be found in an RDF graph
 */
@Getter @AllArgsConstructor
public class SaiRdfNotFoundException extends Exception {
    private final String message;
}
