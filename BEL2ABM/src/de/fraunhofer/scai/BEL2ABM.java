package de.fraunhofer.scai;
/**
 * 
 */
//package de.fraunhofer.scai.BEL2ABM;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openbel.framework.api.KamStore;
import org.openbel.framework.api.KamStoreImpl;
import org.openbel.framework.common.InvalidArgument;
import org.openbel.framework.common.cfg.SystemConfiguration;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;
import org.openbel.framework.core.df.DBConnection;
import org.openbel.framework.core.df.DatabaseService;
import org.openbel.framework.core.df.DatabaseServiceImpl;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;
import org.openbel.framework.internal.KamDbObject;
import org.openbel.framework.internal.KAMStoreDaoImpl.Annotation;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelStatement;
import org.openbel.framework.tools.kamstore.KamSummary;
import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamEdge;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.KamStoreException;
import org.openbel.framework.api.NodeFilter;
import org.openbel.framework.internal.KAMCatalogDao.KamInfo;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fraunhofer.scai.Region.ProvenanceType;




/**
 * @author Michaela Michi Guendel, Fraunhofer SCAI
 *
 */
//public class BEL2ABM {
public class BEL2ABM {
	
	private SystemConfiguration systemConfiguration;
	private KamStore kamStore;
	private DBConnection dbConnection;
	
	private List<KamNode> allAbundanceNodes;              //retrieve BelTerms via kamStore.getSupportingTerms(kamnode, true)
	private List<KamNode> allBioProcessNodes;
	private List<KamNode> allActivityNodes;
	private HashMap<String,String> abundanceIdTermMapping;
	private HashMap<String,String> processIdTermMapping;
	private HashMap<KamNode,OntClass> allAbundances;      //all agents contained in the kam
	private HashMap<KamNode,OntClass> agents;             //agents contained in KAM and ontology
	private HashMap<KamNode,OntClass> chosenAgents;       //agents to be used for display and analysis in the ABM
	private HashMap<KamNode,OntClass> nodeClassMapping;
	private HashMap<KamNode, ArrayList<Region>> agentToRegionMap;
	private HashMap<KamNode, ArrayList<Region>> agentProducedInMap;
	protected Ontology onto;
	private ABMCode abmcode;
	
	private ArrayList<Region> regions;
	
	private static final String logging = "BEL2ABM.log";
	private static final String settings = "BEL2ABM.ini";

	private static final String HUMAN_TAX_ID = "9606";
	private static final String MOUSE_TAX_ID = "10090";
	private static final String RAT_TAX_ID = "10116";
	
	private static String ontopath = "";
	private static String agentRelation;  //eg ClassA has_role (==agentRelation) some 
	private static String agentClass;     //                                          agent_role (==agentClass)
	private static String belAnnoProp;    //"BELterm" in HuPSON; ie, annotation property used to annotate onto classes with BEL term
	private static String compositeAbundanceURI;
	private static String complexAbundanceURI;
	private static String defaultAbundanceURI;
	private static String defaultProcessURI;
	private static String proteinAbundanceURI;
	private static String locatedInURI;
	private static String producedInURI;
	private static List<String> locatedInAnnotationNames = new ArrayList<String>();		//where to find the regional information in the KAM, eg: Anatomy

	/**
	 * Constructs the KamSummarizer
	 */
	public BEL2ABM() {
		this.allAbundanceNodes = new ArrayList<KamNode>();
		this.allBioProcessNodes = new ArrayList<KamNode>();
		this.allActivityNodes = new ArrayList<KamNode>();
		this.onto = new Ontology();
		this.abmcode = new ABMCode(true);
		this.allAbundances = new HashMap<KamNode,OntClass>();
		this.agents = new HashMap<KamNode,OntClass>();
		this.chosenAgents = new HashMap<KamNode,OntClass>();
		this.nodeClassMapping = new HashMap<KamNode,OntClass>();
		this.abundanceIdTermMapping = new HashMap<String,String>();
		this.processIdTermMapping = new HashMap<String,String>();
		this.agentToRegionMap = new HashMap<KamNode, ArrayList<Region>> ();
		this.agentProducedInMap = new HashMap<KamNode, ArrayList<Region>> ();
		this.regions = new ArrayList<Region>();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// https://github.com/OpenBEL/openbel-framework-examples/tree/master/java-api/com/selventa/belframework/api/examples
		
		//from example code  https://github.com/OpenBEL/openbel-framework-examples/blob/master/java-api/com/selventa/belframework/api/examples/KamSummarizer.java
		final StringBuilder bldr = new StringBuilder();
		bldr.append("\n");
		bldr.append(": KAM Summarizer\n");
		bldr.append("Copyright (c) 2011-2012, Selventa. All Rights Reserved.\n");
		bldr.append("\n");
		System.out.println(bldr.toString());
		
		String kamName = null;
		boolean listCatalog = false;
		
		BEL2ABM myBEL2ABM = new BEL2ABM();
		
		//read the arguments; initialize
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("-l") || arg.equals("--list-catalog")) {
				listCatalog = true;
			} else if (arg.equals("-k") || arg.equals("--kam-name")) {
				if ((i + 1) < args.length) {
					kamName = args[i + 1];
				} else {
					printUsageThenExit();
				}
			}
			if (arg.equalsIgnoreCase("-ABMCode")){
				myBEL2ABM.getAbmcode().create(args[i + 1]);
				Utils.appendToFile(new StringBuffer().append("ABM code output file: "+args[i + 1]+ " \n"), logging);
			}
			if (arg.equalsIgnoreCase("-v")){
				myBEL2ABM.getAbmcode().setVerbous(true);
				Utils.appendToFile(new StringBuffer().append("Verbous output  \n"), logging);
			}
		}

		if (kamName == null && !listCatalog) {
			printUsageThenExit();
		}
		readSettings();

		

		
		
		//start the program!
		try {
			myBEL2ABM.run(listCatalog, kamName);
		} catch (Exception e) {
			System.out.println("Error running BEL2ABM - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** 
	 * reads settings file BEL2ABM.ini
	 * attention: there is another readsettings method in ABMCode.java
	 * TODO merge
	 * @return
	 */
	private static List<String> readSettings() {
		Utils.writeToFile(new StringBuffer().append("Reading settings file: "+settings+" \n"), logging);
		List<String> settArray = new ArrayList<String>();
		try {
			System.out.println("try reading ini from jar: "+settings);
			settArray = Utils.readLinesFromJar(settings);
			Utils.appendToFile(new StringBuffer().append("\nOK. settings file "+settings+" read from " + "."+File.separator+settings+". \n"), logging);
		} catch (IOException e) {
			try {
				System.out.println("trying to read ini from "+ "files"+File.separator+ settings);
				settArray = Utils.readLines( "files"+File.separator+ settings);
				System.out.println("  ini file read from "+ "files"+File.separator+ settings);
			} catch (FileNotFoundException e2){
				System.err.print("ini not found");
				Utils.appendToFile(new StringBuffer().append(e.toString()
						+"\nError: settings file "+settings+" not found. Checked in "+"."+File.separator+settings+" and "+ 
						"files"+File.separator + settings+" Exiting. \n"), logging);
				System.exit(-1);
			}
		}
		String command="";
		for (String settingLine : settArray){
			try {
				command = settingLine.split("\t")[0];
				if (command.startsWith("-")){
					switch (command){
					case("-onto"):
							 ontopath = settingLine.split("\t")[1];
							 Utils.appendToFile(new StringBuffer().append("ontology path: "+ontopath+" \n"), logging);
							 break;
					case("-agentrelation"):
						agentRelation = settingLine.split("\t")[1];
						 Utils.appendToFile(new StringBuffer().append("agent relation: "+agentRelation+" \n"), logging);
						 break;
					case("-agentclass"):
						agentClass = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("agent class: "+agentClass+" \n"), logging);
					 	break;
					case("-BELTermAnnotationProperty"):
						belAnnoProp = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("BEL annotation property: "+belAnnoProp+" \n"), logging);
					 	break;
					case("-defaultProcess"):
						defaultProcessURI = settingLine.split("\t")[1];
				 		Utils.appendToFile(new StringBuffer().append("default process URI: "+defaultProcessURI+" \n"), logging);
				 		break;
					case("-defaultAbundance"):
						defaultAbundanceURI = settingLine.split("\t")[1];
				 		Utils.appendToFile(new StringBuffer().append("default abundance URI: "+defaultAbundanceURI+" \n"), logging);
				 		break;
					case("-proteinAbundance"):
						proteinAbundanceURI = settingLine.split("\t")[1];
				 		Utils.appendToFile(new StringBuffer().append("protein abundance URI: "+proteinAbundanceURI+" \n"), logging);
				 		break;
					case("-complexAbundance"):
						complexAbundanceURI = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("complex abundance URI: "+complexAbundanceURI+" \n"), logging);
					 	break;
					case("-compositeAbundance"):
						compositeAbundanceURI = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("composite abundance URI: "+compositeAbundanceURI+" \n"), logging);
					 	break;
					case("-locatedIn"):
						locatedInURI = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("located in URI: "+locatedInURI+" \n"), logging);
					 	break;
					case("-producedIn"):
						producedInURI = settingLine.split("\t")[1];
					 	Utils.appendToFile(new StringBuffer().append("produced in URI: "+producedInURI+" \n"), logging);
					 	break;
					case("-locatedInAnnotationName"):
						addLocatedInAnnotationName(settingLine.split("\t")[1]);
					 	Utils.appendToFile(new StringBuffer().append("located_in annotation name: "+settingLine.split("\t")[1]+" \n"), logging);
					 	break;
					}
				}
			} catch (IndexOutOfBoundsException iobe){
				Utils.appendToFile(new StringBuffer().append("Warning: Check "+logging+" settings file, at least one of the settings has not"
						+ " been set correctly.  \n"), logging);
			}
		}
		return null;
	}

	
	public void run(boolean listCatalog, String kamName2) throws IOException, SQLException, KamStoreException {

		String kamName = kamName2;
		// connect to the KAM store
		setUpKamStore();

		// list the available kams in the kam store
		if (listCatalog) {
			List<KamInfo> kamInfos = kamStore.readCatalog();
			printKamCatalogSummary(kamInfos);
		}
		Utils.appendToFile(new StringBuffer().append("loading ontology...\n"), logging);
		//load ontology
		this.onto.loadOnto_RDF_XML(ontopath);
		Utils.appendToFile(new StringBuffer().append("finished loading ontology...\n"), logging);
		
		try {
			Kam kam;
			//TODO eliminate duplicate statements before starting the whole process
			if (kamName != null) {
				// Look up the requested KAM and summarize.
				kam = this.kamStore.getKam(kamName);
				KamSummary summary = summarizeKam(kam);
				printKamSummary(summary);
				
				
				//################################################################################################################################
				//GO THROUGH THE WHOLE NETWORK AND LOOK FOR ALL ABUNDANCES -->  allAbundances
				//################################################################################################################################
				this.allAbundanceNodes = getAllAgents(kam);
				for (KamNode nexInAllNodes : this.allAbundanceNodes){
					this.allAbundances.put(nexInAllNodes, null);
				}
				
				
				
				//################################################################################################################################
				//FILL HASHMAP abundanceIdTermMapping  eg for abundance(37): abundance(Cell:"native cell")
				//this wouldn't have been necessary: kamStore.getSupportingTerms(KamNode, true) would have done the job
				//################################################################################################################################
				for (KamNode curKamNode : this.allAbundanceNodes) {
					List<BelTerm> terms = this.kamStore.getSupportingTerms(curKamNode, true);
					for (BelTerm t : terms){  //I don't see why there should be more than 1 term??!!
						this.abundanceIdTermMapping.put(curKamNode.toString(), t.toString());
						Utils.appendToFile(new StringBuffer().append("\t added to abundanceIdTermMapping HashMap: key: "+
								curKamNode.toString()+ " --- value: "+t.toString()+"\n"), logging);
					}
				}
				
				
				//################################################################################################################################
				//go through KAM and check all abundances and bioprocesses for dis-connection to ontology
				//and add nodes to ontology (those connected via isA,                   (hasMember(s) and hasComponent(s) not yet implemented)
				//so these entities will be treated as what they are (cells, processes, proteins, complexes, composites, ...)
				//################################################################################################################################
				NodeIterator vIt;  //ontology RDF node iterator
				RDFNode c; 
				ExtendedIterator<OntClass> allClasses;
				OntClass cl;
				boolean found;
				ArrayList<KamNode> nodesNotInOnto = new ArrayList<KamNode>();
				
				for (KamNode curKamNode : allAbundanceNodes) {
					found = false;
					List<BelTerm> terms = kamStore.getSupportingTerms(curKamNode, true);
					allClasses = this.onto.getM().listClasses();
					for (BelTerm t : terms){
						while (allClasses.hasNext()){
							cl = allClasses.next();
							vIt = this.onto.listPropertyValues(cl, this.onto.getAnnoProp(belAnnoProp));
							while (vIt.hasNext()){
								c = vIt.next();
								if (t.toString().equals(c.toString())){
									//then a mapping to the ontology has been found
									found = true;
								}
							}
						}
					}
						
					//if no mapping to ontology has been found, record curKamNode
					if (!found){
							nodesNotInOnto.add(curKamNode);
					}
				}
				//check all processes, too
				for (KamNode curProcess : this.getAllBioProcesses(kam)){
					found = false;
					//theBelterm = this.abundanceIdTermMapping.get(curKamNode.getLabel());
					List<BelTerm> terms = kamStore.getSupportingTerms(curProcess, true);
					allClasses = this.onto.getM().listClasses();
					for (BelTerm t : terms){
						while (allClasses.hasNext()){
							cl = allClasses.next();
							vIt = this.onto.listPropertyValues(cl, this.onto.getAnnoProp(belAnnoProp));
							while (vIt.hasNext()){
								c = vIt.next();
								if (t.toString().equals(c.toString())){
									//then a mapping to the ontology has been found
									found = true;
								}
							}
						}
					}
					//if no mapping to ontology has been found, record curProcess
					if (!found){
							nodesNotInOnto.add(curProcess);
					}
				}
				
				//TODO  also use hasMember(s) or hasComponent(s) to connect the agents to ontology 
				//TODO can hascomponent etc somehow be translated to the agent's behaviour?? 
				
				//screen the KAM whether there is some isA, otherwise use the defaultAbundanceURI
				//to establish the connection to the ontology
				ArrayList<KamNode> nodesAdded = new ArrayList<KamNode>();
				Utils.appendToFile(new StringBuffer().append("\nADDING CLASSES to ontology: "+"\n"), logging);
				Collection<KamEdge> edges = kam.getEdges();
				for (KamEdge e : edges ){
					if(e.getRelationshipType() == RelationshipType.IS_A) {
						for (KamNode node : nodesNotInOnto){
							if (kamStore.getSupportingTerms(e.getSourceNode(), true).equals(kamStore.getSupportingTerms(node, true))){
								//an isA statement has been found in the BEL code! class to be inserted as child of e.getTargetNode
								//nodeToBeInserted isA e.getTargetNode
								String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
								String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_"); 
								OntClass addClass = this.onto.createClass("http://tmpns/" + id);
								String annotationString = kamStore.getSupportingTerms(e.getTargetNode(), true).get(0).toString();  //the BELTerm annotation eg abundance(MSO:"T cell")
								OntClass parentClass = this.onto.getOntClassFromAnnotationProperty(annotationString, this.onto.getAnnoProp(belAnnoProp));
								if (id.contains(":"))
									id = id.substring(id.indexOf(":")+1);
								addClass.addLabel(id.replaceAll("_", " "), null);
								addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
								this.onto.addSubClass(parentClass,addClass);
								Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
										addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
										+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
								nodesAdded.add(node);
							}
							else if (kamStore.getSupportingTerms(e.getTargetNode(), true).equals(kamStore.getSupportingTerms(node, true))){
								//an isA statement has been found in the BEL code! class to be inserted as parent of e.getSourceNode
								//e.getSourceNode isA nodeToBeInserted
								String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
								String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_"); 
								OntClass addClass = this.onto.createClass("http://tmpns/" + id);
								String annotationString = kamStore.getSupportingTerms(e.getSourceNode(), true).get(0).toString();  //the BELTerm annotation eg abundance(MSO:"T cell")
								OntClass childClass = this.onto.getOntClassFromAnnotationProperty(annotationString, this.onto.getAnnoProp(belAnnoProp));
								if (id.contains(":"))
									id = id.substring(id.indexOf(":")+1);
								addClass.addLabel(id.replaceAll("_", " "), null);
								addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
								this.onto.addSubClass(addClass,childClass);
								Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
										addClass.getLabel(null)+ " --- parent of: "+childClass.getLabel(null)
										+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
								nodesAdded.add(node);
							}
						}
					}
				}
				
				//################################################################################################################################
				//ADD COMPLEX AND COMPOSITE ABUNDANCES TO THE ONTOLOGY
				//AS SUBCLASSES OF complexAbundanceURI and compositeAbundanceURI
				//use URIs from .ini setting file for default abundances and process that are still disconnected 
				//################################################################################################################################
				for (KamNode node : nodesNotInOnto){
					if (!nodesAdded.contains(node) && node.getLabel().startsWith("compositeAbundance")){
						if (Ontology.getOntClassFromURIString(this.onto.getM(), compositeAbundanceURI) != null){
							String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
							String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" (),]", "_"); 
							OntClass addClass = this.onto.createClass("http://tmpns/" + id);
							addClass.addLabel(nodelabel, null);
							addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
							OntClass parentClass = Ontology.getOntClassFromURIString(this.onto.getM(), compositeAbundanceURI); 
							this.onto.addSubClass(parentClass,addClass);
							Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
									addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
									+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
							nodesAdded.add(node);
						}
					}
					if (!nodesAdded.contains(node) && node.getLabel().startsWith("complexAbundance")){
						if (Ontology.getOntClassFromURIString(this.onto.getM(), complexAbundanceURI) != null){
							String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
							String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_"); 
							OntClass addClass = this.onto.createClass("http://tmpns/" + id);
							addClass.addLabel(nodelabel, null);
							addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
							OntClass parentClass = Ontology.getOntClassFromURIString(this.onto.getM(), complexAbundanceURI); 
							this.onto.addSubClass(parentClass,addClass);
							Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
									addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
									+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
							nodesAdded.add(node);
						}
					}
					//if it's a protein abundance
					if (!nodesAdded.contains(node) && Utils.isProteinAbundance(node)){
						if (Ontology.getOntClassFromURIString(this.onto.getM(), proteinAbundanceURI) != null){
							String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
							String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_"); 
							OntClass addClass = this.onto.createClass("http://tmpns/" + id);
							id = nodelabel.charAt(0)+"."+id;
							addClass.addLabel(nodelabel, null);
							addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
							OntClass parentClass = Ontology.getOntClassFromURIString(this.onto.getM(), proteinAbundanceURI); 
							this.onto.addSubClass(parentClass,addClass);
							Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
									addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
									+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
							nodesAdded.add(node);
						}
					}
					//if it's a default abundance
					if (!nodesAdded.contains(node) && Utils.isAbundance(node)){
						if (Ontology.getOntClassFromURIString(this.onto.getM(), defaultAbundanceURI) != null){
							String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
							String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_");
							id = nodelabel.charAt(0)+"."+id;
							OntClass addClass = this.onto.createClass("http://tmpns/" + id);
							addClass.addLabel(nodelabel, null);
							addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
							OntClass parentClass = Ontology.getOntClassFromURIString(this.onto.getM(), defaultAbundanceURI); 
							this.onto.addSubClass(parentClass,addClass);
							Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
									addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
									+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
							nodesAdded.add(node);
						}
					}
					//or it's a default process
					if (!nodesAdded.contains(node) && Utils.isBioProcess(node)){
						if (Ontology.getOntClassFromURIString(this.onto.getM(), defaultProcessURI) != null){
							String nodelabel = kamStore.getSupportingTerms(node, true).get(0).toString(); 
							String id = nodelabel.substring(nodelabel.indexOf("(" )+1, nodelabel.length()).replaceAll("[\" ()]", "_"); 
							OntClass addClass = this.onto.createClass("http://tmpns/"+ id); 
							addClass.addLabel(nodelabel, null);
							addClass.addProperty(this.onto.getAnnoProp(belAnnoProp), nodelabel);
							OntClass parentClass = Ontology.getOntClassFromURIString(this.onto.getM(), defaultProcessURI); 
							this.onto.addSubClass(parentClass,addClass);
							Utils.appendToFile(new StringBuffer().append("class added to ontology: "+
									addClass.getLabel(null)+ " --- child of: "+parentClass.getLabel(null)
									+" with annotation property "+nodelabel+"("+belAnnoProp+")\n"), logging);
							nodesAdded.add(node);
						}
					}
				}
				
				
				
				//################################################################################################################################
				//go through KAM edges and check whether the BEL doc contains isA assertions that are not contained in the ontology
				//add these subclass-of relations
				//################################################################################################################################
				Utils.appendToFile(new StringBuffer().append("\nADDING additional subclassOf statements to ontology: "+"\n"), logging);
				edges = kam.getEdges();
				List<KamNode> allNodes = this.allAbundanceNodes;
				allNodes.addAll(this.allBioProcessNodes);
				for (KamEdge e : edges ){
					if(e.getRelationshipType() == RelationshipType.IS_A || e.getRelationshipType() == RelationshipType.SUB_PROCESS_OF) {
						String annotationString = kamStore.getSupportingTerms(e.getSourceNode(), true).get(0).toString();  //the BELTerm annotation eg abundance(MSO:"T cell")
						//find sourceNode ontClass in the ontology
						OntClass sourceCl = this.onto.getOntClassFromAnnotationProperty(annotationString, this.onto.getAnnoProp(belAnnoProp));
						annotationString = kamStore.getSupportingTerms(e.getTargetNode(), true).get(0).toString();  //the BELTerm annotation eg abundance(MSO:"T cell")
						//find targetNode ontClass in the ontology
						OntClass targetCl = this.onto.getOntClassFromAnnotationProperty(annotationString, this.onto.getAnnoProp(belAnnoProp));
						if (targetCl != null && sourceCl != null){
							this.onto.addSubClass(targetCl,sourceCl);
							Utils.appendToFile(new StringBuffer().append("subclassOf added to ontology: "+
									targetCl.getLabel(null)+ " --- addSubclass: "+sourceCl.getLabel(null)+"\n"), logging);
						}
					}
				}
				
				//#############################################################################
				//look up in the ontology which classes may be used as agents
				this.onto.createPossibleAgentsList(agentRelation, agentClass);
				//#############################################################################
				for (KamNode curKamNode : this.allAbundanceNodes) {
					List<BelTerm> terms = this.kamStore.getSupportingTerms(curKamNode, true);
					
					//################################################################################################################################
					//COMPARE KAM ABUNDANCE NODES WITH ONTOLOGY AND REDUCE TO THOSE 
					//THAT CAN BE USED AS AGENTS ACCORDING TO THE ONTOLOGY
					//COPY REMAINING NODES TO agents KAMNODE-ontclass HASHMAP
					//################################################################################################################################
					NodeIterator valueIt;  //ontology RDF node iterator
					RDFNode cur;     
					
					for (BelTerm t : terms){  //I don't see why there should be more than 1 term??!!
						for (OntClass ontcl : this.onto.getPossibleAgentClasses()){  //das funktioniert schon
							//via class.annotationprop "BELterm"
							valueIt = this.onto.listPropertyValues(ontcl, this.onto.getAnnoProp(belAnnoProp));
							while (valueIt.hasNext()){
								cur = valueIt.next();
								//System.out.println(cur.toString());
								if (t.toString().equals(cur.toString())){
									this.agents.put(curKamNode, ontcl);
									//update ontology information in this.allPossibleAgents
									//System.out.println(kamStore.getSupportingTerms(curKamNode).get(0)+"--"+ontcl.getLabel(null)+"\n---\n");
									this.allAbundances.put(curKamNode, ontcl);
								}
							}
						}
					}
				}

				
				//################################################################################################################################
				//GO THROUGH THE WHOLE ONTOLOGY AND KAM NETWORK AND 
				//GENERATE NODECLASSMAPPING = HASHMAP KAMNODE --> CORRESPONDING ONTCLASS (FOR LATER LOOK-UP)
				//this is kind of a double implementation, same is done by Utils.getOntClasses(key, onto, kamName2, processIdTermMapping, kamName)
				//nodeClassMapping contains ALL mappings, not only those of the possible agents according to the ontology!! 
				//################################################################################################################################
				Utils.appendToFile(new StringBuffer().append("\nGenerating KamNode - OntClass mapping:\n"), logging);
				NodeIterator valueIt;  //ontology RDF node iterator
				RDFNode cur; 
				allClasses = this.onto.getM().listClasses();
				OntClass ontcl;
				while (allClasses.hasNext()){
					ontcl = allClasses.next();
					valueIt = this.onto.listPropertyValues(ontcl, this.onto.getAnnoProp(belAnnoProp));
					while (valueIt.hasNext()){
						cur = valueIt.next();
						for (KamNode curKamNode : allAbundanceNodes) {
							List<BelTerm> terms = kamStore.getSupportingTerms(curKamNode, true);
							for (BelTerm t : terms){
								if (t.toString().equals(cur.toString())){
									this.nodeClassMapping.put(curKamNode, ontcl);
									Utils.appendToFile(new StringBuffer().append("added to NodeClassMapping HashMap: key: "+curKamNode+"="+cur.toString()+
											" --- value: "+ontcl.getURI()+"="+ontcl.getLabel(null)+"\n"), logging);
								}
							}
						}
					}
				}
				//logging
				Utils.appendToFile(new StringBuffer().append("\n\nAll possible agents contained in Kam: \n"), logging);
				for (KamNode n : this.agents.keySet()){
					Utils.appendToFile(new StringBuffer().append("\t key: "+n.toString()), logging);
					if(this.nodeClassMapping.get(n) != null){
						Utils.appendToFile(new StringBuffer().append("\t value: "+this.nodeClassMapping.get(n).getLabel(null)
								+" - "+this.nodeClassMapping.get(n).getURI()+"\n"), logging);//hier ist die ontclass drin
					} else 
						Utils.appendToFile(new StringBuffer().append("\n"), logging);
				}
				
				//################################################################################################################################
				//GO THROUGH THE ONTOLOGY AND KAM AND COMPARE REGIONS      agents' regions as in ontology added in ABMCode
				//################################################################################################################################
				//check regional annotations in KAM, tloc() mentions in KAM, compare with ontology belannotation terms
				createPlottingRegions(kam);       //Fills the regions list of type Region with provenance = kam
				createPlottingRegions(this.onto, this.agents, locatedInURI, producedInURI); //Further fills the regions list of type Region with provenance = ontology
				this.abmcode.addRegions(this.regions);
				
				//################################################################################################################################
				//CHECK WHICH AGENTS ARE LOCATED IN WHICH REGIONS   SAVE IN AGENT2REGIONMAP 
				//################################################################################################################################
				this.agentToRegionMap = connectAgentsToRegions(this.agents, this.regions, kam);  //these are all possible regions, ie all regions
																								 //the agent may be located in, source: KAM anno-
																								 //tations, KAM tloc and ontology located_in, produced_in
				this.agentProducedInMap = connectAgentsToProductionRegions(this.agents, this.regions, this.onto, this.producedInURI);
				
				//################################################################################################################################
				//LET USER DECIDE WHICH AGENTS HE WANTS TO BE DISPLAYED IN THE SIMULATION
				//################################################################################################################################
				this.chosenAgents = askUserWhichAgents(this.agents, true);
				
				//################################################################################################################################
				//INITIALIZE AGENTS IN ABM
				//LOOK UP THEIR POSSIBLE QUALITIES IN THE ONTOLOGY
				//################################################################################################################################
				this.abmcode.initializeAgents (this.chosenAgents, this.agents, this.abundanceIdTermMapping, this.onto.getM(), this.agentToRegionMap, this.agentProducedInMap);
				
				//################################################################################################################################
				//INITIALIZE PROCESSES 
				//GO THROUGH THE KAM; LOOK UP ALL BIOPROCESSES;  based on kam statements, bp1 increases/decreases bp2  
				//COMPARE WITH ONTOLOGY; SET UP ALL BIOPROCESSES 
				//################################################################################################################################
				//treat pathologies as BioProcesses?? yes, but careful with link to ontology, they are dispositions there
				this.allBioProcessNodes = getAllBioProcesses(kam);
				this.processIdTermMapping = generateProcessIdTermMapping(new HashMap<String,String>());  //BEL ID - BEL Term
				this.abmcode.initializeProcesses(this.allBioProcessNodes, this.processIdTermMapping, this.onto, belAnnoProp);
				this.abmcode.setBioProcessBehaviour(kam);  //bp1 inreases/decreases bp2
				this.allActivityNodes = getAllActivityNodes(kam);
				this.abmcode.generateActivities(this.allActivityNodes);
				this.abmcode.generateReactions(kam);
				
				//################################################################################################################################
				//INITIALIZE AGENTS' BEHAVIOUR
				//TRAVERSE THE KAM
				//based on kam statements, composite/complex abundances;hasMember/hasComponent
				//################################################################################################################################
				this.abmcode.generateMemberList();	//connects the agents amongst each other (A increases B)	
				this.abmcode.adjustAgentABMCodeLabels();
				this.abmcode.generateProcessParticipations();
				
				this.abmcode.generateTranslocationInfo(kamStore);
				this.abmcode.generateActivityParticipations();
				
				//################################################################################################################################
				//SEMANTICALLY AUGMENT AGENTS' BEHAVIOUR
				//EXPANSION VIA ONTOLOGY AXIOMS
				//################################################################################################################################
				this.abmcode.generateBehaviourBasics(this.onto); //looks up what characteristics (qualities) the agents have
				
				//################################################################################################################################
				//WRITE CODE TO FILE
				//################################################################################################################################
				this.abmcode.generateCode(kamStore);
			}
		} catch (InvalidArgument e) {
			System.out.println(e.getMessage());
		}
		tearDownKamStore();
	}
	
	/**
	 * connects the agents to only those regions they are produced in
	 * needed for agent insert in their specific production regions
	 * @param agents2
	 * @param regions2
	 * @param onto2
	 * @param producedInURI2
	 * @return
	 */
	private HashMap<KamNode, ArrayList<Region>> connectAgentsToProductionRegions(
			HashMap<KamNode, OntClass> agents2, ArrayList<Region> regions2,
			Ontology onto2, String producedInURI2) {
		HashMap<KamNode, ArrayList<Region>> theMap = new HashMap<KamNode, ArrayList<Region>>();
		Utils.appendToFile(new StringBuffer().append("\nConnecting agents to their production regions via a HashMap<KamNode, ArrayList<Region>>: \n"), logging);
		
		//check the ontology for agent - producedIn region connections
		OntClass cl;
		String dictionary;
		String name;
		Utils.appendToFile(new StringBuffer().append("\nExtracting agent - regions from agents' ontology classes: \n"), logging);
		Boolean foundAnnotation = false;
		for (KamNode node : agents2.keySet()){
			cl = agents2.get(node);
			if (cl != null){
				OntProperty prop = Utils.getObjectPropFromURIString(onto2.getM(), producedInURI2);
				foundAnnotation = false;
				//if the class has a produced_in axiom attached
				if (prop != null && Utils.hasRestriction(cl, prop)){
					Utils.getRestrictionValue(cl, prop);
					for (OntClass obj : Utils.generateObjectOntClassListRecursively(cl, prop)){
						Utils.appendToFile(new StringBuffer().append("\t"+cl.getURI()+" "+producedInURI2+": "), logging);
						Utils.appendToFile(new StringBuffer().append(obj.getLabel(null)+" "), logging);
						//check if region exists, either add provenance or create new region
						//read annotation property belannoprop
						NodeIterator classAnno = onto2.listPropertyValues(obj, onto2.getAnnoProp(getBelAnnoProp()));
						RDFNode annotation;
						while (classAnno.hasNext()){
							dictionary = "";
							name = "";
							annotation = classAnno.next();
							if (!annotation.toString().contains("(") && annotation.toString().contains(":")){
								foundAnnotation = true;
								dictionary = annotation.toString().split(":")[0];
								name = annotation.toString().split(":")[1];
								if (Region.exists(this.getRegions(), name, dictionary)){
									Region e = Region.getRegion(regions2, name, dictionary);
									if (theMap.containsKey(node)){
										ArrayList<Region> tmpList = theMap.get(node);
										if (!tmpList.contains(e)){
											tmpList.add(e);
											theMap.put(node, tmpList);
											Utils.appendToFile(new StringBuffer().append("agent produced in region: "+node+" "+cl.getLabel(null)+" "+e.getABMCodeLabel()+"\n"), logging);
										}
									}
									else {
										ArrayList<Region> tmpList = new ArrayList<Region>();
										if (!tmpList.contains(e)){
											tmpList.add(e);
											theMap.put(node, tmpList);
											Utils.appendToFile(new StringBuffer().append("agent produced in region: "+node+" "+cl.getLabel(null)+" "+e.getABMCodeLabel()+"\n"), logging);
										}
									}
								}
							}
						}
						//if no annotation has been found, ie the class the produced_inURI points to has no (valid) BELTerm annotation such as Anatomy:Intestine
						if (!foundAnnotation){
							name = obj.getLabel(null);
							if (Region.exists(this.getRegions(), name, null)){
								Region e = Region.getRegion(regions2, name, null);
								if (theMap.containsKey(node)){
									ArrayList<Region> tmpList = theMap.get(node);
									if (!tmpList.contains(e)){
										tmpList.add(e);
										theMap.put(node, tmpList);
										Utils.appendToFile(new StringBuffer().append("agent produced in region: "+node+" "+cl.getLabel(null)+"\n"), logging);
									}
								}
								else {
									ArrayList<Region> tmpList = new ArrayList<Region>();
									if (!tmpList.contains(e)){
										tmpList.add(e);
										theMap.put(node, tmpList);
										Utils.appendToFile(new StringBuffer().append("agent produced in region: "+node+" "+cl.getLabel(null)+"\n"), logging);
									}
								}
							}
						}
					}
				}
			}
		}
		
		return theMap;
	}

	/**
	 * 
	 * @param agents2
	 * @param regions2
	 * @param kam
	 * @return
	 */
	private HashMap<KamNode, ArrayList<Region>> connectAgentsToRegions(
			HashMap<KamNode, OntClass> agents2, ArrayList<Region> regions2, Kam kam) {
		HashMap<KamNode, ArrayList<Region>> theMap = new HashMap<KamNode, ArrayList<Region>>();
		Utils.appendToFile(new StringBuffer().append("\nConnecting agents to their regions via a HashMap<KamNode, ArrayList<Region>>: \n"), logging);
		Utils.appendToFile(new StringBuffer().append("\n *** via KAM annotations: \n"), logging);
		//check edges of agent nodes for statements that connect them to their regions
		for (KamNode agentNode : agents2.keySet()){
			// retrieve the edges of this node
			for (KamEdge edge : kam.getAdjacentEdges(agentNode)){
				List<BelStatement> statements;
				try {
					statements = kamStore.getSupportingEvidence(edge);
					for (BelStatement statement : statements) {
						List<Annotation> annotations = statement.getAnnotationList();
						for (Annotation annotation : annotations) {
							for (String annoName : this.getLocatedInAnnotationNames()){  //traverse dictionary names, eg Anatomy, MESHCL...
								if (annoName.equals(annotation.getAnnotationType().getName())){
									if (Region.exists(this.regions, annotation.getValue(), annotation.getAnnotationType().getName())){
										Region e = Region.getRegion(this.regions, annotation.getValue(), annotation.getAnnotationType().getName());
										if (theMap.containsKey(agentNode)){
											ArrayList<Region> tmpList = theMap.get(agentNode);
											if (!tmpList.contains(e)){
												tmpList.add(e);
												theMap.put(agentNode, tmpList);
												Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(agentNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
											}
										}
										else {
											ArrayList<Region> tmpList = new ArrayList<Region>();
											if (!tmpList.contains(e)){
												tmpList.add(e);
												theMap.put(agentNode, tmpList);
												Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(agentNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
											}
										}
									}
								}
							}
						}
					}
				} catch (KamStoreException e) {
					Utils.appendToFile(new StringBuffer().append("\nKamStoreException when scanning for agent - region connection. \n"), logging);
					//e.printStackTrace();
				}
			}
		}
		//add agent - regions from the KAM - tloc()
		Utils.appendToFile(new StringBuffer().append("\n *** via tloc() information: \n"), logging);
		String namefrom;
		String dictionaryfrom;
		String nameto;
		String dictionaryto;
		KamNode translocatedNode;
		for (KamNode node : kam.getNodes()){
			if (node.getFunctionType() == FunctionEnum.TRANSLOCATION){
				try {
//					System.out.println("node: "+node);
					List<BelTerm> terms = kamStore.getSupportingTerms(node); //node is sth like this: translocation(abundance(1),29,40)
					translocatedNode = kam.findNode(node.toString().substring( node.toString().indexOf("(")+1, node.toString().indexOf(",")) );
					
//					System.out.println("translocated node: "+translocatedNode+" "+translocatedNode.getLabel() );
					for (BelTerm t : terms){
						namefrom="";
						dictionaryfrom="";
						nameto="";
						dictionaryto="";
						namefrom = t.getLabel().split(",")[1];
						namefrom = namefrom.substring(0, namefrom.length());
						if (namefrom.contains(":")){
							dictionaryfrom = namefrom.split(":")[0];
							namefrom = namefrom.split(":")[1];
						}
						nameto = t.getLabel().split(",")[2];
						nameto = nameto.substring(0, nameto.length()-1);
						if (nameto.contains(":")){
							dictionaryto = nameto.split(":")[0];
							nameto = nameto.split(":")[1];
						}
						namefrom = namefrom.replaceAll("\"", "");
						nameto= nameto.replaceAll("\"", "");
//						System.out.println("  "+namefrom + " to "+nameto);
						if (Region.exists(this.regions, namefrom, dictionaryfrom)){
							Region e = Region.getRegion(this.regions, namefrom, dictionaryfrom);
							if (theMap.containsKey(translocatedNode)){
								ArrayList<Region> tmpList = theMap.get(translocatedNode);
								if (!tmpList.contains(e)){
									tmpList.add(e);
									theMap.put(translocatedNode, tmpList);
									Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(translocatedNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
								}
							}
							else {
								ArrayList<Region> tmpList = new ArrayList<Region>();
								tmpList.add(e);
								theMap.put(translocatedNode, tmpList);
								Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(translocatedNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
							}
						}
						if (Region.exists(this.regions, nameto, dictionaryto)){
							Region e = Region.getRegion(this.regions, nameto, dictionaryto);
							if (theMap.containsKey(translocatedNode)){
								ArrayList<Region> tmpList = theMap.get(translocatedNode);
								if (!tmpList.contains(e)){
									tmpList.add(e);
									theMap.put(translocatedNode, tmpList);
									Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(translocatedNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
								}
							}
							else {
								ArrayList<Region> tmpList = new ArrayList<Region>();
								tmpList.add(e);
								theMap.put(translocatedNode, tmpList);
								Utils.appendToFile(new StringBuffer().append("agent in region: "+kamStore.getSupportingTerms(translocatedNode).get(0)+" "+e.getABMCodeLabel()+"\n"), logging);
							}
						}
					}
				} catch (KamStoreException e) {
					Utils.appendToFile(new StringBuffer().append("\nKamStoreException when scanning for regions in tloc(). \n"), logging);
					//e.printStackTrace();
				}
			}
		}
		//check the ontology for agent - region connections
		OntClass cl;
		String dictionary;
		String name;
		Utils.appendToFile(new StringBuffer().append("\nExtracting agent - regions from agents' ontology classes: \n"), logging);
		Boolean foundAnnotation = false;
		for (KamNode node : agents2.keySet()){
			cl = agents2.get(node);
			if (cl != null){
				OntProperty prop = Utils.getObjectPropFromURIString(this.onto.getM(), locatedInURI);
				foundAnnotation = false;
				//if the class has a located_in axiom attached
				if (prop != null && Utils.hasRestriction(cl, prop)){
					for (OntClass obj : Utils.generateObjectOntClassList(cl, prop)){
						Utils.appendToFile(new StringBuffer().append("\t"+cl.getURI()+" "+locatedInURI+": "), logging);
						Utils.appendToFile(new StringBuffer().append(obj.getLabel(null)+" "), logging);
						//check if region exists, either add provenance or create new region
						//read annotation property belannoprop
						NodeIterator classAnno = this.onto.listPropertyValues(obj, this.onto.getAnnoProp(getBelAnnoProp()));
						RDFNode annotation;
						while (classAnno.hasNext()){
							dictionary = "";
							name = "";
							annotation = classAnno.next();
							if (!annotation.toString().contains("(") && annotation.toString().contains(":")){
								foundAnnotation = true;
								dictionary = annotation.toString().split(":")[0];
								name = annotation.toString().split(":")[1];
								if (Region.exists(this.getRegions(), name, dictionary)){
									Region e = Region.getRegion(this.regions, name, dictionary);
									if (theMap.containsKey(node)){
										ArrayList<Region> tmpList = theMap.get(node);
										if (!tmpList.contains(e)){
											tmpList.add(e);
											theMap.put(node, tmpList);
											Utils.appendToFile(new StringBuffer().append("agent in region: "+node+" "+e.getABMCodeLabel()+"\n"), logging);
										}
									}
									else {
										ArrayList<Region> tmpList = new ArrayList<Region>();
										if (!tmpList.contains(e)){
											tmpList.add(e);
											theMap.put(node, tmpList);
											Utils.appendToFile(new StringBuffer().append("agent in region: "+node+" "+e.getABMCodeLabel()+"\n"), logging);
										}
									}
								}
							}
						}
						//if no annotation has been found, ie the class the located_inURI points to has no (valid) BELTerm annotation such as Anatomy:Intestine
						if (!foundAnnotation){
							name = obj.getLabel(null);
							if (Region.exists(this.getRegions(), name, null)){
								Region e = Region.getRegion(this.regions, name, null);
								if (theMap.containsKey(node)){
									ArrayList<Region> tmpList = theMap.get(node);
									if (!tmpList.contains(e)){
										tmpList.add(e);
										theMap.put(node, tmpList);
										Utils.appendToFile(new StringBuffer().append("agent in region: "+node+" "+e.getABMCodeLabel()+"\n"), logging);
									}
								}
								else {
									ArrayList<Region> tmpList = new ArrayList<Region>();
									if (!tmpList.contains(e)){
										tmpList.add(e);
										theMap.put(node, tmpList);
										Utils.appendToFile(new StringBuffer().append("agent in region: "+node+" "+e.getABMCodeLabel()+"\n"), logging);
									}
								}
							}
						}
					}
				}
			}
		}
		
		return theMap;
	}

	/**
	 * traverses all agent ontology classes and checks the regions attached to them via the locatedInURI2
	 * updates the this.regions List
	 * @param onto2
	 * @param agents2
	 * @param locatedInURI2
	 */
	private void createPlottingRegions(Ontology onto2,
			HashMap<KamNode, OntClass> agents2, String locatedInURI2, String producedInURI2) {
		OntClass cl;
		String dictionary;
		String name;
		Region region;
		ArrayList<ProvenanceType> arProv = new ArrayList<ProvenanceType>();
		arProv.add(Region.ProvenanceType.KAM);
		Utils.appendToFile(new StringBuffer().append("\nExtracting regions from agents' ontology classes: \n"), logging);
		Boolean foundAnnotation = false;
		ArrayList<String> URIsToCheck = new ArrayList<String>();
		if (locatedInURI2 != null)
			URIsToCheck.add(locatedInURI2);
		if (producedInURI2 != null)
			URIsToCheck.add(producedInURI2);
		for (KamNode node : agents2.keySet()){
			for (String uri : URIsToCheck){
				cl = agents2.get(node);
				if (cl != null){
					OntProperty prop = Utils.getObjectPropFromURIString(onto2.getM(), uri);
					foundAnnotation = false;
					//if the class has a located_in axiom attached
					if (prop != null && Utils.hasRestriction(cl, prop)){
						for (OntClass obj : Utils.generateObjectOntClassList(cl, prop)){
							Utils.appendToFile(new StringBuffer().append("\t"+cl.getURI()+" "+uri+": "), logging);
							Utils.appendToFile(new StringBuffer().append(obj.getLabel(null)+" "), logging);
							//check if region exists, either add provenance or create new region
							//read annotation property belannoprop
							NodeIterator classAnno = this.onto.listPropertyValues(obj, this.onto.getAnnoProp(getBelAnnoProp()));
							RDFNode annotation;
							while (classAnno.hasNext()){
								dictionary = "";
								name = "";
								annotation = classAnno.next();
								if (!annotation.toString().contains("(") && annotation.toString().contains(":")){
									foundAnnotation = true;
									dictionary = annotation.toString().split(":")[0];
									name = annotation.toString().split(":")[1];
									name=name.trim();
									if (name.startsWith("\""))
										name = name.substring(1);
									if (name.endsWith("\""))
										name = name.substring(0, name.length()-1);
									if (!Region.exists(this.getRegions(), name, dictionary)){
										region = new Region(name,arProv) ;
										region.setDictionaryName(dictionary);
										this.regions.add(region);
										Utils.appendToFile(new StringBuffer().append("found region: "+region.getABMCodeLabel()+" in "+region.getDictionaryName()+"\n"), logging);
									}
								}
							}
							//if no annotation has been found, ie the class the located_inURI points to has no (valid) BELTerm annotation such as Anatomy:Intestine
							if (!foundAnnotation){
								name = obj.getLabel(null);
								name=name.trim();
								if (name.startsWith("\""))
									name = name.substring(1);
								if (name.endsWith("\""))
									name = name.substring(0, name.length()-1);
								if (!Region.exists(this.getRegions(), name, null)){
									region = new Region(name,arProv) ;
									this.regions.add(region);
									Utils.appendToFile(new StringBuffer().append("found region: "+region.getABMCodeLabel()+" in "+region.getDictionaryName()+"\n"), logging);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * traverses KAM for regional information and compares with ontology 
	 * @param kam
	 * @return
	 */
	private void createPlottingRegions(Kam kam) {
		//add regions from the KAM - annotations 
		Utils.appendToFile(new StringBuffer().append("\n\nGenerating the regions:\n"), logging);
		Region region;
		ArrayList<ProvenanceType> arProv = new ArrayList<ProvenanceType>();
		arProv.add(Region.ProvenanceType.KAM);
		for (KamEdge edge : kam.getEdges()) {
			List<BelStatement> statements;
			try {
				statements = kamStore.getSupportingEvidence(edge);
				for (BelStatement statement : statements) {
					List<Annotation> annotations = statement.getAnnotationList();
					for (Annotation annotation : annotations) {
						for (String annoName : this.getLocatedInAnnotationNames()){  //traverse dictionary names, eg Anatomy, MESHCL...
							if (annoName.equals(annotation.getAnnotationType().getName())){
								String name=annotation.getValue().trim();
								if (name.startsWith("\""))
									name = name.substring(1);
								if (name.endsWith("\""))
									name = name.substring(0, name.length()-1);
								if (!Region.exists(this.regions, name, annotation.getAnnotationType().getName())){
									region = new Region(name,arProv) ;
									region.setDictionaryName(annotation.getAnnotationType().getName());
									this.regions.add(region);
									Utils.appendToFile(new StringBuffer().append("found region: "+region.getABMCodeLabel()+" in "+region.getDictionaryName()+"\n"), logging);
								}
							}
						}
					}
				}
			} catch (KamStoreException e) {
				Utils.appendToFile(new StringBuffer().append("\nKamStoreException when scanning for regions in annotations. \n"), logging);
				//e.printStackTrace();
			}
		}
		//add regions from the KAM - tloc()
		String namefrom;
		String dictionaryfrom;
		String nameto;
		String dictionaryto;
		for (KamNode node : kam.getNodes()){
			if (node.getFunctionType() == FunctionEnum.TRANSLOCATION){
				try {
					List<BelTerm> terms = kamStore.getSupportingTerms(node);
					for (BelTerm t : terms){
						namefrom="";
						dictionaryfrom="";
						nameto="";
						dictionaryto="";
						
						namefrom = t.getLabel().split(",")[1];
						namefrom = namefrom.substring(0, namefrom.length());
						if (namefrom.contains(":")){
							dictionaryfrom = namefrom.split(":")[0];
							namefrom = namefrom.split(":")[1];
							namefrom=namefrom.trim();
						}
						if (namefrom.startsWith("\""))
							namefrom = namefrom.substring(1);
						if (namefrom.endsWith("\""))
							namefrom = namefrom.substring(0, namefrom.length()-1);
						
						nameto = t.getLabel().split(",")[2];
						nameto = nameto.substring(0, nameto.length()-1);
						if (nameto.contains(":")){
							dictionaryto = nameto.split(":")[0];
							nameto = nameto.split(":")[1];
							nameto=nameto.trim();
						}
						if (nameto.startsWith("\""))
							nameto = nameto.substring(1);
						if (nameto.endsWith("\""))
							nameto = nameto.substring(0, nameto.length()-1);
						
						if (!Region.exists(this.regions, namefrom, dictionaryfrom)){
							region = new Region(namefrom,arProv);
							region.setDictionaryName(dictionaryfrom);
							this.regions.add(region);
							Utils.appendToFile(new StringBuffer().append("found region: "+region.getABMCodeLabel()+" in "+region.getDictionaryName()+"\n"), logging);
						}
						if (!Region.exists(this.regions, nameto, dictionaryto)){
							region = new Region(nameto,arProv);
							region.setDictionaryName(dictionaryto);
							this.regions.add(region);
							Utils.appendToFile(new StringBuffer().append("found region: "+region.getABMCodeLabel()+" in "+region.getDictionaryName()+"\n"), logging);
						}
					}
				} catch (KamStoreException e) {
					Utils.appendToFile(new StringBuffer().append("\nKamStoreException when scanning for regions in tloc(). \n"), logging);
					//e.printStackTrace();
				}
			}
		}
		//connect to ontology and look for the corresponding classes
		//ExtendedIterator<OntClass> allClasses;
		ArrayList<Region> theRegions = this.getRegions();
		for (Region r : theRegions){
			ExtendedIterator<OntClass> allClasses = this.getOnto().getAllClasses();
			OntClass cl;
			while (allClasses.hasNext()){
				cl = allClasses.next();
				NodeIterator classAnno = this.onto.listPropertyValues(cl, this.onto.getAnnoProp(getBelAnnoProp()));
				RDFNode annotation;
				while (classAnno.hasNext()){
					annotation = classAnno.next();
					if ((r.getDictionaryName()+":"+r.getABMCodeLabel()).equals(annotation.toString()) ||
							(r.getDictionaryName()+":\""+r.getABMCodeLabel()+"\"").equals(annotation.toString()) ||
							r.getABMCodeLabel().equals(annotation.toString())){
						r.setOntClass(cl);
						Utils.appendToFile(new StringBuffer().append("Ontology class found for region "+r.getABMCodeLabel()+": "+cl.getURI()+" \n"), logging);
					}
				}
			}
		}
	}
	
	

	protected List<KamNode> getAllActivityNodes(Kam kam) {
		List<KamNode> activityNodes = new ArrayList<KamNode>();
		Collection<KamEdge> edges = kam.getEdges();

		for (KamEdge e : edges ){
			if (Utils.isActivityNode(e.getSourceNode())){
					if (!activityNodes.contains(e.getSourceNode())){
						activityNodes.add(e.getSourceNode());
					}
			}
			if (Utils.isActivityNode(e.getTargetNode())){
					if (!activityNodes.contains(e.getTargetNode())){
						activityNodes.add(e.getTargetNode());
					}
			}
		}
		return activityNodes;
	}

	/**
	 * generates a mapping of bioProcess BEL IDs -> bioProcess BEL terms
	 *   eg for biologicalProcess(36): biologicalProcess(GO:"???")
	 * @param processIdTermMapping2
	 * @return
	 */
	private HashMap<String, String> generateProcessIdTermMapping(HashMap<String, String> processIdTermMapping2) {
		Utils.appendToFile(new StringBuffer().append("\nGenerating the mapping bioProcess BEL id -> bioProcess BEL term:\n"), logging);
		for (KamNode curKamNode : this.allBioProcessNodes) {
			//System.out.println(all.getLabel());
			List<BelTerm> terms;
			try {
				terms = this.kamStore.getSupportingTerms(curKamNode, true);
				//the following prints the BelTerms corresponding to the bioProcess nodes  eg for biologicalProcess(36): biologicalProcess(GO:"???")
				for (BelTerm t : terms){  //I don't see why there should be more than 1 term??!!
					processIdTermMapping2.put(curKamNode.toString(), t.toString());
					Utils.appendToFile(new StringBuffer().append("\t added to bioProcessIdTermMapping HashMap: key: "+
							curKamNode.toString()+ " --- value: "+t.toString()+"\n"), logging);
				}
			} catch (KamStoreException e) {
				e.printStackTrace();
			}
		}
		return processIdTermMapping2;
	}
	
	

	/**
	 * 
	 * @param agents2
	 * @param b  boolean value: if true, uses settings file (BEL2ABM.ini)
	 * @return
	 */
	protected HashMap<KamNode, OntClass> askUserWhichAgents(
			HashMap<KamNode, OntClass> agents2, boolean b) {
		Utils.appendToFile(new StringBuffer().append("\nThe following KamNodes will be used for display in the simulation:\n"), logging);
		HashMap<KamNode, OntClass> agentsRet = new HashMap<KamNode, OntClass>();
		if (b) {
			List<String> settingsArray = new ArrayList<String>();
			try {
				settingsArray = Utils.readLinesFromJar(settings);
			} catch (IOException e) {
				try {
					settingsArray = Utils.readLines( "files"+File.separator+ settings);
				} catch (FileNotFoundException e2){
					Utils.appendToFile(new StringBuffer().append("ini not found inside askUserWhichAgents"), logging);
					Utils.appendToFile(new StringBuffer().append(e.toString()
							+"\nError: settings file "+settings+" not found. Checked in "+"."+File.separator+settings+" and "+ 
							"files"+File.separator + settings+" Exiting. \n"), logging);
					System.exit(-1);
				}
			}
			
			
			for (String settingLine : settingsArray){
				if (settingLine.split("\t")[0].equalsIgnoreCase("-agent")){
					 if (this.abundanceIdTermMapping.containsValue(settingLine.split("\t")[1])){ //abundance(3) -> abundance(Cell:"LTi Cell")
						 //find the corresponding entry in agents2, copy to agentsRet
						 for (KamNode kn : agents2.keySet()){
							 if (this.abundanceIdTermMapping.get(kn.toString()).equals(settingLine.split("\t")[1])){
								 agentsRet.put(kn, agents2.get(kn));
							 }
						 }
					 }
				}
			}
			for (KamNode kn : agentsRet.keySet()){
				Utils.appendToFile(new StringBuffer().append(kn.toString()+" "+agentsRet.get(kn).getLabel(null)+"\n"), logging);
			}
			return agentsRet;
		
		}
		return agents2;
	}

	/**
	 * iterates all KamNodes in the network and returns all abundance nodes
	 * @param kam
	 * @return List of type KamNode
	 */
	protected List<KamNode> getAllAgents(Kam kam) {
		List<KamNode> agents = new ArrayList<KamNode>();
		Collection<KamEdge> edges = kam.getEdges();
		Collection<KamNode> nodes = kam.getNodes();
		Utils.appendToFile(new StringBuffer().append("\n\nList of "+edges.size()+" statements contained in KAM:\n"), logging);
		for (KamEdge e : edges ){
			Utils.appendToFile(new StringBuffer().append("\t"+e.toString()+" \n"), logging);
		}
		
		Utils.appendToFile(new StringBuffer().append("\n\nList of "+nodes.size()+" nodes contained in KAM:\n"), logging);
		for (KamNode n : nodes ){
			Utils.appendToFile(new StringBuffer().append("\t"+n.toString()+" \n"), logging);
			if(n.getFunctionType() == FunctionEnum.ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.COMPLEX_ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.COMPOSITE_ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.GENE_ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.MICRORNA_ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE ||
					n.getFunctionType() == FunctionEnum.RNA_ABUNDANCE){
					if (!agents.contains(n)){
						agents.add(n);
					}
			}
		}
		return agents;
	}
	
	/**
	 * iterates all KamNodes in the network and returns all bioprocess and pathology kam nodes
	 * @param kam
	 * @return List of type KamNode
	 */
	protected List<KamNode> getAllBioProcesses( Kam kam) {
		List<KamNode> bioProcesses = new ArrayList<KamNode>();
		Collection<KamEdge> edges = kam.getEdges();

		for (KamEdge e : edges ){
			if (Utils.isBioProcess(e.getSourceNode())){
					if (!bioProcesses.contains(e.getSourceNode())){
						bioProcesses.add(e.getSourceNode());
					}
			}
			if (Utils.isBioProcess(e.getTargetNode())){
					if (!bioProcesses.contains(e.getTargetNode())){
						bioProcesses.add(e.getTargetNode());
					}
			}
			if (Utils.isPathology(e.getSourceNode())){
				if (!bioProcesses.contains(e.getSourceNode())){
					bioProcesses.add(e.getSourceNode());
				}
		}
		if (Utils.isPathology(e.getTargetNode())){
				if (!bioProcesses.contains(e.getTargetNode())){
					bioProcesses.add(e.getTargetNode());
				}
		}
		}
		return bioProcesses;
	}


	private void printNetworkSummary(KamSummary summary) throws InvalidArgument, KamStoreException {
		System.out.println(String.format("\tNum Nodes:\t%d", summary.getNumOfNodes()));
		System.out.println(String.format("\tNum Edges:\t%d", summary.getNumOfEdges()));
		System.out.println();
		System.out.println(String.format("\tNum Unique Gene References:\t%d", summary.getNumOfUniqueGeneReferences()));
		System.out.println(String.format("\tNum RNA Abundances:\t\t%d", summary.getNumOfRnaAbundanceNodes()));
		System.out.println(String.format("\tNum Phospho-Proteins:\t\t%d", summary.getNumOfPhosphoProteinNodes()));
		System.out.println();
		System.out.println(String.format("\tNum Transcriptional Controls:\t%d", summary.getNumOfTranscriptionalControls()));
		System.out.println(String.format("\tNum Hypotheses:\t%d", summary.getNumOfHypotheses()));
		if (summary.getAverageHypothesisUpstreamNodes() != null && summary.getAverageHypothesisUpstreamNodes() > 0) {
			System.out.println(String.format("\tAverage Upstream Nodes/Hypothesis:\t%s", (new DecimalFormat("#.0")).format(summary
					.getAverageHypothesisUpstreamNodes())));
		}
		System.out.println(String.format("\tNum Increase Edges:\t\t%d", summary.getNumOfIncreaseEdges()));
		System.out.println(String.format("\tNum Decrease Edges:\t\t%d", summary.getNumOfDecreaseEdges()));
		System.out.println();
	}
	
	private void printKamSummary(KamSummary summary) throws InvalidArgument, KamStoreException {

		//System.out.println(String.format("\n\nSummarizing KAM: %s", summary.getKamInfo().getName()));
		//System.out.println(String.format("\tLast Compiled:\t%s", summary.getKamInfo().getLastCompiled()));
		//System.out.println(String.format("\tDescription:\t%s", summary.getKamInfo().getDescription()));
		System.out.println();
		System.out.println(String.format("\tNum BEL Documents:\t%d", summary.getNumOfBELDocuments()));
		System.out.println(String.format("\tNum Namespaces:\t\t%d", summary.getNumOfNamespaces()));
		System.out.println(String.format("\tNum Annotation Types:\t\t%d", summary.getNumOfAnnotationTypes()));
		System.out.println();
		for (String species : summary.getStatementBreakdownBySpeciesMap().keySet()) {
			System.out.println(String.format("\tNum Statements (%s):\t\t%d", species, summary.getStatementBreakdownBySpeciesMap().get(species)));
		}
		System.out.println();
		printNetworkSummary(summary);

		// print filtered kam summaries if they are available
		if (summary.getFilteredKamSummaries() != null && !summary.getFilteredKamSummaries().isEmpty()) {
			for (String filteredKam : summary.getFilteredKamSummaries().keySet()) {
				System.out.println(filteredKam + ":");
				printNetworkSummary(summary.getFilteredKamSummaries().get(filteredKam));
			}
		}
	}
	
	/**
	 * returns the number of rnaAbundance nodes.
	 * 
	 * @param nodes
	 * @return
	 */
	private int getNumRnaNodes(Collection<KamNode> nodes) {
		int count = 0;
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.RNA_ABUNDANCE) {
				count++;
			}
		}
		return count;
	}

	/**
	 * return number of protein with phosphorylation modification
	 * 
	 * @param nodes
	 * @return
	 */
	private int getPhosphoProteinNodes(Collection<KamNode> nodes) {
		int count = 0;
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE && node.getLabel().indexOf("modification(P") > -1) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns number unique gene reference
	 * 
	 * @param edges
	 * @return
	 */
	private int getUniqueGeneReference(Collection<KamNode> nodes) {
		// count all protienAbundance reference
		Set<String> uniqueLabels = new HashSet<String>();
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE && StringUtils.countMatches(node.getLabel(), "(") == 1
					&& StringUtils.countMatches(node.getLabel(), ")") == 1) {
				uniqueLabels.add(node.getLabel());
			}
		}

		return uniqueLabels.size();
	}

	/**
	 * returns number of inceases and directly_increases edges.
	 * 
	 * @param edges
	 * @return
	 */
	private int getIncreasesEdges(Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getRelationshipType() == RelationshipType.INCREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns number of deceases and directly_decreases edges.
	 * 
	 * @param edges
	 * @return
	 */
	private int getDecreasesEdges(Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getRelationshipType() == RelationshipType.DECREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES) {
				count++;
			}
		}
		return count;
	}

	private int getUpstreamCount(String label, Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getSourceNode().getLabel().equals(label) && isCausal(edge)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns nodes with causal downstream to rnaAbundance() nodes.
	 * 
	 * @param edges
	 * @return
	 */
	private Map<String, Integer> getTranscriptionalControls(Collection<KamEdge> edges) {
		Map<String, Integer> controlCountMap = new HashMap<String, Integer>();
		for (KamEdge edge : edges) {
			if (edge.getTargetNode().getFunctionType() == FunctionEnum.RNA_ABUNDANCE && isCausal(edge)) {
				if (controlCountMap.containsKey(edge.getSourceNode().getLabel())) {
					int count = controlCountMap.get(edge.getSourceNode().getLabel());
					count = count + 1;
					controlCountMap.put(edge.getSourceNode().getLabel(), count);
				} else {
					controlCountMap.put(edge.getSourceNode().getLabel(), 1);
				}
			}
		}

		return controlCountMap;
	}

	/**
	 * returns nodes with 4+ causal downstream to rnaAbundance() nodes.
	 * 
	 * @param edges
	 * @return
	 */
	private Map<String, Integer> getHypotheses(Collection<KamEdge> edges) {
		Map<String, Integer> controlCountMap = getTranscriptionalControls(edges);
		Map<String, Integer> hypCountMap = new HashMap<String, Integer>();
		for (String hyp : controlCountMap.keySet()) {
			if (controlCountMap.get(hyp) >= 4) {
				hypCountMap.put(hyp, controlCountMap.get(hyp));
			}
		}
		return hypCountMap;
	}

	/**
	 * returns true if the edge has one of the 4 causal relationship types.
	 * 
	 * @param edge
	 * @return
	 */
	private boolean isCausal(KamEdge edge) {
		return edge.getRelationshipType() == RelationshipType.INCREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
				|| edge.getRelationshipType() == RelationshipType.DECREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES;
	}

	private KamSummary summarizeKam(Kam kam) throws InvalidArgument, KamStoreException {
		KamSummary summary;
		summary = new KamSummary();
		summary.setKamInfo(kam.getKamInfo());
		summary.setNumOfNodes(kam.getNodes().size());
		summary.setNumOfEdges(kam.getEdges().size());
		summary.setNumOfBELDocuments(kamStore.getBelDocumentInfos(kam.getKamInfo()).size());
		summary.setNumOfNamespaces(kamStore.getNamespaces(kam.getKamInfo()).size());
		summary.setNumOfAnnotationTypes(kamStore.getAnnotationTypes(kam.getKamInfo()).size());
		summary.setNumOfRnaAbundanceNodes(getNumRnaNodes(kam.getNodes()));
		summary.setNumOfPhosphoProteinNodes(getPhosphoProteinNodes(kam.getNodes()));
		summary.setNumOfUniqueGeneReferences(getUniqueGeneReference(kam.getNodes()));
		summary.setNumOfIncreaseEdges(getIncreasesEdges(kam.getEdges()));
		summary.setNumOfDecreaseEdges(getDecreasesEdges(kam.getEdges()));
		summary.setNumOfTranscriptionalControls(getTranscriptionalControls(kam.getEdges()).size());
		summary.setNumOfHypotheses(getHypotheses(kam.getEdges()).size());

		for (KamEdge edge : kam.getEdges()) {
			List<BelStatement> statements = kamStore.getSupportingEvidence(edge);
			for (BelStatement statement : statements) {
				List<Annotation> annotations = statement.getAnnotationList();
				for (Annotation annotation : annotations) {
					String species = null;
					if (HUMAN_TAX_ID.equals(annotation.getValue())) {
						species = "Human";
					} else if (MOUSE_TAX_ID.equals(annotation.getValue())) {
						species = "Mouse";
					} else if (RAT_TAX_ID.equals(annotation.getValue())) {
						species = "Rat";
					}
					if (species != null) {
						addSpeciesCount(summary, species);
					}
				}
			}
		}

		// breakdown human, mouse, rat and summary sub-network
		summary.setFilteredKamSummaries(summarizeSpeciesSpecificEdges(kam));

		return summary;
	}
	
	/**
	 * Summarize human, mouse, and rat individually
	 * 
	 * @param kam
	 * @param kamStore
	 * @throws KamStoreException
	 */
	private Map<String, KamSummary> summarizeSpeciesSpecificEdges(Kam kam) throws KamStoreException {
		Map<String, KamSummary> summaries = new LinkedHashMap<String, KamSummary>();

		Collection<KamEdge> humanEdges = filterEdges(kam, HUMAN_TAX_ID);
		KamSummary humanSummary = summarizeKamNetwork(humanEdges);
		summaries.put("Human specific edges", humanSummary);

		Collection<KamEdge> mouseEdges = filterEdges(kam, MOUSE_TAX_ID);
		KamSummary mouseSummary = summarizeKamNetwork(mouseEdges);
		summaries.put("Mouse specific edges", mouseSummary);

		Collection<KamEdge> ratEdges = filterEdges(kam, RAT_TAX_ID);
		KamSummary ratSummary = summarizeKamNetwork(ratEdges);
		summaries.put("Rat specific edges", ratSummary);

		return summaries;
	}
	
	/**
	 * Summarize nodes and edges
	 * 
	 * @param edges
	 * @return
	 */
	private KamSummary summarizeKamNetwork(Collection<KamEdge> edges) {
		KamSummary summary = new KamSummary();

		Set<KamNode> nodes = new HashSet<KamNode>(); // unique set of nodes
		for (KamEdge edge : edges) {
			nodes.add(edge.getSourceNode());
			nodes.add(edge.getTargetNode());
		}
		summary.setNumOfNodes(nodes.size());
		summary.setNumOfEdges(edges.size());
		summary.setNumOfRnaAbundanceNodes(getNumRnaNodes(nodes));
		summary.setNumOfPhosphoProteinNodes(getPhosphoProteinNodes(nodes));
		summary.setNumOfUniqueGeneReferences(getUniqueGeneReference(nodes));
		summary.setNumOfIncreaseEdges(getIncreasesEdges(edges));
		summary.setNumOfDecreaseEdges(getDecreasesEdges(edges));
		summary.setNumOfTranscriptionalControls(getTranscriptionalControls(edges).size());
		Map<String, Integer> hypCountMap = getHypotheses(edges);
		summary.setNumOfHypotheses(hypCountMap.size());
		// calculate average number of upstream nodes per hypothesis
		int sumUpStreamNodes = 0;
		for (String hyp : hypCountMap.keySet()) {
			sumUpStreamNodes += getUpstreamCount(hyp, edges);
		}
		summary.setAverageHypothesisUpstreamNodes(((double) sumUpStreamNodes) / hypCountMap.size());

		return summary;

	}
	
	private Collection<KamEdge> filterEdges(Kam kam, String speciesTaxId) throws KamStoreException {
		Collection<KamEdge> filteredEdges = new ArrayList<KamEdge>();
		for (KamEdge edge : kam.getEdges()) {
			List<BelStatement> statements = kamStore.getSupportingEvidence(edge);
			for (BelStatement statement : statements) {
				List<Annotation> annotations = statement.getAnnotationList();
				boolean isSpeciesAnnotated = false;
				for (Annotation annotation : annotations) {
					if (HUMAN_TAX_ID.equals(annotation.getValue()) || MOUSE_TAX_ID.equals(annotation.getValue()) || RAT_TAX_ID.equals(annotation.getValue())) {
						isSpeciesAnnotated = true;
					}
					if (speciesTaxId.equals(annotation.getValue())) {
						filteredEdges.add(edge);
						break;
					}
				}
				if (!isSpeciesAnnotated) {
					// add all non species-specific edges
					// filteredEdges.add(edge);
				}
			}
		}
		return filteredEdges;
	}
	
	private void addSpeciesCount(KamSummary summary, String species) {
		if (summary.getStatementBreakdownBySpeciesMap() == null) {
			summary.setStatementBreakdownBySpeciesMap(new HashMap<String, Integer>());
		}
		Integer count = summary.getStatementBreakdownBySpeciesMap().get(species);
		if (count == null) {
			count = 1;
		} else {
			count = count + 1;
		}
		summary.getStatementBreakdownBySpeciesMap().put(species, count);
	}

	

	
	protected void tearDownKamStore() throws SQLException {
		// Tearsdown the KamStore. This removes any cached data and queries
		kamStore.teardown();

		// Close the DBConnection
		dbConnection.getConnection().close();
	}
	
	private void printKamCatalogSummary(List<KamInfo> kamInfos) {
		// Get a list of all the Kams available in the KamStore
		System.out.println("Available KAMS:");
		System.out.println("\tName\tLast Compiled\tSchema Name");
		System.out.println("\t------\t-------------\t-----------");
		for (KamInfo kamInfo : kamInfos) {
			KamDbObject kamDb = kamInfo.getKamDbObject();
			System.out.println(String.format("\t%s\t%s\t%s", kamDb.getName(), kamDb.getLastCompiled(), kamDb.getSchemaName()));
		}
		System.out.print("\n");
	}
	
	private static void printUsageThenExit() {
		System.out.println("Usage:\n" + "  -l       --list-catalog       Lists the KAMs in the KAM Store\n"
				+ "  -k KAM,  --kam-name KAM       The kam to summarize\n"
				+ "  -runBEL2ABM                   runs BEL to ABM conversion\n"
				+ "  -onto                         ontology to be used\n"
				+ "  -ABMCode                      output ABM code file\n"
				+ "  -agentRelation                eg ClassA has_role (==agentRelation) some"
				+ "  -agentClass                                                              agent_role (==agentClass)\n");
		 
		System.exit(1);
	}
	
	/**
	 * Reads the system configuration from the default location
	 * 
	 * @throws IOException
	 */
	protected void setUpSystemConfiguration() throws IOException {
		SystemConfiguration.createSystemConfiguration(null);
		systemConfiguration = SystemConfiguration.getSystemConfiguration();
	}

	/**
	 * Sets up the KAM store using the database information specified in the
	 * SystemConfiguration.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void setUpKamStore() throws SQLException, IOException {
		setUpSystemConfiguration();
		// Setup a database connector to the KAM Store.
		DatabaseService dbService = new DatabaseServiceImpl();
		dbConnection = dbService.dbConnection(systemConfiguration.getKamURL(), systemConfiguration.getKamUser(), systemConfiguration.getKamPassword());

		// Connect to the KAM Store. This establishes a connection to the
		// KamStore database and sets up the system to read and process
		// Kams.
		kamStore = new KamStoreImpl(dbConnection);
	}

	/**
	 * 
	 * @param args
	 * @param string  
	 * @return  the value of the "string" argument (ie, the next item in args)
	 */
	@SuppressWarnings("unused")
	private static String getArg(String[] args, String string) {
		for (int i = 0; i<args.length-1; i++){
			if (args[i].equalsIgnoreCase(string))
				return args[i+1];
		}
		return null;
	}

	/**
	 * @return the allAgents
	 */
	@SuppressWarnings("unused")
	private List<KamNode> getAllAgentsAbundanceNodes() {
		return allAbundanceNodes;
	}

	/**
	 * @return the agents
	 */
	@SuppressWarnings("unused")
	private HashMap<KamNode, OntClass> getAgents() {
		return agents;
	}

	/**
	 * @param agents the agents to set
	 */
	@SuppressWarnings("unused")
	private void setAgents(HashMap<KamNode, OntClass> agents) {
		this.agents = agents;
	}

	/**
	 * @return the onto
	 */
	@SuppressWarnings("unused")
	private Ontology getOnto() {
		return onto;
	}

	/**
	 * @param onto the onto to set
	 */
	@SuppressWarnings("unused")
	private void setOnto(Ontology onto) {
		this.onto = onto;
	}

	/**
	 * @return the abmcode
	 */
	private ABMCode getAbmcode() {
		return abmcode;
	}

	/**
	 * @return the regions
	 */
	private ArrayList<Region> getRegions() {
		return regions;
	}

	/**
	 * @param regions the regions to set
	 */
	private void setRegions(ArrayList<Region> regions) {
		this.regions = regions;
	}

	/**
	 * @param regions the region to add
	 */
	private void addRegion(Region region) {
		this.regions.add(region);
	}

	/**
	 * @return the locatedInAnnotationNames
	 */
	public List<String> getLocatedInAnnotationNames() {
		return locatedInAnnotationNames;
	}

	/**
	 * @param locatedInAnnotationNames the locatedInAnnotationName to add
	 */
	private static void addLocatedInAnnotationName(String locatedInAnnotationName) {
		locatedInAnnotationNames.add(locatedInAnnotationName);
	}

	/**
	 * @return the belAnnoProp
	 */
	public static String getBelAnnoProp() {
		return belAnnoProp;
	}

	/**
	 * @return the locatedInURI
	 */
	public static String getLocatedInURI() {
		return locatedInURI;
	}

}
