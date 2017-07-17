/**
 * 
 */
package de.fraunhofer.scai;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author latitude_user
 *
 */
public class QualitativeProperty {
	
	private OntClass quality;
	
	public QualitativeProperty(OntClass qual){
		this.quality = qual;
	}

	/**
	 * @return the quality
	 */
	public OntClass getQuality() {
		return quality;
	}

}
