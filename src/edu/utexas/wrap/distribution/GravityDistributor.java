package edu.utexas.wrap.distribution;

import java.util.HashMap;
import java.util.Map;

import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.PAMap;
import edu.utexas.wrap.demand.containers.AggregateODHashMatrix;
import edu.utexas.wrap.demand.containers.AggregatePAHashMatrix;
import edu.utexas.wrap.demand.containers.DemandHashMap;
import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.net.Node;

public class GravityDistributor extends TripDistributor {
	private FrictionFactorMap friction;
	private Graph g;

	public GravityDistributor(Graph g, FrictionFactorMap fm) {
		this.g = g;
		friction = fm;
	}
	@Override
	public AggregatePAMatrix distribute(PAMap pa) {
		Map<Node, Double> a = new HashMap<Node,Double>();
		Map<Node, Double> b = new HashMap<Node, Double>();
		AggregatePAMatrix pam = new AggregatePAHashMatrix(g);
		Boolean converged = false;
		while (!converged) {
			converged = true;
			
			for (Node i : pa.getProducers()) {
				Double denom = 0.0;
				
				for (Node z : pa.getAttractors()) {
					denom += b.getOrDefault(z, 1.0) * pa.getAttractions(z) * friction.get(i,z);
				}
				
				if (!a.containsKey(i) || !converged(a.get(i), 1.0/denom)) {
					converged = false;
					a.put(i, 1.0/denom);
				}
			}
		
			for (Node j : pa.getAttractors()) {
				Double denom = 0.0;
				for (Node z : pa.getProducers()) {
					denom += a.get(z) * pa.getProductions(z) * friction.get(j, z);
				}
				
				if (!b.containsKey(j) || !converged(b.get(j), 1.0/denom)) {
					converged = false;
					b.put(j, 1.0/denom);
				}
			}
		}
		
		for (Node i : pa.getProducers()) {
			DemandHashMap d = new DemandHashMap(g);
			
			for (Node j : pa.getAttractors()) {
				d.put(j, (float) (a.get(i)*pa.getProductions(i)*b.get(j)*pa.getAttractions(j)*friction.get(i, j)));
			}
			
			pam.putDemand(i, d);
		}
		return pam;
	}
	
	private Boolean converged(Double a, Double b) {
		Double margin = 2*Math.max(Math.ulp(a), Math.ulp(b));
		return  ((a < b && b-a < margin) || a-b < margin); 
	}
	//TODO Use JDBC to write out the PA Map into the actual database


}