package edu.utexas.wrap.util;

import edu.utexas.wrap.marketsegmentation.IncomeGroupSegment;
import edu.utexas.wrap.marketsegmentation.MarketSegment;
import edu.utexas.wrap.modechoice.Mode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides static methods to read information relevant to Mode Choice information and rates
 */
public class ModeFactory {

    /**
     * This method produces a map from market segments to a mapping of modes to their proprtions
     * It expects a .csv with the data in the following order:
     * |Mode, Rate for MarketSegment1, Rate for MarketSegment2, ....|
     * @param file File object with the information
     * @param segs Market segments of interest
     * @return A Mapping between market segments to modes to their proportions
     * @throws IOException
     */
    public static Map<MarketSegment, Map<Mode, Double>> readModeShares(File file, Collection<MarketSegment> segs) throws IOException {
        Map<MarketSegment, Map<Mode, Double>> modeShares = new HashMap<MarketSegment, Map<Mode, Double>>();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));
            String headers = in.readLine();
            String[] headersL = headers.split(",");
            HashMap<MarketSegment, Integer> positions = new HashMap<MarketSegment, Integer>();
            int i = 0;
            while(positions.size() < 3 && i < headersL.length) { //TODO Handle more than HWB Segments
                if(headersL[i].contains("HBW")) {
                    for(MarketSegment m: segs) {
                        IncomeGroupSegment seg = (IncomeGroupSegment) m;
                        if(headersL[i].contains(seg.getIncomeGroup() +"")) {
                            positions.put(m, i);
                        }
                    }
                }
                i++;
            }
            String line = in.readLine();

            while (line != null) {
                String[] args = line.split(",");
                Mode mod = Mode.valueOf(args[0]);
                for(MarketSegment m: segs) {
                    Map<Mode, Double> segInfo = modeShares.getOrDefault(m, new HashMap<>());
                    segInfo.put(mod, Double.parseDouble(args[positions.get(m)]));
                    modeShares.put(m,segInfo);
                }
                line = in.readLine();
            }
        } finally {
            if (in != null) in.close();
        }
        return modeShares;
    }

    /**
     * This method produces a map between a mode and the occupancy rate of the mode
     * It expects a .csv file to contain information in the following order:
     * |Mode, Occupancy rate|
     * @param file File with occupancy rates
     * @param header boolean indicating whether the file has a header row
     * @return A Map between the mode and the occupancy rate
     * @throws IOException
     */
    public static Map<Mode,Double> readOccRates(File file, boolean header) throws IOException {
        Map<Mode, Double> occRates = new HashMap<Mode, Double>();
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));
            if (header) in.readLine();
            String line = in.readLine();

            while (line != null) {
                String[] args = line.split(",");
                Mode mod = Mode.valueOf(args[0]);
                Double rate = Double.parseDouble(args[1]);
                occRates.put(mod, rate);


                line = in.readLine();
            }
        } finally {
            if (in != null) in.close();
        }
        return occRates;
    }

}
