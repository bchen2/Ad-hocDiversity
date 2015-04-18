/**
 * 
 */
package AdhocCollaboration;

import java.util.ArrayList;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class SubTask {
	private int Id;  //subtask type id
	private int cap; //the required capability to finish this subtask, it has 1-1 correspondence to subtask type (Id)
	private int selectedNumAgentsCount; // number of agents has been selected
	private int numAgentsRequired; // The required number of agents to finish this subtask
	private double quality; // The required minimum quality of agents' capability to finish this subtask
//	private double localSat; //
	private int numOfAgentFinished;//when one agent successfully complete this subtask, this number +1 ,check if this equals numAgents required, if yes then this whole subtask is finished
	private ArrayList<Integer> failedAgentId; //if a agent failed this subtask, then its ID will be added into this arrayList
	private int subtaskFlag;// 1--finished , -1 -- failed, 0--unfinished
	private int QualifiedAgentsCount; //count number of qualified agents of all agents in the environment for this subtask
	private int QualifiedBiddingAgentsCount;
	
	
	public SubTask() {	
		Id 					= 0;
		cap 				= 0;
		selectedNumAgentsCount 	= 0;
		numAgentsRequired 			= 0;
		quality 			= 0.0;
		//localSat			= 0.0;
		subtaskFlag = 0;
		failedAgentId = new ArrayList<Integer>();
		QualifiedAgentsCount=0;
		QualifiedBiddingAgentsCount=0;
	}
	
	/**
	 * this method checks if the subtask is finished or not by comparing the numOfAgentFinished with the numAgentsRequired
	 */
	public Boolean SetNumOfAgentFinished(){
		numOfAgentFinished++;
		if (numOfAgentFinished==numAgentsRequired){
			subtaskFlag=1;
			return true;
		}
		return false ;
	}
	
	/**
	 * 
	 */
	public void setFailedAgentId(int agentId){
		failedAgentId.add(agentId);
		subtaskFlag = -1;
	}
	
	/**
	 * 
	 */
	public int getSubtaskFlag(){
		return subtaskFlag;
	}
	
	/**
	 * 
	 * @param id 	subtask id
	 */
	public void setId(int id) {
		Id = id;
	}
	
	/**
	 * 
	 * @return subtask id
	 */
	public int getId() {
		return Id;
	}

	/**  
	 * @param capId		The capability id required to complete this subtask
	 */
	public void setCap(int capId) {
		cap = capId;
	}
	
	/**
	 * @return 	The capability required to complete this subtask
	 */
	public int getCap() {
		return cap;
	}
	
	/** 
	 * @param num	The minimum number of agents with this capability required to complete this subtask 
	 */
	public void setNumAgents(int num) {
		numAgentsRequired = num;
	}
	
	/**
	 * @return The minimum number of agents with this capability required to complete this subtask 
	 */
	public int getNumAgents() {
		return numAgentsRequired;
	}
	
	/** 
	 * @param num	The minimum quality of the capability for this subtask that is required
	 */
	public void setQuality(double quality) {
		this.quality = quality;
	}
	
	/**
	 * 
	 * @return The minimum quality of the capability for this subtask that is required
	 */
	public double getQuality() {
		return quality;
	}
	
	/**
	 *  @param selectedAgents	The number of agents that selected this subtask 
	 */
	public void setSelectedNumAgentsCount(int selectedAgents) {
		selectedNumAgentsCount = selectedAgents;
	}
	
	/**
	 * 
	 */
	public void addSelectedAgentCount(){
		selectedNumAgentsCount++;
	}
	
	/**
	 * 
	 * @return	The number of agents that selected this subtask 
	 */
	public int getSelectedNumAgentsCount() {
		return selectedNumAgentsCount;
	}
	
	public ArrayList<Integer> getFailedAgentList(){
		return failedAgentId;
	}

	public int getQualifiedAgentsCount() {
		return QualifiedAgentsCount;
	}

	public void setQualifiedAgentsCount(int qualifiedAgentsCount) {
		QualifiedAgentsCount = qualifiedAgentsCount;
	}
	
	
	public void addQualfiedAgentsCount(){
		this.QualifiedAgentsCount++;
	}

	public int getQualifiedBiddingAgentsCount() {
		return QualifiedBiddingAgentsCount;
	}

	public void setQualifiedBiddingAgentsCount(int qualifiedBiddingAgentsCount) {
		QualifiedBiddingAgentsCount = qualifiedBiddingAgentsCount;
	}
	
	
	public void addQualifiedBiddingAgentsCount(){
		this.QualifiedBiddingAgentsCount++;
	}
	
	
	/**
	 * 
	 * @param sat	The local satisfaction of an agent
	 */
//	public void setLocalSatAllAgents(double sat) {
//		localSat += sat;
//	}
	
//	/**
//	 * 
//	 * @return The sum of local satisfaction of all agent
//	 */
//	public int getLocalSatAllAgents() {
//		return selectedNumAgentsCount;
//	}
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//
//	}

}
