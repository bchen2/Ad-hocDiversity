/**
 * 
 */
package AdhocCollaboration;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class LearningType {
	private int typeId;
	private int numLearningTypes;	// 0-by observation, 1-teaching,being taught,apprenticeship, 3-practice, 4-discussion
	
	public LearningType() {
		typeId = 0;
		numLearningTypes = 4;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getNumLearningTypes() {
		return numLearningTypes;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void setId(int id) {
		typeId = id;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getId() {
		return typeId;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public double getClearn(int id) {
		typeId = id;
		double clearn = 0;
		if(typeId==1) {
			clearn = 0.8;
		}
		else if(typeId==0) {
			clearn = 0.2;
		}
		else if(typeId==2) {
			clearn = 0.4;
		}
		else if(typeId==3) {
			clearn = 0.6;
		}
		
		return clearn;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public double getElearn(int id) {
		typeId = id;
		double elearn = 0;
		if(typeId==1) {
			elearn = 0.8;
		}
		else if(typeId==0) {
			elearn = 0.2;
		}
		else if(typeId==2) {
			elearn = 0.4;
		}
		else if(typeId==3) {
			elearn = 0.6;
		}
		
		return elearn;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
