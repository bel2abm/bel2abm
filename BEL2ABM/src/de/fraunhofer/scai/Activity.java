package de.fraunhofer.scai;
/**
 * 
 */
//package de.fraunhofer.scai.BEL2ABM;

import org.openbel.framework.api.Kam.KamNode;


/**
 * @author latitude_user
 * biologicalProcess(22) increases molecularActivity(abundance(5))
 */
public class Activity {
	
	//static List<String> activityTypes = new ArrayList<String>(Arrays.asList("catalyticActivity","chaperoneActivity",
		//	"gtpBoundActivity","kinaseActivity","molecularActivity","peptidaseActivity", "phosphataseActivity",
			//"ribosylationActivity",	"transcriptionalActivity","transportActivity"));
	private ActivityType type;     		//**molecularActivity**(abundance(5)) increases  biologicalProcess(22)
	private Relationship relationship;  //molecularActivity(abundance(5)) **increases**  biologicalProcess(22)
	private Object theObject;       		// molecularActivity(abundance(5)) increases  **biologicalProcess(22)**
	private KamNode activityNode;
	
	/**
	 * 
	 * @param type
	 * @param relation
	 * @param what   either process, agent or activity
	 */
	private Activity(ActivityType type, Relationship relation, Object what){
		this.type = ActivityType.MOLECULAR; //default
		for (ActivityType t : ActivityType.values()){
			if (t == type){
				this.type = type;
				break;
			} 
		}
		this.relationship = Relationship.ASSOCIATION;  //default
		
		for (Relationship r : Relationship.values()){
			if (relation != null && r.toString().equals(relation.toString())){
				this.relationship = relation;
			} 
		}
		this.theObject = what;  //either process, agent or activity
		//System.out.println("\ntype: "+type+"relationship: "+relationship+" object: "+what);
	}
	
	/**
	 * 
	 * @param type
	 */
	public Activity(ActivityType type, KamNode activityNode){
		this.type = ActivityType.MOLECULAR; //default
		this.activityNode = activityNode;
		for (ActivityType t : ActivityType.values()){
			if (t == type){
				this.type = type;
				break;
			} 
		}
		this.theObject = "";  //default
		this.relationship = Relationship.ASSOCIATION; //default
	}
	
	/**
	 * 
	 * @param relation
	 */
	public void setRelationship(Relationship relation){
		this.relationship = Relationship.ASSOCIATION;  //default
		
		for (Relationship r : Relationship.values()){
			if (relation != null && r.toString().equals(relation.toString())){
				this.relationship = relation;
			} 
		}
	}
	

	/**
	 * 
	 * @param what   either process, agent or activity
	 */
	public void setObject(Object what){
		this.theObject = what;  //either process, agent or activity
	}

	
	
	/**
	 * one of INCREASES, INCREASEDBY, DECREASES, DECREASEDBY, ASSOCIATION
	 * @author latitude_user
	 *
	 */
	public enum Relationship {
	    INCREASES, INCREASEDBY, DECREASES, DECREASEDBY, ASSOCIATION
	}
	
	public enum ActivityType {
		CATALYTIC, CHAPERONE,
		GTPBOUND,KINASE,MOLECULAR,PEPTIDASE, PHOSPHATASE,
		RIBOSYLATION,TRANSCRIPTIONAL,TRANSPORT
	}

	/**
	 * 
	 * @param act
	 * @return
	 */
	public static ActivityType getActivityType(String act) {
		if (act.equalsIgnoreCase("calatyticActivity"))
			return ActivityType.CATALYTIC;
		if (act.equalsIgnoreCase("chaperoneActivity"))
			return ActivityType.CHAPERONE;
		if (act.equalsIgnoreCase("gtpBoundActivity"))
			return ActivityType.GTPBOUND;
		if (act.equalsIgnoreCase("kinaseActivity"))
			return ActivityType.KINASE;
		if (act.equalsIgnoreCase("molecularActivity"))
			return ActivityType.MOLECULAR;
		if (act.equalsIgnoreCase("peptidaseActivity"))
			return ActivityType.PEPTIDASE;
		if (act.equalsIgnoreCase("phosphataseActivity"))
			return ActivityType.PHOSPHATASE;
		if (act.equalsIgnoreCase("ribosylationActivity"))
			return ActivityType.RIBOSYLATION;
		if (act.equalsIgnoreCase("transcriptionalActivity"))
			return ActivityType.TRANSCRIPTIONAL;
		if (act.equalsIgnoreCase("transportActivity"))
			return ActivityType.TRANSPORT;
		
		return null;
	}

	/**
	 * eg **molecularActivity**(abundance(5))
	 * @return the type
	 */
	public ActivityType getType() {
		return type;
	}

	/**
	 * @return the relationship   one of INCREASES, INCREASEDBY, DECREASES, DECREASEDBY, ASSOCIATION
	 */
	public Relationship getRelationship() {
		return relationship;
	}

	/**
	 * subject - predicate - object, eg molecularActivity(abundance(5)) increases  **biologicalProcess(22)**
	 * @return the object  can be either a process or an agent
	 */
	public Object getObject() {
		return theObject;
	}

	/**
	 * @return the activityNode
	 */
	public KamNode getActivityNode() {
		return activityNode;
	}


}
