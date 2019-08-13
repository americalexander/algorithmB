package edu.utexas.wrap.modechoice;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import edu.utexas.wrap.demand.containers.ModalFixedMultiplierPassthroughMatrix;
import edu.utexas.wrap.marketsegmentation.MarketSegment;
import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.ModalPAMatrix;

/**The purpose of this class is to return
 * a set of modal PA matrices that contain
 * the proportion of trips that are taken for
 * a particular mode.
 *
 * @author Karthik & Rishabh
 *
 */
public class FixedProportionSplitter extends TripInterchangeSplitter {

	private Map<MarketSegment, Map<Mode, Float>> map;

	public FixedProportionSplitter(Map<MarketSegment, Map<Mode, Float>> map) {
		this.map = map;
	}

	/**
	 *	This method returns a set of Modal PA matrices
	 *
	 *  This method	essentially iterates through all
	 *  (i,j) pairs for each mode type and then adds the
	 *  respective proportional value to the a new matrix
	 *  which is added to a set.
	 */
	@Override
	public Set<ModalPAMatrix> split(AggregatePAMatrix aggregate, MarketSegment ms) {
		Set<ModalPAMatrix> fixedProp = new HashSet<ModalPAMatrix>(map.size());
		Map<Mode, Float> map = this.map.get(ms);
		for (Mode m : map.keySet()) {
			Float pct = map.get(m);
			if (pct <= 0.0) continue;
			
			ModalPAMatrix pa = new ModalFixedMultiplierPassthroughMatrix(m,pct,aggregate);
			
			fixedProp.add(pa);
		}
		return fixedProp;
	}

}
