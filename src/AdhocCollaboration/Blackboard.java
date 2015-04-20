/**
 * 
 */
package AdhocCollaboration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class Blackboard {
	private ArrayList<Agent> agentList;//use it to keep track of all the agents in the environment 
	public boolean newMsg;
	public boolean allHasResponsed;
	public boolean newAssignments;
	private int agentCount;
	private int numFinishedSubtasks;
	private int numFinishedTasks;
	private int numUnfinishedTasks;//the total number of returned messages
	private ArrayList<BlackboardMessage> messageList;
	private ArrayList<BlackboardMessage> messagesToBeReturned;
	public int numAgentsHasResponsed; // keeps track of the number of agents has selected task to bid, when all agents has bid on task, main agent will make the task assignment
	private HashMap<Integer,Agent> agentMap = new HashMap<Integer,Agent>();
	private double agentOpenness;//actual agentOpenness read from the configuration
	private double taskOpenness;//actual taskOpenness read from the configuration
	private Set<Integer> killedAgentSet;//used to calculate agents' perception of agent openness.record all the agents (its agent_id) who has left the environment
	private Set<Integer> sharedKnowAgentSet;//used to calculate agents' perception of agent openness. When an agent is reading the assignment,  we add its collaborators agent_id into sharedKnownAgentSet
	private Set<Integer> intersection;//defined as the intersection of killedAgentSet and sharedKnowAgentSet
//	private Parameters params;
	private double sharedAgentOpenness;//agents perception of agent openness, will be updated at each tick before main agent introduce a new task
	private double sharedTaskOpenness;//agents' perception of task openness,will be updated at each tick before main agent introduce a new task
	private Set<Integer> sharedNewTaskSet;
	private Set<Integer> sharedEncounteredTaskSet;
	private double totalLearnedCap;//keeps track of all the capabilities that agents gained from learning.
	private double totalLostCap;//when an agent leaves the environment, the capability this agent learned is considered lost cap.
	private int numUnassignedAgents;//the number of agents who never been assigned a job 
	private double U_learnTotal=0;
	private int numU_learn=0;
	private double U_solveTotal=0;
	private int numU_solve=0;
	

	
	//	private boolean matched;
//	private boolean matchAgents;
//	private int numFinishedTasks;
//	private int numFinishedSubtaskAgents;
//	private int agentTypeUtilityCount;
//	private LearningMatch learningTeaching;
//	private HashMap<Integer, Double> agentOpenness;
//	private HashMap<Integer, ArrayList<SubTask>> removableAgents; // agentId, list of subtasks to remove
//	private HashMap<Task, ArrayList<Double>> globalSatMap; // agentId, list of global sat values for subtasks
	//private HashMap<Task, HashMap<SubTask, HashMap<Integer,Double>>> allQuality;
//	private HashMap<Integer, SubTask> agentMatch;		// contains agents that have been matched up by the main agent
//	private HashMap<Integer,ArrayList<Double>> qualityMap;
//	private HashMap<Integer,Double> typeUtility;	// type id (total possible 31), utility (last equation)	
//	private ArrayList<Double> learningCost;			// learning utility, eq. 3
	
	public Blackboard(){
		agentList= new ArrayList<Agent> ();
    	newMsg 			= false;
    	allHasResponsed = false;
       	numFinishedSubtasks=0;
       	numFinishedTasks=0;
       	messageList = new ArrayList<BlackboardMessage>();
    	messagesToBeReturned = new ArrayList<BlackboardMessage>();
    	numAgentsHasResponsed = 0;
    	newAssignments = false;
    	numUnfinishedTasks = 0;
    	agentMap = new HashMap<Integer,Agent>();
    	agentOpenness 	= 1.0;
    	taskOpenness=1.0; 
    	killedAgentSet=new HashSet<Integer>();
    	sharedKnowAgentSet=new HashSet<Integer>();
    	intersection= new HashSet<Integer>();
    	sharedAgentOpenness=0;
    	sharedTaskOpenness=1;
    	sharedNewTaskSet= new HashSet<Integer>();
    	sharedEncounteredTaskSet=new HashSet<Integer>();
    	totalLearnedCap=0;
    	totalLostCap=0;
    	
    	 U_learnTotal=0;
    	 numU_learn=0;
    	 U_solveTotal=0;
    	 numU_solve=0;
    	
    	
    	//numUnassignedAgents=agentCount;
    	
//    	agentOpenness 	= (Double) params.getValue("agent_openness"); 
 //   	taskOpenness=(Double) params.getValue("task_openness"); 
//    	matched			= false;
//    	matchAgents		= false;
//    	numFinishedTasks= 0;
//    	numFinishedSubtaskAgents=0;
//    	agentTypeUtilityCount=0;   	
//		contains all the messages for unassigned tasks to be auctioned out, if an task is assigned, then it will be removed from this list,
//    	mainAgent has access to this list(adding new message for new task,  	
//      agentOpenness 	= new HashMap<Integer, Double>();
//      removableAgents = new HashMap<Integer, ArrayList<SubTask>>();
//      globalSatMap	= new HashMap<Task, ArrayList<Double>>();
//		allQuality		= new HashMap<Task, HashMap<SubTask, HashMap<Integer,Double>>>();
//      agentMatch		= new HashMap<Integer,SubTask>();
//      qualityMap		= new HashMap<Integer,ArrayList<Double>>();
//      typeUtility		= new HashMap<Integer,Double>();
//      learningCost	= new ArrayList<Double>();     
	}
	
	





	/**
	 * Post a list of BlackboardMessage to the blackboard
	 * @param messages	A message list contains all available tasks, including one new task and old unclaimed tasks
	 */
	public void post(ArrayList<BlackboardMessage> messages){
		messageList.addAll(messages);
		newMsg = true;  //this triggers agent to start doing their thing (either execute current task on hand or biding task)
	}
	
//	public void removeCompleteSubtask(BlackboardMessage message, SubTask subtask){
//		if(message.getSubtasks().contains(subtask)){
//			message.removeSubtask(subtask);
//		}
//		if(message.getSubtasks().size() == 0){
//			removeCompleteTask(message);
//		}
//	}
	
//	public void removeCompleteTask(BlackboardMessage message){
//		Task task = message.getTask();
//		message.getTask().setFinished();
//		task.setFinished();
// //   	task.setGlobalSat();
//    	messageList.remove(message);
//	}
	
	/**
	 * A simple algorithm only select top n agents for a subtask who has highest capability quality. 
	 * n is the required number of agents of  a subtask
	 *
	 */
	public void auction(){
		print("auction start!");
		//resetBlackboard();
		//newMsg = false;
		for(BlackboardMessage message : new ArrayList<BlackboardMessage>(messageList)){
			//Subtask assignment: subtaskId, agentId. Assign each subtask to a list of agents.
			HashMap<Integer, ArrayList<Integer>> assignment = message.getAssignment();
			ArrayList<Agent> agentBiddingList = new ArrayList<Agent>(message.getAgentBiddingList());
			InnerLoop:
			for(SubTask subtask : message.getSubtasks()){
				// sort the agent according to agent's required capability of completing this subtask
				Collections.sort(agentBiddingList, new AgentQualityComparator(subtask.getId()));
				ArrayList<Integer> subtaskSelectedAgents = new ArrayList<Integer>();
				//check if the number of agents who bid on this task is greater the required number of agents for this subtask
				if(agentBiddingList.size() >= subtask.getNumAgents()){
					if(agentBiddingList.get(subtask.getNumAgents() - 1).getCapabilityQuality(subtask.getId()) > subtask.getQuality()){
						for(int i = 0; i < subtask.getNumAgents(); i++){
							Agent agent = agentBiddingList.get(i);
							subtaskSelectedAgents.add(agent.getId());
							subtask.addSelectedAgentCount();
							//agent.setTaskToBeExecuted(message);
						}
						assignment.put(subtask.getId(), subtaskSelectedAgents);
					}
					else{
						message.removeAssignment();//this clears assignmentMap also clears agentBiddingList
						messagesToBeReturned.add(message);
						break InnerLoop;
					}
				}
				else{
					//this subtask can not be finished due to lack of agent. So the entire task can not be finished.
					message.removeAssignment();
					messagesToBeReturned.add(message);
					break InnerLoop;
				}
			}
			//message.setAssignment(assignment);
			messageList.remove(message);
			message.printAssignment();
//			print(" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality););
			message.PrintTaskDetail();
			message.PrintSubtaskQualificationAllAgentsMap();
			message.PrintSubtaskQualificationBiddingAgentsMap(agentBiddingList);
			print("# of agent Bid for this Task = "+agentBiddingList.size()+"\n");
		}	
		print("All acution has done!");
		newAssignments = true;
	}
	
	
	
	public Set<Integer> getKilledAgentSet() {
		return killedAgentSet;
	}



//	public Set<Integer> getSharedKnowAgentSet() {
//		return sharedKnowAgentSet;
//	}



	public double getSharedAgentOpenness() {
		return sharedAgentOpenness;
	}
	
	public double updateSharedAgentOpenness1(){
		
		
		if (this.sharedKnowAgentSet.size()!=0){
			sharedAgentOpenness=(double) killedAgentSet.size()/this.sharedKnowAgentSet.size();
		}
		else{
			sharedAgentOpenness=0;
		}
		
		return sharedAgentOpenness;
		
		//return sharedAgentOpenness=(double) killedAgentSet.size()/agentCount;
	}
	
	
	
	public double updateSharedAgentOpenness2(){//this is one we need to use
		 intersection=findIntersection(this.killedAgentSet,this.sharedKnowAgentSet);
		
		if (!intersection.isEmpty()){
			sharedAgentOpenness=(double) intersection.size()/this.sharedKnowAgentSet.size();
		}
		else{
			sharedAgentOpenness=0;
		}
		
		return sharedAgentOpenness;
		
		//return sharedAgentOpenness=(double) killedAgentSet.size()/agentCount;
	}
	
	public double updateSharedAgentOpenness3(){//use to calculate actual openness
		
		return sharedAgentOpenness=(double) killedAgentSet.size()/agentCount;
	}
	
	public Set<Integer> findIntersection(Set<Integer>set1,Set<Integer>set2){
		Set<Integer> intersection = new HashSet<Integer>(set1); // use the copy constructor
		intersection.retainAll(set2);
		return intersection;
	}
	
	
	public int getIntersectionSize(){

		return intersection.size();
	}

	public void updateSharedTaskOpenness(){
		sharedTaskOpenness=(double) sharedNewTaskSet.size()/sharedEncounteredTaskSet.size();
	}

	public double getSharedTaskOpenness() {
		return sharedTaskOpenness;
	}



	public HashMap<Integer, Agent> getAgentMap() {
		return agentMap;
	}

	public void AddToAgentMap(int key,Agent agent) {
		this.agentMap.put(key, agent);
	}
	
	public void RemoveFromAgentMap(int key) {
		this.agentMap.remove(key);
	}
	
	/**
	 * 
	 * @return number of messages in this blackboard
	 */
	public int getSize(){
		return messageList.size();
	}
	
//	 /**
//	  * return global sat values, calculated after task is completed and is removed from blackboard
//	  */
//	public HashMap<Task, ArrayList<Double>> getGlobalSat(){
//		return globalSatMap;
//	}
	
	/** return all messages that are on blackboard currently */
    public ArrayList<BlackboardMessage> getAllMessages() {
    	return messageList;
    }
    
    /**
     * 
     * @return messages to be returned
     */
    public ArrayList<BlackboardMessage> getReturnedMessages(){
    	numUnfinishedTasks = messagesToBeReturned.size();
    	ArrayList<BlackboardMessage> returnedMessages = messagesToBeReturned;
    	messagesToBeReturned = new ArrayList<BlackboardMessage>();
    	return returnedMessages;
    }

//	public void setAgentOpenness(int agentId, double openness) {
//		agentOpenness.put(agentId, openness);
//	}
//	
//	public double getAgentOpenness(int agentId) {
//		return agentOpenness.get(agentId);
//	}
	
//	public void setAgentQuality(int agentId,ArrayList<Double> quality) {
//		qualityMap.put(agentId, quality);
//	}
//	
//	public HashMap<Integer,ArrayList<Double>> getAgentQuality() {
//		return qualityMap;
//	}
	
//	public void setLearningCost(Double cost) {
//		learningCost.add(cost);
//	}
	
//	public ArrayList<Double> getLearningCost() {
//		return learningCost;
//	}
    
    /**
     * 
     * @return
     */
    public int getNumFinishedSubtasks(){
    	return numFinishedSubtasks;
    }
    
	/**
	 * 
	 * @param n
	 */
	public void setNumFinishedSubtasks(int n) {
		numFinishedSubtasks += n;
		//numFinishedSubtaskAgents++;
	}
	
	public int getNumUnfinishedTasks(){
		return numUnfinishedTasks;
	}
//	
//	/**
//	 * 
//	 * @return
//	 */
//	public int getNumFinishedSubtasks() {
//		if(numFinishedSubtaskAgents!=0) {
//			return numFinishedSubtasks/numFinishedSubtaskAgents;
//		}
//		else {
//			return 0;
//		}
//	}
//
//	/**
//	 * 
//	 * @return
//	 */
//	public int getNumFinishedTasks() {
//		return numFinishedTasks;
//	}
	
	/**
	 * 
	 * @return numAgentsHasResponsed	number of agents who has bid on a task
	 */
	public int getNumAgentHasResponsed(){
		return numAgentsHasResponsed;
	}

	/**
	 * 
	 */
	public void addNumAgentHasResponsed(){
		numAgentsHasResponsed++;
		if(numAgentsHasResponsed == agentCount){
			print("All agent has responsed");
			allHasResponsed = true;
			//newMsg = false;
			numAgentsHasResponsed = 0;
		}
	}
	
//	/**
//	 * reset the numAgentHasResponsed counter
//	 */
//	public void resetBlackboard(){
//		numAgentsHasResponsed = 0;
//		allHasResponsed = false;
//		newMsg = false;
//	}
	
	
	
//	public void setTypeUtility(HashMap<Integer,Double> utility) {
//		agentTypeUtilityCount++;
//		if(utility.size()>0) {
//			double temp = 0;
//			double temp1 = 0;
//			int type = 1;
//			for(int i=0; i<utility.size(); i++) {
//				for(int j=0; j<32; j++) {
//					if(utility.get(type)!=null) {
//						if(typeUtility.get(type)!=null) {
//							temp = typeUtility.get(type);
//						}
//						temp1 = temp+(double)utility.get(type);
//						typeUtility.put(type, temp1);
//					}
//					type++;
//				}
//			}
//		}
//	}

//	public int getAgentTypeUtilityCount () {
//		return agentTypeUtilityCount;
//	}
	
//	public HashMap<Integer,Double> getTypeUtility() {
//		return typeUtility;
//	}
	
	public Agent getAgent(Integer agentId){
		return agentMap.get(agentId);
	}
	
	public int getAgentCount() {
		return agentCount;
	}

	public boolean getAllHasResposed(){
		return allHasResponsed;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void setAgentCount(int agentCount) {
		this.agentCount = agentCount;
		
	}

	public ArrayList<Agent> getAgentList() {
		return agentList;
	}

	public void addAgent(Agent agent) {
		this.agentList.add(agent);
	}
	public int getId(){
		return 1;
	}



	public double getAgentOpenness() {
		return agentOpenness;
	}



	public void setAgentOpenness(double agentOpenness) {
		this.agentOpenness = agentOpenness;
		print("Set bb agentOpenness = "+agentOpenness);
	}



	public double getTaskOpenness() {
		return taskOpenness;
	}



	public void setTaskOpenness(double taskOpenness) {
		this.taskOpenness = taskOpenness;
		print("Set bb taskOpenness = "+taskOpenness);
	}



	public Set<Integer> getSharedNewTaskSet() {
		return sharedNewTaskSet;
	}



	public Set<Integer> getSharedEncounteredTaskSet() {
		return sharedEncounteredTaskSet;
	}
	
	public double getAgentOpennessPerception1(){
		return this.agentMap.get(MainAgent.getNonKillAgentId1()).getAgentOpennessPerception();
		
	}
	
	public double getTaskOpennessPerception1(){
		return this.agentMap.get(MainAgent.getNonKillAgentId1()).getTaskOpennessPerception();
		
	}
	
	public double getAgentOpennessPerception2(){
		return this.agentMap.get(MainAgent.getNonKillAgentId2()).getAgentOpennessPerception();
		
	}
	
	public double getTaskOpennessPerception2(){
		return this.agentMap.get(MainAgent.getNonKillAgentId2()).getTaskOpennessPerception();
		
	}
	
	public double getAgentOpennessPerception3(){
		return this.agentMap.get(MainAgent.getNonKillAgentId3()).getAgentOpennessPerception();
		
	}
	
	public double getTaskOpennessPerception3(){
		return this.agentMap.get(MainAgent.getNonKillAgentId3()).getTaskOpennessPerception();
		
	}
	
	public int getTaskAssigmentAtOneTick1(){
		return this.agentList.get(MainAgent.getNonKillAgentId1()).getTaskAssignmentAtOneTick();
		//return this.agentMap.get(MainAgent.getNonKillAgentId1()).getTaskAssignmentAtOneTick();
	}

	public int getTaskAssigmentAtOneTick2(){
		return this.agentList.get(MainAgent.getNonKillAgentId2()).getTaskAssignmentAtOneTick();
		//return this.agentMap.get(MainAgent.getNonKillAgentId2()).getTaskAssignmentAtOneTick();
	}
	
	public int getTaskAssigmentAtOneTick3(){
		return this.agentList.get(MainAgent.getNonKillAgentId3()).getTaskAssignmentAtOneTick();
		//return this.agentMap.get(MainAgent.getNonKillAgentId3()).getTaskAssignmentAtOneTick();
	}


	public Set<Integer> getSharedKnowAgentSet() {
		return sharedKnowAgentSet;
	}
	
	public double getLearnedCap() {
		return totalLearnedCap;
	}



	public void updateLearnedCap(double learnedCap) {
		this.totalLearnedCap += learnedCap;
	}

	
	public int getSharedKnownAgentSetSize(){
		return this.sharedKnowAgentSet.size();
	}

	public int getKilledAgentSetSize(){
		return this.killedAgentSet.size();
	}



	public double getTotalLostCap() {
		return totalLostCap;
	}
	
	public void updateTotalLostCap(double learnedCap) {
		this.totalLostCap += learnedCap;
	}
	
	public String showAgentsInfo(){
		//TODO
		String info=null;
		for (Agent agent: agentList){
			//print out every info about each agent
			
			//info=info+"\n"+agent.getId()+","+agent.getNumTaskInvolved()+","+agent.getAgentOpennessPerception()+","+agent.getQualityList();
			info=info+"\n"+agent.getId()+","+agent.getNumTaskInvolved()+","+agent.getAgentOpennessPerception();
		}
		return info;
	}
	
	public Double getMeanKnownAgents(){
		double MeanKnownAgents=(double) this.sharedKnowAgentSet.size()/this.agentList.size();
		
		return MeanKnownAgents;
		
	}







	public int getNumUnassignedAgents() {
		return numUnassignedAgents;
	}







	public void setNumUnassignedAgents(int numUnassignedAgents) {
		this.numUnassignedAgents = numUnassignedAgents;
	}







	public int getNumFinishedTasks() {
		return numFinishedTasks;
	}







	public void setNumFinishedTasks() {
		this.numFinishedTasks++;
	}







	public double getU_learnTotal() {
		return U_learnTotal;
	}







	public void setU_learnTotal(double u_learnTotal) {
		U_learnTotal = u_learnTotal;
	}







	public int getNumU_learn() {
		return numU_learn;
	}







	public void setNumU_learn(int numU_learn) {
		this.numU_learn = numU_learn;
	}







	public double getU_solveTotal() {
		return U_solveTotal;
	}







	public void setU_solveTotal(double u_solveTotal) {
		U_solveTotal = u_solveTotal;
	}







	public int getNumU_solve() {
		return numU_solve;
	}







	public void setNumU_solve(int numU_solve) {
		this.numU_solve = numU_solve;
	}
	
	   /** debug method */
	
    @SuppressWarnings("unused")
	private void print(String s){
		if (PrintClass.DebugMode && PrintClass.printClass){
			System.out.println(this.getClass().getSimpleName()+"::"+s);
		}else if(PrintClass.DebugMode){
			System.out.println(s);
		}
	}

}
