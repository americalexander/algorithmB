package edu.utexas.wrap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Origin extends Node{

	private List<Bush> bushes;
	
	public Origin(Node self) {
		super(self.getIncomingLinks(), self.getOutgoingLinks(), self.getID(), self.isCentroid());
		bushes = new LinkedList<Bush>();
	}

	public List<Bush> getBushes() {
		return bushes;
	}

	Double getDemand(Integer n) {
		Double demand = 0.0;
		for(Bush bush : this.bushes) {
			demand += bush.getDemand(n);
		}
		
		return demand;
	}
	
	/** Build the origin's initial bush using Dijkstra's algorithm
	 * 
	 * Create from the full network an initial bush by finding the
	 * shortest path to each node in the network from the origin, 
	 * then selecting the paths which lead to destinations to which
	 * the origin has demand.
	 * 
	 * @param links all links in the network
	 * @param nodes all nodes in the network
	 * @throws Exception 
	 */
	public void buildBush(Set<Link> links, Map<Integer, Node> nodes, Float vot, Map<Integer, Float> destDemand, VehicleClass c) {
		bushes.add(new Bush(this, nodes, links, vot, destDemand, c));
	}
	
	public void buildBush(Graph g, Float vot, Map<Integer, Float> destDemand, VehicleClass c) {
		this.buildBush(g.getLinks(), g.getNodeMap(), vot, destDemand, c);
	}
	
	public int hashCode() {
		return getID();
	}
}
