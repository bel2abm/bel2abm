/**
 * 
 */
package de.fraunhofer.scai;

import java.util.ArrayList;
import java.util.List;

import org.openbel.framework.api.Kam.KamNode;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author latitude_user
 *
 */
public class BioProcess {
	private KamNode processInfoKamNode;
	private List<OntClass> processInfoOntClass;  //in case more than 1 class has been annotated with the BEL term
	private String label;
	private String BELTermLabel;
	private String BELIdLabel;
	private String ABMCodeLabel;
	
	private List<Agent> decreasesAbundance;
	private List<Agent> decreasedByAbundance;
	private List<Agent> increasesAbundance;
	private List<Agent> increasedByAbundance;
	private List<BioProcess> increasesBioProcess;
	private List<BioProcess> increasedByBioProcess;
	private List<BioProcess> decreasesBioProcess;
	private List<BioProcess> decreasedByBioProcess;
	
	private String logging;
	
	private String code;
	

	/**
	 * a bioProcess has a KamNode corresponding to the BEL code, an OntClass corresponding to its mapped ontology class
	 * and a logging string pointing to log file
	 * @param key
	 * @param ontClass
	 * @param logging
	 * @param b 
	 * @param BELIdLabel   eg "bioProcess(3)"
	 */
	public BioProcess(KamNode key, List<OntClass> ontClass, String logging, String BELIdLabel, String BELTermLabel) {
		this.processInfoKamNode = key;
		this.processInfoOntClass = ontClass;
		if (this.processInfoOntClass != null && this.processInfoOntClass.size()>0) {
			this.label = this.processInfoOntClass.get(0).getLabel(null);
		} else {
			this.label = null;
		}
		this.BELTermLabel = BELTermLabel;
		this.BELIdLabel = BELIdLabel;
		this.ABMCodeLabel = this.label;
		if (ABMCodeLabel == null){
			this.ABMCodeLabel = this.BELTermLabel.substring(this.BELTermLabel.indexOf("(")+1, this.BELTermLabel.length());
		}
		if (this.ABMCodeLabel.contains(":"))
			this.ABMCodeLabel = this.ABMCodeLabel.substring(this.ABMCodeLabel.indexOf(":")+1);
		this.ABMCodeLabel = this.ABMCodeLabel.replaceAll("[ (),\"]+", "_");
		if(this.ABMCodeLabel.startsWith("_"))
			this.ABMCodeLabel =this.ABMCodeLabel.substring(1, this.ABMCodeLabel.length());
		if (this.ABMCodeLabel.endsWith("_"))
			this.ABMCodeLabel =this.ABMCodeLabel.substring(0, this.ABMCodeLabel.length()-1);
		
		this.decreasesAbundance = new ArrayList<Agent>();
		this.decreasedByAbundance = new ArrayList<Agent>();
		this.increasesAbundance = new ArrayList<Agent>();
		this.increasedByAbundance = new ArrayList<Agent>();
		this.increasesBioProcess = new ArrayList<BioProcess>();
		this.increasedByBioProcess = new ArrayList<BioProcess>();
		this.decreasesBioProcess = new ArrayList<BioProcess>();
		this.decreasedByBioProcess = new ArrayList<BioProcess>();
		
		this.logging = logging;
		
		Utils.appendToFile(new StringBuffer().append("New bioProcess added to knowledge base: \n\t"
				+ "label: "+this.label 
				+" * BEL term: "+this.BELTermLabel +"\n\t"+" * BEL ID label: "+this.BELIdLabel + "\n"), logging);
		
		if (this.label != null)
			Utils.appendToFile(new StringBuffer().append("\t * has onto URI: "+ this.processInfoOntClass.get(0).getURI()+"\n"), logging);
		
		this.code = "";  //is filled from ABMCode
	}



	/**
	 * @return the decreasesAbundance
	 */
	public List<Agent> getDecreasesAbundance() {
		return decreasesAbundance;
	}


	/**
	 * @param decreasesAbundance the decreasesAbundance to set
	 */
	public void setDecreasesAbundance(List<Agent> decreasesAbundance) {
		this.decreasesAbundance = decreasesAbundance;
	}


	/**
	 * @return the decreasedByAbundance
	 */
	public List<Agent> getDecreasedByAbundance() {
		return decreasedByAbundance;
	}


	/**
	 * @param decreasedByAbundance the decreasedByAbundance to set
	 */
	public void setDecreasedByAbundance(List<Agent> decreasedByAbundance) {
		this.decreasedByAbundance = decreasedByAbundance;
	}


	/**
	 * @return the increasesAbundance
	 */
	public List<Agent> getIncreasesAbundance() {
		return increasesAbundance;
	}


	/**
	 * @param increasesAbundance the increasesAbundance to set
	 */
	public void setIncreasesAbundance(List<Agent> increasesAbundance) {
		this.increasesAbundance = increasesAbundance;
	}


	/**
	 * @return the increasedByAbundance
	 */
	public List<Agent> getIncreasedByAbundance() {
		return increasedByAbundance;
	}


	/**
	 * @param increasedByAbundance the increasedByAbundance to set
	 */
	public void setIncreasedByAbundance(List<Agent> increasedByAbundance) {
		this.increasedByAbundance = increasedByAbundance;
	}


	/**
	 * @return the increasesBioProcess
	 */
	public List<BioProcess> getIncreasesBioProcess() {
		return increasesBioProcess;
	}


	/**
	 * @param increasesBioProcess the increasesBioProcess to set
	 */
	public void setIncreasesBioProcess(List<BioProcess> increasesBioProcess) {
		this.increasesBioProcess = increasesBioProcess;
	}


	/**
	 * @return the increasedByBioProcess
	 */
	public List<BioProcess> getIncreasedByBioProcess() {
		return increasedByBioProcess;
	}


	/**
	 * @param increasedByBioProcess the increasedByBioProcess to set
	 */
	public void setIncreasedByBioProcess(List<BioProcess> increasedByBioProcess) {
		this.increasedByBioProcess = increasedByBioProcess;
	}


	/**
	 * @return the decreasesBioProcess
	 */
	public List<BioProcess> getDecreasesBioProcess() {
		return decreasesBioProcess;
	}


	/**
	 * @param decreasesBioProcess the decreasesBioProcess to set
	 */
	public void setDecreasesBioProcess(List<BioProcess> decreasesBioProcess) {
		this.decreasesBioProcess = decreasesBioProcess;
	}


	/**
	 * @return the decreasedByBioProcess
	 */
	public List<BioProcess> getDecreasedByBioProcess() {
		return decreasedByBioProcess;
	}


	/**
	 * @param decreasedByBioProcess the decreasedByBioProcess to set
	 */
	public void setDecreasedByBioProcess(List<BioProcess> decreasedByBioProcess) {
		this.decreasedByBioProcess = decreasedByBioProcess;
	}


	/**
	 * @return the processInfoKamNode
	 */
	public KamNode getProcessInfoKamNode() {
		return processInfoKamNode;
	}


	/**
	 * @return the processInfoOntClass
	 */
	public List<OntClass> getProcessInfoOntClass() {
		return processInfoOntClass;
	}


	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}


	/**
	 * @return the bELTermLabel
	 */
	public String getBELTermLabel() {
		return BELTermLabel;
	}


	/**
	 * @return the bELIdLabel
	 */
	public String getBELIdLabel() {
		return BELIdLabel;
	}


	public String getABMCodeLabel() {
		return ABMCodeLabel;
	}


	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}


	/**
	 * @param code the code to set
	 */
	public void addToCode(String code) {
		this.code += code;
	}
	

}
