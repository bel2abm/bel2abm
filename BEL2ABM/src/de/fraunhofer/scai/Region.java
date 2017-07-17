/**
 * 
 */
package de.fraunhofer.scai;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author Michaela Michi Guendel
 * (I decided to create a class for this to be able to accomodate further information later on, eg from ontology)
 *
 */
public class Region {
	private String ABMCodeLabel;	//eg Liver
	private OntClass ontClass;
	private String dictionaryName; 	//eg Anatomy
	private ArrayList<ProvenanceType> provenance;  //either kam or ontology, or both
	
	public Region(String abmCodeLabel, ArrayList<ProvenanceType> pt){
		this.ABMCodeLabel = abmCodeLabel;
		this.provenance = pt;
	}

	public enum ProvenanceType {
		KAM, ONTOLOGY
	}
	
	public void addProvenanceType(ProvenanceType pt){
		this.provenance.add(pt);
	}
	
	public List<ProvenanceType> getProvenance(){
		return this.provenance;
	}
	
	/**
	 * @return the aBMCodeLabel
	 */
	public String getABMCodeLabel() {
		return ABMCodeLabel;
	}

	/**
	 * @param aBMCodeLabel the aBMCodeLabel to set
	 */
	public void setABMCodeLabel(String aBMCodeLabel) {
		ABMCodeLabel = aBMCodeLabel;
	}

	/**
	 * @return the ontClass
	 */
	public OntClass getOntClass() {
		return ontClass;
	}

	/**
	 * @param ontClass the ontClass to set
	 */
	public void setOntClass(OntClass ontClass) {
		this.ontClass = ontClass;
	}

	/**
	 * @return the dictionaryName
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * @param dictionaryName the dictionaryName to set
	 */
	public void setDictionaryName(String dictionaryName) {
		this.dictionaryName = dictionaryName;
	}

	/**
	 * to check whether the region with the name name and the dictionary dictionary already exists in regions
	 * @param regions
	 * @param name
	 * @param dictionary
	 * @return
	 */
	public static boolean exists(ArrayList<Region> regions, String name,
			String dictionary) {
		for (Region r : regions){
			if (r.getABMCodeLabel().equals(name) && r.getDictionaryName().equals(dictionary))
				return true;
		}
		return false;
	}
	
	/**
	 * return the region with the name name and the dictionary dictionary from regions
	 * @param regions
	 * @param name
	 * @param dictionary
	 * @return
	 */
	public static Region getRegion(ArrayList<Region> regions, String name,
			String dictionary) {
		for (Region r : regions){
			if (r.getABMCodeLabel().equals(name) && r.getDictionaryName().equals(dictionary))
				return r;
		}
		return null;
	}

	

	

}
