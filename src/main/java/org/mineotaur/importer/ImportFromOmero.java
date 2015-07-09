
package org.mineotaur.importer;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.RType;
import omero.ServerError;
import omero.api.IContainerPrx;
import omero.api.IMetadataPrx;
import omero.api.IQueryPrx;
import omero.api.ServiceFactoryPrx;
import omero.client;
import omero.grid.*;
import omero.model.*;
import omero.sys.ParametersI;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.FileUtil;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import pojos.*;

import java.io.*;
import java.util.*;


/**
 * Created by balintantal on 06/07/2015.
 */

public class ImportFromOmero extends DatabaseGenerator{

    private String hostName;
    private client client;
    private ServiceFactoryPrx entry;
    private String userName;
    private String password;
    protected ScreenData screen;
    protected Set<PlateData> plates;
    protected Long screenId;
    private Label experimentLabel;
    private String[] descriptiveHeader;

    public ImportFromOmero(  String hostName, String userName, String password, Long screenId) {
        this.password = password;
        this.userName = userName;
        this.hostName = hostName;
        this.screenId = screenId;
    }


    protected void establishConnection() throws CannotCreateSessionException, PermissionDeniedException, ServerError {
        client = new client(hostName);
        entry = client.createSession(userName, password);
    }

    protected void closeConnection() {
        if (client != null) {
            client.closeSession();
        }
    }

    protected void getWellData(Long plateId, Long plateAcquisitionId) throws ServerError {
        IQueryPrx proxy = entry.getQueryService();
        StringBuilder sb = new StringBuilder();
        ParametersI param = new ParametersI();
        param.addLong("plateID", plateId);
        sb.append("select well from Well as well ");
        sb.append("left outer join fetch well.plate as pt ");
        sb.append("left outer join fetch well.wellSamples as ws ");
        sb.append("left outer join fetch ws.plateAcquisition as pa ");
        sb.append("left outer join fetch ws.image as img ");
        sb.append("left outer join fetch img.pixels as pix ");
        sb.append("left outer join fetch pix.pixelsType as pt ");
        sb.append("where well.plate.id = :plateID");
        if (plateAcquisitionId > 0) {
            sb.append(" and pa.id = :acquisitionID");
            param.addLong("acquisitionID", plateAcquisitionId);
        }
        List<IObject> results = proxy.findAllByQuery(sb.toString(), param);
        Iterator<IObject> i = results.iterator();
        WellData well;
        while (i.hasNext()) {
            well = new WellData((Well) i.next());
            System.out.println(well.getColumn() + " " + well.getRow());
            //Do something
        }
    }


    protected void createNode(Label label, Map<String, Object> data) {
        Node node = db.createNode(label);
        Set<String> keySet = data.keySet();
        for (String key: keySet) {
            node.setProperty(key, data.get(key));
        }
    }

    protected void getScreenData() throws ServerError {
        IContainerPrx proxy = entry.getContainerService();
        ParametersI param = new ParametersI();
        long userId = entry.getAdminService().getEventContext().userId;

        List<Long> screenIds = new ArrayList<>();
        screenIds.add(screenId);
        List<IObject> results = proxy.loadContainerHierarchy(Screen.class.getName(), screenIds, param);

        Iterator<IObject> i = results.iterator();
//        ScreenData screen;


/*Iterator<PlateData> j;
        PlateData plate;*/

        while (i.hasNext()) {
            screen = new ScreenData((Screen) i.next());
            if (i.hasNext()) {
                Mineotaur.LOGGER.info("There are multiple screens with id: " + screenId);
            }
            break;
//            System.out.println(screen.getName());
        }
        Mineotaur.LOGGER.info("Screen loaded.");
    }

    protected void getPlates() {
        plates = screen.getPlates();
    }

    protected void getAnnotations(String name, Long Id) throws ServerError {
        List<String> nsToInclude = new ArrayList<String>();
        List<String> nsToExclude = new ArrayList<String>();
        ParametersI param = new ParametersI();
        param.addLong(name, Id);
        IMetadataPrx proxy = entry.getMetadataService();
        List<Annotation> annotations = proxy.loadSpecifiedAnnotations(FileAnnotation.class.getName(), nsToInclude, nsToExclude, param);
        Iterator<Annotation> j = annotations.iterator();
        while (j.hasNext()) {
            FileAnnotation annotation = (FileAnnotation) j.next();
            FileAnnotationData fad = new FileAnnotationData(annotation);
            //System.out.println(fad.getFileName());
            readTable(annotation.getFile());
            break;
        }
//Do something with annotations.
    }

    protected void readTable(OriginalFile file) throws ServerError {
        TablePrx table = entry.sharedResources().openTable(file);

//read headers
        Column[] cols = table.getHeaders();
        List<Integer> experimentIDs = new ArrayList<>();
        List<Integer> strainIDs = new ArrayList<>();
//        String[] descriptiveHeader = null;
        Integer filter = null;
        Integer experimentID = null;
        Integer strainID = null;
        Integer descriptiveID = null;
        for (int i = 0; i < cols.length; i++) {
            String colName = cols[i].name;
            //System.out.println(colName);
            if ("ImageID".equals(colName)) {
                //experimentIDs.add(i);
                experimentID = i;
            }
            else if ("strainID".equals(colName)) {
                //strainIDs.add(i);
                strainID = i;
            }
            if ("PlateID".equals(colName) || "WellID".equals(colName) || "ImageID".equals(colName) || "imageName".equals(colName) || "plateName".equals(colName) || "well".equals(colName)) {
                experimentIDs.add(i);
            }
            else if ("reference".equals(colName) || "name".equals(colName) || "alternativeName".equals(colName) || "description".equals(colName)) {
                strainIDs.add(i);
            }
            if (cols[i].getClass().equals(DoubleArrayColumn.class)) {
                descriptiveID = i;
                descriptiveHeader = colName.split(",");
                for (int j = 0; j < descriptiveHeader.length; ++j) {

                    if (descriptiveHeader[j].equals("predictedMT")) {
                        filter = j;
                        break;
                    }
                }
            }
        }

// Depending on size of table, you may only want to read some blocks.
        long[] columnsToRead = new long[cols.length];
        for (int i = 0; i < cols.length; i++) {
            columnsToRead[i] = i;
        }

// The number of columns we wish to read.
        long[] rowSubset = new long[(int) (table.getNumberOfRows()-1)];
        for (int j = 0; j < rowSubset.length; j++) {
            rowSubset[j] = j;
        }
        Data data = table.slice(columnsToRead, rowSubset); // read the data.
        cols = data.columns;
        Map<Long, Node> images = new HashMap<>();
        Map<String, Node> strains = new HashMap<>();
        String[] strainColumn = ((StringColumn) cols[strainID]).values;
        long[] experimentColumn = ((ImageColumn) cols[experimentID]).values;
        double[][] descriptiveColumn = ((DoubleArrayColumn)cols[descriptiveID]).values;
        double[] filterColumn = descriptiveColumn[filter];
        filterProps.add(descriptiveHeader[filter]);
        for (int i = 0; i < strainColumn.length; ++i) {
            try (Transaction tx = db.beginTx()) {
                Mineotaur.LOGGER.info("Line " + i);
                String sid = strainColumn[i];
                Node strain = strains.get(sid);
                if (strain == null) {
                    strain = db.createNode(groupLabel);
                    strain.setProperty("strainID", sid);
                    addProperties(strain, cols, strainIDs, i);
                    strains.put(sid, strain);
                    Mineotaur.LOGGER.info("Strain created with imageID: " + sid);
                }
                else {
                    Mineotaur.LOGGER.info("Strain loaded with imageID: " + sid);
                }
                Long imageID = experimentColumn[i];
                Node experiment = images.get(imageID);
                if (experiment == null) {
                    experiment = db.createNode(experimentLabel);
                    experiment.setProperty("imageID", imageID);
                    addProperties(experiment, cols, experimentIDs, i);
                    images.put(imageID, experiment);
                    experiment.createRelationshipTo(strain, DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());
                    Mineotaur.LOGGER.info("Experiment created with imageID: " + imageID);
                }
                else {
                    Mineotaur.LOGGER.info("Experiment loaded with imageID: " + imageID);
                }
                Node descriptive = createDescriptiveNode(descriptiveHeader, descriptiveColumn[i]);
                descriptive.createRelationshipTo(strain, DefaultRelationships.GROUP_CELL.getRelationshipType());
                Mineotaur.LOGGER.info("Descriptive node created.");
                tx.success();
            }


        }


        table.close();
    }

    protected Node createDescriptiveNode(String[] descriptiveHeader, double[] descriptiveColumn) {
        Node node = db.createNode(descriptiveLabel);
        for (int i = 0; i< descriptiveHeader.length; ++i) {
            node.setProperty(descriptiveHeader[i], descriptiveColumn[i]);
        }
        return node;
    }

    protected void addProperties(Node node, Column[] cols, List<Integer> indices, int i) {
        for (Integer j: indices) {
            Column c = cols[j];
            Object value = null;
            Class claz = c.getClass();
            if (claz.equals(PlateColumn.class)) {
                long[] plateIDs = ((PlateColumn)c).values;
                value = plateIDs[i];
            }
            else if (claz.equals(WellColumn.class)) {
                long[] wellIDs = ((WellColumn)c).values;
                value = wellIDs[i];
            }
            else if (claz.equals(ImageColumn.class)) {
                long[] imageIDs = ((ImageColumn)c).values;
                value = imageIDs[i];
            }
            else if (claz.equals(StringColumn.class)) {
                String[] stringValues = ((StringColumn)c).values;
                value = stringValues[i];

            }
            if (value == null) {
                continue;
            }
            node.setProperty(c.name, value);

        }
    }

    @Override
    public void generateDatabase() {
        try {
            Mineotaur.LOGGER.info("Establishing connection with the Omero server.");
            establishConnection();
            Mineotaur.LOGGER.info("Processing metadata.");
            processMetadata();
            getImageIDs(DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());
            /*
            Mineotaur.LOGGER.info("Creating directories.");
            createDirs();
            Mineotaur.LOGGER.info("Creating indices.");
            createIndex(db);
            Mineotaur.LOGGER.info("Processing input data.");
            processData();
            Mineotaur.LOGGER.info("Processing label data.");
            labelGenes();
            if (filterProps != null && !filterProps.isEmpty()) {
                createFilters();
            }
            if (toPrecompute) {
                Mineotaur.LOGGER.info("Precomputing nodes.");
                precomputed = DynamicLabel.label(group+COLLECTED);
                precomputeOptimized(10000);
            }
            else {
                mineotaurProperties.put("query_relationship", relationships.get(group).get(descriptive));
            }
            Mineotaur.LOGGER.info("Generating property files.");
            storeFeatureNames();
            storeGroupnames(db);
            generatePropertyFile();
        } catch (IOException e) {
            e.printStackTrace(); */
        } catch (CannotCreateSessionException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (ServerError serverError) {
            serverError.printStackTrace();
        } finally {
            closeConnection();
        }
        Mineotaur.LOGGER.info("Database generation finished. Start Mineotaur instance with -start " + name);
    }




    @Override
    protected void storeFeatureNames() {
        List features = Arrays.asList(descriptiveHeader);
        Collections.sort(features);
        FileUtil.saveList(confDir + "mineotaur.features", features);
    }


    @Override
    public void processData() {
        Mineotaur.LOGGER.info("Loading annotation table.");
        try {
            getAnnotations("screenID", screen.getId());
        } catch (ServerError serverError) {
            serverError.printStackTrace();
        }
    }

    public void processMetadata() {
        try {
            getScreenData();
            name = screen.getName().replaceAll("\\P{Alnum}", "");
            Mineotaur.LOGGER.info("Screen name:" + name);
            confDir = name + FILE_SEPARATOR + CONF + FILE_SEPARATOR;

            dbPath = name + FILE_SEPARATOR + DB + FILE_SEPARATOR;
            addDummyValues();
            startDB();

        } catch (ServerError serverError) {
            serverError.printStackTrace();
        }

    }

    protected void addDummyValues() {
        overwrite = true;
        totalMemory = "4G";
        cache = "none";
        groupLabel = DefaultLabels.GROUP.getLabel();
        groupName = groupLabel.name();
        group = DefaultLabels.GROUP.name();
        descriptive = DefaultLabels.DESCRIPTIVE.name();
        descriptiveLabel = DefaultLabels.DESCRIPTIVE.getLabel();
        experimentLabel = DefaultLabels.EXPERIMENT.getLabel();
        Map<String, RelationshipType> rel= new HashMap<>();
        rel.put("EXPERIMENT", DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());
        rel.put(descriptive, DefaultRelationships.GROUP_CELL.getRelationshipType());
        relationships.put(group, rel);
        toPrecompute = true;
        separator = "\t";
    }

    @Override
    public void labelGenes() {
        labelGenes("Input/sysgro_labels.tsv");
    }

    protected void labelGenes(String file) {
        try (Transaction tx = db.beginTx(); BufferedReader br = new BufferedReader(new FileReader(file))) {
            String[] header = br.readLine().split(separator);
            String line;
            List<String> list = new ArrayList<>(Arrays.asList(header).subList(1, header.length));
            FileUtil.saveList(confDir + "mineotaur.hitLabels", list);
            while ((line = br.readLine()) != null) {
                String[] terms = line.split(separator);

                Iterator<Node> nodes = db.findNodesByLabelAndProperty(groupLabel, header[0], terms[0]).iterator();

                if (!nodes.hasNext()) {
                    Mineotaur.LOGGER.warning("No such gene: " + terms[0]);
                    continue;

                }
                Node node = nodes.next();

                if (nodes.hasNext()) {
                    throw new IllegalStateException("Id is not unique: " + terms[0]);

                }
                boolean hasLabel = false;
                for (int i = 1; i < terms.length; ++i) {
                    if (terms[i].equals("1")) {
                        node.addLabel(DynamicLabel.label(header[i]));
                        hasLabel = true;
                    }
                }
                if (!hasLabel) {
                    node.addLabel(wildTypeLabel);
                }
                tx.success();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void generatePropertyFile() throws IOException {
        if (filterProps == null || filterProps.isEmpty()) {
            mineotaurProperties.put("hasFilters", "false");
        }
        else {
            mineotaurProperties.put("hasFilters", "true");
            mineotaurProperties.put("filterName", filterProps.get(0));
            storeFilters();
        }
        if (toPrecompute) {
            mineotaurProperties.put("query_relationship", precomputed.name());
        }
        else {
            mineotaurProperties.put("query_relationship", relationships.get(groupLabel.name()).get(descriptiveLabel.name()));
        }
        mineotaurProperties.put("group", "GROUP");
        mineotaurProperties.put("groupName", "reference");
        mineotaurProperties.put("db_path", dbPath);
        mineotaurProperties.put("cache", "soft");
        mineotaurProperties.put("total_memory", "4G");
        mineotaurProperties.put("omero", hostName);
        mineotaurProperties.store(new FileWriter(confDir + "mineotaur.properties"), "Mineotaur configuration properties");
    }




}

