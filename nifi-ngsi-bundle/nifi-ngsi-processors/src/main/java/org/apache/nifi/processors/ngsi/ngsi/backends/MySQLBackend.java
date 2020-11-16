package org.apache.nifi.processors.ngsi.ngsi.backends;

import org.apache.nifi.processors.ngsi.ngsi.utils.*;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

public class MySQLBackend {

    public ArrayList listOfFields (String attrPersistence, Entity entity){
        ArrayList<String> aggregation = new ArrayList<>();

        aggregation.add(NGSIConstants.RECV_TIME_TS);
        aggregation.add(NGSIConstants.RECV_TIME);
        aggregation.add(NGSIConstants.FIWARE_SERVICE_PATH);
        aggregation.add(NGSIConstants.ENTITY_ID);
        aggregation.add(NGSIConstants.ENTITY_TYPE);

        if (attrPersistence.compareToIgnoreCase("row")==0){
            aggregation.add(NGSIConstants.ATTR_NAME);
            aggregation.add(NGSIConstants.ATTR_TYPE);
            aggregation.add(NGSIConstants.ATTR_VALUE);
            aggregation.add(NGSIConstants.ATTR_MD);
        }else if(attrPersistence.compareToIgnoreCase("column")==0){
            //TBD
            ArrayList<Attributes> attributes = entity.getEntityAttrs();
            if (attributes != null && !attributes.isEmpty()) {
                for (Attributes attribute : attributes) {
                    String attrName = attribute.getAttrName();
                    aggregation.add(attrName);
                    aggregation.add(attrName + "_md");
                } // for
            } // if
            System.out.println("column");
        }
        return aggregation;
    }

    public String getValuesForInsert(Entity entity, long creationTime, String fiwareServicePath, String attrPersistence) {
        String valuesForInsert = "";
        if ((NGSIConstants.ATTR_PER_ROW).equalsIgnoreCase(attrPersistence)){
            for (int i = 0; i < entity.getEntityAttrs().size(); i++) {
                if (i == 0) {
                    valuesForInsert += "(";

                } else {
                    valuesForInsert += ",(";
                } // if else

                valuesForInsert += "'" + creationTime + "'";
                valuesForInsert += ",'" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(creationTime) + "'";
                valuesForInsert += ",'" + fiwareServicePath.replace("/", "") + "'";
                valuesForInsert += ",'" + entity.getEntityId() + "'";
                valuesForInsert += ",'" + entity.getEntityType() + "'";
                valuesForInsert += ",'" + entity.getEntityAttrs().get(i).getAttrName() + "'";
                valuesForInsert += ",'" + entity.getEntityAttrs().get(i).getAttrType() + "'";
                valuesForInsert += ",'" + entity.getEntityAttrs().get(i).getAttrValue() + "'";
                if (entity.getEntityAttrs().get(i).getAttrMetadata() != null) {
                    valuesForInsert += ",'" + entity.getEntityAttrs().get(i).getMetadataString() + "'";
                } else {
                    valuesForInsert += ",'[]'";
                }
                valuesForInsert += ")";

            } // for
        } else if((NGSIConstants.ATTR_PER_COLUMN).equalsIgnoreCase(attrPersistence)) {
            int i=0;
            valuesForInsert += "(";
            valuesForInsert += "'" + creationTime + "'";
            valuesForInsert += ",'" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(creationTime) + "'";
            valuesForInsert += ",'" + fiwareServicePath.replace("/", "") + "'";
            valuesForInsert += ",'" + entity.getEntityId() + "'";
            valuesForInsert += ",'" + entity.getEntityType() + "'";
            for (Attributes attribute : entity.getEntityAttrs()) {
                valuesForInsert += ",'" + attribute.getAttrValue() + "'";
                if ( attribute.getMetadataString() != null && !attribute.getMetadataString().isEmpty()) {
                    valuesForInsert += ",'" + attribute.getMetadataString() + "'";
                } else {
                    valuesForInsert += ",'[]'";
                }
            } //for
            valuesForInsert += ")";
        }

        return valuesForInsert;
    } // getValuesForInsert


    public String getFieldsForCreate(String attrPersistence, Entity entity) {
        Iterator it = listOfFields(attrPersistence, entity).iterator();
        String fieldsForCreate = "(";
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                fieldsForCreate += (String) it.next() + " text";
                first = false;
            } else {
                fieldsForCreate += "," + (String) it.next() + " text";
            } // if else
        } // while

        return fieldsForCreate + ")";
    } // getFieldsForCreate

    public String getFieldsForInsert(String attrPersistence, Entity entity) {

        String fieldsForInsert = "(";
        boolean first = true;
        Iterator it = listOfFields(attrPersistence, entity).iterator();
        while (it.hasNext()) {
            if (first) {
                fieldsForInsert += (String) it.next();
                first = false;
            } else {
                fieldsForInsert += "," + (String) it.next();
            } // if else
        } // while

        return fieldsForInsert + ")";
    } // getFieldsForInsert

    public String buildDbName(String service,boolean enableEncoding,boolean enableLowercase) throws Exception{
        String dbName="";
        if (enableEncoding) {
            dbName = NGSICharsets.encodeMySQL((enableLowercase)?service.toLowerCase():service);
        } else {
            dbName = NGSICharsets.encode((enableLowercase)?service.toLowerCase():service, false, true);
        } // if else

        if (dbName.length() > NGSIConstants.MYSQL_MAX_NAME_LEN) {
            Exception e = new Exception("Building database name '" + dbName+ "' and its length is greater than " + NGSIConstants.MYSQL_MAX_NAME_LEN);
            throw e;
        } // if
        return dbName;
    }

    public String createDb(String dbName) {
        String query = "create database if not exists `" + dbName + "`;";
        return query;
    }

    public String createTable(String tableName, String attrPersistence, Entity entity){

        String query= "create table if not exists `" + tableName + "`" + getFieldsForCreate(attrPersistence, entity) + ";";
        return query;
    }

    public String buildTableName(String fiwareServicePath,Entity entity, String dataModel, boolean enableEncoding, boolean enableLowercase) throws Exception {
        String tableName="";
        String servicePath=(enableLowercase)? fiwareServicePath.toLowerCase():fiwareServicePath;
        String entityId = (enableLowercase) ? entity.getEntityId().toLowerCase() : entity.getEntityId();
        String entityType = (enableLowercase) ? entity.getEntityType().toLowerCase() : entity.getEntityType();

        if (enableEncoding) {
            switch (dataModel) {
                case "db-by-service-path":
                    tableName = NGSICharsets.encodeMySQL(servicePath);
                    break;
                case "db-by-entity":
                    tableName = NGSICharsets.encodeMySQL(servicePath)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeMySQL(entityId)
                            + CommonConstants.CONCATENATOR
                            + NGSICharsets.encodeMySQL(entityType);
                    break;
                default:
                    System.out.println("Unknown data model '" + dataModel + "'. Please, use dm-by-service-path, dm-by-entity or dm-by-attribute");
                    break;
            } // switch
        } else {
            switch (dataModel) {
                case "db-by-service-path":
                    if ("/".equals(servicePath)) {
                        System.out.println("Default service path '/' cannot be used with "
                                + "dm-by-service-path data model");
                    } // if

                    tableName = NGSICharsets.encode(servicePath, true, false);
                    break;
                case "db-by-entity":
                    String truncatedServicePath = NGSICharsets.encode(servicePath, true, false);
                    tableName = (truncatedServicePath.isEmpty() ? "" : truncatedServicePath + '_')
                            + NGSICharsets.encode(entityId, false, true) + "_"
                            + NGSICharsets.encode(entityType, false, true);
                    break;
                default:
                    System.out.println("Unknown data model '" + dataModel + "'. Please, use DMBYSERVICEPATH, DMBYENTITY or DMBYATTRIBUTE");
                    break;
            } // switch
        } // if else

        if (tableName.length() > NGSIConstants.MYSQL_MAX_NAME_LEN) {
            throw new Exception("Building table name '" + tableName
                    + "' and its length is greater than " + NGSIConstants.MYSQL_MAX_NAME_LEN);
        } // if

        return tableName;
    }

    public String insertQuery (Entity entity, long creationTime, String fiwareServicePath, String tableName, String dataModel){
        String query="Insert into `" + tableName + "` " +this.getFieldsForInsert(dataModel, entity)+ " values " +this.getValuesForInsert(entity, creationTime, fiwareServicePath, dataModel);
        System.out.println("MYSQL_BACKEND query is: "+query);
        return query;
    }
}
