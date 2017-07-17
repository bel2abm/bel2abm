/**
 * 
 */
package de.fraunhofer.scai;

import java.util.ArrayList;

import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.internal.KAMStoreDaoImpl.Annotation;

/**
 * @author latitude_user
 *
 */
public class Translocation {
	Agent translocatedAgent;
	Region from;
	Region to;
	ArrayList<Annotation> annotations;
	KamNode tNode;              // eg translocation(abundance(15),29,30)
	Agent increasesAgent;
	Agent decreasesAgent;
	Activity increasesAgentActivity;
	Activity decreasesAgentActivity;
	BioProcess increasesBioProcess;
	BioProcess decreasesBioProcess;
	
	public Translocation (Region fr, Region t, KamNode node, Agent a){
		this.translocatedAgent = a;
		this.from = fr;
		this.to = t;
		this.annotations = new ArrayList<Annotation>();
		this.tNode = node;
	}
	
	public Translocation (){
		this.annotations = new ArrayList<Annotation>();
	}

	/**
	 * @return the from
	 */
	public Region getFrom() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(Region from) {
		this.from = from;
	}

	/**
	 * @return the to
	 */
	public Region getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(Region to) {
		this.to = to;
	}

	public void addAnnotation(Annotation a) {
		this.annotations.add(a);
	}

	public ArrayList<Annotation> getAnnotations() {
		return this.annotations;
	}

	/**
	 * @return the tNode
	 * eg: translocation(abundance(15),29,30)
	 */
	public KamNode gettNode() {
		return tNode;
	}

	/**
	 * @param tNode the tNode to set
	 */
	public void settNode(KamNode tNode) {
		this.tNode = tNode;
	}

	/**
	 * @return the increasesAgent
	 */
	public Agent getIncreasesAgent() {
		return increasesAgent;
	}

	/**
	 * @param increasesAgent the increasesAgent to set
	 */
	public void setIncreasesAgent(Agent increasesAgent) {
		this.increasesAgent= increasesAgent;
	}

	/**
	 * @return the decreasesAgent
	 */
	public Agent getDecreasesAgent() {
		return decreasesAgent;
	}

	/**
	 * @param decreasesAgent the decreasesAgent to set
	 */
	public void setDecreasesAgent(Agent decreasesAgent) {
		this.decreasesAgent= decreasesAgent;
	}

	/**
	 * @return the increasesAgentActivity
	 */
	public Activity getIncreasesAgentActivity() {
		return increasesAgentActivity;
	}

	/**
	 * @param increasesAgentActivity the increasesAgentActivity to set
	 */
	public void setIncreasesAgentActivity(Activity increasesAgentActivity) {
		this.increasesAgentActivity = increasesAgentActivity;
	}

	/**
	 * @return the decreasesAgentActivity
	 */
	public Activity getDecreasesAgentActivity() {
		return decreasesAgentActivity;
	}

	/**
	 * @param decreasesAgentActivity the decreasesAgentActivity to set
	 */
	public void setDecreasesAgentActivity(Activity decreasesAgentActivity) {
		this.decreasesAgentActivity = decreasesAgentActivity;
	}

	/**
	 * @return the increasesBioProcess
	 */
	public BioProcess getIncreasesBioProcess() {
		return increasesBioProcess;
	}

	/**
	 * @param increasesBioProcess the increasesBioProcess to set
	 */
	public void setIncreasesBioProcess(BioProcess increasesBioProcess) {
		this.increasesBioProcess = increasesBioProcess;
	}

	/**
	 * @return the decreasesBioProcess
	 */
	public BioProcess getDecreasesBioProcess() {
		return decreasesBioProcess;
	}

	/**
	 * @param decreasesBioProcess the decreasesBioProcess to set
	 */
	public void setDecreasesBioProcess(BioProcess decreasesBioProcess) {
		this.decreasesBioProcess = decreasesBioProcess;
	}

	/**
	 * 
	 * @return this.increasesAgent==null && this.increasesAgentActivity==null && this.increasesBioProcess ==null &&
				this.decreasesAgent==null && this.decreasesAgentActivity==null && this.decreasesBioProcess == null
	 */
	public boolean noFurtherIncreasesOrDecreases() {
		return (
				this.increasesAgent==null && this.increasesAgentActivity==null && this.increasesBioProcess ==null &&
				this.decreasesAgent==null && this.decreasesAgentActivity==null && this.decreasesBioProcess == null
				);
	}

	/**
	 * @return the translocatedAgent
	 */
	public Agent getTranslocatedAgent() {
		return translocatedAgent;
	}

	/**
	 * @param translocatedAgent the translocatedAgent to set
	 */
	public void setTranslocatedAgent(Agent translocatedAgent) {
		this.translocatedAgent = translocatedAgent;
	}
	
	
}
