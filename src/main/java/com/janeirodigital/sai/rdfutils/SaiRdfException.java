package com.janeirodigital.sai.rdfutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * General exception used to represent issues in rdf processing
 */
@Getter @AllArgsConstructor
public class SaiRdfException extends Exception {
    private final String message;
}
