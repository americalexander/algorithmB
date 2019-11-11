package edu.utexas.wrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.utexas.wrap.balancing.Attr2ProdProportionalBalancer;
import edu.utexas.wrap.balancing.TripBalancer;
import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.DemandMap;
import edu.utexas.wrap.demand.ModalPAMatrix;
import edu.utexas.wrap.demand.ODMatrix;
import edu.utexas.wrap.demand.PAMap;
import edu.utexas.wrap.demand.containers.PAPassthroughMap;
import edu.utexas.wrap.distribution.FrictionFactorMap;
import edu.utexas.wrap.distribution.GravityDistributor;
import edu.utexas.wrap.generation.AreaSpecificTripGenerator;
import edu.utexas.wrap.generation.BasicTripGenerator;
import edu.utexas.wrap.generation.RateProportionTripGenerator;
import edu.utexas.wrap.marketsegmentation.MarketSegment;
import edu.utexas.wrap.modechoice.FixedProportionSplitter;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.modechoice.TripInterchangeSplitter;
import edu.utexas.wrap.net.AreaClass;
import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.util.AggregatePAMatrixCollector;
import edu.utexas.wrap.util.DemandMapCollector;
import edu.utexas.wrap.util.DepartureArrivalConverter;

class NHBThread extends Thread{
	private Graph graph;
	private Map<TimePeriod,Map<TripPurpose,Collection<ODMatrix>>> nhbODs;
	private Map<TripPurpose,Map<MarketSegment,PAMap>> hbMaps;
	private ModelInput model;
	
	public NHBThread(Graph graph, ModelInput model, Map<TripPurpose,Map<MarketSegment,PAMap>> hbMaps) {
		this.graph = graph;
		this.hbMaps = hbMaps;
		this.model = model;
	}
	
	public void run() {
		Map<TripPurpose,PAMap> nhbMaps = generate();
		
		balance(nhbMaps);
		
		Map<TripPurpose,AggregatePAMatrix> nhbMatrices = distribute(nhbMaps);
		
		Map<TripPurpose,AggregatePAMatrix> combinedMatrices = combinePurposes(nhbMatrices);
		

		Map<TripPurpose,Collection<ModalPAMatrix>> nhbModalMtxs = modeChoice(combinedMatrices);
		
		paToOD(nhbModalMtxs);
	}
	
	public Map<TimePeriod,Map<TripPurpose,Collection<ODMatrix>>> getODs(){
		return nhbODs;
	}
	
	public Map<TripPurpose,PAMap> generate(){
		
		Map<TripPurpose,TripPurpose> source = new HashMap<TripPurpose,TripPurpose>();
		source.put(TripPurpose.WORK_WORK,	TripPurpose.HOME_WORK);
		source.put(TripPurpose.WORK_ESH,	TripPurpose.HOME_WORK);
		source.put(TripPurpose.WORK_OTH,	TripPurpose.HOME_WORK);
		source.put(TripPurpose.SHOP_SHOP,	TripPurpose.HOME_SHOP);
		source.put(TripPurpose.SHOP_OTH,	TripPurpose.HOME_SHOP);
		source.put(TripPurpose.OTH_OTH,		TripPurpose.HOME_OTH);
		
		Map<TripPurpose,Map<MarketSegment,Double>> secondaryProdRates = source.keySet().parallelStream()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getGeneralProdRates(purpose)));
		
		Map<TripPurpose,Map<MarketSegment,Double>> primaryProdRates = source.values().parallelStream().distinct()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getGeneralProdRates(purpose)));
		
		Map<TripPurpose, DemandMap> secondaryProds = source.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, entry ->{
			RateProportionTripGenerator generator = new RateProportionTripGenerator(graph, primaryProdRates.get(entry.getValue()), secondaryProdRates.get(entry.getKey()),hbMaps.get(entry.getValue()));
			return hbMaps.get(entry.getValue()).entrySet().parallelStream()
			.map(segEntry -> generator.generate(segEntry.getValue().getProductionMap(), segEntry.getKey()))
			.collect(new DemandMapCollector());
		}));

		
		Map<TripPurpose,Map<MarketSegment,Map<AreaClass,Double>>> secondaryAttrRates = source.keySet().parallelStream()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getAreaClassAttrRates(purpose)));
		
		Map<TripPurpose, DemandMap> secondaryAttrs = source.keySet().parallelStream().collect(Collectors.toMap(Function.identity(), purpose ->{
			Map<MarketSegment,Map<AreaClass,Double>> rates =  secondaryAttrRates.get(purpose);
			BasicTripGenerator generator = new AreaSpecificTripGenerator(graph, rates);
			return rates.keySet().parallelStream().map(seg -> generator.generate(seg)).collect(new DemandMapCollector());
		}));
		
		
		return source.keySet().parallelStream().collect(Collectors.toMap(Function.identity(), 
				purpose -> new PAPassthroughMap(graph, null, secondaryProds.get(purpose), secondaryAttrs.get(purpose))));
	}
	
	public void balance(Map<TripPurpose,PAMap> nhbMaps) {
		TripBalancer balancer = new Attr2ProdProportionalBalancer();
		nhbMaps.values().parallelStream().forEach( map -> balancer.balance(map));
	}
	
	public Map<TripPurpose,AggregatePAMatrix> distribute(Map<TripPurpose,PAMap> paMaps) {
		Map<TripPurpose,FrictionFactorMap> ffs = Stream.of(
				TripPurpose.NONHOME_EDU,
				TripPurpose.OTH_OTH,
				TripPurpose.SHOP_OTH,
				TripPurpose.SHOP_SHOP,
				TripPurpose.WORK_ESH,
				TripPurpose.WORK_OTH,
				TripPurpose.WORK_WORK).parallel()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getFrictionFactors(purpose).get(null)));
		
		return paMaps.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, entry->
			 new GravityDistributor(graph, ffs.get(entry.getKey())).distribute(entry.getValue())
		));
	}
	
	public Map<TripPurpose,AggregatePAMatrix> combinePurposes(Map<TripPurpose,AggregatePAMatrix> oldMatrices){
		Map<TripPurpose,AggregatePAMatrix> ret = new HashMap<TripPurpose,AggregatePAMatrix>();
		
		ret.put(TripPurpose.NONHOME_WORK, 
				oldMatrices.entrySet().parallelStream()
				.filter(entry -> 
					entry.getKey() == TripPurpose.WORK_WORK ||
					entry.getKey() == TripPurpose.WORK_ESH ||
					entry.getKey() == TripPurpose.WORK_OTH
				)
				.map(entry -> entry.getValue())
				.collect(new AggregatePAMatrixCollector())
				);
		
		ret.put(TripPurpose.NONHOME_NONWORK, 
				oldMatrices.entrySet().parallelStream()
				.filter(entry -> 
					entry.getKey() == TripPurpose.SHOP_SHOP ||
					entry.getKey() == TripPurpose.SHOP_OTH ||
					entry.getKey() == TripPurpose.OTH_OTH ||
					entry.getKey() == TripPurpose.NONHOME_EDU ||
					entry.getKey() == TripPurpose.NONHOME_OTH
				)
				.map(entry -> entry.getValue())
				.collect(new AggregatePAMatrixCollector())
				);

		return ret;
	}
	
	public Map<TripPurpose,Collection<ModalPAMatrix>> modeChoice(
			Map<TripPurpose,AggregatePAMatrix> combinedMatrices){
		Map<TripPurpose,Map<Mode,Double>> modalRates = Stream.of(
				TripPurpose.NONHOME_EDU,
				TripPurpose.OTH_OTH,
				TripPurpose.SHOP_OTH,
				TripPurpose.SHOP_SHOP,
				TripPurpose.WORK_ESH,
				TripPurpose.WORK_OTH,
				TripPurpose.WORK_WORK).parallel()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getModeShares(purpose).get(null)));;
		
		return combinedMatrices.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, entry -> {
			TripInterchangeSplitter mc = new FixedProportionSplitter(modalRates.get(entry.getKey()));
			return mc.split(entry.getValue()).collect(Collectors.toSet());
		}));
	}
	
	public void paToOD(
			Map<TripPurpose,Collection<ModalPAMatrix>> map) {
		Map<Mode,Double> occupancyRates = model.getOccupancyRates();
		Map<TripPurpose,Map<TimePeriod,Double>> depRates = Stream.of(
					TripPurpose.NONHOME_EDU,
					TripPurpose.OTH_OTH,
					TripPurpose.SHOP_OTH,
					TripPurpose.SHOP_SHOP,
					TripPurpose.WORK_ESH,
					TripPurpose.WORK_OTH,
					TripPurpose.WORK_WORK).parallel()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getDepartureRates(purpose, null))), 
				arrRates = Stream.of(
					TripPurpose.NONHOME_EDU,
					TripPurpose.OTH_OTH,
					TripPurpose.SHOP_OTH,
					TripPurpose.SHOP_SHOP,
					TripPurpose.WORK_ESH,
					TripPurpose.WORK_OTH,
					TripPurpose.WORK_WORK).parallel()
				.collect(Collectors.toMap(Function.identity(), purpose -> model.getArrivalRates(purpose,null)));
		
		nhbODs = Stream.of(TimePeriod.values()).parallel().collect(Collectors.toMap(Function.identity(), time ->
			map.entrySet().parallelStream().collect(Collectors.toMap(Entry::getKey, purposeEntry ->{
				DepartureArrivalConverter converter = new DepartureArrivalConverter(
						depRates.get(purposeEntry.getKey()).get(time),
						arrRates.get(purposeEntry.getKey()).get(time));
				return purposeEntry.getValue().parallelStream()
				.map(modalMtx -> converter.convert(modalMtx, occupancyRates.get(modalMtx.getMode())))
				.collect(Collectors.toSet());
			}))
		));
	}
}