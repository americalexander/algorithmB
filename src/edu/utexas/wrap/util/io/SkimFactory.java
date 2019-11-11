package edu.utexas.wrap.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.net.TravelSurveyZone;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * This class provides static methods to read information about Skim rates
 */
public class SkimFactory {
	/**
	 * This method reads a skim file and produces a 2d array mapping between two zones and the skim factor
	 * It expects a csv file with information in the following order:
	 * |origin zone id, destination zone id, ..,..,..,.., skim  factor| //!notice the skim factor is the 6th column!!!
	 * @param file File with skim rates
	 * @param header boolean indicating if the file has a header row
	 * @param graph Graph representation of the network
	 * @return 2d array of floats that can be indexed by the pair of zones storing the skim rates between the zones
	 * @throws IOException
	 */
	public static float[][] readSkimFile(File file, boolean header, Graph graph) throws IOException {
		float[][] ret = new float[graph.numZones()][graph.numZones()];
//		Map<TravelSurveyZone,Map<TravelSurveyZone,Float>> ret = new ConcurrentSkipListMap<TravelSurveyZone, Map<TravelSurveyZone,Float>>(new ZoneComparator());
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(file));
			if (header) in.readLine();
			in.lines().parallel().forEach(line -> processLine(graph,ret,line));
		}
		finally {
			if (in != null) in.close();
		}
		return ret;
	}

	private static void processLine(Graph graph, float[][] ret, String line) {
		String[] args = line.split(",");
		TravelSurveyZone orig = graph.getNode(Integer.parseInt(args[0])).getZone();
		TravelSurveyZone dest = graph.getNode(Integer.parseInt(args[1])).getZone();
		Float cost = Float.parseFloat(args[5]);
//		ret.putIfAbsent(orig, new ConcurrentSkipListMap<TravelSurveyZone, Float>(new ZoneComparator()));
		ret[orig.getOrder()][dest.getOrder()] = cost;
//		ret.get(orig).put(dest, cost);
	}

	/**
	 * This metord returns a comparator between two zones based on their order value
	 */
	public static class ZoneComparator implements Comparator<TravelSurveyZone> {

		@Override
		public int compare(TravelSurveyZone z1, TravelSurveyZone z2) {
			return (z1.getOrder() - z2.getOrder());
		}
	}


}