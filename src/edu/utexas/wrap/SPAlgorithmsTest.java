package edu.utexas.wrap;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.jupiter.api.*;

class SPAlgorithmsTest {
	static Graph graph;
	static Node A,B,C,D;
	static Link AB,AC,BC,CD,BD;
	
	@BeforeClass
	void setUpBraess(){
		graph = new Graph();
		
		A = new Node(1);
		B = new Node(2);
		C = new Node(3);
		D = new Node(4);
		
		AB = new Link(A, B, null, null, null, null, null,null) {
			@Override
			public Double getTravelTime() { return 15.0;}
		};
		AC = new Link(A, C, null, null, null, null, null, null) {
			@Override
			public Double getTravelTime() { return 22.0;}
		};
		BC = new Link(B, C, null, null, null, null, null, null) {
			@Override
			public Double getTravelTime() { return 5.0;}
		};
		CD = new Link(C, D, null, null, null, null, null, null) {
			@Override
			public Double getTravelTime() { return 6.0;}
		};
		BD = new Link(B, D, null, null, null, null, null, null) {
			@Override
			public Double getTravelTime() { return 17.0;}
		};

		A.addOutgoing(AB);
		A.addOutgoing(AC);
		B.addIncoming(AB);
		B.addOutgoing(BC);
		B.addOutgoing(BD);
		C.addIncoming(AC);
		C.addIncoming(BC);
		C.addOutgoing(CD);
		D.addIncoming(BD);
		D.addIncoming(CD);
		
		graph.addLink(AB);
		graph.addLink(AC);
		graph.addLink(BC);
		graph.addLink(BD);
		graph.addLink(CD);
		
	}

	@Test
	void braessTestFixed() {
		
		List<Path> shortPath = null;
		try {
			shortPath = SPAlgorithms.kShortestPaths(graph, A, D,3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail(e);
		}
		Path abd = new Path(); abd.add(AB); abd.add(BD);
		Path acd = new Path(); acd.add(AC); acd.add(CD);
		Path abcd = new Path(); abcd.add(AB); abcd.add(BC); abcd.add(CD);
		
		LinkedList<Path> p = new LinkedList<Path>();
		p.add(abcd); p.add(acd); p.add(abd);
		
		if (shortPath == null || !(shortPath.equals(p))) fail("K Short Path returned "+shortPath.toString());
		assertTrue(shortPath != null && shortPath.equals(p));
		assertEquals(shortPath,p);
		
	}

}