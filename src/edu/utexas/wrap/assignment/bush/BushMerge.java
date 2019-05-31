package edu.utexas.wrap.assignment.bush;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.utexas.wrap.net.Link;
import edu.utexas.wrap.net.Node;
import edu.utexas.wrap.util.AlternateSegmentPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**A way to represent the Links which merge at a given Node in a Bush,
 * namely, a set of Links with a longest and shortest path named and a
 * Map of each link's share of the total load
 * @author William
 *
 */
public class BushMerge implements BackVector, Iterable<Link>{
	private Link shortLink;
	private Link longLink;
	private AlternateSegmentPair asp;
	private Map<Link, Float> split;
	private final Bush bush;
	
	/** Create a BushMerge by adding a shortcut link to a node
	 * @param b the bush upon whose structure the merge depends
	 * @param u the pre-existing back-vector that is short-circuited
	 * @param l the shortcut link providing a shorter path to the node
	 */
	public BushMerge(Bush b, Link u, Link l) {
		bush = b;


		split = new Object2ObjectOpenHashMap<Link, Float>(2);
		split.put(u, 1.0F);
		split.put(l, 0.0F);
	}
	
	/**Duplication constructor
	 * @param bm	the BushMerge to be copied
	 */
	public BushMerge(BushMerge bm) {
		bush = bm.bush;
		longLink = bm.longLink;
		shortLink = bm.shortLink;
		asp = bm.asp;
		split = new Object2ObjectOpenHashMap<Link,Float>(bm.split);
	}
	
	/**Constructor for empty merge
	 * @param b
	 */
	protected BushMerge(Bush b) {
		bush = b;
		split = new Object2ObjectOpenHashMap<Link,Float>(2);
	}
	
	/**
	 * @return the shortest cost path Link
	 */
	public Link getShortLink() {
		return shortLink;
	}
	
	/**
	 * @return the longest cost path Link
	 */
	public Link getLongLink() {
		return longLink;
	}
	
	/**
	 * @return the shortest and longest path AlternateSegmentPair merging here
	 */
	public AlternateSegmentPair getShortLongASP() {
		if (asp == null) asp = bush.getShortLongASP(shortLink.getHead());
		return asp;
	}
	
	/**Set the shortest path cost link
	 * @param l	the new shortest path Link to here
	 */
	protected void setShortLink(Link l) {
		shortLink = l;
	}
	
	/**Set the longest path cost link
	 * @param l the new longest path Link to here
	 */
	protected void setLongLink(Link l) {
		longLink = l;
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#toString()
	 */
	public String toString() {
		return "Merge at "+longLink.getHead().toString();
	}
		
	/** Remove a link from the merge
	 * @param l the link to be removed
	 * @return whether there is one link remaining
	 */
	public boolean remove(Link l) {
		if (shortLink != null && shortLink.equals(l)) {
			shortLink = null;
		}
		else if (longLink != null && longLink.equals(l)) {
			longLink = null;
		}
		if (split.remove(l) == null) 
			throw new RuntimeException("A Link was removed that wasn't in the BushMerge");
		if (split.size() < 2) return true;
		return false;
	}
	
	/** get a Link�s share of the merge demand
	 * @param l the link whose split should be returned
	 * @return the share of the demand through this node carried by the Link
	 */
	public Float getSplit(Link l) {
		Float r = split.getOrDefault(l, 0.0F);
		if (r.isNaN()) {	//NaN check
			throw new RuntimeException("BushMerge split is NaN");
		}
		return r;
	}

	/**Get the maximum flow that can be shifted from the longest to shortest cost path
	 * @param bushFlows	the current flows on the Bush
	 * @return the nmax flow that can be shifted away from the longest path
	 */
	public double getMaxDelta(Map<Link, Double> bushFlows) {
		//Find the start and end of the ASP
		Node cur = longLink.getHead();
		Node stop = bush.divergeNode(cur);
		//Check all links in the longest path, taking the smallest Bush flow
		Double max = null;
		while (cur != stop) {
			Link ll = bush.getqLong(cur);
			if (max == null) max = bushFlows.get(ll);
			else max = Math.min(max,bushFlows.get(ll));
			cur = ll.getTail();
		}
		return max;
	}

	/**Set the split for a given link
	 * @param l	the link whose split should be set
	 * @param d	the split value
	 * @return	the previous value, or 0.0 if the link wasn't in the Merge before
	 */
	public Float setSplit(Link l, Float d) {
		if (d.isNaN()) {
			throw new RuntimeException("BushMerge split set to NaN");
		}
		Float val = split.put(l, d);
		return val == null? 0.0F : val;
	}

	@Override
	public Iterator<Link> iterator() {
		// TODO Auto-generated method stub
		return split.keySet().iterator();
	}

	public Boolean add(Link l) {
		// TODO Auto-generated method stub
		try { 
			split.put(l, 0.0F);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean contains(Link i) {
		// TODO Auto-generated method stub
		return split.containsKey(i);
	}

	public Set<Link> getLinks() {
		// TODO Auto-generated method stub
		return split.keySet();
	}
}
