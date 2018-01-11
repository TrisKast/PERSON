package arangoDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
 
public class ArangoDBMethods {
	public static int PRETTY_PRINT_INDENT_FACTOR = 4;
	static ArangoDB arangoDB = new ArangoDB.Builder().build();
	
	public static void createDB(String dbName){
		try {
		  arangoDB.createDatabase(dbName);
		  System.out.println("Database created: " + dbName);
		} catch (ArangoDBException e) {
		  System.err.println("Failed to create database: " + dbName + "; " + e.getMessage());
		}
	}
	
	public static void createCollection(String dbName, String collectionName){
		try {
		  CollectionEntity myArangoCollection = arangoDB.db(dbName).createCollection(collectionName);
		  System.out.println("Collection created: " + myArangoCollection.getName());
		} catch (ArangoDBException e) {
		  System.err.println("Failed to create collection: " + collectionName + "; " + e.getMessage());
		}
	}
	
	public static JSONObject createJSONfromXML(String xmlString){
		
		//Input is XMLString, will be converted to JSONObject and then returned
		JSONObject xmlJSONObj = null;
	    try {
	    	xmlJSONObj = XML.toJSONObject(xmlString);
	    } catch (JSONException je) {
	        System.out.println(je.toString());
	    }
	    return xmlJSONObj;   
	}
	
	public static boolean checkForDifferences(JSONObject doc1, JSONObject doc2){
		
		/* Following attributes are and should be unique, so they don't count as changes */
		Set<String> keysDoc1 = doc1.keySet();
		Set<String> keysDoc2 = doc2.keySet();
		keysDoc1.remove("versionnumber");
		keysDoc2.remove("versionnumber");
		keysDoc1.remove("_id");
		keysDoc2.remove("_id");
		keysDoc1.remove("_key");
		keysDoc2.remove("_key");
		keysDoc1.remove("_rev");
		keysDoc2.remove("_rev");
		keysDoc1.remove("hashindex");
		keysDoc2.remove("hashindex");
		
		/* Use the following three lists to count changes */
		ArrayList <String> missing = new ArrayList<String>();
		ArrayList <String> changed = new ArrayList<String>();
		ArrayList <String> added = new ArrayList<String>();
		
		for (String key : keysDoc1) {
			if (! keysDoc2.contains(key)){
				missing.add(key); 
			} else {
				if(!(doc2.getString(key).equals(doc1.getString(key)))){
					changed.add(key);
				}
			}
		}
		for (String key : keysDoc2) {
			if (! keysDoc1.contains(key)){
				added.add(key); 
			}
		}
		
		//System.out.println("Missing: " + missing);
		//System.out.println("Added: " + added);
		//System.out.println("Changed: " + changed);
		
		/* The returned value depends on changes */ 
		if(missing.isEmpty() && changed.isEmpty() && added.isEmpty()){
			return false;
		} else{
			return true;
		}
	}
	
	public static void insertDocument(String dbName, String collectionName, JSONObject JSONObj){
		
		
		/* Create the new document and first of all save it in the db */
		ArangoCollection collection = arangoDB.db(dbName).collection(collectionName);
		DocumentCreateEntity<String> entity = collection.insertDocument(JSONObj.toString());
		String key = entity.getKey();
		BaseDocument objectToAdd = arangoDB.db(dbName).collection(collectionName).getDocument(key,BaseDocument.class);
		
		/* It is new, so it can't be deleted and should be active */
		objectToAdd.addAttribute("Status", "Active");
		try {
		  arangoDB.db(dbName).collection(collectionName).updateDocument(key, objectToAdd);
		} catch (ArangoDBException e) {
		  System.err.println("Failed to update document. " + e.getMessage());
		}
		
		/* Compute custom hashindex to compare to older versions */
		String hashindex = objectToAdd.getAttribute("patientID").toString() + objectToAdd.getAttribute("birthdate").toString() + objectToAdd.getAttribute("gender").toString();
		
		/* Create a JSONObject instance with the status attribute */
		String jsonToAdd = arangoDB.db(dbName).collection(collectionName).getDocument(objectToAdd.getKey(), String.class);
		JSONObject jsonObjToAdd = new JSONObject(jsonToAdd);
		
		/* Catch the keys of all entries with the same hashindex and save them in keylist */
		List<String> keylist = new ArrayList<String>();
		try {
		  String query = "FOR t IN "+collectionName+" FILTER t.hashindex == @hashindex RETURN t";
		  Map<String, Object> bindVars = new MapBuilder().put("hashindex", hashindex).get();
		  ArangoCursor<BaseDocument> cursor = arangoDB.db(dbName).query(query, bindVars, null,
		      BaseDocument.class);
		  cursor.forEachRemaining(aDocument -> {
		    keylist.add(aDocument.getKey());
		  });
		} catch (ArangoDBException e) {
		  System.err.println("Failed to execute query. " + e.getMessage());
		}
		
		
		/* Get the versionnumbers to know the "real" version numbers, apart from the 9999999, also to get the number of already existing versions */
		List<Integer> versionnumberList = new ArrayList<Integer>();
		for(int i = 0; i<keylist.size(); i++){
			BaseDocument docForVersionUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(keylist.get(i),BaseDocument.class);
			int versionnumber = Integer.parseInt((String) docForVersionUpdate.getAttribute("versionnumber"));
			versionnumberList.add(versionnumber);
		}
		
		/* Now there are 3 options: 
		 * Option 1: There is already 1 version of this entry*/ 
		if(versionnumberList.size() == 1){
			for(int i = 0; i<keylist.size(); i++){
				BaseDocument docForVersionUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(keylist.get(i),BaseDocument.class);
				
				/* check if the new version differs from the 9999999 
				 * For this get a JSONObject instance of the older version*/
				 String jsonUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(docForVersionUpdate.getKey(), String.class);
				 JSONObject jsonObjToUpdate = new JSONObject(jsonUpdate);
				 
				 /* If there are differences, older entry is from now on version 1, the new one is 9999999 */
				 if(checkForDifferences(jsonObjToAdd ,jsonObjToUpdate)){
					 sudoUpdateDocument("DB303", "col303",keylist.get(i), "versionnumber", "1");
					 updateDocument("DB303", "col303",key, "hashindex", hashindex);		
					 updateDocument("DB303", "col303",key, "versionnumber", "9999999");
				 /* If there are no differences delete the newest version instanly and keep the rest the same */ 
				 }else{
					 arangoDB.db(dbName).collection(collectionName).deleteDocument(key);
				 }
			}
		/* Option 2: There are already more than 1 version */
		}else if(versionnumberList.size() > 1){
			
			/* Sort the version list; 
			 * Get the second highest number and save it in currentMaxVersion */
			Collections.sort(versionnumberList);
			int currentMaxVersion = 0;
			for(int i = 0; i<keylist.size(); i++){
				BaseDocument docForVersionUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(keylist.get(i),BaseDocument.class);
				if(docForVersionUpdate.getAttribute("versionnumber").equals(String.valueOf(versionnumberList.get(keylist.size()-2)))){
					currentMaxVersion = Integer.parseInt((String) docForVersionUpdate.getAttribute("versionnumber"));
				}
			}
			
			for(int i = 0; i<keylist.size(); i++){
				BaseDocument docForVersionUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(keylist.get(i),BaseDocument.class);
				if(docForVersionUpdate.getAttribute("versionnumber").equals("9999999")){
					
					/* check if the new version differs from the 9999999 
					 * For this get a JSONObject instance of the older version*/
					 String jsonUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(docForVersionUpdate.getKey(), String.class);
					 JSONObject jsonObjToUpdate = new JSONObject(jsonUpdate);
					 
					 /* If there are differences, older entry is from now on version 1, the new one is 9999999 */
					 if(checkForDifferences(jsonObjToAdd ,jsonObjToUpdate)){
						 sudoUpdateDocument("DB303", "col303",keylist.get(i), "versionnumber", Integer.toString(currentMaxVersion + 1));
						 updateDocument("DB303", "col303",key, "hashindex", hashindex);		
						 updateDocument("DB303", "col303",key, "versionnumber", "9999999");
					 /* If there are no differences delete the newest version instanly and keep the rest the same */ 
					 }else{
						 arangoDB.db(dbName).collection(collectionName).deleteDocument(key);
					 }
				}
			}
		/* Option 3: There is no version till now, so just add the new object nice and easy */
		}else{
			 updateDocument("DB303", "col303",key, "hashindex", hashindex);		
			 updateDocument("DB303", "col303",key, "versionnumber", "9999999");
		}
	}
	
	public static void deleteDocument(String dbName, String collectionName, String key){
		BaseDocument docToDelete = arangoDB.db(dbName).collection(collectionName).getDocument(key,BaseDocument.class);
		docToDelete.addAttribute("Status", "Deleted");
		try {
		  arangoDB.db(dbName).collection(collectionName).updateDocument(key, docToDelete);
		} catch (ArangoDBException e) {
		  System.err.println("Failed to update document. " + e.getMessage());
		}
	}
	
	public static void updateDocument(String dbName, String collectionName, String key, String attributeToAdd, String valueOfattributeToAdd){
		BaseDocument docToUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(key,BaseDocument.class);
		if(docToUpdate.getAttribute("Status").equals("Deleted")){
    		System.out.println("Document is already deleted and cannot be updated!");
    	}else{
    		docToUpdate.addAttribute(attributeToAdd, valueOfattributeToAdd);
    		try {
    		  arangoDB.db(dbName).collection(collectionName).updateDocument(key, docToUpdate);
    		} catch (ArangoDBException e) {
    		  System.err.println("Failed to update document. " + e.getMessage());
    		}
    	}
	}
	

	public static void sudoUpdateDocument(String dbName, String collectionName, String key, String attributeToAdd, String valueOfattributeToAdd){
		//needed if a document is deleted, and then REupladed
		
		BaseDocument docToUpdate = arangoDB.db(dbName).collection(collectionName).getDocument(key,BaseDocument.class);
    	docToUpdate.addAttribute(attributeToAdd, valueOfattributeToAdd);
    	try {
    		arangoDB.db(dbName).collection(collectionName).updateDocument(key, docToUpdate);
    	} catch (ArangoDBException e) {
    		System.err.println("Failed to update document. " + e.getMessage());
    	}
    	
	}
}
