package de.fraunhofer.scai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openbel.framework.api.Kam.KamEdge;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author Michaela
 * diverse utils for reading, writing etc.
 * to be called via (de.fraunhofer.scai.VPHonto.) Utils
 */

public class Utils {
	
	
	/**
	 * @author Michaela
	 * method to read a single file
	 * @params String of the inputfolder, name of the file
	 */
	public static List<String> readLines(String inputfolder, String filename) throws FileNotFoundException{
		List<String> lines = new ArrayList<String>();
				
         File file = new File(inputfolder + File.separator + filename);
         Scanner scanner = new Scanner(file);
         while (scanner.hasNextLine()) {
           lines.add(scanner.nextLine());
         }
         scanner.close();
		
		return lines;
	}
	
	
	/**
	 * @author Michaela
	 * method to read a single file via a stream
	 * @throws IOException 
	 * @params String /path + name of the file that is located in the jar (upper level jar, no subfolder)
	 */
	public static List<String> readLinesFromJar(String filename) throws IOException{
		List<String> lines = new ArrayList<String>();
         URL url = Utils.class.getResource("/" + filename);
         InputStream is = url.openStream();
 		BufferedReader r = new BufferedReader(new InputStreamReader(is));
 		String line ="";
 		try {
 			while ((line=r.readLine()) != null) {
 			    lines.add(line);
 			}  
 		} catch (IOException e) {
 		    // TODO Auto-generated catch block
 		    e.printStackTrace();
 		}

		return lines;
		
		
	}

	
	/**
	 * @author Michaela
	 * method to read a single file via a Scanner
	 * @params String /path + name of the file
	 */
	public static List<String> readLines(String filename) throws FileNotFoundException{
		List<String> lines = new ArrayList<String>();
         File file = new File( filename);
         Scanner scanner = new Scanner(file);
         while (scanner.hasNextLine()) {
           lines.add(scanner.nextLine());
         }
         scanner.close();
		return lines;
	}

	/**
	 * @author Michaela
	 * method to read a single file via a BufferedReader
	 * @throws IOException 
	 * @params String /path + name of the file
	 */
	public static List<String> readLinesBuffered(String filename) throws IOException{
		List<String> lines = new ArrayList<String>();
         @SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(filename));
         String thisLine = null;
         while ((thisLine = br.readLine()) != null)  // while loop begins here
           lines.add(thisLine);

		return lines;
	}
	
	

	 
	/**
	 * method to write contents of a StringBuffer into a file
	 */
	public static void writeToFile (StringBuffer resultBuffer, String folder, String filename){
		Writer fw = null; 
		String outputfile = folder + File.separator + filename;
		
		try 
		{ 
		  fw = new FileWriter( outputfile ); 
		  fw.write( resultBuffer.toString() );
		  System.out.println("Utils.writeToFile: Output saved in "+outputfile);
		} 
		catch ( IOException e ) { 
		  System.err.println( "Utils.writeToFile: Couldn't create file." );
		  e.printStackTrace();
		} 
		finally { 
		  if ( fw != null ) 
		    try { fw.close(); } catch ( IOException e ) { } 
		}
	}

	/**
	 * method to write contents of a StringBuffer into a file
	 * @param resultBuffer  Buffer that contains the stuff to be written
	 * @param filename   file name with complete path where to write the output
	 */
	public static void writeToFile (StringBuffer resultBuffer, String filename){
		Writer fw = null; 
		String outputfile =  filename;
		
		try 
		{ 
		  fw = new FileWriter( outputfile ); 
		  fw.write( resultBuffer.toString() );
		  //fw.append( System.getProperty("line.separator") ); // e.g. "\n"
		  //System.out.println("Utils.writeToFile: Output saved in "+outputfile);
		} 
		catch ( IOException e ) { 
		  System.err.println( "Utils.writeToFile: Couldn't create file." ); 
		} 
		finally { 
		  if ( fw != null ) 
		    try { fw.close(); } catch ( IOException e ) { } 
		}
	}

	
	/**
	 * method to append contents of a StringBuffer to a file
	 */
	public static void appendToFile (StringBuffer resultBuffer, String folder, String filename){
		Writer fw = null; 
		String outputfile = folder + File.separator + filename;
		
		try 
		{ 
		  fw = new FileWriter( outputfile, true );//FileWriter( String filename, boolean append ) 
		  fw.append( resultBuffer.toString() );
		  //fw.append( System.getProperty("line.separator") ); // e.g. "\n"
		  //System.out.println("Utils.writeToFile: Output saved in "+outputfile);
		} 
		catch ( IOException e ) { 
		  System.err.println( "Utils.appendToFile: Couldn't append stream to file." ); 
		} 
		finally { 
		  if ( fw != null ) 
		    try { fw.close(); } catch ( IOException e ) { } 
		}
	}

	
	/**
	 * method that writes the current directory to stdout
	 */
	public static void printCurrentDir (){
		File dir1 = new File (".");
	     File dir2 = new File ("..");
	     try {
	       System.out.println ("Current dir : " + dir1.getCanonicalPath());
	       System.out.println ("Parent  dir : " + dir2.getCanonicalPath());
	       }
	     catch(Exception e) {
	       e.printStackTrace();
	       }
	}
	
	/**
	 * @return method that returns the name of the current directory 
	 */
	public static String getCurrentDir (){
		File dir1 = new File (".");
		String directoryName = "";
	     try {
	       directoryName = dir1.getCanonicalPath();
	       }
	     catch(Exception e) {
	       e.printStackTrace();
	       }
	     return directoryName;
	}

	/**
	 * @return returns a String Array of all the files contained in a folder 
	 */
	public static String[] getFilenamesInFolder (String path){
		return new File( path ).list();
	}
	
	public static Boolean isNullOrEmpty (String stringVar){
		return (stringVar == null || stringVar.length() == 0);
	}
	
	

	
	
	/**
	 * method to append contents of a StringBuffer to a file
	 */
	public static void appendToFile (StringBuffer resultBuffer, String filename){
		Writer fw = null; 
		String outputfile =  filename;
		
		try 
		{ 
		  fw = new FileWriter( outputfile, true );//FileWriter( String filename, boolean append ) 
		  fw.append( resultBuffer.toString() );
		  //fw.append( System.getProperty("line.separator") ); // e.g. "\n"
		  //System.out.println("Utils.writeToFile: Output saved in "+outputfile);
		} 
		catch ( IOException e ) { 
		  System.err.println( "Utils.appendToFile: Couldn't append stream to file." ); 
		} 
		finally { 
		  if ( fw != null ) 
		    try { fw.close(); } catch ( IOException e ) { } 
		}
	}



	/**
	 * 
	 * @param args = args of the main or some string[] containing arguments
	 * @param arg = arg whose value is to be returned
	 * @return
	 */
	public static String getArgument(String args[], String arg){
		String retString ="";
		for (int i=0; i< args.length; i=i+2){
			if (args[i].equalsIgnoreCase(arg)){
				retString = args[i+1];
				break;
			}
		}
		return retString;
	}


	public static ArrayList<OntClass> getAllSubclasses(OntClass cl) {
		Iterator<OntClass> myIt = cl.listSubClasses();
		ArrayList<OntClass> allClasses = new ArrayList<OntClass>();
		OntClass ontclass;
		
		while (myIt.hasNext()){
			ontclass = myIt.next();
			//allClasses.add(ontclass);   doClass adds the current class!
			doClass( allClasses, ontclass, new ArrayList<OntClass> () );
		}
		
		return allClasses;
	}
	
	/** 
	 * @param cls
	 * @param occurs
	 * @param depth
	 * @param e  Element to which the next level of elements is to be attached
	 */
	
	private static void doClass( ArrayList<OntClass> allClasses, OntClass cls,  
			 ArrayList<OntClass> occurs  ) {
		
       if (cls.canAs( OntClass.class ) && !occurs.contains( cls )) {
       		occurs.add ( cls );
       		allClasses.add(cls);
       		// recurse to the next level down
       		for (Iterator<OntClass> i = cls.listSubClasses( true );  i.hasNext(); ) {
               OntClass sub = i.next();
               doClass( allClasses, sub,  occurs);
       		}
       }
   }


	/**
	 * traverses cl and all superclasses of cl and checks if they have the prop restriction assigned
	 * @param cl
	 * @param prop
	 * @return
	 */
	public static boolean hasRestriction(OntClass cl,
			Property prop) {
		if (cl==null || prop==null)
			return false;
		boolean hasProp = false;
  		ArrayList<String> foundRestriction;
  		OntClass nextCl = null; 
  		Iterator<OntClass> classesToRecurse = cl.listSuperClasses();
  		//for statement überprüfen
  		for (Iterator<OntClass> supers = cl.listSuperClasses(); supers.hasNext(); ) {
  			foundRestriction = new ArrayList<String>();
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
  			nextCl = supers.next();
  			if (nextCl.isRestriction())
  				foundRestriction.add( displayType( nextCl ));
  			if (nextCl.isIntersectionClass())
  				foundRestriction.addAll(displayTypes( nextCl ));
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
  			for (String found : foundRestriction){
	  			if (found.length() > 1 && found.split(" ")[1].equalsIgnoreCase(prop.getURI())){
	  				//System.out.println("  "+found);
	  				hasProp = true;
	  				break;  //so once it finds the first occurrence of the restriction, it breaks
	  			} 
  			}
  		}
  		OntClass nc;
  		while (!hasProp && classesToRecurse.hasNext()){
  			nc = classesToRecurse.next();
	  		if (nc.canAs(OntClass.class)) {
					hasProp = hasRestriction(nc, prop);
			}
  		}
		return hasProp;
	}
	
	
	/**
	 * traverses cl and all superclasses of cl and checks if they have the prop restriction assigned
	 * @param cl
	 * @param prop
	 * @return the ontClass the restriction points to
	 */
	public static OntClass getRestrictionValue(OntClass cl,
			Property prop) {
		OntClass valueClass = null;
		if (cl==null || prop==null)
			return valueClass;
  		ArrayList<String> foundRestriction;
  		OntClass nextCl = null; 
  		Iterator<OntClass> classesToRecurse = cl.listSuperClasses();
  		//for statement überprüfen
  		for (Iterator<OntClass> supers = cl.listSuperClasses(); supers.hasNext(); ) {
  			foundRestriction = new ArrayList<String>();
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
  			nextCl = supers.next();
  			if (nextCl.isRestriction())
  				foundRestriction.add( displayType( nextCl ));
  			if (nextCl.isIntersectionClass())
  				foundRestriction.addAll(displayTypes( nextCl ));
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
  			for (String found : foundRestriction){
	  			if (found.length() > 1 && found.split(" ")[1].equalsIgnoreCase(prop.getURI())){
	  				valueClass = generateObjectOntClassList(cl, (OntProperty)prop).get(0);
	  				break;  //so once it finds the first occurrence of the restriction, it breaks
	  			} 
  			}
  		}
  		OntClass nc;
  		while (valueClass== null && classesToRecurse.hasNext()){
  			nc = classesToRecurse.next();
	  		if (nc.canAs(OntClass.class)) {
					valueClass = getRestrictionValue(nc, prop);
			}
  		}
		return valueClass;
	}
	
	
	
	/**
	 * traverses cl and all superclasses of cl and checks if they have the prop restriction assigned
	 * if so, the values of the restrictions will be saved and returned
	 * @param cl
	 * @param prop
	 * @param ontModel 
	 * @return List that contains the qualitative property values the prop relation points to
	 */
	public static List<QualitativeProperty> getQualPropValues(OntClass cl,
			Property prop, OntModel ontModel, List<QualitativeProperty> resultList) {
		String [] unionClasses;
		//System.out.println(cl.getLabel(null));
  		ArrayList<String> foundRestriction;
  		OntClass nextCl = null; 
  		Iterator<OntClass> classesToRecurse = cl.listSuperClasses();
  		for (Iterator<OntClass> supers = cl.listSuperClasses(); supers.hasNext(); ) {
  			foundRestriction = new ArrayList<String>();
  			nextCl = supers.next();
  			if (nextCl.isRestriction())
  				foundRestriction.add( displayType( nextCl ));
  			if (nextCl.isIntersectionClass())
  				foundRestriction.addAll( displayTypes( nextCl ));
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
  			for (String found : foundRestriction){
	  			if (found.length() > 1 && found.split(" ")[1].equalsIgnoreCase(prop.getURI()) && 
	  					!found.split(" ")[2].equals("null")){
	  				//System.out.println("  "+found);
	  				if (found.split(" ")[2].indexOf("union{") != -1){
	  					found = found.substring(found.indexOf("union{ ")+8,found.length()-1);
	  					unionClasses = found.split(" ");
	  					for (int uc = 0; uc < unionClasses.length; uc++){
	  						resultList.add(new QualitativeProperty(ontModel.getOntClass(unionClasses[uc])));
	  						//System.out.println("\t(for)\t"+ontModel.getOntClass(unionClasses[uc]).getLabel(null));
	  					}
	  				} else {
	  					resultList.add(new QualitativeProperty(ontModel.getOntClass(found.split(" ")[2])));
	  					//System.out.println("\t(else) "+cl.getLabel(null)+"\t"+ontModel.getOntClass(found.split(" ")[2]).getLabel(null));
	  				}
	  			} 	
  			}
  		}
  		OntClass nc;
  		while (classesToRecurse.hasNext()){
  			nc = classesToRecurse.next();
	  		if (nc.canAs(OntClass.class)) {
					getQualPropValues(nc, prop, ontModel, resultList);
			}
  		}
		return resultList;
	}
	

    //////////////////////////////////////////////
    // following code based on: http://stackoverflow.com/questions/7779927/get-owl-restrictions-on-classes-using-jena
    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    protected static String displayType( OntClass sup ) {
        if (sup.isRestriction()) {
            return displayRestriction( sup.asRestriction() );
        } 
        return "";
    }
    
    protected static ArrayList<String> displayTypes (OntClass sup){
    	ArrayList<String> returnTypes = new ArrayList<String>();
    	if (sup.isIntersectionClass()){
        	for (Iterator i = sup.asIntersectionClass().listOperands(); i.hasNext(); ){
        		OntClass op = (OntClass) i.next();

        	      if (op.isRestriction()) {
        	          //System.out.println( "Restriction on property " + 
        	          //                    op.asRestriction().getOnProperty() );
        	          returnTypes.add(displayRestriction( op.asRestriction()));
        	      }
        	      else {
        	          //System.out.println( "Named class " + op );
        	      }
        	}
        } 
        return returnTypes;
    }
    
    protected static String displayRestriction( Restriction sup ) {
        if (sup.isAllValuesFromRestriction()) {
        	//System.out.println("all values from");
            return displayRestriction( "all", sup.getOnProperty(), sup.asAllValuesFromRestriction().getAllValuesFrom() );
        }
        else if (sup.isSomeValuesFromRestriction()) {
        	//System.out.println("some values from");
            return displayRestriction( "some", sup.getOnProperty(), sup.asSomeValuesFromRestriction().getSomeValuesFrom() );
        }
        return "";
    }

    protected static String displayRestriction( String qualifier, OntProperty onP, Resource constraint ) {
        String out = String.format( "%s %s %s",
                                    qualifier, renderURI( onP ), renderConstraint( constraint ) );
        return out;
    }

    protected static Object renderConstraint( Resource constraint ) {
        if (constraint.canAs( UnionClass.class )) {
            UnionClass uc = constraint.as( UnionClass.class );
            // this would be so much easier in ruby ...
            String r = "union{ ";
            for (Iterator<? extends OntClass> i = uc.listOperands(); i.hasNext(); ) {
                r = r + " " + renderURI( i.next() );
            }
            return r + "}";
        }
        else {
            return renderURI( constraint );
        }
    }

    protected static Object renderURI( Resource onP ) {
    	try {
        String qName = onP.getModel().qnameFor( onP.getURI() );
        return qName == null ? onP.getURI() : qName;
    	} catch (NullPointerException npe){
    		// sometimes URI is null
    	}
		return null;
    }


    /**
     * checks if the current KamNode is an abundance node
     * @param n
     * @return
     */
	public static boolean isAbundance(KamNode n) {
		return 
				(n.getFunctionType() == FunctionEnum.ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.COMPLEX_ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.COMPOSITE_ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.GENE_ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.MICRORNA_ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE ||
				n.getFunctionType() == FunctionEnum.RNA_ABUNDANCE); 
	}
	
	/**
     * checks if the current KamNode is a process node
     * @param n
     * @return
     */
	public static boolean isBioProcess(KamNode n) {
		return 
				(n.getFunctionType() == FunctionEnum.BIOLOGICAL_PROCESS ||
				 n.getFunctionType() == FunctionEnum.PATHOLOGY); 
	}
	
	
	/**
	 * iterates all agents and returns the agent with the KamNode targetNode; else null
	 * @param targetNode
	 * @param agents
	 * @return
	 */
	public static Agent getAgentByNode(KamNode targetNode, List<Agent> agents) {
		if (targetNode == null || agents.size()<1)
			return null;
		for (Agent a : agents){
			if (a.getAgentInfoKamNode().getId().equals(targetNode.getId())){
				return a;
			}
		}
		return null;
	}


	/**
	 * iterates all bioProcesses and returns the bioProcess with the KamNode targetNode; else null
	 * @param targetNode
	 * @param bioProcesses
	 * @return
	 */
	public static BioProcess getBioProcessByNode(KamNode targetNode,
			List<BioProcess> bioProcesses) {
		for (BioProcess p : bioProcesses){
			if (p.getProcessInfoKamNode().getLabel().equals(targetNode.getLabel())){
				return p;
			}
		}
		return null;
	}


	/**
	 * get the OntClass corresponding to the key KamNode
	 * @param key
	 * @param onto
	 * @param belAnnoProp annotation property inside onto used for annation of the class with the BEL term  eg abundance(Cell:"Lto cell")
	 * @param processIdTermMapping 
	 * @param logging 
	 * @return
	 */
	public static List<OntClass> getOntClasses(KamNode key, Ontology onto, String belAnnoProp, 
			HashMap<String, String> processIdTermMapping, String logging) {
		List<OntClass> retClasses = new ArrayList<OntClass>();
		OntClass nextClass;
		ExtendedIterator<OntClass> allClasses =onto.getM().listClasses(); 
		NodeIterator valueIt;
		RDFNode cur;
		String t;  //BEL term corresponding to key KamNode
		while (allClasses.hasNext()){  
			nextClass = allClasses.next();
			//via class.annotationprop "BELterm"
			valueIt = onto.listPropertyValues(nextClass, onto.getAnnoProp(belAnnoProp));
			while (valueIt.hasNext()){
				cur = valueIt.next();
				t = processIdTermMapping.get(key.getLabel());
				if (t != null && t.toString().equals(cur.toString())){
					
					retClasses.add(nextClass);
					Utils.appendToFile(new StringBuffer().append("\nfound ontology class corresponding to kamnode: "+key.getLabel()+"("+t.toString()+")"+
							" : "+nextClass.getURI()+"="+nextClass.getLabel(null)+"\n"), logging);
				}
			}
		}
		return retClasses;
	}


	public static boolean isActivityNode(KamNode sourceNode) {
		if (sourceNode == null) return false;
		return 
				sourceNode.getFunctionType() == FunctionEnum.CATALYTIC_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.CHAPERONE_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.GTP_BOUND_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.KINASE_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.MOLECULAR_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.PEPTIDASE_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.PHOSPHATASE_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.RIBOSYLATION_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.TRANSCRIPTIONAL_ACTIVITY ||
				sourceNode.getFunctionType() == FunctionEnum.TRANSPORT_ACTIVITY ;

	}
	
	/**
	 * returns the Activity that corresponds to sourceNode; null if there is none
	 * @param sourceNode
	 * @param agents
	 * @return
	 */
	public static Activity getActivity(KamNode sourceNode, List<Agent> agents){
		if (!isActivityNode(sourceNode))
		  return null;
		for (Agent ag : agents){
			for (Activity act : ag.getActivities()){
				if (act.getActivityNode() == sourceNode){
					return act;
				}
			}
		}
		return null;
	}

	
	/**
	 * creates an ArrayList of OntClasses that the predicate points to; eg subject: demyelination predicate: decreases --> object: myelin sheath
	 * only considers restrictions such as   // some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
	 * DOESNT consider unions                // all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}     
	 * @param subject
	 * @param predicate
	 */
	public static ArrayList<OntClass> generateObjectOntClassList(OntClass subject, OntProperty predicate ) {
		ArrayList<OntClass> retList = new ArrayList<OntClass>();
  	  	//System.out.println(" agent:"+subject.getLabel(null));
  	  	OntProperty rel =  predicate;
  		ArrayList<String> foundRestriction;
  		for (Iterator<OntClass> supers = subject.listSuperClasses(); supers.hasNext(); ) {
  			foundRestriction = new ArrayList<String>();
  			OntClass nextCl = supers.next();
  			if (nextCl.isRestriction())
  				foundRestriction.add(displayType( nextCl ));
  			if (nextCl.isIntersectionClass())
  				foundRestriction.addAll(displayTypes( nextCl ));
  			//System.out.println(foundRestriction);
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			for (String found : foundRestriction) {
	  			if (found.length() > 1 && found.split(" ")[1].equalsIgnoreCase(rel.getURI())){
					retList.add(Utils.getOntClass(subject.getOntModel(), found.split(" ")[2]));
	  			}
  			}
  		} 
  		return retList;
	}
	
	
	
	/**
	 * creates an ArrayList of OntClasses that the predicate points to; eg subject: demyelination predicate: decreases --> object: myelin sheath
	 * only considers restrictions such as   // some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
	 * DOESNT consider unions                // all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
	 * works even if the predicate points to a superclass of subject     
	 * @param subject
	 * @param predicate
	 */
	public static ArrayList<OntClass> generateObjectOntClassListRecursively(OntClass subject, OntProperty predicate ) {
		ArrayList<OntClass> retList = new ArrayList<OntClass>();
  	  	//System.out.println(" agent:"+subject.getLabel(null));
  	  	OntProperty rel =  predicate;
  		ArrayList<String> foundRestriction;
  		Iterator<OntClass> classesToRecurse = subject.listSuperClasses();
  		for (Iterator<OntClass> supers = subject.listSuperClasses(); supers.hasNext(); ) {
  			foundRestriction = new ArrayList<String>();
  			OntClass nextCl = supers.next();
  			if (nextCl.isRestriction())
  				foundRestriction.add(displayType( nextCl ));
  			if (nextCl.isIntersectionClass())
  				foundRestriction.addAll(displayTypes( nextCl ));
  			//System.out.println(foundRestriction);
  			// entries are like these:
  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
  			for (String found : foundRestriction) {
	  			if (found.length() > 1 && found.split(" ")[1].equalsIgnoreCase(rel.getURI())){
					retList.add(Utils.getOntClass(subject.getOntModel(), found.split(" ")[2]));
	  			}
  			}
  		} 
  		
  		OntClass nc;
		while (retList.size()<1 && classesToRecurse.hasNext()){
			nc = classesToRecurse.next();
	  		if (nc.canAs(OntClass.class)) {
	  			retList.add(getRestrictionValue(nc, predicate));
			}
		}
		
  		return retList;
	}
	
	
	
	
		/**
		 * returns the ontclass with URI uri from the ontmodel
		 * @param ontModel
		 * @param uri
		 * @return
		 */
		private static OntClass getOntClass(OntModel ontModel, String uri) {
		ExtendedIterator<OntClass> classes = ontModel.listClasses();
		OntClass cl = null;
		while (classes.hasNext()){
			cl = classes.next();
			if (cl.getURI() != null && cl.getURI().equals(uri))
				break;
		}
		return cl;
	}


	/**
	 * 
	 * @param BELTermId
	 * @param agents
	 * @return  the agent whose BELTermIdLabel matches the String BELTermId
	 */
	public static Agent getAgentByBELTermId(String BELTermId, List<Agent> agents) {
		for (Agent ag : agents){
			if (ag.getBELIdLabel().equalsIgnoreCase(BELTermId))
				return ag;
		}
		return null;
	}


	public static boolean isBioProcess(OntClass obj,
			List<BioProcess> bioProcesses) {
		for (BioProcess bp : bioProcesses){
			if (bp.getProcessInfoOntClass().size()>0 && 
					bp.getProcessInfoOntClass().get(0).getURI().equals(obj.getURI()))
				return true;
		}
		return false;
	}


	public static boolean isAgent(OntClass obj, List<Agent> agents) {
		if (obj == null || agents == null)
			return false;
		for (Agent ag : agents){
			if (ag.getAgentInfoOntClass() != null &&
					ag.getAgentInfoOntClass().getURI().equals(obj.getURI()))
				return true;
		}
		return false;
	}


	public static boolean isGlobalVar(OntClass obj, List<String> globalVars, String belAnnoProp ) {
		NodeIterator belt;
		RDFNode BELTerm;
		String term;
		for (String v : globalVars){
			belt = obj.listPropertyValues(obj.getModel().getProperty(belAnnoProp));
			while (belt.hasNext()){
				BELTerm = belt.next();
				//System.out.println("inside Utils.isGlobalVar(): "+BELTerm.toString());
				term = BELTerm.toString().substring(BELTerm.toString().indexOf("("), BELTerm.toString().length()-1);
				term = term.substring(term.indexOf(":")+1);
				term = term.replaceAll("\"", "");
				if (v.contains(term))
					return true;
			}
		}
		return false;
	}


	/**
	 * returns the agent from agents list whose URI corresponds to obj.getUri()
	 * @param obj
	 * @param agents
	 * @return
	 */
	public static Agent getAgentByOntClass(OntClass obj, List<Agent> agents) {
		for (Agent ag : agents){
			if (ag.getAgentInfoOntClass().getURI().equals(obj.getURI()))
				return ag;
		}
		return null;
	}

	/**
	 * returns the name of the global variable that corresponds to cl OntClass
	 * @param cl
	 * @param globalVars
	 * @param belAnnoProp
	 * @return
	 */
	public static String getGlobalVarName(OntClass cl,
			List<String> globalVars, String belAnnoProp) {
		NodeIterator belt;
		RDFNode BELTerm;
		for (String v : globalVars){
			belt = cl.listPropertyValues(cl.getModel().getProperty(belAnnoProp));
			while (belt.hasNext()){
				BELTerm = belt.next();
				if (v.contains(BELTerm.toString()))
					return v;
			}
		}
		return "";
	}


	/**
	 * converts global variable (eg abundance(MSO:"Myelin sheath") AND COUNT) into variable as used in ABM code (eg Myelin_sheath_COUNT)
	 * @param v
	 * @return
	 */
	public static String convertGlobalVarIntoCodeLabel(String v) {
		String label= v.substring(v.indexOf(":")+1, v.indexOf("AND")-2);
		label = label.replaceAll(" ", "_");
		label = label.replaceAll("\"", "");
		label = label+"_";
		label += v.substring(v.indexOf("AND")+4, v.length());
		return label;
	}


	/**
	 * 
	 * @param n
	 * @return true if the KamNode is a pathology node
	 */
	public static boolean isPathology(KamNode n) {
		return 
				(n.getFunctionType() == FunctionEnum.PATHOLOGY );
	}


	/**
	 * iterates all bioProcesses and returns that with the same URI as obj OntClass
	 * @param obj
	 * @param bioProcesses
	 * @return
	 */
	public static BioProcess getBioProcessByOntClass(OntClass obj,
			List<BioProcess> bioProcesses) {
		for (BioProcess bp : bioProcesses){
			//there can be more than 1 OntClass assigned to the BioProcess
			for (OntClass ontCl : bp.getProcessInfoOntClass()){
				if (obj.getURI().equals(ontCl.getURI()))
					return bp;
			}
		}
		return null;
	}


	/**
	 * checks if obj is a super class of some global variable
	 * @param obj
	 * @param globalVars
	 * @param belAnnoProp
	 * @return
	 */
	public static boolean isSuperClassOfGlobalVar(OntClass obj,
			List<String> globalVars, String belAnnoProp) {
		NodeIterator belt;
		RDFNode BELTerm;
		String term;
		ExtendedIterator<OntClass> subclasses = obj.listSubClasses();
		OntClass nextSubClass;
		for (String v : globalVars){
			while (subclasses.hasNext()){
				nextSubClass = subclasses.next();
				belt = nextSubClass.listPropertyValues(nextSubClass.getModel().getProperty(belAnnoProp));
				while (belt.hasNext()){
					BELTerm = belt.next();
					//System.out.println("inside Utils.isGlobalVar(): "+BELTerm.toString());
					term = BELTerm.toString().substring(BELTerm.toString().indexOf("("), BELTerm.toString().length()-1);
					term = term.substring(term.indexOf(":")+1);
					term = term.replaceAll("\"", "");
					if (v.contains(term))
						return true;
				}
			}
		}
		return false;
	}


	/**
	 * 
	 * @param node
	 * @return  true if node is a protein abundance FunctionEnum.PROTEIN_ABUNDANCE, else false
	 */
	public static boolean isProteinAbundance(KamNode node) {
		return (node.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE);
	}

	/**
	 * 
	 * @param node
	 * @return  true if node is a complex abundance FunctionEnum.COMPLEX_ABUNDANCE, else false
	 */
	public static boolean isComplexAbundance(KamNode node) {
		return (node.getFunctionType() == FunctionEnum.COMPLEX_ABUNDANCE);
	}
	
	
	/**
	 * 
	 * @param node
	 * @return  true if node is a composite abundance FunctionEnum.COMPOSITE_ABUNDANCE, else false
	 */
	public static boolean isCompositeAbundance(KamNode node) {
		return (node.getFunctionType() == FunctionEnum.COMPOSITE_ABUNDANCE);
	}


	/**
	 * 
	 * @param m
	 * @param URIString
	 * @return  the OntProperty corresponding to the URI string 
	 */
	public static OntProperty getObjectPropFromURIString(OntModel m,
			String URIString) {
		return m.getOntProperty(URIString);
		
	}


	/**
	 * if targetNode is a translocation node, then the Translocation is returned as saved in one of the agents; otherwise null
	 * @param targetNode
	 * @param agents
	 * @return
	 */
	public static Translocation getTranslocation(KamNode targetNode,
			List<Agent> agents) {
		if (targetNode.getFunctionType() != FunctionEnum.TRANSLOCATION)
			return null;
		for (Agent ag : agents){
			for (Translocation t : ag.getTranslocations()){
				if (targetNode == t.gettNode())
					return t;
			}
		}
		return null;
	}


	/**
	 * find the source node of eg molecularActivity(proteinAbundance(2))   ie  proteinAbundance(2)
	 * @param sourceNode
	 * @return
	 */
	public static KamNode getSourceNodeOfActivityNode(KamNode sourceNode) {
		if (sourceNode == null || sourceNode.getLabel() == null || !isActivityNode(sourceNode))
			return null;
		KamNode ret = sourceNode.getKam().findNode(
				sourceNode.getLabel().substring(
						sourceNode.getLabel().indexOf("(")+1, sourceNode.getLabel().length()-1));
		return ret;
	}


	/**
	 * 
	 * @param agents
	 * @return  all the Translocations of all agents
	 */
	public static ArrayList<Translocation> getAllTranslocations(
			List<Agent> agents) {
		ArrayList<Translocation> tlocs = new ArrayList<Translocation>();
		for (Agent a : agents){
			tlocs.addAll(a.getTranslocations());
		}
		return tlocs;
	}


	/**
	 * reads a tab separated file into a hashmap
	 * key of hashmap: i; value of hashmap: j
	 * @param filename
	 * @param i
	 * @param j
	 * @return
	 */
	public static HashMap<String, String> readTabsepFile(
			String filename, int i, int j) {
		HashMap <String,String> m = new HashMap<String,String>();
		try {
			List<String> fileList = readLines(filename);
			for (String s : fileList){
				m.put(s.split("\t")[i], s.split("\t")[j]);
			}
		} catch (FileNotFoundException | ArrayIndexOutOfBoundsException e) {
			//e.printStackTrace();
		}
		return m;
	}


	/**
	 * Method that parses a String and returns the position of the closing bracket of the first opening bracket
	 * eg: whatever(thisbla(a), bla(bla())), and more bla(this)  will return 31
	 * 
	 * @param theString
	 * @return position of closing bracket corresponding to first opening bracket; -1 in case of error
	 */
	public static int getEndBracketPosition(String theString) {
		int countOpen =0;
		int countClose=0;
		for (int i = 0; i < theString.length(); i++){
			if (theString.charAt(i) == '(')
				countOpen ++;
			if (theString.charAt(i) == ')')
				countClose ++;
			if (countOpen>0 && countOpen == countClose)
				return i;
		}
		return -1;
	}

	/**
	 * counts how many times substring can be found in longString
	 * @param substring
	 * @param longString
	 * @return
	 */
	public static int getNumberOfOccurrences(String substring,
			String longString) {
		int index = longString.indexOf(substring);
		int count = 0;
		while (index != -1) {
		    count++;
		    longString = longString.substring(index + 1);
		    index = longString.indexOf(substring);
		}
		return count;
	}



}
