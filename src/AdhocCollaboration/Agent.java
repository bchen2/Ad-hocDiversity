/**
 * 
 */
package AdhocCollaboration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class Agent {
	private Parameters params;
	private int initalCapNum;//Number of  capabilities agents have when created
	private int agentId;
	private int option;
	private int agentType;/* 1 - expert, 2 - average ,3-novice*/
	private int finishedSubtasks;
	private int numFailedSubtasks;
	private double w;
	private double maxQuality;
	private boolean isBusy;
	//private double agentOpenness;
    private Blackboard bb;
	private Random random;
	private BlackboardMessage taskToBidMessage;//stores the task that this agent is about to bid at this tick
	private BlackboardMessage taskToBeExcutedMessage;	// a blackboard message contains assigned task to be executed	
	private ArrayList<SubTask> subtaskToBeExecuted; // 
	private ArrayList<Double> qualityList;	// quality for this agent(i.e. qualityList[0] is the quality for cap 1)
	private HashMap<Task,ArrayList<SubTask>> myTaskMap; // task, list of subtasks 
	private int ticksToFinishRunning;
	private HashMap<Integer, HashMap<Integer, Integer>> blackList;// agentId, subtaskId, failedTimes
	ArrayList<BlackboardMessage> historyMessage;
	BlackboardMessage temporaryMessage;// contains one last participated message which learning gain has not been calculated yet
	private double agentOpennessPerception;
	private HashMap<Integer,Double> agentOpennessPerceptionMap;//maps tick number to agentOpennessPerception. 
	private ArrayList<Double> agentOpennessPerceptionList;//record the agentOpennessPerception.
	private double taskOpennessPerception;
	private Set<Integer> newTaskSet;//store task_type
	private Set<Integer> encounteredTaskSet;//store task_id
	private HashMap<Integer,Double> taskOpennessPerceptionMap;//maps tick number to taskOpennessPerception. 
	private ArrayList<Double> taskOpennessPerceptionList;//record the taskOpennessPerception.
	private Set<Integer> knownAgentSet;//record all the agents that this agent has cooperated in a task before
	private int agentOpennessOption;//--1. Agent knows only its collaborator.  --2.Agents share info and every agent has the same agent openness. --3.Given the exact agent openness in the beginning
	private int taskOpennessOption;//--1.Agent calculates task openness based on the tasks it has seen by itself. --2.Agents share info and every agent has the same task openness  --3. Given the exact task openness in the beginning
	private double individualLearnedCap;
	private int taskAssignmentAtOneTick;
	private ArrayList<Double> capDiff;//records the difference of cap-qualityThreshold of a task
	private int numTaskInvolved;//records the total number of tasks this agent get involved in. will be updated when agents read assignment
	private boolean neverAssignedJob;
	
	private int TaskIdForLearningGain=0;//When agent update learning gain, this records the taskId that this agent gained learning from
	private double TaskLearningGain=0;//the learning gain from the task it finishes 
	private int TaskTobidAtOneTick=0;//records the task that the agent bid at that tick, if agent did not bid, then it will be 0;
	private int NumberOfAvailableTasksAtOneTick;//reords number of Available Tasks at this tick when agent check the blackboard to check all the tasks,when agent is executing task, this will be -1;
	
	/*code for potential Utility decay
	 * it records the number of times that an agent bid for a task and failed,
	 * when agent compute the potential utility of the avaliable tasks to determine which task to bid, it will look at this map and find out if the task
	 * he has bid before and failded, if it is the case , then the total potential utility for this task will be discounted
	 * */
	private HashMap<Integer,Integer> biddingFailRecordMap; // map taskType to number of times that bid fails
	double decayFactor=0.1;
	
	
	public Agent(int parmAgentId, Blackboard b, int type, double agentQualityMax,int initalCapNummber) {
		//print("******adding a new agent now");
		initalCapNum=initalCapNummber	;	
		agentId = parmAgentId;
		agentType = type;
		finishedSubtasks = 0;
		maxQuality = agentQualityMax;
		bb = b;
		random = new Random();
		taskToBeExcutedMessage = null;
		subtaskToBeExecuted = new ArrayList<SubTask>();
		blackList = new HashMap<Integer, HashMap<Integer, Integer>>();
		taskToBidMessage = null;
		ticksToFinishRunning = 0;
		numFailedSubtasks = 0;
		qualityList = new ArrayList<Double>();
		myTaskMap = new HashMap<Task, ArrayList<SubTask>>();// records the task and subtasks this agent has
		historyMessage = new ArrayList<BlackboardMessage>();
		temporaryMessage = null; // contains one last participated message which learning gain has not been calculated yet
		for(int i=0; i<20; i++) {
			qualityList.add(i, 0.0);
		}
		
		biddingFailRecordMap=new HashMap<Integer,Integer>();
		
		/*AgentQualityHelper
		 *  This object helps  generate  quality/capability for this agent
		 *  This object takes initialCapNum and type (expert=1, Average=2, Novice=3) and gives back agent quality of the 
		 *  required type
		 */
		AgentQualityHelper agentType = new AgentQualityHelper(); 
		
		// This will contain the Initial Non zero Quality of this agent.
		double [] agentQualityList = new double [initalCapNum]; 
		
		
		// type (Expert, Average or Novice Agents)
		
		if (type ==1 ) //Get Expert Agent Quality
		{
			agentQualityList=agentType.GetExpertAgent(initalCapNum);
		} 
		else if (type==2){ // Get Average Agent Quality
			agentQualityList=agentType.GetAverageAgent(initalCapNum);
		}
		else { //Get Novice Agent Quality
			agentQualityList=agentType.GetNoviceAgent(initalCapNum);
		}
		
		
		for(int i = 0; i < initalCapNum; i++){
			int index = random.nextInt(20);		
			while(qualityList.get(index) > 0){
				index = random.nextInt(20);
			}
			// Set agent quality, using the quality array received from the helper class.
			qualityList.set(index, agentQualityList[i]);
		}
		
		print("@@@@@@@@@@@@Agent "+agentId+" Type: "+ agentType.CheckType(agentQualityList) +  "cap is "+qualityList);
		
		params 			= RunEnvironment.getInstance().getParameters();		
		option 			= (Integer) params.getValue ("option");	//agents has different task selection strategies 
		agentOpennessPerception=0;
		agentOpennessPerceptionMap=new HashMap<Integer,Double>();
		agentOpennessPerceptionMap.put(1, 0.0);//Initialize 
		agentOpennessPerceptionList=new ArrayList<Double>(0);
		taskOpennessPerception=1;
		taskOpennessPerceptionMap=new HashMap<Integer,Double>();
		taskOpennessPerceptionMap.put(1, 1.0);//Initialize
		taskOpennessPerceptionList=new ArrayList<Double>(1);
		knownAgentSet=new HashSet<Integer>(getId());//add itself into this set
		agentOpennessOption=(Integer) params.getValue ("agentOpennessOption");
		taskOpennessOption=(Integer) params.getValue ("taskOpennessOption");
		newTaskSet= new HashSet<Integer>();
		encounteredTaskSet=  new HashSet<Integer>();
		individualLearnedCap=0;
		taskAssignmentAtOneTick=0;//0 means no task assignment, task number starts at 1
		capDiff=new ArrayList<Double>();
		numTaskInvolved=0;
		neverAssignedJob=true;
		
//		learningType	= 0;
//		w				= 0.5;
//		agentOpenness 	= (Double) params.getValue("agent_openness");
//		learningSubtask = new SubTask();
//		subtaskList 	= new ArrayList<SubTask>();
//		historyLearningUtility = new ArrayList<HashMap<Integer,HashMap<SubTask,HashMap<Integer,Double>>>>();
//		localSatMap 	= new HashMap<Task, HashMap<SubTask,Double>>();
//		numEncountersTask = new HashMap<Task,Integer>();
//		taskUtility 	= new HashMap<Task,Double>();
//		taskTypeUtility = new HashMap<Integer,Double>();
//		[TODO] Change Hard Coding 
//		Capability id have one to one correspondence to subtask id. Subtasks id start from 1.
//		WARNING!: from 1 to 20, not 0 to 19
//		bb.setAgentOpenness(agentId,agentOpenness);	    
	}
	
	/**
	 * 
	 */
	@Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
			watcheeFieldNames = "newMsg",//when main agent post a task, will set this to be true
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void run() {
		this.TaskIdForLearningGain=0;
		this.TaskLearningGain=0;
		if(temporaryMessage != null){//check last tick finished task to calculate learning gain 
			this.isBusy=false;
			checkFinishedMessage();//this checks if the task is failed or not and calls updateCapbilities 
		}else{
			/*no finished task, do not need to update capbilites */
			
		}
		if(subtaskToBeExecuted.size() > 0){
			this.TaskTobidAtOneTick=0;
			this.NumberOfAvailableTasksAtOneTick=-1;//agent is excecting task, set this to be -1;
			bb.addNumAgentHasResponsed();
			executeSubtasks();
			
		}
		else {
			selectTask();
			bb.addNumAgentHasResponsed();//when all agent has Responded, then it triggers the auction to start
		}
	}

	/**
	 * Select task to bid base on the total difference between each capabilit's quality and each subtask's threshold
	 */
	private void selectTask() {
		ArrayList<BlackboardMessage> taskList = bb.getAllMessages();//get all the tasks posted on blackboard
		this.NumberOfAvailableTasksAtOneTick=taskList.size();
		double maxPotentialUtility = 0.0;
		BlackboardMessage maxPotentialUtilityMessage = null;
		
		//updating newTaskSet and encounteredTaskSet
		for(BlackboardMessage bm : taskList){
			this.newTaskSet.add(bm.getTask().getType());
			this.encounteredTaskSet.add(bm.getTask().getId());
			this.bb.getSharedEncounteredTaskSet().add(bm.getTask().getId());
		}
		
		//update Openness
		this.updateOpenness();
		
//		print("AO = "+this.agentOpennessPerception);
//		print("TO = "+this.taskOpennessPerception);
		
		//Choose task by Calculating potentialUtility accounting to different task selection strategies
		for(BlackboardMessage bm : taskList){
			if(!bm.isAssigned()){
				double potentialUtility = 0.0;
				
				if (option==1){//Strategy 1 choose the task it is most qualified for
					potentialUtility = ComputePotentialUtilityStrategy1(bm);
					
					potentialUtility= addDecay(potentialUtility, bm);

				}
				else if (option==2){//Strategy 2 Max absolute difference    it is the same as U_observe, but we did not just call findU_observe because we don't want to count #of qualified agents for subtasks in U_observe
					potentialUtility = ComputePotentialUtilityStrategy2(bm); 
					potentialUtility= addDecay(potentialUtility, bm);
				}
				
				/**count number of qualified agents for subtasks in U_learn only*/
				else if (option==3){//Strategy 3 Max 1 difference + Max absolute negative difference   ( U=U_learn)
					potentialUtility = ComputePotentialUtilityStrategy3(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==4){//Strategy41 this is the same as strategy3 , we set W_L=1, W_S=0, so we have U=U_learn only.
					potentialUtility = ComputePotentialUtilityStrategy41(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==5){//Strategy42 we set W_L=0, W_S=1, so we have U=U_solve only.  we did not call U_solve, rewrite it again to count number of qualified agents for each subtasks; 
					potentialUtility = ComputePotentialUtilityStrategy42(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				
				/**for option 6-12 we count number of qualified agents for subtasks in U_learn only*/
				
				else if (option==6){//Strategy43 we set W_L=0.5, W_S=0.5, so we have U=0.5*U_learn+0.5*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy43(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==7){//Strategy44 we set W_L=0.25, W_S=0.75, so we have U=0.25*U_learn+0.75*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy44(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==8){//Strategy45 we set W_L=0.75, W_S=0.25, so we have U=0.75*U_learn+0.25*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy45(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==9){//Strategy46 we set W_L=1, W_S=1, so we have U=U_learn+U_solve .
					potentialUtility = ComputePotentialUtilityStrategy46(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==10){//Strategy5 we set W_L=AO, W_S=1-AO, so we have U=AO*U_learn+(1-AO)*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy5(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==11){//Strategy6 we set W_L=1-TO, W_S=TO, so we have U=(1-TO)*U_learn+TO*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy6(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				else if (option==12){//Strategy7 Total Potential Utilities With AO and TO: AO/(AO+TO)*U_learn+ TO/(AO+TO)*U_solve
					potentialUtility = ComputePotentialUtilityStrategy7(bm);
					potentialUtility= addDecay(potentialUtility, bm);
				}
				
//				print("Task "+bm.getTask().getId() +" potential = "+potentialUtility);
				
				//choose the one that has maxPotentialUtility
				if(potentialUtility > maxPotentialUtility){
					/**if an agent does not bid for an task, we set the potentialUtility of that task to be negative infinity, if the agent does not bid for every task
					 * then  potentialUtiltiy for every task will be negative infinity and will not satisify this if condition ,there for maxPotentialUtilityMessage will be null, that is no bid for any task*/
					maxPotentialUtility = potentialUtility ;
					maxPotentialUtilityMessage = bm;
				}
			}
		}
		
		
		
		//agent bid for the task
		if(maxPotentialUtilityMessage != null){
			//add agent to this message's agentBiddingList
//			print("Agent "+agentId+" capbility = "+this.qualityList);
//			print("Agent "+agentId+" maxPotential = "+maxPotentialUtility);
//			print(String.format("Agent %d bids for task %d ", agentId, maxPotentialUtilityMessage.getTask().getId()));
			
//			print("Agent "+agentId+" capDiff for the last task= "+this.capDiff +"\n");
			maxPotentialUtilityMessage.addAgentToBiddingList(this);
			this.TaskTobidAtOneTick=maxPotentialUtilityMessage.getTask().getType();
			taskToBidMessage = maxPotentialUtilityMessage;
		}
		else{
//			print("Agent "+agentId+" capbility = "+this.qualityList);
//			print("Agent "+agentId+" maxPotential = "+maxPotentialUtility);
//			print(String.format("Agent %d bids for No task", agentId));
//			print("Agent "+agentId+" capDiff for the last task = "+this.capDiff+"\n");
			this.TaskTobidAtOneTick=0;
		}
		
	}
	
	/**
	 * 
	 * @param bm 
	 * @return
	 */
	private double ComputePotentialUtilityStrategy1(BlackboardMessage bm){
		double potentialUtility = 0.0;
		for(SubTask subtask : bm.getSubtasks()){
			if(qualityList.get(subtask.getId()-1) > subtask.getQuality()){
				
				/**counting # of qualified agents for subtask*/
				subtask.addQualfiedAgentsCount();
				
				potentialUtility += (qualityList.get(subtask.getId()-1) - subtask.getQuality());
			}
		}
		return potentialUtility;
	}
	
	
	
	/**
	 * Strategy 2 Max absoulute negative difference   (U_observe)
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy2(BlackboardMessage bm){
		//it is the same as U_observe, but we did not just call findU_observe because we don't want to count #of qualified agents for subtasks in U_observe
		
		capDiff.clear();
		double potentialUtility = 0.0;
		
		double maxQualDiff=-1;	
		for (int i=0;i<bm.getSubtasks().size();i++){
			double QualDiff=qualityList.get(bm.getSubtasks().get(i).getId()-1) - bm.getSubtasks().get(i).getQuality();
			
			capDiff.add(QualDiff);
			if(QualDiff>0){
				
				/**counting # of qualified agents for subtask*/
				bm.getSubtasks().get(i).addQualfiedAgentsCount();
			
				if (QualDiff>maxQualDiff){//find maxQualDiff
					maxQualDiff=QualDiff;
				}
			}
		}
		if(maxQualDiff<0){
			 //if no capability is qualified, then do not bid.
			potentialUtility=Double.NEGATIVE_INFINITY;
		}
		else {//has at least one capability meet the requirement, adding the absolute value of the unqualified ones
			int NumSubtaskObsered = 0;
			for (double diff :capDiff){
				if (diff<0){
					potentialUtility +=Math.abs(diff);
					NumSubtaskObsered++;
				}
			}
			
			potentialUtility=(double) potentialUtility/NumSubtaskObsered;//normalize it 
		}
	//print("agent "+ this.agentId+ "potentialUtility for task "+ bm.getTask().getId()+ "=  "+ potentialUtility);
	//print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
		return potentialUtility;
	}
	

	private double findU_observe(BlackboardMessage bm){
		capDiff.clear();
		double potentialUtility = 0.0;
		
		double maxQualDiff=-1;	
		for (int i=0;i<bm.getSubtasks().size();i++){
			double QualDiff=qualityList.get(bm.getSubtasks().get(i).getId()-1) - bm.getSubtasks().get(i).getQuality();
			
			capDiff.add(QualDiff);
			if(QualDiff>0){
				
			
				if (QualDiff>maxQualDiff){//find maxQualDiff
					maxQualDiff=QualDiff;
				}
			}
		}
		if(maxQualDiff<0){
			 //if no capability is qualified, then do not bid.
			potentialUtility=Double.NEGATIVE_INFINITY;
		}
		else {//has at least one capability meet the requirement, adding the absolute value of the unqualified ones
			int NumSubtaskObsered = 0;
			for (double diff :capDiff){
				if (diff<0){
					potentialUtility +=Math.abs(diff);
					NumSubtaskObsered++;
				}
			}
			
			potentialUtility=(double) potentialUtility/NumSubtaskObsered;//normalize it 
		}
	//print("agent "+ this.agentId+ "potentialUtility for task "+ bm.getTask().getId()+ "=  "+ potentialUtility);
	//print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
		return potentialUtility;
	}
	
	
	
	
	/**find U_learn, U_learn= 0.5U_doing+0.5U_observe */
	private double ComputePotentialUtilityStrategy3(BlackboardMessage bm){
		return findU_learn(bm);
	}
	
	/**find U_learn, U_learn= 0.5U_doing+0.5U_observe */
	public double findU_learn(BlackboardMessage bm){
		/**finding U_doing*/
		capDiff.clear();
		double potentialUtility = 0.0;
		int tauJ = -1;
		//find tauJ such that qualCap-requiedCap>0 and the difference is the max difference
		double maxQualDiff=-1;	
		for (int i=0;i<bm.getSubtasks().size();i++){
			double QualDiff=qualityList.get(bm.getSubtasks().get(i).getId()-1) - bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);
			if(QualDiff>0){
				
				/**count number of qualified Agents for subtask*/
				bm.getSubtasks().get(i).addQualfiedAgentsCount();
				
				//potentialUtility += (qualityList.get(subtask.getId()-1) - subtask.getQuality());
				if (QualDiff>maxQualDiff){//find maxQualDiff
					maxQualDiff=QualDiff;
					tauJ=i;
				}
			}
		}
		if(tauJ!=-1){//if found such tauJ
			
			
//			print("Task "+bm.getTask().getId()+" U_doing = "+ maxQualDiff);
			
			/**finding U_observe*/
			//subtaskAll.remove(bm.getSubtasks().get(tauJ));
			 potentialUtility+=0.5*maxQualDiff;/*add U_doing to the  potentialUtility*/
			 
			 int numSubtaskObserved=0;
			 double potentialUtilityFromObservation=0;
			 for(double diff: capDiff){
					//note : agent's qualityList index starts with 0, subtaskId starts with 1
				 	if(diff<0){//add up the absolute value of difference of the unqualified capability and the requirement
				 		potentialUtilityFromObservation += Math.abs((diff));/*add U_observe to the  potentialUtility*/
				 		numSubtaskObserved++;
					}
				 }
			 potentialUtility=potentialUtility+ 0.5*(potentialUtilityFromObservation/numSubtaskObserved);
			 
//			 print("Task "+bm.getTask().getId()+" U_observe= "+ (potentialUtility-maxQualDiff));
		}
		else {//if no qualified capability then do not bid
//			print("Task "+bm.getTask().getId()+" U_doing = -Infinity");
//			print("Task "+bm.getTask().getId()+" U_observe = -Infinity");
			potentialUtility=Double.NEGATIVE_INFINITY;
		}
		
		//print("agent "+ this.agentId+ "potentialUtility for task "+ bm.getTask().getId()+ "=  "+ potentialUtility);
		//print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
//		print("agent "+ this.agentId+ "U_learn for task ="+potentialUtility);
		return potentialUtility;
	}
	
	
	
	
	
	
	
	
	private double findU_solve(BlackboardMessage bm){
		/**find qualThresh_jmax**/
		capDiff.clear();
		double U_solve = 0.0;
		int tauJ = -1;
		//find tauJ such that qualCap-requiedCap>0 and the difference is the max difference
		double maxQualDiff=-1;	
		for (int i=0;i<bm.getSubtasks().size();i++){
			double QualDiff=qualityList.get(bm.getSubtasks().get(i).getId()-1) - bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);
			if(QualDiff>0){
				//potentialUtility += (qualityList.get(subtask.getId()-1) - subtask.getQuality());
				if (QualDiff>maxQualDiff){//find maxQualDiff
					maxQualDiff=QualDiff;
					tauJ=i;
				}
			}
		}
		if(tauJ!=-1){//if found such tauJ
			/**find qualThresh_jmax and the sum then do the division to get U_solve*/
			double qualThresh_jmax = maxQualDiff;
			double R_t=1;//reward for completing the task, we set it for 1 for all task for now, may use different value for different task later
			double sum =0;//this is the sum of each qualThresh of a subtask times its number of required agent
			for (SubTask subtask: bm.getSubtasks()){
				sum=sum+subtask.getQuality()*subtask.getNumAgents();
//				sum=sum+subtask.getQuality();
			}
//			U_solve=(qualThresh_jmax*R_t)/(sum);
			U_solve=5*(qualThresh_jmax*R_t)/(sum);
		}
		else {//if no qualified capability then do not bid
			U_solve=Double.NEGATIVE_INFINITY;
		}
		
//		print("Task "+bm.getTask().getId()+" U_solve = "+U_solve);
		
		return U_solve;
	}
	
	
	
	private double findTotalPitentialUtilities(BlackboardMessage bm,double W_L,double W_S){
		 //W_L weight for learning
	    // W_S weight for solving a task
		
		double U_solve=findU_solve(bm);
		double U_learn=findU_learn(bm);
		
		
		if (U_learn>0){
//			print("W_L= "+W_L+ "  W_L*U_learn= "+W_L*U_learn);
			bb.setNumU_learn(bb.getNumU_learn()+1);
			bb.setU_learnTotal(bb.getU_learnTotal()+U_learn);
		}
		if ( U_solve>0){
//			print("W_S= "+W_S+ "  W_S*U_solve= "+W_S*U_solve);
			bb.setNumU_solve(bb.getNumU_solve()+1);
			bb.setU_solveTotal(bb.getU_solveTotal()+U_solve);
		}
		
		
	
		
		return W_L*U_learn+W_S*U_solve;
	}
	
	
	
 	//Strategy4.1
	private double ComputePotentialUtilityStrategy41(BlackboardMessage bm){
		//this is the same as strategy3 , we set W_L=1, W_S=0, so we have U=U_learn only.
		return findTotalPitentialUtilities( bm,1,0);
	}
	
 	//Strategy4.2
	private double ComputePotentialUtilityStrategy42(BlackboardMessage bm){
		//this strategy is the same as just call U_solve but it counts number of qualified agents for each subtasks
		//rewrite U_solve here, since we do not want to count number of qualified agents for each subtasks in U_solve, we count it in U_learn
		/**find qualThresh_jmax**/
		capDiff.clear();
		double U_solve = 0.0;
		int tauJ = -1;
		//find tauJ such that qualCap-requiedCap>0 and the difference is the max difference
		double maxQualDiff=-1;	
		for (int i=0;i<bm.getSubtasks().size();i++){
			double QualDiff=qualityList.get(bm.getSubtasks().get(i).getId()-1) - bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);
			if(QualDiff>0){
				//potentialUtility += (qualityList.get(subtask.getId()-1) - subtask.getQuality());
				if (QualDiff>maxQualDiff){//find maxQualDiff
					maxQualDiff=QualDiff;
					tauJ=i;
				}
			}
		}
		if(tauJ!=-1){//if found such tauJ
			/**find qualThresh_jmax and the sum then do the division to get U_solve*/
			double qualThresh_jmax = maxQualDiff;
			double R_t=1;//reward for completing the task, we set it for 1 for all task for now, may use different value for different task later
			double sum =0;//this is the sum of each qualThresh of a subtask times its number of required agent
			for (SubTask subtask: bm.getSubtasks()){
				sum=sum+subtask.getQuality()*subtask.getNumAgents();
//				sum=sum+subtask.getQuality();
			}
//			U_solve=(qualThresh_jmax*R_t)/(sum);
			U_solve=5*(qualThresh_jmax*R_t)/(sum);
		}
		else {//if no qualified capability then do not bid
			U_solve=Double.NEGATIVE_INFINITY;
		}
	
		return U_solve;
	}
	
 	//Strategy4.3
	private double ComputePotentialUtilityStrategy43(BlackboardMessage bm){
		// we set W_L=0.5, W_S=0.5, so we have U=0.5*U_learn+0.5*U_solve .
		return findTotalPitentialUtilities( bm,0.5,0.5);
	}
	
	
 	//Strategy4.4
	private double ComputePotentialUtilityStrategy44(BlackboardMessage bm){
		// we set W_L=0.25, W_S=0.75, so we have U=0.25*U_learn+0.75*U_solve .
		return findTotalPitentialUtilities( bm,0.25,0.75);
	}
	
 	//Strategy4.5
	private double ComputePotentialUtilityStrategy45(BlackboardMessage bm){
		// we set W_L=0.75, W_S=0.25, so we have U=0.75*U_learn+0.25*U_solve .
		return findTotalPitentialUtilities( bm,0.75,0.25);
	}
	
	
	
 	//Strategy4.6
	private double ComputePotentialUtilityStrategy46(BlackboardMessage bm){
		// we set W_L=1, W_S=1, so we have U=1*U_learn+1*U_solve .
		return findTotalPitentialUtilities( bm,1.0,1.0);
	}
	
	
	
	//Total Potential Utilities With AO:
	private double ComputePotentialUtilityStrategy5(BlackboardMessage bm){
		// we set W_L=AO, W_S=1-AO, so we have U=AO*U_learn+(1-AO)*U_solve .
		double W_S=1-this.agentOpennessPerception;
//		print("~~~~~~~~~~~~~~~1-AO= "+W_S);
		return findTotalPitentialUtilities( bm,this.agentOpennessPerception,W_S);
	}
	
	//Total Potential Utilities With TO:
	private double ComputePotentialUtilityStrategy6(BlackboardMessage bm){
		// we set W_L=1-TO, W_S=TO, so we have U=(1-TO)*U_learn+TO*U_solve .
		return findTotalPitentialUtilities( bm,(1-this.taskOpennessPerception),this.taskOpennessPerception);
	}
	
	
	//Total Potential Utilities With AO and TO:
	private double ComputePotentialUtilityStrategy7(BlackboardMessage bm){
		double W_L=this.agentOpennessPerception/(this.agentOpennessPerception+this.taskOpennessPerception);
		double W_S=this.taskOpennessPerception/(this.agentOpennessPerception+this.taskOpennessPerception);
		return findTotalPitentialUtilities( bm,W_L,W_S);
	}
	
	
	/**
	 * 
	 */
	@Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
			watcheeFieldNames = "newAssignments",//when action is done (main agent has announced the action result, it sets newAssignments=true and trigger agents to read assignment 
			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void readAssignment(){
		
		if(taskToBidMessage != null){//if agent did not bid for task (due to it is busy) then it will not read assignment( this method does nothing)
			//read its own subtask assignment and store it in the arrayList subtaskToBeExecuted
			subtaskToBeExecuted = taskToBidMessage.getSubtasksAssignmentByAgent(agentId);
			if(subtaskToBeExecuted.size() > 0){//if this agent has an assignment 
				if (this.neverAssignedJob){
					this.bb.setNumUnassignedAgents(this.bb.getNumUnassignedAgents()-1); 
					this.neverAssignedJob=false;
				}
				
				//increasing the count of numTaskInvolved
				this.numTaskInvolved++;

				taskToBeExcutedMessage = taskToBidMessage;
				
//				this.taskAssignmentAtOneTick=taskToBeExcutedMessage.getTask().getId();
				this.taskAssignmentAtOneTick=taskToBeExcutedMessage.getTask().getType();
				
				//adding all the collaborators into knownAgentSet and SharedKnowAgentSet
				for (SubTask subtask: taskToBeExcutedMessage.getSubtasks()){
					this.knownAgentSet.addAll(taskToBeExcutedMessage.getSubtasksAssignmentBySubtaskId(subtask.getId()));	
					this.bb.getSharedKnowAgentSet().addAll(taskToBeExcutedMessage.getSubtasksAssignmentBySubtaskId(subtask.getId()));
				}
				
				
				ticksToFinishRunning = taskToBeExcutedMessage.getTask().getTickToFinish();
				isBusy = true;
			}else{//this agent did not have an assignment i.e. did not win this task in the auction
				
				 updateBidingFailingRecord();
				
			}
			
			
			taskToBidMessage = null;
		}
		else 
			this.taskAssignmentAtOneTick=0;//no task assignment at this tick
	}
	
	/**
	 * Execute subtasks of a task. 
	 * Update local counter of finisheSubtasks
	 * Update blackboard's counter of finishedSubtasks
	 */
	private void executeSubtasks() {
		if(ticksToFinishRunning > 1){
			ticksToFinishRunning--;
			Task task = taskToBeExcutedMessage.getTask();
			int remainingTicks=task.getTickToFinish()-ticksToFinishRunning ;	
			print(String.format("Agent %-5d start executing subtask(s) of task%-5d  %d/%d",this.agentId, task.getId(), remainingTicks, task.getTickToFinish()));
//			for(int i = 0; i < subtaskToBeExecuted.size(); i++){
//				try{
////					Thread.sleep(10);
//				}
//				catch(Exception e){}
//			}
		}
		else{
			Task task = taskToBeExcutedMessage.getTask();
			//print(String.format("Agent %d start executing subtasks of task%d",this.agentId,task.getId()));
//			print(String.format("Agent %-5d start executing subtasks of task%-5d  %d/%d",this.agentId, task.getId(), task.getTickToFinish(), task.getTickToFinish()));
			int numFinishedSubtasks = 0;
			for(SubTask subtask : subtaskToBeExecuted){
				try{
//					Thread.sleep(10);
					
					
					/**
					// 5% failure rate for each agent each subtask. Will be changed base on the difference between required capability and agent capability
					if(random.nextInt(100) < 5){ //subtask failed
						taskToBeExcutedMessage.setSubtaskFailed(subtask, agentId);
						numFailedSubtasks++;//update the total numFailedSubtasks of this particular agent
					}
					else{//subtask succeed
						numFinishedSubtasks++;
						taskToBeExcutedMessage.setSubtaskFinish(subtask);
					}
					*/
					numFinishedSubtasks++;
					taskToBeExcutedMessage.setSubtaskFinish(subtask);
				}
				catch(Exception e){}
			}
			myTaskMap.put(task, subtaskToBeExecuted);//store it here for later reference 
			finishedSubtasks += numFinishedSubtasks;//update the total finishedSubtasks of this particular agent
			temporaryMessage = taskToBeExcutedMessage;
			taskToBeExcutedMessage = null;
			subtaskToBeExecuted.clear();
			
			bb.setNumFinishedSubtasks(numFinishedSubtasks);//update the total NumFinishedSubtasks on blackboard
//			isBusy = false;
			print(String.format("Agent %d finished %d subtasks of task %-4d",this.agentId, numFinishedSubtasks,task.getId()));
		}
	}
	
	/**
	 * agent calculate learning gain after task's completion(next tick)
	 * update learning by doing and learning by observation together
	 */
	private void updateCapabilities(){
		double c = 2.0;
		double eta = 0.01;
		double epsilon = 0.001;
		//learn by doing from the temporaryMessage
		Task task = temporaryMessage.getTask();
		
		
		
		
		ArrayList<SubTask> subtaskAll = new ArrayList<SubTask>(task.getSubtasks());
		print(this.agentId+" read assingment  "+temporaryMessage.getAssignment());
		print(this.agentId+" to do subtask "+temporaryMessage.getSubtasksAssignmentByAgent(agentId));
		for (SubTask subtask: temporaryMessage.getSubtasksAssignmentByAgent(agentId)){
			double elearn=0.4;//learn type is learn by Practice, has effectiveness of 2
			int subtaskId=subtask.getId();
			int capId=subtaskId;
			double LearningGain=(eta/(this.getCapabilityQuality(subtaskId)+epsilon));//self learn    getCapabilityQuality(subtaskId)=qualityList.get(subtaskId-1)
			double DeltaQuality=elearn*LearningGain; //the total quality gain from learning
			this.bb.updateLearnedCap(DeltaQuality);//record learned quality
			this.updateIndividualLearnedCap(DeltaQuality);//record individual learned quality
			this.TaskLearningGain=TaskLearningGain+DeltaQuality;
			
		
			
			double newQuality = (double)this.getCapabilityQuality(subtaskId) + DeltaQuality;
			if (newQuality>1){//max is 1
				newQuality=1;
			}
			print(String.format("Agent %d 's capability %d gained %.4f from completing subtask%d (from task%d),    %.4f---> %.4f \n",this.getId(),capId,DeltaQuality,subtaskId,task.getId(),this.getCapabilityQuality(subtaskId),newQuality));
//			System.out.printf("Agent %d 's capability %d gained %.4f from completing subtask%d (from task%d),    %.4f---> %.4f \n",this.getId(),capId,DeltaQuality,subtaskId,task.getId(),this.getCapabilityQuality(subtaskId),newQuality);
			
			//update newQuality
			this.setCapabilityQuality(capId, newQuality);
			subtaskAll.remove(subtask);//remove the subtask executed by this agent  from all the subtasks of this task, these remaining subtasks are the ones this agent learn from observing	
		}
		
		//Calculate learning by observation gain
		for(SubTask observingSubtask : subtaskAll){
			int capId = observingSubtask.getId();
			ArrayList<Integer> agentIdList = temporaryMessage.getSubtasksAssignmentBySubtaskId(observingSubtask.getId());
			double maxGain = 0;
			double gain;
			double diff;
			//if a subtask has more than one agent to do, choose the agent who can give the max learning from observing gain to learn from 
			for(Integer agentId : agentIdList){
				diff = bb.getAgent(agentId).getCapabilityQuality(capId) - this.getCapabilityQuality(capId);
				if(diff > 0){
					gain = calcuateLearningByObservationGain(diff);
					if(gain > maxGain){
						maxGain = gain;
					}
				}
			}
			double updatedQuality = getCapabilityQuality(observingSubtask.getId()) + maxGain;
			this.bb.updateLearnedCap(maxGain);//record learned quality
			this.updateIndividualLearnedCap(maxGain);//record individual learned quality
			this.TaskLearningGain=TaskLearningGain+maxGain;
			
			print(String.format("Agent %d gain learning %.4f utility by observating,	%.4f---> %.4f ", agentId, maxGain, getCapabilityQuality(observingSubtask.getId()), updatedQuality));
			this.setCapabilityQuality(capId, updatedQuality);
		}
		
		this.TaskIdForLearningGain=task.getType();
		print("~~~~~~~~~"+this.agentId+"    "+this.TaskIdForLearningGain);
		temporaryMessage = null;
//		print("#############oberving size: "+subtaskAll.size());
//		// learn by observing from the other subtasks other than the ones in subtaskToBeExecuted
//		for (SubTask subtaskOberving:subtaskAll){
//			double elearn=0.2;//learn type is learn by Observing, has effectiveness of 1
//			int subtaskId=subtaskOberving.getId();
//			print("@@@@@@subtaskId="+subtaskId);
//			int capId=subtaskId-1;
//			print("learning from "+taskToBeExecuted.getAssignment().get(subtaskId));
//			int AgentToLearnFromId=taskToBeExecuted.getAssignment().get(subtaskId).get(0);//get the first agent's id from the assignment for this subtask, we can choose the one that have max learning gain
//			//TODO one that have max learning gain
//			print("!!!!!!!!!AgentToLearnFromId"+AgentToLearnFromId);
//			print("==========================================");
//			//print("@@@@@@@@@@@@ai"+(double)this.getCapabilityQuality(subtaskId));
//			//get the capbility of the second agent 
//			print("@@@@@@@@@@@@@@getting agent "+bb.getAgentMap().get(AgentToLearnFromId).agentId);
//			print("cap is"+bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId));
//			//print("@@@@@@@@@@@@other "+(double)bb.getAgentList().get(AgentToLearnFromId-1).getCapabilityQuality(subtaskId));
//			if (this.getCapabilityQuality(subtaskId)<bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId)){
//				double LearningGain=(c*c - Math.pow((( (double)bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId))-(double)this.getCapabilityQuality(subtaskId) - c),2))/((double)this.getCapabilityQuality(subtaskId)+epsilon);//learn by observation    getCapabilityQuality(subtaskId)=qualityList.get(subtaskId-1)
//				double DeltaQuality=elearn*LearningGain; //the total quality gain from learning
//				double newQuality = (double)this.getCapabilityQuality(subtaskId) + DeltaQuality;
//				if (newQuality>1){//max is 1
//					newQuality=1;
//				}
//				System.out.printf("Agent %d 's capability %d gained %.4f from oberving subtask%d (from task%d) from Agent%d,    %.4f---> %.4f ",this.getId(),capId,DeltaQuality,subtaskId,task.getId(),AgentToLearnFromId,this.getCapabilityQuality(subtaskId),newQuality);
//				print();
//				//update newQuality
//				this.setCapabilityQuality(capId, newQuality);
//			}
//			else 
//			{
//				print("No learning!!!");
//			}
//		}	
	}

	/**
	 * check finished task from last tick and update learning gain
	 * If a task failed, add agents who failed the subtask into blacklist.
	 */
	private void checkFinishedMessage(){
		if(temporaryMessage.getTaskFinishedFlag() == -1){//task failed
			for(SubTask subtask : temporaryMessage.getSubtasks()){
				for(Integer agentId : subtask.getFailedAgentList()){
					if(blackList.containsKey(agentId)){
						if(blackList.get(agentId).containsKey(subtask.getId())){
							int times = blackList.get(agentId).get(subtask.getId()) + 1;
							blackList.get(agentId).put(subtask.getId(), times);
						}
						else{
							blackList.get(agentId).put(subtask.getId(), 1);
						}
					}
					else{
						blackList.put(agentId, new HashMap<Integer, Integer>());
						blackList.get(agentId).put(subtask.getId(), 1);						
					}
				}
			}
		}
		//though task is failed, agent still gained capabilities 
		updateCapabilities();
		historyMessage.add(temporaryMessage);
		temporaryMessage = null;
	}
	
	/**
	 * 
	 * @param diff
	 * @return learningGain
	 */
	private double calcuateLearningByObservationGain(double diff){
		double gain;
		if(diff < 0.25){
			gain = (-4.0/5.0) * Math.pow(diff, 2) + (2.0 * diff / 5.0);
		}
		else{
			gain = (-4.0 * Math.pow(diff, 2) / 45.0) + (2.0 * diff / 45.0) + (2.0 / 45.0);
		}
		return gain;
	}

//	@Watch (watcheeClassName = "AdhocCollaboration.Task",
//			watcheeFieldNames = "isFinished",
//			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
//	/**
//	 * Calculate utility when the task is finished
//	 * [TODO] Decide to teach or learn
//	 */
//	public void calculateUtility(){
//		for(Entry<Task, ArrayList<SubTask>> entry : myTaskMap.entrySet()){
//			Task task = entry.getKey();
//			if(task.getFinished()){
//				double localSat = 0.0;
//				double utility = 0.0;
//				if(localSatMap.containsKey(task) && !taskUtility.containsKey(task)){
//					Set<SubTask> subtaskSet = localSatMap.get(task).keySet();
//					double cost = 0;
//					for (int i = 0; i < subtaskSet.size(); i++){
//						SubTask subtask = subtaskSet.iterator().next();
//						localSat += localSatMap.get(task).get(subtask);
//						//[TODO] add cost
//					}
//					utility = localSat + task.getGlobalSat() - cost;
//					taskUtility.put(task, utility);
//				}
//			}
//		}
//	}
	
	/**
	 * [TODO] Agent make decision to teach or learn
	 */
	
	/**
	 * [TODO] Agent decide who to learn, what to learn and how to learn
	 */
	
	/**
	 * [TODO] Agent update its capabilities' quality after learning
	 */
	
	
	/**
	 * Agent update agentOpennessPerception
	 */
	public void updateOpenness(){
		//Updating AgentOpenness Perciption
		if (agentOpennessOption==1){//Agent knows only its collaborator
			Set<Integer> intersection = new HashSet<Integer>(this.bb.getKilledAgentSet()); // use the copy constructor
			intersection.retainAll(this.knownAgentSet);
			
			if (!intersection.isEmpty()){
				this.agentOpennessPerception=(double) intersection.size()/this.knownAgentSet.size();
			}
			else{
				this.agentOpennessPerception=0;
			}
		}
		else if (agentOpennessOption==2){//Agents share info and every agent has the same agent openness
			//read it from blackboard
			//sharedAgentOpenness get updated at eachtick before main agent post a new task
			this.agentOpennessPerception=this.bb.getSharedAgentOpenness();
					
		}
		else{//agentOpenness option 3--Given the exact agent openness in the beginning 
			this.agentOpennessPerception=this.bb.getAgentOpenness();
		}
		// record agentOpennessPerception to Map
		agentOpennessPerceptionMap.put(MainAgent.getTick(), agentOpennessPerception);
		agentOpennessPerceptionList.add(agentOpennessPerception);
		
		
		//Updating TaskOpenness Perciption
		if (taskOpennessOption==1){//Agent calculates task openness based on the tasks it has seen by itself. 
			//print("===========================");
			//print("agent "+this.agentId);
				this.taskOpennessPerception=(double) newTaskSet.size()/this.encounteredTaskSet.size();
			
		}
		else if (taskOpennessOption==2){// Agents share info and every agent has the same task openness 
			//print("~~~~~====================================2222222222");
			//read it from blackboard
			this.taskOpennessPerception=this.bb.getSharedTaskOpenness();
		}
		else{//taskOpenness option 3--Given the exact task openness in the beginning 
			//print("~~~~~====================================33333333333");
			this.taskOpennessPerception=this.bb.getTaskOpenness();
		}
		// record taskOpennessPerception to Map
		taskOpennessPerceptionMap.put(MainAgent.getTick(), taskOpennessPerception);
		taskOpennessPerceptionList.add(taskOpennessPerception);
	}
	
	
	
/**
 *Bidding fali record is keep in biddingFailRecordMap, it maps task_id to number of times that this agent has bid but fail to win this task
 */
	public void updateBidingFailingRecord(){
		int task_id =taskToBidMessage.getTask().getId();
		if (this.biddingFailRecordMap.get(task_id)!=null){
			int value=this.biddingFailRecordMap.get(task_id);
			biddingFailRecordMap.put(task_id, value+1);
			print("agent "+this.agentId+" has failed on bidding task "+task_id+" one more time "+(value+1));
		}else{
			this.biddingFailRecordMap.put(taskToBidMessage.getTask().getId(), 1);
			print("agent "+this.agentId+" first time failed on bidding task "+task_id);
		}
	}
	
	
	/**
	 * this function discount the potential utility based on the number of times this agent has failed biding this same task
	 */
	public double addDecay(double utility,BlackboardMessage bm){
		double newUtility=0;
		if (this.biddingFailRecordMap.get(bm.getTask().getId())!=null){//this agent has failed biding this task before
			int timeFailed=this.biddingFailRecordMap.get(bm.getTask().getId());
//			newUtility=utility*Math.pow(1-this.decayFactor, timeFailed);
			newUtility=utility-(this.decayFactor*timeFailed);
			if (newUtility<0){
				return 0;
			}
			
//			print("#####agent "+this.agentId+" has failed task "+bm.getTask().getId()+" "+timeFailed+ "times "+utility+"---> "+newUtility);
			return newUtility;
		}
		
		else{
//			print("else~~~~");
			return utility;
		}
		
	}
	
	
	
	
	
	/**
	 * Return quality of a capability
	 */
	public double getCapabilityQuality(int subtaskId){//only need to put taskId in here
		return qualityList.get(subtaskId-1);//agent's cap[0]=taskType1
	}
	
	/**
	 * 
	 */
	public Integer getId(){
		return this.agentId;
	}
	
	/**
	 * 
	 * @param capId 	capability ID
	 * @param quality 	quality of the capability
	 */
	public void setCapabilityQuality(int capId, double quality){
		qualityList.set(capId-1, quality);
	}
	
	/**
	 * get busy status of the agent
	 */
	public boolean getBusy(){
		return isBusy;
	}
	
	/**
	 * set the busy status of the agent
	 */
	public void setBusy(boolean status){
		isBusy = status;
	}
	
	public int getNumFailedSubtasks(){
		return numFailedSubtasks;
}

	public double getAgentOpennessPerception() {
		return agentOpennessPerception;
	}

	public double getTaskOpennessPerception() {
		return taskOpennessPerception;
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

	public double getIndividualLearnedCap() {
		return individualLearnedCap;
	}
	
	public void updateIndividualLearnedCap(double LearnedCap){
		this.individualLearnedCap+=LearnedCap;
	}

	public int getTaskAssignmentAtOneTick() {
		return taskAssignmentAtOneTick;
	}

	public int getNumTaskInvolved() {
		return numTaskInvolved;
	}

	public ArrayList<Double> getQualityList() {
		return qualityList;
	}
	
	public String getQualityListString(){
		String info=new String();
		for (Double quality: this.getQualityList()){
			info=info+quality+",";
		}
		return  info;
	}
	
	public int getKnownAgentSetSize(){
		return this.knownAgentSet.size();
	}
	

	public int getTaskIdForLearningGain() {
		return TaskIdForLearningGain;
	}

	public void setTaskIdForLearningGain(int taskIdForLearningGain) {
		TaskIdForLearningGain = taskIdForLearningGain;
	}

	public double getTaskLearningGain() {
		return TaskLearningGain;
	}

	public void setTaskLearningGain(double taskLearningGain) {
		TaskLearningGain = taskLearningGain;
	}

	public boolean isBusy() {
		return isBusy;
	}

	public int getTaskTobidAtOneTick() {
		return TaskTobidAtOneTick;
	}

	public void setTaskTobidAtOneTick(int taskTobidAtOneTick) {
		TaskTobidAtOneTick = taskTobidAtOneTick;
	}

	public int getNumberOfAvailableTasksAtOneTick() {
		return NumberOfAvailableTasksAtOneTick;
	}

	public void setNumberOfAvailableTasksAtOneTick(
			int numberOfAvailableTasksAtOneTick) {
		NumberOfAvailableTasksAtOneTick = numberOfAvailableTasksAtOneTick;
	}
	
	public String getTaskId_And_LearningGain(){
		String s=this.TaskIdForLearningGain+","+this.TaskLearningGain;
		 return s;
	}

	public int getAgentType() {
		return agentType;
	}

	public void setAgentType(int agentType) {
		this.agentType = agentType;
	}
	
	
	
	

}//end class

/**
 * 
 * @author Xi Chen
 * Compare two instance of Agent base on a specify capability
 * Return -1 if agent a has higher quality of that capability
 * Return 0 if agent a has same quality of that capability
 * return 1 if agent a has lower quality of that capability
 */
class AgentQualityComparator implements Comparator<Agent>{
	int capId;
	AgentQualityComparator(int id){
		capId = id;
	}
	@Override
	public int compare(Agent a, Agent b) {
		return a.getCapabilityQuality(capId) < b.getCapabilityQuality(capId) ? 1 : a.getCapabilityQuality(capId) == b.getCapabilityQuality(capId) ? 0 : -1; 
	}
	
}


/***************************************************/

/**
 * @author Anish
 * Helper class for AgentSpecification
 * Just gets quality between Minimum and Maximum Quality Value
 * Note to Self: Collapse this class to a method inside AgentQualityHelper Class.
 */
class Capabilities {


	private double Quality;

	
	public double getQuality() {
		return Quality;
	}
	
	public void setQuality(double quality) {
		Quality = quality;
	}
	
	public void MakeAgentWithQualityBetween(double MinQuality, double MaxQuality){
		
		this.Quality = round1d(MinQuality+Math.random()*(MaxQuality-MinQuality));
		
	}


	
	public static double round1d(double a){
		return a;
	}//end of method
}

/**
 * AgentQualityHelper - Helper class to generate Capabilities value for any agent type
 * @return : double[] AgentSpec  with capability values that corresponds to expert, average or novice agents
 * For example, it can return [0.9,0.9,0.3,0.3,0.3] - Which is the capability of an expert agent etc. 
 */
 class AgentQualityHelper {
	
	private double [] AgentSpec;

	
	public double [] GetExpertAgent(int NumOfCapabilities){
		
		int NumOfExpertCapabilities = (int) ( Math.ceil(NumOfCapabilities/3.0) );  	
		 AgentSpec  = new double[NumOfCapabilities];
		int i=0;
		
		while(i<NumOfExpertCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}
		
		while(i<NumOfCapabilities){
			Capabilities Temp = new Capabilities();
			AgentSpec[i] = round1d(Math.random()*(1.0));
			i++;
		}
		
		return AgentSpec;
	}
	

	public double [] GetAverageAgent(int NumOfCapabilities){
	
		int NumOfAverageCapabilities = (int) ( Math.ceil(NumOfCapabilities/3.0))   ;  // Make the task Average SubTask Dominant  ;   
		
		int NumOfExpertCapabilities = (int) (Math.random() * Math.floor(NumOfCapabilities/3));; 
		
		AgentSpec = new double[NumOfCapabilities];
		int i=0;
	

		while(i<NumOfExpertCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}
		
		
		while(i<NumOfAverageCapabilities + NumOfExpertCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.3, 0.699999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}
		
	
		while(i<NumOfCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.0, 0.699999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;		}
		
		
		return AgentSpec;
	}
	
	
	public double[] GetNoviceAgent(int NumOfCapabilities){
		
		int NumOfExpertCapabilities = (int) (Math.random() * Math.floor(NumOfCapabilities/3));
		int NumOfAverageCapabilities = (int) (Math.random() * Math.floor(NumOfCapabilities/3));
		
		int NumOfNoviceCapabilities = NumOfCapabilities - (NumOfExpertCapabilities + NumOfAverageCapabilities ) ;   
		
		 AgentSpec = new double[NumOfCapabilities];
		int i=0;
		
		while(i<NumOfNoviceCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.0, 0.299999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}
		
		while(i<NumOfNoviceCapabilities+NumOfExpertCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}
		
		while(i<NumOfCapabilities){
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween( 0.3, 0.699999);
			AgentSpec[i] = round1d(0.1+Math.random()*(1.0-0.1));
			i++;
		}
		
		return AgentSpec;
	}
	
	
	public static double round1d(double a){
		return a;
	}//end of method
	
	
	public String CheckType(double [] AgentSpec){
	
		int ExpertCount=0, NoviceCount=0, AverageCount=0;
		
		for (int i=0; i< AgentSpec.length; i++){
		if (AgentSpec[i] >= 0.7){ ExpertCount++;}
		else if(AgentSpec[i] >= 0.3){ AverageCount++;}
		else{ NoviceCount++;}
		}
		
		
		if((double)ExpertCount >=  AgentSpec.length/3)
		{
		//	print("Expert");
		
		return "Expert";
		}
		
		else if ((double)AverageCount >=  AgentSpec.length/3)
		{
			//print("Average");
			return "Average";
		}
		
		else{
//			print("Novice");
			return "Novice";
		}
	}
	

	
}//end class














