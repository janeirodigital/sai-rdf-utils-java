package com.janeirodigital.sai.rdfutils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.rdfutils.RdfUtils.*;
import static com.janeirodigital.sai.rdfutils.TestableVocabulary.TESTABLE_MILESTONE;
import static com.janeirodigital.sai.rdfutils.TestableVocabulary.TESTABLE_PROJECT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RdfUtilsTests {

    private static final String SOLID_OIDC_CONTEXT = "https://www.w3.org/ns/solid/oidc-context.jsonld";
    private static final String INTEROP_CONTEXT = "https://solid.github.io/data-interoperability-panel/specification/interop.jsonld";

    private static URI resourceUri;
    private static URI additionalUri;
    private static String resourcePath;
    private static String invalidResourcePath;
    private static Model readableModel;
    private static Resource readableResource;
    private static Model updatableModel;
    private static Resource updatableResource;

    private static List<URI> READABLE_TAGS;
    private static List<String> READABLE_COMMENTS;
    private static URI READABLE_MILESTONE;
    private static boolean READABLE_ACTIVE;
    private static OffsetDateTime READABLE_CREATED_AT;
    private static String READABLE_NAME;
    private static int READABLE_ID;

    @BeforeAll
    static void beforeAll() throws SaiRdfException {
        resourceUri = URI.create("https://data.example/resource#project");
        additionalUri = URI.create("https://data.example/resource#milestone");
        resourcePath = "rdf-resource.ttl";
        invalidResourcePath = "invalid-rdf-resource.ttl";
        readableModel = getModelFromString(resourceUri, getRdfResourceBody(), TEXT_TURTLE);
        readableResource = getResourceFromModel(readableModel, resourceUri);
        READABLE_TAGS = Arrays.asList(URI.create("https://data.example/tags/tag-1"),
                URI.create("https://data.example/tags/tag-2"),
                URI.create("https://data.example/tags/tag-3"));
        READABLE_COMMENTS = Arrays.asList("First original comment" ,
                "Second original comment" ,
                "Third original comment");
        READABLE_MILESTONE = URI.create("https://data.example/data/projects/project-1/milestone-3/#milestone");
        READABLE_ACTIVE = true;
        READABLE_CREATED_AT = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        READABLE_NAME = "Great Validations";
        READABLE_ID = 6;
    }

    @BeforeEach
    void beforeEach() throws SaiRdfException {
        updatableModel = getModelFromString(resourceUri, getRdfResourceBody(), TEXT_TURTLE);
        updatableResource = getResourceFromModel(updatableModel, resourceUri);
    }

    @Test
    @DisplayName("Get RDF model from string")
    void checkGetModelFromString() throws SaiRdfException {
        Model model = getModelFromString(resourceUri, getRdfResourceBody(), TEXT_TURTLE);
        assertNotNull(model);
        assertNotNull(model.getResource(resourceUri.toString()));
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid string")
    void failToGetModelFromInvalidString() {
        assertThrows(SaiRdfException.class, () -> {
            getModelFromString(resourceUri, getInvalidRdfResourceBody(), TEXT_TURTLE);
        });
    }

    @Test
    @DisplayName("Get RDF model from file")
    void checkGetModelFromFile() throws SaiRdfException, IOException {
        Model model = getModelFromFile(resourceUri, resourcePath, TEXT_TURTLE);
        assertNotNull(model);
        assertNotNull(model.getResource(resourceUri.toString()));
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid file")
    void failToGetModelFromInvalidFile() {
        assertThrows(SaiRdfException.class, () -> {
            getModelFromFile(resourceUri, invalidResourcePath, TEXT_TURTLE);
        });
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid file with null stream")
    void failToGetModelFromInvalidFileNull() {
        try (MockedStatic<RDFDataMgr> mockMgr = Mockito.mockStatic(RDFDataMgr.class)) {
            mockMgr.when(() -> RDFDataMgr.open(anyString())).thenReturn(null);
            Model mockModel = mock(Model.class);
            when(mockModel.read(any(InputStream.class), anyString(), anyString())).thenThrow(RiotException.class);
            assertDoesNotThrow(() -> getModelFromFile(resourceUri, invalidResourcePath, TEXT_TURTLE));
        }
    }

    @Test
    @DisplayName("Get resource from RDF model")
    void checkGetResourceFromModel() {
        Resource resource = getResourceFromModel(readableModel, resourceUri);
        assertNotNull(resource);
        assertEquals(resourceUri.toString(), resource.getURI());
    }


    @Test
    @DisplayName("Get new resource for RDF Type as String")
    void checkGetNewResourceForTypeString() {
        Resource resource = getNewResourceForType(resourceUri, TESTABLE_PROJECT.toString());
        assertNotNull(resource);
        assertNotNull(getObject(resource, RDF.type));
    }

    @Test
    @DisplayName("Get new resource for RDF Type as Node")
    void checkGetNewResourceForTypeNode() {
        Resource resource = getNewResourceForType(resourceUri, TESTABLE_PROJECT);
        assertNotNull(resource);
        assertNotNull(getObject(resource, RDF.type));
    }

    @Test
    @DisplayName("Get new resource from existing model for RDF Type as String")
    void checkGetNewResourceFromExistingModelForTypeString() {
        Resource resource = getNewResourceForType(resourceUri, TESTABLE_PROJECT.toString());
        assertNotNull(resource);
        assertNotNull(getObject(resource, RDF.type));
        Model existingDataset = resource.getModel();
        Resource additionalResource = getNewResourceForType(existingDataset, additionalUri, TESTABLE_MILESTONE.toString());
        assertNotNull(additionalResource);
        assertNotNull(getObject(additionalResource, RDF.type));
    }

    @Test
    @DisplayName("Get new resource from existing model for RDF Type as Node")
    void checkGetNewResourceFromExistingModelForTypeNode() {
        Resource resource = getNewResourceForType(resourceUri, TESTABLE_PROJECT);
        assertNotNull(resource);
        assertNotNull(getObject(resource, RDF.type));
        Model existingDataset = resource.getModel();
        Resource additionalResource = getNewResourceForType(existingDataset, additionalUri, TESTABLE_MILESTONE);
        assertNotNull(additionalResource);
        assertNotNull(getObject(additionalResource, RDF.type));
    }

    @Test
    @DisplayName("Get statement from resource by property")
    void checkGetStatement() {
        Statement statement = getStatement(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(statement);
        assertEquals(READABLE_NAME, statement.getObject().asLiteral().getString());

        Statement missing = getStatement(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required statement from resource by property")
    void checkGetRequiredStatement() throws SaiRdfNotFoundException {
        Statement statement = getRequiredStatement(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(statement);
        assertEquals(READABLE_NAME, statement.getObject().asLiteral().getString());

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredStatement(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });

        try {
            getRequiredStatement(readableResource, TestableVocabulary.TESTABLE_MISSING);
        } catch (SaiRdfNotFoundException ex) {
            assertNotNull(ex.getMessage());
        }

    }

    @Test
    @DisplayName("Serialize RDF model to string")
    void checkGetStringFromRdfModel() throws SaiRdfException {
        String serialized = getStringFromRdfModel(readableModel, getLangForContentType(TEXT_TURTLE));
        Model comparableModel = getModelFromString(resourceUri, serialized, TEXT_TURTLE);
        Model difference = comparableModel.difference(readableModel);
        assertTrue(difference.isEmpty());
    }

    @Test
    @DisplayName("Serialize RDF model to JSON-LD string with context")
    void checkGetJsonLdStringFromRdfModel() throws SaiRdfException {
        String serialized = getJsonLdStringFromModel(readableModel, buildRemoteJsonLdContext(INTEROP_CONTEXT));
        assertNotNull(serialized);
    }

    @Test
    @DisplayName("Serialize RDF model to JSON-LD string without context")
    void checkGetJsonLdStringFromRdfModelNoContext() throws SaiRdfException {
        String serialized = getJsonLdStringFromModel(readableModel, null);
        assertNotNull(serialized);
    }

    @Test
    @DisplayName("Serialize RDF model to JSON-LD string with empty context")
    void checkGetJsonLdStringFromRdfModelEmptyContext() throws SaiRdfException {
        String serialized = getJsonLdStringFromModel(readableModel, "");
        assertNotNull(serialized);
    }

    @Test
    @DisplayName("Fail to serialize RDF model to JSON-LD string - invalid JSON-LD")
    void failToGetJsonLdStringFromRdfModelInvalid() {
        assertThrows(SaiRdfException.class, () -> getJsonLdStringFromModel(readableModel, getInvalidJsonLdContext()));
        try {
            getJsonLdStringFromModel(readableModel, getInvalidJsonLdContext());
        } catch (SaiRdfException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    @DisplayName("Fail to build remote JSON-LD contexts - empty arguments")
    void failToBuildRemoteContexts() {
        List<String> contexts = new ArrayList<>();
        assertThrows(SaiRdfException.class, () -> buildRemoteJsonLdContexts(contexts));
    }

    @Test
    @DisplayName("Build remote JSON-LD context - multiple contexts")
    void buildMultipleRemoteJsonLdContexts() {
        List<String> contexts = new ArrayList<>();
        contexts.addAll(Arrays.asList(INTEROP_CONTEXT, SOLID_OIDC_CONTEXT));
        assertDoesNotThrow(() -> buildRemoteJsonLdContexts(contexts));
    }

    @Test
    @DisplayName("Get an object from resource by property")
    void checkGetObject() {
        RDFNode object = getObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(6, object.asLiteral().getInt());

        RDFNode missing = getObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get a required object from resource by property")
    void checkGetRequiredObject() throws SaiRdfNotFoundException {
        RDFNode object = getRequiredObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(6, object.asLiteral().getInt());

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });

    }

    @Test
    @DisplayName("Get list of objects from resource by property")
    void checkGetObjects() {
        List<RDFNode> objects = getObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());

        List<RDFNode> missing = getObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required objects from resource by property")
    void checkGetRequiredObjects() throws SaiRdfNotFoundException {
        List<RDFNode> objects = getRequiredObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get list of URI objects from resource by property")
    void checkGetUriObjects() throws SaiRdfException {
        List<URI> objects = getUriObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_TAGS));

        assertThrows(SaiRdfException.class, () -> {
            getRequiredUriObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        });

        List<URI> missing = getUriObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required URI objects from resource by property")
    void checkGetRequiredUriObjects() throws SaiRdfNotFoundException, SaiRdfException {
        List<URI> objects = getRequiredUriObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_TAGS));

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredUriObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get list of String objects from resource by property")
    void checkGetStringObjects() throws SaiRdfException {
        List<String> objects = getStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_COMMENTS));

        assertThrows(SaiRdfException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        });

        assertThrows(SaiRdfException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertThrows(SaiRdfException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        List<String> missing = getStringObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required String objects from resource by property")
    void checkGetRequiredStringObjects() throws SaiRdfNotFoundException, SaiRdfException {
        List<String> objects = getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_COMMENTS));

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get URI object from resource by property")
    void checkGetUriObject() throws SaiRdfException {
        URI object = getUriObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertNotNull(object);
        assertEquals(READABLE_MILESTONE, object);

        assertThrows(SaiRdfException.class, () -> {
            getUriObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        URI missing = getUriObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required URI object from resource by property")
    void checkGetRequiredUriObject() throws SaiRdfNotFoundException, SaiRdfException {
        URI object = getRequiredUriObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertNotNull(object);
        assertEquals(READABLE_MILESTONE, object);

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredUriObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get String object from resource by property")
    void checkGetStringObject() throws SaiRdfException {
        String object = getStringObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(object);
        assertEquals(READABLE_NAME, object);

        assertThrows(SaiRdfException.class, () -> {
            getStringObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiRdfException.class, () -> {
            getStringObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        String missing = getStringObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required String object from resource by property")
    void checkGetRequiredStringObject() throws SaiRdfNotFoundException, SaiRdfException {
        String object = getRequiredStringObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(object);
        assertEquals(READABLE_NAME, object);

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredStringObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get Integer object from resource by property")
    void checkGetIntegerObject() throws SaiRdfException {
        Integer object = getIntegerObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(READABLE_ID, object);

        assertThrows(SaiRdfException.class, () -> {
            getIntegerObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiRdfException.class, () -> {
            getIntegerObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        Integer missing = getIntegerObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required Integer object from resource by property")
    void checkGetRequiredIntegerObject() throws SaiRdfNotFoundException, SaiRdfException {
        Integer object = getRequiredIntegerObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(READABLE_ID, object);

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredIntegerObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get DateTime object from resource by property")
    void checkGetDateTimeObject() throws SaiRdfException {
        OffsetDateTime object = getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        assertNotNull(object);
        assertEquals(READABLE_CREATED_AT, object);

        assertThrows(SaiRdfException.class, () -> {
            getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_ID);
        });

        assertThrows(SaiRdfException.class, () -> {
            getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        OffsetDateTime missing = getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required DateTime object from resource by property")
    void checkGetRequiredDateTimeObject() throws SaiRdfNotFoundException, SaiRdfException {
        OffsetDateTime object = getRequiredDateTimeObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        assertNotNull(object);
        assertEquals(READABLE_CREATED_AT, object);

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredDateTimeObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get Boolean object from resource by property")
    void checkGetBooleanObject() throws SaiRdfNotFoundException, SaiRdfException {
        Boolean object = getBooleanObject(readableResource, TestableVocabulary.TESTABLE_ACTIVE);
        assertEquals(READABLE_ACTIVE, object);

        assertThrows(SaiRdfException.class, () -> {
            getBooleanObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiRdfException.class, () -> {
            getBooleanObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertNull(getBooleanObject(readableResource, TestableVocabulary.TESTABLE_MISSING));
    }

    @Test
    @DisplayName("Get required Boolean object from resource by property")
    void checkGetRequiredBooleanObject() throws SaiRdfNotFoundException, SaiRdfException {
        boolean object = getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_ACTIVE);
        assertEquals(READABLE_ACTIVE, object);

        assertThrows(SaiRdfException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiRdfException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertThrows(SaiRdfNotFoundException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Update RDF node object by property")
    void checkUpdateNodeObject() throws SaiRdfException {
        URI url = URI.create("https://solidproject.org");
        Node node = NodeFactory.createURI(url.toString());
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, updatableModel.asRDFNode(node));
        assertEquals(url, getUriObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE));
    }

    @Test
    @DisplayName("Update string object by property")
    void checkUpdateStringObject() throws SaiRdfException {
        String name = "Updated name";
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_NAME, name);
        assertEquals(name, getStringObject(updatableResource, TestableVocabulary.TESTABLE_NAME));
    }

    @Test
    @DisplayName("Update URI object by property")
    void checkUpdateUriObject() throws SaiRdfException {
        URI milestone = URI.create("https://solidproject.org/roadmap#milestone");
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, milestone);
        assertEquals(milestone, getUriObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE));
    }

    @Test
    @DisplayName("Update date time object by property")
    void checkUpdateDateTimeObject() throws SaiRdfException {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-12-25T06:00:00.000Z", DateTimeFormatter.ISO_DATE_TIME);
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_CREATED_AT, dateTime);
        assertEquals(dateTime, getDateTimeObject(updatableResource, TestableVocabulary.TESTABLE_CREATED_AT));
    }

    @Test
    @DisplayName("Update Integer object by property")
    void checkUpdateIntegerObject() throws SaiRdfException {
        int id = 777;
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_ID, id);
        assertEquals(id, getIntegerObject(updatableResource, TestableVocabulary.TESTABLE_ID));
    }

    @Test
    @DisplayName("Update boolean object by property")
    void checkUpdateBooleanObject() throws SaiRdfNotFoundException, SaiRdfException {
        RdfUtils.updateObject(updatableResource, TestableVocabulary.TESTABLE_ACTIVE, false);
        Boolean value = getBooleanObject(updatableResource, TestableVocabulary.TESTABLE_ACTIVE);
        assertNotNull(value);
        assertFalse(value);
    }

    @Test
    @DisplayName("Update list of RDF node objects by property")
    void checkUpdateObjects() throws SaiRdfException {

        List<RDFNode> tags = Arrays.asList(updatableResource.getModel().asRDFNode(NodeFactory.createURI("https://data.example/tags/tag-11111")),
                                           updatableResource.getModel().asRDFNode(NodeFactory.createURI("https://data.example/tags/tag-33333")),
                                           updatableResource.getModel().asRDFNode(NodeFactory.createURI("https://data.example/tags/tag-22222")));
        updateObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, tags);
        assertTrue(CollectionUtils.isEqualCollection(tags, getObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE)));
    }

    @Test
    @DisplayName("Update list of URI objects by property")
    void checkUpdateUriObjects() throws SaiRdfException {

        List<URI> tags = Arrays.asList(URI.create("https://data.example/tags/tag-11111"),
                URI.create("https://data.example/tags/tag-22222"),
                URI.create("https://data.example/tags/tag-33333"));
        updateUriObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, tags);
        assertTrue(CollectionUtils.isEqualCollection(tags, getUriObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE)));
    }

    @Test
    @DisplayName("Update list of string objects by property")
    void checkUpdateStringObjects() throws SaiRdfException {
        List<String> comments = Arrays.asList("First UPDATED comment" ,
                "Second UPDATED comment" ,
                "Third UPDATED comment");
        updateStringObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_COMMENT, comments);
        assertTrue(CollectionUtils.isEqualCollection(comments, getStringObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_COMMENT)));
    }

    @Test
    @DisplayName("Get URI value from RDF node")
    void checkNodeToUri() throws SaiRdfException {
        RDFNode object = getObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertEquals(READABLE_MILESTONE, nodeToUri(object));
        // Fail when URI is not a resource
        RDFNode notResource = getObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertThrows(SaiRdfException.class, () -> { nodeToUri(notResource); });
    }

    @Test
    @DisplayName("Fail to get malformed URI value from RDF node")
    void failNodeToUriMalformed() {
        // Fail when URI is not a resource
        RDFNode mockNode = mock(RDFNode.class);
        Resource mockResource = mock(Resource.class);
        when(mockNode.isResource()).thenReturn(true);
        when(mockNode.asResource()).thenReturn(mockResource);
        when(mockResource.getURI()).thenReturn("http:{}{}cool\\web");
        assertThrows(SaiRdfException.class, () -> { nodeToUri(mockNode); });
    }

    @Test
    @DisplayName("Get Jena Lang for content-type")
    void checkLangForContentType() {
        assertEquals(Lang.TURTLE, getLangForContentType(null));
        assertEquals(Lang.TURTLE, getLangForContentType(TEXT_TURTLE));
        assertEquals(Lang.JSONLD11, getLangForContentType(LD_JSON));
        assertEquals(Lang.RDFXML, getLangForContentType(RDF_XML));
        assertEquals(Lang.NTRIPLES, getLangForContentType(N_TRIPLES));
    }

    private static String getRdfResourceBody() {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "PREFIX test: <http://testable.example/ns/testable#>\n" +
                "\n" +
                "<> ldp:contains </data/projects/project-1/milestone-3/> .\n" +
                "\n" +
                "<#project>\n" +
                "  test:id 6 ;\n" +
                "  test:name \"Great Validations\" ;\n" +
                "  test:createdAt \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime ;\n" +
                "  test:active true ;\n" +
                "  test:hasMilestone </data/projects/project-1/milestone-3/#milestone> ;\n" +
                "  test:hasTag\n" +
                "    </tags/tag-1> ,\n" +
                "    </tags/tag-2> ,\n" +
                "    </tags/tag-3> ;\n" +
                "  test:hasComment\n" +
                "    \"First original comment\" ,\n" +
                "    \"Second original comment\" ,\n" +
                "    \"Third original comment\" .";
    }

    private static String getInvalidRdfResourceBody() {
        return "PRE rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PR rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PR xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PRE xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFI ldp: <http://www.w3.org/ns/ldp#>\n" +
                "X test: <http://testable.example/ns/testable#>\n" +
                "\n" +
                "<#project>\n" +
                "  test:id 6 ;\n" +
                "  test:name \"Great Validations\" .\n" +
                "  test:createdAt \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime .\n" +
                "  test:active true ;\n" +
                "  test:hasMilestone </data/projects/project-1/milestone-3/#milestone> ;\n" +
                "  test:hasTag\n" +
                "    </tags/tag-1> ,\n" +
                "    </tags/tag-2> .\n" +
                "    </tags/tag-3> ;\n" +
                "  test:hasComment\n" +
                "    \"First original comment\" ,\n" +
                "    \"Second original comment\" ,\n" +
                "    \"Third original comment\" .";
    }

    private static String getInvalidJsonLdContext() {
        return "  {\n" +
                "      \"@context\" \"https://solid.github.io/data-interoperability-panel/specification/contexts/social-agent-profile.jsonld\",\n" +
                "  }";
    }

}
