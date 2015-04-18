/**
 * 
 */
package AdhocCollaboration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.collections.IndexedIterable;


/** MainAgent is responsible for:
 * 	- introducing the tasks 
 *  - dividing each task into subtasks(with cap, minNumAgents, quality)
 *  - populating the blackboard with tasks/subtasks
 *  step() method is a scheduled method(method is invoked on instances of this class)
 *  step() is called every iteration of the simulation on all Agents starting at tick 1 and every tick after that
@author Bin Chen 
@author Xi Chen
*/
public class MainAgent {

	private int lastTaskId; 
	protected static int tick;
	private int totalTick; // use this and agentOpenness to calculate at which tick to kill one agent;
	private double taskOpenness;
	private Context context;
//	private ArrayList<LearningMatch> learningTeaching;
//	private ArrayList<Double> averageQualityList;
//	private HashMap<Integer,SubTask> agentMatch;//$$$$$$matchs agent with subtask
	private Blackboard blackboard;
	private Parameters params;
	private int agentCount;//total number of agents in this simulation
	private int agentTypeScenario;
//	private int prevFinishedSubtasks;
//	private int prevFinishedTasks;
	private double agentOpenness;
	private double agentQualityMax;
	private Random random;
	private ArrayList<Agent> killingQueue;//put selected agents to the killingQueue and start kill them in the next tick. Agent is killed if they are not busy, if busy then try to kill in the tick 
	private LinkedList<BlackboardMessage> returnedMessages;
	private int lastAgentId;
//	private int tickToKill; // the time interval to kill an agent
//	private int tickToKill2;//use this to do the second round kill to maintain the preset agent openness
	private int initalCapNum;//Number of  capabilities agents have when created
	private static int NonKillAgentId1;//Set the agentId here so that it won't be killed during the simulation to see how they perceive AO and TO
	private static int NonKillAgentId2;
	private static int NonKillAgentId3;
	private int ticksToFinish;//ticks to finish a task
	private int haveAddedToKillingQueue=0;//the number of agents has been added to killingQueue so far;
	private double killPerTick;//how many agents need to be killed in one tick.
	private double  shouldHaveKilledAtThisTick;
	
	/*user enter a string of two numbers, not separated by comma. Example: 3333  .this means 33% of HardTask, 33% of AverageTask, and 100-(33+33) % of EasyTask*/
	private String TaskDistrubution;// the string that user entered which specifies the task distribution, we use this to pick the congfic file to read the task order
	private String hardTaskPerString;
	private String averageTaskPerString;
	private static int InitalTaskNumber=40;// the number of tasks that we wanted to intruduce in the enviroment before the simulation starts, when the simulation starts, we intruduce 1 new task at each tick
	
	
	
	
    
    /** Creates a new instance of Model */
    public MainAgent(Blackboard bb, double task_openness,int total_tick, int agent_count,int agent_types_scenario,double agent_openness,int initalCapNummber) {
    	random = new Random();
    	print(" Main agent is called");
    	initalCapNum=initalCapNummber;
    	lastTaskId 		= -1;
    	tick 			= 0;
    	taskOpenness	= task_openness;
    	blackboard 		= bb;   
    	params 			= RunEnvironment.getInstance().getParameters();
    	totalTick       =(Integer) params.getValue ("total_tick");
    	agentCount 		= (Integer) params.getValue ("agent_count");
    	agentTypeScenario = (Integer) params.getValue("agent_types_scenario");
    	NonKillAgentId1 =(Integer) params.getValue("NonKillAgentId1");
    	NonKillAgentId2 =(Integer) params.getValue("NonKillAgentId2");
    	NonKillAgentId3 =(Integer) params.getValue("NonKillAgentId3");
//    	prevFinishedSubtasks=0;
//    	prevFinishedTasks=0;
//    	agentOpenness 	= (Double) params.getValue("agent_openness");    
    	agentOpenness=agent_openness;
    	agentQualityMax = 1;
    	killingQueue = new ArrayList<Agent>();
    	returnedMessages = new LinkedList<BlackboardMessage>();
    	lastAgentId = agent_count;
//    	tickToKill = (int) Math.ceil(totalTick / (agentOpenness * agentCount));
//    	tickToKill2=getTickToKill2();
    	killPerTick= (agentOpenness * agentCount)/totalTick;
    	
    	print("********* Task openness "+taskOpenness+"     Agent openness "+agentOpenness+"  Total agents needs to be killed = "+(agentOpenness * agentCount));
    }
    
    /** 
     * Controls what is done during each tick of the simulation. 
     **/
    @ScheduledMethod(start=1, interval=1) // duration = num_ticks
    public void step() {    	
    	tick++;
      	if(tick > this.totalTick){
    		
        	System.out.println("~~~~~~~~~ Current tick is "+tick);
        	blackboard.post(new ArrayList<BlackboardMessage>()); //post empty messageList, still trigger agents to act, new task to select, so they keep finishing current task on hand
    	}
      	else{
      		//blackboard.resetBlackboard();
        	System.out.println("~~~~~~~~~ Current tick is "+tick);
        	
        	maintainAO();
        	
        	//update sharedAgentOpenness
        	blackboard.updateSharedAgentOpenness2();
        	Task newTask = findNewTask();
        	postMessages(newTask);//this triggers agent to act
        	
        	System.out.println("Finished Task = "+this.blackboard.getNumFinishedTasks());
      	}
    
    	
    	if (tick==this.totalTick+this.ticksToFinish+2){//this gives agent chance to finish the task that auctioned off at the last tick(total tick)
    		print("!!!!!!!!!!!!!!!!!!!!!!!!!!!END");
    		System.out.println("Total # agents have been putting in KillingQueue = "+haveAddedToKillingQueue+"   Should kill "+(agentOpenness * agentCount));
			RunEnvironment.getInstance().endRun();
    	}
    	
    }
    
    
    
    
    /**
     * this method it used to maintain AO, it will determine if current tick needs to kill agents, if needs then add them to KillingQueue and then kill the ideal agents.
     */
    private void maintainAO(){
    	//adding agent to kill Queue
    	
    	shouldHaveKilledAtThisTick= killPerTick* tick;
    	double needKill=	shouldHaveKilledAtThisTick-	haveAddedToKillingQueue;
		int needKillRoundDown = (int) Math.floor(needKill);
		if (needKillRoundDown>=1){
			//need to kill multiple agents in one tick
			
			for (int i=0;i<needKillRoundDown;i++){
				//add to killingQueue
				Agent selectedAgent = getOneAgent();
	        	while(killingQueue.contains(selectedAgent)){
	        		selectedAgent = getOneAgent();
	        	}
	        	killingQueue.add(selectedAgent);
	        	System.out.println(String.format("Add agent %d into killing queue", selectedAgent.getId()));
				
				haveAddedToKillingQueue++;
			}
		}
		else{//need to kill one after several tick	
		}
		
		//do actual kill 
    	for(Agent a : new ArrayList<Agent>(killingQueue)){
    		//Only kill idle agents
    		if(!a.getBusy()){
    			int killedAgentType=a.getAgentType();/* 1 - expert, 2 - average ,3-novice*/
    			killingQueue.remove(a);
    			killAgent(a);
    			addAgent(killedAgentType);// add a new agent to replace the killed one
    		}
    		else{}
    	}	
		
		
    }
    
    
    
    
    
    /**
     * Find and decompose a task
     * @return <code>task</code> A new decomposed task
     * @throws IOException 
     */
    private Task findNewTask() {
    	
    	
    	if (tick==1){//post the inital number of tasks onto blackboard at tick 1
    		AddInitialTask();
    	}
    	
    	//Get task's information from configuration files
    	Properties configTask = new Properties();
    	
    	try {
//			configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+taskOpenness+".properties"));
    		configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+"_TO"+taskOpenness+this.hardTaskPerString+this.averageTaskPerString));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    	
    	
//    	if(taskOpenness == 0) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.0.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 0.2) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.2.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 0.5) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.5.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 1) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task1.0.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
    	
    	String t = Integer.toString(tick+InitalTaskNumber);
    	/*
    	 * trying to grab the task from config files starting from InitalTaskNumer+1.
    	 */
    	if(tick <= totalTick) {
//    	if((taskOpenness == 0 || taskOpenness == 0.2 || taskOpenness == 0.5 || taskOpenness == 1) && tick <= totalTick) {
    		String task_type = configTask.getProperty(t); 	
        	print("******* task type "+task_type);
        	
        	// get the task specifics from the configuration file
        	Properties configFile = new Properties();
        	try {
				configFile.load(this.getClass().getClassLoader().getResourceAsStream("config_types.properties"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
        	//print("s="+num_subtasks+" g="+subtasks);
        	
    		Task task = new Task();
    		task.setId(++lastTaskId);
    		task.setType(Integer.parseInt(task_type));
    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
    		print(" New task id "+task.getId());
    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
    	
    		// decompose task into subtasks
    		//**************************************************
    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
    		String [] subtasksString = subtasks.split(",");
    		//change it to integer array 
    		for(int i=0; i<subtasksString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			subtasksList.add(Integer.parseInt(subtasksString[i]));
    		}
    		
    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
    		String[] Num_AgentsString = Num_Agents.split(",");
    		for(int i=0; i<Num_AgentsString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
    		}
    		   		
    		ArrayList<Double> QualityList = new ArrayList<Double>();
    		String[] QualityString = Quality.split(",");
    		for(int i=0; i<QualityString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			QualityList.add(Double.parseDouble(QualityString[i]));
    		}
 
    		task.decompose(subtasksList,Num_AgentsList,QualityList);
    		print(" finished decomposing" );
    		return task;
    		}
    	return null;
    }
    
    
    
    /**
     * Find and decompose initial number of tasks, this method is called at the first tick to introduce certain number of tasks as the initial tasks on blackboard for agent to bid 
     * this method add the tasks to returnedMessage list, to be treated as returened tasks and to be posted together with the first new task
     * @return <code>task</code> A new decomposed task
     * @throws IOException 
     */
    private void AddInitialTask() {
    	//Get task's information from configuration files
    	Properties configTask = new Properties();
    	
    	try {
//			configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+taskOpenness+".properties"));
    		configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+"_TO"+taskOpenness+this.hardTaskPerString+this.averageTaskPerString));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	for (int j=1;j<=InitalTaskNumber;j++){
    		String task_type = configTask.getProperty(Integer.toString(j)); 	
//        	print("******* task type "+task_type);
        	
        	// get the task specifics from the configuration file
        	Properties configFile = new Properties();
        	try {
				configFile.load(this.getClass().getClassLoader().getResourceAsStream("config_types.properties"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
        	//print("s="+num_subtasks+" g="+subtasks);
        	
    		Task task = new Task();
    		task.setId(++lastTaskId);
    		task.setType(Integer.parseInt(task_type));
    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
    		print(" New task id "+task.getId());
    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
    	
    		// decompose task into subtasks
    		//**************************************************
    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
    		String [] subtasksString = subtasks.split(",");
    		//change it to integer array 
    		for(int i=0; i<subtasksString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			subtasksList.add(Integer.parseInt(subtasksString[i]));
    		}
    		
    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
    		String[] Num_AgentsString = Num_Agents.split(",");
    		for(int i=0; i<Num_AgentsString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
    		}
    		   		
    		ArrayList<Double> QualityList = new ArrayList<Double>();
    		String[] QualityString = Quality.split(",");
    		for(int i=0; i<QualityString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			QualityList.add(Double.parseDouble(QualityString[i]));
    		}
 
    		task.decompose(subtasksList,Num_AgentsList,QualityList);
//    		print(" finished decomposing" );
    		
    		
    		/*
    		 * package the task into blackboardMessage and add them to returnedMessages, the mainAgent will trade these messages as returned messages and post them together with the new message at tick 1.
    		 */
    		returnedMessages.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
    		
    		}
    	print(" finished adding inital tasks to returnedMessages list, wait to be posted on blackboard together with the first new task" );
    }
    
    
    /**
     * post the new found and decomposed task message along with old returned messages to blackboard
     * @param <code>task</code> the new found and decomposed task 
     */
    private void postMessages(Task task){
    	ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>();
    	//adding returned messages
    	if (!returnedMessages.isEmpty()){
    		//ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>(returnedMessages);
    		messagesToPost.addAll(returnedMessages);
    		for (BlackboardMessage Msg: returnedMessages){
    			System.out.println(String.format("Post Returned task -- task %d to blackboard ", Msg.getTask().getId()));
    			
    		}
    	}
    	
    	
    	//adding newly founded decomposed task
    	messagesToPost.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
    	returnedMessages.clear();
    	System.out.println(String.format("Post new task -- task %d to blackboard ", task.getId()));
    	blackboard.post(messagesToPost); 
    	
    	
    	//adding this new task to SharedEncounteredTaskSet
    	blackboard.getSharedEncounteredTaskSet().add(task.getId());
    	blackboard.getSharedNewTaskSet().add(task.getType());
    	//update the sharedTaskOpenness
    	blackboard.updateSharedTaskOpenness();
    	
    }
    
    @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
			watcheeFieldNames = "allHasResponsed", //this is triggered when all agents has submitted the bid
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
    public void holdAuction(){  	
    	blackboard.auction();
    	//blackboard.resetBlackboard();
    }
    
    /**
     * 
     * @return <code>true</code> if the MainAgent should kill one agent at current tick, <code>false</code> otherwise
     */
//    private boolean timeToKill(){
////    	if (this.tickToKill2==-1){
////    		if(tick > 0 && (tick % tickToKill == 0)){
////        		return true;
////        	}
////    	}
////    	else{
////    		if(tick > 0 && ((tick % tickToKill == 0) || (tick % tickToKill2 == 0))){
////        		return true;
////        	}
////    	}
//    	
//    	if(tick > 0 && (tick % tickToKill == 0)){
//    		return true;
//    	}
//    	
//    	return false;
//    }
//    
//    private boolean timeToKill2(){
//    	if (this.tickToKill2==-1){
//    		return false;
//	}
//	else{
//		if(tick > 0 &&  (tick % tickToKill2 == 0)){
//    		return true;
//    	}
//	}
//    	return false;
//    	
//    }
    
    /**
     * 
     * @param <code>agent</code> an agent to be removed
     */
    private void killAgent(Agent agent){
		//blackboard.RemoveFromAgentMap(agent.getId());
    	blackboard.updateTotalLostCap(agent.getIndividualLearnedCap());
    	blackboard.getKilledAgentSet().add(agent.getId());
    	//blackboard.updateSharedAgentOpenness();
		blackboard.getAgentList().remove(agent);
		context.remove(agent);


		System.out.println(String.format("Kill agent %d", agent.getId()));
		System.out.println("Killed agents are "+ blackboard.getKilledAgentSet());
	}
    
    @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
			watcheeFieldNames = "newAssignments",
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
    public void receiveReturnedMessage(){
    	System.out.println(String.format("MainAgent is receiving returned messages"));
    	returnedMessages.addAll(blackboard.getReturnedMessages());
    }
    
    /**
     * Introduce a new agent into environment, we add the agent that is the same type of the killed agent(initial type when it was created)
     */
    private void addAgent(int agentType){
   
		agentQualityMax = 1;
		Agent newAgent = new Agent(++lastAgentId, blackboard, agentType, agentQualityMax,initalCapNum);
		context.add(newAgent);
		blackboard.addAgent(newAgent);
		blackboard.AddToAgentMap(newAgent.getId(), newAgent );
		blackboard.setNumUnassignedAgents(blackboard.getNumUnassignedAgents()+1);
		System.out.print(String.format("Add new agent %d ", newAgent.getId()));
		System.out.println();
    }
	


	/**
     * 
     * @return A randomly selected agent
     */
	private Agent getOneAgent(){
		IndexedIterable<Agent> agents = context.getObjects(Agent.class);
		int randomNum = random.nextInt(agents.size());
	
		//System.out.println("Do not Kill agents "+NonKillAgentId1+" "+NonKillAgentId2+" "+NonKillAgentId3 );
		//do not choose agent which agent number is "NonKillAgentId1" to kill
		int choosenAgentId=agents.get(randomNum).getId();
		while (choosenAgentId==NonKillAgentId1 || choosenAgentId==NonKillAgentId2 ||choosenAgentId==NonKillAgentId3){
			System.out.println("Do not Kill this agent !!!!!!!!!!!!!!!!!!!!!!!");
			randomNum = random.nextInt(agents.size());
			choosenAgentId=agents.get(randomNum).getId();
		}
		
		
		Agent selectedAgent = agents.get(randomNum);
		System.out.println(String.format("Get random agent %d to kill", selectedAgent.getId()));
		return selectedAgent;
	}
	
    /** debug method */
    private void print(String s){
		System.out.println(getClass()+"::"+s);
    }
    
    /**
     * 
     * @param <code>context</code> the context contains all agents
     */
    public void setContext(Context context){
    	this.context = context;
    }

    /**
     * 
     * @return agent count
     */
	public int getAgentCount() {
		return agentCount;
	}
	
	/**
	 * 
	 */
	public int getUnfinishedTasks(){
		return returnedMessages.size();
	}
	
	/**
	 * 
	 * @return tick
	 */
    public static int getTick() {
		return tick;
	}

	public static int getNonKillAgentId1() {
		return NonKillAgentId1;
	}

	public static int getNonKillAgentId2() {
		return NonKillAgentId2;
	}

	public static int getNonKillAgentId3() {
		return NonKillAgentId3;
	}
	
//	public int getTickToKill2(){
//		int actualKill=(int) (Math.floor((double)totalTick/tickToKill));
//		System.out.println("actualKill round down = "+actualKill);
//		double needToKill2= (agentCount*agentOpenness-actualKill);
//		System.out.println("needToKill2 = "+needToKill2);
//		if (needToKill2==0){
//			tickToKill2=-1;//if tickToKill2=-1 then do not do extra kill
//		}
//		else{
//			tickToKill2=(int) Math.floor((double)totalTick/needToKill2);
//		}
//		
//		return tickToKill2;
//		
//	}
//	
	
//	public void checkTimeToKill1(){
//    	if(timeToKill()){
//    		System.out.println(String.format("tick %d, time to kill!", tick));
//    		Agent selectedAgent = getOneAgent();
//        	while(killingQueue.contains(selectedAgent)){
//        		selectedAgent = getOneAgent();
//        	}
//        	killingQueue.add(selectedAgent);
//        	System.out.println(String.format("Add agent %d into killing queue", selectedAgent.getId()));
//    	}
//	}
//	
//	public void checkTimeToKill2(){
//    	if(timeToKill2()){
//    		System.out.println(String.format("tick %d, time to kill!", tick));
//    		Agent selectedAgent = getOneAgent();
//        	while(killingQueue.contains(selectedAgent)){
//        		selectedAgent = getOneAgent();
//        	}
//        	killingQueue.add(selectedAgent);
//        	System.out.println(String.format("Add agent %d into killing queue", selectedAgent.getId()));
//    	}
//
//	}

	public int getTicksToFinish() {
		return ticksToFinish;
	}

	public void setTicksToFinish(int ticksToFinish) {
		this.ticksToFinish = ticksToFinish;
	}

	public String getTaskDistrubution() {
		return TaskDistrubution;
	}

	public void setTaskDistrubution(String taskDistrubution) {
		TaskDistrubution = taskDistrubution;
	}

	public String getHardTaskPerString() {
		return hardTaskPerString;
	}

	public void setHardTaskPerString(String hardTaskPerString) {
		this.hardTaskPerString = hardTaskPerString;
	}

	public String getAverageTaskPerString() {
		return averageTaskPerString;
	}

	public void setAverageTaskPerString(String averageTaskPerString) {
		this.averageTaskPerString = averageTaskPerString;
	}
	
	
}
