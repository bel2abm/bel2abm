package de.fraunhofer.scai;
/**
 * 
 */
//package de.fraunhofer.scai.BEL2ABM;

import java.util.ArrayList;

/**
 * @author latitude_user
 *
 */
public class Reaction {

	private int ID;
	private ArrayList<Agent> reactants;
	private ArrayList<Agent> products;
	private ArrayList<Object> increases;
	private ArrayList<Object> decreases;
	
	public ArrayList<Object> getIncreases() {
		return increases;
	}

	public void setIncreases(ArrayList<Object> increases) {
		this.increases = increases;
	}

	public ArrayList<Object> getDecreases() {
		return decreases;
	}

	public void setDecreases(ArrayList<Object> decreases) {
		this.decreases = decreases;
	}

	public Reaction(int id, ArrayList<Agent> reactants, ArrayList<Agent> products){
		this.ID = id;
		this.reactants = reactants;
		this.products = products;
		this.increases = new ArrayList<Object>();
		this.decreases = new ArrayList<Object>();
	}

	public ArrayList<Agent> getReactants() {
		return reactants;
	}

	public ArrayList<Agent> getProducts() {
		return products;
	}
	
	public static boolean exists(ArrayList<Reaction> reactions, int id) {
		for (Reaction r : reactions){
			if (r.getID() == id )
				return true;
		}
		return false;
	}
	
	public void setReactants(ArrayList<Agent> reactants) {
		this.reactants = reactants;
	}

	public void setProducts(ArrayList<Agent> products) {
		this.products = products;
	}

	/**
	 * 
	 * @param reactions
	 * @param id
	 * @return the reaction instance corresponding to id, else null
	 */
	public static Reaction getReaction(ArrayList<Reaction> reactions, int id) {
		for (Reaction r : reactions){
			if (r.getID() == id )
				return r;
		}
		return null;
	}

	public int getID() {
		return this.ID;
	}
}
