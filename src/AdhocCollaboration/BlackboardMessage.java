/**
 * 
 */
package AdhocCollaboration;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class BlackboardMessage {
	private Task task;
	private ArrayList <SubTask> subtasks;
	private HashMap<Integer, ArrayList<Integer>> subtaskAssignment; // Subtask assignment: subtaskId-->[ agentId,...]. Assign each subtask to a list of agents.
	private ArrayList<Agent> agentBiddingList; // records agents who intend to bid on this task
	private boolean hasAssigned;
	private int taskFinishedFlag; // -1 falied, 0 unfinished, 1 finished
	private int numFinishedSubtasks; //keep count of number of finished subtask, if the number equals to total number of subtasks, then this task is finished
	private Blackboard bb;
	private HashMap<Integer, Integer> subtaskQualificationAllAgentsMap;//it maps the subtaskId to the number of qualified agents for this subtask, it counts all the qualified agents in the environment
	private HashMap<Integer, Integer> subtaskQualificationBiddingAgentsMap;//it maps the subtaskId to the number of qualified agents for this subtask, concerning all the agents qualified agents who have bid on this task
	/**
	 * Create a new instance of BlackboardMessage
	 */
	public BlackboardMessage(Task task, ArrayList<SubTask> subtasks,Blackboard b){
		this.task = task;
		this.subtasks = subtasks;
		this.hasAssigned = false;
		agentBiddingList = new ArrayList<Agent>();
		taskFinishedFlag = 0;
		numFinishedSubtasks = 0;
		bb=b;
		subtaskAssignment = new HashMap<Integer, ArrayList<Integer>>();//<subtaskId,array of Agentsid>
		subtaskQualificationAllAgentsMap=new HashMap<Integer, Integer>();//<subtaskId,number of agents qualified for this subtask>
		subtaskQualificationBiddingAgentsMap=new HashMap<Integer, Integer>();
		for(SubTask subtask : subtasks){
			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
		}
//		for(SubTask subtask : subtasks){
//			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
//		}
	}
	
	/**
	 * 
	 */
	public void removeAssignment(){
		for(SubTask subtask : subtasks){
			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
		}
		agentBiddingList.clear();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Task getTask() {
	    return task;
	}

	/**
	 * 
	 * @param task
	 */
	public void setTask(Task task) {
	    this.task = task;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<SubTask> getSubtasks() {
	    return subtasks;
	}

	/**
	 * 
	 * @param subtask
	 * @return
	 */
	public ArrayList<SubTask> removeSubtask(SubTask subtask) {
		subtasks.remove(subtask);
	    return subtasks;
	} 
	
	/**
	 * 
	 * @param subtasks
	 */
	public void setSubtasks(ArrayList<SubTask> subtasks) {
		this.subtasks = subtasks;
	}	
	
	/**
	 * 
	 * @return agents assignment for each subtask
	 */
	public HashMap<Integer, ArrayList<Integer>> getAssignment(){
		return this.subtaskAssignment;
	}
	
	/**
	 * Assign agents to do subtasks
	 * @param assignment agents assignment for each subtask 
	 */
	public void setAssignment(HashMap<Integer, ArrayList<Integer>> assignment){
		this.subtaskAssignment = assignment;
		hasAssigned = true;
	}
	
	/**
	 * Add agent to agentBiddingList of this message
	 * @param agent The agent who want to involved in this task
	 */
	public void addAgentToBiddingList(Agent agent){
//		print(String.format("Add agent %d to task%d's bidding list", agent.getId(), task.getId()));
		this.agentBiddingList.add(agent);
	}
	
	/**
	 * 
	 */
	public void setSubtaskFinish(SubTask subtask){
		boolean subtaskFinished = subtask.SetNumOfAgentFinished();
		if(subtaskFinished){//if the whole subtask get finished 
			numFinishedSubtasks++;
			if(numFinishedSubtasks == subtasks.size()){
				taskFinishedFlag = 1;
				bb.setNumFinishedTasks();
				print("Task " + task.getId() + "finished");
			}
		}
	
	}
	/**
	 * 
	 */
	public void setSubtaskFailed(SubTask subtask, int agentId){
		subtask.setFailedAgentId(agentId);
		taskFinishedFlag = -1;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTaskFinishedFlag(){
		return taskFinishedFlag;
	}
	
	/**
	 * 
	 */
	public ArrayList<Agent> getAgentBiddingList(){
		return agentBiddingList;
	}
	
	/**
	 * 
	 */
	public boolean isAssigned(){
		return hasAssigned;
	}
	/**
	 * 
	 */
	public void setAssigned(){
		this.hasAssigned = true;
	}
	
	/**
	 * 
	 * @param agentId
	 * @return
	 */
	public ArrayList<SubTask> getSubtasksAssignmentByAgent(Integer agentId){
		ArrayList<SubTask> subtasksToBeExecuted = new ArrayList<SubTask>();
		for(SubTask subtask : subtasks){
			if(subtaskAssignment.get(subtask.getId()).contains(agentId)){
				subtasksToBeExecuted.add(subtask);
			}
		}
		return subtasksToBeExecuted;
	}
	
	/**
	 * 
	 * @param subtaskId
	 * @return
	 */
	public ArrayList<Integer> getSubtasksAssignmentBySubtaskId(Integer subtaskId){
		return subtaskAssignment.get(subtaskId);
	}
	
	/**
	 * 
	 */
	public void printAssignment(){
		print(String.format("Task %d assignment", task.getId()));
		print(this.subtaskAssignment);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public HashMap<Integer, Integer> getSubtaskQualificationMap() {
		return subtaskQualificationAllAgentsMap;
	}

	public void setSubtaskQualificationMap(
			HashMap<Integer, Integer> subtaskQualificationMap) {
		this.subtaskQualificationAllAgentsMap = subtaskQualificationMap;
	}

	
   public void PrintSubtaskQualificationAllAgentsMap(){
	   for (SubTask s : this.subtasks){
		   subtaskQualificationAllAgentsMap.put(s.getId(), s.getQualifiedAgentsCount());
	   }
	  
	   print("Task "+this.task.getId()+ "  subtaskQualificationAllAgentsMap ----"+subtaskQualificationAllAgentsMap);
	   //after Printing  reset the Count and clear the Map
	   resetSubtaskQualificationCount();
	   subtaskQualificationAllAgentsMap.clear();
	   
   }
   
   
   
   public void resetSubtaskQualificationCount(){
	   for (SubTask s : this.subtasks){
		   s.setQualifiedAgentsCount(0);
	   }
   }
   
   
   
   public void resetSubtaskQualificationBiddingAgentsCount(){
	   for (SubTask s : this.subtasks){
		   s.setQualifiedBiddingAgentsCount(0);
	   }
   }
   
   
   public void PrintSubtaskQualificationBiddingAgentsMap(ArrayList<Agent> agentBiddingList){
	   for (SubTask s : this.subtasks){
//		   print("Subtabsk ~~~~~~"+s.getId());
		   for(Agent agent: agentBiddingList){
//			   print("agents Cap >= subtask Cap ???");
//			   print("AgentID "+agent.getId()+" Cap ="+agent.getCapabilityQuality(s.getId()));
//			   print("Subtask Cap = "+s.getCap());
			   if (agent.getCapabilityQuality(s.getId())>=s.getQuality()){
//				   print("Yes>>>>>>");
				   s.addQualifiedBiddingAgentsCount();
				   
			   }
		   }
		   this.subtaskQualificationBiddingAgentsMap.put(s.getId(), s.getQualifiedBiddingAgentsCount());
	   }
	   print("Task "+this.task.getId()+ "  subtaskQualificationBiddingAgentsMap ----"+this.subtaskQualificationBiddingAgentsMap);
	   //after Printing  reset the Count and clear the Map
	   resetSubtaskQualificationBiddingAgentsCount();
	   this.subtaskQualificationBiddingAgentsMap.clear();
	   
   }
   
   
   
   public void PrintTaskDetail(){
	   String Line="";
	   for(SubTask s: this.subtasks){
		   Line+="[Id "+ s.getId()+"|"+s.getNumAgents()+"|"+s.getQuality()+"] ";
	   }
	   print("Subtasks :"+Line);

   }
   

public HashMap<Integer, Integer> getSubtaskQualificationBiddingAgentsMap() {
	return subtaskQualificationBiddingAgentsMap;
}

public void setSubtaskQualificationBiddingAgentsMap(
		HashMap<Integer, Integer> subtaskQualificationBiddingAgentsMap) {
	this.subtaskQualificationBiddingAgentsMap = subtaskQualificationBiddingAgentsMap;
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

private void print(HashMap<Integer, ArrayList<Integer>> map){
	if (PrintClass.DebugMode && PrintClass.printClass){
		System.out.println(this.getClass().getSimpleName()+"::"+map);
	}else if(PrintClass.DebugMode){
		System.out.println(map);
	}
}


}
