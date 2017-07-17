package de.fraunhofer.scai;
/**
 * 
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.internal.KAMStoreDaoImpl.Annotation;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


/**
 * @author Michaela Michi Guendel, Fraunhofer SCAI
 *
 */
public class Ontology {
	
	Model m1;
	OntModel m;   //model containing the ontology to work with
	ArrayList<OntClass> possibleAgentClasses;
	
	private static final String ontologging = "onto.log";

	public Ontology(String string) {
		this.possibleAgentClasses = new ArrayList<OntClass>();
	}

	public Ontology() {
		this.possibleAgentClasses = new ArrayList<OntClass>();
	}

	/**
	 * loads an owl ontology in RDF/XML format
	 * @param path the String pointing to the path of the physical location of the ontology
	 */
	public void loadOnto_RDF_XML(String path){
		this.m1 = ModelFactory.createDefaultModel() ;

		//load ontology
		Utils.writeToFile(new StringBuffer().append("Opening ontology rdf/xml file from "+path+ " ...\n"), ontologging);
		
		InputStream in = FileManager.get().open( path );
		if (in == null) {
			Utils.appendToFile(new StringBuffer().append("File: " + path + " not found\n"), ontologging);
		    throw new IllegalArgumentException(
		                                 "File: " + path + " not found");
		}
		this.m1.read(in, null);
		
		this.m = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM );		
		this.m.add(this.m1);
		Utils.appendToFile(new StringBuffer().append("Ontology contains "+this.m.listClasses().toList().size()+ " classes\n"), ontologging);
	}

	/**
	 * creates an ArrayList of OntClasses that have agentRelation some agentClass (directly or inherited) assigned to them
	 * @param agentRelation  eg has_role
	 * @param agentClass     eg agent_role
	 */
	public void createPossibleAgentsList(String agentRelation, String agentClass) {
		//this.possibleAgentClasses auffüllen
  	  	OntClass cl;
  	  	
  	  	OntProperty rel = m.getObjectProperty(agentRelation);
  	  	ExtendedIterator<OntClass> theClasses = this.m.listClasses();
  	  	
  	  	while (theClasses.hasNext()){
  	  		// in some cases the URI is null
  	  		try {
	  	  		cl = theClasses.next();
	  	  		String foundRestriction;
	  	  		String [] unionClasses;
	  	  		for (Iterator<OntClass> supers = cl.listSuperClasses(); supers.hasNext(); ) {
	  	  			foundRestriction= displayType( supers.next() );
	  	  			// entries are like these:
	  	  			// some http://www.obofoundry.org/ro/ro.owl#has_proper_part http://nerged.cellandgrossanatomy.owl#birnlex_1459
	  	  			// all http://www.obofoundry.org/ro/ro.owl#has_proper_part union{  http://nerged.cellandgrossanatomy.owl#birnlex_1545 http://nerged.cellandgrossanatomy.owl#birnlex_1617}
	  	  			if (foundRestriction.length() > 1 && foundRestriction.split(" ")[1].equalsIgnoreCase(rel.getURI())){
	  	  				//System.out.println("INFO: "+cl.getURI() + " : "+ foundRestriction);	
	  	  				if (foundRestriction.split(" ")[2].indexOf("union{") != -1){
	  	  					foundRestriction = foundRestriction.substring(foundRestriction.indexOf("union{ ")+8,foundRestriction.length()-1);
	  	  					unionClasses = foundRestriction.split(" ");
	  	  					for (int uc = 0; uc < unionClasses.length; uc++){
	  	  	  					//System.out.print(" adding:"+cl.getURI()+"  +  ");
	  	  	  					//System.out.println(unionClasses[uc]);
	  	  						if (unionClasses[uc].equalsIgnoreCase(agentClass)){
	  	  							this.possibleAgentClasses.add(cl);
	  	  							this.possibleAgentClasses = addAllSubclasses(cl, this.possibleAgentClasses);
	  	  							Utils.appendToFile(new StringBuffer().append(cl.getLabel(null)+" and subclasses can act as an agent. "
  	  									+ "current no. of poss. agents: "+this.possibleAgentClasses.size()+"\n"), ontologging);
	  	  						}
	  	  	  					//System.out.println("rel added:"+rel.get(i).getURI()+"  relationUri size:"+relationUri.size());
	  	  					}
	  	  				} else {
	  	  					//System.out.print(" adding:"+cl.getURI()+"  +  ");
	  	  					if(foundRestriction.split(" ")[2].equalsIgnoreCase(agentClass)){
	  	  						this.possibleAgentClasses.add(cl);
	  	  						this.possibleAgentClasses = addAllSubclasses(cl, this.possibleAgentClasses);
	  	  						Utils.appendToFile(new StringBuffer().append(cl.getLabel(null)+" and subclasses can act as an agent. "
	  									+ "current no. of poss. agents: "+this.possibleAgentClasses.size()+"\n"), ontologging);
	  	  					}
	  	  					//System.out.println("rel added:"+rel.get(i).getURI()+"  relationUri size:"+relationUri.size());
	  	  				}
	  	  			}
	  	  		}
  	  		} catch (NullPointerException ne){
  	  			// URI was null??
  	  		}
  	  	}
	}
	
	

	
	
	/**
	 * adds all sublcasses of OntClass cl to arraylist of OntClasses theClassesList
	 * @param cl
	 * @param possibleAgentClasses2
	 * @return the merged ArrayList<OntClass> theClassesList with all subclasses of cl
	 */
    private ArrayList<OntClass> addAllSubclasses(OntClass cl,
			ArrayList<OntClass> theClassesList) {
		ArrayList<OntClass> subclList = Utils.getAllSubclasses(cl);
		for (OntClass c : subclList){
			theClassesList.add(c);
			//System.out.println("\t "+c.getURI() + c.getLabel(null));
		}
		return theClassesList;
	}

	//////////////////////////////////////////////
    // following code based on: http://stackoverflow.com/questions/7779927/get-owl-restrictions-on-classes-using-jena
    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    private String displayType( OntClass sup ) {
        if (sup.isRestriction()) {
            return displayRestriction( sup.asRestriction() );
        }
        return "";
    }
  	    
    private String displayRestriction( Restriction sup ) {
    	if (sup.isAllValuesFromRestriction()) {
          return displayRestriction( "all", sup.getOnProperty(), sup.asAllValuesFromRestriction().getAllValuesFrom() );
    	}
    	else if (sup.isSomeValuesFromRestriction()) {
          return displayRestriction( "some", sup.getOnProperty(), sup.asSomeValuesFromRestriction().getSomeValuesFrom() );
    	}
    	return "";
    }

    private String displayRestriction( String qualifier, OntProperty onP, Resource constraint ) {
    	String out = String.format( "%s %s %s",
                                  qualifier, renderURI( onP ), renderConstraint( constraint ) );
    	return out;
    }

    private Object renderConstraint( Resource constraint ) {
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

    private Object renderURI( Resource onP ) {
    	try {
    		String qName = onP.getModel().qnameFor( onP.getURI() );
    		return qName == null ? onP.getURI() : qName;
    	} catch (NullPointerException npe){
  		// sometimes URI is null
    	}
    	return null;
    }

	/**
	 * @return the possibleAgentClasses
	 */
	public ArrayList<OntClass> getPossibleAgentClasses() {
		return possibleAgentClasses;
	}

	/**
	 * 
	 * @param ap
	 * @return AnnotationProperty used for BEL terms in ontology
	 */
	public AnnotationProperty getAnnoProp(String ap) {
		AnnotationProperty rel = m.getAnnotationProperty(ap);
		if (rel == null){
			Utils.appendToFile(new StringBuffer().append("Error." +ap+" annotation property to be used for BEL terms "+
					 "not found in specified ontology.\n"), ontologging);
			System.exit(-1);
		}
		return rel;
	}

	/**
	 * 
	 * @param ontcl
	 * @param annotationProperty
	 * @return NodeIterator containing all values of annotationProperty
	 */
	public NodeIterator listPropertyValues(OntClass ontcl, AnnotationProperty annotationProperty) {
		return ontcl.listPropertyValues(annotationProperty);
	}



	/**
	 * @return the m
	 */
	public OntModel getM() {
		return m;
	}

	/**
	 * @return the allClasses
	 */
	public ExtendedIterator<OntClass> getAllClasses() {
		return this.getM().listClasses();
	}

	/**
	 * 
	 * @param model
	 * @param classByURI
	 * @return  OntClass corresponding to URI
	 */
	public static OntClass getOntClassFromURIString(OntModel model,
			String classByURI) {
		ExtendedIterator<OntClass> classes = model.listClasses();
		OntClass c;
		while (classes.hasNext()){
			c = classes.next();
			//System.out.println(c.getURI());
			if (c.getURI()!=null && c.getURI().equals(classByURI))
				return c;
		}
		Utils.appendToFile(new StringBuffer().append("Warning. Class " +classByURI+" not found in ontology.\n"), ontologging);
		return null;
	}
	
	
	/**
	 * 
	 * @param model
	 * @param uri
	 * @return  Individual corresponding to URI
	 */
	public static Individual getOntIndividualFromURIString(OntModel model,
			String uri) {
		ExtendedIterator<Individual> indivs = model.listIndividuals();
		Individual i;
		while (indivs.hasNext()){
			i = indivs.next();
			//System.out.println(c.getURI());
			if (i.getURI()!=null && i.getURI().equals(uri))
				return i;
		}
		Utils.appendToFile(new StringBuffer().append("Warning. Individual " +uri+" not found in ontology.\n"), ontologging);
		return null;
	}

	/**
	 * 
	 * @param model
	 * @param prop
	 * @return
	 */
	public static AnnotationProperty getAnnoPropFromURIString(
			OntModel model, String prop) {
		ExtendedIterator<AnnotationProperty> props = model.listAnnotationProperties();
		AnnotationProperty p;
		while (props.hasNext()){
			p = props.next();
			//System.out.println("annotation property: "+p.getURI());
			if (p.getURI()!= null && p.getURI().equals(prop))
				return p;
		}
		Utils.appendToFile(new StringBuffer().append("Warning. Annotation property " +prop+" not found in ontology.\n"), ontologging);
		return null;
	}

	/**
	 * 
	 * @param annotationString //eg the BELTerm annotation eg abundance(MSO:"T cell")
	 * @param annoProp   the property in which to search to annotationString
	 * @return OntClass with that annotation; else null
	 */
	public OntClass getOntClassFromAnnotationProperty(String annotationString,
			AnnotationProperty annoProp) {
		ExtendedIterator<OntClass> allClasses = this.getM().listClasses();
		OntClass cl;
		NodeIterator annotationIt;  //ontology RDF node iterator
		RDFNode anno; 
		while (allClasses.hasNext()){
			cl = allClasses.next();
			annotationIt = this.listPropertyValues(cl, annoProp);
			while (annotationIt.hasNext()){
				anno = annotationIt.next();
				if (anno.toString().equals(annotationString)){
					return cl;
				}
			}
		}
		return null;
	}

	/**
	 * creates a new class with string URI and returns it
	 * @param string
	 * @return
	 */
	public OntClass createClass(String string) {
		this.m.createClass(string);
		return this.m.getOntClass(string);
	}

	/**
	 * adds addClass as a subClass of parentClass
	 * @param parentClass
	 * @param addClass
	 */
	public void addSubClass(OntClass parentClass, OntClass addClass) {
		this.m.getOntClass(parentClass.getURI()).addSubClass(addClass);;
	}

	
	/**
	 * checks whether subClass is a subclass of upperClass (direct or indirect via recursion) 
	 * @param upperClass
	 * @param subClass
	 * @return
	 */
	public static Boolean hasSubClass(OntClass upperClass, OntClass subClass) {
		ArrayList<OntClass> allClasses = Utils.getAllSubclasses(upperClass);
		if (allClasses.contains(subClass))
			return true;
		return false;
	}
	

	/**
	 * Iterates the upperclasses of theClass and returns the lowermost upperclass 
	 * whose URI is contained as a key in theHashMap 
	 * @param theClass
	 * @param theHashMap
	 * @return
	 */
	public static OntClass getUpperClassWithValue(OntClass theClass,
			HashMap<String, String> theHashMap) {
		Iterator<OntClass> supclasses = theClass.listSuperClasses();
		//System.out.println( "   "+theClass.getLabel(null)+" has "+theClass.listSuperClasses().toList().size()+" superclasses.");
		OntClass cl = null;             
		while (supclasses.hasNext()){
			cl = supclasses.next();
			//System.out.println("            next superclass of "+theClass.getLabel(null)+" is "+cl.getURI());
			if (cl.canAs(OntClass.class) && theHashMap.containsKey(cl.getURI())){
				//System.out.println("           in the map: "+cl.getLabel(null)+" - "+cl.getURI());
				return cl;
			}
			else if (cl.canAs(OntClass.class) && cl.getURI()!=null){
				return getUpperClassWithValue(cl, theHashMap);
			}
		}
		return null;
	}


}
