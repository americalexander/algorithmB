package edu.utexas.wrap.demand.containers;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.net.Node;

public class AggregatePAHashMatrix extends HashMap<Integer, DemandMap> implements AggregatePAMatrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4656252954122078293L;

	public void putDemand(Integer i, DemandMap d) {
		put(i,d);
	}

	@Override
	public Object getAttribute(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getVOT() {
		// TODO Auto-generated method stub
		return 0;
	}

}
