/*
    wrap - free and open-source urban transportation modeling software
    Copyright (C) 2017 the wrap project, The University of Texas at Austin

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.utexas.wrap.assignment;

import java.util.Collection;
import java.util.Map;

import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.net.Link;
import edu.utexas.wrap.net.Node;
import edu.utexas.wrap.net.TravelSurveyZone;

/**A label indicating an object may be associated with a link flow in route 
 * choice. Examples include bushes and paths for bush- and path-based 
 * optimizers, respectively.
 * 
 * @author William
 *
 */
public interface AssignmentContainer {

	/**
	 * @return the Mode used for this assignment
	 */
	public Mode vehicleClass();
	
	/**
	 * @return the value of time for this container
	 */
	public Float valueOfTime();

	/**
	 * @return the set of used links in the container
	 */
	public Collection<Link> usedLinks();

	/**
	 * @return the set of link flows from this container
	 */
	public Map<Link, Double> flows();

	public double incurredCost();

	public TravelSurveyZone root();
	
	public double demand(Node n);

	public DemandMap getDemandMap();
	
}
