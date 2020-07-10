package edu.utexas.wrap.assignment.bush.algoB;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import edu.utexas.wrap.assignment.AssignmentOptimizer;
import edu.utexas.wrap.assignment.AssignmentProvider;
import edu.utexas.wrap.assignment.AssignmentConsumer;
import edu.utexas.wrap.assignment.bush.Bush;
import edu.utexas.wrap.assignment.bush.BushEvaluator;
import edu.utexas.wrap.assignment.bush.PathCostCalculator;

public class AlgorithmBOptimizer implements AssignmentOptimizer<Bush> {
	private final AssignmentProvider<Bush> provider;
	private final AssignmentConsumer<Bush> consumer;

	private final AlgorithmBUpdater updater;
	private final AlgorithmBEquilibrator equilibrator;

	private final BushEvaluator evaluator;
	private double threshold;
	private int maxIterations = 10;

	public AlgorithmBOptimizer(
			AssignmentProvider<Bush> provider, 
			AssignmentConsumer<Bush> consumer, 
			BushEvaluator evaluator,
			double threshold) {

		this.provider = provider;
		this.consumer = consumer;
		this.evaluator = evaluator;
		this.threshold = threshold;

		updater = new AlgorithmBUpdater();
		equilibrator = new AlgorithmBEquilibrator();
	}

	@Override
	public void optimize(Stream<Bush> bushStream) {
		//		queue = 
		bushStream
		//				.map( bush -> {
		//					try{
		//						provider.getStructure(bush);
		//					} catch (IOException e) {
		//						System.err.println("WARN: Could not find source for "+bush+". Ignoring");
		//						return null;
		//					}
		//
		//					updater.update(bush);
		//
		//					try {
		//						consumer.consumeStructure(bush);
		//					} catch (IOException e) {
		//						System.err.println("WARN: Could not write structure for "+bush+". Source may be corrupted");
		//					}
		//
		//					return bush;
		//				})
		//				.collect(Collectors.toSet());

		//		int numIterations = 0;

		//FIXME: This while loop is inconsistent with design
		//		while (!queue.isEmpty() && numIterations < maxIterations) {
		//			System.out.print("Inner iteration "+numIterations+++"\tQueue length: "+queue.size()+"     \r");
		//			queue = queue
		//					.parallelStream()
		.parallel()
		.forEach(bush -> {
			//						boolean isEquilibrated = false;
			try{
				provider.getStructure(bush);
			} catch (IOException e) {
				System.err.println("WARN: Could not find source for "+bush+". Ignoring");
				return;
			}

			PathCostCalculator pcc = new PathCostCalculator(bush);
			updater.update(bush, pcc);
			
			for (int i = 0; i < maxIterations;i++) {
				try {
					equilibrator.equilibrate(bush, pcc);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Double val = evaluator.getValue(bush);
				//							System.out.println(val);
				if (val < threshold) break;
				pcc.clear();
			}
			

			try {
				consumer.consumeStructure(bush);
			} catch (IOException e) {
				System.err.println("WARN: Could not write structure for "+bush+". Source may be corrupted");
			}
			//						return !isEquilibrated;
		});
		//					.collect(Collectors.toSet());
	}
	//	}


}
