package edu.utexas.wrap.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.utexas.wrap.assignment.AssignmentLoader;
import edu.utexas.wrap.demand.containers.AutoODMatrix;
import edu.utexas.wrap.demand.AutoDemandMap;
import edu.utexas.wrap.demand.containers.AutoDemandHashMap;
import edu.utexas.wrap.demand.containers.AutoFixedSizeDemandMap;
import edu.utexas.wrap.demand.containers.DemandHashMap;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.net.Node;
import edu.utexas.wrap.net.TravelSurveyZone;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * This class provides static methods to read infomration relating to demand in a network
 */
public class OriginFactory {
	
	private static Double parse(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/**
	 * This method reads a file containing information about the destination demand
	 * @param of BufferedReader object of the file
	 * @param g The Graph object of the relevant network
	 * @param zoneCount Count of zones in the network
	 * @return A Demand hashmap indicating the demand between an origin and a destination
	 * @throws IOException
	 */
	private static DemandHashMap readDestinationDemand(BufferedReader of, Graph g, Integer zoneCount) throws IOException {
		String[] cols;
		Integer destID;
		Float demand;
		AutoDemandHashMap dests = new AutoDemandHashMap(g,null);

		while (true) {
			String line = of.readLine();
			if (line == null || line.trim().startsWith("O") || line.trim().equals(""))
				break; // If we've reached the gap, move to the next origin
			String[] entries = line.trim().split(";");

			for (String entry : entries) { // For each entry on this line
				cols = entry.split(":"); // Get its values
				destID = Integer.parseInt(cols[0].trim());
				demand = Float.parseFloat(cols[1].trim());
				if (demand > 0.0) {
					TravelSurveyZone tsz = g.getNode(destID).getZone();
					if (tsz == null) { 
						tsz = new TravelSurveyZone(g.getNode(destID),zoneCount++,null);
						g.getNode(destID).setTravelSurveyZone(tsz);
						g.addZone(tsz);
					}
					dests.put(tsz, demand.doubleValue());
				}
			}
		}
		return dests;
	}

	/**
	 * This method reads origin and desitnation demand information relating to the CONAC graph representation
	 * @param odMatrix File containing information about the origin and destination matrix
	 * @param g Graph object representation of the network
	 * @param dl AssigmentLoader object storing the demand that is read from the file to be loaded onto the network
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void readEnhancedTrips(File odMatrix, Graph g, AssignmentLoader dl) throws FileNotFoundException, IOException {
		int numZones = 0;
		BufferedReader matrixFile = new BufferedReader(new FileReader(odMatrix));
		String line;
		Integer curOrig = null;
		AutoODMatrix odda17 = new AutoODMatrix(g, 0.17F, Mode.SINGLE_OCC);
		AutoODMatrix odda35 = new AutoODMatrix(g, 0.35F, Mode.SINGLE_OCC);
		AutoODMatrix odda45 = new AutoODMatrix(g, 0.45F, Mode.SINGLE_OCC);
		AutoODMatrix odda90 = new AutoODMatrix(g, 0.90F, Mode.SINGLE_OCC);
		
		AutoODMatrix odsr17 = new AutoODMatrix(g, 0.17F, Mode.HOV_2_PSGR);
		AutoODMatrix odsr35 = new AutoODMatrix(g, 0.35F, Mode.HOV_2_PSGR);
		AutoODMatrix odsr45 = new AutoODMatrix(g, 0.45F, Mode.HOV_2_PSGR);
		AutoODMatrix odsr90 = new AutoODMatrix(g, 0.90F, Mode.HOV_2_PSGR);
		
		AutoODMatrix odmt = new AutoODMatrix(g, 1.0F, Mode.MED_TRUCK);
		AutoODMatrix odht = new AutoODMatrix(g, 1.0F, Mode.HVY_TRUCK);
		
		AutoDemandMap solo17 = null, solo35 = null, solo45 = null, solo90 = null,
				hov17 = null, hov35 = null, hov45 = null, hov90 = null,
				medTrucks = null, hvyTrucks = null;

		// read each line and map to correct bush identity
		while (true) {
			line = matrixFile.readLine();
			if (line == null || line.trim().equals("")) {
				if (curOrig != null) {
					// build previous bushes
					TravelSurveyZone curZone = g.getNode(curOrig).getZone();
					if (curZone == null) {
						curZone = new TravelSurveyZone(g.getNode(curOrig),numZones++,null);
						g.addZone(curZone);
					}
					
					dl.submit(curZone, solo17);
					dl.submit(curZone, solo35);
					dl.submit(curZone, solo45);
					dl.submit(curZone, solo90);

					dl.submit(curZone, hov17);
					dl.submit(curZone, hov35);
					dl.submit(curZone, hov45);
					dl.submit(curZone, hov90);
					
					dl.submit(curZone, medTrucks);
					dl.submit(curZone, hvyTrucks);
					
					dl.start(curZone);
				}
				break;
			}
			String[] args = line.split(",");

			Integer orig = Integer.parseInt(args[0]);
			Node destNode = g.getNode(Integer.parseInt(args[1]));
			TravelSurveyZone destZone = destNode.getZone();
			if (destZone == null) {
				destZone = new TravelSurveyZone(destNode,numZones++,null);
				destNode.setTravelSurveyZone(destZone);
				g.addZone(destZone);
			}

			Double da35 = parse(args[2]),
			 da90 = parse(args[3]),
			sr35 = parse(args[4]),
			sr90 = parse(args[5]),
			da17 = parse(args[6]),
			da45 = parse(args[7]),
			sr17 = parse(args[8]),
			sr45 = parse(args[9]),
			mdtk = parse(args[10]),
			hvtk = parse(args[11]);

			if (curOrig == null || !orig.equals(curOrig)) {
				// Moving on to next origin
				if (curOrig != null) {
					TravelSurveyZone curZone = g.getNode(curOrig).getZone();
					if (curZone == null) {
						curZone = new TravelSurveyZone(g.getNode(curOrig),numZones++,null);
						g.getNode(curOrig).setTravelSurveyZone(curZone);
						g.addZone(curZone);
					}
					// build previous origin's bushes
					dl.submit(curZone, solo17);
					dl.submit(curZone, solo35);
					dl.submit(curZone, solo45);
					dl.submit(curZone, solo90);

					dl.submit(curZone, hov17);
					dl.submit(curZone, hov35);
					dl.submit(curZone, hov45);
					dl.submit(curZone, hov90);
					
					dl.submit(curZone, medTrucks);
					dl.submit(curZone, hvyTrucks);
					
					dl.start(curZone);

				}

				// Reset maps
				System.out.print("\rReading demand for origin " + orig);
				curOrig = orig;
				solo17 = new AutoDemandHashMap(g,odda17);
				solo35 = new AutoDemandHashMap(g,odda35);
				solo45 = new AutoDemandHashMap(g,odda45);
				solo90 = new AutoDemandHashMap(g,odda90);

				hov17 = new AutoDemandHashMap(g,odsr17);
				hov35 = new AutoDemandHashMap(g,odsr35);
				hov45 = new AutoDemandHashMap(g,odsr45);
				hov90 = new AutoDemandHashMap(g,odsr90);

				medTrucks = new AutoDemandHashMap(g,odmt);
				hvyTrucks = new AutoDemandHashMap(g,odht);
			}

			if (da17 > 0.0F)
				solo17.put(destZone, da17);
			if (da35 > 0.0F)
				solo35.put(destZone, da35);
			if (da45 > 0.0F)
				solo45.put(destZone, da45);
			if (da90 > 0.0F)
				solo90.put(destZone, da90);

			if (sr17 > 0.0F)
				hov17.put(destZone, sr17);
			if (sr35 > 0.0F)
				hov35.put(destZone, sr35);
			if (sr45 > 0.0F)
				hov45.put(destZone, sr45);
			if (sr90 > 0.0F)
				hov90.put(destZone, sr90);

			if (mdtk > 0.0F)
				medTrucks.put(destZone, mdtk);
			if (hvtk > 0.0F)
				hvyTrucks.put(destZone, hvtk);
		}
		
		matrixFile.close();
	}

	/**
	 * Used when each origin has a specific VOT proportion breakdown with a single
	 * class
	 * 
	 * @param odMatrix
	 * @param map
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void readTNTPOriginSpecificProportionalVOTDemand(File odMatrix, Map<Node, List<Double[]>> map, Graph g, AssignmentLoader dl)
			throws FileNotFoundException, IOException {
		/////////////////////////////////////
		// Read OD Matrix and assign flows
		/////////////////////////////////////
		BufferedReader of = new BufferedReader(new FileReader(odMatrix));
		String line;
		
		Map<Double, AutoODMatrix> ods = new Double2ObjectOpenHashMap<AutoODMatrix>();
		int numZones = 0;
		
		do { // Move past headers in the file
			line = of.readLine();
		} while (!line.startsWith("Origin"));

		while (true) { // While more Origins to read
			while (line != null && !line.startsWith("O"))
				line = of.readLine(); // Read in the origin header
			if (line == null || line.trim().equals(""))
				break; // If the origin header is empty, we've reached the end of the list

			Integer origID = Integer.parseInt(line.trim().split("\\s+")[1]);
			Node root = g.getNode(origID); // Retrieve the existing node with that ID
			
			TravelSurveyZone zone = root.getZone();
			
			if (zone == null) {
				zone = new TravelSurveyZone(root,numZones++,null);
				root.setTravelSurveyZone(zone);
				g.addZone(zone);
			}
			System.out.print("\rReading demand for origin " + origID);
			DemandHashMap unified = readDestinationDemand(of, g, numZones);
			
			for (Double[] entry : map.get(root)) {
				ods.putIfAbsent(entry[0], new AutoODMatrix(g, entry[0].floatValue(), null)); //Ensure a parent OD matrix exists
				AutoDemandMap split = new AutoFixedSizeDemandMap(g, ods.get(entry[0]));	//Attach the parent OD
				
				for (TravelSurveyZone dest : unified.getZones()) { //Split each destination proportionally
					split.put(dest, entry[1] * unified.get(dest));
				}
				dl.submit(zone,split);
			}
			
			dl.start(zone);
			
			line = of.readLine();

		}
		g.setNumZones(numZones);
		of.close();

	}

	public static void readTNTPUniformVOTtrips(File VOTfile, File odMatrix, Graph g, AssignmentLoader dl) throws FileNotFoundException {
		if (g == null)
			throw new RuntimeException("Graph must be constructed before reading OD matrix");
		try {
			readTNTPOriginSpecificProportionalVOTDemand(odMatrix, readUniformVOTDistrib(VOTfile,g), g, dl);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param VOTfile
	 * @param g
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Map<Node, List<Double[]>> readUniformVOTDistrib(File VOTfile, Graph g) throws FileNotFoundException, IOException {

		BufferedReader vf = new BufferedReader(new FileReader(VOTfile));
		LinkedList<Double[]> VOTs = new LinkedList<Double[]>();

		String line;
		vf.readLine(); // Ignore header line
		do {
			line = vf.readLine();
			if (line == null)
				break;
			String[] args = line.split("\t");
			Double vot = parse(args[0]);
			Double vProp = parse(args[1]);
			Double[] entry = { vot, vProp };
			VOTs.add(entry);
		} while (!line.equals(""));
		vf.close();

		Map<Node, List<Double[]>> votMap = new Object2ObjectOpenHashMap<Node, List<Double[]>>();
		for (Node n : g.getNodes()) {
			votMap.put(n, VOTs);
		}
		return votMap;
	}
}