/**
 * 
 */
package AdhocCollaboration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class Task {
    private int Id;
	private int type;
	private int numSubtasks;
	private Parameters params;
	//private int numTasksFinished;
//	private double taskOpenness;
//	private double taskProb;
//	private double globalSat;
	private boolean isFinished;
	private ArrayList<SubTask> subtasksList;
	private Random random;
	private int tickToFinish;
    
    public Task() {
		Id 				= 0;
		type			= 0;
		numSubtasks 	= 0;
		params 			= RunEnvironment.getInstance().getParameters();
		//numTasksFinished= 0;
//		taskOpenness 	= 0.0;
//		taskProb 		= 0.0;
//		globalSat 		= 0.0;
		isFinished 		= false;
		subtasksList 	= new ArrayList<SubTask>();
		random 			= new Random();
		tickToFinish = (Integer) params.getValue ("tickToFinish");;//change ticks to finish a task, only change it here
	}
    
    
    /**
     * 
     */
    public int getTickToFinish(){
    	return tickToFinish;
    }
    
    /**
    *
    *@param id 	Task Id
    **/
    public void setId(int id){
    	Id = id;
    }

    /**
    *
    *@return id Task Id
    **/
    public int getId(){
    	return Id;
    }
    
    /**
    *
    *@return An array of subtask contained in this task
    **/
    public ArrayList<SubTask> getSubtasks(){
    	return subtasksList;
    }
    
    /**
     * 
     * @param num
     */
    public void setNumSubtasks(int num){
    	numSubtasks = num;
    }
    
    /**
     * 
     * @return
     */
    public int getNumSubtasks(){
    	return numSubtasks;
    }
    
    /**
     * 
     * @param taskType
     */
    public void setType(int taskType){
    	type = taskType;
    }
    
    /**
     * 
     * @return
     */
    public int getType(){
    	return type;
    }
    
    /**
     * 
     * @param openness
     */
//    public void setTaskOpenness(double openness){
//    	taskOpenness = openness;
//    }
//    
//    /**
//     * 
//     * @return
//     */
//    public double getTaskOpenness(){
//    	return taskOpenness;
//    }
    
//    /**
//     * 
//     */
//    public void setFinished(){
//    	isFinished = true;
//    	numTasksFinished++; //why?
//    }
    
    /**
     * 
     */
    public boolean getFinished(){
    	return isFinished;
    }
    
    
    /**
     * 
     * @param prob
     */
//    public void setTaskProb(double prob){
//		taskProb = prob;
//	}
//    
//    /**
//     * 
//     * @return
//     */
//	public double getTaskProb() {
//		return taskProb;
//	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	public int getNumTasksFinished() {
//		return numTasksFinished;
//	}

	/**
	* decompose a task into subtasks 
	*@param subtasks 	A List of subtasks'Id
	*@return subtaskList 	A list of subtasks
	* @throws IOException 
	*/
	public ArrayList<SubTask> decompose(ArrayList<Integer> subtasks,ArrayList<Integer> Num_AgentsList, ArrayList<Double> QualityList){
		for (int i=0; i<subtasks.size(); i++) {
			SubTask st = new SubTask();
			st.setId(subtasks.get(i));
			st.setCap(subtasks.get(i));
			st.setNumAgents(Num_AgentsList.get(i));
			st.setQuality(QualityList.get(i));
			subtasksList.add(st);
		}
		return subtasksList;
	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	public double getGlobalSat(){
//		return globalSat;
//	}
	
//	/**
//	 * 
//	 */
//	public void setGlobalSat(){
//		double localValue = 0;
//		ArrayList<SubTask> subtasks = this.getSubtasks();
//		for(int i = 0; i < subtasks.size(); i++){
//			localValue += subtasks.get(i).getLocalSatAllAgents();
//		}
//		if(isFinished){
//			globalSat = localValue + random.nextGaussian();
//		}
//	}
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
