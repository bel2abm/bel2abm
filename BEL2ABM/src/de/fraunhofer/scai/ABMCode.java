package de.fraunhofer.scai;
/**
 * 
 */
//package de.fraunhofer.scai.BEL2ABM;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamEdge;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.KamStore;
import org.openbel.framework.api.KamStoreException;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;
import org.openbel.framework.internal.KAMStoreDaoImpl.Annotation;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelStatement;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fraunhofer.scai.Activity.ActivityType;
import de.fraunhofer.scai.Activity.Relationship;
import de.fraunhofer.scai.mathML.MathMLParser;

/**
 * @author Michaela Michi Guendel, Fraunhofer SCAI
 * 
 **         TODO Not yet implemented / to be done: 
 *         RelationshipType.ANALOGOUS; 
 *         RelationshipType.CAUSES_NO_CHANGE; RelationshipType.HAS_MODIFICATION;
 *         RelationshipType.HAS_VARIANT;
 *         RelationshipType.NEGATIVE_CORRELATION;
 *         RelationshipType.ORTHOLOGOUS; RelationshipType.POSITIVE_CORRELATION;
 *         RelationshipType.RATE_LIMITING_STEP_OF;
 *         RelationshipType.TRANSCRIBED_TO; RelationshipType.TRANSLATED_TO;
 */
public class ABMCode {
	private List<Agent> agents;
	private List<BioProcess> bioProcesses;
	private HashMap<String, String> variableHash; // to save eg -COUNT
													// http://purl.org/obo/owl/PATO#PATO_0001555
													// from the settings file
	private String qualPropUri;
	private String quantPropUri;
	private String ABMCodeProp;
	@SuppressWarnings("unused")
	private String energyProp;
	private String reactionDistance; // distance between 2 entities in which
										// they start to react
	private String increasesUri; // URI used in ontology meaning a process
									// increases either a material entity or a
									// process
	private String decreasesUri; // URI used in ontology meaning a process
									// decreases either a material entity or a
									// process
	@SuppressWarnings("unused")
	private String increasedByUri; // URI used in ontology meaning a process is
									// increased by either a material entity or
									// a process
	@SuppressWarnings("unused")
	private String decreasedByUri; // URI used in ontology meaning a process is
									// decreased by either a material entity or
									// a process
	private String reproduceURI; // quality to indicate whether a class can
									// reproduce; eg cells and organisms can,
									// proteins cannot
	private ArrayList<String> agentReproduceAlgo; // algorithm to be used for
													// agent reproduction (URI
													// of class / individual
													// that has MathML attached)
													// first position is URI,
													// all following positions
													// contain variable values
	private ArrayList<String> agentDefaultReproduceAlgo; // default
															// reproduceAlgorithm
	private ArrayList<String> mathmlProp;

	private static Boolean verbous;

	private static final String settings = "BEL2ABM.ini";
	private static final String logging = "ABMCode.log";
	private static String colors[] = { "gray", "red", "orange", "brown",
			"yellow", "green", "lime", "turquoise", "cyan", "sky", "blue",
			"violet", "magenta", "pink", "black" };
	private static String colorIntegers[] = { "-7500403", "-2674135",
			"-955883", "-6459832", "-1184463", "-10899396", "-13840069",
			"-14835848", "-11221820", "-13791810", "-13345367", "-8630108",
			"-5825686", "-2064490", "-16777216" };
	private String outPath;
	private HashMap<KamNode, OntClass> allPossibleAgents;
	private ArrayList<Region> regions;
	private ArrayList<Reaction> reactions;
	private ArrayList<Activity> activities;
	private String homeostatic_concentrFile;
	private String homeostatic_concentr_default;
	private String reproductionCondition;
	private String inactiveProperty;
	private String lifePropertyURI;
	private String[] noHomeostasis; // eg 0: property
									// "number controlled by homeostasis" 1:
									// false (class)
	private String[] agentPulses; // eg 0: property is_output_of 1: class
									// hematopoiesis
	private HashMap<String, String> maxHomeostaticValues;
	private HashMap<String, String> homeostaticConcentrations;
	private HashMap<String, String> upperLimitConcentrations;
	private static String belAnnoProp; // "BELterm" in HuPSON; ie, annotation
										// property used to annotate onto
										// classes with BEL term
	private static String processURI = "";
	private static String enzymeURI = "";
	private static String allostericEnzymeURI = "";

	public ABMCode(Boolean v) {
		this.agents = new ArrayList<Agent>();
		this.activities = new ArrayList<Activity>();
		this.bioProcesses = new ArrayList<BioProcess>();
		this.qualPropUri = "";
		this.quantPropUri = "";
		this.inactiveProperty = ""; // points to PATO class; need to check
									// whether the qualprop
		this.outPath = "";
		this.mathmlProp = new ArrayList<String>();
		this.agentReproduceAlgo = new ArrayList<String>();
		this.agentDefaultReproduceAlgo = new ArrayList<String>();
		this.variableHash = new HashMap<String, String>();
		this.ABMCodeProp = "";
		this.energyProp = "";
		this.reactionDistance = "5"; // default ??
		this.increasedByUri = "";
		this.increasesUri = "";
		this.decreasedByUri = "";
		this.decreasesUri = "";
		this.reproduceURI = "";
		this.lifePropertyURI = "";
		this.regions = new ArrayList<Region>();
		this.reactions = new ArrayList<Reaction>();
		this.homeostatic_concentrFile = "";
		this.homeostatic_concentr_default = "";
		this.homeostaticConcentrations = new HashMap<String, String>();
		this.upperLimitConcentrations = new HashMap<String, String>();
		this.reproductionCondition = "";
		this.noHomeostasis = new String[2];
		this.agentPulses = new String[2];
		this.maxHomeostaticValues = new HashMap<String, String>();

		verbous = v;

		readSettings();
	}

	/**
	 * reads settings file BEL2ABM.ini
	 * 
	 * @return
	 */
	private List<String> readSettings() {
		Utils.writeToFile(
				new StringBuffer().append("Reading settings file: " + settings
						+ " \n"), logging);
		List<String> settArray = new ArrayList<String>();
		try {
			settArray = Utils.readLinesFromJar(settings);
		} catch (IOException e) {
			try{
				settArray = Utils.readLines( "files"+File.separator+ settings);
			} catch (FileNotFoundException e2) {
				Utils.appendToFile(new StringBuffer().append(e.toString()
						+"\nError: settings file "+settings+" not found. Checked in jar at "+"."+File.separator+settings+" and "+ 
						 "files"+File.separator+ settings+" Exiting. \n"), logging);
				System.exit(-1);
			}
		}
		String command = "";
		for (String settingLine : settArray) {
			try {
				command = settingLine.split("\t")[0];
				if (command.startsWith("-")) {
					switch (command) {
					case ("-qualProp"):
						this.qualPropUri = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("Qualitative property URI: "
										+ this.qualPropUri + " \n"), logging);
						break;
					case ("-quantProp"):
						this.quantPropUri = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("Quantitative property URI: "
										+ this.quantPropUri + " \n"), logging);
						break;
					case ("-ABMCodeProp"):
						this.ABMCodeProp = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("ABM code annotation property URI: "
										+ this.ABMCodeProp + " \n"), logging);
						break;
					case ("-energyProperty"):
						this.energyProp = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("Energy property URI: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-reactionDistance"):
						this.reactionDistance = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("Reaction distance between to entities needed to react: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-increases"):
						this.increasesUri = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("process - process/material entity INCREASES Uri set to: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-decreases"):
						this.decreasesUri = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("process - process/material entity DECREASES Uri set to: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-increasedBy"):
						this.increasedByUri = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("process - process/material entity INCREASED_BY Uri set to: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-decreasedBy"):
						this.decreasedByUri = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("process - process/material entity DECREASED_BY Uri set to: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-BELTermAnnotationProperty"):
						belAnnoProp = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("benAnnoProp set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-processURI"):
						processURI = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer().append("processURI set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-enzyme"):
						enzymeURI = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer().append("enzymeURI set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;  
					case ("-allostericEnzyme"):
						allostericEnzymeURI = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer().append("allostericEnzymeURI set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-reproduce"):
						this.reproduceURI = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("reproduceURI set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-agentreproducealgorithm"):
						String p = settingLine.substring(settingLine
								.indexOf("\t") + 1);
						for (String s : p.split("\t")) {
							this.agentReproduceAlgo.add(s);
							Utils.appendToFile(new StringBuffer()
									.append("reproduce algorithm and values: "
											+ s + " \n"), logging);
						}
						break;
					case ("-agentreproducealgorithm_default"):
						String defalgo = settingLine.substring(settingLine
								.indexOf("\t") + 1);
						for (String s : defalgo.split("\t")) {
							this.agentDefaultReproduceAlgo.add(s);
							Utils.appendToFile(
									new StringBuffer()
											.append("default reproduce algorithm and values: "
													+ s + " \n"), logging);
						}
						break;
					case ("-inactiveProperty"):
						this.inactiveProperty = settingLine.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("inactiveProperty set to: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-mathmlProp"):
						this.mathmlProp.add(settingLine.split("\t")[1]);
						Utils.appendToFile(
								new StringBuffer().append("MathML property: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-homeostatic_concentrations"):
						this.homeostatic_concentrFile = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("File used to set homeostatic concentrations: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-lifePropertyURI"):
						this.lifePropertyURI = settingLine.split("\t")[1];
						Utils.appendToFile(
								new StringBuffer()
										.append("Agents with the following lifePropertyURI quality will have a life span: "
												+ settingLine.split("\t")[1]
												+ " \n"), logging);
						break;
					case ("-homeostatic_concentrations_default"):
						this.homeostatic_concentr_default = settingLine
								.split("\t")[1];
						Utils.appendToFile(new StringBuffer()
								.append("default homeostatic concentration: "
										+ settingLine.split("\t")[1] + " \n"),
								logging);
						break;
					case ("-noHomeostasis"):
						this.noHomeostasis[0] = settingLine.split("\t")[1];
						this.noHomeostasis[1] = settingLine.split("\t")[2];
						Utils.appendToFile(
								new StringBuffer()
										.append("participation in homeostasis controlled by property: "
												+ this.noHomeostasis[0] + " \n"),
								logging);
						Utils.appendToFile(
								new StringBuffer()
										.append("                             and by class \"false\": "
												+ this.noHomeostasis[1] + " \n"),
								logging);
						break;
					case ("-isBodilyDevelopmentalProcess"):
						this.agentPulses[0] = settingLine.split("\t")[1];
						this.agentPulses[1] = settingLine.split("\t")[2];
						Utils.appendToFile(new StringBuffer()
								.append("agents that have the property: "
										+ this.agentPulses[0] + " "), logging);
						Utils.appendToFile(
								new StringBuffer()
										.append("  pointing to the class : "
												+ this.agentPulses[1]
												+ " are introduced via periodic pulses. \n"),
								logging);
						break;

					// else it's a different kind of variable, safe in hashmap
					default:
						try {
							variableHash.put(settingLine.split("\t")[0],
									settingLine.split("\t")[1]);
							Utils.appendToFile(new StringBuffer()
									.append("\t variable info: "
											+ settingLine.split("\t")[0] + "\t"
											+ settingLine.split("\t")[1]
											+ " \n"), logging);
						} finally {
						}
						break;
					}
				}
			} catch (IndexOutOfBoundsException iobe) {
				Utils.appendToFile(
						new StringBuffer()
								.append("Warning: Check "
										+ logging
										+ " settings file, at least one of the settings has not"
										+ " been set correctly.  \n"), logging);
			}
		}
		return null;
	}

	/**
	 * writes the code
	 * 
	 * @param outPath
	 */
	public void create(String outPath) {
		this.outPath = outPath;
		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		Utils.writeToFile(
				new StringBuffer()
						.append(";; auto-generated NetLogo code. "
								+ "Author: Michaela Michi Guendel, Fraunhofer SCAI Bio, St. Augustin, Germany.\n"+
								";; "+currentTimestamp.toString()+"\n"),
				outPath);
	}

	private void addToCode(String code) {
		Utils.appendToFile(new StringBuffer().append(code + "\n"), this.outPath);
	}

	/**
	 * creates the agents; doesn't do anything else this far
	 * 
	 * @param allPossibleAgents
	 *            all agents corresponding to abundance nodes in KAM
	 * @param agentsChosenByUser
	 *            those that the user wants to display in the simulation
	 * @param abundanceIdTermMapping
	 */
	private void init(HashMap<KamNode, OntClass> allPossibleAgents,
			HashMap<KamNode, OntClass> agentsChosenByUser,
			HashMap<String, String> abundanceIdTermMapping, OntModel m,
			HashMap<KamNode, ArrayList<Region>> agentToRegionMap,
			HashMap<KamNode, ArrayList<Region>> agentProducedInMap) {
		this.homeostaticConcentrations = Utils.readTabsepFile(
				this.homeostatic_concentrFile, 0, 2);
		if (this.homeostaticConcentrations.isEmpty())
			this.homeostaticConcentrations = Utils.readTabsepFile(
					"files"+ File.separator + this.homeostatic_concentrFile, 0, 2);
		Utils.appendToFile(new StringBuffer()
				.append("\nFollowing homeostatic concentrations found in "
						+ this.homeostatic_concentrFile + ":\n"), logging);
		for (String k : homeostaticConcentrations.keySet()) {
			Utils.appendToFile(
					new StringBuffer().append("\t" + k + ":"
							+ homeostaticConcentrations.get(k) + " \n"),
					logging);
		}
		this.calculateMaxHomeostaticValue(allPossibleAgents.values());
		this.upperLimitConcentrations = Utils.readTabsepFile(
				this.homeostatic_concentrFile, 0, 3);
		Utils.appendToFile(new StringBuffer()
				.append("\nFollowing upper limits for concentrations found in "
						+ this.homeostatic_concentrFile + ":\n"), logging);
		for (String k : upperLimitConcentrations.keySet()) {
			Utils.appendToFile(
					new StringBuffer().append("\t" + k + ":"
							+ upperLimitConcentrations.get(k) + " \n"), logging);
		}
		for (KamNode key : allPossibleAgents.keySet()) {
			ArrayList<Region> theregions = agentToRegionMap.get(key);
			ArrayList<Region> productionregions = agentProducedInMap.get(key);
			String conc = homeostaticConcentrations.get(allPossibleAgents.get(
					key).getURI());
			String upperLimit = upperLimitConcentrations.get(allPossibleAgents
					.get(key).getURI());
			String maxConc = "";
			if (conc == null)
				maxConc = this.getMaxHomeostaticValue(allPossibleAgents
						.get(key));
			if (conc == null && maxConc == null)
				conc = this.homeostatic_concentr_default;
			Agent a = new Agent(key, allPossibleAgents.get(key),
					ABMCode.logging, agentsChosenByUser.containsKey(key),
					key.getLabel(), abundanceIdTermMapping.get(key.getLabel()),
					theregions, productionregions, conc, maxConc);
			if (upperLimit != null && upperLimit.length() > 0)
				a.setUpperValueLimit(upperLimit);
			this.agents.add(a);
		}

		Utils.appendToFile(
				new StringBuffer().append("\n" + this.agents.size()
						+ " agents initialized in total \n"), logging);
	}

	/**
	 * 
	 * @param ontClass
	 * @return the previously calculated maximum homeostatic concentration, else
	 *         null
	 */
	private String getMaxHomeostaticValue(OntClass ontClass) {
		if (maxHomeostaticValues.get(ontClass.getURI()) != null)
			return maxHomeostaticValues.get(ontClass.getURI());
		else
			return null;
	}

	/**
	 * 
	 * @param ontClass
	 * @return
	 */
	private void calculateMaxHomeostaticValue(Collection<OntClass> ontClasses) {
		// iterate all agent ont classes and calculate max possible values for
		// all that don't have a homeostatic concentration
		Utils.appendToFile(
				new StringBuffer()
						.append("\ncalculating max possible values for all that don't have a homeostatic concentration: \n"),
				logging);
		Iterator<OntClass> ontIt = ontClasses.iterator();
		OntClass nextClass;
		while (ontIt.hasNext()) {
			nextClass = ontIt.next();
			if (this.homeostaticConcentrations.containsKey(nextClass.getURI())) // then
																				// it
																				// has
																				// a
																				// direct
																				// homeostatic
																				// concentration
				continue;
			// if there is no homeostatic conc available, calculate the max
			// possible concentration
			// find the lowest upper class that has a homeostatic concentration
			// System.out.println(nextClass.getLabel(null));
			OntClass upperClass = Ontology.getUpperClassWithValue(nextClass,
					this.homeostaticConcentrations);
			if (upperClass != null) {
				// System.out.println("     has lowermost upper class with value: "+upperClass.getLabel(null));
				Utils.appendToFile(
						new StringBuffer().append("\t("
								+ nextClass.getLabel(null)
								+ " has lowermost upper class with value: "
								+ upperClass.getLabel(null) + ")\n"), logging);
				int result = sumConcentrationOfSubtree(upperClass,
						Integer.parseInt(this.homeostaticConcentrations
								.get(upperClass.getURI())));
				this.maxHomeostaticValues.put(nextClass.getURI(),
						String.valueOf(result));
				Utils.appendToFile(
						new StringBuffer().append("\t" + nextClass.getURI()
								+ " : " + result + " \n"), logging);
			}
		}
	}

	/**
	 * traverses the subtree under upperClass and calculates conc(upperClass)
	 * -(sum(conc(all subtrees))
	 * 
	 * @param upperClass
	 * @return the calculated value as a String
	 */
	private int sumConcentrationOfSubtree(OntClass upperClass, int val) {
		int value = val;
		ExtendedIterator<OntClass> subClIt = upperClass.listSubClasses();
		OntClass nextCl;
		while (subClIt.hasNext()) {
			nextCl = (OntClass) subClIt.next();
			// System.out.println("      -> nextClass is "+nextCl.getLabel(null));
			// System.out.println("         current value is "+value);
			if (nextCl.canAs(OntClass.class)
					&& this.homeostaticConcentrations.containsKey(nextCl
							.getURI())) {
				// System.out.println("           conc of "+nextCl.getLabel(null)+" is "+
				// this.homeostaticConcentrations.get(nextCl.getURI()));
				value -= Integer.parseInt(this.homeostaticConcentrations
						.get(nextCl.getURI()));
			} else if (nextCl.canAs(OntClass.class)
					&& !this.homeostaticConcentrations.containsKey(nextCl
							.getURI())
					&& nextCl.listSubClasses().toList().size() > 0) {
				// recursive call
				// System.out.println("         value vorher:  "+value);
				value = sumConcentrationOfSubtree(nextCl, value);
				// System.out.println("         value nachher: "+value);
				// immer wenn ein subtree fertig ist, wird der aktuelle Wert
				// zurückgegeben!!! falsch, darf nur endwert zurückgegeben
				// werden
			}
		}
		// System.out.println("return value: "+value);
		return value;
	}

	/**
	 * 
	 * @param agentsChosenByUser
	 * @param allPossibleAgents
	 * @param abundanceIdTermMapping
	 *            maps eg abundance(3) --> LTo cell
	 * @param agentToRegionMap
	 */
	public void initializeAgents(HashMap<KamNode, OntClass> agentsChosenByUser,
			HashMap<KamNode, OntClass> allPossibleAgents,
			HashMap<String, String> abundanceIdTermMapping, OntModel m,
			HashMap<KamNode, ArrayList<Region>> agentToRegionMap,
			HashMap<KamNode, ArrayList<Region>> agentProducedInMap) {
		this.init(allPossibleAgents, agentsChosenByUser,
				abundanceIdTermMapping, m, agentToRegionMap, agentProducedInMap);
		this.setAllPossibleAgents(allPossibleAgents);
	}

	private void setAllPossibleAgents(
			HashMap<KamNode, OntClass> allPossibleAgents) {
		this.allPossibleAgents = allPossibleAgents;
	}

	/**
	 * looks up in onto what qualities the agent has
	 * 
	 * @param onto
	 */
	public void generateBehaviourBasics(Ontology onto) {
		for (Agent agent : this.agents) {
			agent.generateBehaviour(onto, this.qualPropUri, this.quantPropUri);

			// wenn eine der qualprops nach this.inactiveProperty zeigt, dann
			// auf inactive setzen
			if (agent.hasQualPropValues()) {
				for (QualitativeProperty qp : agent.getQualPropValues()) {
					if (qp.getQuality().getURI().equals(this.inactiveProperty)) {
						agent.setIsInActive(true);
						Utils.appendToFile(
								new StringBuffer().append("-> Agent "
										+ agent.getABMCodeLabel()
										+ " activity state is inactive : "
										+ agent.IsInActive() + "\n"), logging);
					}
				}
			}

			// wenn eine der qualprops nach this.lifePropertyURI zeigt, dann hat
			// agent eine life span
			if (agent.hasQualPropValues()) {
				for (QualitativeProperty qp : agent.getQualPropValues()) {
					if (qp.getQuality().getURI().equals(this.lifePropertyURI)) {
						agent.setLifespan(true);
						Utils.appendToFile(
								new StringBuffer().append("-> Agent "
										+ agent.getABMCodeLabel()
										+ " has a life span ?: "
										+ agent.hasLifespan() + "\n"), logging);
					}
					if (qp.getQuality().getURI().equals(this.reproduceURI)) {
						agent.setCanReproduce(true);
						Utils.appendToFile(
								new StringBuffer().append("-> Agent "
										+ agent.getABMCodeLabel()
										+ " can reproduce ?: "
										+ agent.canReproduce() + "\n"), logging);
					}
				}
			}

			// wenn -noHomeostasis dann agent.homeostaticControl auf false
			// setzen
			OntClass falseClass = Ontology.getOntClassFromURIString(agent
					.getAgentInfoOntClass().getOntModel(),
					this.noHomeostasis[1]);
			OntProperty noHomeoProp = Utils.getObjectPropFromURIString(agent
					.getAgentInfoOntClass().getOntModel(),
					this.noHomeostasis[0]);
			if (Utils.hasRestriction(agent.getAgentInfoOntClass(), noHomeoProp)) {
				Utils.appendToFile(
						new StringBuffer().append("-> Agent "
								+ agent.getABMCodeLabel()
								+ " regulated by homeostasis ?: \n"), logging);
				OntClass obj = Utils.getRestrictionValue(
						agent.getAgentInfoOntClass(), noHomeoProp);
				if (obj.getURI().equals(falseClass.getURI())) {
					agent.setHomeostaticControl(false);
					Utils.appendToFile(
							new StringBuffer()
									.append("                                                false \n"),
							logging);
				}
			}
		}

	}

	/**
	 * checks for complex and composite abundances and hasMember and
	 * hasComponent and connects to member agents
	 * 
	 * @param agents
	 * @return
	 */
	public void generateMemberList() {
		// iterate all members in the agents list and check whether it is
		// complex abundance, composite abundance or hasMember/ hasComponent the
		// current agent (this)
		Utils.appendToFile(
				new StringBuffer()
						.append("\nConnecting agents amongst each other via "
								+ "increases/decreases, hasMember/hasComponent and complex and composite abundances.\n"),
				logging);
		Set<KamEdge> edges;
		Agent tmpAgent;
		for (Agent ag : this.agents) {
			edges = ag.getAgentInfoKamNode().getKam()
					.getAdjacentEdges(ag.getAgentInfoKamNode());
			for (KamEdge thisedge : edges) { // edges : adjacent edges of "this"
												// node
				//System.out.println(" "+thisedge.getSourceNode()+" "+thisedge.getRelationshipType()+" "+thisedge.getTargetNode());
				Utils.appendToFile(
						new StringBuffer().append("\n  agent-related statement: "
								+ thisedge.getSourceNode() + " "
								+ thisedge.getRelationshipType() + " "
								+ thisedge.getTargetNode() + "\n"), logging);
				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
						|| thisedge.getRelationshipType() == RelationshipType.INCREASES) {
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), agents);

						if (!ag.getIncreasesAbundance().contains(tmpAgent)
								&& !(tmpAgent == null)) {
							ag.setIncreasesAbundance(addAgentToList(tmpAgent,
									ag.getIncreasesAbundance()));
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel() + " increases "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}

						if (!tmpAgent.getIncreasedByAbundance().contains(ag)
								&& !(tmpAgent == null)) {
							tmpAgent.setIncreasedByAbundance(addAgentToList(ag,
									tmpAgent.getIncreasedByAbundance()));
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ tmpAgent.getBELTermLabel()
											+ " increased by "
											+ ag.getBELTermLabel() + "\n"),
									logging);
						}
					}
					if (thisedge.getTargetNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getSourceNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getSourceNode(), agents);

						if (!ag.getIncreasedByAbundance().contains(tmpAgent)
								&& !(tmpAgent == null)) {
							ag.setIncreasedByAbundance(addAgentToList(tmpAgent,
									ag.getIncreasedByAbundance()));
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel() + " increased by "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}

						if (!tmpAgent.getIncreasesAbundance().contains(ag)
								&& !(tmpAgent == null)) {
							tmpAgent.setIncreasesAbundance(addAgentToList(ag,
									tmpAgent.getIncreasesAbundance()));
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ tmpAgent.getBELTermLabel()
											+ " increases "
											+ ag.getBELTermLabel() + "\n"),
									logging);
						}
					}
				}
				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES
						|| thisedge.getRelationshipType() == RelationshipType.DECREASES) {
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), agents);

						if (!ag.getDecreasesAbundance().contains(tmpAgent)
								&& !(tmpAgent == null)) {
							ag.setDecreasesAbundance(addAgentToList(tmpAgent,
									ag.getDecreasesAbundance()));
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel() + " decreases "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}

						if (!tmpAgent.getDecreasedByAbundance().contains(ag)
								&& !(tmpAgent == null)) {
							tmpAgent.setDecreasedByAbundance(addAgentToList(ag,
									tmpAgent.getDecreasedByAbundance()));
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ tmpAgent.getBELTermLabel()
											+ " decreased by "
											+ ag.getBELTermLabel() + "\n"),
									logging);
						}
					}
					if (thisedge.getTargetNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getSourceNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getSourceNode(), agents);

						if (!ag.getDecreasedByAbundance().contains(tmpAgent)
								&& !(tmpAgent == null)) {
							ag.setDecreasedByAbundance(addAgentToList(tmpAgent,
									ag.getDecreasedByAbundance()));
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel() + " decreased by "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}

						if (!tmpAgent.getDecreasesAbundance().contains(ag)
								&& !(tmpAgent == null)) {
							tmpAgent.setDecreasesAbundance(addAgentToList(ag,
									tmpAgent.getDecreasesAbundance()));
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ tmpAgent.getBELTermLabel()
											+ " decreases "
											+ ag.getBELTermLabel() + "\n"),
									logging);
						}
					}
				}
				if (thisedge.getRelationshipType() == RelationshipType.HAS_COMPONENT
						|| thisedge.getRelationshipType() == RelationshipType.HAS_MEMBER) {
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), agents);
						//complexAbundance(1) hasMember abundance(2)  with complexAbundance(1) being the ag under investigation 
						//if (!ag.getHasMember().contains(tmpAgent)  //if it is already included there must a reason why this is contained twice in the code
						//		&& !(tmpAgent == null)) {
						if (!(tmpAgent == null)) {//hier
							//check if it's a complex of the same entities such as
							//complexAbundance(proteinAbundance(3),proteinAbundance(3)) hasComponent                        proteinAbundance(3)
							//thisedge.getSourceNode()                                  thisedge.getRelationshipType()      thisedge.getTargetNode());
							//the BEL Framework saves this only once!!!!!!!!!!!!!!!!!!!!!
							String tmp =thisedge.getSourceNode().getLabel();
							int numberOcc = 0;
							while (tmp.indexOf(thisedge.getTargetNode().toString()) != -1){
								tmp = tmp.substring(tmp.indexOf(thisedge.getTargetNode().toString())+1);
								numberOcc ++;
								//System.out.println(tmp);
							}
							//System.out.println(numberOcc);
							int cnt = 0;
							while (cnt < numberOcc){
								ag.setHasMember(addAgentToList(tmpAgent,   
										ag.getHasMember()));
								// turn off homeostatic control for complexes
								ag.setHomeostaticControl(false);
								Utils.appendToFile(new StringBuffer().append("\t"
										+ ag.getBELTermLabel() + " has member "
										+ tmpAgent.getBELTermLabel()
										+ ". Homeostatic control turned off for "
										+ ag.getABMCodeLabel() + ".\n"), logging);
								//tmpAgent.setMemberOf(addAgentToList(ag,
								//		tmpAgent.getMemberOf()));
								Utils.appendToFile(
										new StringBuffer().append("\t"
												+ tmpAgent.getBELTermLabel()
												+ " is member of "
												+ ag.getBELTermLabel() + ".\n"),
										logging);
								cnt++;
							}
						}

					}
					/*if (thisedge.getTargetNode() == ag.getAgentInfoKamNode()  //dont't include, leads to info being saved twice
							& Utils.isAbundance(thisedge.getSourceNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getSourceNode(), agents);
						// complexAbundance(abundance(5),proteinAbundance(18))
						// hasComponent abundance(5) 
						//if (!ag.getMemberOf().contains(tmpAgent)  //if it is already included there must a reason why this is contained twice in the code
						//		&& !(tmpAgent == null)) {
						if ( !(tmpAgent == null)) {
							ag.setMemberOf(addAgentToList(tmpAgent,
									ag.getMemberOf()));
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel() + " member of "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
							tmpAgent.setHasMember(addAgentToList(ag,
									tmpAgent.getHasMember()));
							tmpAgent.setHomeostaticControl(false);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ tmpAgent.getBELTermLabel()
									+ " has member " + ag.getBELTermLabel()
									+ ". Homeostatic control turned off for "
									+ tmpAgent.getABMCodeLabel() + ". \n"),
									logging);
						}
					}*/
				}
				// composites (need different behaviour than complexes)
				if (thisedge.getRelationshipType() == RelationshipType.INCLUDES) {
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), agents);

						//if (!ag.getIncludes().contains(tmpAgent)  //if it is already included there must a reason why this is contained twice in the code
						//		&& !(tmpAgent == null)) {
						if (!(tmpAgent == null)) {
							ag.setIncludes(addAgentToList(tmpAgent,
									ag.getIncludes()));
							// turn off homeostatic control for composites
							ag.setHomeostaticControl(false);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ ag.getBELTermLabel()
									+ " is a composite and includes "
									+ tmpAgent.getBELTermLabel()
									+ ". Homeostatic control turned off for "
									+ ag.getABMCodeLabel() + ".\n"), logging);
							tmpAgent.setIncludedIn(addAgentToList(ag,
									tmpAgent.getIncludedIn()));
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ tmpAgent.getBELTermLabel()
											+ " is included in the composite "
											+ ag.getBELTermLabel() + "\n"),
									logging);
						}

					}
				}
			}

		}
	}

	/**
	 * adds agent tmpAgent to list<Agent> tmpList and returns the list
	 * 
	 * @param tmpAgent
	 * @param tmpList
	 * @return
	 */
	private List<Agent> addAgentToList(Agent tmpAgent, List<Agent> tmpList) {
		tmpList.add(tmpAgent);
		return tmpList;
	}

	/**
	 * adds agent p to list<BioProcess> tmpList and returns the list
	 * 
	 * @param p
	 * @param tmpList
	 * @return
	 */
	private List<BioProcess> addToBioProcessList(BioProcess p,
			List<BioProcess> tmpList) {
		tmpList.add(p);
		return tmpList;
	}

	/**
	 * goes through all agents and looks up in what processes they are involved
	 * also checks if they are biomarkers for a process fills the lists
	 * Agent.increasesBioProcess, Agent.decreasesBioProcess,
	 * Agent.increasedByBioProcess, Agent.decreasedByBioProcess,
	 * Agent.isBiomarkerForProcess
	 */
	public void generateProcessParticipations() {
		// iterate all members in the agents list and check whether it is
		// connected to a biologicalProcess
		Utils.appendToFile(
				new StringBuffer()
						.append("\nScanning for agent participation in biological processes.\n"),
				logging);
		Set<KamEdge> edges;
		BioProcess tmpProcess;
		for (Agent ag : this.agents) {
			edges = ag.getAgentInfoKamNode().getKam()
					.getAdjacentEdges(ag.getAgentInfoKamNode());
			// System.out.println("\ncurrent agent: "+ag.getBELTermLabel()+" "+ag.getBELIdLabel());
			// ############### check inside KAM #########################
			for (KamEdge thisedge : edges) { // edges : adjacent edges of "this"
												// node
				// System.out.println(thisedge.getSourceNode()+" "+thisedge.getRelationshipType()+" "+thisedge.getTargetNode());

				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
						|| thisedge.getRelationshipType() == RelationshipType.INCREASES) {
					// agent -> bp()
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isBioProcess(thisedge.getTargetNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getTargetNode(), bioProcesses);

						ag.setIncreasesBioProcess(addToBioProcessList(
								tmpProcess, ag.getIncreasesBioProcess()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ ag.getBELTermLabel() + " increases "
										+ tmpProcess.getBELTermLabel() + "\n"),
								logging);

						tmpProcess.setIncreasedByAbundance(addAgentToList(ag,
								tmpProcess.getIncreasedByAbundance()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ tmpProcess.getBELTermLabel()
										+ " increased by "
										+ ag.getBELTermLabel() + "\n"), logging);
					}
					// bp() -> a(agent)
					if (thisedge.getTargetNode() == ag.getAgentInfoKamNode()
							& Utils.isBioProcess(thisedge.getSourceNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getSourceNode(), bioProcesses);

						ag.setIncreasedByBioProcess(addToBioProcessList(
								tmpProcess, ag.getIncreasedByBioProcess()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ ag.getBELTermLabel()
										+ " increased by "
										+ tmpProcess.getBELTermLabel() + "\n"),
								logging);

						tmpProcess.setIncreasesAbundance(addAgentToList(ag,
								tmpProcess.getIncreasesAbundance()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ tmpProcess.getBELTermLabel()
										+ " increases " + ag.getBELTermLabel()
										+ "\n"), logging);
					}
				}
				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES
						|| thisedge.getRelationshipType() == RelationshipType.DECREASES) {
					// agent -| bp()
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							& Utils.isBioProcess(thisedge.getTargetNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getTargetNode(), bioProcesses);

						ag.setDecreasesBioProcess(addToBioProcessList(
								tmpProcess, ag.getDecreasesBioProcess()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ ag.getBELTermLabel() + " decreases "
										+ tmpProcess.getBELTermLabel() + "\n"),
								logging);

						tmpProcess.setDecreasedByAbundance(addAgentToList(ag,
								tmpProcess.getDecreasedByAbundance()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ tmpProcess.getBELTermLabel()
										+ " decreased by "
										+ ag.getBELTermLabel() + "\n"), logging);
					}
					// bp() -| a(agent)
					if (thisedge.getTargetNode() == ag.getAgentInfoKamNode()
							& Utils.isBioProcess(thisedge.getSourceNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getSourceNode(), bioProcesses);

						ag.setDecreasedByBioProcess(addToBioProcessList(
								tmpProcess, ag.getDecreasedByBioProcess()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ ag.getBELTermLabel()
										+ " decreased by "
										+ tmpProcess.getBELTermLabel() + "\n"),
								logging);

						tmpProcess.setDecreasesAbundance(addAgentToList(ag,
								tmpProcess.getDecreasesAbundance()));
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ tmpProcess.getBELTermLabel()
										+ " decreases " + ag.getBELTermLabel()
										+ "\n"), logging);
					}
				}
				// agent bioMarkerFor process
				if (thisedge.getRelationshipType() == RelationshipType.BIOMARKER_FOR) {
					if (thisedge.getSourceNode() == ag.getAgentInfoKamNode()
							&& Utils.isBioProcess(thisedge.getTargetNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getTargetNode(), bioProcesses);

						ag.addIsBiomarkerForProcess(tmpProcess);
						Utils.appendToFile(
								new StringBuffer().append("\t"
										+ ag.getBELTermLabel()
										+ " is biomarker for process "
										+ tmpProcess.getBELTermLabel() + "\n"),
								logging);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param allBioProcessNodes
	 * @param processIdTermMapping
	 * @param onto
	 * @param belAnnoProp
	 *            Annotation property inside onto that contains the BEL Term
	 */
	public void initializeProcesses(List<KamNode> allBioProcessNodes,
			HashMap<String, String> processIdTermMapping, Ontology onto,
			String belAnnoProp) {
		Utils.appendToFile(
				new StringBuffer().append("\nProcesses initialized: ("
						+ allBioProcessNodes.size() + " in total) \n"), logging);
		// BioProcess(KamNode key, OntClass ontClass, String logging, String
		// BELIdLabel, String BELTermLabel)
		List<OntClass> ontCl;
		for (KamNode key : allBioProcessNodes) {
			ontCl = Utils.getOntClasses(key, onto, belAnnoProp,
					processIdTermMapping, logging);
			this.bioProcesses.add(new BioProcess(key, ontCl, ABMCode.logging,
					key.getLabel(), processIdTermMapping.get(key.getLabel())));
		}
	}

	
	/**
	 * example: molecularActivity(abundance(5)) increases biologicalProcess(22)
	 * 
	 * @param allActivityNodes
	 */
	public void generateActivities(List<KamNode> allActivityNodes) {
		// iterate all members in the agents list and check whether they have an
		// activity
		// that associates them to another agent or a process
		Utils.appendToFile(
				new StringBuffer().append("\nScanning for agent activities.\n"),
				logging);
		String act; // activity string
		//first, generate all activities
		for (KamNode an : allActivityNodes) {
			act = an.getLabel().substring(0, an.getLabel().indexOf('('));
			Activity act_ag = new Activity(Activity.getActivityType(act), an);
			addActivity(act_ag);
			Agent a = Utils.getAgentByBELTermId(
							an.getLabel().substring(
									an.getLabel().indexOf('(') + 1,
											an.getLabel().length() - 1),
					this.agents);
			a.addActivity(act_ag);

			Utils.appendToFile(
					new StringBuffer().append("\n  activity: "+ act_ag.getActivityNode().getLabel()+" - type: "+act_ag.getType()+"\n"),
					logging);
		}
	}
	
	
	
	/**
	 * example: molecularActivity(abundance(5)) increases biologicalProcess(22)
	 * 
	 * @param allActivityNodes
	 */
	public void generateActivityParticipations() {
		// iterate all members in the agents list and check whether they have an
		// activity
		// that associates them to another agent or a process
		Utils.appendToFile(
				new StringBuffer().append("\nScanning for agent activity participations.\n"),
				logging);
		Set<KamEdge> edges;
		BioProcess tmpProcess;
		Agent tmpAgent;
		//go through the activities and search how they are connected to the agents, other activities and processes
		for (Activity a : getActivities()) {
			Agent agentOfThisActivity = Utils.getAgentByBELTermId(
					a.getActivityNode().getLabel().substring(
							a.getActivityNode().getLabel().indexOf('(') + 1,
									a.getActivityNode().getLabel().length() - 1),
									this.agents);
			edges = a.getActivityNode().getKam().getAdjacentEdges(a.getActivityNode());
			// System.out.println("\ncurrent agent: "+ag.getBELTermLabel()+" "+ag.getBELIdLabel());
			for (KamEdge thisedge : edges) { // edges : adjacent edges of "this"
												// node
				// System.out.println(thisedge.getSourceNode()+" "+thisedge.getRelationshipType()+" "+thisedge.getTargetNode());

				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
						|| thisedge.getRelationshipType() == RelationshipType.INCREASES) {

					// case 2: act(a(ag)) [-> or -|] act(a(ag2)) and case 5:
					// act(a(ag2)) [-> or -|] act(a(ag))
					// save these cases only from left to right to prevent
					// double implementation
					// case 2: act(a(ag2)) -> act_ag2(a(ag3))
					// #########################################
					if (Utils.isActivityNode(thisedge.getSourceNode())
							&& Utils.isActivityNode(thisedge.getTargetNode())) {
						if (thisedge.getSourceNode() == a.getActivityNode()) {
							a.setRelationship(Relationship.INCREASES);
							Agent agentIncreased = Utils.getAgentByNode(thisedge.getTargetNode(), agents);
							a.setObject(agentIncreased);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity.getABMCodeLabel()
											+ " increases "
											+ Activity.getActivityType(thisedge.getTargetNode().getLabel().substring(0, thisedge.getTargetNode().getLabel().indexOf('(')))
											+ " of " + Utils.getAgentByNode(thisedge.getTargetNode(), agents).getABMCodeLabel()
											+ "\n"), logging);
						}
					}

					// act(agent) -> bp()
					// #########################################
					if (thisedge.getSourceNode() == a.getActivityNode()
							& Utils.isBioProcess(thisedge.getTargetNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getTargetNode(), bioProcesses);

						if (tmpProcess != null) {
							a.setRelationship(Relationship.INCREASES);
							a.setObject(tmpProcess);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity.getABMCodeLabel()
											+ " increases "
											+ tmpProcess.getBELTermLabel()
											+ "\n"), logging);
						}
					}  // act(agent1) -> a(agent2)
							// #####################################
					if (thisedge.getSourceNode() == a.getActivityNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), this.agents);

						if (tmpAgent != null) {
							a.setRelationship(Relationship.INCREASES);
							a.setObject(tmpAgent);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ a.getType() + " of "
									+ agentOfThisActivity.getABMCodeLabel() + " increases "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}
					} 
					// bp() -> act(a(agent))
					// #########################################
					if (thisedge.getTargetNode() == a.getActivityNode()
							& Utils.isBioProcess(thisedge.getSourceNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getSourceNode(), bioProcesses);

						if (tmpProcess != null) {
							a.setRelationship(Relationship.INCREASEDBY);
							a.setObject(tmpProcess);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity
											+ " increased by "
											+ tmpProcess.getBELTermLabel()
											+ "\n"), logging);
						}
					}
					 // a(agent1) -> act(a(agent2))
					// #####################################
					if (thisedge.getTargetNode() == a.getActivityNode()
							& Utils.isAbundance(thisedge.getSourceNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getSourceNode(), this.agents);

						if (tmpAgent != null) {
							a.setRelationship(Relationship.INCREASEDBY);
							a.setObject(tmpAgent);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ a.getType() + " of "
									+ agentOfThisActivity + " increased by "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}
					}
				}
				if (thisedge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES
						|| thisedge.getRelationshipType() == RelationshipType.DECREASES) {

					// case 2: act(a(ag)) [-> or -|] act(a(ag2)) and case 5:
					// act(a(ag2)) [-> or -|] act(a(ag))
					// save these cases only from left to right to prevent
					// double implementation
					// case 2: act(a(ag2)) -| act_ag2(a(ag3))
					// #########################################
					if (Utils.isActivityNode(thisedge.getSourceNode())
							&& Utils.isActivityNode(thisedge.getTargetNode())) {
						if (thisedge.getSourceNode() == a.getActivityNode()) {
							a.setRelationship(Relationship.DECREASES);
							Agent agentDecreased = Utils.getAgentByNode(thisedge.getTargetNode(), agents);
							a.setObject(agentDecreased);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity.getABMCodeLabel()
											+ " decreases "
											+ Activity.getActivityType(thisedge.getTargetNode().getLabel().substring(0, thisedge.getTargetNode().getLabel().indexOf('(')))
											+ " of " + Utils.getAgentByNode(thisedge.getTargetNode(), agents).getABMCodeLabel()
											+ "\n"), logging);
						}
					}
					// act(agent) -| bp()
					// #########################################
					if (thisedge.getSourceNode() == a.getActivityNode()
							& Utils.isBioProcess(thisedge.getTargetNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getTargetNode(), bioProcesses);

						if (tmpProcess != null) {
							a.setRelationship(Relationship.DECREASES);
							a.setObject(tmpProcess);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity.getABMCodeLabel()
											+ " decreases "
											+ tmpProcess.getBELTermLabel()
											+ "\n"), logging);
						}
					}  // act(agent1) -| a(agent2)
					// ####################################
					if (thisedge.getSourceNode() == a.getActivityNode()
							& Utils.isAbundance(thisedge.getTargetNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getTargetNode(), this.agents);

						if (tmpAgent != null) {
							a.setRelationship(Relationship.DECREASES);
							a.setObject(tmpAgent);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ a.getType() + " of "
									+ agentOfThisActivity + " decreases "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}
					}
					// bp() -| act(a(agent))
					// ########################################
					if (thisedge.getTargetNode() == a.getActivityNode()
							& Utils.isBioProcess(thisedge.getSourceNode())) {
						tmpProcess = Utils.getBioProcessByNode(
								thisedge.getSourceNode(), bioProcesses);

						if (tmpProcess != null) {
							a.setRelationship(Relationship.DECREASEDBY);
							a.setObject(tmpProcess);
							Utils.appendToFile(
									new StringBuffer().append("\t"
											+ a.getType()
											+ " of " + agentOfThisActivity
											+ " decreased by "
											+ tmpProcess.getBELTermLabel()
											+ "\n"), logging);
						}
					}  // a(agent1) -| act(a(agent2))
					// ####################################
					if (thisedge.getTargetNode() == a.getActivityNode()
							& Utils.isAbundance(thisedge.getSourceNode())) {
						tmpAgent = Utils.getAgentByNode(
								thisedge.getSourceNode(), this.agents);

						if (tmpAgent != null) {
							a.setRelationship(Relationship.DECREASEDBY);
							a.setObject(tmpAgent);
							Utils.appendToFile(new StringBuffer().append("\t"
									+ a.getType() + " of "
									+ agentOfThisActivity + " decreased by "
									+ tmpAgent.getBELTermLabel() + "\n"),
									logging);
						}
					}
				}
			}

		}

	}

	private ArrayList<Activity> getActivities() {
		return this.activities;
	}

	/**
	 * adds a new activity to the list of all activities found in the BEL code
	 * @param act_ag
	 */
	private void addActivity(Activity act_ag) {
		this.activities.add(act_ag);
	}

	/**
	 * traverses all the agents and their connected items (processes, ontology
	 * features etc.) and generates ABM code
	 * 
	 * @param kamStore
	 */
	public void generateCode(KamStore kamStore) {
		this.initAgents(this.agents);
		this.setGlobalVars();
		this.initPlotting(this.agents);
		
		int cnt = 0;
		for (Agent ag : this.agents)
			if (ag.getChosenByUser()) {       // otherwise the agent won't be plotted and won't need a color
				ag.setPlottingColor(colorIntegers[cnt]);
				ag.setColorString(colors[cnt]); 
				cnt++;
			}
		
		this.reproductionCondition = generateReproductionCondition();

		// TODO Increase decrease zwischen bioprocesses durch Text mining,
		// insert in ontology, then the following method will read the links
		// automatically
		this.doWriteBioProcessProcedures(this.bioProcesses, kamStore); // needs    //writes the code to the bpGoCode
																		// to be
																		// before
																		// doGo() and doSetUp()!?

		this.doSetUp(this.agents);  //directly writes the code to the output file
		this.doGo(this.agents, kamStore); // writes the ask breeds code
		
		this.writeReproduce(this.agents);
		addToCode(this.generateCodeMove());// move + deduct energy
		for (Agent a : this.agents)
			if (a.isComposite())
				addToCode(this.generateCodeMoveCompositeAgent(a)); // check if
																	// composite
																	// agents
																	// decompose
		if (translocationExists())
			addToCode(this.generateCodeTLoc());
		addToCode(this.generateCodeDeath());
		for (Agent a : this.agents){
			if (a.isHomeostaticControl())
				addToCode(this.generateCodeDeathHomeostatic(a));
			else if (a.isComposite() || a.isComplex())
				addToCode(this.generateCodeDeath(a));
		}
		// add bp code to the document
		for (BioProcess bp : this.bioProcesses) {
			addToCode(bp.getCode() + "end\n\n");
		}
		this.addFunctions("MathUtils.nlogo");
		this.setupGraphicsWindow();
		this.setupSETUPButton();
		this.setupGOButton();
		this.setupPLOT(this.agents);

		int x1right = 1130;
		int y1right = 25;
		int x2right = 1300;
		int y2right = 60;
		this.setupReactionDistanceSlider(this.reactionDistance, x1right,
				y1right, x2right, y2right);
		y1right += 45;
		y2right += 45;
		this.setupHomeostasisMimickingSwitch(x1right, y1right, x2right, y2right);
		y1right += 45;
		y2right += 45;

		int x1 = 20;
		int y1 = 85;
		int x2 = 200;
		int y2 = 120;

		int x1centre = 712;
		int y1centre = 285;
		int x2centre = 950;
		int y2centre = 305;
		// agent-move-speed sliders
		for (Agent a : this.agents){
			if (a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents) && !a.isConnectedViaOntology()){
				continue;
			}
			this.setupMoveSpeedSlider(a, x1centre, y1centre, x2centre, y2centre);
			y1centre += 45;
			y2centre += 45;
		}
		// composite and complex element numbers choosers
		int x1_under_graphics = 230;
		int y1_under_graphics = 510;
		int x2_under_graphics = 400;
		int y2_under_graphics = 530;
		for (Agent ag : this.agents){
			if (ag.isComplex() && ag.getHasMember().size() == 2){
				this.setupMemberNumberChooser(ag, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
				this.setupMemberNumberMaxInput(ag, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
			}
			if (ag.isComposite() && ag.getIncludes().size() == 2){
				this.setupMemberNumberChooser(ag, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
				this.setupMemberNumberMaxInput(ag, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
			}
		}
		
		for (Agent a : this.agents){
			if (a.isComposite() && a.getIncludes().size() == 2){
				this.setupComositeLabelsSwitch(a, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
			}
		}
		
		for (Agent a : this.agents){
			if (a.isComplex() && a.getHasMember().size() == 2){
				this.setupComplexLabelsSwitch(a, x1_under_graphics, y1_under_graphics, x2_under_graphics, y2_under_graphics);
				y1_under_graphics += 45;
				y2_under_graphics += 45;
			}
		}
		
		// dupli-rate sliders for chosen agents that can reproduce
		for (Agent a : this.agents) {
			if ((a.getChosenByUser() && !a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents))
					|| (!a.getChosenByUser() && a.hasQualPropValues())) {
				for (QualitativeProperty qp : a.getQualPropValues()) {
					if (qp.getQuality().getURI().equals(this.reproduceURI)) {
						this.setupReproductionRateSlider(a, x1, y1, x2, y2);
						y1 += 45;
						y2 += 45;
					}
				}
			}
		}
		for (Agent a : this.agents) {
			if (a.hasLifespan() && !a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
				this.setupLifespanInputBox(a, x1right, y1right, x2right, y2right);
				y1right += 65;
				y2right += 35;
			}
		}
		
		int x1_under_graphics_ri = 430;
		int y1_under_graphics_ri = 510;
		int x2_under_graphics_ri = 600;
		int y2_under_graphics_ri = 530;
		for (Agent a : this.agents){
			if (a.isComplex() && !a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)){
				this.setupBindingStrengthSlider(a, x1_under_graphics_ri, y1_under_graphics_ri, x2_under_graphics_ri, y2_under_graphics_ri);
				y1_under_graphics_ri += 45;
				y2_under_graphics_ri += 45;
			}
		}
		
		// initial count sliders
		for (Agent a : this.agents) {
			if (!a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
				this.setupInitialAgentNumberSlider(a, x1, y1, x2, y2);
				y1 += 45;
				y2 += 45;
			}
		}
		// upper limit sliders
		for (Agent a : this.agents) {
			if (a.hasUpperValueLimit() && !a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
				this.setupUpperValueSlider(a, x1, y1, x2, y2);
				y1 += 45;
				y2 += 45;
			}
		}
		// this.setupProcessParticipationRateSlider("process_particip_rate", x1,
		// y1, x2, y2);
		this.addStuffForFileFormatCompliance();
	}

	/**
	 * in case of 1:n or n:1 for composite or complex agents, lets the user choose the maximum n (ie number of receptors) 
	 * @param a
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	private void setupMemberNumberMaxInput(Agent a, int x1, int y1, int x2, int y2) {
		String initvalue = "1";
		String code = "\n" + "INPUTBOX\n" +
		+ x1+ "\n"
		+ y1+ "\n"
		+ x2+ "\n"
		+ y2+ "\n";
		if (a.isComplex())
		   code += a.getHasMember().get(0).getABMCodeLabel()+"."+a.getHasMember().get(1).getABMCodeLabel()+"_maxn\n";
		if (a.isComposite())
		   code += a.getIncludes().get(0).getABMCodeLabel()+"."+a.getIncludes().get(1).getABMCodeLabel()+"_maxn\n";
		code += initvalue+ "\n"
		+ 1+ "\n"
		+ 0+ "\n"
		+ "Number\n";
		addToCode(code);
	}

	private void setupMemberNumberChooser(Agent ag, int x1,
			int y1, int x2, int y2) {
		String code = "";
		code += "CHOOSER\n";
		code += x1+"\n";
		code += y1+"\n";
		code += x2+"\n";
		code += y2+"\n";
		
		if (ag.isComplex()){
			code += ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_members\n";
			code += ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_members\n";
		}
		if (ag.isComposite()){
			code += ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_members\n";
			code += ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_members\n";
		}
		code += "\"1:n\" \"1:1\" \"n:1\"\n";
		code += "1\n";
		
		if ( !ag.isComplex() && !ag.isComposite() )
			code = "";
		if (ag.isComplex() && ag.getHasMember().size() != 2)
			code = "";
		if (ag.isComposite() && ag.getIncludes().size() != 2)
			code = "";
		addToCode(code);
	}

	/**
	 * in cases when an agent's number mustn't get higher than a given upper
	 * limit
	 * 
	 * @param a
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	private void setupUpperValueSlider(Agent a, int x1, int y1, int x2, int y2) {
		String code = "";
		if (a.hasUpperValueLimit()) {
			String upperLimit = a.getUpperValueLimit();
			code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n" + y2
					+ "\n" + "upper-lim-" + a.getABMCodeLabel() + "\n"
					+ "upper-lim-" + a.getABMCodeLabel() + "\n" + "0\n"
					+ Integer.parseInt(upperLimit) * 2 + "\n" + upperLimit
					+ "\n" + "1\n" + "1\n" + "NIL\n" + "HORIZONTAL\n\n";
		}
		addToCode(code);
	}

	private void setupHomeostasisMimickingSwitch(int x1right, int y1right,
			int x2right, int y2right) {
		String code = "\n" + "SWITCH\n" + x1right + "\n" + y1right + "\n"
				+ x2right + "\n" + y2right + "\n" + "homeostasis_mimicking?\n"
				+ "homeostasis_mimicking?\n" + "1\n" + "1\n" + "-1000\n\n";
		addToCode(code);
	}
	
	private void setupComositeLabelsSwitch(Agent a, int x1, int y1,
			int x2, int y2) {
		String code = "\n" + "SWITCH\n" + x1 + "\n" + y1 + "\n"
				+ x2 + "\n" + y2 + "\n" + a.getABMCodeLabel()+"_labels?\n"
				+ a.getABMCodeLabel()+"_labels?\n" + "1\n" + "1\n" + "-1000\n\n";
		addToCode(code);
	}
	
	private void setupComplexLabelsSwitch(Agent a, int x1, int y1,
			int x2, int y2) {
		String code = "\n" + "SWITCH\n" + x1 + "\n" + y1 + "\n"
				+ x2 + "\n" + y2 + "\n" + a.getABMCodeLabel()+"_labels?\n"
				+ a.getABMCodeLabel()+"_labels?\n" + "1\n" + "1\n" + "-1000\n\n";
		addToCode(code);
	}

	
	private void setupMoveSpeedSlider(Agent a, int x1right, int y1right,
			int x2right, int y2right) {
		String code = "\n" + "SLIDER\n" + x1right + "\n" + y1right + "\n"
				+ x2right + "\n" + y2right + "\n" + a.getABMCodeLabel()+"-move-speed\n"
				+ a.getABMCodeLabel()+"-move-speed\n" + "0\n" + "2\n" + "1\n" + "0.1\n" + "1\n"+ "NIL\n" + "HORIZONTAL\n\n";
		addToCode(code);
	}

	/**
	 * checks if at least 1 translocation exists in the code
	 * 
	 * @return
	 */
	private boolean translocationExists() {
		Boolean exists = false;
		for (Agent a : this.agents) {
			if (a.getTranslocations().size() > 0) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	/**
	 * turtle dies when energy <= 0
	 * 
	 * @return code snippet for procedure
	 */
	private String generateCodeDeath(Agent a) {
		String code = "\nto death-"+a.getABMCodeLabel()+"                      ;; turtle procedure\n"
				+ "\t;; when energy dips below zero, die\n";
		if (!a.isComposite())
			code += "\tif energy <= 0 [ die ]\n";
		if (a.isComplex() &&  a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
			code += "\t    if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [die]\n";
		if (a.isComplex() &&  !a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
			code += "\t    if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies [die]\n";
		
		//TODO review this horrible code
		if (a.isComposite() && a.getIncludes().size() ==2){
			code += "\t  ;; let members with energy == 0 die, the others can continue to live if there is still at least one of both kinds\n";
			code += "\t  set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies remove 0 "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
			//code += "\t  let cnt 0\n";
			//foreach geht nicht, while schleife benutzen und mit item und item-remove drauf zugreifen über cnt; selbe Stelle der activities liste löschen
			//nochmal drübergucken, müsste aber eigentlich funktionieren
			//code += "\t  while [ cnt < length "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies ] [\n";
			//code += "\t    ifelse item cnt "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies  = \"0\" [ \n";
			//code += "\t       set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies remove-item cnt "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies \n";
			//code += "\t       set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_activities remove-item cnt "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_activities \n";
			//code += "\t    ]\n";
			//code += "\t    [set cnt cnt + 1]\n";
			//code += "\t  ]\n";
			if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
				code += "\t  set "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies remove 0 "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n";
			else
				code += "\t  set "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies remove 0 "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n";
			code += "\t  ;; but before that, release any possibly still remaining members\n"; 
			if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
				code += "\t  if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies and not empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t  if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies and not empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
				code += "\t    foreach "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t    foreach "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			code += "\t      "+hatchagent(a.getIncludes().get(1), 1, a.getIncludes().get(1).getColorString(), "random 100", 
					a.getIncludes().get(1).getSize(), "?", "0", "0", "0", "null")+"\n";
			code += "\t      ]\n";
			code += "\t    ]\n";
			if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))			
				code += "\t  if not empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies and empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t  if not empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies and empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			code += "\t    foreach "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			code += "\t      "+hatchagent(a.getIncludes().get(0), 1, a.getIncludes().get(0).getColorString(), "random 100", 
					a.getIncludes().get(0).getSize(), "?", "0", "0", "0", "null")+"\n";
			code += "\t      ]\n";
			code += "\t    ]\n";
			if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
				code += "\t    if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [die]\n";
			else
				code += "\t    if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies [die]\n";
		}
		if (a.isComplex() && a.getHasMember().size() ==2){
			code += "\t;; if binding strength too low, release one of the bound molecules/agents\n";
			code += "\tlet en_list_1 "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
				code += "\tlet en_list_2 "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n";
			else
				code += "\tlet en_list_2 "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n";
			code += "\tif random 100 > bind-str-"+a.getABMCodeLabel()+"\n";
			code += "\t  [\n";
			code += "\t    if "+a.getABMCodeLabel()+"_members = \"n:1\"\n";
			code += "\t    [\n";
			
			//check if the agent to be hatched is a complex himself (then it needs energy lists)
			if (a.getHasMember().get(0).isComplex() && a.getHasMember().get(0).getHasMember().size() == 2){
				String energylist2="";
				if (a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()))
					energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
				else
					energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
				code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
						a.getHasMember().get(0).getSize(), "last en_list_1", "0", 
						"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel()+")", 
						"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()+")", 
						energylist2)+"\n";
			}
			else //ie, the agent to be hatched is not a complex
				code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
						a.getHasMember().get(0).getSize(), "last en_list_1", "0", "0", "0", "null")+"\n";
			code += "\t      set "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies but-last "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
			//check if it is an allosteric enzyme, then a needs to loose activity points
			if (a.isAllostericEnzyme(allostericEnzymeURI)){
				code += "\t      ;;"+a.getABMCodeLabel()+" is an allosteric enzyme\n"; 
				code += "\t      set activity  activity - ( 50 / ( "+a.getABMCodeLabel()+"_maxn - 1 ) )\n"; 
				code += "\t      if activity < 0 [ set activity 0 ]\n";
			}
			code += "\t    ]\n";
			
			/////////////////////////////////////////////////////////////////////
			code += "\t    if "+a.getABMCodeLabel()+"_members = \"1:n\"\n";
			code += "\t    [\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel())){
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(1).isComplex() && a.getHasMember().get(1).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
							a.getHasMember().get(1).getSize(), "last en_list_2", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
						a.getHasMember().get(1).getSize(), "last en_list_2", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies but-last "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n";
				}
			else {
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(1).isComplex() && a.getHasMember().get(1).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
							a.getHasMember().get(1).getSize(), "last en_list_2", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
					a.getHasMember().get(1).getSize(), "last en_list_2", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies but-last "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n";
				if (a.isAllostericEnzyme(allostericEnzymeURI)){
					code += "\t      ;;"+a.getABMCodeLabel()+" is an allosteric enzyme\n"; 
					code += "\t      set activity  activity - ( 50 / ( "+a.getABMCodeLabel()+"_maxn - 1 ) )\n"; 
					code += "\t      if activity < 0 [ set activity 0 ]\n";
				}
			}
			code += "\t    ]\n";
			///////////////////////////////////////////////////////////////////////////
			code += "\t    if "+a.getABMCodeLabel()+"_members = \"1:1\"\n";
			code += "\t    [\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel())){
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(0).isComplex() && a.getHasMember().get(0).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
							a.getHasMember().get(0).getSize(), "last en_list_1", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
						a.getHasMember().get(0).getSize(), "last en_list_1", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies but-last "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(1).isComplex() && a.getHasMember().get(1).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
							a.getHasMember().get(1).getSize(), "last en_list_2", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
						a.getHasMember().get(1).getSize(), "last en_list_2", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies but-last "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n";
				}
			else {  //if the 2 agents are of different type (ie no s2_energies)
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(0).isComplex() && a.getHasMember().get(0).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
							a.getHasMember().get(0).getSize(), "last en_list_1", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
						a.getHasMember().get(0).getSize(), "last en_list_1", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies but-last "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
				//check if the agent to be hatched is a complex himself (then it needs energy lists)
				if (a.getHasMember().get(1).isComplex() && a.getHasMember().get(1).getHasMember().size() == 2){
					String energylist2="";
					if (a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()))
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
					else
						energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
							a.getHasMember().get(0).getSize(), "last en_list_2", "0", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel()+")", 
							"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()+")", 
							energylist2)+"\n";
				}
				else
					code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
					a.getHasMember().get(1).getSize(), "last en_list_2", "0", "0", "0", "null")+"\n";
				code += "\t      set "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies but-last "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n";
			}
			code += "\t    ]\n";
			
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
				code += "\t  if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies and not empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t  if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies and not empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
				code += "\t    foreach "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t    foreach "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			//check if the agent to be hatched is a complex himself (then it needs energy lists)
			if (a.getHasMember().get(1).isComplex() && a.getHasMember().get(1).getHasMember().size() == 2){
				String energylist2="";
				if (a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()))
					energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
				else
					energylist2 = a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
				code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
						a.getHasMember().get(1).getSize(), "?", "0", "random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(0).getABMCodeLabel()+")", 
						"random (2 * lifespan-"+a.getHasMember().get(1).getHasMember().get(1).getABMCodeLabel()+")", energylist2)+"\n";
			}
			else
				code += "\t      "+hatchagent(a.getHasMember().get(1), 1, a.getHasMember().get(1).getColorString(), "random 100", 
					a.getHasMember().get(1).getSize(), "?", "0", "0", "0", "null")+"\n";
			code += "\t      ]\n";
			code += "\t    ]\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))			
				code += "\t  if not empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies and empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [\n";
			else
				code += "\t  if not empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies and empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			code += "\t    foreach "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies [\n";
			//check if the agent to be hatched is a complex himself (then it needs energy lists)
			if (a.getHasMember().get(0).isComplex() && a.getHasMember().get(0).getHasMember().size() == 2){
				String energylist2="";
				if (a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()))
					energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
				else
					energylist2 = a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
				code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
						a.getHasMember().get(0).getSize(), "?", "0", "random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(0).getABMCodeLabel()+")", 
						"random (2 * lifespan-"+a.getHasMember().get(0).getHasMember().get(1).getABMCodeLabel()+")", energylist2)+"\n";
			}
			else
				code += "\t      "+hatchagent(a.getHasMember().get(0), 1, a.getHasMember().get(0).getColorString(), "random 100", 
					a.getHasMember().get(0).getSize(), "?", "0", "0", "0", "null")+"\n";
			code += "\t      ]\n";
			code += "\t    ]\n";
			if (a.getHasMember().get(0).getABMCodeLabel().equals(a.getHasMember().get(1).getABMCodeLabel()))
				code += "\t    if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies [die]\n";
			else
				code += "\t    if empty? "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies or empty? "+a.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies [die]\n";
			code += "\t  ]\n";
		}
	
		code += "end\n\n";
		return code;
	}
	
	/**
	 * turtle dies when energy <= 0
	 * 
	 * @return code snippet for procedure
	 */
	private String generateCodeDeath() {
		String code = "\nto death                      ;; turtle procedure\n"
				+ "\t;; when energy dips below zero, die\n"
				+ "\tif energy <= 0  [ die ]\n";
		code += "end\n\n";

		return code;
	}

	/**
	 * adds mathematical functions to the document
	 */
	private void addFunctions(String theFile) {
		try {
			ArrayList<String> txt = (ArrayList<String>) Utils
					.readLines("files" + File.separator+theFile);
			for (String str : txt) {
				addToCode(str);
			}
		} catch (FileNotFoundException e) {
			try {
				ArrayList<String> txt = (ArrayList<String>) Utils
						.readLinesFromJar(theFile);
				for (String str : txt) {
					addToCode(str);
				}
			} catch (IOException ioe) {
				e.printStackTrace();
				ioe.printStackTrace();
				Utils.appendToFile(
						new StringBuffer()
								.append("\nFile MathUtils.nlogo not found. \n\tOutput file"
										+ " won't be conformant to requirements, but contains all necessary code. \n"),
						logging);
			}
			
		}

	}

	/**
	 * reads this.agentReproduceAlgo and / or this.agentDefaultReproduceAlgo
	 * expressed in MathML from the ontology and generates the NetLogo
	 * expression that represents the condition expressed by this algorithm eg:
	 * random-float 1 < ( 100 / 365) for stochastic pulse train implementation
	 * 
	 * @return the string of the condition, eg "random-float 1 < ( 100 / 365 )"
	 */
	private String generateReproductionCondition() {
		OntModel theontmodel = this.agents.get(0).getAgentInfoOntClass()
				.getOntModel();
		OntClass reproduceAlgoClass = null;
		Individual reproduceAlgoIndividual = null;
		// read MathML for agent reproduce method

		// System.out.println(this.agentReproduceAlgo.get(0));
		reproduceAlgoClass = Ontology.getOntClassFromURIString(theontmodel,
				this.agentReproduceAlgo.get(0)); // contains the URI of the
													// algorithm class
		if (reproduceAlgoClass == null)
			reproduceAlgoIndividual = Ontology.getOntIndividualFromURIString(
					theontmodel, this.agentReproduceAlgo.get(0)); // contains
																	// the URI
																	// of the
																	// algorithm
																	// class
		if (reproduceAlgoClass == null && reproduceAlgoIndividual == null) {
			// point to default class
			reproduceAlgoIndividual = Ontology.getOntIndividualFromURIString(
					theontmodel, this.agentDefaultReproduceAlgo.get(0)); // contains
																			// the
																			// URI
																			// of
																			// the
																			// algorithm
																			// class
		}
		String netlogomath = parseMathML(theontmodel, reproduceAlgoClass,
				reproduceAlgoIndividual);

		return netlogomath;
	}

	/**
	 * goes through all agents and if necessary writes reproduce procedures
	 * depending on whether the agent has the reproduceURI amongst its
	 * qualitative property values
	 * 
	 * @param agents2
	 */
	private void writeReproduce(List<Agent> agents2) {
		for (Agent ag : this.agents) {
			if (!ag.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents) && ag.hasQualPropValues()) {
				for (QualitativeProperty qp : ag.getQualPropValues()) {
					if (qp.getQuality().getURI().equals(this.reproduceURI)) {
						addToCode(this.generateCodeReproduceHomeostatic(ag));
						continue;
					}
				}
			}
		}
	}

	/**
	 * creates the slider at position x1, y1, x2, y2
	 * 
	 * @param reactionDistance2
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	private void setupReactionDistanceSlider(String reactionDistance2, int x1,
			int y1, int x2, int y2) {
		String code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n"
				+ y2 + "\n" + "reaction-distance\n" + "reaction-distance\n"
				+ "0\n" + "10\n";
		code += reactionDistance2 + "\n" + "1\n" + "1\n" + "NIL\n"
				+ "HORIZONTAL\n\n";
		addToCode(code);
	}

	private void setupReproductionRateSlider(Agent a, int x1, int y1, int x2,
			int y2) {
		String initvalue = "50";
		if (a.isComposite())
			initvalue = "0";
		String code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n"
				+ y2 + "\n" + "dupli-rate-" + a.getABMCodeLabel() + "\n"
				+ "dupli-rate-" + a.getABMCodeLabel() + "\n" + "0\n" + "100\n"
				+ initvalue + "\n" + "1\n" + "1\n" + "NIL\n" + "HORIZONTAL\n\n";
		addToCode(code);
	}

	private void setupLifespanInputBox(Agent a, int x1, int y1, int x2, int y2) {
		String initvalue = "100";
		if (a.isComposite())
			initvalue = "50";
		String code = "\n" + "INPUTBOX\n" +
		+ x1+ "\n"
		+ y1+ "\n"
		+ x2+ "\n"
		+ y2+ "\n"
		+ "lifespan-" + a.getABMCodeLabel() + "\n"
		+ initvalue+ "\n"
		+ 1+ "\n"
		+ 0+ "\n"
		+ "Number\n";
		addToCode(code);
	}
	
	private void setupBindingStrengthSlider(Agent a, int x1, int y1, int x2, int y2) {
		String initvalue = "100";
		String code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n" + y2
				+ "\n" + "bind-str-" + a.getABMCodeLabel() + "\n" + "bind-str-"
				+ a.getABMCodeLabel() + "\n" + "0\n" + "100\n" 
				+ initvalue + "\n" + "1\n" + "1\n"
				+ "NIL\n" + "HORIZONTAL\n\n";
		addToCode(code);
	}

	private void setupInitialAgentNumberSlider(Agent a, int x1, int y1, int x2,
			int y2) {
		String count = this.homeostatic_concentr_default;
		if (a.isComposite())
			count = "0";
		String code = "";
		if (a.hasMaxHomeostaticConcentration()) {
			count = a.getMaxHomeostaticConcentration();
			code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n" + y2
					+ "\n" + "ini-no-" + a.getABMCodeLabel() + "\n" + "ini-no-"
					+ a.getABMCodeLabel() + "\n" + "0\n" + count + "\n"
					+ Integer.parseInt(count) * 0.5 + "\n" + "1\n" + "1\n"
					+ "NIL\n" + "HORIZONTAL\n\n";
		}
		if (a.hasHomeostaticConcentration()) {
			count = a.getHomeostaticConcentration();
			code = "\n" + "SLIDER\n" + x1 + "\n" + y1 + "\n" + x2 + "\n" + y2
					+ "\n" + "ini-no-" + a.getABMCodeLabel() + "\n" + "ini-no-"
					+ a.getABMCodeLabel() + "\n" + "0\n" + count + "\n" + count
					+ "\n" + "1\n" + "1\n" + "NIL\n" + "HORIZONTAL\n\n";
		}
		addToCode(code);
	}

	/**
	 * iterates all bioProcesses and creates procedures implementing their
	 * effects in NetLogo if bp increases/decreases something inside the
	 * ontology that has not been instantiated in the code subprocesses/sub-cell
	 * types will be iterated and checked whether these are inside the KAM
	 * NetLogo procedure will have as input : effect : one of
	 * "increases_process" or "decreases_process"
	 * 
	 * @param bioProcesses2
	 */
	private void doWriteBioProcessProcedures(List<BioProcess> bioProcesses2,
			KamStore kamStore) {
		String code = "";
		for (BioProcess bp : bioProcesses2) {
			code = "";
			// TODO augment ontology by adding regions to bioprocesses;
			// implement here!
			code += "to " + bp.getABMCodeLabel().replaceAll(" ", "_")
					+ " [effect whichRegion]  ;; turtle procedure\n";
			code += "  let break 0  ;; ontology axioms of a process are either executed ALL in the simulation or, if one fails, NONE\n";
			code += "               ;; (AND connection)  0 : processes are executed;  1 : process execution is stopped \n";
			// ################################################
			// add information about the process from the KAM
			// ################################################
			code += this.generateKamBPCode(bp, kamStore);
			// TODOlater Attention: chooses first mapped ontClass it finds!
			// others will be discarded
			if (bp.getProcessInfoOntClass() == null
					|| bp.getProcessInfoOntClass().size() < 1) { // then there
																	// is no
																	// mapping
																	// to the
																	// ontology;
																	// create
																	// empty
																	// procedure
				code += "\t\t ;;no ontology mapping available\n";
				bp.addToCode(code);
				continue;
			}
			// otherwise connect to ontology
			// -------------------------------------------------
			OntClass bpOntClass = bp.getProcessInfoOntClass().get(0);
			OntClass processClass = Ontology.getOntClassFromURIString(
					bpOntClass.getOntModel(), processURI);
			Utils.appendToFile(
					new StringBuffer()
							.append("\nChecking ontology for increases,decreases axioms of bp: "
									+ bp.getBELTermLabel() + ": \n"), logging);
			OntProperty increases = bpOntClass.getOntModel().getOntProperty(
					this.increasesUri);
			OntProperty decreases = bpOntClass.getOntModel().getOntProperty(
					this.decreasesUri);

			// TODOlater increases and decreases might be translated via some
			// process execution rate
			// (eg 0 in normal case, positve for increases, negative for
			// decreases)
			// various cases:
			// TODO: bioprocess increases/decreases another process (that
			// increases/decreases a process ... recursion) that
			// increases/decreases an agent

			// process decreases process or an agent

			code += "  let codetoexecute \"\"\n";
			code += "  let closest one-of breed ;; to initialize\n";
			// ############## DECREASES from ontology #######################
			if (bpOntClass != null && decreases != null) { // ie: + - (increases
															// decreases)
				if (Utils.hasRestriction(bpOntClass, decreases)) {
					// then find WHAT it decreases
					Utils.appendToFile(
							new StringBuffer().append("\t" + decreases.getURI()
									+ ": "), logging);
					for (OntClass obj : Utils.generateObjectOntClassList(
							bpOntClass, decreases)) {
						Utils.appendToFile(
								new StringBuffer().append(obj.getLabel(null)
										+ " "), logging);
						// obj might be an agent or a bioprocess!
						if (Utils.isBioProcess(obj, this.bioProcesses)
								|| processClass.hasSubClass(obj)) {
							// [-]^+
							// case 1: obj is a bioProcess inside the BEL code,
							// ie, inside bioProcesses2
							if (Utils.isBioProcess(obj, this.bioProcesses)) {
								if (verbous)
									code += "\t ;; code source: ontology - process decreases process  [+]^+ \n";
								code += "\tif effect = \"increases_process\"   [\n";
								code += "\t\t\n";
								code += "\t\tset codetoexecute ( word codetoexecute \"   "
										+ Utils.getBioProcessByOntClass(obj,
												this.bioProcesses)
												.getABMCodeLabel()
										+ " \"decreases_process\" \"null\" ] \")\n";
								if (verbous)
									code += "\t;;if effect = \"decreases_process\" [ do nothing? ] \n";
								Utils.appendToFile(
										new StringBuffer()
												.append("\n\t\t"
														+ Utils.getBioProcessByOntClass(
																obj,
																this.bioProcesses)
																.getABMCodeLabel()
														+ " \"decreases_process\" \"null\" \n"),
										logging);
							}
							// case 2: obj is a bioProcess that is NOT in the
							// BEL code (can be a mixture of both cases, too)
							// iterate all the subclasses of obj and check if
							// they are inside this.bioProcesses
							for (OntClass subCl : Utils.getAllSubclasses(obj)) {
								if (Utils
										.isBioProcess(subCl, this.bioProcesses)) {
									if (verbous)
										code += "\t ;; source: ontology - process decreases sub-process  [+]^+ \n";
									code += "\tif effect = \"increases_process\"  [\n";
									code += "\t\tset codetoexecute (word  \"   "
											+ Utils.getBioProcessByOntClass(
													subCl, this.bioProcesses)
													.getABMCodeLabel()
											+ " \"decreases_process\" \"null\" ] \"codetoexecute)\n";
									if (verbous)
										code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
									Utils.appendToFile(
											new StringBuffer()
													.append("\n\t\t"
															+ Utils.getBioProcessByOntClass(
																	obj,
																	this.bioProcesses)
																	.getABMCodeLabel()
															+ " \"decreases_(sub)process\" \"null\" \n"),
											logging);
								}
							}
						}

						// bpontclass decreases abundance of a) an agent inside
						// the initialized agents or b) a possible agent from
						// the ontology
						if (Utils.isAgent(obj, this.agents)
								|| this.allPossibleAgents.containsValue(obj)) {
							if (Utils.isAgent(obj, this.agents)) {
								// agent increases process AND process decreases
								// abundance of an agent
								Utils.getAgentByOntClass(obj, this.agents)
										.setConnectedViaOntology(true);
								System.out.println("connected via ontology set to true for agent "+Utils.getAgentByOntClass(obj,
												this.agents).getABMCodeLabel());
								if (verbous)
									code += "\t;;code source: ontology : process decreases agent\n";
								code += "\tif effect = \"increases_process\"  \n";
								code += "\t	[\n";
								code += "\t  ifelse whichRegion != \"null\" \n";
								code += "\t		  [\n";
								code += "\t		    ifelse region = whichRegion\n";
								code += "\t		    [\n";
								code += "\t		      set closest min-one-of "
										+ Utils.getAgentByOntClass(obj,
												this.agents).getABMCodeLabel()
										+ "s with [region = whichregion] [distance myself] \n";
								code += "\t		      ifelse closest != nobody and distance closest <= reaction-distance\n";
								code += "\t		      [\n";
								code += "\t		            set codetoexecute (word codetoexecute \" ask turtle \" [who] of closest \" [ die ] \" )\n";
								code += "\t		      ]\n";
								code += "\t		      [\n";
								code += "\t		        set break 1\n";
								code += "\t		      ]\n";
								code += "\t		    ]\n";
								code += "\t         [\n";
								code += "\t           set break 1  ;; stop if regions don't match\n";
								code += "\t         ]\n";
								code += "\t		  ]\n";
								code += "\t		  [\n";
								code += "\t		      set closest min-one-of "
										+ Utils.getAgentByOntClass(obj,
												this.agents).getABMCodeLabel()
										+ "s [distance myself] \n";
								code += "\t		      ifelse closest != nobody and distance closest <= reaction-distance\n";
								code += "\t		      [\n";
								code += "\t		            set codetoexecute (word codetoexecute \" ask turtle \" [who] of closest \" [ die ] \" )\n";
								code += "\t		      ]\n";
								code += "\t		      [\n";
								code += "\t		        set break 1\n";
								code += "\t		      ]\n";
								code += "\t		  ]\n";
								code += "\t		]\n";
								if (verbous)
									code += "\t;;if effect = \"decreases_process\"  [ do nothing ] \n";
							}
							if (this.allPossibleAgents.containsValue(obj)) {
								for (OntClass possibleAgentClass : this.allPossibleAgents
										.values()) {
									if (possibleAgentClass.getURI().equals(
											obj.getURI())) {
										// if bpontclass is a possible agent
										// from the ontology
										// then check if subclasses of
										// possibleAgentClass are inside
										// this.agents and implement behaviour
										// for the subclass(es)
										for (OntClass subCl : Utils
												.getAllSubclasses(possibleAgentClass)) {
											for (Agent a : this.agents) {
												if (subCl
														.getURI()
														.equals(a
																.getAgentInfoOntClass()
																.getURI())) {
													a.setConnectedViaOntology(true);
													if (verbous)
														code += "\t;;code source: ontology : process decreases sub-agent (subclass) of possible agent\n";
													code += "\tif effect = \"increases_process\" \n";
													code += "\t	[\n";
													code += "\t  ifelse whichRegion != \"null\" \n";
													code += "\t		  [\n";
													code += "\t		    ifelse region = whichRegion\n";
													code += "\t		    [\n";
													code += "\t		      set closest min-one-of "
															+ a.getABMCodeLabel()
															+ "s with [region = whichregion] [distance myself] \n";
													code += "\t		      ifelse closest != nobody and distance closest <= reaction-distance\n";
													code += "\t		      [\n";
													code += "\t		            set codetoexecute (word codetoexecute \" ask turtle \" [who] of closest \" [ die ] \" )\n";
													code += "\t		      ]\n";
													code += "\t		      [\n";
													code += "\t		        set break 1\n";
													code += "\t		      ]\n";
													code += "\t		    ]\n";
													code += "\t         [\n";
													code += "\t           set break 1  ;; stop if regions don't match\n";
													code += "\t         ]\n";
													code += "\t		  ]\n";
													code += "\t		  [\n";
													code += "\t		      set closest min-one-of "
															+ a.getABMCodeLabel()
															+ "s [distance myself] \n";
													code += "\t		      ifelse closest != nobody and distance closest <= reaction-distance\n";
													code += "\t		      [\n";
													code += "\t		            set codetoexecute (word codetoexecute \" ask turtle \" [who] of closest \" [ die ] \" )\n";
													code += "\t		      ]\n";
													code += "\t		      [\n";
													code += "\t		        set break 1\n";
													code += "\t		      ]\n";
													code += "\t		  ]\n";
													code += "\t		]\n";
													if (verbous)
														code += "\t;;if effect = \"decreases_process\"  [ do nothing ] \n";
												}
											}
										}
									}
								}
							}
						}

						code += "\n";
					}
				}
				Utils.appendToFile(new StringBuffer().append("\n"), logging);
			}

			// ############## INCREASES from ontology #######################
			if (bpOntClass != null && increases != null) {
				if (Utils.hasRestriction(bpOntClass, increases)) { // ie: + +
																	// (increases
																	// increases)
					// then find WHAT it increases
					Utils.appendToFile(
							new StringBuffer().append("\t" + increases.getURI()
									+ ": "), logging);
					for (OntClass obj : Utils.generateObjectOntClassList(
							bpOntClass, increases)) {
						Utils.appendToFile(
								new StringBuffer().append(obj.getLabel(null)
										+ " "), logging);
						// obj might be an agent or a bioprocess!
						// bpontclass increases a bioprocess inside a) the
						// bioProcesses list (KAM) or b) the ontology
						if (Utils.isBioProcess(obj, this.bioProcesses)
								|| Ontology.hasSubClass(processClass, obj)) {
							// [+]^+
							// case 1: obj is a bioProcess inside the BEL code,
							// ie, inside bioProcesses2
							if (Utils.isBioProcess(obj, this.bioProcesses)) {
								if (verbous)
									code += "\t ;; code source: ontology - process increases process  [+]^+ \n";
								code += "\tif effect = \"increases_process\"  [\n";
								code += "\t\tset codetoexecute (word  \"  "
										+ Utils.getBioProcessByOntClass(obj,
												this.bioProcesses)
												.getABMCodeLabel()
										+ " \"increases_process\" \"null\"  ] \" codetoexecute)\n";
								if (verbous)
									code += "\t;;if effect = \"decreases_process\"  [ do nothing ] \n";
								Utils.appendToFile(
										new StringBuffer()
												.append("\n\t\t"
														+ Utils.getBioProcessByOntClass(
																obj,
																this.bioProcesses)
																.getABMCodeLabel()
														+ " \"increases_process\" \"null\"\n"),
										logging);
							}
							// case 2: obj is a bioProcess that is NOT in the
							// BEL code (can be a mixture of both cases, too)
							// iterate all the subclasses of obj and check if
							// they are inside this.bioProcesses
							for (OntClass subCl : Utils.getAllSubclasses(obj)) {
								if (Utils
										.isBioProcess(subCl, this.bioProcesses)) {
									if (verbous)
										code += "\t ;; source: ontology - process increases sub-process  [+]^+ \n";
									code += "\tif effect = \"increases_process\"  [\n";
									code += "\t\tset codetoexecute ( word codetoexecute \" "
											+ Utils.getBioProcessByOntClass(
													subCl, this.bioProcesses)
													.getABMCodeLabel()
											+ " \"increases_process\" \"null\" ] \")\n";
									if (verbous)
										code += "\t;;if effect = \"decreases_process\"  [ do nothing ] \n";
									Utils.appendToFile(
											new StringBuffer()
													.append("\n\t\t"
															+ Utils.getBioProcessByOntClass(
																	obj,
																	this.bioProcesses)
																	.getABMCodeLabel()
															+ " \"increases_(sub)process\" \"null\" \n"),
											logging);
								}
							}
						}

						// bpontclass increases abundance of a) an agent inside
						// the initialized agents or b) a possible agent from
						// the ontology
						if (Utils.isAgent(obj, this.agents)
								|| this.allPossibleAgents.containsValue(obj)) {
							if (Utils.isAgent(obj, this.agents)) {
								// agent increases process AND process increases
								// abundance of an agent
								Agent ag = Utils.getAgentByOntClass(obj,
										this.agents);
								ag.setConnectedViaOntology(true);
								if (verbous)
									code += "\t;;code source: ontology : process increases agent\n";
								//TODO check if agent is a composite or a complex
								code += "\tif effect = \"increases_process\" [\n";
								code += "\t ifelse whichRegion != \"null\"  [\n";
								code += "\t   ifelse region = whichRegion   [\n";
								code += "\t     set codetoexecute (word  \"  "
										+ hatchagent(
												ag,
												1,
												ag.getColorString(),
												"random 100",
												ag.getSize(),
												"random (2 * lifespan-"
														+ ag.getABMCodeLabel()
														+ ")", "0", "0", "0", "null") + "\" codetoexecute )\n";
								code += "\t	    ]\n";
								code += "\t     [\n";
								code += "\t       set break 1\n";
								code += "\t     ]\n";
								code += "\t	  ]\n";
								code += "\t	  [\n";
								code += "\t     set codetoexecute (word  \"  "
										+ hatchagent(
												ag,
												1,
												ag.getColorString(),
												"random 100",
												ag.getSize(),
												"random (2 * lifespan-"
														+ ag.getABMCodeLabel()
														+ ")", "0", "0", "0", "null") + " \" codetoexecute)\n";
								code += "\t	   ]\n";
								code += "\t	  ]\n";
								if (verbous)
									code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
							}
							// bpontclass increases a possible agent from the
							// ontology
							if (this.allPossibleAgents.containsValue(obj)) {
								for (OntClass possibleAgentClass : this.allPossibleAgents
										.values()) {
									if (possibleAgentClass.getURI().equals(
											obj.getURI())) {
										Agent ag = Utils.getAgentByOntClass(
												obj, this.agents);
										// if bpontclass is a possible agent
										// from the ontology
										// then check if subclasses of
										// possibleAgentClass are inside
										// this.agents and implement behaviour
										// for the subclass(es)
										for (OntClass subCl : Utils
												.getAllSubclasses(possibleAgentClass)) {
											for (Agent a : this.agents) {
												if (subCl
														.getURI()
														.equals(a
																.getAgentInfoOntClass()
																.getURI())) {
													a.setConnectedViaOntology(true);
													if (verbous)
														code += "\t;;source: ontology   process increases agent\n";
													//TODO check if agent is a composite or a complex
													if (verbous)
														code += "\t;;"
																+ bpOntClass
																		.getLabel(null)
																+ " increases "
																+ a.getABMCodeLabel()
																+ "\n";
													code += "\tif effect = \"increases_process\"  [\n";
													code += "\t  set codetoexecute ( word (codetoexecute \"  "
															+ hatchagent(
																	ag,
																	1,
																	ag.getColorString(),
																	"random 100",
																	ag.getSize(),
																	"random (2 * lifespan-"
																			+ ag.getABMCodeLabel()
																			+ ")",
																	"0", "0", "0", "null")
															+ " \")\n";
													code += "\t	]\n";
													if (verbous)
														code += "\t;;if effect = \"decreases_process\"  [ do nothing ] \n";
												}
											}
										}
									}
								}
							}
						}
					}
				}
				Utils.appendToFile(new StringBuffer().append("\n"), logging);
			}
			code += "  if break != 1 \n";
			code += "  [ \n";
			code += "    ;;let execcode task [ report codetoexecute ] \n";
			code += "    run codetoexecute \n";
			code += "  ] \n";
			bp.addToCode(code);
		}
	}

	/**
	 * iterates the whole kam and writes some lines of code corresponding to
	 * what the process does according to the BEL code
	 * 
	 * @param bp
	 * @return code string
	 */
	private String generateKamBPCode(BioProcess bp, KamStore kamStore) {
		String code = "";
		// bp acts on an agent
		// region abfragen
		for (Agent a : bp.getIncreasesAbundance()) {
			if (verbous)
				code += "\t;;code source: KAM : process increases agent\n";
			//TODO check if agent is a composite or a complex
			if (verbous)
				code += "\t;;" + bp.getABMCodeLabel() + " increases "
						+ a.getABMCodeLabel() + "\n";
			code += "\tif effect = \"increases_process\" \n";
			code += "\t	[\n";
			code += "\t  ifelse whichRegion != \"null\" \n";
			code += "\t	 [\n";
			code += "\t    if region = whichRegion\n";
			;
			code += "\t		  [\n";
			code += hatchagent(a, 1, a.getColorString(), "random 100",
					a.getSize(), "random (2 * lifespan-" + a.getABMCodeLabel()
							+ ")", "0", "0", "0", "null")
					+ "\n";
			code += "\t		  ]\n";
			code += "\t  ]\n";
			code += "\t	 [\n";
			code += hatchagent(a, 1, a.getColorString(), "random 100",
					a.getSize(), "random (2 * lifespan-" + a.getABMCodeLabel()
							+ ")", "0", "0", "0", "null")
					+ "\n";
			code += "\t	 ]\n";
			code += "\t	]\n";
			if (verbous)
				code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
		}
		for (Agent a : bp.getDecreasesAbundance()) {
			if (verbous)
				code += "\t;;code source: KAM : process decreases agent\n";
			if (verbous)
				code += "\t;;" + bp.getABMCodeLabel() + " decreases "
						+ a.getABMCodeLabel() + "\n";
			code += "\tif effect = \"increases_process\" \n";
			code += "\t	[\n";
			code += "\t  ifelse whichRegion != \"null\" \n";
			code += "\t		  [\n";
			code += "\t		    if region = whichRegion\n";
			code += "\t		    [\n";
			code += "\t		      if one-of " + a.getABMCodeLabel()
					+ "s != nobody\n";
			code += "\t		      [\n";
			code += "\t		            ask one-of " + a.getABMCodeLabel()
					+ "s [ die ]\n";
			code += "\t		      ]\n";
			code += "\t		    ]\n";
			code += "\t		  ]\n";
			code += "\t		  [\n";
			code += "\t		      if one-of " + a.getABMCodeLabel()
					+ "s != nobody\n";
			code += "\t		      [\n";
			code += "\t		            ask one-of " + a.getABMCodeLabel()
					+ "s [ die ]\n";
			code += "\t		      ]\n";
			code += "\t		  ]\n";
			code += "\t		]\n";
			if (verbous)
				code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
		}
		// bp acts on an agent's activity
		// eg
		// bp(GO:"positive regulation of peripheral T cell tolerance induction")
		// -| act(a(Cell:"effector T cell"))
		// bp("latent EBV virus infection") -> act(a(Cell:"effector T cell"))
		for (Agent a : this.agents) {
			for (Activity thisact : a.getActivities()) {
				if (thisact.getRelationship() == Relationship.DECREASEDBY
						&& thisact.getObject() == bp) {
					if (verbous)
						code += "\t;;code source: KAM : process decreases agent activity\n";
					if (verbous)
						code += "\t;;" + bp.getABMCodeLabel() + " decreases "
								+ thisact.getType() + " of "
								+ a.getABMCodeLabel() + "\n";
					code += "\tif effect = \"increases_process\" \n";
					code += "\t	[\n";
					code += "\t  ifelse whichRegion != \"null\" \n";
					code += "\t		  [\n";
					code += "\t		    if region = whichRegion\n";
					code += "\t		    [\n";
					code += "\t		      if one-of " + a.getABMCodeLabel()
							+ "s != nobody\n";
					code += "\t		      [\n";
					if (!a.IsInActive()) {
						code += "\t		          if random 100 <= activity\n";
						code += "\t		          [\n";
						code += "\t		            ask "
								+ a.getABMCodeLabel()
								+ "s [  if activity > 0 and random 100 <= activity [ set activity activity - 1 ] \n";
						code += "\t		          ]\n";
						code += "\t		      ]\n";
					}
					code += "\t		    ]\n";
					code += "\t		  ]\n";
					code += "\t		  [\n";
					code += "\t		      if one-of " + a.getABMCodeLabel()
							+ "s != nobody\n";
					code += "\t		      [\n";
					if (!a.IsInActive()) {
						code += "\t		          if random 100 <= activity\n";
						code += "\t		          [\n";
						code += "\t		            ask "
								+ a.getABMCodeLabel()
								+ "s [  if activity > 0 and random 100 <= activity [ set activity activity - 1 ] ] \n";
						code += "\t		          ]\n";
					}
					code += "\t		      ]\n";
					code += "\t		  ]\n";
					code += "\t		  ]\n";
					if (verbous)
						code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
				}
				if (thisact.getRelationship() == Relationship.INCREASEDBY
						&& thisact.getObject() == bp) {
					if (!a.IsInActive()) {
						if (verbous)
							code += "\t;;code source: KAM : process increases agent activity\n";
						if (verbous)
							code += "\t;;" + bp.getABMCodeLabel()
									+ " increases " + thisact.getType()
									+ " of " + a.getABMCodeLabel() + "\n";
						code += "\tif effect = \"increases_process\" \n";
						code += "\t	[\n";
						code += "\t  ifelse whichRegion != \"null\" \n";
						code += "\t	 [\n";
						code += "\t    if region = whichRegion\n";
						code += "\t	   [\n";
						code += "\t		      if one-of " + a.getABMCodeLabel()
								+ "s != nobody\n";
						code += "\t		      [\n";
						if (!a.IsInActive()) {
							code += "\t		          if random 100 <= activity\n";
							code += "\t		          [\n";
							code += "\t		            ask "
									+ a.getABMCodeLabel()
									+ "s [ if activity < 100 [ set activity activity + 1 ]] \n";
							code += "\t		          ]\n";
							code += "\t		      ]\n";
						}
						code += "\t		  ]\n";
						code += "\t		]\n";
						code += "\t		[\n";
						code += "\t		      if one-of " + a.getABMCodeLabel()
								+ "s != nobody\n";
						code += "\t		      [\n";
						if (!a.IsInActive()) {
							code += "\t		          if random 100 <= activity\n";
							code += "\t		          [\n";
							code += "\t		            ask "
									+ a.getABMCodeLabel()
									+ "s [ if activity < 100 [ set activity activity + 1 ]] \n";
							code += "\t		          ]\n";
						}
						code += "\t		      ]\n";
						code += "\t		  ]\n";
						code += "\t		]\n";
						if (verbous)
							code += "\t;;if effect = \"decreases_process\" [ do nothing ] \n";
					}
				}
			}
		}
		// bp acts on a process
		for (BioProcess proc : bp.getIncreasesBioProcess()) {
			ArrayList<Annotation> annotations = getAnnotationsFromNodes(
					proc.getProcessInfoKamNode(), "increases",
					bp.getProcessInfoKamNode(), kamStore);
			ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
			if (verbous)
				code += "\t;;code source: KAM : process increases process\n";
			if (verbous)
				code += "\t;;" + bp.getABMCodeLabel() + " increases "
						+ proc.getABMCodeLabel() + "\n";
			code += "\tif effect = \"increases_process\" [\n";
			if (validRegions != null && validRegions.size() > 0)
				for (Region re : validRegions) {
					code += "\t\t  " + proc.getABMCodeLabel()
							+ " \"increases_process\" \""
							+ re.getABMCodeLabel() + "\" ]\n";
				}
			else {
				code += "\t\t" + proc.getABMCodeLabel()
						+ " \"increases_process\" \"null\" ]\n";
			}
			if (verbous)
				code += "\t;;if effect = \"decreases_process\" [  do nothing ]\n";
		}
		for (BioProcess proc : bp.getDecreasesBioProcess()) {
			ArrayList<Annotation> annotations = getAnnotationsFromNodes(
					proc.getProcessInfoKamNode(), "increases",
					bp.getProcessInfoKamNode(), kamStore);
			ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
			if (verbous)
				code += "\t;;code source: KAM : process decreases process\n";
			if (verbous)
				code += "\t;;" + bp.getABMCodeLabel() + " decreases "
						+ proc.getABMCodeLabel() + "\n";
			code += "\tif effect = \"increases_process\" [\n";
			if (validRegions != null && validRegions.size() > 0)
				for (Region re : validRegions) {
					code += "\t\t  " + proc.getABMCodeLabel()
							+ " \"decreases_process\" \""
							+ re.getABMCodeLabel() + "\" ]\n";
				}
			else {
				code += "\t\t" + proc.getABMCodeLabel()
						+ " \"decreases_process\" \"null\" ]\n";
			}
			if (verbous)
				code += "\t;;if effect = \"decreases_process\" [  do nothing ]\n";
		}
		// bp increases or decreases a tloc
		code += generateTlocCode(bp);
		return code;
	}

	/**
	 * 
	 * @param a
	 * @param numberagents
	 * @param colorString
	 * @param activity
	 * @param size
	 * @param energy
	 * @param energy_member1  set to zero if none
	 * @param energy_member2  set to zero if none
	 * @return
	 */
	private String hatchagent(Agent a, int numberagents, String colorString,
			String activity, double size, String energy, String memberDistance, 
			String energy_member1, String energy_member2, String energylist_member_2) {
		String code;
		code = "       hatch-" + a.getABMCodeLabel() + "s 1 [";
		code += " set color " + colorString;
		if (!a.IsInActive())
			code += " set activity " + activity;
		code += " set size " + size;
		code += " set energy " + energy;
		if (a.isComposite() && a.getIncludes().size() == 2){
			code += " set memberdist " + memberDistance;
			if (!energy_member1.equals("0")){
				code += " set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies [] " +" set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput " + energy_member1
				+" "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies" ;
			}
			if (!energy_member1.equals("0") && !energylist_member_2.equals("null"))
				code += " set "+energylist_member_2+" [] " +" set "+energylist_member_2+" fput " + energy_member2 +" " + energylist_member_2;
		}
		if (a.isComplex() && a.getHasMember().size() == 2){
			if (!energy_member1.equals("0")){
				code += " set "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies [] " +" set "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput " + energy_member1
				+" "+a.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies" ;
			}
			if (!energy_member1.equals("0") && !energylist_member_2.equals("null"))
				code += " set "+energylist_member_2+" [] " +" set "+energylist_member_2+" fput " + energy_member2 +" " + energylist_member_2;
		}
		code += " ] ";
		return code;
	}

	/**
	 * 
	 * @param annotations
	 * @return
	 */
	private ArrayList<Region> getRegionsFromAnnotations(
			ArrayList<Annotation> annotations) {
		ArrayList<Region> retRegions = new ArrayList<Region>();
		String name;
		String dictionary;
		for (Annotation anno : annotations) {
			name = anno.getValue();
			dictionary = anno.getAnnotationType().getName();
			if (Region.exists(getregions(), name, dictionary)
					&& !retRegions.contains(Region.getRegion(getregions(),
							name, dictionary))) {
				retRegions
						.add(Region.getRegion(getregions(), name, dictionary));
			}
		}
		return retRegions;
	}

	/**
	 * returns annotations between the two nodes; currently only implemented for
	 * string one of "increases", "decreases", "translocates", "increasedBy", "decreasedBy"
	 * 
	 * @param processInfoKamNode
	 * @param string one of "increases", "decreases", "translocates", "increasedBy", "decreasedBy"
	 * @param agentInfoKamNode
	 * @param kamStore
	 * @return
	 */
	private ArrayList<Annotation> getAnnotationsFromNodes(
			KamNode processInfoKamNode, String string,
			KamNode agentInfoKamNode, KamStore kamStore) {
		ArrayList<Annotation> annos = new ArrayList<Annotation>();
		List<Annotation> annotations;
		for (KamEdge edge : processInfoKamNode.getKam().getEdges()) {
			List<BelStatement> statements;
			try {
				statements = kamStore.getSupportingEvidence(edge);
				for (BelStatement statement : statements) {
					if (string.equalsIgnoreCase("increasedBy")
							&& (statement.getRelationshipType() == RelationshipType.INCREASES || statement
									.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)) {
						if (statement.getSubject().getId() == processInfoKamNode
								.getId()
								&& statement.getObject().getId() == agentInfoKamNode
										.getId()
								|| statement.getObject().getId() == processInfoKamNode
										.getId()
								&& statement.getSubject().getId() == agentInfoKamNode
										.getId()) {
							// System.out.println(statement.getSubject().getLabel()
							// + " "+ statement.getObject());
							annotations = statement.getAnnotationList();
							for (Annotation annotation : annotations) {
								annos.add(annotation);
							}
						}
					}
					if (string.equalsIgnoreCase("decreasedBy")
							&& (statement.getRelationshipType() == RelationshipType.DECREASES || statement
									.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)) {
						if (statement.getSubject().getId() == processInfoKamNode
								.getId()
								&& statement.getObject().getId() == agentInfoKamNode
										.getId()
								|| statement.getObject().getId() == processInfoKamNode
										.getId()
								&& statement.getSubject().getId() == agentInfoKamNode
										.getId()) {
							// System.out.println(statement.getSubject().getLabel()
							// + " "+ statement.getObject());
							annotations = statement.getAnnotationList();
							for (Annotation annotation : annotations) {
								annos.add(annotation);
							}
						}
					}
					
					if (string.equalsIgnoreCase("increases")
							&& (statement.getRelationshipType() == RelationshipType.INCREASES || statement
									.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)) {
						if (statement.getSubject().getId() == processInfoKamNode
								.getId()
								&& statement.getObject().getId() == agentInfoKamNode
										.getId()
								|| statement.getObject().getId() == processInfoKamNode
										.getId()
								&& statement.getSubject().getId() == agentInfoKamNode
										.getId()) {
							// System.out.println(statement.getSubject().getLabel()
							// + " "+ statement.getObject());
							annotations = statement.getAnnotationList();
							for (Annotation annotation : annotations) {
								// System.out.println(annotation.getAnnotationType().getName()+" "+annotation.getValue());
								annos.add(annotation);
							}
						}
					}
					
					if (string.equalsIgnoreCase("decreases")
							&& (statement.getRelationshipType() == RelationshipType.DECREASES || statement
									.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)) {
						if (statement.getSubject().getId() == processInfoKamNode
								.getId()
								&& statement.getObject().getId() == agentInfoKamNode
										.getId()
								|| statement.getObject().getId() == processInfoKamNode
										.getId()
								&& statement.getSubject().getId() == agentInfoKamNode
										.getId()) {
							// System.out.println(statement.getSubject().getLabel()
							// + " "+ statement.getObject());
							annotations = statement.getAnnotationList();
							for (Annotation annotation : annotations) {
								// System.out.println(annotation.getAnnotationType().getName()+" "+annotation.getValue());
								annos.add(annotation);
							}
						}
					}
					if (string.equalsIgnoreCase("translocates")
							&& (statement.getRelationshipType() == RelationshipType.TRANSLOCATES)) {
						if (statement.getSubject().getId() == processInfoKamNode
								.getId()
								&& statement.getObject().getId() == agentInfoKamNode
										.getId()
								|| statement.getObject().getId() == processInfoKamNode
										.getId()
								&& statement.getSubject().getId() == agentInfoKamNode
										.getId()) {
							// System.out.println(statement.getSubject().getLabel()
							// + " "+ statement.getObject());
							annotations = statement.getAnnotationList();
							for (Annotation annotation : annotations) {
								// System.out.println(annotation.getAnnotationType().getName()+" "+annotation.getValue());
								annos.add(annotation);
							}
						}
					}
				}
			} catch (KamStoreException e) {
				Utils.appendToFile(
						new StringBuffer()
								.append("\nException while trying to get annotations from nodes "
										+ processInfoKamNode.getLabel()
										+ " and "
										+ agentInfoKamNode.getLabel()
										+ "\n"), logging);
			}

		}
		return annos;
	}

	/**
	 * reads fileFormatCompliance.txt and appends it to the code
	 */
	private void addStuffForFileFormatCompliance() {
		try {
			ArrayList<String> txt = (ArrayList<String>) Utils
					.readLines("files/fileFormatCompliance.txt");
			for (String str : txt) {
				addToCode(str);
			}
		} catch (FileNotFoundException e) {
			try {
				ArrayList<String> txt = (ArrayList<String>) Utils
						.readLinesFromJar("fileFormatCompliance.txt");
				for (String str : txt) {
					addToCode(str);
				}
			} catch (IOException ioe){
				e.printStackTrace();
				ioe.printStackTrace();
				Utils.appendToFile(
						new StringBuffer()
								.append("\nFile fileFormatCompliance.txt not found. \n\tOutput file"
										+ " won't be conformant to requirements, but contains all necessary code. \n"),
						logging);
			}
			
		}

	}

	private void setupPLOT(List<Agent> agents2) {
		String code = "\n";
		code += "PLOT\n" + "712\n" + "25\n" + "1099\n" + "255\n"
				+ "populations\n" + "time \n" + "entities\n" + "0.0\n"
				+ "100.0\n" + "0.0\n" + "100.0\n" + "true\n" + "true\n";
		code += "\"\" \"\"\n" + "PENS\n";
		for (Agent ag : agents2) {
			if (ag.getChosenByUser()) {
				code += "\"" + ag.getABMCodeLabel() + "\"  1.0 0 "
						+ ag.getPlottingColor() + " true \"\" \"\"\n";
			}
		}
		addToCode(code);
	}

	private void setupGOButton() {
		addToCode("\nBUTTON\n" + "90\n" + "28\n" + "157\n" + "61\n" + "go\n"
				+ "go\n" + "T\n" + "1\n" + "T\n" + "OBSERVER\n" + "NIL\n"
				+ "NIL\n" + "NIL\n" + "NIL\n" + "1\n");
	}

	private void setupSETUPButton() {
		addToCode("\nBUTTON\n" + "8\n" + "28\n" + "77\n" + "61\n" + "setup\n"
				+ "setup\n" + "NIL\n" + "1\n" + "T\n" + "OBSERVER\n" + "NIL\n"
				+ "NIL\n" + "NIL\n" + "NIL\n" + "1\n");
	}

	private void setupGraphicsWindow() {
		addToCode("\n@#$#@#$#@\n" + "GRAPHICS-WINDOW\n" + "230\n" + "10\n"
				+ "699\n" + "500\n" + "25\n" + "25\n" + "9.0\n" + "1\n"
				+ "14\n" + "1\n" + "1\n" + "1\n" + "0\n" + "1\n" + "1\n"
				+ "1\n" + "-25\n" + "25\n" + "-25\n" + "25\n" + "1\n" + "1\n"
				+ "1\n" + "ticks\n" + "1.0\n");
	}

	@SuppressWarnings("unused")
	private void doGo(List<Agent> agents2, KamStore kamStore) {
		// here come the agents' behaviours
		String agCode = "";
		// if (verbous) agCode +=
		// ";;  IMPLEMENTATION OF KAM BEHAVIOUR ONLY FROM LEFT TO RIGHT (ie INCREASES but not INCREASED BY)\n"
		// +
		// ";; except for activities for the way they are saved in the code\n";
		ArrayList<Agent> agentPlusSubAgents = new ArrayList<Agent>();
		for (Agent ag : agents2) {
			if (ag.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents))
				continue;
			agentPlusSubAgents = new ArrayList<Agent>();
			agentPlusSubAgents.add(ag);
			agentPlusSubAgents.addAll(ag.getSubAgents(agents2));
			// implement the behaviour for the agent itself and all its
			// subagents, if there are any
			// ie whenever a turtle is asked to do something, this needs to be
			// implemented also for all "sub-turtles" (subclasses in the
			// ontology)
			for (Agent subagent : agentPlusSubAgents) {
				//TODO ag -> or -| reaction
				//TODO act(ag) -> or -| reaction
				
				// tloc procedure(s) == but write only for the given statement
				// ==
				agCode += generateTlocCode(ag);

				// agent increases and decreases other abundances
				for (Agent ag2 : ag.getIncreasesAbundance()) {
					agCode += this.generateCodeIncreasesAbundance(ag, ag2,
							kamStore);
				}
				for (Agent ag2 : ag.getDecreasesAbundance()) {
					agCode += this.generateCodeDecreasesAbundance(ag, ag2,
							kamStore);
				}

				// participation in processes
				for (BioProcess bp : ag.getIncreasesBioProcess()) {
					agCode += this.generateCodeIncreasesBioProcess(ag, bp,
							kamStore);
				}
				for (BioProcess bp : ag.getIncreasedByBioProcess()) {
					// not to be implemented, only from left to right
				}
				for (BioProcess bp : ag.getDecreasesBioProcess()) {
					agCode += this.generateCodeDecreasesBioProcess(ag, bp,
							kamStore);
				}
				for (BioProcess bp : ag.getDecreasedByBioProcess()) {
					// not to be implemented, only from left to right
				}

				// agent is biomarker for a process
				for (BioProcess bp : ag.getIsBiomarkerForProcess()) {
					//agCode += this.generateCodeIsBiomarkerForBioProcess(ag, bp);
				}

				// agent is a complex or a composite; whenever its members meet
				// and are close enough -> complex formation
				if (ag.isComplex()){
					//save code in all the member agents
					ArrayList<Agent> agentsDone = new ArrayList <Agent>();
					@SuppressWarnings("unchecked")
					ArrayList<Agent> agmembers = (ArrayList<Agent>) ((ArrayList<Agent>) ag.getHasMember()).clone();
					for (Agent memb : agmembers){
						//in case of dimers don't add the code twice  eg app.app needs to be saved only once in agent app
						if (!agentsDone.contains(memb)) memb.addToGOCode(complexFormationCode(ag, memb));   //is added to the go code of first member of ag
						ArrayList<Agent> tempMemberList = (ArrayList<Agent>) ag.getHasMember();
						tempMemberList.remove(0); // this is agent a
						tempMemberList.add(memb);    //remove from first position and add as last item
						ag.setHasMember(tempMemberList);
						agentsDone.add(memb);
					} 
					//restore to original order
					ag.setHasMember(agmembers);
					// additionally check whether agent can be in a 1:n or n:1 relationship
					agCode += increaseComplexMemberCode(ag);
				}
				
				if (ag.isComposite()){
					//save code in all the included agents
					ArrayList<Agent> agentsDone = new ArrayList <Agent>();
					@SuppressWarnings("unchecked")
					ArrayList<Agent> agmembers = (ArrayList<Agent>) ((ArrayList<Agent>) ag.getIncludes()).clone();
					for (Agent memb : agmembers){
						//in case of 2 of the same entities don't add the code twice 
						if (!agentsDone.contains(memb)) memb.addToGOCode(compositeFormationCode(ag, memb));   //is added to the go code of first member of ag
						ArrayList<Agent> tempMemberList = (ArrayList<Agent>) ag.getIncludes();
						tempMemberList.remove(0); // this is agent a
						tempMemberList.add(memb);    //remove from first position and add as last item
						ag.setIncludes(tempMemberList);
						agentsDone.add(memb);
					} 
					//restore to original order
					ag.setIncludes(agmembers);
					// additionally check whether agent can be in a 1:n or n:1 relationship
					agCode += increaseCompositeMemberCode(ag);
				} 
				
				//TODO reactions; if called by an abundance, save in there; 
				//TODO reactions; if called by a process, save in there; 
				//if not called, save in the reactants (implemented here below)
				for (Reaction r : getReactions()){
					if (r.getReactants().contains(ag)){
						agCode += reactionParticipationCode(ag, r, kamStore);
					}
				}
				
				// participation in activities
				for (Activity act : ag.getActivities()) {
					// TODOlater also consider type of activity, in a second
					// coding iteration
					// new Activity(Activity.getActivityType(act) ,
					// Activity.Relationship.INCREASES, tmpProcess);//type ,
					// relation , what

					// case 1: act(a(ag)) [-> or -|] bp and case 4: bp [-> or
					// -|] act(a(ag))
					// ////////////////////////////////////////////////////////////////////////////////////////
					if (act.getObject().getClass().equals(BioProcess.class)) {
						if (act.getRelationship() == Activity.Relationship.INCREASES) {
							ArrayList<Annotation> annotations = getAnnotationsFromNodes(
									act.getActivityNode(), "increases",
									((BioProcess) act.getObject())
											.getProcessInfoKamNode(), kamStore);
							ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
							// case 1.1
							if (!ag.IsInActive()) {
								if (verbous)
									agCode += "\t ;; source: KAM   case 1.1 act(a(ag)) [-> or -|] bp\n";
								if (verbous)
									agCode += "\t ;; act of "
											+ ag.getABMCodeLabel()
											+ " increases "
											+ ((BioProcess) act.getObject())
													.getABMCodeLabel() + "\n";
								if (validRegions != null
										&& validRegions.size() > 0)
									for (Region re : validRegions) {
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t    "
												+ ((BioProcess) act.getObject())
														.getABMCodeLabel()
												+ " \"increases_process\" \""
												+ re.getABMCodeLabel()
												+ "\" \n";
										agCode += "\t]\n";
									}
								else {
									agCode += "\tif random 100 < activity\n";
									agCode += "\t[\n";
									agCode += "\t\t   "
											+ ((BioProcess) act.getObject())
													.getABMCodeLabel()
											+ " \"increases_process\" \"null\" \n";
									agCode += "\t]\n";
								}
							}
						}
						if (act.getRelationship() == Activity.Relationship.DECREASES) {
							ArrayList<Annotation> annotations = getAnnotationsFromNodes(
									act.getActivityNode(), "decreases",
									((BioProcess) act.getObject())
											.getProcessInfoKamNode(), kamStore);
							ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
							// case 1.2
							if (!ag.IsInActive()) {
								if (verbous)
									agCode += "\t ;; source: KAM   case 1.2 act(a(ag)) [-> or -|] bp\n";
								if (verbous)
									agCode += "\t ;; act of "
											+ ag.getABMCodeLabel()
											+ " decreases "
											+ ((BioProcess) act.getObject())
													.getABMCodeLabel() + "\n";
								if (validRegions != null
										&& validRegions.size() > 0)
									for (Region re : validRegions) {
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t    "
												+ ((BioProcess) act.getObject())
														.getABMCodeLabel()
												+ " \"decreases_process\" \""
												+ re.getABMCodeLabel()
												+ "\" \n";
										agCode += "\t]\n";
									}
								else {
									agCode += "\tif random 100 < activity\n";
									agCode += "\t[\n";
									agCode += "\t\t   "
											+ ((BioProcess) act.getObject())
													.getABMCodeLabel()
											+ " \"decreases_process\" \"null\" \n";
									agCode += "\t]\n";
								}
							}
						}
						//TODO check if the following is true, maybe necessary to implement
						// if (act.getRelationship() ==
						// Activity.Relationship.INCREASEDBY ){
						// case 4.1
						// no need to implement, only from left to right
						// if (act.getRelationship() ==
						// Activity.Relationship.DECREASEDBY ){
						// case 4.2
						/* no need to implement, only from left to right */
						// }
					}

					// case 2: act(a(ag)) [-> or -|] act(a(ag2)) see case 5,
					// implemented!
					// /////////////////////////////////////////////////////////////////////////////////////////

					// case 3: act(a(ag)) [-> or -|] a(ag2)
					// /////////////////////////////////////////////////////////////////////////////////////////
					if (act.getObject().getClass().equals(Agent.class)) {
						if (act.getRelationship() == Activity.Relationship.INCREASES
								&& (act.getType() == ActivityType.MOLECULAR || act.getType() == ActivityType.KINASE
								    || act.getType() == ActivityType.CATALYTIC)) {
							ArrayList<Annotation> annotations = getAnnotationsFromNodes(
									act.getActivityNode(), "increases",
									((Agent) act.getObject())
											.getAgentInfoKamNode(), kamStore);
							ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
							// case 3.1
							if (!ag.IsInActive()) {
								if (verbous)
									agCode += "\t ;; source: KAM   case 3.1 act(a(ag)) [-> or -|] a(ag2)\n";
								if (verbous)
									agCode += "\t ;; molecular activity of "
											+ ag.getABMCodeLabel()
											+ " increases "
											+ ((Agent) act.getObject())
													.getABMCodeLabel() + "\n";
								if (validRegions != null
										&& validRegions.size() > 0)
									for (Region re : validRegions) {
										agCode += "\tif random 100 < activity \n"; // AND
																					// patch
																					// hat
																					// region
																					// =
																					// re
										agCode += "\t[\n";
										agCode += "\t if region = \""
												+ re.getABMCodeLabel() + "\"\n";
										agCode += "\t  [\n";
										agCode += "\t    ask patch-here \n";
										agCode += "\t    [\n";
										agCode += "\t        sprout-"
												+ ((Agent) act.getObject())
														.getABMCodeLabel()
												+ "s 1 [ lt 45  \n";
										agCode += "\t             set color "
												+ ((Agent) act.getObject())
														.getColorString()
												+ "\n";
										agCode += "\t             set size "
												+ ((Agent) act.getObject())
														.getSize() + "\n";
										agCode += "\t             set energy random (2 * lifespan-"
												+ ((Agent) act.getObject())
														.getABMCodeLabel()
												+ ")\n";
										if (!((Agent) act.getObject())
												.IsInActive())
											agCode += "\t             set activity random 100\n";
										if (((Agent) act.getObject())
												.isHomeostaticControl()
												&& ((Agent) act.getObject())
														.hasHomeostaticConcentration())
											agCode += "\t             set homeostatic-"
													+ ((Agent) act.getObject())
															.getHomeostaticConcentration()
													+ " \n";
										if (((Agent) act.getObject())
												.isHomeostaticControl()
												&& ((Agent) act.getObject())
														.hasMaxHomeostaticConcentration())
											agCode += "\t             set maxhomeostatic-"
													+ ((Agent) act.getObject())
															.getMaxHomeostaticConcentration()
													+ " \n";
										agCode += "\t        ]\n";
										agCode += "\t    ]\n";
										agCode += "\t  ]\n";
										agCode += "\t]\n";
									}
								else {
									Agent a = ((Agent) act.getObject());
									agCode += "\tif random 100 < activity \n";
									agCode += "\t  [\n";
									agCode += hatchagent(
											a,
											1,
											a.getColorString(),
											"random 100",
											a.getSize(),
											"random (2 * lifespan-"
													+ a.getABMCodeLabel() + ")",
											"0", "0", "0", "null")
											+ "\n";
									agCode += "\t  ]\n";
								}
							}
						}

						if (act.getRelationship() == Activity.Relationship.INCREASES
								&& act.getType() == ActivityType.TRANSPORT) {
							ArrayList<Annotation> annotations = getAnnotationsFromNodes(
									act.getActivityNode(), "increases",
									((Agent) act.getObject())
											.getAgentInfoKamNode(), kamStore);
							ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
							// case 3.1 (transport activity)
							// TODO so far only implemented for composites and
							// complexes and increases (eg
							// tport(chemokine) -> composite(Lti cell, Lto cell)
							if (!ag.IsInActive()) {
								if (verbous
										&& ((Agent) act.getObject())
												.isComposite()) {
									agCode += "\t ;; source: KAM   case 3.1 tport(a(ag)) ->  compositeAbundance(ab1,ag2)\n";
									agCode += "\t ;;       => grab closest composite members inside the region and get them closer to ag\n             \n";
								}
								if (verbous
										&& ((Agent) act.getObject())
												.isComposite())
									agCode += "\t ;; transport activity of "
											+ ag.getABMCodeLabel()
											+ " increases composite "
											+ ((Agent) act.getObject())
													.getABMCodeLabel() + "\n";
								if (((Agent) act.getObject()).isComposite()) {
									agCode += "\tif random 100 < activity \n";
									agCode += "\t[\n";
									// grab closest composite members inside the
									// region and get them closer to each other
									ArrayList<Agent> memberlist = (ArrayList<Agent>) ((Agent) act
											.getObject()).getIncludes();
									int cnt = 0;
									if (memberlist.size() > 1) {
										agCode += "\t   let this-"
												+ memberlist.get(0)
														.getABMCodeLabel()+ " min-one-of "+ memberlist.get(0).getABMCodeLabel()+ "s with [region = re and distance myself <= reaction-distance * 3 ] [ distance myself ]\n";
									}
									for (Agent member : memberlist) {
										if (cnt++ == 0 && memberlist.size() > 1)
											continue;
										if (memberlist.size() == 1) {
											// then it's a composite of 2 of the
											// same entities
											agCode += "\t    if count "+ ((Agent) act.getObject()).getABMCodeLabel() + "s with [region = re  and distance myself <= reaction-distance * 3 ]  > 1 [\n";
											agCode += "\t      let closest2 min-n-of 2 "
													+ member.getABMCodeLabel()
													+ "s with [region = re and distance myself <= reaction-distance * 3 ] [ distance myself ]\n";
											agCode += "\t       ask min-one-of closest2 [xcor] \n";
											agCode += "\t        [ \n";
											agCode += "\t         facexy x y\n";
											agCode += "\t         ask patch-ahead ("+member.getABMCodeLabel()+"-move-speed * 1.5)\n";
											agCode += "\t           [ if region = re [ ask min-one-of closest2 [xcor] [ fd ("+member.getABMCodeLabel()+"-move-speed * 1.5) ] ] ]\n";
											agCode += "\t       ]\n";
											agCode += "\t       ask max-one-of closest2 [xcor] \n";
											agCode += "\t        [ \n";
											agCode += "\t          facexy x y\n";
											agCode += "\t          ask patch-ahead ("+member.getABMCodeLabel()+"-move-speed * 1.5)\n";
											agCode += "\t           [ if region = re [ ask max-one-of closest2 [xcor] [fd ("+member.getABMCodeLabel()+"-move-speed* 1.5) ] ] ]\n";
											agCode += "\t        ] \n";
											agCode += "\t     ] \n";
											break;
										}
										agCode += "\t    let closest-"+ cnt+ " min-one-of "+ member.getABMCodeLabel()+ "s with [region = re and distance myself <= reaction-distance * 3 ] [ distance myself ]\n";
										agCode += "\t    if closest-"+ cnt+ " != nobody and this-"+ memberlist.get(0).getABMCodeLabel()+ " != nobody [\n";
										agCode += "\t      ask closest-"+ cnt + "\n";
										agCode += "\t            [ \n";
										agCode += "\t               facexy x y\n";
										agCode += "\t               ask patch-ahead ("+member.getABMCodeLabel()+"-move-speed * 1.5)\n";
										agCode += "\t                 [ if region = re [ ask closest-"+ cnt + " [ fd ("+member.getABMCodeLabel()+"-move-speed * 1.5) ] ] ] \n";
										agCode += "\t            ] \n";
										agCode += "\t      ask this-"+ memberlist.get(0).getABMCodeLabel()+ "\n";
										agCode += "\t            [ \n";
										agCode += "\t               facexy x y\n";
										agCode += "\t               ask patch-ahead ("+memberlist.get(0).getABMCodeLabel()+"-move-speed * 1.5)\n";
										agCode += "\t                 [ if region = re [ ask this-"+ memberlist.get(0).getABMCodeLabel()+ " [ fd ("+member.getABMCodeLabel()+"-move-speed * 1.5) ] ] ] \n";
										agCode += "\t            ] \n";
										agCode += "\t    ]	\n";
									}
									agCode += "\t]\n";
								}
								if (verbous
										&& ((Agent) act.getObject())
												.isComplex())
									agCode += "\t ;; transport activity of "
											+ ag.getABMCodeLabel()
											+ " increases composite "
											+ ((Agent) act.getObject())
													.getABMCodeLabel() + "\n";
								if (((Agent) act.getObject()).isComplex()) {
									if (verbous) {
										agCode += "\t ;; source: KAM   case 3.1 tport(a(ag)) ->  complexAbundance(ab1,ag2)\n";
										agCode += "\t ;;       => grab closest complex members inside the region and get them closer to each other\n             \n";
									}
									if (verbous)
										agCode += "\t ;; transport activity of "
												+ ag.getABMCodeLabel()
												+ " increases complex "
												+ ((Agent) act.getObject())
														.getABMCodeLabel()
												+ "\n";
									agCode += "\tif random 100 < activity \n";
									agCode += "\t[\n";
									// grab closest composite members inside the
									// region and get them closer to each other
									ArrayList<Agent> memberlist = (ArrayList<Agent>) ((Agent) act
											.getObject()).getMemberOf((Agent) act.getObject(),this.agents);

									agCode += "\t   let this-"
											+ memberlist.get(0)
													.getABMCodeLabel()
											+ " min-one-of "
											+ memberlist.get(0)
													.getABMCodeLabel()
											+ "s with [region = re and distance myself <= reaction-distance * 3 ] [ distance myself ]\n";
									int cnt = 0;
									for (Agent member : memberlist) {
										if (cnt++ == 0)
											continue;
										agCode += "\t    let closest-"+ cnt+ " min-one-of "+ member.getABMCodeLabel()
												+ "s with [region = re and distance myself <= reaction-distance * 3 ] [ distance myself ]\n";
										agCode += "\t    if closest-"	+ cnt+ " != nobody and this-"+ memberlist.get(0).getABMCodeLabel()+ " != nobody [\n";
										agCode += "\t      ask closest-"+ cnt + "\n";
										agCode += "\t            [ \n";
										agCode += "\t               facexy x y \n";
										agCode += "\t               ask patch-ahead ("+ member.getABMCodeLabel()+"-move-speed * 1.5) \n";
										agCode += "\t               [ if region = re [ ask closest-"+cnt+" [ fd ("+member.getABMCodeLabel()+"-move-speed * 1.5)]]]  \n";
										agCode += "\t            ] \n";
										agCode += "\t      ask this-"+ memberlist.get(0).getABMCodeLabel()+ "\n";
										agCode += "\t            [ \n";
										agCode += "\t               facexy x y \n";
										agCode += "\t               ask patch-ahead ("+ memberlist.get(0).getABMCodeLabel()+"-move-speed * 1.5) \n";
										agCode += "\t               [ if region = re [ ask this-"+ memberlist.get(0).getABMCodeLabel()+ " [ fd ("+memberlist.get(0).getABMCodeLabel()+"-move-speed * 1.5) ]]]  \n";
										agCode += "\t            ] \n";
										agCode += "\t    ]	\n";
									}
									agCode += "\t]\n";
								}
							}
						}

						if (act.getRelationship() == Activity.Relationship.DECREASES) {
							// case 3.2
							if (!ag.IsInActive()) {
								if (verbous)
									agCode += "\t ;; source: KAM   case 3.2 act(a(ag)) [-> or -|] a(ag2)\n";
								if (verbous)
									agCode += "\t ;; act of "
											+ ag.getABMCodeLabel()
											+ " decreases "
											+ ((Agent) act.getObject())
													.getABMCodeLabel() + "\n";
								ArrayList<Annotation> annotations = getAnnotationsFromNodes(
										act.getActivityNode(), "decreases",
										((Agent) act.getObject())
												.getAgentInfoKamNode(),
										kamStore);
								ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
								if (validRegions != null
										&& validRegions.size() > 0)
									for (Region re : validRegions) {
										agCode += "\tif random 100 < activity \n"; // AND
																					// patch
																					// hat
																					// region
																					// =
																					// re
										agCode += "\t[\n";
										agCode += "\t if region  = \""
												+ re.getABMCodeLabel() + "\"\n";
										agCode += "\t  [\n";
										agCode += "\t    ask patch-here \n";
										agCode += "\t    [\n";
										agCode += "\t        if one-of "
												+ ((Agent) act.getObject())
														.getABMCodeLabel()
												+ "s != nobody\n";
										agCode += "\t        [ \n";
										agCode += "\t          ask one-of "
												+ ((Agent) act.getObject())
														.getABMCodeLabel()
												+ "s  [ die ]    \n";
										agCode += "\t        ] \n";
										agCode += "\t    ]\n";
										agCode += "\t  ]\n";
										agCode += "\t]\n";
									}
								else {
									agCode += "\tif random 100 < activity \n";
									agCode += "\t  [\n";
									agCode += "\t      if one-of "
											+ ((Agent) act.getObject())
													.getABMCodeLabel()
											+ "s != nobody\n";
									agCode += "\t      [ \n";
									agCode += "\t        ask one-of "
											+ ((Agent) act.getObject())
													.getABMCodeLabel()
											+ "s  [ die ]    \n";
									agCode += "\t      ] \n";
									agCode += "\t  ]\n";
								}
							}
						}
						
						// case 6: a(ag2))
						//                      [-> or -|] act(a(ag))
						// /////////////////////////////////////////////////////////////////////////////////////////
						if (act.getObject().getClass().equals(Agent.class)) {
							if (act.getRelationship() == Activity.Relationship.INCREASEDBY
									&& (act.getType() == ActivityType.MOLECULAR || act.getType() == ActivityType.KINASE
									    || act.getType() == ActivityType.CATALYTIC)) {
								ArrayList<Annotation> annotations = getAnnotationsFromNodes(
										act.getActivityNode(), "increasedBy",
										((Agent) act.getObject())
												.getAgentInfoKamNode(), kamStore);
								ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
								Agent agentThatIncreases = (Agent) act.getObject();
								// case 6.1
								if (!ag.IsInActive()) { //ag is the agent that HAS the activity, not the one that increases it!
									if (verbous)
										agentThatIncreases.addToGOCode( "\t ;; source: KAM   case 6.1 a(ag) [-> or -|] act(a(ag2))\n");
									if (verbous && act.getType() == ActivityType.MOLECULAR)
										agentThatIncreases.addToGOCode( "\t ;; molecular activity of "
												+ ag.getABMCodeLabel()
												+ " increased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (verbous && act.getType() == ActivityType.KINASE)
										agentThatIncreases.addToGOCode("\t ;; kinase activity of "
												+ ag.getABMCodeLabel()
												+ " increased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (verbous && act.getType() == ActivityType.CATALYTIC)
										agentThatIncreases.addToGOCode("\t ;; catalytic activity of "
												+ ag.getABMCodeLabel()
												+ " increased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (!((Agent) act.getObject())
											.IsInActive())
										if (validRegions != null
												&& validRegions.size() > 0)
											for (Region re : validRegions) {
												agentThatIncreases.addToGOCode("\tif random 100 < activity \n"); 
												agentThatIncreases.addToGOCode("\t[\n"); 
												agentThatIncreases.addToGOCode("\t if region = \""
														+ re.getABMCodeLabel() + "\"\n"); 
												agentThatIncreases.addToGOCode("\t  [\n"); 
												agentThatIncreases.addToGOCode("\t    if min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance myself > 0][distance myself] != nobody\n"); 
												agentThatIncreases.addToGOCode("\t    [  \n"); 
												agentThatIncreases.addToGOCode("\t      ask min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance myself > 0][distance myself] \n"); 
												agentThatIncreases.addToGOCode("\t      [ \n"); 
												agentThatIncreases.addToGOCode("\t         if activity < 100 [ set activity activity + 1 ]\n"); 
												agentThatIncreases.addToGOCode("\t      ]\n"); 
												agentThatIncreases.addToGOCode("\t    ]\n"); 
												agentThatIncreases.addToGOCode("\t  ]\n"); 
												agentThatIncreases.addToGOCode("\t]\n"); 
											}
										else {
											agentThatIncreases.addToGOCode("\tif random 100 < activity \n"); 
											agentThatIncreases.addToGOCode("\t[\n"); 
											agentThatIncreases.addToGOCode("\t  if min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance myself > 0][distance myself] != nobody\n"); 
											agentThatIncreases.addToGOCode("\t  [\n"); 
											agentThatIncreases.addToGOCode("\t    ask min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance myself > 0][distance myself] \n"); 
											agentThatIncreases.addToGOCode("\t    [ \n"); 
											agentThatIncreases.addToGOCode("\t       if activity < 100 [ set activity activity + 1 ]\n"); 
											agentThatIncreases.addToGOCode("\t    ]\n"); 
											agentThatIncreases.addToGOCode("\t  ]\n"); 
											agentThatIncreases.addToGOCode("\t]\n"); 
										}
								} else {
									if (verbous)
										agentThatIncreases.addToGOCode( "\t ;; agent is inactive, activity cannot be increased\n");
								}
							}
							///6.2 decreasedBy
							if (act.getRelationship() == Activity.Relationship.DECREASEDBY
									&& (act.getType() == ActivityType.MOLECULAR || act.getType() == ActivityType.KINASE
									    || act.getType() == ActivityType.CATALYTIC)) {
								ArrayList<Annotation> annotations = getAnnotationsFromNodes(
										act.getActivityNode(), "decreasedBy",
										((Agent) act.getObject())
												.getAgentInfoKamNode(), kamStore);
								ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
								Agent agentThatIncreases = (Agent) act.getObject();
								// case 6.2
								if (!ag.IsInActive()) { //ag is the agent that HAS the activity, not the one that decreases it!
									if (verbous)
										agentThatIncreases.addToGOCode( "\t ;; source: KAM   case 6.2 a(ag) [-> or -|] act(a(ag2))\n");
									if (verbous && act.getType() == ActivityType.MOLECULAR)
										agentThatIncreases.addToGOCode( "\t ;; molecular activity of "
												+ ag.getABMCodeLabel()
												+ " decreased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (verbous && act.getType() == ActivityType.KINASE)
										agentThatIncreases.addToGOCode( "\t ;; kinase activity of "
												+ ag.getABMCodeLabel()
												+ " decreased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (verbous && act.getType() == ActivityType.CATALYTIC)
										agentThatIncreases.addToGOCode( "\t ;; catalytic activity of "
												+ ag.getABMCodeLabel()
												+ " decreased by "
												+ ((Agent) act.getObject())
														.getABMCodeLabel() + "\n");
									if (!((Agent) act.getObject())
											.IsInActive())
										if (validRegions != null
												&& validRegions.size() > 0)
											for (Region re : validRegions) {
												agentThatIncreases.addToGOCode("\tif random 100 < activity \n"); 
												agentThatIncreases.addToGOCode("\t[\n"); 
												agentThatIncreases.addToGOCode("\t if region = \""
														+ re.getABMCodeLabel() + "\"\n"); 
												agentThatIncreases.addToGOCode("\t  [\n"); 
												agentThatIncreases.addToGOCode("\t    if min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance > 0][distance myself] != nobody\n"); 
												agentThatIncreases.addToGOCode("\t    [\n"); 
												agentThatIncreases.addToGOCode("\t      ask min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distancemyself < reaction-distance and distance > 0][distance myself] \n"); 
												agentThatIncreases.addToGOCode("\t      [ \n"); 
												agentThatIncreases.addToGOCode("\t         if activity > 0 [ set activity activity - 1 ]\n"); 
												agentThatIncreases.addToGOCode("\t      ]\n"); 
												agentThatIncreases.addToGOCode("\t    ]\n"); 
												agentThatIncreases.addToGOCode("\t  ]\n"); 
												agentThatIncreases.addToGOCode("\t]\n"); 
											}
										else {
											agentThatIncreases.addToGOCode("\tif random 100 < activity \n"); 
											agentThatIncreases.addToGOCode("\t[\n"); 
											agentThatIncreases.addToGOCode("\t  if min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance > 0][distance myself] != nobody\n"); 
											agentThatIncreases.addToGOCode("\t  [\n"); 
											agentThatIncreases.addToGOCode("\t    ask min-one-of "+ag.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance and distance > 0][distance myself] \n"); 
											agentThatIncreases.addToGOCode("\t    [ \n"); 
											agentThatIncreases.addToGOCode("\t       if activity > 0 [ set activity activity - 1 ]\n"); 
											agentThatIncreases.addToGOCode("\t    ]\n"); 
											agentThatIncreases.addToGOCode("\t  ]\n"); 
											agentThatIncreases.addToGOCode("\t]\n"); 
										}
								} else {
									if (verbous)
										agentThatIncreases.addToGOCode( "\t ;; agent is inactive, activity cannot be decreased\n");
								}
							}
						}
					}

					// case 5: act(a(ag2)) [-> or -|] act(a(ag)) and case 2:
					// act(a(ag)) [-> or -|] act(a(ag2))
					// /////////////////////////////////////////////////////////////////////////////////////////
					if (act.getObject().getClass().equals(Activity.class)
							&& ag == subagent) {
						if (!ag.IsInActive()) {
							if (act.getRelationship() == Activity.Relationship.INCREASES) {
								// TODO the following probably won't work....
								ArrayList<Annotation> annotations = getAnnotationsFromNodes(
										act.getActivityNode(), "increases",
										((Activity) act.getObject())
												.getActivityNode(), kamStore);
								ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
								if (verbous)
									agCode += "\t ;; source: KAM   case 3.2 act(a(ag)) [-> or -|] a(ag2)\n";
								if (verbous)
									agCode += "\t ;; act of "
											+ ag.getABMCodeLabel()
											+ " increases "
											+ ((Activity) act.getObject())
													.getType()
											+ " activity of "
											+ ((Activity) act.getObject())
													.getActivityNode()
													.getLabel() + "\n";
								// search for agent that has this activity!
								for (Agent targetAgent : agents2) {
									for (Activity targetAct : targetAgent
											.getActivities()) {
										if (targetAct == (Activity) act
												.getObject()) {
											if (verbous)
												agCode += "\t ;; source: KAM   cases 2 / 5 act(a(ag)) ->  act(a(ag2))\n";
											if (verbous)
												agCode += "\t ;; act of "
														+ ag.getABMCodeLabel()
														+ " increases act of "
														+ targetAgent
																.getABMCodeLabel()
														+ "\n";
											agCode += "\tif random 100 < activity\n";
											agCode += "\t  [\n";
											agCode += "\t      if min-one-of "
													+ targetAgent
															.getABMCodeLabel()
													+ "s with [region = re] [distance myself] != nobody\n";
											agCode += "\t      [ \n";
											agCode += "\t       let closest min-one-of "
													+ targetAgent
															.getABMCodeLabel()
													+ "s with [region = re] [distance myself]\n";
											agCode += "\t      if distance closest < reaction-distance \n";
											agCode += "\t      [ \n";
											if (!targetAgent.IsInActive()) {
												agCode += "\t        ask closest\n";
												agCode += "\t        [ \n";
												agCode += "\t          if activity < 100\n";
												agCode += "\t          [\n";
												agCode += "\t            set activity activity + 1\n";
												agCode += "\t          ]\n";
												agCode += "\t         ]\n";
											}
											agCode += "\t       ]    \n";
											agCode += "\t      ] \n";
											agCode += "\t  ]\n";
										}
									}
								}
							}
						}
						if (act.getRelationship() == Activity.Relationship.DECREASES) {
							// TODO the following probably won't work....
							ArrayList<Annotation> annotations = getAnnotationsFromNodes(
									act.getActivityNode(), "decreases",
									((Activity) act.getObject())
											.getActivityNode(), kamStore);
							ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
							// search for agent that has this activity!
							if (!ag.IsInActive()) {
								for (Agent targetAgent : agents2) {
									for (Activity targetAct : targetAgent
											.getActivities()) {
										if (targetAct == (Activity) act
												.getObject()) {
											if (verbous)
												agCode += "\t ;; source: KAM   cases 2 / 5 act(a(ag)) or -| a(ag2)\n";
											if (verbous)
												agCode += "\t ;; act of "
														+ ag.getABMCodeLabel()
														+ " decreases act of "
														+ targetAgent
																.getABMCodeLabel()
														+ "\n";
											if (validRegions != null
													&& validRegions.size() > 0)
												for (Region re : validRegions) {
													agCode += "\tif random 100 < activity \n"; // AND
																								// patch
																								// hat
																								// region
																								// =
																								// re
													agCode += "\t[\n";
													agCode += "\t if region = \""
															+ re.getABMCodeLabel()
															+ "\"\n";
													agCode += "\t  [\n";
													agCode += "\t    ask patch-here \n";
													agCode += "\t    [\n";
													agCode += "\t        if one-of "
															+ targetAgent
																	.getABMCodeLabel()
															+ "s != nobody\n";
													agCode += "\t        [ \n";
													if (!targetAgent
															.IsInActive())
														agCode += "\t          ask one-of "
																+ targetAgent
																		.getABMCodeLabel()
																+ "s  [ set activity activity - 1 ]    \n";
													agCode += "\t        ] \n";
													agCode += "\t    ] \n";
													agCode += "\t  ]\n";
													agCode += "\t]\n";
												}
											else {
												agCode += "\tif random 100 < activity \n";
												agCode += "\t  [\n";
												agCode += "\t      if one-of "
														+ targetAgent
																.getABMCodeLabel()
														+ "s != nobody\n";
												agCode += "\t      [ \n";
												if (!targetAgent.IsInActive())
													agCode += "\t        ask one-of "
															+ targetAgent
																	.getABMCodeLabel()
															+ "s  [ set activity activity - 1 ]    \n";
												agCode += "\t      ] \n";
												agCode += "\t  ]\n";
											}
										}
									}
								}
							}
						}
					}
				}

				subagent.addToGOCode(agCode);
				agCode = "";
			}

		}

		// new iteration of agents to write their code
		String code = "\n";
		if (verbous)
			code += " ;; doGo()\n";
		code += "to go";
		addToCode(code);
		code = "";
		addToCode("ask turtles [ set label-color black set label \"\"]");
		addToCode("ask turtles [ file-write ticks file-write who file-write breed  file-write xcor file-write ycor file-write energy file-write activity file-print \"\"] ");
		if (verbous)
			addToCode(";;to close and view the file, type file-close in the console\n ");

		ArrayList<Agent> pulseAgents = new ArrayList<Agent>();
		if (this.agentPulses.length > 1) {
			OntClass developmentalProcessClass = Ontology
					.getOntClassFromURIString(this.agents.get(0)
							.getAgentInfoOntClass().getOntModel(),
							this.agentPulses[1]);
			OntProperty pulseProp = Utils.getObjectPropFromURIString(
					this.agents.get(0).getAgentInfoOntClass().getOntModel(),
					this.agentPulses[0]);
			for (Agent nextagent : this.agents) {
				if (Utils.hasRestriction(nextagent.getAgentInfoOntClass(),
						pulseProp)) {
					OntClass obj = Utils.getRestrictionValue(
							nextagent.getAgentInfoOntClass(), pulseProp);
					if (developmentalProcessClass.getURI().equals(obj.getURI())
							|| Ontology.hasSubClass(developmentalProcessClass,
									obj))
						pulseAgents.add(nextagent);
					Utils.appendToFile(
							new StringBuffer().append("-> Agent "
									+ nextagent.getABMCodeLabel()
									+ " periodically inserted via pulses.  \n"),
							logging);
				}
			}
		}

		addToCode(insertAgents(pulseAgents, true, false));
		addToCode("\n ask turtles [\n");
		
		for (Agent ag : agents2) {
			if (!ag.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
				addToCode("\n if breed = " + ag.getABMCodeLabel() + "s [\n");
				addToCode("\tlet c color");
				addToCode("\tlet s size");
				addToCode("\tlet re region");
				addToCode("\tlet re_closest re");
				addToCode("\tlet x xcor");
				addToCode("\tlet y ycor");
				addToCode("\tlet form_composite 0");
				addToCode("\tlet a0 no-turtles   ;; found no way to check whether a variable has already been initiliazed ;( ");
				addToCode("\tlet a1 no-turtles    ");
				addToCode("\tlet a2 no-turtles");
				addToCode( "\tlet a3 no-turtles");
				addToCode("\tlet a4 no-turtles");
				addToCode("\tlet a5 no-turtles");
				addToCode("\tlet a6 no-turtles");
				addToCode("\tlet a7 no-turtles");
				addToCode("\tlet a8 no-turtles");
				addToCode("\tlet a9 no-turtles");
				addToCode("\tlet a10 no-turtles");
				addToCode("\tlet newenergy 0");
				if (ag.getCurrentEventNumber()>0)
					addToCode("\tlet bindingcheck random "+ag.getCurrentEventNumber());  //this is the last, ie highest, of the event numbers

				addToCode(ag.getGOCode());
				if (ag.isComposite())
					addToCode("\tmove-inside-composite-" + ag.getABMCodeLabel());
				else{
					addToCode("\t  move-inside "+ ag.getABMCodeLabel()+"-move-speed");
				}
				
				//labels of composites
				if (ag.isComposite() && ag.getIncludes().size() == 2){
					addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"n:1\" "
							+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies ]]");
				}
				if (ag.isComposite() && ag.getIncludes().size() == 2){
					if (ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
						addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"1:n\" "
								+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies ]]");
					else
						addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"1:n\" "
								+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies ]]");
					}
				
				//labels of complexes
				if (ag.isComplex() && ag.getHasMember().size() == 2){
					addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"n:1\" "
							+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies ]]");
				}
				if (ag.isComplex() && ag.getHasMember().size() == 2){
					if (ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel()))
						addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"1:n\" "
								+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies ]]");
					else
						addToCode( "\tif "+ag.getABMCodeLabel()+"_labels? and "+ag.getABMCodeLabel()+"_members = \"1:n\" "
								+ "[ ask "+ag.getABMCodeLabel()+"s [ set label length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies ]]");
					}
				
				if (ag.hasQualPropValues()) {
					for (QualitativeProperty qp : ag.getQualPropValues()) {
						if (qp.getQuality().getURI().equals(this.reproduceURI)) {
							addToCode("\treproduce-" + ag.getABMCodeLabel()
									+ " 1\n");
						}
					}
				}
				if (ag.isHomeostaticControl())
					addToCode("\tdeath-homeostatic-" + ag.getABMCodeLabel()
							+ " ]");
				else if (ag.isComposite() || ag.isComplex())
					addToCode("\tdeath-" + ag.getABMCodeLabel()+" ]");
				else
					addToCode("\tdeath ]");
			}
		}
		addToCode(" ] ;; end of ask turtles\n");
		code = "\tdo-plotting\n\ttick\n";
		code += "end\n";
		addToCode(code);
	}

	private String increaseCompositeMemberCode(Agent ag) {
		String agCode= "";
		if (ag.getIncludes().size()==2){
			agCode += "\tif "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_members = \"1:n\"\n";
			agCode += "\t  [\n";
			agCode += " ;; " + ag.getABMCodeLabel()+" can be in 1:n relation with "+ag.getIncludes().get(1).getABMCodeLabel()+"\n";
			agCode += "\t     if min-one-of "+ ag.getIncludes().get(1).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] != nobody \n";
			agCode += "\t     [\n";
			if (!ag.IsInActive()) {
				agCode += "\t         if  random 100 < activity \n";
			} else
				agCode += "\t         if  random 100 < 50 \n";
			agCode += "\t         [    \n";
			if (ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
				agCode += "\t           if length "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies < "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_maxn \n"; 
			else
				agCode += "\t           if length "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies < "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_maxn \n";
			agCode += "\t           [ \n";
			agCode += "\t             let new 0    \n";
			agCode += "\t            ask min-one-of "+ ag.getIncludes().get(1).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] [set new energy die]\n";
			if (ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
				agCode += "\t            set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies fput new "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies;; add the energy of the new member to the list\n";
			else
				agCode += "\t            set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies fput new "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies;; add the energy of the new member to the list\n";
			agCode += "\t            ;;if activity < 100 [ set activity activity + 1 ]   \n";
			agCode += "\t         ]   \n";
			agCode += "\t        ] \n";
			agCode += "\t      ]   \n";
			agCode += "\t  ]   \n";
			// else
			agCode += "\tif "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_members = \"n:1\"\n";
			agCode += "\t  [\n";
			agCode += " ;; " + ag.getABMCodeLabel()+" can be in n:1 relation with "+ag.getIncludes().get(0).getABMCodeLabel()+"\n";
			agCode += "\t     if min-one-of "+ ag.getIncludes().get(0).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] != nobody \n";
			agCode += "\t     [\n";
			if (!ag.IsInActive()) {
				agCode += "\t         if  random 100 < activity \n";
			} else
				agCode += "\t         if  random 100 < 50 \n";
			agCode += "\t         [    \n";
			if (ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
				agCode += "\t           if length "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s2_energies < "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_maxn \n"; 
			else
				agCode += "\t           if length "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies < "+ag.getIncludes().get(0).getABMCodeLabel()+"."+ag.getIncludes().get(1).getABMCodeLabel()+"_maxn \n";
			agCode += "\t           [ \n";
			agCode += "\t             let new 0    \n";
			agCode += "\t            ask min-one-of "+ ag.getIncludes().get(0).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] [set new energy die]\n";
			agCode += "\t            set "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput new "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies;;add the energy of the new member to the list\n";
			agCode += "\t            ;;if activity < 100 [ set activity activity + 1 ]   \n";
			agCode += "\t         ]   \n";
			agCode += "\t        ] \n";
			agCode += "\t      ]   \n";
			agCode += "\t  ]   \n";
		}
		return agCode;
	}

	private String increaseComplexMemberCode(Agent ag) {
		String agCode= "";
		if (ag.getHasMember().size()==2){
			agCode += "\tif "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_members = \"1:n\"\n";
			agCode += "\t  [\n";
			agCode += " ;; " + ag.getABMCodeLabel()+" can be in 1:n relation with "+ag.getHasMember().get(1).getABMCodeLabel()+"\n";
			agCode += "\t     if min-one-of "+ ag.getHasMember().get(1).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] != nobody \n";
			agCode += "\t     [\n";
			if (!ag.IsInActive()) {
				agCode += "\t         if  random 100 < activity  \n";
			} else
				agCode += "\t         if  random 100 < 50 \n";
			agCode += "\t         [    \n";
			if (ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel()))
				agCode += "\t           if length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies < "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_maxn \n"; 
			else
				agCode += "\t           if length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies < "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_maxn \n";
			agCode += "\t           [ \n";
			agCode += "\t             let new 0  \n";
			agCode += "\t             ask min-one-of "+ ag.getHasMember().get(1).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] [set new energy die]\n";
			if (ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel())){
				agCode += "\t             set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies fput new "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies;; add the energy of the new member to the list\n";
				if (ag.isAllostericEnzyme(allostericEnzymeURI)){
					agCode += "\t             ;;"+ag.getABMCodeLabel()+" is an allosteric enzyme  \n"; 
					agCode += "\t             if length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies > 1 [ set activity activity + ( 50 / ( "+ag.getABMCodeLabel()+"_maxn - 1)  ) ]  \n";
				}
			}
			else{
				agCode += "\t             set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies fput new "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies;; add the energy of the new member to the list\n";
				if (ag.isAllostericEnzyme(allostericEnzymeURI)){
					agCode += "\t             ;;"+ag.getABMCodeLabel()+" is an allosteric enzyme  \n";
					agCode += "\t             if length "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies > 1 [ set activity activity + ( 50 / ( "+ag.getABMCodeLabel()+"_maxn - 1)  ) ]  \n";
				}
			}
			agCode += "\t           ]   \n";
			agCode += "\t         ] \n";
			agCode += "\t      ]   \n";
			agCode += "\t  ]   \n";
			// else
			agCode += "\tif "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_members = \"n:1\"\n";
			agCode += "\t  [\n";
			agCode += " ;; " + ag.getABMCodeLabel()+" can be in n:1 relation with "+ag.getHasMember().get(0).getABMCodeLabel()+"\n";
			agCode += "\t     if min-one-of "+ ag.getHasMember().get(0).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] != nobody \n";
			agCode += "\t     [\n";
			if (!ag.IsInActive()) {
				agCode += "\t         if  random 100 < activity \n";
			} else
				agCode += "\t         if  random 100 < 50 \n";
			agCode += "\t         [    \n";
			if (ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel()))
				agCode += "\t           if length "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s2_energies < "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_maxn \n"; 
			else
				agCode += "\t           if length "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies < "+ag.getHasMember().get(0).getABMCodeLabel()+"."+ag.getHasMember().get(1).getABMCodeLabel()+"_maxn \n";
			agCode += "\t           [ \n";
			agCode += "\t             let new 0  \n";
			agCode += "\t             ask min-one-of "+ ag.getHasMember().get(0).getABMCodeLabel()+ "s  "
					+ "with [distance myself > 0 and distance myself < reaction-distance and region = re][distance myself] [set new energy die]\n";
			agCode += "\t             set "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput new "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies;; add the energy of the new member to the list\n";
			if (ag.isAllostericEnzyme(allostericEnzymeURI)){
				agCode += "\t             ;;"+ag.getABMCodeLabel()+" is an allosteric enzyme  \n";
				agCode += "\t             if length "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies > 1 [ set activity activity + ( 50 / ( "+ag.getABMCodeLabel()+"_maxn - 1)  ) ]  \n";
			}
			agCode += "\t           ]   \n";
			agCode += "\t         ] \n";
			agCode += "\t      ]   \n";
			agCode += "\t  ]   \n";
		}
		return agCode;
	}

	private String reactionParticipationCode(Agent ag, Reaction r, KamStore kamStore) {
		String code = "";
		if (verbous)
			code += "\t;;code source: KAM : agent participates in reaction\n";
		if (verbous){
			code += "\t;;" + ag.getABMCodeLabel() + " participates in reaction ";
			for (Agent react : r.getReactants())
				code += react.getABMCodeLabel()+" ";
			code +=  "with products ";
			for (Agent prod : r.getProducts())
				code += prod.getABMCodeLabel()+" ";
		}
		code += "\n";
		Utils.appendToFile(
				new StringBuffer().append("\n"+ag.getABMCodeLabel() + " participates in reaction "+r.getID()+" "
						+ r.getReactants() + " with products "+ r.getProducts()+"\n"), logging);
		code += "\tset form_composite 1  ;;borrow this variable to flag whether to run the reaction or not\n ";
		code += "\tifelse ";
		for (Agent react : r.getReactants()){
			code += " min-one-of "+react.getABMCodeLabel()+"s ";
			code += "with [region = re and distance myself < reaction-distance][distance myself] != nobody and ";
		}
		code = code.substring(0, code.length()-4);  //cut off the last "and"
		code += "  []\n ";
		code += "\t  [set form_composite 0] \n ";
		//only if all reactants are close enough will the reaction take place
		code += "\tif form_composite = 1 and random 100 < activity  and bindingcheck = "+ ag.getCurrentEventNumber()+" [ ;;dependent on activity, otherwise reactions with only 1 reactant will always and immediately decompose the reactant\n";

		///////reactants//////////////////////////////////////////////
		
		boolean agentDie = false;
		Agent enzymeAgent = null;
		
		for (Agent react : r.getReactants()){
			//TODO I assume there will be only 1 enzyme involved, which may be problematic
			enzymeAgent = react.getEnzymeMember(enzymeURI);
			
			if (!react.getABMCodeLabel().equals(ag.getABMCodeLabel())){
				if (!r.getProducts().contains(react) && !r.getProducts().contains(enzymeAgent))
					code += "\t  ask min-one-of "+react.getABMCodeLabel()+"s with [distance myself < reaction-distance and region = re][distance myself] [die] \n ";
				if (r.getProducts().contains(enzymeAgent) && enzymeAgent.isAllostericEnzyme(allostericEnzymeURI)){
					Agent nmaxAgent;
					if (react.getHasMember().get(0) != enzymeAgent)
						nmaxAgent = react.getHasMember().get(0);
					else
						nmaxAgent = react.getHasMember().get(1);
					code += "\t  ;;"+enzymeAgent.getABMCodeLabel()+" is an allosteric enzyme that catalyzes the reaction\n ";
					code += "\t  ask min-one-of "+react.getABMCodeLabel()+"s with [distance myself < reaction-distance and region = re][distance myself]  \n ";
					code += "\t  [  \n ";
					code += "\t    if "+react.getABMCodeLabel()+"_maxn > 1 \n ";
					code += "\t    [ \n ";
					code += "\t       set "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies but-last "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies \n ";
					code += "\t       if  length "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies > 0  \n ";
					code += "\t       [  \n ";
					code += "\t         set activity activity - ( 50 / ( "+react.getABMCodeLabel()+"_maxn - 1)  )  \n ";
					code += "\t       ] \n ";
					code += "\t    ] \n ";
					code += "\t  ]  \n ";
					
				}
				//skip the rest because this goes into the GO code of ag
				continue;
			}
			
			//ab hier   react == ag
			
			//check if the reactant is an (allosteric) enzyme with molecules bound to it
			if (react.isComplex() && react.hasEnzymeAsMember(enzymeURI))
			{
				//if so, then check whether this enzyme is contained in the products list
				if (enzymeAgent!=null && r.getProducts().contains(enzymeAgent)){
					if (enzymeAgent.isAllostericEnzyme(allostericEnzymeURI)){
						Agent nmaxAgent;
						if (react.getHasMember().get(0) != enzymeAgent)
							nmaxAgent = react.getHasMember().get(0);
						else
							nmaxAgent = react.getHasMember().get(1);
						//if 1:n or n:1, then I need to check how many molecules are bount to the enzyme, and change the enzyme's activity
						code += "\t  ;;"+enzymeAgent.getABMCodeLabel()+" is an allosteric enzyme that catalyzes the reaction\n ";
						code += "\t  if "+ag.getABMCodeLabel()+"_maxn > 1  \n ";
						code += "\t  [ \n "; 
						code += "\t     set "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies but-last "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies \n ";
						code += "\t     if length "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies > 1 \n ";
						code += "\t     [\n ";
						code += "\t       set activity activity - ( 50 / ( "+ag.getABMCodeLabel()+"_maxn - 1)  )  \n ";
						code += "\t     ] \n ";
						code += "\t  ] \n ";
					}
				} 
			} 
			
			if (   (!r.getProducts().contains(react) && !r.getProducts().contains(enzymeAgent)) 
					|| (!r.getProducts().contains(react) && r.getProducts().contains(enzymeAgent) && !enzymeAgent.isAllostericEnzyme(allostericEnzymeURI)) ){
				agentDie = true;  //otherwise agent is passed through the reaction
			}
			ag.setCurrentEventNumber(ag.getCurrentEventNumber() + 1);
		}

		///////products//////////////////////////////////////////////7
		code += "\t;;   products of the reaction:\n";	
		ArrayList <Agent> productsDoneWith = new ArrayList <Agent>();
		for (Agent prod : r.getProducts()){
			if (productsDoneWith.contains(prod))
				continue;
			productsDoneWith.add(prod);
			//check if the agent to be hatched is a complex himself (then it needs energy lists)
			//but don't create a new allosteric enzyme if it is passed through the reaction with molecules bount to it (2. row)
			if (!r.getReactants().contains(prod) && prod.isComplex() && prod.getHasMember().size() == 2 ){
				String energylist2="";
				if (prod.getHasMember().get(0).getABMCodeLabel().equals(prod.getHasMember().get(1).getABMCodeLabel()))
					energylist2 = prod.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies";
				else
					energylist2 = prod.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies";
				//im Fall eines allosterischen enzyms, das folgende nur, wenn vor der reaktion nur 1 Molekül gebunden war!
				if (enzymeAgent!=null && prod.getABMCodeLabel().equals(enzymeAgent.getABMCodeLabel()) 
						&& enzymeAgent.isAllostericEnzyme(allostericEnzymeURI) && !prod.memberOf(enzymeAgent) ) {
					Agent nmaxAgent;
					if (ag.getHasMember().get(0) != enzymeAgent)
						nmaxAgent = ag.getHasMember().get(0);
					else
						nmaxAgent = ag.getHasMember().get(1);
					code += "\t  ;; don't create new allosteric enzyme if the current one won't die during the reaction\n ";
					code += "\t  if "+ag.getABMCodeLabel()+"_maxn > 1 and length "+nmaxAgent.getABMCodeLabel().toLowerCase()+"s_energies"+" < 1 [\n ";
					code += "\t "+hatchagent(prod, 1, prod.getColorString(), "random 100", 
						prod.getSize(), "random (2 * lifespan-"+ prod.getABMCodeLabel()+")", "reaction-distance", 
						"random (2 * lifespan-"+prod.getHasMember().get(0).getABMCodeLabel()+")", 
						"random (2 * lifespan-"+prod.getHasMember().get(1).getABMCodeLabel()+")", 
						energylist2)+" \n";
					code += "\t  ]\n";
				}
			}
			//if product is not a complex
			//   if product is contained more than once in the product list and also contained in the reactants, it needs to be created numberOfOccurences - 1 times
			//   (with the remaining 1 passed through the reaction)
			String allProductsString ="" ;
			for (Agent newProd : r.getProducts()){
				allProductsString += newProd.getABMCodeLabel()+" ";
			}
			String allReactantsString ="" ;
			for (Agent newReact : r.getReactants()){
				allReactantsString += newReact.getABMCodeLabel()+" ";
			}
			int prodOccurrence = Utils.getNumberOfOccurrences(prod.getABMCodeLabel(), allProductsString);
			int occurrenceProductInReactants = Utils.getNumberOfOccurrences(prod.getABMCodeLabel(), allReactantsString);
			
			if ((!r.getReactants().contains(prod) || (r.getReactants().contains(prod) && prodOccurrence > occurrenceProductInReactants) ) 
					&& !prod.isComplex()){
					code += "\t"+hatchagent(prod, 1, prod.getColorString(), "random 100", prod.getSize(), 
					"random (2 * lifespan-"+ prod.getABMCodeLabel()+")", "reaction-distance", "0", "0", "null")+" \n ";
			}
		}

		//check if the reaction increases or decreases anything  
		for (int i = 0; i < r.getIncreases().size(); i++){
			Object o = r.getIncreases().get(i);
			if (o.getClass().equals(Agent.class)){
				code += "  ;; reaction increases agent " + ((Agent)o).getABMCodeLabel()+"\n";
				code += "  if random 100 < activity [ "+hatchagent((Agent) o, 1, ((Agent) o).getColorString(), 
						"random 100", ((Agent) o).getSize(), "random (2 * lifespan-"+((Agent) o).getABMCodeLabel(), null, null, null, null)+" ] \n";
			}
			if (o.getClass().equals(BioProcess.class)){
				code += "  ;; reaction increases bioprocess " + ((BioProcess)o).getABMCodeLabel()+"\n";
				code += generateCodeIncreasesBioProcess(ag, (BioProcess)o, kamStore);
			}
			if (o.getClass().equals(Activity.class)){
				code += "  ;; reaction increases activity ";
				//find the agent that has the activity
				Agent a = getAgent((Activity)o);
				code += "of agent "+a.getABMCodeLabel()+"\n";
				if (!a.IsInActive()){
				  code += "  if min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] != nobody [\n"; 
				  code += "    ask min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] \n"; 
				  code += "\t      [ \n"; 
				  code += "\t         if activity < 100 [ set activity activity + 1 ]\n"; 
				  code += "\t      ]\n";
				  code += "\t]\n";
				}
			}
		}
		
		for (int i = 0; i < r.getDecreases().size(); i++){
			Object o = r.getDecreases().get(i);
			if (o.getClass().equals(Agent.class)){
				code += "  ;; reaction decreases agent " + ((Agent)o).getABMCodeLabel()+"\n";
				Agent a = (Agent)o;
				code += " if min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] != nobody [\n";
				code += "    ask min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] \n"; 
				code += "\t      [ \n"; 
				code += "\t         die ]\n"; 
				code += "\t      ]\n";
				code += "\t  ]\n";
			}
			if (o.getClass().equals(BioProcess.class)){
				//TODO no implementation yet
			}
			if (o.getClass().equals(Activity.class)){
				code += "  ;; reaction decreases activity ";
				//find the agent that has the activity
				Agent a = getAgent((Activity)o);
				code += "of agent "+a.getABMCodeLabel()+"\n";
				if (!a.IsInActive()){
				  code += "  if min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] != nobody [\n";
				  code += "    ask min-one-of "+a.getABMCodeLabel()+"s with [region = re and distance myself < reaction-distance][distance myself] \n"; 
				  code += "\t      [ \n"; 
				  code += "\t         if activity > 0 [ set activity activity - 1 ]\n"; 
				  code += "\t      ]\n";
				  code += "\t  ]\n";
				}
			}
		}
		if (agentDie){
			code += "\t   die  ;;agent itself also needs to die \n ";
		} 
		
		code += "\t] \n ";

		return code;
	}

	
	/**
	 * iterates all agents and returns the agent that has the specific activity
	 *   remark: an activity  cannot be shared among more than 1 agent
	 *           thus, no need to return a list of agents, just one!
	 * @param o
	 * @return
	 */
	private Agent getAgent(Activity o) {
		for (Agent a : agents){
			for (Activity act : a.getActivities()){
				if (act.equals(o))
					return a;
			}
		}
		return null;
	}

	/**
	 * when the agents that the complex is composed of get close enough, a complex is formed and the single agents die
	 * @param ag (complex agent)
	 * @param ag2  this is the agent into whose go code the code snippet will go
	 * @return
	 */
	private String complexFormationCode(Agent ag, Agent ag2) {
		if (ag.getHasMember().size() < 2) {
			return "";
		}
		Agent a0 = ag.getHasMember().get(0);
		//check if all other agents are close enough to a0, if so, form a commplex
		
		String c = " ;; " + a0.getABMCodeLabel()
				+ " forms a functional entity (complex) with ";
		int cnt = 0;
		for (Agent a1 : ag.getHasMember()) {
			if (cnt > 0)
				c += a1.getABMCodeLabel() + "   ";
			cnt++;
		}
		c += "\n";
		cnt = 0;
		for (Agent a1 : ag.getHasMember()) {
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			c += "\tset a"+cnt+" min-one-of "+ a1.getABMCodeLabel() + "s  with [distance myself > 0 and distance myself < reaction-distance and region = re and random 100 < activity][distance myself] \n";
			cnt++;
		}
		cnt = 0;
		for (@SuppressWarnings("unused") Agent a1 : ag.getHasMember()){
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			if (cnt ==1)
				c += "\tif a" + cnt + " != nobody and bindingcheck = "+ ag2.getCurrentEventNumber() ;  
			if (cnt >1)
				c += " and a" + cnt + " != nobody";
			cnt++;
		}
		ag2.setCurrentEventNumber(ag2.getCurrentEventNumber() + 1);
		cnt = 0;
		c += "\t   [ \n";
		if (!a0.IsInActive()) {
			c += "\t         if  random 100 < activity \n";
		} else
			c += "\t         if  random 100 < 50 ;;agent without any activity -> throw a dice\n";
		c += "\t         [    \n";
		for (Agent a1 : ag.getHasMember()){
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			c += "\t           ask a"+cnt+" [  set newenergy energy ]  ;; the complex agent will get the higher one of the energies  \n";
			cnt++;
		}
		cnt = 0;	
		c += "\t         ifelse energy > newenergy    \n";
		c += "\t         [   \n";
		if (a0.getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel()))
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "energy", "reaction-distance", "energy", "newenergy", ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies")  //first agent itself, then closest
				+ "\n";
		else
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "energy", "reaction-distance", "energy", "newenergy", ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies")  //first agent itself, then closest
				+ "\n";
		c += "\t         ]   \n";
		c += "\t         [   \n";
		if (a0.getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel()))
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "newenergy", "reaction-distance", "energy", "newenergy", ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies")  //first agent itself, then closest
				+ "\n";
		else
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "newenergy", "reaction-distance", "energy", "newenergy", ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies")  //first agent itself, then closest
				+ "\n";
		c += "\t         ]   \n";
		
		
		//then the single members of the complex all need to die
		for (Agent a1 : ag.getHasMember()){
			if (cnt == 0){
				cnt++;
				continue;
			}
			c += "\t           ask a"+cnt+" [ die ]    \n";
			cnt++;
		}
		c += "\t           die     \n";
		c += "\t         ]    \n";
		c += "\t   ] \n";
			
		return c;
	}

	/**
	 * when the agents that the composite is composed of get close enough, a composite is formed and the single agents die
	 * @param ag (composite agent)
	 * @param ag2  this is the agent into whose go code the code snippet will go
	 * @return
	 */
	private String compositeFormationCode(Agent ag, Agent ag2) {
		if (ag.getIncludes().size() < 2) {
			return "";
		}
		Agent a0 = ag.getIncludes().get(0);
		//check if all other agents are close enough to a0, if so, form a composite
		
		String c = " ;; " + a0.getABMCodeLabel()
				+ " forms a functional entity (composite) with ";
		int cnt = 0;
		for (Agent a1 : ag.getIncludes()) {
			if (cnt > 0)
				c += a1.getABMCodeLabel() + "   ";
			cnt++;
		}
		c += "\n";
		cnt = 0;
		for (Agent a1 : ag.getIncludes()) {
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			c += "\tset a"+cnt+" min-one-of "+ a1.getABMCodeLabel() + "s  with [distance myself > 0 and distance myself < reaction-distance and region = re and random 100 < activity][distance myself] \n";
			cnt++;
		}
		cnt = 0;
		for (@SuppressWarnings("unused") Agent a1 : ag.getIncludes()){
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			if (cnt ==1)
				c += "\tif a" + cnt + " != nobody and bindingcheck = "+ ag2.getCurrentEventNumber() ;  
			if (cnt >1)
				c += " and a" + cnt + " != nobody";
			cnt++;
		}
		ag2.setCurrentEventNumber(ag2.getCurrentEventNumber() + 1);
		cnt = 0;
		c += "\t   [ \n";
		if (!a0.IsInActive()) {
			c += "\t         if  random 100 < activity \n";
		} else
			c += "\t         if  random 100 < 50 ;;agent without any activity -> throw a dice\n";
		c += "\t         [    \n";
		for (Agent a1 : ag.getIncludes()){
			if (cnt == 0){
				cnt++;
				continue;  //jump to next agent because this is a0
			}
			c += "\t           ask a"+cnt+" [  set newenergy energy ]  ;; the complex agent will get the higher one of the energies  \n";
			cnt++;
		}
		cnt = 0;	
		c += "\t         ifelse energy > newenergy    \n";
		c += "\t         [   \n";
		if (a0.getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "energy", "reaction-distance", "energy", "newenergy", ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies")  //first agent itself, then closest
				+ "\n";
		else
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "energy", "reaction-distance", "energy", "newenergy", ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies")  //first agent itself, then closest
				+ "\n";
		c += "\t         ]   \n";
		c += "\t         [   \n";
		if (a0.getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel()))
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "newenergy", "reaction-distance", "energy", "newenergy", ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies")  //first agent itself, then closest
				+ "\n";
		else
			c += hatchagent(ag, 1, ag.getColorString(), "random 100",
				ag.getSize(), "newenergy", "reaction-distance", "energy", "newenergy", ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies")  //first agent itself, then closest
				+ "\n";
		c += "\t         ]   \n";
		
		
		//then the single members of the composite all need to die
		for (Agent a1 : ag.getIncludes()){
			if (cnt == 0){
				cnt++;
				continue;
			}
			c += "\t           ask a"+cnt+" [ die ]    \n";
			cnt++;
		}
		c += "\t           die     \n";
		c += "\t         ]    \n";
		c += "\t   ] \n";
			
		return c;
	}

	/**
	 * for implementation check generateTlocCode(Agent ag) below
	 * 
	 * @param bp
	 * @return
	 */
	private String generateTlocCode(BioProcess bp) {
		String bpCode = "";
		// ////////////////////////////////////////////////////
		// tloc for processes

		// Set<KamEdge> edges =
		// bp.getProcessInfoKamNode().getKam().getAdjacentEdges(bp.getProcessInfoKamNode().getKam().findNode(bp.getBELIdLabel()));
		Set<KamEdge> edges = bp.getProcessInfoKamNode().getKam()
				.getAdjacentEdges(bp.getProcessInfoKamNode());
		// check if the target node is a translocation node
		for (KamEdge thisedge : edges) {
			if (thisedge.getTargetNode().getFunctionType() == FunctionEnum.TRANSLOCATION) {
				// go through all agents to find this translocation
				Translocation tl = Utils.getTranslocation(
						thisedge.getTargetNode(), this.agents);
				// process -> or -| translocation()
				BioProcess bpSource = Utils.getBioProcessByNode(
						thisedge.getSourceNode(), this.bioProcesses);
				if ((thisedge.getRelationshipType() == RelationshipType.INCREASES || thisedge
						.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
						&& bpSource != null
						&& Utils.isBioProcess(bpSource.getProcessInfoOntClass()
								.get(0), this.bioProcesses)) {
					// bioprocess -> tloc
					if (verbous)
						bpCode += "\t;;code source: KAM : bioprocess -> tloc\n";
					if (verbous)
						bpCode += "\t;;" + bp.getABMCodeLabel()
								+ " increases tloc from "
								+ tl.getFrom().getABMCodeLabel() + " to "
								+ tl.getTo().getABMCodeLabel() + "\n";
					Utils.appendToFile(
							new StringBuffer().append("\t translocation: "
									+ bp.getABMCodeLabel()
									+ " increases tloc from "
									+ tl.getFrom().getABMCodeLabel() + " to "
									+ tl.getTo().getABMCodeLabel() + "\n"),
							logging);
					if (tl.noFurtherIncreasesOrDecreases()) {
						bpCode += "\tmove-tloc " + "\""
								+ tl.getFrom().getABMCodeLabel() + "\" " + "\""
								+ tl.getTo().getABMCodeLabel()
								+ "\" \"null\" \"null\" \"null\" \"null\"\n";
					} else // move-tloc [fromregion toregion relation
							// whatprocess whatagent whatagentactivity]
					{
						String relation = "\"null\"";
						String whatprocess = "\"null\"";
						String whatagent = "\"null\"";
						String whatagentactivity = "\"null\"";
						if (tl.getIncreasesAgent() != null) {
							if (verbous)
								bpCode += "\t;;and tloc -> abundance(agent)\n";
							whatagent = tl.getIncreasesAgent()
									.getABMCodeLabel();
							bpCode += "\tmove-tloc " + "\""
									+ tl.getFrom().getABMCodeLabel() + "\" "
									+ "\"" + tl.getTo().getABMCodeLabel()
									+ "\"" + "\"" + relation + "\" "
									+ "\"null\" \"" + whatagent + "\" \"null\""
									+ "\n";
						}
						if (tl.getIncreasesBioProcess() != null) {
							if (verbous)
								bpCode += "\t;;and tloc -> bioprocess\n";
							whatprocess = tl.getIncreasesBioProcess()
									.getABMCodeLabel();
							bpCode += "\tmove-tloc " + "\""
									+ tl.getFrom().getABMCodeLabel() + "\" "
									+ "\"" + tl.getTo().getABMCodeLabel()
									+ "\"" + "\"" + relation + "\" " + "\""
									+ whatprocess + "\" \"null\" \"null\""
									+ "\n";
						}
						if (tl.getIncreasesAgentActivity() != null) {
							// I need the ABMCodeLabel of the agent......
							for (Agent a : this.agents) {
								for (Activity acti : a.getActivities()) {
									if (tl.getIncreasesAgentActivity() == acti) {
										if (verbous)
											bpCode += "\t;;and tloc -> act(abundance(agent))\n";
										whatagentactivity = a.getABMCodeLabel();
										bpCode += "\tmove-tloc "
												+ "\""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" " + "\""
												+ tl.getTo().getABMCodeLabel()
												+ "\"" + "\"" + relation
												+ "\" \"null\" \"null\" \""
												+ whatagentactivity + "\" "
												+ "\n";
									}
								}
							}
						}
						if (tl.getDecreasesAgent() != null) {
							if (verbous)
								bpCode += "\t;;and tloc -| abundance(agent)\n";
							whatagent = tl.getDecreasesAgent()
									.getABMCodeLabel();
							bpCode += "\tmove-tloc " + "\""
									+ tl.getFrom().getABMCodeLabel() + "\" "
									+ "\"" + tl.getTo().getABMCodeLabel()
									+ "\"" + "\"" + relation + "\" "
									+ "\"null\" \"" + whatagent + "\" \"null\""
									+ "\n";
						}
						if (tl.getDecreasesBioProcess() != null) {
							if (verbous)
								bpCode += "\t;;and tloc -| bioprocess\n";
							whatprocess = tl.getDecreasesAgent()
									.getABMCodeLabel();
							bpCode += "\tmove-tloc " + "\""
									+ tl.getFrom().getABMCodeLabel() + "\" "
									+ "\"" + tl.getTo().getABMCodeLabel()
									+ "\"" + "\"" + relation + "\" " + "\""
									+ whatprocess + "\" \"null\" \"null\""
									+ "\n";
						}
						if (tl.getDecreasesAgentActivity() != null) {
							// I need the ABMCodeLabel of the agent......
							for (Agent a : this.agents) {
								for (Activity acti : a.getActivities()) {
									if (tl.getDecreasesAgentActivity() == acti) {
										if (verbous)
											bpCode += "\t;;and tloc -| act(abundance(agent))\n";
										whatagentactivity = a.getABMCodeLabel();
										bpCode += "\tmove-tloc "
												+ "\""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" " + "\""
												+ tl.getTo().getABMCodeLabel()
												+ "\"" + "\"" + relation
												+ "\" \"null\" \"null\" \""
												+ whatagentactivity + "\" "
												+ "\n";
									}
								}
							}
						}
					}
					if ((thisedge.getRelationshipType() == RelationshipType.DECREASES || thisedge
							.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
							&& Utils.isBioProcess(bpSource
									.getProcessInfoOntClass().get(0),
									this.bioProcesses)) {
						// bioprocess -| tloc
						if (verbous)
							bpCode += "\t;;code source: KAM : bioprocess -| tloc\n";
						if (verbous)
							bpCode += "\t;;bioprocess decreases tloc from "
									+ tl.getFrom().getABMCodeLabel() + " to "
									+ tl.getTo().getABMCodeLabel() + "\n";
						if (verbous)
							bpCode += "\t;; do nothing";
					}
				}
			}
		}
		return bpCode;
	}

	/**
	 * generates translocation code snippet for the agent ag example: act (
	 * p(MGI:Cxcr5) ) -> tloc (a("LTi cell"), MESHCL:Liver, "primordial patch")
	 * AGENT ag TRANSLOCATION: AGENT2 FROM TO
	 * 
	 * @param ag
	 * @return code snippet
	 * 
	 */
	private String generateTlocCode(Agent ag) {
		String agCode = "";
		ArrayList<Translocation> tlocs = (ArrayList<Translocation>) Utils
				.getAllTranslocations(this.agents);
		// I need the KamNode(s) of the translocation and all its adjacent
		// KamNodes to check what KamNode actually calls the translocation
		Set<KamEdge> edges;
		for (Translocation tl : tlocs) {
			edges = ag.getAgentInfoKamNode().getKam()
					.getAdjacentEdges(tl.gettNode());
			for (KamEdge thisedge : edges) { // edges : adjacent edges of "this"
												// translocation node
				Agent sourceAgent = Utils.getAgentByNode(
						thisedge.getSourceNode(), this.agents); // might also be
																// null if not
																// an agent, eg
																// might be an
																// activity etc
				// ///////////////////////////////// abundance(ag) ->
				// tloc(agent2,from,to)
				// /////////////////////////////////////////////////////////////////////////////
				if (thisedge.getTargetNode() == tl.gettNode()
						&& (thisedge.getRelationshipType() == RelationshipType.INCREASES || thisedge
								.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
						&& sourceAgent != null
						&& Utils.isAgent(sourceAgent.getAgentInfoOntClass(),
								this.agents)
						&& sourceAgent.getAgentInfoKamNode().getId() == ag
								.getAgentInfoKamNode().getId()) {
					if (verbous)
						agCode += "\t;;code source: KAM : abundance(agent) -> tloc\n";
					if (verbous)
						agCode += "\t;;" + ag.getABMCodeLabel()
								+ " increases tloc of "
								+ tl.getTranslocatedAgent().getABMCodeLabel()
								+ " from " + tl.getFrom().getABMCodeLabel()
								+ " to " + tl.getTo().getABMCodeLabel() + "\n";
					Utils.appendToFile(new StringBuffer()
							.append("\t translocation: "
									+ ag.getABMCodeLabel()
									+ " increases tloc of "
									+ tl.getTranslocatedAgent()
											.getABMCodeLabel() + "s from "
									+ tl.getFrom().getABMCodeLabel() + " to "
									+ tl.getTo().getABMCodeLabel() + "\n"),
							logging);
					if (!ag.getBELIdLabel().equals(
							tl.getTranslocatedAgent().getBELIdLabel())) { // to
																			// prevent
																			// double
																			// ask
																			// in
																			// NetLogo
																			// code
						agCode += "\t ask " // eg in this case: ask LTin_Rets [
								+ tl.getTranslocatedAgent().getABMCodeLabel() // ;;LTin_Ret
																				// increases
																				// tloc
																				// of
																				// LTin_Ret
																				// from
																				// Liver
																				// to
																				// midgut
								+ "s \n"; // ask LTin_Rets [ move-tloc "Liver"
											// "midgut" "null" "null" "null"
											// "null" ]
						agCode += "\t [ \n";
					}
					if (ag.getBELIdLabel().equals(
							tl.getTranslocatedAgent().getBELIdLabel()))
						agCode += "\t  move-tloc " + "\""
								+ tl.getFrom().getABMCodeLabel() + "\" " + "\""
								+ tl.getTo().getABMCodeLabel()
								+ "\" \"null\" \"null\" \"null\" \"null\"\n";
					if (!ag.getBELIdLabel().equals(
							tl.getTranslocatedAgent().getBELIdLabel())) {
						agCode += "\t ] \n";
					}

					if (tl.getTranslocatedAgent().getTranslocations().size() >= 1) { // in
																						// cases
																						// when
																						// a
																						// translocation
																						// has
																						// an
																						// effect,
																						// such
																						// as
																						// increase
																						// a
																						// process
						// move-tloc [fromregion toregion relation whatprocess
						// whatagent whatagentactivity]
						// tloc (a("LTi cell"), MESHCL:Liver,
						// "primordial patch") -> bp("LTi cell aggregation")
						for (Translocation agentTransloc : tl
								.getTranslocatedAgent().getTranslocations()) {
							String whatprocess = "\"null\"";
							String whatagent = "\"null\"";
							String whatagentactivity = "\"null\"";
							if (agentTransloc.getIncreasesAgent() != null) {
								if (verbous)
									agCode += "\t;;and tloc -> abundance(agent)\n";
								whatagent = agentTransloc.getIncreasesAgent()
										.getABMCodeLabel();
								if (verbous)
									agCode += "\t;;                            : "
											+ whatagent + "\n";
								agCode += "\t  move-tloc " + "\""
										+ tl.getFrom().getABMCodeLabel()
										+ "\" " + "\""
										+ tl.getTo().getABMCodeLabel() + "\""
										+ " \"increases\" " + "\"null\" \""
										+ whatagent + "\" \"null\"" + "\n";
								agCode += "\t  ]\n";
								agCode += "\t]\n";
							}
							if (agentTransloc.getIncreasesBioProcess() != null) {
								if (verbous)
									agCode += "\t;;and tloc -> bioprocess\n";
								whatprocess = agentTransloc
										.getIncreasesBioProcess()
										.getABMCodeLabel();
								if (verbous)
									agCode += "\t;;                            : "
											+ whatprocess + "\n";
								agCode += "\t  move-tloc " + "\""
										+ tl.getFrom().getABMCodeLabel()
										+ "\" " + "\""
										+ tl.getTo().getABMCodeLabel() + "\""
										+ " \"increases\" " + "\""
										+ whatprocess + "\" \"null\" \"null\""
										+ "\n";
								agCode += "\t  ]\n";
								agCode += "\t]\n";
							}
							if (agentTransloc.getIncreasesAgentActivity() != null) {
								// I need the ABMCodeLabel of the agent......
								for (Agent a : this.agents) {
									for (Activity acti : a.getActivities()) {
										if (agentTransloc
												.getIncreasesAgentActivity() == acti) {
											if (verbous)
												agCode += "\t;;and tloc -> act(abundance(agent))\n";
											whatagentactivity = a
													.getABMCodeLabel();
											if (verbous)
												agCode += "\t;;                            : "
														+ whatagentactivity
														+ "\n";
											agCode += "\t  move-tloc "
													+ "\""
													+ tl.getFrom()
															.getABMCodeLabel()
													+ "\" "
													+ "\""
													+ tl.getTo()
															.getABMCodeLabel()
													+ "\""
													+ " \"increases\" \"null\" \"null\" \""
													+ whatagentactivity + "\" "
													+ "\n";
											agCode += "\t  ]\n";
											agCode += "\t]\n";
										}
									}
								}
							}
							if (agentTransloc.getDecreasesAgent() != null) {
								if (verbous)
									agCode += "\t;;and tloc -| abundance(agent)\n";
								whatagent = agentTransloc.getDecreasesAgent()
										.getABMCodeLabel();
								if (verbous)
									agCode += "\t;;                            : "
											+ whatagent + "\n";
								agCode += "\tmove-tloc " + "\""
										+ tl.getFrom().getABMCodeLabel()
										+ "\" " + "\""
										+ tl.getTo().getABMCodeLabel() + "\""
										+ " \"decreases\" " + "\"null\" \""
										+ whatagent + "\" \"null\"" + "\n";
								agCode += "\t  ]\n";
								agCode += "\t]\n";
							}
							if (agentTransloc.getDecreasesBioProcess() != null) {
								if (verbous)
									agCode += "\t;;and tloc -| bioprocess\n";
								whatprocess = agentTransloc
										.getDecreasesBioProcess()
										.getABMCodeLabel();
								if (verbous)
									agCode += "\t;;                            : "
											+ whatprocess + "\n";
								agCode += "\tmove-tloc " + "\""
										+ tl.getFrom().getABMCodeLabel()
										+ "\" " + "\""
										+ tl.getTo().getABMCodeLabel() + "\""
										+ " \"decreases\" " + "\""
										+ whatprocess + "\" \"null\" \"null\""
										+ "\n";
								agCode += "\t  ]\n";
								agCode += "\t]\n";
							}
							if (agentTransloc.getDecreasesAgentActivity() != null) {
								// I need the ABMCodeLabel of the agent......
								for (Agent a : this.agents) {
									for (Activity acti : a.getActivities()) {
										if (agentTransloc
												.getDecreasesAgentActivity() == acti) {
											if (verbous)
												agCode += "\t;;and tloc -| act(abundance(agent))\n";
											whatagentactivity = a
													.getABMCodeLabel();
											if (verbous)
												agCode += "\t;;                            : "
														+ whatagentactivity
														+ "\n";
											agCode += "\tmove-tloc "
													+ "\""
													+ tl.getFrom()
															.getABMCodeLabel()
													+ "\" "
													+ "\""
													+ tl.getTo()
															.getABMCodeLabel()
													+ "\""
													+ " \"decreases\" \"null\" \"null\" \""
													+ whatagentactivity + "\" "
													+ "\n";
											agCode += "\t  ]\n";
											agCode += "\t]\n";
										}
									}
								}
							}
						}
					}
				}
				// ///////////////////////////////// abundance(ag) -| tloc
				// /////////////////////////////////////////////////////////////////////////////
				if (thisedge.getTargetNode() == tl.gettNode()
						&& (thisedge.getRelationshipType() == RelationshipType.DECREASES || thisedge
								.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
						&& sourceAgent != null
						&& Utils.isAgent(sourceAgent.getAgentInfoOntClass(),
								this.agents)
						&& sourceAgent.getAgentInfoKamNode().getId() == ag
								.getAgentInfoKamNode().getId()) {
					if (verbous)
						agCode += "\t;;code source: KAM : abundance(agent) -| tloc\n";
					if (verbous)
						agCode += "\t;;" + ag.getABMCodeLabel()
								+ " decreases tloc of "
								+ tl.getTranslocatedAgent().getABMCodeLabel()
								+ " from " + tl.getFrom().getABMCodeLabel()
								+ " to " + tl.getTo().getABMCodeLabel() + "\n";
					if (verbous)
						agCode += "\t;; do nothing";
				}
				// ///////////////////////////////// act(abundance(ag)) -> tloc
				// /////////////////////////////////////////////////////////////////////////////
				if (thisedge.getTargetNode() == tl.gettNode()
						&& (thisedge.getRelationshipType() == RelationshipType.INCREASES || thisedge
								.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
						&& Utils.isActivityNode(thisedge.getSourceNode())) {
					// find the source node of eg
					// molecularActivity(proteinAbundance(2)) ie
					// proteinAbundance(2)
					KamNode sourceNode = Utils
							.getSourceNodeOfActivityNode(thisedge
									.getSourceNode());
					// then, find the corresponding agent
					sourceAgent = Utils.getAgentByNode(sourceNode, this.agents);
					if (!sourceAgent.IsInActive()) {
						if (ag.getBELIdLabel().equals(
								sourceAgent.getBELIdLabel())) { // das hier muss
																// wahrscheinlich
																// überall rein!
							if (verbous)
								agCode += "\t;;code source: KAM : act(abundance(agent)) -> tloc\n";
							if (verbous)
								agCode += "\t;;act of "
										+ sourceAgent.getABMCodeLabel()
										+ " increases tloc of "
										+ tl.getTranslocatedAgent()
												.getABMCodeLabel() + " from "
										+ tl.getFrom().getABMCodeLabel()
										+ " to " + tl.getTo().getABMCodeLabel()
										+ "\n";
							Utils.appendToFile(new StringBuffer()
									.append("\t translocation: act of "
											+ sourceAgent.getABMCodeLabel()
											+ " increases tloc of "
											+ tl.getTranslocatedAgent()
													.getABMCodeLabel()
											+ " from "
											+ tl.getFrom().getABMCodeLabel()
											+ " to "
											+ tl.getTo().getABMCodeLabel()
											+ "\n"), logging);

							if (tl.getTranslocatedAgent().getTranslocations()
									.size() < 1) {
								agCode += "\tif random 100 < activity\n";
								agCode += "\t[\n";
								agCode += "\t ask "
										+ tl.getTranslocatedAgent()
												.getABMCodeLabel() + "s \n";
								agCode += "\t [ \n";
								agCode += "\t  move-tloc "
										+ "\""
										+ tl.getFrom().getABMCodeLabel()
										+ "\" "
										+ "\""
										+ tl.getTo().getABMCodeLabel()
										+ "\" \"null\" \"null\" \"null\" \"null\"\n";
								agCode += "\t ]\n";
								agCode += "\t]\n";
							}

							else
								// move-tloc [fromregion toregion relation
								// whatprocess whatagent whatagentactivity]
								for (Translocation agentTransloc : tl
										.getTranslocatedAgent()
										.getTranslocations()) {
									String whatprocess = "\"null\"";
									String whatagent = "\"null\"";
									String whatagentactivity = "\"null\"";
									if (agentTransloc.getIncreasesAgent() != null) {
										if (verbous)
											agCode += "\t;;and tloc -> abundance(agent)\n";
										whatagent = agentTransloc
												.getIncreasesAgent()
												.getABMCodeLabel();
										if (verbous)
											agCode += "\t;;                            : "
													+ whatagent + "\n";
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t ask "
												+ tl.getTranslocatedAgent()
														.getABMCodeLabel()
												+ "s \n";
										agCode += "\t [ \n";
										agCode += "\t  move-tloc \""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" \""
												+ tl.getTo().getABMCodeLabel()
												+ "\""
												+ " \"increases\" \"null\" \""
												+ whatagent + "\" \"null\""
												+ "\n";
										agCode += "\t  ]\n";
										agCode += "\t]\n";
									}
									if (agentTransloc.getIncreasesBioProcess() != null) {
										if (verbous)
											agCode += "\t;;and tloc -> bioprocess\n";
										whatprocess = agentTransloc
												.getIncreasesBioProcess()
												.getABMCodeLabel();
										if (verbous)
											agCode += "\t;;                            : "
													+ whatprocess + "\n";
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t ask "
												+ tl.getTranslocatedAgent()
														.getABMCodeLabel()
												+ "s \n";
										agCode += "\t [ \n";
										agCode += "\t  move-tloc \""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" \""
												+ tl.getTo().getABMCodeLabel()
												+ "\"" + " \"increases\" \""
												+ whatprocess
												+ "\" \"null\" \"null\"" + "\n";
										agCode += "\t ]\n";
										agCode += "\t]\n";
										agCode += "\t\n";
									}
									if (agentTransloc
											.getIncreasesAgentActivity() != null) {
										// I need the ABMCodeLabel of the
										// agent......
										for (Agent a : this.agents) {
											for (Activity acti : a
													.getActivities()) {
												if (agentTransloc
														.getIncreasesAgentActivity() == acti) {
													if (verbous)
														agCode += "\t;;and tloc -> act(abundance(agent))\n";
													whatagentactivity = a
															.getABMCodeLabel();
													if (verbous)
														agCode += "\t;;                            : "
																+ whatagentactivity
																+ "\n";
													agCode += "\tif random 100 < activity\n";
													agCode += "\t[\n";
													agCode += "\t ask "
															+ tl.getTranslocatedAgent()
																	.getABMCodeLabel()
															+ "s \n";
													agCode += "\t [ \n";
													agCode += "\t  move-tloc \""
															+ tl.getFrom()
																	.getABMCodeLabel()
															+ "\" \""
															+ tl.getTo()
																	.getABMCodeLabel()
															+ "\""
															+ " \"increases\" \"null\" \"null\" \""
															+ whatagentactivity
															+ "\" " + "\n";
													agCode += "\t ]\n";
													agCode += "\t]\n";
													agCode += "\t\n";
												}
											}
										}
									}
									if (agentTransloc.getDecreasesAgent() != null) {
										if (verbous)
											agCode += "\t;;and tloc -| abundance(agent)\n";
										whatagent = agentTransloc
												.getDecreasesAgent()
												.getABMCodeLabel();
										if (verbous)
											agCode += "\t;;                            : "
													+ whatagent + "\n";
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t ask "
												+ tl.getTranslocatedAgent()
														.getABMCodeLabel()
												+ "s \n";
										agCode += "\t [ \n";
										agCode += "\t  move-tloc "
												+ "\""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" \""
												+ tl.getTo().getABMCodeLabel()
												+ "\""
												+ " \"decreases\" \"null\" \""
												+ whatagent + "\" \"null\""
												+ "\n";
										agCode += "\t  ]\n";
										agCode += "\t]\n";
										agCode += "\t\n";
									}
									if (agentTransloc.getDecreasesBioProcess() != null) {
										if (verbous)
											agCode += "\t;;and tloc -| bioprocess\n";
										whatprocess = agentTransloc
												.getDecreasesBioProcess()
												.getABMCodeLabel();
										if (verbous)
											agCode += "\t;;                            : "
													+ whatprocess + "\n";
										agCode += "\tif random 100 < activity\n";
										agCode += "\t[\n";
										agCode += "\t ask "
												+ tl.getTranslocatedAgent()
														.getABMCodeLabel()
												+ "s \n";
										agCode += "\t [ \n";
										agCode += "\t  move-tloc "
												+ "\""
												+ tl.getFrom()
														.getABMCodeLabel()
												+ "\" \""
												+ tl.getTo().getABMCodeLabel()
												+ "\"" + " \"decreases\" \""
												+ whatprocess
												+ "\" \"null\" \"null\"" + "\n";
										agCode += "\t  ]\n";
										agCode += "\t]\n";
										agCode += "\t\n";
									}
									if (agentTransloc
											.getDecreasesAgentActivity() != null) {
										// I need the ABMCodeLabel of the
										// agent......
										for (Agent a : this.agents) {
											for (Activity acti : a
													.getActivities()) {
												if (agentTransloc
														.getDecreasesAgentActivity() == acti) {
													if (verbous)
														agCode += "\t;;and tloc -| act(abundance(agent))\n";
													whatagentactivity = a
															.getABMCodeLabel();
													if (verbous)
														agCode += "\t;;                            : "
																+ whatagentactivity
																+ "\n";
													agCode += "\tif random 100 < activity\n";
													agCode += "\t[\n";
													agCode += "\t ask "
															+ tl.getTranslocatedAgent()
																	.getABMCodeLabel()
															+ "s \n";
													agCode += "\t [ \n";
													agCode += "\t  move-tloc "
															+ "\""
															+ tl.getFrom()
																	.getABMCodeLabel()
															+ "\" \""
															+ tl.getTo()
																	.getABMCodeLabel()
															+ "\" "
															+ " \"decreases\" \"null\" \"null\" \""
															+ whatagentactivity
															+ "\" \n";
													agCode += "\t  ]\n";
													agCode += "\t]\n";
													agCode += "\t\n";
												}
											}
										}
									}
								}
						}
					}
				}
				// ///////////////////////////////// act(abundance(agent)) -|
				// tloc
				if (thisedge.getTargetNode() == tl.gettNode()
						&& (thisedge.getRelationshipType() == RelationshipType.DECREASES || thisedge
								.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
						&& Utils.isActivityNode(thisedge.getSourceNode())) {
					if (verbous)
						agCode += "\t;;code source: KAM : act(abundance(agent)) -| tloc\n";
					if (verbous)
						agCode += "\t;;act of " + ag.getABMCodeLabel()
								+ " decreases tloc of "
								+ tl.getTranslocatedAgent().getABMCodeLabel()
								+ " from " + tl.getFrom().getABMCodeLabel()
								+ " to " + tl.getTo().getABMCodeLabel() + "\n";
					if (verbous)
						agCode += "\t;; do nothing";
				}
			}
		}
		return agCode;
	}

	/**
	 * checks if the agent is a biomarker for (a) process(es)
	 * 
	 * @param ag
	 * @param bp
	 * @return the code snippet
	 */
	private String generateCodeIsBiomarkerForBioProcess(Agent ag, BioProcess bp) {
		String code = "";
		if (verbous)
			code += "\t;;code source: KAM : agent is biomarker for process\n";
		if (verbous)
			code += "\t;;" + ag.getABMCodeLabel() + " is biomarker for "
					+ bp.getABMCodeLabel() + "\n";
		Utils.appendToFile(
				new StringBuffer().append("\nLikelihood used for "
						+ ag.getABMCodeLabel() + " increases "
						+ bp.getABMCodeLabel() + " set to random.\n"), logging);
		code += "\tif random 100 <= 50\n ";
		code += "\t\t[ " + bp.getABMCodeLabel()
				+ " \"increases_process\" \"null\"]\n";

		return code;
	}

	/**
	 * abundance of ag decreases bp according to KAM
	 * 
	 * @param bp
	 * @param ag
	 * @return
	 */
	private String generateCodeDecreasesBioProcess(Agent ag, BioProcess bp,
			KamStore kamStore) {
		String code = "";
		ArrayList<Annotation> annotations = getAnnotationsFromNodes(
				ag.getAgentInfoKamNode(), "decreases",
				bp.getProcessInfoKamNode(), kamStore);
		ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
		if (verbous)
			code += "\t;;code source: KAM : agent decreases process - "
					+ ag.getABMCodeLabel() + " decreases "
					+ bp.getABMCodeLabel() + "\n";
		// TODOlater Attention: chooses first mapped ontClass it finds! others
		// will be discarded
		OntClass bpOntClass = bp.getProcessInfoOntClass().get(0);
		Utils.appendToFile(new StringBuffer()
				.append("\nChecking ontology for decreases of bp "
						+ bp.getBELTermLabel() + ": \n"), logging);
		OntProperty decreases = bpOntClass.getOntModel().getOntProperty(
				this.decreasesUri);
		if (bpOntClass != null && decreases != null) {
			if (Utils.hasRestriction(bpOntClass, decreases)) {
				if (validRegions != null && validRegions.size() > 0)
					for (Region re : validRegions) {
						code += "\t" + bp.getABMCodeLabel()
								+ " \"decreases_process\" \""
								+ re.getABMCodeLabel() + "\"\n";
					}
				else
					code += "\t" + bp.getABMCodeLabel()
							+ " \"decreases_process\" \"null\"\n";
			}
		}
		return code;
	}

	/**
	 * eg agent increases MSO:Demyelination -------------------------------- ;
	 * and MSO:Demyelination has axiom DECREASES some "Myelin Sheath" --> create
	 * code "ask agent [ set myelin myelin - 1 ]"
	 * 
	 * @param ag
	 * @param bp
	 * @return String theCode
	 */
	private String generateCodeIncreasesBioProcess(Agent ag, BioProcess bp,
			KamStore kamStore) {
		// need to look the bioprocess up in the ontology and implements its
		// effects
		String code = "";
		ArrayList<Annotation> annotations = getAnnotationsFromNodes(
				ag.getAgentInfoKamNode(), "increases",
				bp.getProcessInfoKamNode(), kamStore);
		ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
		if (verbous)
			code += "\t;;code source: KAM : agent increases process\n";
		if (verbous)
			code += "\t ;;" + ag.getABMCodeLabel() + " increases "
					+ bp.getABMCodeLabel() + "\n";
		if (validRegions != null && validRegions.size() > 0)
			for (Region re : validRegions) {
				code += "\tif random 100 < activity\n";
				code += "\t[\n";
				code += "\t" + bp.getABMCodeLabel()
						+ "  \"increases_process\" \"" + re.getABMCodeLabel()
						+ "\"\n";
				code += "\t]\n";
			}
		else {
			code += "\tif random 100 < activity\n";
			code += "\t[\n";
			code += "\t" + bp.getABMCodeLabel()
					+ "  \"increases_process\" \"null\"\n";
			code += "\t]\n";
		}
		return code;
	}

	/**
	 * creates i agents of type obj at position xpos ypos
	 * 
	 * @param obj
	 * @param i
	 */
	private String createAgentCode(OntClass obj, int i, String xpos, String ypos) {
		String code = "";
		if (Utils.getAgentByOntClass(obj, this.agents) == null) {
		} else {
			Agent a = Utils.getAgentByOntClass(obj, this.agents);
			code += "\t create-" + a.getABMCodeLabel() + "s 1\n";
			code += "\t [\n";
			code += "\tset color " + a.getColorString() + "\n";
			code += "\tset size " + a.getSize() + "\n";
			code += "\tsetxy " + xpos + " " + ypos + "\n";
			code += "]\n";
		}
		return code;
	}

	/**
	 * abundance of ag decreases abundance of ag2
	 * 
	 * @param ag
	 * @param ag2
	 * @return code snippet
	 */
	private String generateCodeDecreasesAbundance(Agent ag, Agent ag2,
			KamStore kamStore) {
		String code = "";
		ArrayList<Annotation> annotations = getAnnotationsFromNodes(
				ag.getAgentInfoKamNode(), "decreases",
				ag2.getAgentInfoKamNode(), kamStore);
		ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
		if (verbous)
			code += "\t;;code source: KAM : agent decreases agent\n";
		if (verbous)
			code += "\t ;;" + ag.getABMCodeLabel() + " decreases "
					+ ag2.getABMCodeLabel() + "\n";
		if (validRegions != null && validRegions.size() > 0)
			for (Region re : validRegions) {
				if (!ag.IsInActive()) {
					code += "\tif random 100 < activity \n"; // AND patch hat
																// region = re
					code += "\t[\n";
				}
				code += "\t if region = \"" + re.getABMCodeLabel() + "\"\n";
				code += "\t  [\n";
				code += "\t    ask patch-here \n";
				code += "\t    [\n";
				code += "\t        if one-of " + ag2.getABMCodeLabel()
						+ "s != nobody\n";
				code += "\t        [ \n";
				code += "\t          let this_" + ag2.getABMCodeLabel()
						+ " min-one-of " + ag2.getABMCodeLabel()
						+ "s [distance myself]\n";
				code += "\t          if  distance this_"
						+ ag2.getABMCodeLabel() + " <= reaction-distance \n";
				code += "\t          [    \n";
				code += "\t              ask this_" + ag2.getABMCodeLabel()
						+ "  [ die ]    \n";
				code += "\t          ] \n";
				code += "\t        ] \n";
				code += "\t    ] \n";
				code += "\t  ]\n";
				if (!ag.IsInActive())
					code += "\t]\n";
			}
		else {
			if (!ag.IsInActive()) {
				code += "\tif random 100 < activity \n";
				code += "\t  [\n";
			}
			code += "\t      if one-of " + ag2.getABMCodeLabel()
					+ "s != nobody\n";
			code += "\t      [ \n";
			code += "\t        let this_" + ag2.getABMCodeLabel()
					+ " min-one-of " + ag2.getABMCodeLabel()
					+ "s [distance myself]\n";
			code += "\t        if  distance this_" + ag2.getABMCodeLabel()
					+ " <= reaction-distance \n";
			code += "\t        [    \n";
			code += "\t            ask this_" + ag2.getABMCodeLabel()
					+ "  [ die ]    \n";
			code += "\t        ] \n";
			code += "\t      ] \n";
			if (!ag.IsInActive())
				code += "\t  ]\n";
		}
		return code;
	}

	/**
	 * turtle dies when energy <= 0 and method supposes the organism aims to
	 * achieve homeostasis: agent dies when there are too many of its kind
	 * relative to homeostatic number / max homeostatic number (and, as in
	 * reproduce, outcome whether it dies or not depends on the reproduction
	 * algorithm used)
	 * 
	 * @return code snippet for procedure
	 */
	private String generateCodeDeathHomeostatic(Agent a) {
		String code = "";
		if (!a.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
			code += "\nto death-homeostatic-" + a.getABMCodeLabel()
					+ "                      ;; turtle procedure\n"
					+ "\t;; when energy dips below zero, die\n";
			if (!a.isComposite())
				code += "\tif energy <= 0 [ die ]\n";
			//if the agent is a composite
			if (a.isComposite()){
				//TODO correct this
				code += "\tif energy <= 0 and is-agentset? higher_en_memb \n";
				code += "\t[\n";
				code += "\t  ;; the agent needs to die (composite) but the member with the higher energy needs to be split off and live\n";
				code += "\t  ;; set default fallbacks\n";
				code += "\t  let co black\n";
				code += "\t  let si 1\n";
				for (Agent incl : a.getIncludes()){
					code += "\t  if higher_en_memb = "+incl.getABMCodeLabel().toLowerCase()+"s"+" \n";
					code += "\t  [ \n";
					code += "\t    set co "+incl.getColorString()+"\n";
					code += "\t    set si "+incl.getSize()+"\n";
					code += "\t  ] \n";
				}
				
				code += "\t  let command (word \"hatch-\" higher_en_memb \" 1 [ set energy \" higher_en_memb_diff \" set higher_en_memb 0 set higher_en_memb_diff 0 set activity \" activity \" set xcor \" xcor \" set ycor \" ycor \" set color \" co \" set size \" si \"]\")\n";
				code += "\t  run command\n";
				code += "\t  die\n";
				code += "\t]\n";
			}
			//if homeostasis mimicking is switched on
			code += "\tif homeostasis_mimicking?\n";
			code += "\t[\n";
			code += "\t  ;;homeostasis mimicking: die when there are too many of your kind\n";
			code += "\t  let current count breed                            ;;homeostasis mimicking: die when there are too many of your kind\n";
			if (a.hasHomeostaticConcentration())
				code += "\t  let h homeostatic-" + a.getABMCodeLabel() + " \n";
			else if (a.hasMaxHomeostaticConcentration())
				code += "\t  let h maxhomeostatic-" + a.getABMCodeLabel()
						+ " \n";
			code += "\t  let minimum h / 3\n";
			code += "\t  let maximum h * 3\n";
			code += "\t  let dev_cur_from_homeo  current - h                          ;; deviation of current number from homeostatic value\n";
			code += "\t  let ran random-normal-in-bounds h (h / 20) minimum maximum\n";
			code += "\t  let dev_ran_from_homeo abs h - ran                           ;; deviation of random normal number from homeostatic value\n";
			code += "\t  if dev_cur_from_homeo > 0 and ( random-float 1 >= abs ( dev_ran_from_homeo / dev_cur_from_homeo) )     ;; the greater the deviation, the higher the probability to die\n";
			code += "\t  [\n";
			code += "\t    if random 100 < 50 \n  ";
			code += "         [ die ]\n";
			code += "\t  ]\n";
			code += "\t]\n";

			code += "end\n\n";
		}
		return code;
	}



	/**
	 * reproduce ; if under homeostatic control: compares current number to
	 * homeostatic cell number ; increases/decreased probability if it only has
	 * maxHomeostaticConcentration, treats agent as if not under homeostatic
	 * contral if not : reproduce according to dupli-rate-x
	 * 
	 * @param a
	 */
	private String generateCodeReproduceHomeostatic(Agent a) {
		String code = "\nto reproduce-" + a.getABMCodeLabel()
				+ " [times] ;; turtle procedure\n";
		if (a.hasUpperValueLimit()) {
			code += "\t;; agent has an upper limit of "
					+ a.getUpperValueLimit() + "\n";
			code += "\t;; if its number gets as high or higher than this, let its youngest agents die\n";
			code += "\tlet cur_no count breed\n";
			code += "\tlet youngest one-of breed  ;; just to initialize\n";
			code += "\trepeat cur_no - upper-lim-" + a.getABMCodeLabel() + " - 1 \n";
			code += "\t[\n";
			code += "\t  set youngest max-one-of breed [energy]\n";
			code += "\t  ask youngest [ die ]\n";
			code += "\t]\n";
		}
		if (a.isHomeostaticControl() && a.hasHomeostaticConcentration()) {
			code += "\tifelse homeostasis_mimicking? \n";
			code += "\t[\n";
			code += "\t\tlet current count breed\n";
			code += "\t\tlet minimum homeostatic-" + a.getABMCodeLabel()
					+ " / 3\n";
			code += "\t\tlet maximum homeostatic-" + a.getABMCodeLabel()
					+ " * 3\n";
			code += "\t\tlet dev_cur_from_homeo  current - homeostatic-"
					+ a.getABMCodeLabel()
					+ "            ;; deviation of current number from homeostatic value\n";
			code += "\t\tlet ran random-normal-in-bounds homeostatic-"
					+ a.getABMCodeLabel() + " (homeostatic-"
					+ a.getABMCodeLabel() + " / 20) minimum maximum\n";
			code += "\t\tlet dev_ran_from_homeo abs homeostatic-"
					+ a.getABMCodeLabel()
					+ " - ran                       ;; deviation of random normal number from homeostatic value\n";
			code += "\t\tif (dev_cur_from_homeo < 0 and ( random-float 1 >= abs ( dev_ran_from_homeo / dev_cur_from_homeo ) ))  or  (dev_cur_from_homeo > 0 and ( random-float 1 <= abs ( dev_ran_from_homeo / dev_cur_from_homeo ) ))	   ;; the greater the deviation, the higher the probability to reproduce\n";
			code += "\t\t[\n";
			code += "\t\t  if random 100 < dupli-rate-" + a.getABMCodeLabel()
					+ " and energy > 0 [\n ";
		}
		if (!a.isHomeostaticControl()) {
			code += "\t\t  if random 100  < dupli-rate-" + a.getABMCodeLabel()
					+ " and energy > 0 ;;agent is not under hom. control \n ";
			code += "\t\t\t  [                 \n";
		}
		if (a.isHomeostaticControl() && a.hasMaxHomeostaticConcentration()
				&& !a.hasHomeostaticConcentration()) {
			code += "\tifelse homeostasis_mimicking?\n";
			code += "\t[\n";
			code += "\t\tlet current count breed\n";
			code += "\t\tlet minimum maxhomeostatic-" + a.getABMCodeLabel()
					+ " / 3\n";
			code += "\t\tlet maximum maxhomeostatic-" + a.getABMCodeLabel()
					+ " * 3\n";
			code += "\t\tlet dev_cur_from_homeo  current - maxhomeostatic-"
					+ a.getABMCodeLabel()
					+ "            ;; deviation of current number from homeostatic value\n";
			code += "\t\tlet ran random-normal-in-bounds maxhomeostatic-"
					+ a.getABMCodeLabel() + " (maxhomeostatic-"
					+ a.getABMCodeLabel() + " / 20) minimum maximum\n";
			code += "\t\tlet dev_ran_from_homeo abs maxhomeostatic-"
					+ a.getABMCodeLabel()
					+ " - ran                       ;; deviation of random normal number from homeostatic value\n";
			code += "\t\tif dev_cur_from_homeo > 0 and ( random-float 1 <= abs ( dev_ran_from_homeo / dev_cur_from_homeo ) )     ;; the greater the deviation from maxvalue, the lower the probability to reproduce\n";
			code += "\t\t[\n";
			code += "\t\t  if random 100 < dupli-rate-" + a.getABMCodeLabel()
					+ " and energy > 0 [\n ";
		}
		code += "\t\t\t    set activity (activity / 2)             ;; divide activity between parent and offspring\n"
				+ "\t\t\t    hatch-"
				+ a.getABMCodeLabel()
				+ "s times [ lt random 90   set energy random (2 * lifespan-"
				+ a.getABMCodeLabel()
				+ ")    set color "
				+ a.getColorString()
				+ "  set size "
				+ a.getSize()
				+ " ]          ;; don't move forward to prevent leaving the region\n";
		if (a.isHomeostaticControl() && a.hasHomeostaticConcentration()) {
			code += "\t\t]\n";
			code += "\t]\n";
			code += " ]\n";
			// else part of the ifelse
			code += "\t[\n";
			code += "\t  if random 100 < dupli-rate-" + a.getABMCodeLabel()
					+ " and energy > 0 [\n ";
			code += "\t\t    set activity (activity / 2)             ;; divide activity between parent and offspring\n"
					+ "\t\t    hatch-"
					+ a.getABMCodeLabel()
					+ "s times [ lt random 90   set energy random (2 * lifespan-"
					+ a.getABMCodeLabel()
					+ ")      set color "
					+ a.getColorString()
					+ "  set size "
					+ a.getSize()
					+ " ]          ;; don't move forward to prevent leaving the region\n";
			code += "\t\t]\n";
			code += "\t]\n";
		}
		if (a.hasMaxHomeostaticConcentration()
				&& !a.hasHomeostaticConcentration()) {
			code += "\t\t]\t                   \n";
			code += "\t]\n";
			code += " ]\n";
			// else part of the ifelse
			code += "\t[\n";
			code += "\t  if random 100 < dupli-rate-" + a.getABMCodeLabel()
					+ " and energy > 0 [\n ";
			code += "\t\t    set activity (activity / 2)             ;; divide activity between parent and offspring\n"
					+ "\t\t    hatch-"
					+ a.getABMCodeLabel()
					+ "s times [ lt random 90   set energy random (2 * lifespan-"
					+ a.getABMCodeLabel()
					+ ")     set color "
					+ a.getColorString()
					+ "  set size "
					+ a.getSize()
					+ " ]          ;; don't move forward to prevent leaving the region\n";
			code += "\t\t]\n";
			code += "\t]\n";
		}
		if (!a.isHomeostaticControl()) {
			code += "\t]\t                   \n";
		}
		code += "end\n\n";
		return code;
	}

	/**
	 * reproduce according to duplication-rate (turtle own variable)
	 * 
	 * @param a
	 */
	private String generateCodeReproduceReproRate(Agent a) {
		String code = "\nto reproduce-" + a.getABMCodeLabel()
				+ " [times]   ;; turtle procedure\n";
		if (!a.IsInActive())
			code += "\t\t  if energy > 0 \n ";
		else
			code += "\t\t  if energy > 0   \n "
					+ "\t\t\t[ set energy (energy / 2)                 ;; divide energy between parent and offspring\n"
					+ "\t\t\t  set activity (activity / 2)             ;; divide activity between parent and offspring\n"
					+ "\t\t\t  hatch-"
					+ a.getABMCodeLabel()
					+ "s times [ lt 45     set color "
					+ a.getColorString()
					+ "  set size "
					+ a.getSize()
					+ "  set region region]  ]                ;; don't move forward to prevent leaving the region\n"
					+ "end\n\n";
		return code;
	}

	/**
	 * move a turtle randomly inside its region
	 * 
	 * @return code snippet
	 */
	private String generateCodeMove() {
		// turtle needs to stay inside its allocated region
		String code = "\n";
		code += "to move-inside [speed] ;; turtle procedure\n";
		code += "	set energy energy - 1\n";
		code += "	rt random 90\n";
		code += "   let re region\n";
		code += "   let move 0   ;;set to 1 if turtle is to move\n";
		code += "	ask patch-ahead speed \n";
		code += "	[  \n";
		code += "	  if region = re\n";
		code += "	  [\n";
		code += "	   	 set move 1\n";
		code += "	  ]\n";
		code += "	]\n";
		code += "	if move = 1\n";
		code += "	[\n";
		code += "	  fd speed\n";
		code += "	]\n";
		code += "end\n";

		return code;
	}

	/**
	 * move a turtle randomly inside its region function lets a composite's
	 * member agents move. the composite dissolves if its agents are > reaction
	 * distance from one another
	 * 
	 * @return code snippet
	 */
	private String generateCodeMoveCompositeAgent(Agent a) {
		// turtle needs to stay inside its allocated region
		String code = "\n";
		code += "to move-inside-composite-" + a.getABMCodeLabel()
				+ "  ;; turtle procedure\n";
		code += " ;; simulate movement of the two included agents and save distance\n";
		code += " ;; let composite die when distance > reaction distance, hatch 2 new agents instead\n";
		code += "let membersplitoff 0\n";
		// hatch 2 turtles with distance a.distance, make them move and save new
		// distance
		code += "let dist memberdist\n";
		code += "let newdist 0  ;; will be overwritten\n";
		code += "let en energy\n";
		code += "let re region\n";
		code += "let a activity\n";
		code += "let tmp random-float 1000\n";
		code += "let movefd 0\n";
		code += "let number_repeats length "+ a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n";
		code += "let n_agent_breed \"\" ;  this is the n in 1:n or n:1 \n";
		code += "ifelse "+a.getABMCodeLabel()+"_members = \"n:1\"  ;  this is the n in 1:n or n:1 \n";
		code += "[\n";
		code += "   set n_agent_breed \""+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s\"\n";
		code += "]\n";
		code += "[\n";
		code += "   set n_agent_breed \""+a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s\"\n";
		code += "]\n";
		if (a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
			code += "if length "+ a.getIncludes().get(1).getABMCodeLabel().toLowerCase().toLowerCase()+"s2_energies > number_repeats [ set number_repeats length "+ a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies ]\n";
		else
			code += "if length "+ a.getIncludes().get(1).getABMCodeLabel().toLowerCase().toLowerCase()+"s_energies > number_repeats [ set number_repeats length "+ a.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies ]\n";
		//caution: the following only works for 1:n 1:1 or n:1. It won't work once the user can freely chose numbers!
		code += "repeat number_repeats\n";
		code += "  [\n";
		code += "  hatch-" + a.getIncludes().get(0).getABMCodeLabel() + "s 1 [\n";
		code += "                  setxy xcor ycor\n";
		code += "                  set region re\n";
		code += "                  rt random 90\n";
		code += "                  set color "+ a.getIncludes().get(0).getColorString() + "\n";
		code += "                  set size "+ a.getIncludes().get(0).getSize() + "\n";
		code += "                  ask patch-ahead "+a.getIncludes().get(0).getABMCodeLabel()+"-move-speed [\n";
		code += "                   if region = re \n";
		code += "                   [ \n";
		code += "                     set movefd 1\n";
		code += "                   ]\n";
		code += "                  ]\n";
		code += "                  ifelse movefd = 1 [ fd "+a.getIncludes().get(0).getABMCodeLabel()+"-move-speed ]\n";
		code += "                   [ \n";
		code += "                      rt random 360\n";
		code += "                   ]\n";
		code += "                  set energy tmp \n";
		code += "  ]\n";
		code += "  set movefd 0\n";

		Agent secondAgent = a.getIncludes().get(0);
		if (a.getIncludes().size() > 1)
			secondAgent = a.getIncludes().get(1);
		code += "  hatch-" + secondAgent.getABMCodeLabel() + "s 1 [\n";
		code += "                  setxy xcor ycor\n";
		code += "                  set region re \n";
		code += "                  let x xcor \n";
		code += "                  let y ycor \n";
		code += "                  let move 0 ;;boolean to indicate whether turtle can move to the new patch at dist away \n";
		code += "                  ask patch-at 0 dist [ \n";
		code += "                    if region = re [\n";
		code += "                      set move 1\n";
		code += "                    ]\n";
        code += "                  ] \n";
        code += "                  if move = 1 [\n";
        code += "                    setxy x (y + dist)\n";
        code += "                  ]\n";
		code += "                  set color " + secondAgent.getColorString()+ "\n";
		code += "                  set size " + secondAgent.getSize() + "\n";
		code += "                  ask patch-ahead "+secondAgent.getABMCodeLabel()+"-move-speed [\n";
		code += "                   if region = re \n";
		code += "                   [ \n";
		code += "                      set movefd 1\n";
		code += "                   ]\n";
		code += "                  ]\n";
		code += "                  ifelse movefd = 1 [ fd "+secondAgent.getABMCodeLabel()+"-move-speed ]\n";
		code += "                  [\n";
		code += "                    rt random 360\n";
		code += "                  ]\n";
		code += "                  set energy tmp \n";
		code += "  ]\n";

		code += "  ask one-of " + a.getIncludes().get(0).getABMCodeLabel()+ "s with [energy = tmp] [\n";
		code += "    let closest other " + secondAgent.getABMCodeLabel()+"s with [energy = tmp] \n";
		code += "    set newdist distance one-of closest\n";
		code += "    if distance one-of closest > reaction-distance [\n";
		code += "    ;; then 1 of the n included agents needs to be split off\n";
		code += "       set membersplitoff 1\n";
		code += "    ]\n";
		code += "   ]\n";
		code += " ;; let 1 of the n members of the correct breed walk out of the composite\n";
		if(a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
			code += "   let command (word \"set \" n_agent_breed \"2_energies\" \" but-last \" n_agent_breed \"2_energies\")\n";
		else
			code += "   let command (word \"set \" n_agent_breed \"_energies\" \" but-last \" n_agent_breed \"_energies\")\n";
		code += "   if membersplitoff = 1 [  run command  ]                       \n";
		code += "   ask turtles with [energy = tmp]   [ \n";
		code += "     ifelse membersplitoff = 1 [ if breed != n_agent_breed [die] ]   ;; the n_agent_breed turtle needs to be split off and live\n";
		code += "     [ die ];                                                        ;; otherwise all agents used for simulation die\n";
		code += "   ]   \n";
		code += "   set dist memberdist\n";
		code += "   set newdist 0  ;; will be overwritten\n";
		code += "   set en energy\n";
		code += "   set re region\n";
		code += "   set a activity\n";
		code += "   set tmp random-float 1000\n";
		code += "   set movefd 0\n";
		code += "]\n";
		code += "\n";
		
		code += " ;; deduct energy from each of the agents contained   \n";
		code += " let cnt 0\n";
		code += " repeat length "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies   \n";
		code += "   [ set "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies replace-item cnt "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies (item cnt "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies - 1) \n";
		code += "     set cnt cnt + 1  ]   \n";
		code += " set cnt 0\n";
		if(a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
			code += " repeat length "+secondAgent.getABMCodeLabel().toLowerCase()+"s2_energies   \n";
		else
			code += " repeat length "+secondAgent.getABMCodeLabel().toLowerCase()+"s_energies   \n";
		if(a.getIncludes().get(0).getABMCodeLabel().equals(a.getIncludes().get(1).getABMCodeLabel()))
			code += "   [ set "+secondAgent.getABMCodeLabel().toLowerCase()+"s2_energies replace-item cnt "+secondAgent.getABMCodeLabel().toLowerCase()+"s2_energies (item cnt "+secondAgent.getABMCodeLabel().toLowerCase()+"s2_energies - 1) \n";
		else
			code += "   [ set "+secondAgent.getABMCodeLabel().toLowerCase()+"s_energies replace-item cnt "+secondAgent.getABMCodeLabel().toLowerCase()+"s_energies (item cnt "+secondAgent.getABMCodeLabel().toLowerCase()+"s_energies - 1) \n";
		code += "     set cnt cnt + 1  ]   \n";
		code += " move-inside "+a.getABMCodeLabel()+"-move-speed       ;; composite moves normally\n";
		/*code += " if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies and not empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase+"s_energies\n";
		code += " [ foreach "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase+"s_energies \n";
		code += "    [ "+hatchagent(a.getIncludes().get(1), 1, a.getIncludes().get(1).getColorString(), "a", 
				a.getIncludes().get(1).getSize(), "?", "0", "0", "0", "null")+"]\n";
		code += " ]\n";
		code += " if not empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase+"s_energies and  empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase+"s_energies\n";
		code += " [ foreach "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase+"s_energies \n";
		code += "    [ "+hatchagent(a.getIncludes().get(0), 1, a.getIncludes().get(0).getColorString(), "a", 
				a.getIncludes().get(0).getSize(), "?", "0", "0", "0", "null")+"]\n";
		code += " ]\n";
		code += " if empty? "+a.getIncludes().get(0).getABMCodeLabel().toLowerCase+"s_energies or empty? "+a.getIncludes().get(1).getABMCodeLabel().toLowerCase+"s_energies [die]\n";*/
		code += " set memberdist newdist                      \n";
		code += "end\n\n";
		return code;
	}

	/**
	 * moves a turtle from one region to another region depending on the
	 * Translocation info inside the agent relation: if the tloc has a further
	 * effect, such as increases (=relation) a process (=whatprocess) an agent
	 * (=whatagent) an agent's activity (=whatagentactivity) the variables not
	 * needed need to be set to \"null\"
	 * 
	 * @return code snippet
	 */
	private String generateCodeTLoc() {
		String code = "\n";
		code += ";; relation: if the tloc has a further effect, such as increases (=relation)\n";
		code += ";;                                                     a process   (=whatprocess)\n";
		code += ";;                                                     an agent    (=whatagent)\n";
		code += ";;                                                     an agent's activity   (=whatagentactivity)\n";
		code += ";;                                            the variables not needed need to be set to \"null\"\n";
		code += "to move-tloc [fromregion toregion relation whatprocess whatagent whatagentactivity]   ;; turtle procedure\n";
		code += "  if region = fromregion\n";
		code += "		  [\n";
		code += "		    let closestToPatch min-one-of patches with [region = toregion] [distance myself]\n";
		code += "		    if distance closestToPatch <= reaction-distance\n";
		code += "		    [\n";
		code += "		      if random 100 < activity\n";
		code += "		      [\n";
		code += "		        move-to closestToPatch\n";
		code += "		        let proc whatprocess\n";
		code += "		        let repproc proc \n";
		code += "		        let repactivityagents word whatagentactivity  \"s\"\n";
		code += "		        let hatchagent \"\"\n";
		code += "		        if whatagent != \"null\" [ set hatchagent (word \"hatch-\" whatagent \"s 1 [ set color \" [color] of whatagent \"  set activity \" [activity] of whatagent \"  set energy \" [energy] of whatagent \" set size \" [size] of whatagent \" ]\" ) ]\n";
		code += "		        if relation = \"increases\"\n";
		code += "		        [\n";
		// code += "		          if random 100 >= 50\n";
		// code += "		          [\n";
		code += "		            if whatprocess != \"null\"\n";
		code += "		            [ \n";
		code += "		              (run repproc \"increases_process\" fromregion)\n";
		code += "		              (run repproc \"increases_process\" toregion) \n";
		code += "		            ] \n";
		code += "		            if whatagent != \"null\" [ run hatchagent ]\n";
		code += "		            if whatagentactivity != \"null\"\n";
		code += "		            [\n";
		code += "		              ask repactivityagents              [\n";
		code += "		                if region = fromregion or region = toregion  \n";
		code += "		                [  \n";
		code += "		                  if activity < 100 [ set activity activity + 1 ]\n";
		code += "		                ]\n";
		code += "		              ]  \n";
		code += "		            ]  \n";
		// code += "		          ]\n";
		code += "		        ]\n";
		code += "		        if relation = \"decreases\"\n";
		code += "		        [\n";
		// code += "		          if random 100 >= 50\n";
		// code += "		          [\n";
		code += "		            if whatprocess != \"null\"\n";
		code += "		            [ \n";
		code += "		              (run repproc \"decreases_process\" fromregion)\n";
		code += "		              (run repproc \"decreases_process\" toregion) \n";
		code += "		            ] \n";
		code += "		            if whatagent != \"null\"\n";
		code += "		            [ \n";
		code += "		              if one-of whatagent != \"null\"\n";
		code += "		              [ \n";
		code += "		                let ag one-of whatagent\n";
		code += "		                  if region = fromregion or region = toregion\n";
		code += "		                  [  \n";
		code += "		                    ask ag [ die ]\n";
		code += "		                  ]   \n";
		code += "		              ]  \n";
		code += "		            ]  \n";
		code += "		            if whatagentactivity != \"null\"\n";
		code += "		            [   \n";
		code += "		              ask (run-result repactivityagents)\n";
		code += "		              [  \n";
		code += "		                if region = fromregion or region = toregion\n";
		code += "		                [  \n";
		code += "		                  if activity > 0 [ set activity activity - 1 ]\n";
		code += "		                ]\n";
		code += "		              ]  \n";
		code += "		            ]  \n";
		// code += "		          ]\n";
		code += "		        ]\n";
		code += "		      ]\n";
		code += "		    ]\n";
		code += "		  ] \n";
		code += "		end\n\n";
		return code;
	}

	private String generateCodeIncreasesAbundance(Agent ag, Agent ag2,
			KamStore kamStore) {
		String code = "";
		ArrayList<Annotation> annotations = getAnnotationsFromNodes(
				ag.getAgentInfoKamNode(), "increases",
				ag2.getAgentInfoKamNode(), kamStore);
		ArrayList<Region> validRegions = getRegionsFromAnnotations(annotations);
		if (verbous)
			code += "\t;;code source: KAM : agent increases agent\n";
		if (verbous)
			code += "\t ;;" + ag.getABMCodeLabel() + " increases "
					+ ag2.getABMCodeLabel() + "\n";
		
		int cnt = 0;
		if (ag2.isComplex() && ag2.getHasMember().size()>1){
			//order of agents in hasMember is important bec the resulting code needs to be executed by the 1st agent in the member list of ag2!
			//call complexFormationCode for code to be saved in all members in the member list (swap members, alternating first position)
			ArrayList<Agent> agentsDone = new ArrayList <Agent>();
			code += complexFormationCode(ag2, ag);   //complexFormationCode() "asks" ag to generate the complex ag2
			agentsDone.add(ag);
			@SuppressWarnings("unchecked")
			ArrayList<Agent> ag2members = (ArrayList<Agent>) ((ArrayList<Agent>) ag2.getHasMember()).clone();
			for (Agent a : ag2members){ 
				if (!agentsDone.contains(a)) a.addToGOCode( complexFormationCode(ag2, a) );   //complexFormationCode() "asks" a to generate the complex ag2
				ArrayList<Agent> tempMemberList = (ArrayList<Agent>) ag2.getHasMember();
				tempMemberList.remove(0); // this is agent a
				tempMemberList.add(a);    //remove from first position and add as last item
				ag2.setHasMember(tempMemberList);
				agentsDone.add(a);
			}
			//restore to original order
			ag2.setHasMember(ag2members);
		}
		cnt = 0;
		if (ag2.isComposite() && ag2.getIncludes().size()>1){
			//if ag2.getincludes.get(1) == ag then swap included agents
			//order of agent in includes is important!
			ArrayList<Agent> agentsDone = new ArrayList <Agent>();
			code += compositeFormationCode(ag2, ag);   //complexFormationCode() "asks" ag to generate the complex ag2
			agentsDone.add(ag);
			@SuppressWarnings("unchecked")
			ArrayList<Agent> ag2members = (ArrayList<Agent>) ((ArrayList<Agent>) ag2.getIncludes()).clone();
			for (Agent a : ag2members){ 
				if (!agentsDone.contains(a)) a.addToGOCode( compositeFormationCode(ag2, a) );   //complexFormationCode() "asks" a to generate the complex ag2
				ArrayList<Agent> tempMemberList = (ArrayList<Agent>) ag2.getIncludes();
				tempMemberList.remove(0); // this is agent a
				tempMemberList.add(a);    //remove from first position and add as last item
				ag2.setIncludes(tempMemberList);
				agentsDone.add(a);
			}
			//restore to original order
			ag2.setIncludes(ag2members);
		}
		if (!ag2.isComplex() && !ag2.isComposite()){
			if ( validRegions != null && validRegions.size() > 0)
				for (Region re : validRegions) {
					if (!ag.IsInActive()) {
						code += "\tif random 100 < activity \n"; // AND patch hat
																	// region = re
						code += "\t[\n";
					}
					code += "\t if region = \"" + re.getABMCodeLabel() + "\"\n";
					code += "\t  [\n";
					code += "\t    ask patch-here \n";
					code += "\t    [\n";
					code += "\t          sprout-" + ag2.getABMCodeLabel()
							+ "s 1 [ lt 45  \n";
					code += "\t             set color " + ag2.getColorString()
							+ "\n";
					code += "\t             set size " + ag2.getSize() + "\n";
					code += "\t             set energy random (2 * lifespan-"
							+ ag2.getABMCodeLabel() + ")\n";
					if (!ag2.IsInActive())
						code += "\t             set activity random 100\n";
					code += "\t        ]\n";
					code += "\t    ]\n";
					code += "\t  ]\n";
					if (!ag.IsInActive())
						code += "\t]\n";
				}
			else {
				if (!ag.IsInActive()) {
					code += "\tif random 100 < activity \n";
					code += "\t  [\n";
				}
	
				code += hatchagent(ag2, 1, ag2.getColorString(), "random 100",
						ag2.getSize(),
						"random (2 * lifespan-" + ag2.getABMCodeLabel() + ")", "0", "0", "0", "null")
						+ "\n";
				if (!ag.IsInActive())
					code += "\t  ]\n";
			}
		}
		return code;
	}

	private void doSetUp(List<Agent> agents2) {
		String code = "\n\n";
		if (verbous)
			code += " ;; doSetUp()\n";
		code += "to setup\n";
		// code += "\tclear-all\n";
		code += "\tclear-all\n";
		code += "\trandom-seed 1000\n";
		String label;
		// set up the homeostatic concentrations
		for (Agent a : agents2) {
			if (a.hasHomeostaticConcentration())
				code += "\tset homeostatic-" + a.getABMCodeLabel() + " "
						+ a.getHomeostaticConcentration() + "\n";
			if (a.hasMaxHomeostaticConcentration())
				code += "\tset maxhomeostatic-" + a.getABMCodeLabel() + " "
						+ a.getMaxHomeostaticConcentration() + "\n";
			
		}

		// create plot regions with different patch colors
		// according to number of regions used in KAM + KAM agent ontology
		// regional annotations
		String reg = "";
		if (this.regions.size() < 1) {
			this.regions.add(new Region("def_region", null));
		}
		for (Region r : this.regions)
			reg += "\"" + r.getABMCodeLabel() + "\" ";

		code += "\n\tset regions [ " + reg + " ]\n";

		code += "\task patches [ set pcolor white] \n";

		code += "\t;; first, colour one patch per region\n";
		code += "\t  foreach regions  \n";
		code += "\t[ ask one-of patches with [pcolor = white]\n";
		code += "\t    [ set pcolor 9 + 10 * random 14 \n";
		code += "\t      set region ?\n";
		code += "\t      set plabel region\n";
		code += "\t      ]\n";
		code += "\t  ]\n";
		code += "\n\t  ;; then, spread colours to neighbouring white patches\n";
		code += "\t   while [ count patches with [pcolor = white] > 0 ]\n";
		code += "\t   [ \n";
		code += "\t     ask patches with [pcolor != white]  ;; ask coloured patches\n";
		code += "\t     [ \n";
		code += "\t       ;; if neighbour is white, change his colour\n";
		code += "\t       if one-of neighbors with [pcolor = white] != nobody\n";
		code += "\t       [ ask one-of neighbors with [pcolor = white]\n";
		code += "\t         [ set pcolor [pcolor] of myself \n";
		code += "\t           set region [region] of myself ]\n";
		code += "\t       ]\n";
		code += "\t     ]\n";
		code += "\t   ]\n";
		code += "\t  ;;some existing shapes: default airplane arrow box bug butterfly car circle cow cylinder fish flag flower house leaf line pentagon person plant police sheep square star target tree triangle truck turtle wheel wolf x\n";
		for (Agent ag : agents2) {
			if (!ag.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents)) {
				label = ag.getABMCodeLabel();
				if (ag.isHomeostaticControl())
					code += "\tset-default-shape " + label + "s \"x\"\n";
				else
					code += "\tset-default-shape " + label + "s \"dot\"\n";
			}
		}

		code += insertAgents(this.agents, false, true);
		
		code += "\treset-ticks\n";
		code += "\tif file-exists? \"locations.txt\" [ file-close file-delete \"locations.txt\" ]\n";
		code += "\tfile-open \"locations.txt\"\n";
		code += "\tfile-write \"tick\" file-write \"agent-id\" file-write \"type\" file-write \"x\" file-write \"y\" file-write \"energy\" file-write \"activity\" file-print \"\"\n";
		
				  
				  
		code += "end\n";
		addToCode(code);
	}

	/**
	 * inserts new agents for hemopoiesis or other developmental fisiological
	 * process; according to reproduceAlgorithm condition if condition == true
	 * (eg stochastic pulse trains) if the agent has a produced_in region (from
	 * ontology), it is only inserted in this region/these regions else it is
	 * inserted in all regions possible for that agent
	 * 
	 * @param init
	 *            if true the method inserts the initial number of agents
	 *            (controlled via sliders)
	 * 
	 * @return
	 */
	private String insertAgents(List<Agent> agentsToIntroduce,
			Boolean condition, Boolean init) {
		String code = "";
		String label;
		int cnt = 0;
		
		for (Agent ag : agentsToIntroduce) {
			if (ag.isDisconnected(this.reactions, (ArrayList<Agent>) this.agents) && !ag.isConnectedViaOntology()){
				continue;
			}
			// agent introduction and re-introduction
			if (condition) {
				code += (" ;; agent introduction according to reproduceAlgorithm - eg stochastic pulse trains\n");
				code += ("\t if  " + this.reproductionCondition + "\n");
				code += ("\t[\n");
			}	
			label = ag.getABMCodeLabel();
			ArrayList<Region> regionsToBeTraversed;
			if (ag.getProductionRegions() != null
					&& ag.getProductionRegions().size() > 0) {
				regionsToBeTraversed = ag.getProductionRegions();
			} else if (ag.getRegions() != null && ag.getRegions().size() > 0) {
				regionsToBeTraversed = ag.getRegions();
			} else {
				regionsToBeTraversed = this.getregions(); // all regions
			}

			// instantiate the agent either on the regions assigned to it, or
			// spread evenly on all regions if no info available
			for (Region r : regionsToBeTraversed) {
				if (init) {
					code += ("\trepeat ini-no-" + ag.getABMCodeLabel() + " / "
							+ regionsToBeTraversed.size() + "\n");
				} else {
					if (ag.hasMaxHomeostaticConcentration())
						code += ("\trepeat maxhomeostatic-"
								+ ag.getABMCodeLabel() + " / "
								+ regionsToBeTraversed.size() + "\n");
					else
						code += ("\trepeat homeostatic-" + ag.getABMCodeLabel()
								+ " / " + regionsToBeTraversed.size() + "\n");
				}
				code += ("\t  [\n");
				code += ("\t    ask one-of patches with [region = \""
						+ r.getABMCodeLabel() + "\"]\n");
				code += ("\t    [\n");
				code += ("\t	     sprout-" + label + "s 1 [ \n");
				// code += "\tcreate-"+label+"s random 100\n\t[\n";
				if (ag.getChosenByUser()) { // otherwise the agent won't be
											// plotted and won't need a color
											// nor a size
					code += ("\t	       set color " + ag.getColorString() + "\n");
					code += ("\t	       set size " + ag.getSize() + "\n");
				}
				if (!ag.getChosenByUser()) {
					code += ("\t	       set size " + ag.getSize() + "\n");
				}
				code += ("\t	       set energy random (2 * lifespan-"
						+ ag.getABMCodeLabel() + ")\n");
				if (!ag.IsInActive())
					code += ("\t	       set activity random 100\n");
				if (ag.isComplex() && ag.getHasMember().size() == 2){
					code += ("\t	       set "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies []\n");
					code += ("\t	       set "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput random (2 * lifespan-"+ag.getHasMember().get(0).getABMCodeLabel()+") "+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n");
					if(ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel())){
						code += ("\t	       set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies []\n");
						code += ("\t	       set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies fput random (2 * lifespan-"+ag.getHasMember().get(1).getABMCodeLabel()+") "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n");
					}
					else{
						code += ("\t	       set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies []\n");
						code += ("\t	       set "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies fput random (2 * lifespan-"+ag.getHasMember().get(1).getABMCodeLabel()+") "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n");
					}
				}
				if (ag.isComposite() && ag.getIncludes().size() == 2){
					code += ("\t	       set "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies []\n");
					code += ("\t	       set "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies fput random (2 * lifespan-"+ag.getIncludes().get(0).getABMCodeLabel()+") "+ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies\n");
					if(ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel())){
						code += ("\t	       set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies []\n");
						code += ("\t	       set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies fput random (2 * lifespan-"+ag.getIncludes().get(1).getABMCodeLabel()+") "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies\n");
					}
					else{
						code += ("\t	       set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies []\n");
						code += ("\t	       set "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies fput random (2 * lifespan-"+ag.getIncludes().get(1).getABMCodeLabel()+") "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies\n");
					}
				}
				code += ("\t      ]\n");
				code += ("\t    ]\n");
				code += ("\t  ]\n");
			}
			cnt++;
			if (cnt == colors.length)
				cnt = 0;
			if (condition)
				code += ("\t]\n");
		}
		
		return code;
	}

	/**
	 * reads MathML code from either reproduceAlgoClass or
	 * reproduceAlgoIndividual (thus, one of them need to be null)
	 * 
	 * @param theontmodel
	 * @param mathmlClass
	 *            OntClass that has the MathML code attached; null else
	 * @param mathmlIndividual
	 *            Individual that has the MathML code attached; null else
	 * @return NetLogo Math code (String)
	 */
	private String parseMathML(OntModel theontmodel, OntClass mathmlClass,
			Individual mathmlIndividual) {
		String netlogomath = "";
		// System.out.println("reproducealgoclass: "+reproduceAlgoClass);
		// System.out.println("reproducealgoindividual: "+reproduceAlgoIndividual.getLabel(null));
		AnnotationProperty aprop;
		NodeIterator mathmlcode = null;
		for (String uristring : this.mathmlProp) {
			// System.out.println("mathml uri string: "+uristring);
			// now get the MathML code attached to this algorithm class
			aprop = theontmodel.getAnnotationProperty(uristring);// Ontology.getAnnoPropFromURIString(theontmodel,
																	// uristring);
			if (aprop == null)
				System.err
						.println(" mathml property class not found. Check owl for owl:AnnotationProperty vs rdf:Description ");
			if (mathmlClass != null)
				mathmlcode = mathmlClass.listPropertyValues(aprop);
			if (mathmlIndividual != null)
				mathmlcode = mathmlIndividual.listPropertyValues(aprop);
			// mathmlcode =
			// reproduceAlgoIndividual.getPropertyResourceValue(aprop);
			if (!mathmlcode.hasNext())
				continue;
			// Converter converter = Converter.getInstance();

			try {
				// if there is more than 1 mathml code attached, I consider only
				// the first
				RDFNode thecode = mathmlcode.next();
				Utils.appendToFile(
						new StringBuffer().append("\nmathmlcode: " + thecode
								+ "\n"), logging);
				Utils.appendToFile(new StringBuffer()
						.append("\n  translated to NetLogo math:\n"), logging);
				// Document doc =
				// MathMLParserSupport.parseString(thecode.toString());
				// doc.normalizeDocument();
				// Element docEle = doc.getDocumentElement();
				// NodeList children = docEle.getChildNodes();
				// Node n;
				// renderMathML(children, "");
				// ///////// MathMLParser (MathML -> LaTex) implementation by
				// Tilman Walther; substitutions file modified to output NetLogo
				// math
				MathMLParser mathmlparser = new MathMLParser();
				netlogomath = mathmlparser.parse(
						new StringBuffer(thecode.toString()), false, true);

				// replace var[0-9]* mentions with parameters from .ini settings
				// file
				if (this.agentReproduceAlgo != null) {
					netlogomath = replaceVarsInNetLogoCode(netlogomath,
							this.agentReproduceAlgo);
				} else if (this.agentDefaultReproduceAlgo != null) {
					netlogomath = replaceVarsInNetLogoCode(netlogomath,
							this.agentDefaultReproduceAlgo);
				}

				Utils.appendToFile(
						new StringBuffer().append("\n  " + netlogomath + "\n"),
						logging);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return netlogomath;
	}

	/**
	 * input: NetLogo math without parameters set in .ini settings file (var1,
	 * var2 etc) output: NetLogo math with parameters inserted as from settings
	 * file
	 * 
	 * @param netlogomath
	 * @param settingsLine
	 * @return the NetLogo Math code with parameter values inserted
	 */
	private String replaceVarsInNetLogoCode(String netlogomath,
			ArrayList<String> settingsLine) {
		String[] splitcode = netlogomath.split(" ");
		netlogomath = "";
		for (int i = 1; i < settingsLine.size(); i++) {
			for (int j = 0; j < splitcode.length; j++) {
				if (splitcode[j].contains("var")) {
					splitcode[j] = splitcode[j].replaceFirst("var[0-9]*",
							settingsLine.get(i));
					break;
				}
			}
		}
		for (int i = 0; i < splitcode.length; i++)
			netlogomath += splitcode[i] + " ";
		return netlogomath;
	}

	private void initPlotting(List<Agent> agents2) {
		// initialize plotting
		String code = "\n";
		if (verbous)
			code += " ;; initPlotting()\n";
		code += "to do-plotting\n";
		code += "  set-current-plot \"populations\"\n";
		String label;
		for (Agent ag : agents2) {
			if (ag.getChosenByUser()) {
				label = ag.getABMCodeLabel();
				code += "  set-current-plot-pen \"" + label + "\"\n";
				code += "  plot count " + label + "s \n";
			}
		}
		code += "end\n";
		addToCode(code);
	}

	private void setGlobalVars() {
		Utils.appendToFile(
				new StringBuffer().append("\nLooking for global variables: \n"),
				logging);
		// globals ganz oben:
		// globals [recoverable unrecoverable total initmyelin] ;; keep track of
		// how much myelin there is
		String code = "\n";
		if (verbous)
			code += " ;; setGlobalVars()\n";
		code += "globals [";
		code += " regions";

		for (Agent ag : this.agents) {
			code += " homeostatic-" + ag.getABMCodeLabel();
			code += " maxhomeostatic-" + ag.getABMCodeLabel();
		}
		code = code.trim() + "]\n";
		code += "turtles-own [energy activity memberdist] \n";
		code += "patches-own [region]\n";
		
		for (Agent ag : this.agents){
			if (ag.isComplex() && ag.getHasMember().size() == 2){
				if(ag.getHasMember().get(0).getABMCodeLabel().equals(ag.getHasMember().get(1).getABMCodeLabel())){
					code += ag.getABMCodeLabel()+"s-own ["+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_energies] \n";
					//code += ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_activities "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s2_activities]\n";
				}
				else {
				    code += ag.getABMCodeLabel()+"s-own ["+ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_energies "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_energies] \n";
				    //code += ag.getHasMember().get(0).getABMCodeLabel().toLowerCase()+"s_activities "+ag.getHasMember().get(1).getABMCodeLabel().toLowerCase()+"s_activities]\n";
				}
			}
			if (ag.isComposite() && ag.getIncludes().size() == 2){
				if (ag.getIncludes().get(0).getABMCodeLabel().equals(ag.getIncludes().get(1).getABMCodeLabel())){
					code += ag.getABMCodeLabel()+"s-own ["	+ ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_energies] \n";
					//code += ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_activities "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s2_activities]\n";
				}
				else {
					code += ag.getABMCodeLabel()+"s-own ["	+ ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_energies "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_energies] \n";
					//code += ag.getIncludes().get(0).getABMCodeLabel().toLowerCase()+"s_activities "+ag.getIncludes().get(1).getABMCodeLabel().toLowerCase()+"s_activities]\n";
				}
			}
		}
		
		addToCode(code);
		Utils.appendToFile(new StringBuffer()
				.append("\tGlobal variable(s) found: " + code), logging);
	}

	/**
	 * creates a breed for every agent
	 * 
	 * @param agents2
	 */
	private void initAgents(List<Agent> agents2) {
		String label;
		try {
			String code = "\n";
			if (verbous)
				code += " ;; initAgents()\n";
			for (Agent ag : agents2) {
				label = ag.getABMCodeLabel();// //////////////////
				code += "breed [" + label + "s " + label + "]\n";

			}
			addToCode(code);
		} catch (NullPointerException np) {
			np.printStackTrace();
			Utils.appendToFile(
					new StringBuffer()
							.append("GRAVE ERROR: initialization of agents failed. Exiting.\n"),
					logging);
			System.exit(-1);
		}
	}

	/**
	 * fills bioProcess.increasesBioProcess and bioProcess.decreasesBioProcess
	 * traverses the KAM
	 * 
	 * @param kam
	 */
	public void setBioProcessBehaviour(Kam kam) {
		Utils.appendToFile(new StringBuffer()
				.append("\n\nSETTING BIOPROCESS BEHAVIOURS:\n"), logging);
		//TODO process -> or -| reaction
		List<BioProcess> increasesBp = new ArrayList<BioProcess>();
		List<BioProcess> decreasesBp = new ArrayList<BioProcess>();
		List<BioProcess> increasedByBp = new ArrayList<BioProcess>();
		List<BioProcess> decreasedByBp = new ArrayList<BioProcess>();
		for (BioProcess bp : this.bioProcesses) {
			increasesBp = new ArrayList<BioProcess>();
			increasedByBp = new ArrayList<BioProcess>();
			decreasesBp = new ArrayList<BioProcess>();
			decreasedByBp = new ArrayList<BioProcess>();
			for (KamEdge e : kam.getAdjacentEdges(bp.getProcessInfoKamNode())) {
				if (e.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
						|| e.getRelationshipType() == RelationshipType.INCREASES) {
					// bp increases another process
					if (e.getSourceNode() == bp.getProcessInfoKamNode()
							&& this.bioProcesses.contains(Utils
									.getBioProcessByNode(e.getTargetNode(),
											this.bioProcesses))) {
						increasesBp.add(Utils.getBioProcessByNode(
								e.getTargetNode(), this.bioProcesses));
						Utils.appendToFile(new StringBuffer().append("\t"
								+ bp.getABMCodeLabel()
								+ " increases "
								+ Utils.getBioProcessByNode(e.getTargetNode(),
										this.bioProcesses).getABMCodeLabel()
								+ "\n"), logging);
					}
					// bp increasedBy another process
					if (e.getTargetNode() == bp.getProcessInfoKamNode()
							&& this.bioProcesses.contains(Utils
									.getBioProcessByNode(e.getSourceNode(),
											this.bioProcesses))) {
						increasedByBp.add(Utils.getBioProcessByNode(
								e.getSourceNode(), this.bioProcesses));
						Utils.appendToFile(new StringBuffer().append("\t"
								+ bp.getABMCodeLabel()
								+ " increased by "
								+ Utils.getBioProcessByNode(e.getSourceNode(),
										this.bioProcesses).getABMCodeLabel()
								+ "\n"), logging);
					}
				}
				if (e.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES
						|| e.getRelationshipType() == RelationshipType.DECREASES) {
					// bp decreases another process
					if (e.getSourceNode() == bp.getProcessInfoKamNode()
							&& this.bioProcesses.contains(Utils
									.getBioProcessByNode(e.getTargetNode(),
											this.bioProcesses))) {
						decreasesBp.add(Utils.getBioProcessByNode(
								e.getTargetNode(), this.bioProcesses));
						Utils.appendToFile(new StringBuffer().append("\t"
								+ bp.getABMCodeLabel()
								+ " decreases "
								+ Utils.getBioProcessByNode(e.getTargetNode(),
										this.bioProcesses).getABMCodeLabel()
								+ "\n"), logging);
					}
					// bp decreasedBy another process
					if (e.getTargetNode() == bp.getProcessInfoKamNode()
							&& this.bioProcesses.contains(Utils
									.getBioProcessByNode(e.getSourceNode(),
											this.bioProcesses))) {
						decreasedByBp.add(Utils.getBioProcessByNode(
								e.getSourceNode(), this.bioProcesses));
						Utils.appendToFile(new StringBuffer().append("\t"
								+ bp.getABMCodeLabel()
								+ " decreased by "
								+ Utils.getBioProcessByNode(e.getSourceNode(),
										this.bioProcesses).getABMCodeLabel()
								+ "\n"), logging);
					}
				}
			}
			bp.setIncreasesBioProcess(increasesBp);
			bp.setIncreasedByBioProcess(increasedByBp);
			bp.setDecreasesBioProcess(decreasesBp);
			bp.setDecreasedByBioProcess(decreasedByBp);
		}

	}

	public void setVerbous(boolean b) {
		verbous = true;
	}

	/**
	 * @return the kamRegions
	 */
	private ArrayList<Region> getregions() {
		return regions;
	}

	public void addRegions(ArrayList<Region> regions) {
		this.regions.addAll(regions);
	}

	public void addRegion(Region region) {
		this.regions.add(region);
	}

	/**
	 * this is called by BEL2ABM, doesn't write any code example:
	 * translocation(abundance(15),29,40) translocates abundance(15) goes
	 * through all translocations and saves them as Translocation type inside
	 * the agent together with from, to, and any additional annotation info
	 */
	public void generateTranslocationInfo(KamStore kamStore) {
		Utils.appendToFile(
				new StringBuffer()
						.append("\nScanning for agent translocations from region A to region B (translocation(abundance(15),29,30) translocates abundance(15) )\n"
								+ " and for translocation effects (translocation(abundance(15),29,30) increases biologicalProcess(27)).\n"),
				logging);
		Set<KamEdge> edges;
		KamNode translocatedNode;
		String namefrom;
		String nameto;
		String dictionaryfrom;
		String dictionaryto;
		for (Agent ag : this.agents) {
			edges = ag.getAgentInfoKamNode().getKam()
					.getAdjacentEdges(ag.getAgentInfoKamNode());
			// System.out.println("\ncurrent agent: "+ag.getBELTermLabel()+" "+ag.getBELIdLabel());
			for (KamEdge thisedge : edges) { // edges : adjacent edges of "this"
												// node
				// System.out.println(thisedge.getSourceNode()+" "+thisedge.getRelationshipType()+" "+thisedge.getTargetNode());
				if (thisedge.getRelationshipType() == RelationshipType.TRANSLOCATES) {
					translocatedNode = thisedge.getTargetNode();
					if (thisedge.getSourceNode().getFunctionType() == FunctionEnum.TRANSLOCATION) {
						Translocation transloc = new Translocation();
						// save all annotations, may be interesting for later
						// use
						ArrayList<Annotation> annos = getAnnotationsFromNodes(
								thisedge.getSourceNode(), "translocates",
								translocatedNode, kamStore);
						transloc.settNode(thisedge.getSourceNode());
						for (Annotation a : annos) {
							transloc.addAnnotation(a);
						}
						// find from and to, add to transloc
						try {
							List<BelTerm> terms = kamStore
									.getSupportingTerms(thisedge
											.getSourceNode());
							for (BelTerm t : terms) {
								namefrom = "";
								dictionaryfrom = "";
								nameto = "";
								dictionaryto = "";
								namefrom = t.getLabel().split(",")[1];
								namefrom = namefrom.substring(0,
										namefrom.length());
								if (namefrom.contains(":")) {
									dictionaryfrom = namefrom.split(":")[0];
									namefrom = namefrom.split(":")[1];
								}
								nameto = t.getLabel().split(",")[2];
								nameto = nameto.substring(0,
										nameto.length() - 1);
								if (nameto.contains(":")) {
									dictionaryto = nameto.split(":")[0];
									nameto = nameto.split(":")[1];
								}
								namefrom = namefrom.replaceAll("\"", "");
								nameto = nameto.replaceAll("\"", "");
								// System.out.println("  "+namefrom +
								// " to "+nameto);
								if (Region.exists(this.regions, namefrom,
										dictionaryfrom)) {
									Region e = Region.getRegion(this.regions,
											namefrom, dictionaryfrom);
									transloc.setFrom(e);
								}
								if (Region.exists(this.regions, nameto,
										dictionaryto)) {
									Region e = Region.getRegion(this.regions,
											nameto, dictionaryto);
									transloc.setTo(e);
								}
								transloc.setTranslocatedAgent(ag);
								ag.addTranslocation(transloc);

								// check what translocations do, save this in
								// the translocation
								Set<KamEdge> edgesAdjacentToTransloc = ag
										.getAgentInfoKamNode().getKam()
										.getAdjacentEdges(transloc.gettNode());
								for (KamEdge thisadjacentedge : edgesAdjacentToTransloc) {
									Agent targetAgent = Utils.getAgentByNode(
											thisadjacentedge.getTargetNode(),
											this.agents);
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.INCREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
											&& targetAgent != null
											&& Utils.isAgent(targetAgent
													.getAgentInfoOntClass(),
													this.agents)) {
										// tloc -> abundance(agent)
										transloc.setIncreasesAgent(Utils.getAgentByNode(
												thisadjacentedge
														.getTargetNode(),
												this.agents));
										Utils.appendToFile(
												new StringBuffer().append("\t"
														+ transloc.gettNode()
																.getLabel()
														+ " increases "
														+ Utils.getAgentByNode(
																thisadjacentedge
																		.getTargetNode(),
																this.agents)
																.getABMCodeLabel()
														+ "\n"), logging);
									}
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.DECREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
											&& Utils.isAgent(
													Utils.getAgentByNode(
															thisadjacentedge
																	.getTargetNode(),
															this.agents)
															.getAgentInfoOntClass(),
													this.agents)) {
										// tloc -| abundance(agent)
										transloc.setDecreasesAgent(Utils.getAgentByNode(
												thisadjacentedge
														.getTargetNode(),
												this.agents));
										Utils.appendToFile(
												new StringBuffer().append("\t"
														+ transloc.gettNode()
																.getLabel()
														+ " decreases "
														+ Utils.getAgentByNode(
																thisadjacentedge
																		.getTargetNode(),
																this.agents)
																.getABMCodeLabel()
														+ "\n"), logging);
									}
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.INCREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
											&& Utils.isActivityNode(thisadjacentedge
													.getTargetNode())) {
										// tloc -> act(abundance(agent))
										Activity activ = Utils.getActivity(
												thisadjacentedge
														.getTargetNode(),
												this.agents);
										if (activ != null) {
											transloc.setIncreasesAgentActivity(activ);
											Utils.appendToFile(
													new StringBuffer()
															.append("\t"
																	+ transloc
																			.gettNode()
																			.getLabel()
																	+ " increases activity "
																	+ activ.getActivityNode()
																			.getLabel()
																	+ "\n"),
													logging);
										} else {
											// shouldn't happen because of KAM
											// expansion by the BEL Framework
										}
									}
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.DECREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
											&& Utils.isActivityNode(thisadjacentedge
													.getTargetNode())) {
										// tloc -| act(abundance(agent))
										Activity activ = Utils.getActivity(
												thisadjacentedge
														.getTargetNode(),
												this.agents);
										if (activ != null) {
											transloc.setDecreasesAgentActivity(activ);
											Utils.appendToFile(
													new StringBuffer()
															.append("\t"
																	+ transloc
																			.gettNode()
																			.getLabel()
																	+ " decreases activity "
																	+ activ.getActivityNode()
																			.getLabel()
																	+ "\n"),
													logging);
										} else {
											// shouldn't happen because of KAM
											// expansion by the BEL Framework
										}
									}
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.INCREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
											&& Utils.isBioProcess(
													Utils.getBioProcessByNode(
															thisadjacentedge
																	.getTargetNode(),
															this.bioProcesses)
															.getProcessInfoOntClass()
															.get(0),
													this.bioProcesses)) {
										// tloc -> bioprocess
										transloc.setIncreasesBioProcess(Utils.getBioProcessByNode(
												thisadjacentedge
														.getTargetNode(),
												this.bioProcesses));
										Utils.appendToFile(
												new StringBuffer()
														.append("\t"
																+ transloc
																		.gettNode()
																		.getLabel()
																+ " increases "
																+ Utils.getBioProcessByNode(
																		thisadjacentedge
																				.getTargetNode(),
																		this.bioProcesses)
																		.getABMCodeLabel()
																+ "\n"),
												logging);
									}
									if (thisadjacentedge.getSourceNode() == transloc
											.gettNode()
											&& (thisadjacentedge
													.getRelationshipType() == RelationshipType.DECREASES || thisadjacentedge
													.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
											&& Utils.isBioProcess(
													Utils.getBioProcessByNode(
															thisadjacentedge
																	.getTargetNode(),
															this.bioProcesses)
															.getProcessInfoOntClass()
															.get(0),
													this.bioProcesses)) {
										// tloc -| bioprocess
										transloc.setDecreasesBioProcess(Utils.getBioProcessByNode(
												thisadjacentedge
														.getTargetNode(),
												this.bioProcesses));
										Utils.appendToFile(
												new StringBuffer()
														.append("\t"
																+ transloc
																		.gettNode()
																		.getLabel()
																+ " decreases "
																+ Utils.getBioProcessByNode(
																		thisadjacentedge
																				.getTargetNode(),
																		this.bioProcesses)
																		.getABMCodeLabel()
																+ "\n"),
												logging);
									}
								}
							}
						} catch (KamStoreException e) {
							Utils.appendToFile(
									new StringBuffer()
											.append("\nKamStoreException when adding tloc() info to agents "
													+ ag.getABMCodeLabel()
													+ " \n"), logging);
							// e.printStackTrace();
						}
					}
				}
			}
		}

	}

	/**
	 * changes the labels of composite and complex abundance to be a simpler
	 * combination of their components
	 */
	private void adjustAgentABMCodeLabels(ArrayList<Agent> ags) {
		// change agent label (too long)
		Utils.appendToFile(
				new StringBuffer()
						.append("\n\tChanging labels of composite and complex abundances:\n"),
				logging);
		List<Agent> memberList = new ArrayList<Agent>();
		String newLabel = "";
		ArrayList<Agent> later = new ArrayList<Agent>();
		boolean skip = false;
		for (Agent ag : ags) {
			skip = false;
			newLabel = "";
			if (ag.isComposite())
				memberList = ag.getIncludes();
			if (ag.isComplex())
				memberList = ag.getHasMember();
			if (memberList.size() < 1 || (!ag.isComplex() && !ag.isComposite()))
				continue;
			for (Agent a : memberList){
				//do complex(complex(...)) later, otherwise labels will be ugly
				if ((a.isComplex() || a.isComposite()) && !a.isLabelAdjusted()){
					later.add(ag);
					skip = true;
					break;
				}
			}
			if (skip) continue;
			if (memberList.size() == 1)
				newLabel += "c." +memberList.get(0).getABMCodeLabel();
			else 
				for (Agent memberagent : memberList) {
					newLabel += memberagent.getABMCodeLabel() + ".";
				}
			if (newLabel.endsWith("."))
				newLabel = newLabel.substring(0, newLabel.length() - 1);
			ag.setABMCodeLabel(newLabel);
			ag.setLabelAdjusted(true);
			Utils.appendToFile(
					new StringBuffer().append("\tABM code label of "
							+ ag.getBELTermLabel() + " changed to "
							+ ag.getABMCodeLabel() + "\n"), logging);
		}
		if (later.size() > 0)
			adjustAgentABMCodeLabels(later);
	}

	/**
	 * goes through the KAM and creates a Reaction instance for every reaction found
	 * saved in reactions ArrayList
	 * @param kam
	 */
	public void generateReactions(Kam kam) {
		Utils.appendToFile(
				new StringBuffer().append("\n\tScanning for agent participation in reactions.\n"), logging);
		Collection<KamNode> allNodes = kam.getNodes();
		for (KamNode n : allNodes){
			if (n.getFunctionType() == FunctionEnum.REACTION){  //n is the reaction node
				//System.out.println( "reaction "+n);
				Set<KamEdge> adjEdges = kam.getAdjacentEdges(n);
				for (KamEdge nextEdge : adjEdges){
					if (nextEdge.getRelationshipType() == RelationshipType.REACTANT_IN 
							&& Utils.isAbundance(nextEdge.getSourceNode())){
						//System.out.println("   reactantin "+n.getId()+" "+nextEdge.getSourceNode()+" "+nextEdge.getRelationshipType()+" "+nextEdge.getTargetNode());
						//if a reactant is mentioned more than once in the reaction, the BEL compiler will only return it once
						//thus I need to parse the reaction String to check for multiple occurrence of the reactant
						String reactantsString = n.toString().substring(n.toString().indexOf("reactants")+9);
						reactantsString = reactantsString.substring(1, Utils.getEndBracketPosition(reactantsString));

						//complexAbundance(proteinAbundance(22),proteinAbundance(20)) reactantIn reaction(reactants(complexAbundance(pro...
						//System.out.println(getReactions().size());
						if (!Reaction.exists(getReactions(), n.getId())){
							ArrayList<Agent> reactants = new ArrayList<Agent>();
							//add the reactant as many times as it appears in reactantsString
							int numberOccurrences = Utils.getNumberOfOccurrences(nextEdge.getSourceNode().toString(), reactantsString);
							for (int i = 0; i < numberOccurrences; i++) {
								reactants.add(Utils.getAgentByNode(nextEdge.getSourceNode(), agents));
							 }
							Reaction r = new Reaction(n.getId(), 
									reactants, 
									new ArrayList<Agent>() );
							//System.out.println("new reaction "+n.getId());
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							//System.out.println("hier unten "+r.getID());
							ArrayList<Agent> reactants = new ArrayList<Agent>();
							reactants.addAll(r.getReactants());
							//add the reactant as many times as it appears in reactantsString
							int numberOccurrences = Utils.getNumberOfOccurrences(nextEdge.getSourceNode().toString(), reactantsString);
							for (int i = 0; i < numberOccurrences; i++) {
								reactants.add(Utils.getAgentByNode(nextEdge.getSourceNode(), agents));
							 }
							r.setReactants(reactants);
						}
					}
					if (nextEdge.getRelationshipType() == RelationshipType.HAS_PRODUCT
							&& Utils.isAbundance(nextEdge.getTargetNode())){
						//if a product is mentioned more than once in the reaction, the BEL compiler will only return it once
						//thus I need to parse the reaction String to check for multiple occurrence of the product
						String productsString = n.toString().substring(n.toString().indexOf("products")+8);
						productsString = productsString.substring(1, Utils.getEndBracketPosition(productsString));
						//reaction(reactants(complexAbundance(proteinAbundance(20),proteinAbundance(21))),products(proteinAbundance(19),proteinAbundance(21))) hasProduct proteinAbundance(21) 
						if (!Reaction.exists(getReactions(), n.getId())){
							ArrayList<Agent> products = new ArrayList<Agent>();
							//add the product as many times as it appears in productssString
							int numberOccurrences = Utils.getNumberOfOccurrences(nextEdge.getTargetNode().toString(), productsString);
							for (int i = 0; i < numberOccurrences; i++) {
								products.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							 }
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									products );
							//System.out.println( "-- neue reaction mit id "+ n.getId());
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Agent> products = new ArrayList<Agent>();
							products.addAll(r.getProducts());
							//add the product as many times as it appears in productssString
							int numberOccurrences = Utils.getNumberOfOccurrences(nextEdge.getTargetNode().toString(), productsString);
							for (int i = 0; i < numberOccurrences; i++) {
								products.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							 }
							r.setProducts(products);
						}
					}
					//save inside the reaction if it increases or decreases anything
					if ((nextEdge.getRelationshipType() == RelationshipType.INCREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
							&& Utils.isAbundance(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							r.setIncreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getIncreases();
							tmpList.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							r.setIncreases(tmpList);
						}
					}
					if ((nextEdge.getRelationshipType() == RelationshipType.DECREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
							&& Utils.isAbundance(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							r.setDecreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getDecreases();
							tmpList.add(Utils.getAgentByNode(nextEdge.getTargetNode(), agents));
							r.setDecreases(tmpList);
						}
					}
					if ((nextEdge.getRelationshipType() == RelationshipType.INCREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
							&& Utils.isBioProcess(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getBioProcessByNode(nextEdge.getTargetNode(), bioProcesses));
							r.setIncreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getIncreases();
							tmpList.add(Utils.getBioProcessByNode(nextEdge.getTargetNode(), bioProcesses));
							r.setIncreases(tmpList);
						}
					}
					if ((nextEdge.getRelationshipType() == RelationshipType.DECREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
							&& Utils.isBioProcess(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getBioProcessByNode(nextEdge.getTargetNode(), bioProcesses));
							r.setDecreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getDecreases();
							tmpList.add(Utils.getBioProcessByNode(nextEdge.getTargetNode(), bioProcesses));
							r.setDecreases(tmpList);
						}
					}
					if ((nextEdge.getRelationshipType() == RelationshipType.INCREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES)
							&& Utils.isActivityNode(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getActivity(nextEdge.getTargetNode(), agents));
							r.setIncreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getIncreases();
							tmpList.add(Utils.getActivity(nextEdge.getTargetNode(), agents));
							r.setIncreases(tmpList);
						}
					}
					if ((nextEdge.getRelationshipType() == RelationshipType.DECREASES || nextEdge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES)
							&& Utils.isActivityNode(nextEdge.getTargetNode())){
						if (!Reaction.exists(getReactions(), n.getId())){
							Reaction r = new Reaction(n.getId(), 
									new ArrayList<Agent>(), 
									new ArrayList<Agent>() );
							ArrayList<Object> tmpList = new ArrayList<Object>();
							tmpList.add(Utils.getActivity(nextEdge.getTargetNode(), agents));
							r.setDecreases(tmpList);
							ArrayList<Reaction> newlist = getReactions();
							newlist.add(r);
							setReactions(newlist);
						} else {
							Reaction r = Reaction.getReaction(getReactions(), n.getId());
							ArrayList<Object> tmpList = r.getDecreases();
							tmpList.add(Utils.getActivity(nextEdge.getTargetNode(), agents));
							r.setDecreases(tmpList);
						}
					}
				}
			}
		}
		Utils.appendToFile(
				new StringBuffer().append("\tFound "+getReactions().size()+" reactions:\n"), logging);
		for (Reaction r : getReactions()){
			Utils.appendToFile(
					new StringBuffer().append("\t\t"+r.getID()+" "+r.getReactants()+" "+r.getProducts()+"\n"), logging);
		}
	}

	public ArrayList<Reaction> getReactions() {
		return reactions;
	}

	public void setReactions(ArrayList<Reaction> reactions) {
		this.reactions = reactions;
	}

	/**
	 * to make the method accessible from BEL2ABM
	 */
	public void adjustAgentABMCodeLabels() {
		adjustAgentABMCodeLabels((ArrayList<Agent>) this.agents);
	}

}
