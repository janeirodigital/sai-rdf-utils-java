package com.janeirodigital.sai.rdfutils;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.jena.datatypes.xsd.XSDDatatype.*;

public class RdfUtils {

    public static final String TEXT_TURTLE = "text/turtle";
    public static final String LD_JSON = "application/ld+json";
    public static final String RDF_XML = "application/rdf+xml";
    public static final String N_TRIPLES = "application/n-triples";

    private RdfUtils() { }

    /**
     * Deserializes the provided String <code>rawContent</code> into a Jena Model
     * @param baseUri Base URI to use for statements
     * @param rawContent String of RDF
     * @param contentType Content type of content
     * @return Deserialized Jean Model
     * @throws SaiRdfException
     */
    public static Model getModelFromString(URI baseUri, String rawContent, String contentType) throws SaiRdfException {
        Objects.requireNonNull(baseUri, "Must provide a base URI to generate a model");
        Objects.requireNonNull(rawContent, "Must provide content to generate a model from");
        Objects.requireNonNull(contentType, "Must provide content type for model generation");
        try {
            Model model = ModelFactory.createDefaultModel();
            StringReader reader = new StringReader(rawContent);
            RDFDataMgr.read(model.getGraph(), reader, baseUri.toString(), RdfUtils.getLangForContentType(contentType));
            return model;
        } catch (RiotException ex) {
            throw new SaiRdfException("Error processing input string", ex);
        }
    }

    /**
     * Deserializes the contents of the provided <code>filePath</code> into a Jena Model.
     * @param baseUri Base URI to use for statements
     * @param filePath Path to file containing input data
     * @param contentType Content type of file data
     * @return Deserialized Jena Model
     * @throws SaiRdfException
     * @throws IOException
     */
    public static Model getModelFromFile(URI baseUri, String filePath, String contentType) throws SaiRdfException, IOException {
        Objects.requireNonNull(baseUri, "Must provide a baseUri to generate a model");
        Objects.requireNonNull(filePath, "Must provide an input file path to provide data for the generated model");
        Objects.requireNonNull(contentType, "Must provide content type for model generation");
        InputStream in = null;
        try {
            Model model = ModelFactory.createDefaultModel();
            in = RDFDataMgr.open(filePath);
            model.read(in, baseUri.toString(), contentType);
            return model;
        } catch (RiotException ex) {
            throw new SaiRdfException("Error processing input from file " + filePath, ex);
        } finally {
            if (in != null) { in.close(); }
        }
    }

    /**
     * Get a String of the provided <code>model</code> serialized in <code>lang</code>.
     * @param model Jena Model to serialize
     * @param lang Format to serialize into
     * @return Serialized string of the provided model
     */
    public static String getStringFromRdfModel(Model model, Lang lang) {
        Objects.requireNonNull(model, "Cannot serialize a null model");
        Objects.requireNonNull(lang, "Must provide a serialization format");
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, model, lang);
        return sw.toString();
    }

    /**
     * Get a String of the provided <code>model</code> serialized in JSON-LD
     * @param model Jena Model to serialize
     * @return Serialized JSON-LD string of the provided model
     */
    public static String getJsonLdStringFromModel(Model model, String jsonLdContext) throws SaiRdfException {
        Objects.requireNonNull(model, "Cannot serialize a null model");
        String quads = getStringFromRdfModel(model, Lang.NQUADS);
        InputStream inputQuads = new ByteArrayInputStream(quads.getBytes());
        String jsonLdString;
        try {
            Document docQuads = RdfDocument.of(inputQuads);
            JsonArray roughJsonLd = JsonLd.fromRdf(docQuads).get();
            Document roughJsonLdDocument = JsonDocument.of(roughJsonLd);
            jsonLdString = roughJsonLd.toString();
            if (jsonLdContext != null && !jsonLdContext.isEmpty()) {
                InputStream inputContext = new ByteArrayInputStream(jsonLdContext.getBytes());
                Document docContext = JsonDocument.of(inputContext);
                JsonObject compacted = JsonLd.compact(roughJsonLdDocument, docContext).compactToRelative(false).get();
                jsonLdString = compacted.toString();
            }
        } catch (JsonLdError ex) {
            throw new SaiRdfException("Failed to serialize resource to JSON-LD", ex);
        }
        return jsonLdString;
    }

    /**
     * Returns a jena Resource at the specified <code>resourceUri</code> from the provided jena Model
     * @param model Model to search
     * @param resourceUri URI of the resource to search for
     * @return Jena Resource at resourceUri
     */
    public static Resource getResourceFromModel(Model model, URI resourceUri) {
        Objects.requireNonNull(model, "Must provide a model to get a resource from it");
        Objects.requireNonNull(resourceUri, "Must provide resource to get from model");
        return model.getResource(resourceUri.toString());
    }

    /**
     * Gets a new Jena Resource (and associated Model) for the provided <code>resourceUri</code>
     * and adds a statement identifying the resource as the provided RDF <code>type</code>.
     * @param resourceUri URI of the resource
     * @return Resource
     */
    public static Resource getNewResourceForType(URI resourceUri, String type) {
        Resource resource = getNewResource(resourceUri);
        resource.addProperty(RDF.type, type);
        return resource;
    }

    /**
     * Gets a new Jena Resource from the supplied model for the provided <code>resourceUri</code>
     * and adds a statement identifying the resource as the provided RDF <code>type</code>.
     * @param dataset model to get resource from
     * @param resourceUri URI of the resource
     * @return Resource
     */
    public static Resource getNewResourceForType(Model dataset, URI resourceUri, String type) {
        Resource resource = getNewResource(dataset, resourceUri);
        resource.addProperty(RDF.type, type);
        return resource;
    }

    /**
     * Gets a new Jena Resource (and associated Model) for the provided <code>resourceUri</code>
     * and adds a statement identifying the resource as the provided RDF <code>type</code>.
     * @param resourceUri URI of the resource
     * @param type RDF type
     * @return Resource
     */
    public static Resource getNewResourceForType(URI resourceUri, RDFNode type) {
        Resource resource = getNewResource(resourceUri);
        resource.addProperty(RDF.type, type);
        return resource;
    }

    /**
     * Gets a new Jena Resource from the supplied model for the provided <code>resourceUri</code>
     * and adds a statement identifying the resource as the provided RDF <code>type</code>.
     * @param dataset Model to get resource from
     * @param resourceUri URI of the resource
     * @param type RDF type
     * @return Resource
     */
    public static Resource getNewResourceForType(Model dataset, URI resourceUri, RDFNode type) {
        Resource resource = getNewResource(dataset, resourceUri);
        resource.addProperty(RDF.type, type);
        return resource;
    }

    /**
     * Gets a new Jena Resource (and associated Model) for the provided <code>resourceUri</code>
     * @param resourceUri URI of the resource
     * @return Resource
     */
    public static Resource getNewResource(URI resourceUri) {
        Model model = ModelFactory.createDefaultModel();
        return model.createResource(resourceUri.toString());
    }

    /**
     * Gets a new Jena Resource from the supplied model for the provided <code>resourceUri</code>
     * @param resourceUri URI of the resource
     * @param dataset Model to get resource from
     * @return Resource
     */
    public static Resource getNewResource(Model dataset, URI resourceUri) {
        return dataset.createResource(resourceUri.toString());
    }

    /**
     * Returns a single Jena Statement matching the provided <code>property</code> in
     * the provided <code>resource</code>. When nothing is found null is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena Statement matching the provided Property or null
     */
    public static Statement getStatement(Resource resource, Property property) {
        Objects.requireNonNull(resource, "Cannot get a statement from a null resource");
        Objects.requireNonNull(property, "Cannot get a statement from a resource with a null property");
        return resource.getProperty(property);
    }

    /**
     * Returns a single Jena Statement matching the provided <code>property</code> in
     * the provided <code>resource</code>. If nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena Statement matching the provided Property
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static Statement getRequiredStatement(Resource resource, Property property) throws SaiRdfNotFoundException {
        Statement statement = getStatement(resource, property);
        if (statement == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property)); }
        return statement;
    }

    /**
     * Returns a single Jena RDFNode matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found null is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena RDFNode matching the provided property or null
     */
    public static RDFNode getObject(Resource resource, Property property) {
        Statement statement = getStatement(resource, property);
        if (statement == null) { return null; }
        return statement.getObject();
    }

    /**
     * Returns a single Jena RDFNode matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena RDFNode matching the provided property
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static RDFNode getRequiredObject(Resource resource, Property property) throws SaiRdfNotFoundException {
        return getRequiredStatement(resource, property).getObject();
    }

    /**
     * Returns a list of Jena RDFNodes matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Jena RDFNodes matching the provided property (possibly empty)
     */
    public static List<RDFNode> getObjects(Resource resource, Property property) {
        Objects.requireNonNull(resource, "Cannot get objects from a null resource");
        Objects.requireNonNull(property, "Cannot get objects from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<RDFNode> objects = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            objects.add(statement.getObject());
        }
        return objects;
    }

    /**
     * Returns a list of Jena RDFNodes matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Jena RDFNodes matching the provided property
     */
    public static List<RDFNode> getRequiredObjects(Resource resource, Property property) throws SaiRdfNotFoundException {
        List<RDFNode> objects = getObjects(resource, property);
        if (objects.isEmpty()) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property)); }
        return objects;
    }

    /**
     * Returns a list of URIs matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of URI object values matching the provided property (possibly empty)
     */
    public static List<URI> getUriObjects(Resource resource, Property property) throws SaiRdfException {
        Objects.requireNonNull(resource, "Cannot get URIs from a null resource");
        Objects.requireNonNull(property, "Cannot get URIs from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<URI> uris = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            RDFNode object = statement.getObject();
            if (!object.isResource()) { throw new SaiRdfException(msgNotUriResource(resource, property, object)); }
            uris.add(nodeToUri(object));
        }
        return uris;
    }

    /**
     * Returns a list of URIs matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of URIs matching the provided property
     */
    public static List<URI> getRequiredUriObjects(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        List<URI> uris = getUriObjects(resource, property);
        if (uris.isEmpty()) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property)); }
        return uris;
    }

    /**
     * Returns a list of Strings matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of String object values matching the provided property (possibly empty)
     */
    public static List<String> getStringObjects(Resource resource, Property property) throws SaiRdfException {
        Objects.requireNonNull(resource, "Cannot get strings from a null resource");
        Objects.requireNonNull(property, "Cannot get strings from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<String> strings = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            RDFNode object = statement.getObject();
            if (!object.isLiteral()) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDstring)); }
            if (!object.asLiteral().getDatatype().equals(XSDstring)) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDstring)); }
            strings.add(object.asLiteral().getString());
        }
        return strings;
    }

    /**
     * Returns a list of Strings matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Strings matching the provided property
     */
    public static List<String> getRequiredStringObjects(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        List<String> strings = getStringObjects(resource, property);
        if (strings.isEmpty()) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property)); }
        return strings;
    }

    /**
     * Returns a single URI value from the object of the statement matching the provided
     * <code>property</code> in the provided <code>resource</code>. Returns null when
     * no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return URI object value or null
     * @throws SaiRdfException
     */
    public static URI getUriObject(Resource resource, Property property) throws SaiRdfException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isResource()) { throw new SaiRdfException(msgNotUriResource(resource, property, object)); }
        return nodeToUri(object);
    }

    /**
     * Returns a single URI value from the object of the statement matching the provided
     * <code>property</code> in the provided <code>resource</code>. Throws an exception
     * when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return URI object value
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static URI getRequiredUriObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        URI uri = getUriObject(resource, property);
        if (uri == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property)); }
        return uri;
    }

    /**
     * Returns a single literal value as String from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns
     * null when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as String or null
     * @throws SaiRdfException
     */
    public static String getStringObject(Resource resource, Property property) throws SaiRdfException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDstring)); }
        if (!object.asLiteral().getDatatype().equals(XSDstring)) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDstring)); }
        return object.asLiteral().getString();
    }

    /**
     * Returns a single literal value as String from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as String
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static String getRequiredStringObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        String string = getStringObject(resource, property);
        if (string == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property, XSDstring)); }
        return string;
    }

    /**
     * Returns a single literal value as Integer from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns
     * null when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Integer or null
     * @throws SaiRdfException
     */
    public static Integer getIntegerObject(Resource resource, Property property) throws SaiRdfException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDinteger)); }
        if (!object.asLiteral().getDatatype().equals(XSDinteger)) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDinteger)); }
        return object.asLiteral().getInt();
    }

    /**
     * Returns a single literal value as Integer from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws
     * an exception when no match is found
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Integer
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static Integer getRequiredIntegerObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        Integer i = getIntegerObject(resource, property);
        if (i == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property, XSDinteger)); }
        return i;
    }

    /**
     * Returns a single literal value as OffsetDateTime from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns null
     * when no match is found
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as OffsetDateTime or null
     * @throws SaiRdfException
     */
    public static OffsetDateTime getDateTimeObject(Resource resource, Property property) throws SaiRdfException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDdateTime)); }
        if (!object.asLiteral().getDatatype().equals(XSDdateTime)) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDdateTime)); }
        return OffsetDateTime.parse(object.asLiteral().getString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * Returns a single literal value as OffsetDateTime from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as OffsetDateTime
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static OffsetDateTime getRequiredDateTimeObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        OffsetDateTime dateTime = getDateTimeObject(resource, property);
        if (dateTime == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property, XSDdateTime)); }
        return dateTime;
    }

    /**
     * Returns a single literal value as Boolean from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns null
     * when no match is found, which requires a Boxed boolean value to be returned.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Boolean
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static Boolean getBooleanObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDboolean)); }
        if (!object.asLiteral().getDatatype().equals(XSDboolean)) { throw new SaiRdfException(msgInvalidDataType(resource, property, XSDboolean)); }
        return object.asLiteral().getBoolean();
    }

    /**
     * Returns a single literal value as Boolean from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Boolean
     * @throws SaiRdfException
     * @throws SaiRdfNotFoundException when nothing is found
     */
    public static Boolean getRequiredBooleanObject(Resource resource, Property property) throws SaiRdfException, SaiRdfNotFoundException {
        Boolean booleanValue = getBooleanObject(resource, property);
        if (booleanValue == null) { throw new SaiRdfNotFoundException(msgNothingFound(resource, property, XSDboolean)); }
        return booleanValue;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the RDFNode <code>object</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param object RDFNode to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, RDFNode object) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null object");

        resource.removeAll(property);
        resource.addProperty(property, object);
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the String literal <code>string</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param string String literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, String string) {
        Objects.requireNonNull(string, "Cannot update a resource by passing a null string");
        Node node = NodeFactory.createLiteral(string);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the URI <code>uri</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param uri URI to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, URI uri) {
        Objects.requireNonNull(uri, "Cannot update a resource by passing a null uri");
        Node node = NodeFactory.createURI(uri.toString());
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the xsd:dateTime provided via <code>dateTime</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param dateTime String literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, OffsetDateTime dateTime) {
        Objects.requireNonNull(dateTime, "Cannot update a resource by passing a null date time value");
        Node node = NodeFactory.createLiteralByValue(dateTime, XSDdateTime);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the integer provided via <code>integer</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param integer integer to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, int integer) {
        Node node = NodeFactory.createLiteralByValue(integer, XSDinteger);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the boolean provided via <code>bool</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param bool boolean literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, boolean bool) {
        Node node = NodeFactory.createLiteralByValue(bool, XSDboolean);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the list of RDF Nodes provided via <code>objects</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param objects List of RDFNodes to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObjects(Resource resource, Property property, List<RDFNode> objects) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(objects, "Cannot update a resource by passing a null list");
        resource.removeAll(property);
        for (RDFNode object : objects) { resource.addProperty(property, object); }
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the list of URIs provided via <code>uris</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param uris List of URIs to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateUriObjects(Resource resource, Property property, List<URI> uris) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(uris, "Cannot update a resource by passing a null list");
        resource.removeAll(property);
        for (URI uri : uris) {
            Node node = NodeFactory.createURI(uri.toString());
            resource.addProperty(property, resource.getModel().asRDFNode(node));
        }
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the list of Strings provided via <code>strings</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param strings List of Strings to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateStringObjects(Resource resource, Property property, List<String> strings) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(strings, "Cannot update a resource by passing a null list");
        resource.removeAll(property);
        for (String string : strings) {
            Node node = NodeFactory.createLiteral(string);
            resource.addProperty(property, resource.getModel().asRDFNode(node));
        }
        return resource;
    }

    /**
     * Convert an RDFNode value to URI
     * @param node RDFNode to convert
     * @return Converted URI
     * @throws SaiRdfException
     */
    public static URI nodeToUri(RDFNode node) throws SaiRdfException {
        Objects.requireNonNull(node, "Cannot convert a null node to URI");
        if (!node.isResource()) { throw new SaiRdfException("Cannot convert literal node to URI"); }
        try {
            return new URI(node.asResource().getURI()).parseServerAuthority();
        } catch (URISyntaxException ex) {
            throw new SaiRdfException("Failed to convert node to URI - " + node.asResource().getURI(), ex);
        }
    }

    /**
     * Determine the Jena language (graph serialization type) based on a content type string
     * @param contentType Content type string
     * @return Serialization language
     */
    public static Lang getLangForContentType(String contentType) {
        if (contentType == null) {
            return Lang.TURTLE;
        }
        switch (contentType) {
            case LD_JSON:
                return Lang.JSONLD11;
            case RDF_XML:
                return Lang.RDFXML;
            case N_TRIPLES:
                return Lang.NTRIPLES;
            default:
                return Lang.TURTLE;
        }
    }

    public static String buildRemoteJsonLdContext(String remote) {
        Objects.requireNonNull(remote, "Must provide remote JSON-LD context to build");
        return "{\n  \"@context\": \"" + remote + "\"\n}";
    }

    public static String buildRemoteJsonLdContexts(List<String> contexts) throws SaiRdfException {
        Objects.requireNonNull(contexts, "Must provide JSON-LD contexts to build");
        if (contexts.isEmpty()) { throw new SaiRdfException("Cannot build JSON-LD context with no input"); }
        StringBuilder combined = new StringBuilder();
        combined.append("{\n \"@context\": [\n");
        for (int i=0; i<contexts.size(); i++) {
            combined.append("    \"" + contexts.get(i) + "\"");
            String commandEnd = (i == (contexts.size() - 1)) ? "\n" : ",\n";
            combined.append(commandEnd);
        }
        combined.append("  ]\n }");
        return combined.toString();
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgInvalidDataType(Resource resource, Property property, RDFDatatype type) {
        return "Excepted literal value of type " + type.toString() + "for " + resource.getURI() + " -- " + property.getURI();
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgNothingFound(Resource resource, Property property, RDFDatatype type) {
        return "Nothing found for " + resource.getURI() + " -- " + property.getURI() + " of type " + type.toString();
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgNothingFound(Resource resource, Property property) {
        return "Nothing found for " + resource.getURI() + " -- " + property.getURI();
    }

    /**
     * Convenience function for common condition when an object type isn't a URI resource
     */
    private static String msgNotUriResource(Resource resource, Property property, RDFNode object) {
        return "Expected non-literal value for object at " + resource.getURI() + " -- " + property.getURI() + " -- " + object;
    }

}
