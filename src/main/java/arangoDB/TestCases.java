package arangoDB;

public class TestCases {
	public static void main(final String[] args) {
		/* This needs to be done only once */
		//createDB("DB303");
		//createCollection("DB303","col303");
		
		/* Some dummy data */
		
		/* Five different datasets */
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc1</patientID> <birthdate>01/01/2001</birthdate> <gender>male</gender> <weight> 50kg </weight> <disease>cancer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc2</patientID> <birthdate>02/02/2002</birthdate> <gender>female</gender> <weight> 60kg </weight> <disease>alzheimer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc3</patientID> <birthdate>03/03/2003</birthdate> <gender>male</gender> <weight> 70kg </weight> <disease>parkison</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc4</patientID> <birthdate>04/04/2004</birthdate> <gender>female</gender> <weight> 80kg </weight> <disease>morbuschron</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc5</patientID> <birthdate>05/06/2005</birthdate> <gender>male</gender> <weight> 90kg </weight> <disease>multiplesclerosis</disease>"));
		
		/* Those 3 are already in the db so no new documents should be created */
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc1</patientID> <birthdate>01/01/2001</birthdate> <gender>male</gender> <weight> 50kg </weight> <disease>cancer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc2</patientID> <birthdate>02/02/2002</birthdate> <gender>female</gender> <weight> 60kg </weight> <disease>alzheimer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc3</patientID> <birthdate>03/03/2003</birthdate> <gender>male</gender> <weight> 70kg </weight> <disease>parkison</disease>"));

		
		/* Those two have changed attributes, so they should update the existing versions */
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc2</patientID> <birthdate>02/02/2002</birthdate> <gender>female</gender> <weight> 65kg </weight> <disease>alzheimer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc5</patientID> <birthdate>05/06/2005</birthdate> <gender>male</gender> <weight> 95kg </weight> <disease>multiplesclerosis</disease>"));
		
		/* Those two datesets have new attributes so they should upaate the existing versions */
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc2</patientID> <birthdate>02/02/2002</birthdate> <gender>female</gender> <weight> 65kg </weight> <eyecolor> green </eyecolor> <disease>alzheimer</disease>"));
		ArangoDBMethods.insertDocument("DB303", "col303", ArangoDBMethods.createJSONfromXML("<patientID>abc5</patientID> <birthdate>05/06/2005</birthdate> <gender>male</gender> <weight> 95kg </weight> <haircolor> brown </haircolor> <disease>multiplesclerosis</disease>"));
	
	}
}
