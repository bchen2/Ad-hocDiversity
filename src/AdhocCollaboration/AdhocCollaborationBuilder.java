package AdhocCollaboration;
import java.io.FileOutputStream;
import java.io.PrintStream;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;


public class AdhocCollaborationBuilder extends DefaultContext<Object> implements ContextBuilder<Object> {

	
	/* (non-Javadoc)
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		//context.clear();
		context.setId("AdhocCollaboration");
		//read in parameters 
		Parameters params 		= RunEnvironment.getInstance().getParameters();
		int initalCapNummber		=(Integer) params.getValue ("initalCapNum");/*Initial Number of non zero capabilities*/
		int agentCount 			= (Integer) params.getValue ("agent_count");
		double taskOpenness 	= (Double) params.getValue("task_openness");
		int totalTick           =(Integer) params.getValue ("total_tick");
		double agentOpenness 	= (Double) params.getValue("agent_openness");  
		int agentType 			= 0; /* 1 - expert, 2 - average ,3-novice*/
		double agentQualityMax 	= 1;
		int ticksToFinish = (Integer) params.getValue ("tickToFinish");/*ticks required for finishing a task, right now every task has the needs same tick to finish.*/
		
		/*user enter a string of two numbers, not separated by comma. Example: 3040  .this means 30% of Expert, 40% of Average, and 100-(30+40) % of Novice*/
		String AgentDistrubution =  (String) params.getValue("AgentDistrubution");
		
		/*user enter a string of two numbers, not separated by comma. Example: 3333  .this means 33% of HardTask, 33% of AverageTask, and 100-(33+33) % of EasyTask*/
		String TaskDistrubution =  (String) params.getValue("TaskDistrubution");
		
		String HardPercentageStr, AveragePercentageStr;
		HardPercentageStr="HP"+TaskDistrubution.substring(0,2);
		AveragePercentageStr="AP"+TaskDistrubution.substring(2,4);
		
		
		
		
		
		/*
		 * creating a new blackboard
		 */
		Blackboard b 			= new Blackboard();	
		b.setAgentCount(agentCount);
		b.setNumUnassignedAgents(agentCount);
		b.setAgentOpenness(agentOpenness);
		b.setTaskOpenness(taskOpenness);
		context.add(b);
		print("Task openness "+taskOpenness);
		// pass parameters to the main agent
		MainAgent mainAgent = new MainAgent(b,taskOpenness, totalTick,agentCount, agentType, agentOpenness,initalCapNummber,HardPercentageStr,AveragePercentageStr);
		mainAgent.setTaskDistrubution(TaskDistrubution);//pass taskDistribution to the mainAgent, later the mainAgent will choose the congfig file based on the distrubution to pick task orders
//		mainAgent.setHardTaskPerString(HardPercentageStr);
//		mainAgent.setAverageTaskPerString(AveragePercentageStr);
		mainAgent.setTicksToFinish(ticksToFinish);//pass tickTOFinish to mainAgent, so it can allow the last auctioned off task to have extra tick to get finished.
		mainAgent.setContext(context);
		context.add(mainAgent);
		
		/*
		 * start the agents, create agent types depending on agentTypeScenario
		 */
		int expertCount=0;int averageCount=0; int noviceCount=0;
		//String[] token=distrubution.split(",");
		
		String expertCountStr, averageCountStr;
		expertCountStr = AgentDistrubution.substring(0,2);
		averageCountStr = AgentDistrubution.substring(2,4);
		
		expertCount=(int) Math.round((Integer.parseInt(expertCountStr)*agentCount/100.0)) ;
		averageCount=(int) Math.round((Integer.parseInt(averageCountStr)*agentCount/100.0));
		noviceCount=agentCount-expertCount-averageCount;
		
		for(int i=0; i<agentCount; i++) {
			if(i < expertCount){
				agentType= 1; // Expert
			}
			else if (i <expertCount + averageCount){
;				agentType = 2; // Average
			}
			else{
				agentType =3; // Novice
			}
	
			Agent newAgent=new Agent(i,b,agentType,agentQualityMax,initalCapNummber);
			context.add(newAgent);
			/* adding this agent to bb's agentList*/
			b.addAgent(newAgent);
			/*adding this agent to bb's AgentMap*/
			b.AddToAgentMap(i, newAgent);	
		}

		print("return context");
		return context;
	}
	
	
	
	public int getAgentCount () {
		return getObjects (Agent.class).size ();
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



