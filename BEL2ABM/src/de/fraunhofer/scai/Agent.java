package de.fraunhofer.scai;
/**
 * 
 */
//package de.fraunhofer.scai.BEL2ABM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openbel.framework.api.Kam.KamNode;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author latitude_user
 *
 *
 */
public class Agent {
	private KamNode agentInfoKamNode;
	private OntClass agentInfoOntClass;
	private String label;
	private boolean labelAdjusted;        //composite and complex agents' labels get adjusted, flag to check
	private ArrayList<Agent> components;  //in case it's a complex or composite agent, save its components
	private String ABMCodeLabel;
	private String BELTermLabel;
	private String BELIdLabel;
	private Boolean chosenByUser;
	ArrayList<Region> regions;            //these are all regions the agent can be located in
	ArrayList<Region> productionRegions;  //these are the regions the agent is produced in
	private List<QualitativeProperty> qualPropValues;  //stores the classes the has_quality relation points to
	@SuppressWarnings("unused")
	private List<QuantitativeProperty> quantPropValues;
	
	private List<Agent> decreasesAbundance;
	private List<Agent> decreasedByAbundance;
	private List<Agent> increasesAbundance;
	private List<Agent> increasedByAbundance;
	private List<BioProcess> increasesBioProcess;
	private List<BioProcess> increasedByBioProcess;
	private List<BioProcess> decreasesBioProcess;
	private List<BioProcess> decreasedByBioProcess;
	private List<BioProcess> isBiomarkerForProcess;
	private List<Agent> hasMember;
	private List<Agent> isMemberOf;
	private List<Agent> includes;   //the agents that a composite agent includes
	private List<Agent> includedIn; //the composited agents that the agent is included in
	private String plottingColor; //negative integer value
	private String colorString;   //color string such as "black", "gray" etc
	
	private List<Activity> activities;  // to capture eg biologicalProcess(22) increases molecularActivity(abundance(5))
	private List<Translocation> translocations;  // to capture eg translocation(abundance(15),29,40) translocates abundance(15)
	private Boolean isInActive;
	private double size;
	private boolean lifespan;
	private boolean homeostaticControl;
	
	private String logging;
	private String goCode;
	private String homeostaticConcentration;
	private String maxHomeostaticConcentration;	//if no homeostatic conc is known, use known values in combination with 
												//ontology hierarchy to calculate maximal possible homeostatic value
												//eg if sum is 100 and 90 are given to other classes, the remaining 10
												//is the upper limit of all other subclasses (each gets 10 assigned as a 
												//maxhomeostaticconc even if all together they result in sum > 100)
	private String upperValueLimit;  			//maximum threshold the agent number cannot get higher than
	private boolean canReproduce;
	private boolean connectedViaOntology;
	private int currentEventNumber;



	/**
	 * an Agent has a KamNode corresponding to the BEL code, an OntClass corresponding to its mapped ontology class
	 * and a logging string pointing to log file
	 * @param key
	 * @param ontClass
	 * @param logging
	 * @param b 
	 * @param BELIdLabel   eg "abundance(3)"
	 * @param homeostat_conc 
	 * @param maxConc 
	 */
	public Agent(KamNode key, OntClass ontClass, String logging, boolean b, String BELIdLabel, String BELTermLabel, 
			ArrayList<Region> regions, ArrayList<Region> productionRegions, 
			String homeostat_conc, String maxConc) {
		
		this.agentInfoKamNode = key;
		this.agentInfoOntClass = ontClass;
		if (this.agentInfoOntClass != null) {
			this.label = this.agentInfoOntClass.getLabel(null);
		} else {
			this.label = null;
		}
		this.labelAdjusted = false;
		this.BELTermLabel = BELTermLabel;
		this.components = new ArrayList<Agent>();
		this.homeostaticConcentration = homeostat_conc;
		this.maxHomeostaticConcentration = maxConc;
		this.upperValueLimit = "";
		this.lifespan = true;            //default
		this.homeostaticControl = true;  //default
		this.canReproduce= false;        //default
		this.goCode = "";
		
		this.ABMCodeLabel = this.label;
		if (this.ABMCodeLabel == null){
			this.ABMCodeLabel = this.BELTermLabel.substring(this.BELTermLabel.indexOf("("),this.BELTermLabel.length());
			if (this.ABMCodeLabel.contains(":")) {
				if (this.ABMCodeLabel.indexOf(")") > -1  && this.ABMCodeLabel.indexOf(")") < this.ABMCodeLabel.indexOf(":")){
					this.ABMCodeLabel = this.BELTermLabel;
				} else {
					this.ABMCodeLabel = this.ABMCodeLabel.substring(this.ABMCodeLabel.indexOf(":")+1);
				}
			}
		} 
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("[ (),\"]+", "_");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("abundance", "");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("complexAbundance", "");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("compositeAbundance", "");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("proteinAbundance", "");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("geneAbundance", "g.");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("microRNAAbundance", "m.");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("rnaAbundance", "r.");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("__", "_");
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("_[a-zA-Z]*:", "");
		if(this.ABMCodeLabel.startsWith("_"))
			this.ABMCodeLabel = this.ABMCodeLabel.substring(1, this.ABMCodeLabel.length());
		if (this.ABMCodeLabel.endsWith("_"))
			this.ABMCodeLabel =this.ABMCodeLabel.substring(0, this.ABMCodeLabel.length()-1);
		
		this.chosenByUser = b;
		if (this.chosenByUser)
			this.size = 1.5;
		else 
			this.size = 0;
		this.regions = regions;
		this.productionRegions = productionRegions;
		this.BELIdLabel = BELIdLabel;
		this.decreasesAbundance = new ArrayList<Agent>();
		this.decreasedByAbundance = new ArrayList<Agent>();
		this.increasesAbundance = new ArrayList<Agent>();
		this.increasedByAbundance = new ArrayList<Agent>();
		this.increasesBioProcess = new ArrayList<BioProcess>();
		this.increasedByBioProcess = new ArrayList<BioProcess>();
		this.decreasesBioProcess = new ArrayList<BioProcess>();
		this.decreasedByBioProcess = new ArrayList<BioProcess>();
		this.isBiomarkerForProcess = new ArrayList<BioProcess>();
		this.hasMember = new ArrayList<Agent>();
		this.isMemberOf = new ArrayList<Agent>();
		this.includes = new ArrayList<Agent>();
		this.includedIn = new ArrayList<Agent>();
		this.currentEventNumber=0;   //start with event number0
		this.plottingColor = "";
		this.colorString = "white";  //default
		
		this.activities = new ArrayList<Activity>();
		this.translocations= new ArrayList<Translocation>();
		this.isInActive = false;   //default  
		this.qualPropValues = new ArrayList<QualitativeProperty>();
		
		this.logging = logging;
		this.connectedViaOntology = false; //agent is or is not connected to the model via ontology axiom
		
		Utils.appendToFile(new StringBuffer().append("New agent type: \n\t"
				+ "label: "+this.label 
				+" * BEL term: "+this.BELTermLabel +"\n\t"+" * BEL ID label: "+this.BELIdLabel 
				+"\n\t"+" * chosen by user for display: "+this.chosenByUser+" * homeostatic concentration: "+this.homeostaticConcentration
				+"\n"), logging);
		if (this.label != null)
			Utils.appendToFile(new StringBuffer().append("\t * has onto URI: "+ this.agentInfoOntClass.getURI()+"\n"), logging);
		if (this.regions != null && this.regions.size() > 0 ){
			for (Region r : this.regions){
				Utils.appendToFile(new StringBuffer().append("\t * located in: "+ r.getABMCodeLabel()+"\n"), logging);
			}
		}
		
	}

	
	public boolean isLabelAdjusted() {
		return labelAdjusted;
	}


	public void setLabelAdjusted(boolean labelAdjusted) {
		this.labelAdjusted = labelAdjusted;
	}


	public ArrayList<Region> getProductionRegions() {
		return productionRegions;
	}


	public boolean isConnectedViaOntology() {
		return connectedViaOntology;
	}


	public void setConnectedViaOntology(boolean connectedViaOntology) {
		this.connectedViaOntology = connectedViaOntology;
	}


	public List<Agent> getIncludedIn() {
		return includedIn;
	}


	public void setIncludedIn(List<Agent> includedIn) {
		this.includedIn = includedIn;
	}


	/**
	 * returns true if the agent doesn't participate in the simulation
	 * eg: agents that are inside the BEL code only as upper class nodes (... isA Cell:"homeostatic cell")
	 *     and that have no further connection to the other agents' behaviours
	 * @return
	 */
	public boolean isDisconnected(ArrayList<Reaction> reactionlist, ArrayList<Agent> allAgents) {
		Boolean reactionParticipation = false;
		Boolean memberOfComplexAgent = false;
		for (Reaction r : reactionlist){
			if (r.getReactants().contains(this) || r.getProducts().contains(this))
				reactionParticipation = true;
		}
		for(Agent otherAgent : allAgents){
			if (otherAgent.getHasMember().contains(this))
				memberOfComplexAgent = true;
		}
		return (getDecreasedByAbundance().size() < 1 &&
				getDecreasedByBioProcess().size() < 1 &&
				getDecreasesAbundance().size() < 1 &&
				getDecreasesBioProcess().size() < 1 &&
				getIncreasedByAbundance().size() < 1 &&
				getIncreasedByBioProcess().size() < 1 &&
				getIncreasesAbundance().size() < 1 &&
				getIncreasesBioProcess().size() < 1 &&
				getIsBiomarkerForProcess().size() < 1 &&
				getComponents().size() < 1 &&
				getHasMember().size() < 1 &&
				getTranslocations().size() < 1 &&
				getActivities().size() < 1 &&
				getIncludedIn().size() < 1 &&
				getIncludes().size() < 1 &&
				!reactionParticipation &&
				!memberOfComplexAgent &&
				!connectedViaOntology );
	}





	public String getUpperValueLimit() {
		return upperValueLimit;
	}



	public void setUpperValueLimit(String upperValueLimit) {
		this.upperValueLimit = upperValueLimit;
	}
	
	public Boolean hasUpperValueLimit() {
		return (this.upperValueLimit != "");
	}



	public String getMaxHomeostaticConcentration() {
		return maxHomeostaticConcentration;
	}



	public void setMaxHomeostaticConcentration(String maxHomeostaticConcentration) {
		this.maxHomeostaticConcentration = maxHomeostaticConcentration;
	}



	public boolean isHomeostaticControl() {
		return homeostaticControl;
	}



	public void setHomeostaticControl(boolean homeostaticControl) {
		this.homeostaticControl = homeostaticControl;
	}



	public boolean hasLifespan() {
		return lifespan;
	}



	public void setLifespan(boolean lifespan) {
		this.lifespan = lifespan;
	}



	public boolean hasQualPropValues(){
		if (this.getQualPropValues() == null || this.getQualPropValues().size()<1)
			return false;
		return true;
	}

	/**
	 * looks for the ontology class of the agent and fills the
	 * qualProps and quantProps Lists
	 * ATTENTION: DOESN'T WORK YET FOR QUANTITATIVE PROPERTIES!!!
	 * @param onto
	 * @param quantPropUri 
	 * @param qualPropUri 
	 */
	public void generateBehaviour(Ontology onto, String qualPropUri, String quantPropUri) {
		Utils.appendToFile(new StringBuffer().append("\nGenerating properties for agent "+this.label
				+"/"+this.BELTermLabel+" \n"), logging);
		//find the qualitative and quantitative Object / Data properties in the ontology
		Property qualProp = onto.getM().getProperty(qualPropUri);
		if (qualProp == null) {
			Utils.appendToFile(new StringBuffer().append("\n  no property with this URI: "+qualPropUri+" found in ontology. \n"), logging);
		}
		//System.out.println(this.getAgentInfoOntClass().getURI());//hier
		//System.out.println("\tAGENT LABEL: "+this.label+"  qualprop URI: " +qualProp.getURI());
		//System.out.println("\t"+Utils.hasRestriction(this.agentInfoOntClass, qualProp));
		if (Utils.hasRestriction(this.agentInfoOntClass, qualProp)){
			Utils.appendToFile(new StringBuffer().append("\tquality property: "+qualPropUri+" \n"), logging);
			qualPropValues = Utils.getQualPropValues(this.agentInfoOntClass, qualProp, this.agentInfoOntClass.getOntModel(), 
					new ArrayList<QualitativeProperty>());
			for (QualitativeProperty q: qualPropValues){
				Utils.appendToFile(new StringBuffer().append("\t\tquality value: "+q.getQuality().getURI()+":"
						+q.getQuality().getLabel(null)+" \n"), logging);
			}
		}
		
		
		//TODO for quantitative properties
		/*Property quantProp = onto.getM().getProperty(quantPropUri);
		if (quantProp != null  Utils.hasRestriction(this.agentInfoOntClass, quantProp)){
			Utils.appendToFile(new StringBuffer().append("\tquality: "+quantPropUri+" \n"), logging);
			//quantProps = getQuantProps(quantProp);
		}*/
		
		//fill qualProps and quantProps lists
		
	}
	

	public void addActivity(Activity act){
		this.activities.add(act);
	}
	

	public String getHomeostaticConcentration() {
		return homeostaticConcentration;
	}
	
	public Boolean hasHomeostaticConcentration() {
		return homeostaticConcentration!=null;
	}
	
	public Boolean hasMaxHomeostaticConcentration() {
		return maxHomeostaticConcentration!=null && maxHomeostaticConcentration.length()>0 ;
	}
	
	
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
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
	 * @return the agentInfoKamNode
	 */
	public KamNode getAgentInfoKamNode() {
		return agentInfoKamNode;
	}


	/**
	 * @return the agentInfoOntClass
	 */
	public OntClass getAgentInfoOntClass() {
		return agentInfoOntClass;
	}


	/**
	 * @return the decreasesAbundance
	 */
	public List<Agent> getDecreasesAbundance() {
		return decreasesAbundance;
	}


	/**
	 * @return the decreasedByAbundance
	 */
	public List<Agent> getDecreasedByAbundance() {
		return decreasedByAbundance;
	}


	/**
	 * @return the increasesAbundance
	 */
	public List<Agent> getIncreasesAbundance() {
		return increasesAbundance;
	}


	/**
	 * @return the increasedByAbundance
	 */
	public List<Agent> getIncreasedByAbundance() {
		return increasedByAbundance;
	}


	/**
	 * @return the increasesBioProcess
	 */
	public List<BioProcess> getIncreasesBioProcess() {
		return increasesBioProcess;
	}


	/**
	 * @return the increasedByBioProcess
	 */
	public List<BioProcess> getIncreasedByBioProcess() {
		return increasedByBioProcess;
	}


	/**
	 * @return the decreasesBioProcess
	 */
	public List<BioProcess> getDecreasesBioProcess() {
		return decreasesBioProcess;
	}


	/**
	 * @return the decreasedByBioProcess
	 */
	public List<BioProcess> getDecreasedByBioProcess() {
		return decreasedByBioProcess;
	}


	/**
	 * @return the hasMember
	 */
	public List<Agent> getHasMember() {
		return hasMember;
	}


	/**
	 * @return the includes (agents that composite abundances include)
	 */
	public List<Agent> getIncludes() {
		return includes;
	}



	/**
	 * @param decreasesAbundance the decreasesAbundance to set
	 */
	public void setDecreasesAbundance(List<Agent> decreasesAbundance) {
		this.decreasesAbundance = decreasesAbundance;
	}


	/**
	 * @param decreasedByAbundance the decreasedByAbundance to set
	 */
	public void setDecreasedByAbundance(List<Agent> decreasedByAbundance) {
		this.decreasedByAbundance = decreasedByAbundance;
	}


	/**
	 * @param increasesAbundance the increasesAbundance to set
	 */
	public void setIncreasesAbundance(List<Agent> increasesAbundance) {
		this.increasesAbundance = increasesAbundance;
	}


	/**
	 * @param increasedByAbundance the increasedByAbundance to set
	 */
	public void setIncreasedByAbundance(List<Agent> increasedByAbundance) {
		this.increasedByAbundance = increasedByAbundance;
	}


	/**
	 * @param increasesBioProcess the increasesBioProcess to set
	 */
	public void setIncreasesBioProcess(List<BioProcess> increasesBioProcess) {
		this.increasesBioProcess = increasesBioProcess;
	}


	/**
	 * @param increasedByBioProcess the increasedByBioProcess to set
	 */
	public void setIncreasedByBioProcess(List<BioProcess> increasedByBioProcess) {
		this.increasedByBioProcess = increasedByBioProcess;
	}


	/**
	 * @param decreasesBioProcess the decreasesBioProcess to set
	 */
	public void setDecreasesBioProcess(List<BioProcess> decreasesBioProcess) {
		this.decreasesBioProcess = decreasesBioProcess;
	}


	/**
	 * @param decreasedByBioProcess the decreasedByBioProcess to set
	 */
	public void setDecreasedByBioProcess(List<BioProcess> decreasedByBioProcess) {
		this.decreasedByBioProcess = decreasedByBioProcess;
	}


	/**
	 * @param hasMember the hasMember to set
	 */
	public void setHasMember(List<Agent> hasMember) {
		this.hasMember = hasMember;
	}




	/**
	 * @return the chosenByUser
	 */
	public Boolean getChosenByUser() {
		return chosenByUser;
	}


	/**
	 * @return the bELIdLabel
	 */
	public String getBELIdLabel() {
		return BELIdLabel;
	}


	/**
	 * @return the bELTermLabel
	 */
	public String getBELTermLabel() {
		return BELTermLabel;
	}


	/**
	 * @return the activities
	 */
	public List<Activity> getActivities() {
		return activities;
	}


	/**
	 * @return the qualPropValues
	 */
	public List<QualitativeProperty> getQualPropValues() {
		return qualPropValues;
	}


	/**
	 * @return the aBMCodeLabel
	 */
	public String getABMCodeLabel() {
		return ABMCodeLabel;
	}
	
	/**
	 * sets the aBMCodeLabel
	 * shouldn't normally be used as the ABMCodeLabel is automatically set from the BEL code / ontology label
	 */
	public void setABMCodeLabel(String string) {
		this.ABMCodeLabel = string;
	}


	/**
	 * @return the isActive
	 */
	public Boolean IsInActive() {
		return isInActive;
	}

	/**
	 * @return the isActive
	 */
	public void  setIsInActive(Boolean yesorno) {
		isInActive = yesorno;
	}
	

	/**
	 * @return the plottingColor
	 */
	public String getPlottingColor() {
		return plottingColor;
	}


	/**
	 * @param plottingColor the plottingColor to set (negative integer value for plotting windows)
	 */
	public void setPlottingColor(String plottingColor) {
		this.plottingColor = plottingColor;
	}

	/**
	 * to set the agent's colour (eg black,gray,orange etc)
	 * @param string
	 */
	public void setColorString(String string) {
		this.colorString = string;
	}

	/**
	 * to get the agent's colour (eg black,gray,orange etc)
	 * @param string
	 */
	public String getColorString() {
		return this.colorString;
	}


	/**
	 * @return the runCode
	 */
	public String getGOCode() {
		return goCode;
	}


	/**
	 * @param runCode the runCode to set
	 */
	public void addToGOCode(String goCode) {
		this.goCode += goCode;
	}



	/**
	 * compares current agent (this) with agents2 list via their ontology classes to check whether the latter are subclasses
	 * @param agents2
	 * @return a list of type Agent with all the subclass Agents (down to leaf)
	 */
	public List<Agent> getSubAgents(List<Agent> agents2) {
		ArrayList <Agent> subAgents = new ArrayList <Agent>();
		for (Agent ag : agents2){
			if (this.getAgentInfoOntClass().hasSubClass( ag.getAgentInfoOntClass())){
				subAgents.add(ag);
			}
		}
		return subAgents;
	}



	/**
	 * @return the isBiomarkerForProcess
	 */
	public List<BioProcess> getIsBiomarkerForProcess() {
		return isBiomarkerForProcess;
	}



	/**
	 * @param isBiomarkerForProcess the isBiomarkerForProcess to set
	 */
	public void addIsBiomarkerForProcess(BioProcess isBiomarkerForProcess) {
		this.isBiomarkerForProcess.add(isBiomarkerForProcess);
	}



	/**
	 * @return the components in case of complex or composite agent
	 */
	public ArrayList<Agent> getComponents() {
		//return components;
		return (ArrayList<Agent>) hasMember;
	}



	/**
	 * @param components the components to set in case of complex or composite agent
	 */
	public void setComponents(ArrayList<Agent> components) {
		this.components = components;
	}
	
	
	
	/**
	 * @param component the Agent to add as a component in case of complex or composite agent
	 */
	public void addComponent(Agent component) {
		this.components.add(component);
	}



	/**
	 * @return the regions
	 */
	public ArrayList<Region> getRegions() {
		return regions;
	}



	/**
	 * @return the translocations
	 */
	public List<Translocation> getTranslocations() {
		return translocations;
	}



	/**
	 * @param translocations the translocations to set
	 */
	public void setTranslocations(List<Translocation> translocations) {
		this.translocations = translocations;
	}
	
	
	/**
	 * @param translocations the translocations to set
	 */
	public void addTranslocation(Translocation translocation) {
		this.translocations.add(translocation);
		Utils.appendToFile(new StringBuffer().append("translocation added to agent "+ this.ABMCodeLabel+": "+
		translocation.getFrom().getABMCodeLabel()+" to "+translocation.getTo().getABMCodeLabel()+"\n"), logging);
	}



	/**
	 * @return the size
	 */
	public double getSize() {
		return size;
	}


	/**
	 * true if the agent can reproduce
	 * @param b
	 */
	public void setCanReproduce(boolean b) {
		this.canReproduce = b;
	}
	
	
	/**
	 * true if the agent can reproduce
	 * @param b
	 */
	public Boolean canReproduce() {
		return this.canReproduce;
	}


	/**
	 * sets the includes (list of agents that the composite abundance includes)
	 * @param agents
	 */
	public void setIncludes(List<Agent> agents) {
		this.includes= agents;
	}



	public boolean isComposite() {
		return this.getIncludes().size() > 0;
	}


	public boolean isComplex() {
		return hasMember!=null && hasMember.size()>1;
	}


	/**
	 * connects to Ontology and checks whether the agent (complex) has a member that is an enzyme
	 * @param enzymeURI
	 * @return
	 */
	public boolean hasEnzymeAsMember(String enzymeURI) {
		if (!this.isComplex()) return false;
		for (Agent member : this.getHasMember()){
			if (Ontology.hasSubClass(Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), enzymeURI), 
					                 Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), member.getAgentInfoOntClass().getURI()))){
				return true;
			}
		}
		return false;
	}

	/**
	 * connects to Ontology and returns the enzyme member of the agent (complex) 
	 * @param enzymeURI
	 * @return
	 */
	public Agent getEnzymeMember(String enzymeURI) {
		if (!this.isComplex()) return null;
		if (!this.hasEnzymeAsMember(enzymeURI)) return null;
		for (Agent member : this.getHasMember()){
			if (Ontology.hasSubClass(Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), enzymeURI), 
					                 Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), member.getAgentInfoOntClass().getURI()))){
				return member;
			}
		}
		return null;
	}


	/**
	 * connects to the Ontology and checks whether the current Agent or - in case of a complex - one of its members 
	 *   is a subclass of allostericEnzymeURI in the ontology
	 * @param allostericEnzymeURI
	 * @return
	 */
	public boolean isAllostericEnzyme(String allostericEnzymeURI) {
		if (Ontology.hasSubClass(Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), allostericEnzymeURI), 
                Ontology.getOntClassFromURIString(this.getAgentInfoOntClass().getOntModel(), this.getAgentInfoOntClass().getURI()))){
			return true;
		}
		if (this.isComplex()){
			ArrayList <Agent> theMembers = (ArrayList<Agent>) this.getHasMember();
			for (Agent member : theMembers){
				if (Ontology.hasSubClass(Ontology.getOntClassFromURIString(member.getAgentInfoOntClass().getOntModel(), allostericEnzymeURI), 
		                Ontology.getOntClassFromURIString(member.getAgentInfoOntClass().getOntModel(), member.getAgentInfoOntClass().getURI()))){
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * 
	 * @param object
	 * @param agents
	 * @return an arraylist of agents in which the agent is contained as a member
	 */
	public ArrayList<Agent> getMemberOf(Agent object, List<Agent> agents) {
		if (this.isMemberOf.size() > 0)
			return (ArrayList<Agent>) this.isMemberOf;
		ArrayList<Agent> memberList = new ArrayList<Agent>();
		for (Agent a : agents){
			if (a.isComplex() && a.getHasMember().contains(object))
				memberList.add(a);
		}
		setIsMemberOf(memberList);
		return (ArrayList<Agent>) this.isMemberOf;
	}


	private void setIsMemberOf(List<Agent> isMemberOf) {
		this.isMemberOf = isMemberOf;
	}


	/**
	 * checks whether complexAgent is a complex agent and then iterates its members to check if the agent is a member of it
	 * @param enzymeAgent
	 * @return
	 */
	public boolean memberOf(Agent complexAgent) {
		if (!complexAgent.isComplex())
			return false;
		if (complexAgent.getHasMember().contains(this))
			return true;
		return false;
	}


	/**
	 * every event (such as complex formation, ontly used for this so far!)
	 * receives a running number 
	 *   (important to make agents execute the different events in a random order, not always the same event first
	 *    as this would have statical impact)
	 * @return
	 */
	public int getCurrentEventNumber() {
		return this.currentEventNumber;
	}


	public void setCurrentEventNumber(int currentEventNumber) {
		this.currentEventNumber = currentEventNumber;
	}

	

    
}
