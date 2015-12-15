
package org.mineotaur.importer;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.api.IContainerPrx;
import omero.api.IQueryPrx;
import omero.api.ServiceFactoryPrx;
import omero.client;
import omero.grid.*;
import omero.model.IObject;
import omero.model.OriginalFile;
import omero.model.Screen;
import omero.model.Well;
import omero.rtypes;
import omero.sys.ParametersI;
import org.mineotaur.application.Mineotaur;
import org.mineotaur.common.GraphDatabaseUtils;
import org.neo4j.graphdb.*;
import pojos.PlateData;
import pojos.ScreenData;
import pojos.WellData;

import java.io.IOException;
import java.util.*;


/**
 * Created by balintantal on 06/07/2015.
 */

public class DatabaseGeneratorFromOmero extends DatabaseGenerator{

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
    private OriginalFile dataFile;
    private OriginalFile labelFile;
    private List<String> hits = new ArrayList<>();
    protected ResourceBundle properties;
    private String groupId;
    private String labelFileName;
    private String dataFileName;
    private String imageId;
    private List<String> groupColumns;
    private List<String> experimentColumns;
    private String filterColumn;
    private String geneForLabel;
    private String hitColumn;
    private int rowsToFetch;
    private int precomputeLimit;

    /*public DatabaseGeneratorFromOmero(String hostName, String userName, String password, Long screenId) {
        this.password = password;
        this.userName = userName;
        this.hostName = hostName;
        this.screenId = screenId;
        addDummyValues();
    }*/
    
    public DatabaseGeneratorFromOmero(ResourceBundle properties) {
        this.properties = properties;
        processProperties();
    }
    
    protected void processProperties() {
        this.password = properties.getString("password");
        this.userName = properties.getString("userName");
        this.hostName = this.omero = properties.getString("hostName");
        this.screenId = Long.valueOf(properties.getString("screenId"));
        if (properties.containsKey("overwrite")) {
            overwrite = Boolean.valueOf(properties.getString("overwrite"));    
        }
        else {
            overwrite = (boolean) DefaultProperty.OVERWRITE.getValue();
        }
        if (properties.containsKey("totalMemory")) {
            totalMemory = properties.getString("totalMemory");
        }
        else {
            totalMemory = (String) DefaultProperty.TOTAL_MEMORY.getValue();
        }
        if (properties.containsKey("cache")) {
            cache = properties.getString("cache");
        }
        else {
            cache = "none";
        }
        if (properties.containsKey("groupName")) {
            groupName = properties.getString("groupName");
        }
        else {
            groupName = (String) DefaultProperty.GROUP_NAME.getValue();
        }
        if (properties.containsKey("separator")) {
            separator = properties.getString("separator");
        }
        else {
            separator = (String) DefaultProperty.SEPARATOR.getValue();
        }
        if (properties.containsKey("process_limit")) {
            limit = Integer.valueOf(properties.getString("process_limit"));
        }
        else {
            limit = (Integer) DefaultProperty.LIMIT.getValue();
        }
        if (properties.containsKey("precompute_limit")) {
            precomputeLimit = Integer.valueOf(properties.getString("precompute_limit"));
        }
        else {
            precomputeLimit = (Integer) DefaultProperty.PRECOMPUTE_LIMIT.getValue();
        }
        if (properties.containsKey("row_prefetch")) {
            rowsToFetch = Integer.valueOf(properties.getString("row_prefetch"));
        }
        else {
            rowsToFetch = (Integer) DefaultProperty.ROW_PREFETCH.getValue();
        }
        if (properties.containsKey("labelFile")) {
            labelFileName = properties.getString("labelFile");
        }
        else {
            throw new IllegalArgumentException("No label file name provided.");
        }
        if (properties.containsKey("dataFile")) {
            dataFileName = properties.getString("dataFile");
        }
        else {
            throw new IllegalArgumentException("No data file name provided.");
        }

        if (properties.containsKey("groupID")) {
            groupId = properties.getString("groupID");
        }
        else {
            throw new IllegalArgumentException("No groupID provided.");
        }
        if (properties.containsKey("imageID")) {
            imageId = properties.getString("imageID");
        }
        else {
            throw new IllegalArgumentException("No imageID provided.");
        }
        if (properties.containsKey("geneForLabel")) {
            geneForLabel = properties.getString("geneForLabel");
        }
        else {
            throw new IllegalArgumentException("No geneForLabel provided.");
        }
        if (properties.containsKey("hitColumn")) {
            hitColumn = properties.getString("hitColumn");
        }
        else {
            throw new IllegalArgumentException("No hitColumn provided.");
        }
        if (properties.containsKey("filterColumn")) {
            filterColumn = properties.getString("filterColumn");
        }
        else {
            throw new IllegalArgumentException("No filterColumn provided.");
        }
        if (properties.containsKey("groupColumns")) {
            groupColumns = Arrays.asList(properties.getString("groupColumns").split(","));
        }
        else {
            throw new IllegalArgumentException("No group columns provided.");
        }
        if (properties.containsKey("experimentColumns")) {
            experimentColumns = Arrays.asList(properties.getString("experimentColumns").split(","));
        }
        else {
            throw new IllegalArgumentException("No experiment columns provided.");
        }
        groupLabel = DefaultLabels.GROUP.getLabel();
        group = DefaultLabels.GROUP.name();
        descriptive = DefaultLabels.DESCRIPTIVE.name();
        descriptiveLabel = DefaultLabels.DESCRIPTIVE.getLabel();
        experimentLabel = DefaultLabels.EXPERIMENT.getLabel();
        Map<String, RelationshipType> rel= new HashMap<>();
        rel.put("EXPERIMENT", DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());
        rel.put(descriptive, DefaultRelationships.GROUP_CELL.getRelationshipType());
        relationships.put(group, rel);
        toPrecompute = true;
        hits.add(wildTypeLabel.name());
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
            Screen s = (Screen)i.next();
            screen = new ScreenData(s);
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
        nsToInclude.add("openmicroscopy.org/omero/bulk_annotations");
        List<String> nsToExclude = new ArrayList<String>();
            ParametersI param = new ParametersI();
            param.add("id",rtypes.rlong(Id));
            IQueryPrx proxy = entry.getQueryService();
            String q = "SELECT link.child.file FROM ScreenAnnotationLink link WHERE link.parent.id=:id";
            List<IObject> results = proxy.findAllByQuery(q, param);
        Iterator<IObject> i = results.iterator();
        while (i.hasNext()) {
            OriginalFile file = (OriginalFile) i.next();
            String fileName = file.getName().getValue();
            if (dataFile != null && labelFile != null) {
                break;
            }
            if (dataFile == null && dataFileName.equals(fileName)) {
                dataFile = file;
                continue;
            }
            if (labelFile == null && labelFileName.equals(fileName)) {
                labelFile = file;
                continue;
            }
            /*FileAnnotation a = (FileAnnotation)i.next();
            System.out.println(a.getFile().getName().getValue());*/
            /*FileAnnotationData fad = new FileAnnotationData(a);
            String fileName = fad.getFileName();
            System.out.println(fad.getAttachedFile().getName());
            System.out.println(fileName);*/
        }
        //IMetadataPrx proxy = entry.getMetadataService();
        //List<Annotation> annotations = proxy.loadSpecifiedAnnotations(FileAnnotation.class.getName(), nsToInclude, nsToExclude, param);
        /*Iterator<Annotation> j = annotations.iterator();
        while (j.hasNext()) {

            FileAnnotation annotation = (FileAnnotation) j.next();
            FileAnnotationData fad = new FileAnnotationData(annotation);
            String fileName = fad.getFileName();
            if (dataFile != null && labelFile != null) {
                break;
            }
            if (dataFile == null || fileName.endsWith(".h5")) {
                dataFile = annotation.getFile();
                continue;
            }
            if (labelFile == null || "bulk_annotations".equals(fileName)) {
                labelFile = annotation.getFile();
                continue;
            }
            //System.out.println(fad.getFileName() + " " + fad.getFileFormat());
            //readTable(annotation.getFile());
            //break;
        }*/
        /*System.out.println(dataFile.getName());
        System.out.println(labelFile.getName());*/
//Do something with annotations.
    }

    protected void readTable(OriginalFile file) throws ServerError {
        TablePrx table = entry.sharedResources().openTable(file);

//read headers
        Column[] cols = table.getHeaders();
        int tableLength = (int) table.getNumberOfRows();
        table.close();

        Mineotaur.LOGGER.info(Arrays.toString(cols));
        List<Integer> experimentIDs = new ArrayList<>();
        List<Integer> strainIDs = new ArrayList<>();
//        String[] descriptiveHeader = null;
        Integer filter = null;
        Integer experimentID = null;
        Integer strainID = null;
        Integer descriptiveID = null;
        for (int i = 0; i < cols.length; i++) {
            String colName = cols[i].name;
            if (imageId.equals(colName)) {
                //experimentIDs.add(i);
                experimentID = i;
            } else if (groupId.equals(colName)) {
                //strainIDs.add(i);
                strainID = i;
            }
            if (experimentColumns.contains(colName)) {
                experimentIDs.add(i);
            } else if (groupColumns.contains(colName)) {
                strainIDs.add(i);
            }
            /*if ("PlateID".equals(colName) || "WellID".equals(colName) || "ImageID".equals(colName) || "imageName".equals(colName) || "plateName".equals(colName) || "well".equals(colName)) {
                experimentIDs.add(i);
            }
            else if ("reference".equals(colName) || "name".equals(colName) || "alternativeName".equals(colName) || "description".equals(colName)) {
                strainIDs.add(i);
            }*/
            if (cols[i].getClass().equals(DoubleArrayColumn.class)) {
                descriptiveID = i;
                descriptiveHeader = colName.split(",");
                for (int j = 0; j < descriptiveHeader.length; ++j) {
                    if (descriptiveHeader[j].equals(filterColumn)) {
                        filter = j;
                        break;
                    }
                }
            }
        }
        Mineotaur.LOGGER.info(String.valueOf(strainID));
// Depending on size of table, you may only want to read some blocks.
        long[] columnsToRead = new long[cols.length];
        for (int i = 0; i < cols.length; i++) {
            columnsToRead[i] = i;
        }
        Map<Long, Node> images = new HashMap<>();
        Map<String, Node> strains = new HashMap<>();
        for (int currentBatch = 0; currentBatch < tableLength; currentBatch += rowsToFetch) {

            int batchSize = Math.min(rowsToFetch, tableLength - currentBatch);
            long[] rowSubset = new long[(int) (batchSize)];
            for (int j = 0; j < rowSubset.length; j++) {
                rowSubset[j] = j + currentBatch;
            }
            table = entry.sharedResources().openTable(file);
            Data data = table.slice(columnsToRead, rowSubset); // read the data.
            cols = data.columns;

            String[] strainColumn = ((StringColumn) cols[strainID]).values;
            long[] experimentColumn = ((ImageColumn) cols[experimentID]).values;
            double[][] descriptiveColumn = ((DoubleArrayColumn) cols[descriptiveID]).values;
//            double[] filterColumn = descriptiveColumn[filter];
            if (!filterProps.contains(descriptiveHeader[filter])) {
                filterProps.add(descriptiveHeader[filter]);
            }

            int count = 0;
            Transaction tx = null;
            try {
                tx = db.beginTx();
                for (int i = 0; i < strainColumn.length; ++i) {

                    Mineotaur.LOGGER.info("Line " + i);
                    String sid = strainColumn[i];
                    Node strain = strains.get(sid);
                    if (strain == null) {
                        strain = db.createNode(groupLabel);
                        count++;
                        strain.setProperty("strainID", sid);
                        addProperties(strain, cols, strainIDs, i);
                        strains.put(sid, strain);
                        Mineotaur.LOGGER.info("Strain created with ID: " + sid);
                    } else {
                        Mineotaur.LOGGER.info("Strain loaded with ID: " + sid);
                    }
                    Long imageID = experimentColumn[i];
                    Node experiment = images.get(imageID);
                    if (experiment == null) {
                        experiment = db.createNode(experimentLabel);
                        count++;
                        experiment.setProperty("imageID", imageID);
                        addProperties(experiment, cols, experimentIDs, i);
                        images.put(imageID, experiment);
                        experiment.createRelationshipTo(strain, DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());
                        Mineotaur.LOGGER.info("Experiment created with imageID: " + imageID);
                    } else {
                        Mineotaur.LOGGER.info("Experiment loaded with imageID: " + imageID);
                    }
                    Node descriptive = createDescriptiveNode(descriptiveHeader, descriptiveColumn[i]);
                    descriptive.createRelationshipTo(strain, DefaultRelationships.GROUP_CELL.getRelationshipType());
                    Mineotaur.LOGGER.info("Descriptive node created.");
                    count++;
                    if (count > limit) {
                        count = 0;
                        tx.success();
                        tx.close();
                        tx = db.beginTx();
                    }

                }
            Mineotaur.LOGGER.info("Number of strains: " + String.valueOf(strains.size()));

            } finally {
                tx.success();
                tx.close();
            }


            table.close();
        }
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
    protected List<String> getHits() {
        return hits;
    }


    @Override
    public void generateDatabase() {
        try {
            Mineotaur.LOGGER.info("Establishing connection with the Omero server.");
            establishConnection();
            Mineotaur.LOGGER.info("Processing metadata.");
            processMetadata();
            //processData(db);
            //Mineotaur.LOGGER.info("Creating directories.");

            Mineotaur.LOGGER.info("Loading attachments.");
            processData(db);
            Mineotaur.LOGGER.info("Processing input data.");
            readTable(dataFile);
            Mineotaur.LOGGER.info("Processing label data.");
            labelGenes();
            closeConnection();
            Mineotaur.LOGGER.info(filterProps.toString());
            if (filterProps != null && !filterProps.isEmpty()) {
                Mineotaur.LOGGER.info("Processing descriptive filters.");
                createFilters(db, ggo, groupLabel, descriptiveLabel, DefaultRelationships.GROUP_CELL.getRelationshipType(), filterProps, limit);
            }
            Mineotaur.LOGGER.info("Connecting images to strains.");
            getImageIDs(DefaultRelationships.GROUP_EXPERIMENT.getRelationshipType());

            Mineotaur.LOGGER.info("Creating indices.");
            GraphDatabaseUtils.createIndex(db, groupLabel, groupName);
            Mineotaur.LOGGER.info("Precomputing nodes.");
            precomputedLabel = DynamicLabel.label(group+COLLECTED);
            precomputeOptimized(db, ggo, groupLabel, descriptiveLabel, precomputedLabel, relationships, filterProps, group, descriptive, precomputeLimit);
            Mineotaur.LOGGER.info("Generating property files.");
            storeConfigurationFiles();
            /*generateFeatureNameList();
            generateGroupnameList(db, groupLabel, groupName);
            generatePropertyFile(filterProps, group, groupName, totalMemory, dbPath, omero);*/
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CannotCreateSessionException | PermissionDeniedException e) {
            e.printStackTrace();
        } catch (ServerError serverError) {
            serverError.printStackTrace();
        } finally {
            closeConnection();
        }
        Mineotaur.LOGGER.info("Database generation finished. Start Mineotaur instance with -start " + name);
    }




    @Override
    protected List<String> generateFeatureNameList() {
        List features = Arrays.asList(descriptiveHeader);
        Collections.sort(features);
        return features;
        //FileUtils.saveList(confDir + "mineotaur.features", features);
    }


    @Override
    public void processData(GraphDatabaseService db) {
        Mineotaur.LOGGER.info("Loading annotation table.");
        try {
            getAnnotations("screenID", screen.getId());
        } catch (ServerError serverError) {
            serverError.printStackTrace();
        }
    }

    @Override
    public void processMetadata() {
        try {
            getScreenData();
            name = screen.getName().replaceAll("\\P{Alnum}", "");
            Mineotaur.LOGGER.info("Screen name:" + name);
            confDir = name + FILE_SEPARATOR + CONF + FILE_SEPARATOR;

            dbPath = name + FILE_SEPARATOR + DB + FILE_SEPARATOR;

            createDirs(name, confDir, overwrite);

            startDB(dbPath, totalMemory, cache);

        } catch (ServerError serverError) {
            serverError.printStackTrace();
        }

    }

    protected void addDummyValues() {
        overwrite = true;
        totalMemory = "4G";
        cache = "none";
        groupLabel = DefaultLabels.GROUP.getLabel();
        groupName = "reference";
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
        hits.add(wildTypeLabel.name());
    }

    @Override
    public void labelGenes() {
        labelGenes(labelFile);

//        labelGenes("Input/sysgro_labels.tsv");
    }

    protected void labelGenes(OriginalFile file){
        TablePrx table = null;
        try {
            table = entry.sharedResources().openTable(file);
            Column[] cols = table.getHeaders();
            List<Integer> experimentIDs = new ArrayList<>();
            List<Integer> strainIDs = new ArrayList<>();
//        String[] descriptiveHeader = null;
            Integer filter = null;
            Integer experimentID = null;
            Integer strainID = null;
            Integer labelID = null;
            for (int i = 0; i < cols.length; i++) {
                String colName = cols[i].name;
                if (geneForLabel.equals(colName)) {
                    strainID = i;
                }
                if (hitColumn.equals(colName)) {
                    labelID = i;
                }
            }
            Mineotaur.LOGGER.info("Label ID:" + labelID);
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
            /*Map<Long, Node> images = new HashMap<>();
            Map<String, Node> strains = new HashMap<>();*/
            String[] strainColumn = ((StringColumn) cols[strainID]).values;
            String[] labelColumn =  ((StringColumn) cols[labelID]).values;
            /*long[] experimentColumn = ((ImageColumn) cols[experimentID]).values;
            double[][] descriptiveColumn = ((DoubleArrayColumn)cols[descriptiveID]).values;
            double[] filterColumn = descriptiveColumn[filter];
            filterProps.add(descriptiveHeader[filter]);       */
            int count = 0;
            Map<String, List<String>> labelMap = new HashMap<>();
            for (int i = 0; i < strainColumn.length; ++i) {
                String sid = strainColumn[i];
//                Mineotaur.LOGGER.info(sid);
                if (labelMap.containsKey(sid)) {
                    continue;
                }
                if (sid == null || "".equals(sid) || "empty".equals(sid)) {
                    Mineotaur.LOGGER.warning("Gene id is empty.");
                    continue;
                }
                Mineotaur.LOGGER.info(sid + " " + labelColumn[i]);

                /*System.out.println(sid);
                System.out.println(labelColumn[i]);*/



                String[] labels = labelColumn[i].split(";");
                labelMap.put(sid, Arrays.asList(labels));
                /*f*/
            }
            try (Transaction tx = db.beginTx()) {
                for (String sid: labelMap.keySet()) {
                    Iterator<Node> strains = db.findNodesByLabelAndProperty(groupLabel, groupName, sid).iterator();
                    if (!strains.hasNext()) {
                        Mineotaur.LOGGER.warning("No group object with id " + sid);
                        continue;
                    }
                    Node strain = strains.next();
                    if (!strains.hasNext()) {
                        Mineotaur.LOGGER.warning("Multiple group object with id " + sid);
                    }
                    List<String> labels = labelMap.get(sid);
                    for (String label: labels) {
                        if ("".equals(label)) {
                            strain.addLabel(wildTypeLabel);
                            continue;
                        }
                        strain.addLabel(DynamicLabel.label(label));
                        if (!hits.contains(label)) {
                            hits.add(label);
                        }
                        Mineotaur.LOGGER.info("New label for group object " + sid + ": " + label);
                    }
                }

                tx.success();
            }
                /*try (Transaction tx = db.beginTx()) {
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
                }*/





            table.close();
        } catch (ServerError serverError) {
            serverError.printStackTrace();
        }




    }





}

