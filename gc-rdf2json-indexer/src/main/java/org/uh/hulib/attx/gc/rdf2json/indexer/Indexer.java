/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.rdf2json.indexer;

import com.github.jsonldjava.utils.JsonUtils;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 *
 * @author jkesanie
 */
public class Indexer {

    private Logger log = Logger.getLogger(Indexer.class.getName());

    private Model model = null;

    private BulkProcessor bulkProcessor;
    private String storeEndpoint;
    private TransportClient client;

    private final String APIBASEURI = "http://data.hulib.helsinki.fi/api/v1/";

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        options.addOption("s", true, "StoreEndpoint");
        options.addOption("i", true, "IndexEndpoint");
        options.addOption("p", true, "IndexPort");
        options.addOption("g", true, "Comma separated list of input graphs");
        options.addOption("b", true, "Bulk size");
        options.addOption("m", true, "Mapping json");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Indexer", options);

        if (!cmd.hasOption("s")
                || !cmd.hasOption("i")
                || !cmd.hasOption("p")
                || !cmd.hasOption("g")
                || !cmd.hasOption("p")
                || !cmd.hasOption("m")) {
            System.out.println("Required options missing!");
            return;
        }

        String storeEndpoint = cmd.getOptionValue('s');
        String indexEndpoint = cmd.getOptionValue('i');
        int indexPort = Integer.parseInt(cmd.getOptionValue('p'));
        String graphs = cmd.getOptionValue('g');
        int bulkSize = Integer.parseInt(cmd.getOptionValue('b'));
        String mapping = cmd.getOptionValue('m');

        TransportClient client = Indexer.getClient(indexEndpoint, indexPort, "elasticsearch");
        String[] inputGraphs = null;
        if (graphs != null && !"".equals(graphs)) {
            inputGraphs = graphs.split(",");
        }

        Indexer i = new Indexer(storeEndpoint, client, bulkSize);
        i.prepareData(inputGraphs);
        i.map(inputGraphs, mapping, bulkSize);

    }

    public static TransportClient getClient(String indexEndpoint, int port, String clusterName) {
        if (indexEndpoint != null) {
            Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
            return new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(indexEndpoint, port));
        } else {
            return null;
        }

    }

    public Indexer(String storeEndpoint, TransportClient client, int bulkSize) {
        this.client = client;
        this.storeEndpoint = storeEndpoint;
        this.model = ModelFactory.createMemModelMaker().createDefaultModel();

        if (client != null) {
            bulkProcessor = BulkProcessor.builder(
                    this.client,
                    new BulkProcessor.Listener() {
                @Override
                public void beforeBulk(long executionId,
                        BulkRequest request) {
                    System.out.println("Bulk started");
                }

                @Override
                public void afterBulk(long executionId,
                        BulkRequest request,
                        BulkResponse response) {
                    System.out.println("Bulk success");
                }

                @Override
                public void afterBulk(long executionId,
                        BulkRequest request,
                        Throwable failure) {
                    System.out.println("Bulk failure");
                }
            })
                    .setBulkActions(bulkSize)
                    //.setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))
                    .setFlushInterval(TimeValue.timeValueSeconds(1))
                    .setConcurrentRequests(1)
                    .build();
        } else {
            log.info("ES Client was null. Could not create bulk processor");
        }

    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return this.model;
    }

    public void map(String[] inputGraphs, String mapping, int bulkSize) throws Exception {

        // read mapping
        Map<String, Object> jsonConf = (Map<String, Object>) JsonUtils.fromString(mapping);
        List<Map<String, Object>> configs = (List<Map<String, Object>>) jsonConf
                .get("configs");

        for (Map<String, Object> o : configs) {
            String index = o.get("index").toString();
            String graph = (String) o.get("graph");
            Map<String, Object> targets = (Map<String, Object>) o
                    .get("targets");

            String newIndex = index + "_" + getSuffix();

            try {
                createNewSirenIndex(newIndex);
                for (String uri : targets.keySet()) {
                    handleTarget(newIndex, uri,
                            (Map<String, Object>) targets.get(uri));

                }
                this.bulkProcessor.flush();
                this.bulkProcessor.close();
                switchIndices(index, newIndex);

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Something went wrong with the indexing ("
                        + index + "). This index was not updated.");
                continue;
            }
        }
    }

    public void prepareData(String[] inputGraphs) throws Exception {
        // download all input graphs to the model using graph store protocol            
        // Note: this could end up using a lot of memory
        if (inputGraphs == null || inputGraphs.length == 0) {
            String url = storeEndpoint + "data";
            RDFDataMgr.read(this.model, url);
            return;
        }
        for (String graphURI : inputGraphs) {
            String url = storeEndpoint + "data?graph=" + graphURI;
            RDFDataMgr.read(this.model, url, Lang.TURTLE);
        }

    }

    void indexDocument(XContentBuilder x, String index,
            String type, String id) {
        
        if(this.bulkProcessor != null) {            
            this.bulkProcessor.add(new IndexRequest(index, type, id).source(x));
            //IndexResponse response = client.prepareIndex(index, type, id)
            //    .setSource(x).execute().actionGet();

        }
        else {
            this.log.warning("No bulkprocessor. Document is not indexed");
        }

    }

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    String getSuffix() {
        long millis = System.currentTimeMillis();
        Date today = new Date(millis);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return dateFormat.format(today) + "." + (millis - cal.getTimeInMillis());
    }

    void switchIndices(String prefix, String newIndex) {
        String currentIndex = getCurrentIndex(prefix);
        if (currentIndex == null) {
            // just add new alias to current
            IndicesAliasesResponse ir = client.admin().indices()
                    .prepareAliases().addAlias(newIndex, "current")
                    .execute().actionGet();
            if (!ir.isAcknowledged()) {
                System.out.println("Could not update index aliases.");
                client.admin().indices().prepareDelete(newIndex).execute()
                        .actionGet();
            }
        } else {
            IndicesAliasesResponse ir = client.admin().indices()
                    .prepareAliases().removeAlias(currentIndex, "current")
                    .addAlias(newIndex, "current").execute().actionGet();

            if (ir.isAcknowledged()) {
                // remove old index
                client.admin().indices().prepareDelete(currentIndex)
                        .execute().actionGet();
            } else {
                System.out
                        .println("Could not update index aliases. Old index ("
                                + currentIndex + ") was not removed.");
                // try to remove the new index
                client.admin().indices().prepareDelete(newIndex).execute()
                        .actionGet();
            }
        }
    }

    boolean createNewSirenIndex(String index) {
        // create timestamped index
        String mapping = "{\"_default_\": {\"properties\": {\"_siren_source\": {\"analyzer\": \"concise\",\"postings_format\": \"Siren10AFor\",\"store\": \"no\",\"type\": \"string\"}},\"_siren\": {},\"dynamic_templates\": [{\"rawstring\": {	\"match\": \"*\",\"match_mapping_type\": \"string\",\"mapping\": {\"type\": \"string\",	\"index\": \"analyzed\",\"fields\": {	\"{name}_raw\": { \"type\": \"string\", \"analyzer\": \"case_insensitive_sort\" }	}}}}]}}";
        CreateIndexResponse cr = client.admin().indices().prepareCreate(index).addMapping("_default_", mapping).execute().actionGet();
//CreateIndexResponse cr = client.admin().indices().prepareCreate(index).execute().actionGet();        

        return cr.isAcknowledged();
    }

    String getCurrentIndex(String prefix) {
        ImmutableOpenMap<String, ImmutableOpenMap<String, AliasMetaData>> aliases = client.admin().cluster()
                .prepareState().execute().actionGet().getState().getMetaData().getAliases();
        ImmutableOpenMap<String, AliasMetaData> current = aliases.get("current");
        if (current == null) {
            return null;
        }

        String currentIndex = null;
        UnmodifiableIterator<String> i = current.keysIt();
        while (i.hasNext()) {
            String index = i.next();
            if (index.startsWith(prefix)) {
                currentIndex = index;
            }
        }
        return currentIndex;
    }

    void handleTarget(String index, String typeURI,
            Map<String, Object> conf) throws Exception {

        ResIterator resi = model.listSubjectsWithProperty(RDF.type,
                ResourceFactory.createResource(typeURI));
        String indexName = index;
        String typeName = (String) conf.get("type");

        while (resi.hasNext()) {
            Resource r = resi.nextResource();
            //System.out.println("Handling resource " + r.getURI());
            String id = "";
            String idProperty = null;
            // id
            if (conf.containsKey("idProperty")
                    && !"".equals(conf.get("idProperty"))) {
                String propertyURI = (String) conf.get("idProperty");
                id = r.getPropertyResourceValue(
                        ResourceFactory.createProperty(propertyURI)).getURI();

            } else {
                id = r.getURI();
            }

            Map<String, Object> docConf = (Map<String, Object>) conf
                    .get("indexDoc");
            XContentBuilder indexDocBuilder = generateDocument(idProperty, id,
                    r, docConf);
            indexDocBuilder.close();

            //System.out.println("Got document");
            //System.out.println(indexDocBuilder.string());
            indexDocument(indexDocBuilder, indexName, typeName, id);
        }
    }

    void handleLiteralProperty(XContentBuilder builder,
            Resource r, String sourceProperty, String targetProperty,
            boolean isFunctional, String defaultValue) throws Exception {

        if (r != null) {

            StmtIterator stmts = r.listProperties(ResourceFactory
                    .createProperty(sourceProperty));
            if (stmts.hasNext()) {
                if (defaultValue != null && !"".equals(defaultValue)) {
                    String[] valueParts = defaultValue.split("\\|");
                    builder.field(targetProperty, valueParts[0]);
                } else {

                    if (!isFunctional) {
                        builder.startArray(targetProperty);
                    }

                    while (stmts.hasNext()) {
                        Statement stmt = stmts.next();
                        RDFNode node = stmt.getObject();
                        Literal lit = node.asLiteral();

                        String typeURI = lit.getDatatypeURI();
                        Object value = null;
                        if (typeURI != null) {
                            if ("java:java.sql.Date".equals(typeURI)) {
                                value = lit.getLexicalForm();
                            } else {

                                if (lit.getValue() != null) {
                                    Object litValue = lit.getValue();

                                    if (typeURI.equals("http://www.w3.org/2001/XMLSchema#decimal")) {
                                        value = ((BigDecimal) litValue).doubleValue();
                                    } else if (typeURI.equals("http://www.w3.org/2001/XMLSchema#integer")) {
                                        value = (Integer) litValue;
                                    } else if (typeURI.equals("http://www.w3.org/2001/XMLSchema#double")) {
                                        value = (Double) litValue;

                                    } else {
                                        value = lit;
                                    }
                                } else {
                                    value = lit.toString();
                                }
                            }
                        } else {
                            value = lit.toString();
                        }
                        if (isFunctional) {
                            builder.field(targetProperty, value);
                            break;

                        } else {
                            builder.value(value);
                        }
                    }

                    if (!isFunctional) {
                        builder.endArray();
                    }
                }
            } else {
                if (defaultValue != null && !"".equals(defaultValue)) {
                    String[] valueParts = defaultValue.split("\\|");
                    if (valueParts.length == 2) {
                        builder.field(targetProperty, valueParts[1]);
                    }
                }
            }
        }
    }

    void handleObjectProperty(XContentBuilder builder,
            String resourceID, Resource r, String sourceProperty,
            String targetProperty, boolean isFunctional,
            Map<String, Object> nestedMapping, String apiTemplate)
            throws Exception {
        if (r != null) {

            if (nestedMapping != null) {
                // resource should be rendered using nested mapping
                StmtIterator stmts = r.listProperties(ResourceFactory
                        .createProperty(sourceProperty));
                if (stmts.hasNext()) {
                    if (!isFunctional) {
                        builder.startArray(targetProperty);
                    }

                    while (stmts.hasNext()) {
                        if (isFunctional) {
                            builder.startObject(targetProperty);
                        } else {
                            builder.startObject();
                        }

                        Statement stmt = stmts.next();
                        Resource targetResource = stmt.getObject().asResource();
                        handleResource(builder, targetResource, nestedMapping);
                        builder.endObject();
                    }

                    if (!isFunctional) {
                        builder.endArray();
                    }

                }

            } else if (apiTemplate != null) {
                String encodedID = URLEncoder.encode(resourceID);
                builder.field(targetProperty, APIBASEURI + "id/" + encodedID
                        + "/" + targetProperty);
            } else {
                StmtIterator stmts = r.listProperties(ResourceFactory
                        .createProperty(sourceProperty));

                if (stmts.hasNext()) {
                    if (!isFunctional) {
                        builder.startArray(targetProperty);
                    }

                    while (stmts.hasNext()) {
                        Statement stmt = stmts.next();
                        // check for canonical uri of the object
                        StmtIterator i2 = ((Resource) stmt.getObject())
                                .listProperties(ResourceFactory
                                        .createProperty("http://data.hulib.helsinki.fi/schemas/uhod/v1/hasCanonicalURI"));
                        if (!isFunctional) {
                            if (i2.hasNext()) {
                                builder.value(i2.next().getObject().asNode()
                                        .getURI());
                            } else {
                                builder.value(stmt.getObject().asNode()
                                        .getURI());
                            }
                        } else {
                            if (i2.hasNext()) {
                                builder.field(targetProperty, i2.next()
                                        .getObject().asNode().getURI());
                            } else {
                                builder.field(targetProperty, stmt.getObject()
                                        .asNode().getURI());
                            }

                        }
                    }

                    if (!isFunctional) {
                        builder.endArray();
                    }
                }

            }
        }

    }

    Resource traverseGraph(Resource origin, String[] st,
            int index) throws Exception {
        String property = st[index];
        Resource object = origin.getPropertyResourceValue(ResourceFactory
                .createProperty(property));
        if (object != null) {
            if (index < (st.length - 2)) {
                index = index + 1;
                return traverseGraph(object, st, index);
            } else {
                return object;
            }
        } else {
            // could not traverse object property
            return null;
        }
    }

    private void handleResource(XContentBuilder builder, Resource r,
            Map<String, Object> conf) throws Exception {
        // id
        String id = null;
        if (conf.containsKey("idProperty")) {
            String propertyURI = conf.get("idProperty").toString();
            if (!"".equals(propertyURI)) {
                Resource idResource = r
                        .getPropertyResourceValue(ResourceFactory
                                .createProperty(propertyURI));
                if (idResource != null) {
                    id = idResource.getURI();
                }
            }

        } else {
            id = r.getURI();
        }
        builder.field("@id", id);
        handleDataProperties(builder, r, conf);
        handleMultilingualDataProperties(builder, r, conf);
        handleObjectProperties(builder, id, r, conf);
    }

    void handleObjectProperties(XContentBuilder builder,
            String resourceID, Resource r, Map<String, Object> conf)
            throws Exception {
        if (conf.containsKey("objectProperties")) {
            List<Map<String, Object>> properties = (List<Map<String, Object>>) conf
                    .get("objectProperties");
            for (Map<String, Object> property : properties) {
                boolean isFunctional = false;
                if (property.containsKey("isFunctional")) {
                    isFunctional = Boolean.parseBoolean(property.get(
                            "isFunctional").toString());
                }
                String targetProperty = property.get("targetProperty")
                        .toString();

                String sourceProperty = property.get("sourceProperty")
                        .toString();

                Map<String, Object> nestedMapping = null;
                if (property.containsKey("nestedMapping")) {
                    nestedMapping = (Map<String, Object>) property
                            .get("nestedMapping");
                }

                String apiTemplate = null;
                if (property.containsKey("apiTemplate")) {
                    apiTemplate = property.get("apiTemplate").toString();
                }

                handleObjectProperty(builder, resourceID, r, sourceProperty,
                        targetProperty, isFunctional, nestedMapping,
                        apiTemplate);
            }

        }

    }

    void handleMultilingualDataProperties(
            XContentBuilder builder, Resource r, Map<String, Object> conf)
            throws Exception {
        if (conf.containsKey("multiLingualDataProperties")) {
            List<Map<String, Object>> properties = (List<Map<String, Object>>) conf
                    .get("multiLingualDataProperties");
            for (Map<String, Object> property : properties) {
                StmtIterator stmts = r.listProperties(ResourceFactory
                        .createProperty(property.get("sourceProperty")
                                .toString()));
                String targetProperty = property.get("targetProperty")
                        .toString();

                boolean isFunctional = false;
                if (property.containsKey("isFunctional")) {

                    isFunctional = Boolean.parseBoolean(property.get(
                            "isFunctional").toString());
                }

                Map<String, List<String>> propertyData = new HashMap<String, List<String>>();

                while (stmts.hasNext()) {
                    Statement stmt = stmts.next();
                    RDFNode node = stmt.getObject();
                    String lang = node.asLiteral().getLanguage();
                    String value = node.asLiteral().getString();
                    if (value != null) {
                        if (propertyData.containsKey(lang)) {
                            propertyData.get(lang).add(value);
                        } else {
                            List<String> list = new ArrayList<String>();
                            list.add(value);
                            propertyData.put(lang, list);
                        }
                    }
                }

                if (!propertyData.isEmpty()) {
                    builder.startObject(targetProperty);
                    // process property data
                    for (String lang : propertyData.keySet()) {
                        if ("".equals(lang)) {
                            lang = "default";
                            if (isFunctional) {
                                builder.field(lang, propertyData.get("").get(0));
                            } else {
                                List<String> arrayValue = propertyData
                                        .get(lang);
                                if (arrayValue != null) {
                                    builder.field(lang, arrayValue);
                                }
                            }
                        } else {
                            if (isFunctional) {
                                builder.field(lang,
                                        propertyData.get(lang).get(0));
                            } else {
                                builder.field(lang, propertyData.get(lang));
                            }
                        }

                    }

                    builder.endObject();
                }

            }
        }
    }

    void handleDataProperties(XContentBuilder builder,
            Resource r, Map<String, Object> conf) throws Exception {
        if (conf.containsKey("dataProperties")) {
            List<Map<String, Object>> properties = (List<Map<String, Object>>) conf
                    .get("dataProperties");
            for (Map<String, Object> property : properties) {

                boolean isFunctional = false;
                if (property.containsKey("isFunctional")) {
                    isFunctional = Boolean.parseBoolean(property.get(
                            "isFunctional").toString());
                }
                String targetProperty = property.get("targetProperty")
                        .toString();

                String sourceProperty = property.get("sourceProperty")
                        .toString();

                String value = (String) property.get("value");

                String[] parts = sourceProperty.split("->");

                if (parts.length == 1) {
                    // no properties to follow, just get the literal object of
                    // the property
                    handleLiteralProperty(builder, r, sourceProperty,
                            targetProperty, isFunctional, value);
                } else {
                    // traverse graph
                    Resource leafResource = traverseGraph(r, parts, 0); // What about if the property is not functional?
                    sourceProperty = parts[parts.length - 1];
                    handleLiteralProperty(builder, leafResource,
                            sourceProperty, targetProperty, isFunctional, value);
                }

            }
        }
    }

    XContentBuilder generateDocument(String idProperty,
            String id, Resource r, Map<String, Object> conf) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();

        builder.field("@id", id);
        // data properties
        handleDataProperties(builder, r, conf);
        // multilingual data properties
        handleMultilingualDataProperties(builder, r, conf);
        // object properties
        handleObjectProperties(builder, id, r, conf);

        // builder.close();
        return builder;
    }
}
