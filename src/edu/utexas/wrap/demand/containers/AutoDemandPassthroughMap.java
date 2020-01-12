package edu.utexas.wrap.demand.containers;

import java.util.Collection;
import java.util.Map;

import edu.utexas.wrap.demand.AutoDemandMap;
import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.net.TravelSurveyZone;

public class AutoDemandPassthroughMap implements AutoDemandMap {

	private DemandMap map;
	private Mode mode;
	private float vot;
	public AutoDemandPassthroughMap(DemandMap map, Mode mode, Float vot) {
		// TODO Auto-generated constructor stub
		this.map = map;
		this.mode = mode;
		this.vot = vot;
	}

	@Override
	public Double get(TravelSurveyZone dest) {
		return map.get(dest);
	}

	@Override
	public Graph getGraph() {
		return map.getGraph();
	}

	@Override
	public Collection<TravelSurveyZone> getZones() {
		return map.getZones();
	}

	@Override
	public Double getOrDefault(TravelSurveyZone node, Double f) {
		return map.getOrDefault(node, f);
	}

	@Override
	public Double put(TravelSurveyZone dest, Double demand) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Map<TravelSurveyZone, Double> doubleClone() {
		return map.doubleClone();
	}

	@Override
	public Float getVOT() {
		return vot;
	}

	@Override
	public Mode getMode() {
		return mode;
	}

}