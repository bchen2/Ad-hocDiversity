/**
 * 
 */
package AdhocCollaboration;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class LearningMatch {
	private int agentId;
	private int otherAgentId;
	private int learningType;
	private SubTask subtask;

	/**
	 * Create a new instance of a Learning Match
	 */
	public LearningMatch(int agentId, int learningType, int otherAgentId, SubTask subtask){
		this.agentId = agentId;
		this.learningType = learningType;
		this.otherAgentId = otherAgentId;
		this.subtask = subtask;
	}
	
	/**
	 * @return the agentId
	 */
	public int getAgentId() {
		return agentId;
	}

	/**
	 * @param agentId the agentId to set
	 */
	public void setAgentId(int agentId) {
		this.agentId = agentId;
	}

	/**
	 * @return the otherAgentId
	 */
	public int getOtherAgentId() {
		return otherAgentId;
	}

	/**
	 * @param otherAgentId the otherAgentId to set
	 */
	public void setOtherAgentId(int otherAgentId) {
		this.otherAgentId = otherAgentId;
	}

	/**
	 * @return the learningType
	 */
	public int getLearningType() {
		return learningType;
	}

	/**
	 * @param learningType the learningType to set
	 */
	public void setLearningType(int learningType) {
		this.learningType = learningType;
	}

	/**
	 * @return the subtask
	 */
	public SubTask getSubtask() {
		return subtask;
	}

	/**
	 * @param subtask the subtask to set
	 */
	public void setSubtask(SubTask subtask) {
		this.subtask = subtask;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
