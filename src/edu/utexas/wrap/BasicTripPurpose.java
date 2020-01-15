package edu.utexas.wrap;

import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.demand.ModalPAMatrix;
import edu.utexas.wrap.demand.ODMatrix;
import edu.utexas.wrap.demand.PAMap;
import edu.utexas.wrap.demand.containers.FixedSizePAMap;
import edu.utexas.wrap.generation.TripGenerator;
import edu.utexas.wrap.marketsegmentation.IncomeGroupSegmenter;
import edu.utexas.wrap.marketsegmentation.MarketSegment;
import edu.utexas.wrap.modechoice.FixedProportionSplitter;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.util.AggregatePAMatrixCollector;
import edu.utexas.wrap.util.DepartureArrivalConverter;

public class BasicTripPurpose extends Thread implements TripPurpose {
	
	private ModelInput model;
	private String name;
	private Map<MarketSegment,Map<TimePeriod,Collection<ODMatrix>>> odMtxs;
	
	public BasicTripPurpose(String name, ModelInput model) {
		this.name = name;
		this.model = model;
	}
	
	public void run() {

		
		Stream<Entry<MarketSegment,DemandMap>> 
			prods = getProductions(),
			attrs = getAttractions();
		
		//TODO implement secondary trip purposes here
		
		Stream<Entry<MarketSegment,PAMap>> maps = buildProductionAttractionMaps(prods,attrs);
		
		maps = balance(maps);
		
		Stream<Entry<MarketSegment,AggregatePAMatrix>> aggMtxs = distribute(maps);
				
		Stream<Entry<MarketSegment,Collection<ModalPAMatrix>>> modalMtxs = modeChoice(aggMtxs);
		
		odMtxs = convertToOD(modalMtxs)
				//FIXME the collector below breaks when passed a null MarketSegment
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public Stream<Entry<MarketSegment, DemandMap>> getProductions() {
		TripGenerator productionGenerator = model.getProductionGenerator(this);
		
		return model.getProductionSegments(this)
				.map(entry -> new SimpleEntry<MarketSegment, DemandMap>(
						entry,
						productionGenerator.generate(entry)));
	}

	@Override
	public Stream<Entry<MarketSegment, DemandMap>> getAttractions() {
		TripGenerator attractionGenerator = model.getAttractionGenerator(this);
		
		return model.getAttractionSegments(this)
				.map(entry -> new SimpleEntry<MarketSegment,DemandMap>(
						entry,
						attractionGenerator.generate(entry)));
	}

	@Override
	public Stream<Entry<MarketSegment, PAMap>> buildProductionAttractionMaps(
			Stream<Entry<MarketSegment, DemandMap>> prods, Stream<Entry<MarketSegment, DemandMap>> attrs) {
		//FIXME the collectors below break when passed a null MarketSegment
		Map<MarketSegment,DemandMap> prodSegs = prods.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		Map<MarketSegment,DemandMap> attrSegs = attrs.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		
		Collection<MarketSegment> bigSegs = model.getSegments(this);
		
		return bigSegs.parallelStream().map(bigSeg -> {
			Stream<DemandMap> 
				prodMaps = prodSegs.entrySet().parallelStream()
					.filter(entry -> bigSeg.equals(entry.getKey()))
					.map(Entry::getValue),
					
				attrMaps = attrSegs.entrySet().parallelStream()
					.filter(entry -> bigSeg.equals(entry.getKey()))
					.map(Entry::getValue);
			
			return new SimpleEntry<MarketSegment,PAMap>(bigSeg, new FixedSizePAMap(prodMaps,attrMaps));
		});
		
	}

	@Override
	public Stream<Entry<MarketSegment, PAMap>> balance(
			Stream<Entry<MarketSegment, PAMap>> paMaps) {
		return paMaps.map(entry -> new SimpleEntry<MarketSegment,PAMap>(
				entry.getKey(),
				model.getBalancer(this).balance(entry.getValue())
				));
	}

	@Override
	public Stream<Entry<MarketSegment, AggregatePAMatrix>> distribute(
			Stream<Entry<MarketSegment, PAMap>> paMaps) {
		
		return paMaps.map( msEntry -> {
			
			Map<TimePeriod,Double> shares = model.getDistributionShares(this,msEntry.getKey());
			
			Stream<AggregatePAMatrix> mtxs = shares.entrySet().parallelStream()
				.filter(tpEntry -> tpEntry.getValue() > 0)
				.map(tpEntry -> 
					model.getDistributor(
							this,
							tpEntry.getKey(),
							msEntry.getKey())
					.distribute(msEntry.getValue()));
			
			
			return new SimpleEntry<MarketSegment,AggregatePAMatrix>(msEntry.getKey(),
					mtxs.collect(new AggregatePAMatrixCollector()));
		});
	}

	@Override
	public Stream<Entry<MarketSegment, Collection<ModalPAMatrix>>> modeChoice(
			Stream<Entry<MarketSegment, AggregatePAMatrix>> paMatrices) {
		Map<MarketSegment,Map<Mode,Double>> shares = model.getModeShares(this); 
		
		return paMatrices.map(entry ->{
		
			Map<Mode,Double> ms = shares.get(((IncomeGroupSegmenter) entry.getKey()));
			return new SimpleEntry<MarketSegment,Collection<ModalPAMatrix>>(
					entry.getKey(),
					new FixedProportionSplitter(ms).split(entry.getValue()).collect(Collectors.toSet()));
		
		});
	}

	@Override
	public Stream<Entry<MarketSegment, Map<TimePeriod,Collection<ODMatrix>>>> convertToOD(
			Stream<Entry<MarketSegment, Collection<ModalPAMatrix>>> paMatrices) {
		
		return paMatrices.map(entry ->
		new SimpleEntry<MarketSegment,Map<TimePeriod,Collection<ODMatrix>>>(
				
				entry.getKey(),
				
				Stream.of(TimePeriod.values())
				.collect(
						Collectors.toMap(Function.identity(), 
								time -> {
									DepartureArrivalConverter converter = new DepartureArrivalConverter(
											model.getDepartureRates(this, 
													((IncomeGroupSegmenter) entry.getKey())).get(time),
											model.getArrivalRates(this, 
													((IncomeGroupSegmenter) entry.getKey())).get(time)
											);
									return entry.getValue().parallelStream()
											.map(mtx -> converter.convert(mtx, 
													model.getOccupancyRates().get(mtx.getMode()))
													)
													.collect(Collectors.toSet());
								}
							)
						)
				)
				);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Map<MarketSegment,Collection<ODMatrix>> getODMap(TimePeriod tp){
		
		return odMtxs.entrySet().parallelStream()
				//FIXME the collector below breaks in Java 8 when passed a null MarketSegment
			.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().get(tp)));
	}
}
