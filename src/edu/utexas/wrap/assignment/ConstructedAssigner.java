package edu.utexas.wrap.assignment;

import java.util.Collection;

public class ConstructedAssigner<C extends AssignmentContainer> implements Assigner {
	private AssignmentEvaluator<C> evaluator;
	private AssignmentOptimizer<C> optimizer;
	private Collection<C> containers;
	private double threshold;
	private final int maxIterations;
	
	public ConstructedAssigner(
			AssignmentInitializer<C> initializer,
			AssignmentEvaluator<C> evaluator,
			AssignmentOptimizer<C> optimizer,
			double threshold){
		
		this.containers = initializer.initializeContainers();
		this.evaluator = evaluator;
		this.optimizer = optimizer;
		this.threshold = threshold;
		maxIterations = 100;
		
	}
	
	public void run() {
		int numIterations = 0;
		double value = evaluator.getValue(containers.parallelStream()); 
		while (value > threshold && numIterations < maxIterations) {
			System.out.println("Iteration "+numIterations++ + "\tValue: "+value);
			optimizer.optimize(containers.stream());
			value = evaluator.getValue(containers.parallelStream());
		}
		System.out.println("Final value: "+value);
	}
	
}