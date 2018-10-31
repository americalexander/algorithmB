package edu.utexas.wrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {
	protected Map<Node, Set<Link>> outLinks;
	protected Map<Node, Set<Link>> inLinks;
	protected Map<Integer, Node> nodeMap;
	
	public Graph() {
		outLinks = new HashMap<Node, Set<Link>>();
		inLinks = new HashMap<Node, Set<Link>>();
		nodeMap = new HashMap<Integer, Node>();
	}
	
	public Graph(Graph g) {
		outLinks = new HashMap<Node, Set<Link>>();
		for (Node n : g.outLinks.keySet()) {
			outLinks.put(n, new HashSet<Link>(g.outLinks.get(n)));
		}
		inLinks = new HashMap<Node, Set<Link>>();
		for (Node n : g.inLinks.keySet()) {
			inLinks.put(n, new HashSet<Link>(g.inLinks.get(n)));
		}
		nodeMap = g.nodeMap;
	}
	
	public Boolean add(Link link) {
		Node head = link.getHead();
		Node tail = link.getTail();
		Set<Link> headIns = inLinks.getOrDefault(head, new HashSet<Link>());
		Set<Link> tailOuts= outLinks.getOrDefault(tail, new HashSet<Link>());
		
		Boolean altered = headIns.add(link);
		altered |= tailOuts.add(link);
		if (altered) {
			inLinks.put(head, headIns);
			outLinks.put(tail, tailOuts);
			nodeMap.put(link.getHead().getID(), link.getHead());
			nodeMap.put(link.getTail().getID(), link.getTail());
		}
		return altered;
	}
	
	public Collection<Node> getNodes(){
		return nodeMap.values();
	}
	
	public Node getNode(Integer id) {
		return nodeMap.get(id);
	}
	
	public Map<Integer, Node> getNodeMap(){
		return nodeMap;
	}
	
	public Set<Link> getLinks(){
		HashSet<Link> ret = new HashSet<Link>();
		for (Node n : outLinks.keySet()) ret.addAll(outLinks.get(n));
		return ret;
	}

	public Set<Link> outLinks(Node u) {
		return outLinks.getOrDefault(u, new HashSet<Link>());
	}

	public Boolean remove(Link link) {
		Boolean altered = outLinks.get(link.getTail()).remove(link);
		altered |= inLinks.get(link.getHead()).remove(link);
		return altered;
	}

	public void remove(Node node) {
		for (Link link : inLinks.getOrDefault(node, new HashSet<Link>())) {
			outLinks.get(link.getTail()).remove(link);
		}
		for (Link link : outLinks.getOrDefault(node, new HashSet<Link>())) {
			inLinks.get(link.getHead()).remove(link);
		}
		inLinks.remove(node);
		outLinks.remove(node);
	}

	public Integer numNodes() {
		// TODO Auto-generated method stub
		return nodeMap.size();
	}
	
	public Boolean contains(Link l) {
		return outLinks.get(l.getTail()).contains(l) || inLinks.get(l.getHead()).contains(l);
	}

}
