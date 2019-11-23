package localsearch.domainspecific.vehiclerouting.apps.truckcontainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import com.google.gson.Gson;

import localsearch.domainspecific.vehiclerouting.apps.sharedaride.ShareARide;
import localsearch.domainspecific.vehiclerouting.apps.sharedaride.SolutionShareARide;
import localsearch.domainspecific.vehiclerouting.apps.sharedaride.Search.VariableNeighborhoodSearch;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.constraints.ContainerCapacityConstraint;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.constraints.ContainerCarriedByTrailerConstraint;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.constraints.MoocCapacityConstraint;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.Container;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ContainerTruckMoocInput;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.DepotContainer;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.DepotMooc;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.DepotTruck;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.DistanceElement;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ExportEmptyRequests;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ExportLadenRequests;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ImportContainerRequest;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ImportContainerTruckMoocRequest;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ImportEmptyRequests;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ImportLadenRequests;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.Mooc;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.Port;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.ShipCompany;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.Truck;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.TruckContainerSolution;
import localsearch.domainspecific.vehiclerouting.apps.truckcontainer.model.Warehouse;
import localsearch.domainspecific.vehiclerouting.vrp.Constants;
import localsearch.domainspecific.vehiclerouting.vrp.ConstraintSystemVR;
import localsearch.domainspecific.vehiclerouting.vrp.IFunctionVR;
import localsearch.domainspecific.vehiclerouting.vrp.VRManager;
import localsearch.domainspecific.vehiclerouting.vrp.VarRoutesVR;
import localsearch.domainspecific.vehiclerouting.vrp.constraints.timewindows.CEarliestArrivalTimeVR;
import localsearch.domainspecific.vehiclerouting.vrp.entities.ArcWeightsManager;
import localsearch.domainspecific.vehiclerouting.vrp.entities.LexMultiValues;
import localsearch.domainspecific.vehiclerouting.vrp.entities.NodeWeightsManager;
import localsearch.domainspecific.vehiclerouting.vrp.entities.Point;
import localsearch.domainspecific.vehiclerouting.vrp.functions.AccumulatedEdgeWeightsOnPathVR;
import localsearch.domainspecific.vehiclerouting.vrp.functions.CapacityConstraintViolationsVR;
import localsearch.domainspecific.vehiclerouting.vrp.functions.ConstraintViolationsVR;
import localsearch.domainspecific.vehiclerouting.vrp.functions.LexMultiFunctions;
import localsearch.domainspecific.vehiclerouting.vrp.functions.TotalCostVR;
import localsearch.domainspecific.vehiclerouting.vrp.invariants.AccumulatedWeightEdgesVR;
import localsearch.domainspecific.vehiclerouting.vrp.invariants.AccumulatedWeightNodesVR;
import localsearch.domainspecific.vehiclerouting.vrp.invariants.EarliestArrivalTimeVR;
import localsearch.domainspecific.vehiclerouting.vrp.neighborhoodexploration.INeighborhoodExplorer;
import localsearch.domainspecific.vehiclerouting.vrp.utils.DateTimeUtils;

public class SolverWithLocalSearch {
	public ContainerTruckMoocInput input;
	
	ArrayList<Point> points;
	public ArrayList<Point> pickupPoints;
	public ArrayList<Point> deliveryPoints;
	public ArrayList<Point> rejectPickupPoints;
	public ArrayList<Point> rejectDeliveryPoints;
	ArrayList<Point> startPoints;
	ArrayList<Point> stopPoints;
	ArrayList<Point> startMoocPoints;
	ArrayList<Point> stopMoocPoints;
	HashMap<Point, Integer> point2Type;
	
	public HashMap<Point, Integer> earliestAllowedArrivalTime;
	public HashMap<Point, Integer> serviceDuration;
	public HashMap<Point, Integer> lastestAllowedArrivalTime;
	public HashMap<Point,Point> pickup2DeliveryOfGood;
	public HashMap<Point,Point> pickup2DeliveryOfPeople;
	public HashMap<Point, Point> pickup2Delivery;
	public HashMap<Point,Point> delivery2Pickup;
	
	public HashMap<Point, Point> start2stopMoocPoint;
	public HashMap<Point,Point> stop2startMoocPoint;
	
	public HashMap<Point, Truck> startPoint2Truck;
	public HashMap<Point, Mooc> startPoint2Mooc;
	
	public HashMap<Point, Integer> point2Group;
	public HashMap<Integer, Integer> group2marked;
	
	public HashMap<Point, Integer> point2moocWeight;
	public HashMap<Point, Integer> point2containerWeight;
	
	public HashMap<Integer, Point> route2DeliveryMooc;
	
	public int nVehicle;
	public static int nRequest;
	
	
	public String[] locationCodes;
	public HashMap<String, Integer> mLocationCode2Index;
	public double[][] distance;// distance[i][j] is the distance from location
								// index i to location index j
	public double[][] travelTime;// travelTime[i][j] is the travel time from
									// location index i to location index j
	
	public HashMap<String, Truck> mCode2Truck;
	public HashMap<String, Mooc> mCode2Mooc;
	public HashMap<String, Container> mCode2Container;
	public HashMap<String, DepotContainer> mCode2DepotContainer;
	public HashMap<String, DepotTruck> mCode2DepotTruck;
	public HashMap<String, DepotMooc> mCode2DepotMooc;
	public HashMap<String, Warehouse> mCode2Warehouse;
	public HashMap<String, Port> mCode2Port;
	public ArrayList<Container> additionalContainers;
	
	public ExportEmptyRequests[] exEmptyRequests;
	public ExportLadenRequests[] exLadenRequests;
	public ImportEmptyRequests[] imEmptyRequests;
	public ImportLadenRequests[] imLadenRequests;
	
	ArcWeightsManager awm;
	VRManager mgr;
	VarRoutesVR XR;
	ConstraintSystemVR S;
	IFunctionVR objective;
	CEarliestArrivalTimeVR ceat;
	LexMultiValues valueSolution;
	EarliestArrivalTimeVR eat;
	CEarliestArrivalTimeVR cEarliest;
	ContainerCapacityConstraint capContCtr;
	MoocCapacityConstraint capMoocCtr;
	ContainerCarriedByTrailerConstraint contmoocCtr;
	
	NodeWeightsManager nwMooc;
	NodeWeightsManager nwContainer;
	AccumulatedWeightNodesVR accMoocInvr;
	AccumulatedWeightNodesVR accContainerInvr;
	HashMap<Point, IFunctionVR> accDisF;
	
	HashMap<Point, Integer> nChosed;
	HashMap<Point, Boolean> removeAllowed;
	
	private int nRemovalOperators = 13;
	private int nInsertionOperators = 14;
	
	//parameters
	public int lower_removal;
	public int upper_removal;
	public int sigma1 = 3;
	public int sigma2 = 1;
	public int sigma3 = -5;
	public double rp = 0.1;
	public int nw = 1;
	public double shaw1st = 0.5;
	public double shaw2nd = 0.2;
	public double shaw3rd = 0.1;
	public double temperature = 200;
	public double cooling_rate = 0.9995;
	public int nTabu = 5;
	
	int INF_TIME = Integer.MAX_VALUE;
	public static double MAX_TRAVELTIME;
	public static final int START_TRUCK = 0;
	public static final int END_TRUCK 	= 1;
	public static final int START_MOOC 	= 2;
	public static final int END_MOOC 	= 3;
	public static final int START_CONT 	= 4;
	public static final int END_CONT 	= 5;
	public static final int PORT	 	= 6;
	public static final int WAREHOUSE 	= 7;
	
	public SolverWithLocalSearch(){
		
	}
	
//	public void adaptiveSearchOperators(String outputfile){	
//		try{
//			
//			PrintWriter fo = new PrintWriter(new File(outputfile));
//			int it = 0;
//			int timeLimit = 36000000;
//	    	int nIter = 100000;
//	    	int maxStable = 1000;
//	    	int iS = 0;
//	    	
//	    	initParamsForALNS();
//	    	//insertion operators selection probabilities
//			double[] pti = new double[nInsertionOperators];
//			//removal operators selection probabilities
//			double[] ptd = new double[nRemovalOperators];
//			
//			//wi - number of times used during last iteration
//			int[] wi = new int[nInsertionOperators];
//			int[] wd = new int[nRemovalOperators];
//			
//			//pi_i - score of operator
//			int[] si = new int[nInsertionOperators];
//			int[] sd = new int[nRemovalOperators];
//			
//			
//			//init probabilites
//			for(int i=0; i<nInsertionOperators; i++){
//				pti[i] = 1.0/nInsertionOperators;
//				wi[i] = 1;
//				si[i] = 0;
//			}
//			for(int i=0; i<nRemovalOperators; i++){
//				ptd[i] = 1.0/nRemovalOperators;
//				wd[i] = 1;
//				sd[i] = 0;
//			}
//	    	
//	    	fo.println("time limit = " + timeLimit + ", nbIters = " + nIter + ", maxStable = " + maxStable);
//	    	
//	    	SearchOptimumSolution opt = new SearchOptimumSolution(this);
//	    	
//			double best_cost = objective.getValue();
//
//			TruckContainerSolution best_solution = new TruckContainerSolution(XR, rejectPickupPoints,
//					rejectDeliveryPoints, best_cost, getNbUsedTrucks(), group2marked);
//	
//			double start_search_time = System.currentTimeMillis();
//			
//			fo.println("iter=====insertion=====removal=====time=====cost=====nbReject=====nbTrucks");
//			fo.println("0 -1 -1 " + " " + System.currentTimeMillis()/1000 + " " + best_cost + " " + getNbRejectedRequests() + " " + getNbUsedTrucks());
//	
//			while( (System.currentTimeMillis()-start_search_time) < timeLimit && it++ < nIter){
//				System.out.println("nb of iterator: " + it);
//				double current_cost = objective.getValue();
//				int current_nbTrucks = getNbUsedTrucks();
//				TruckContainerSolution current_solution = new TruckContainerSolution(XR, rejectPickupPoints, 
//					rejectDeliveryPoints, current_cost, current_nbTrucks, group2marked);
//				
//				removeAllMoocFromRoutes();
//				
//				int i_selected_removal = -1;
//				if(iS >= maxStable){
//					opt.allRemoval();
//					iS = 0;
//				}
//				else{
//					i_selected_removal = get_operator(ptd);
//					//int i_selected_removal = 1;
//					wd[i_selected_removal]++;
//					switch(i_selected_removal){
//						case 0: opt.routeRemoval(); break;
//						case 1: opt.randomRequestRemoval(); break;
//						case 2: opt.shaw_removal(); break;
//						case 3: opt.worst_removal(); break;
//						case 4: opt.forbidden_removal(0); break;
//						case 5: opt.forbidden_removal(1); break;
//						case 6: opt.forbidden_removal(2); break;
//						case 7: opt.forbidden_removal(3); break;
//					}
//				}
//				
//				
//				int i_selected_insertion = get_operator(pti);
//				//int i_selected_insertion = 3;
//				wi[i_selected_insertion]++;
//				switch(i_selected_insertion){
//					case 0: opt.greedyInsertion(); break;
//					case 1: opt.greedyInsertionWithNoise(); break;
//					case 2: opt.regret_n_insertion(2); break;
//					case 3: opt.first_possible_insertion(); break;
//					case 4: opt.sort_before_insertion(0); break;
//					case 5: opt.sort_before_insertion(1); break;
//					case 6: opt.sort_before_insertion(2); break;
//					case 7: opt.sort_before_insertion(3); break;
//				}
//				
//				//insertMoocToRoutes();
//				//System.out.println("s.vio = " + S.violations());
//				int new_nb_reject_points = rejectPickupPoints.size();
//				double new_cost = objective.getValue();
//				int new_nbTrucks = getNbUsedTrucks();
//				int current_nb_reject_points = current_solution.get_rejectPickupPoints().size();
//
//				if( new_nb_reject_points < current_nb_reject_points
//						|| (new_nb_reject_points == current_nb_reject_points && new_nbTrucks < current_nbTrucks)
//						|| (new_nb_reject_points == current_nb_reject_points && new_nbTrucks == current_nbTrucks && new_cost < current_cost)){
//					int best_nb_reject_points = best_solution.get_rejectPickupPoints().size();
//					int best_nbTrucks = best_solution.get_nbTrucks();
//					
//					if(new_nb_reject_points < best_nb_reject_points
//							|| (new_nb_reject_points == best_nb_reject_points && new_nbTrucks < best_nbTrucks)
//							|| (new_nb_reject_points == best_nb_reject_points && new_nbTrucks == best_nbTrucks && new_cost < best_cost)){
//						
//						best_cost = new_cost;
//						best_solution = new TruckContainerSolution(XR, rejectPickupPoints, rejectDeliveryPoints,
//								new_cost, new_nbTrucks, group2marked);
//						fo.println(it + " " + i_selected_insertion 
//							+ " " + i_selected_removal + " "
//							+ System.currentTimeMillis()/1000 + " "
//							+ best_cost + " " + getNbRejectedRequests() + " " + getNbUsedTrucks());
//						si[i_selected_insertion] += sigma1;
//						if(i_selected_removal >= 0)
//							sd[i_selected_removal] += sigma1;
//					}
//					else{
//						si[i_selected_insertion] += sigma2;
//						if(i_selected_removal >= 0)
//							sd[i_selected_removal] += sigma2;
//					}
//				}
//				/*
//				 * if new solution has cost worst than current solution
//				 * 		because XR is new solution
//				 * 			copy current current solution to new solution if don't change solution
//				 */
//				else{
//					si[i_selected_insertion] += sigma3;
//					if(i_selected_removal >= 0)
//						sd[i_selected_removal] += sigma3;
//					double v = Math.exp(-(new_cost-current_cost)/temperature);
//					double e = Math.random();
//					if(e >= v){
//						current_solution.copy2XR(XR);
//						group2marked = current_solution.get_group2marked();
//						rejectPickupPoints = current_solution.get_rejectPickupPoints();
//						rejectDeliveryPoints = current_solution.get_rejectDeliveryPoints();
//					}
//					iS++;
//				}
//				
//				temperature = cooling_rate*temperature;
//				
//				//update probabilities
//				if(it % nw == 0){
//					for(int i=0; i<nInsertionOperators; i++){
//						pti[i] = Math.max(0, pti[i]*(1-rp) + rp*si[i]/wi[i]);
//						//wi[i] = 1;
//						//si[i] = 0;
//					}
//					
//					for(int i=0; i<nRemovalOperators; i++){
//						ptd[i] = Math.max(0, ptd[i]*(1-rp) + rp*sd[i]/wd[i]);
//						//wd[i] = 1;
//						//sd[i] = 0;
//					}
//				}
//			}
//			best_solution.copy2XR(XR);
//			group2marked = best_solution.get_group2marked();
//			
//			rejectPickupPoints = best_solution.get_rejectPickupPoints();
//			rejectDeliveryPoints = best_solution.get_rejectDeliveryPoints();
//			
//			fo.println(it + " -1 -1 "
//					+ System.currentTimeMillis()/1000 + " "
//					+ best_cost + " " + getNbRejectedRequests() + " " + getNbUsedTrucks());
//			fo.close();
//		}catch(Exception e){
//			System.out.println(e);
//		}
//	}
	
	public boolean checkCapacityContainerConstraint(Point x, Point y){
		int k = XR.route(y);
		if(accContainerInvr.getSumWeights(y) + accContainerInvr.getWeights(x) > 2)
			return false;
		for(Point p = XR.next(y); p != XR.getTerminatingPointOfRoute(k); p = XR.next(p)){
			if(accContainerInvr.getSumWeights(p) + accContainerInvr.getWeights(x) > 2)
				return false;
		}
		return true;
	}
	
	public void checkRejectedRequestsInfo(){
		ArrayList<Integer> grs = new ArrayList<Integer>();
		for(int i = 0; i < rejectPickupPoints.size(); i++){
			Point pickup = rejectPickupPoints.get(i);
			int groupId = point2Group.get(pickup);
			
			if(group2marked.get(groupId) == 1 && grs.contains(groupId))
				continue;
			grs.add(groupId);
			System.out.println("p = " + pickup.getID());
			Point delivery = pickup2Delivery.get(pickup);
			Truck truck = input.getTrucks()[11];//getNearestTruck(pickup.getLocationCode());
			Mooc mooc = getNearestMooc(truck.getDepotTruckLocationCode());
			long d = DateTimeUtils.dateTime2Int(truck.getStartWorkingTime())
					+ getTravelTime(truck.getDepotTruckLocationCode(), mooc.getDepotMoocLocationCode())
					+ input.getParams().getLinkMoocDuration()
					+ getTravelTime(mooc.getDepotMoocLocationCode(), pickup.getLocationCode());
			if(d >= lastestAllowedArrivalTime.get(pickup))
				System.out.println("pick = " + pickup.getLocationCode()
						+ ", d = " + DateTimeUtils.unixTimeStamp2DateTime(d)
						+ ", late = " + DateTimeUtils.unixTimeStamp2DateTime(lastestAllowedArrivalTime.get(pickup)) );
			d += getTravelTime(pickup.getLocationCode(), delivery.getLocationCode());
			if(d >= lastestAllowedArrivalTime.get(delivery))
				System.out.println("del = " + delivery.getLocationCode()
						+ ", d = " + DateTimeUtils.unixTimeStamp2DateTime(d)
						+ ", late = " + DateTimeUtils.unixTimeStamp2DateTime(lastestAllowedArrivalTime.get(delivery)) );
		}
	}
	
	public void init(){
		this.exEmptyRequests = input.getExEmptyRequests();
		this.exLadenRequests = input.getExLadenRequests();
		this.imEmptyRequests = input.getImEmptyRequests();
		this.imLadenRequests = input.getImLadenRequests();
		this.nRequest = exEmptyRequests.length
			+ exLadenRequests.length
			+ imEmptyRequests.length
			+ imLadenRequests.length;
		this.nVehicle = input.getTrucks().length;
		points = new ArrayList<Point>();
		earliestAllowedArrivalTime = new HashMap<Point, Integer>();
		serviceDuration = new HashMap<Point, Integer>();
		lastestAllowedArrivalTime = new HashMap<Point, Integer>();
		
		pickupPoints = new ArrayList<Point>();
		deliveryPoints = new ArrayList<Point>();
		rejectPickupPoints = new ArrayList<Point>();
		rejectDeliveryPoints = new ArrayList<Point>();
		startPoints = new ArrayList<Point>();
		stopPoints = new ArrayList<Point>();
		startMoocPoints = new ArrayList<Point>();
		stopMoocPoints = new ArrayList<Point>();
		point2Type = new HashMap<Point, Integer>();
		
		pickup2Delivery = new HashMap<Point, Point>();
		delivery2Pickup = new HashMap<Point, Point>();
		
		start2stopMoocPoint = new HashMap<Point, Point>();
		stop2startMoocPoint = new HashMap<Point, Point>();
		
		startPoint2Truck = new HashMap<Point, Truck>();
		startPoint2Mooc = new HashMap<Point, Mooc>();
		
		point2Group = new HashMap<Point, Integer>();
		group2marked = new HashMap<Integer, Integer>();
		
		point2moocWeight = new HashMap<Point, Integer>();
		point2containerWeight = new HashMap<Point, Integer>();
		
		route2DeliveryMooc = new HashMap<Integer, Point>();
		
		int id = 0;
		int groupId = 0;
		for(int i = 0; i < nVehicle; i++){
			Truck truck = input.getTrucks()[i];
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < truck.getReturnDepotCodes().length; j++){
				id++;
				Point sp = new Point(id, truck.
						getDepotTruckLocationCode());
				
				points.add(sp);
				startPoints.add(sp);
				point2Type.put(sp, START_TRUCK);
				startPoint2Truck.put(sp, truck);
				
				point2Group.put(sp, groupId);
				
				earliestAllowedArrivalTime.put(sp,(int)(DateTimeUtils.dateTime2Int(
						truck.getStartWorkingTime())));
				serviceDuration.put(sp, 0);
				lastestAllowedArrivalTime.put(sp,INF_TIME);
				System.out.println("truck " + i + ", return depot " + j + ", sp =" + id + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(sp) + ", late = " + lastestAllowedArrivalTime.get(sp));
				id++;
				DepotTruck depotTruck = mCode2DepotTruck.get(truck.getReturnDepotCodes()[j]);
				Point tp = new Point(id, depotTruck.getLocationCode());
				points.add(tp);
				stopPoints.add(tp);
				point2Type.put(tp, END_TRUCK);
				
				point2Group.put(tp, groupId);
				
				earliestAllowedArrivalTime.put(tp,(int)(DateTimeUtils.dateTime2Int(
						input.getTrucks()[i].getStartWorkingTime())));
				serviceDuration.put(tp, 0);
				lastestAllowedArrivalTime.put(tp, INF_TIME);
				System.out.println("truck " + i + ", return depot " + j + ", tp =" + id + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(tp) + ", late = " + lastestAllowedArrivalTime.get(tp));
				//pickup2Delivery.put(sp, tp);
				//delivery2Pickup.put(tp, sp);
				
				point2moocWeight.put(sp, 0);
				point2moocWeight.put(tp, 0);
				
				point2containerWeight.put(sp, 0);
				point2containerWeight.put(tp, 0);
			}
		}		
	
		for(int i = 0; i < input.getMoocs().length; i++){
			Mooc mooc = input.getMoocs()[i];
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < mooc.getReturnDepotCodes().length; j++){
				id++;
				Point sp = new Point(id, mooc
						.getDepotMoocLocationCode());
				points.add(sp);
				startMoocPoints.add(sp);
				point2Type.put(sp, START_MOOC);
				startPoint2Mooc.put(sp, mooc);
				
				point2Group.put(sp, groupId);
				
				earliestAllowedArrivalTime.put(sp, 0);
				serviceDuration.put(sp, input.getParams().getLinkMoocDuration());
				lastestAllowedArrivalTime.put(sp,INF_TIME);
				System.out.println("mooc " + i + ", return depot " + j + ", sp =" + id + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(sp) + ", late = " + lastestAllowedArrivalTime.get(sp));
				id++;
				String moocCode = mooc.getReturnDepotCodes()[j];
				DepotMooc depotMooc = mCode2DepotMooc.get(moocCode);
				Point tp = new Point(id, depotMooc.getLocationCode());
				points.add(tp);
				stopMoocPoints.add(tp);
				point2Type.put(tp, END_MOOC);
				point2Group.put(tp, groupId);
				
				earliestAllowedArrivalTime.put(tp, 0);
				serviceDuration.put(tp, 0);
				lastestAllowedArrivalTime.put(tp, INF_TIME);
				System.out.println("mooc " + i + ", return depot " + j + ", tp =" + id + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(tp) + ", late = " + lastestAllowedArrivalTime.get(tp));
				start2stopMoocPoint.put(sp, tp);
				stop2startMoocPoint.put(tp, sp);
				
				point2moocWeight.put(sp, 2);
				point2moocWeight.put(tp, -2);
				
				point2containerWeight.put(sp, 0);
				point2containerWeight.put(tp, 0);
			}
		}
		
		for(int i = 0; i < exEmptyRequests.length; i++){
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < input.getDepotContainers().length; j++){
				DepotContainer depotCont = input.getDepotContainers()[j];
				id++;
				Point pickup = new Point(id, depotCont.getLocationCode());
				id++;
				Warehouse wh = mCode2Warehouse.get(
						exEmptyRequests[i].getWareHouseCode());
				Point delivery = new Point(id, wh.getLocationCode());
	
				points.add(pickup);
				points.add(delivery);
				
				pickupPoints.add(pickup);
				deliveryPoints.add(delivery);
	
				pickup2Delivery.put(pickup, delivery);
				delivery2Pickup.put(delivery, pickup);
				
				point2moocWeight.put(pickup, 0);
				if(exEmptyRequests[i].getIsBreakRomooc())
					point2moocWeight.put(delivery, -2);
				else
					point2moocWeight.put(delivery, 0);
				
				point2containerWeight.put(pickup, 1);
				point2containerWeight.put(delivery, -1);
				
				point2Type.put(pickup, START_CONT);
				point2Type.put(delivery, WAREHOUSE);
				
				point2Group.put(pickup, groupId);
				point2Group.put(delivery, groupId);
				
				int early = 0;
				int latest = INF_TIME;
				if(exEmptyRequests[i].getEarlyDateTimePickupAtDepot() != null)
					early = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getEarlyDateTimePickupAtDepot()));
				if(exEmptyRequests[i].getLateDateTimePickupAtDepot() != null)
					latest = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getLateDateTimePickupAtDepot()));
				earliestAllowedArrivalTime.put(pickup, early);
				serviceDuration.put(pickup, input.getParams().getLinkEmptyContainerDuration());
				lastestAllowedArrivalTime.put(pickup, latest);
				
				early = 0;
				latest = INF_TIME;
				if(exEmptyRequests[i].getEarlyDateTimeLoadAtWarehouse() != null)
					early = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getEarlyDateTimeLoadAtWarehouse()));
				if(exEmptyRequests[i].getLateDateTimeLoadAtWarehouse() != null)
					latest = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getLateDateTimeLoadAtWarehouse()));
				earliestAllowedArrivalTime.put(delivery, early);
				serviceDuration.put(delivery, (int)(input.getParams().getUnlinkEmptyContainerDuration()));
				lastestAllowedArrivalTime.put(delivery, latest);
				
				System.out.println("EE " + i + ", container depot " + j + ", pick =" + pickup.getID() + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(pickup) + ", late = " + lastestAllowedArrivalTime.get(pickup));
				System.out.println("EE " + i + ", container depot " + j + ", del =" + delivery.getID() + ", group = " + groupId 
						+ ", ear = " + earliestAllowedArrivalTime.get(delivery) + ", late = " + lastestAllowedArrivalTime.get(delivery));
			}
		}
		
		for(int i = 0; i < exLadenRequests.length; i++)
		{
			groupId++;
			group2marked.put(groupId, 0);
			id++;
			Warehouse wh = mCode2Warehouse.get(
					exLadenRequests[i].getWareHouseCode());
			Point pickup = new Point(id, wh.getLocationCode());
			id++;
			Port port = mCode2Port.get(
					exLadenRequests[i].getPortCode());
			Point delivery = new Point(id, port.getLocationCode());

			points.add(pickup);
			points.add(delivery);
			
			pickupPoints.add(pickup);
			deliveryPoints.add(delivery);
			
			pickup2Delivery.put(pickup, delivery);
			delivery2Pickup.put(delivery, pickup);
			
			point2Type.put(pickup, WAREHOUSE);
			point2Type.put(delivery, PORT);
			
			point2Group.put(pickup, groupId);
			point2Group.put(delivery, groupId);
			
			point2moocWeight.put(pickup, 0);
			if(exLadenRequests[i].getIsBreakRomooc())
				point2moocWeight.put(delivery, -2);
			else
				point2moocWeight.put(delivery, 0);
			
			point2containerWeight.put(pickup, 1);
			point2containerWeight.put(delivery, -1);
			
			int early = 0;
			int latest = INF_TIME;
			if(exLadenRequests[i].getEarlyDateTimeAttachAtWarehouse() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						exLadenRequests[i].getEarlyDateTimeAttachAtWarehouse()));
			
			earliestAllowedArrivalTime.put(pickup,  early);
			serviceDuration.put(pickup, input.getParams().getLinkLoadedContainerDuration());
			lastestAllowedArrivalTime.put(pickup, latest);
			
			early = 0;
			latest = INF_TIME;
			if(exLadenRequests[i].getLateDateTimeUnloadAtPort() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						exLadenRequests[i].getLateDateTimeUnloadAtPort()));
			earliestAllowedArrivalTime.put(delivery, early);
			serviceDuration.put(delivery, (int)(input.getParams().getUnlinkLoadedContainerDuration()));
			lastestAllowedArrivalTime.put(delivery, latest);
			
			System.out.println("EL " + i + ", pick =" + pickup.getID() + ", group = " + groupId 
					+ ", ear = " + earliestAllowedArrivalTime.get(pickup) + ", late = " + lastestAllowedArrivalTime.get(pickup));
			System.out.println("EL " + i + ", del =" + delivery.getID() + ", group = " + groupId 
					+ ", ear = " + earliestAllowedArrivalTime.get(delivery) + ", late = " + lastestAllowedArrivalTime.get(delivery));
			
		}

		for(int i = 0; i < imEmptyRequests.length; i++){
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < input.getDepotContainers().length; j++){
				DepotContainer depotCont = input.getDepotContainers()[j];
				if(depotCont.getReturnedContainer()){
					id++;
					Warehouse wh = mCode2Warehouse.get(
							imEmptyRequests[i].getWareHouseCode());
					Point pickup = new Point(id, wh.getLocationCode());
					id++;
					
					Point delivery = new Point(id, depotCont.getLocationCode());
		
					points.add(pickup);
					points.add(delivery);
					
					pickupPoints.add(pickup);
					deliveryPoints.add(delivery);
					
					pickup2Delivery.put(pickup, delivery);
					delivery2Pickup.put(delivery, pickup);
					
					point2moocWeight.put(pickup, 0);
					point2moocWeight.put(delivery, 0);
					
					point2containerWeight.put(pickup, 1);
					point2containerWeight.put(delivery, -1);
					
					point2Type.put(pickup, WAREHOUSE);
					point2Type.put(delivery, END_CONT);
					
					point2Group.put(pickup, groupId);
					point2Group.put(delivery, groupId);
					
					int early = 0;
					int latest = INF_TIME;
					if(imEmptyRequests[i].getEarlyDateTimeAttachAtWarehouse() != null)
						early = (int)(DateTimeUtils.dateTime2Int(
								imEmptyRequests[i].getEarlyDateTimeAttachAtWarehouse()));
					earliestAllowedArrivalTime.put(pickup, early);
					serviceDuration.put(pickup, input.getParams().getLinkEmptyContainerDuration());
					lastestAllowedArrivalTime.put(pickup, latest);
					
					early = 0;
					latest = INF_TIME;
		
					if(imEmptyRequests[i].getLateDateTimeReturnEmptyAtDepot() != null)
						latest = (int)(DateTimeUtils.dateTime2Int(
								imEmptyRequests[i].getLateDateTimeReturnEmptyAtDepot()));
					earliestAllowedArrivalTime.put(delivery, early);
					serviceDuration.put(delivery, (int)(input.getParams().getUnlinkEmptyContainerDuration()));
					lastestAllowedArrivalTime.put(delivery, latest);
					System.out.println("IE " + i + ", return container depot " + j + ", pick =" + pickup.getID() + ", group = " + groupId 
							+ ", ear = " + earliestAllowedArrivalTime.get(pickup) + ", late = " + lastestAllowedArrivalTime.get(pickup));
					System.out.println("IE " + i + ", return container depot " + j + ", del =" + delivery.getID() + ", group = " + groupId 
							+ ", ear = " + earliestAllowedArrivalTime.get(delivery) + ", late = " + lastestAllowedArrivalTime.get(delivery));
				}
			}
		}
		
		for(int i = 0; i < imLadenRequests.length; i++)
		{
			groupId++;
			group2marked.put(groupId, 0);
			id++;
			Port port = mCode2Port.get(
					imLadenRequests[i].getPortCode());
			Point pickup = new Point(id, port.getLocationCode());
			
			id++;
			Warehouse wh = mCode2Warehouse.get(
					imLadenRequests[i].getWareHouseCode());
			Point delivery = new Point(id, wh.getLocationCode());

			points.add(pickup);
			points.add(delivery);
			
			pickupPoints.add(pickup);
			deliveryPoints.add(delivery);
			
			pickup2Delivery.put(pickup, delivery);
			delivery2Pickup.put(delivery, pickup);
			
			point2moocWeight.put(pickup, 0);
			if(imLadenRequests[i].getIsBreakRomooc())
				point2moocWeight.put(delivery, -2);
			else
				point2moocWeight.put(delivery, 0);
			
			point2containerWeight.put(pickup, 1);
			point2containerWeight.put(delivery, -1);
			
			point2Type.put(pickup, PORT);
			point2Type.put(delivery, WAREHOUSE);
			
			point2Group.put(pickup, groupId);
			point2Group.put(delivery, groupId);
			
			int early = 0;
			int latest = INF_TIME;
			if(imLadenRequests[i].getEarlyDateTimePickupAtPort() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getEarlyDateTimePickupAtPort()));
			if(imLadenRequests[i].getLateDateTimePickupAtPort() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getLateDateTimePickupAtPort()));
			earliestAllowedArrivalTime.put(pickup, early);
			serviceDuration.put(pickup, input.getParams().getLinkLoadedContainerDuration());
			lastestAllowedArrivalTime.put(pickup, latest);
			
			early = 0;
			latest = INF_TIME;
			if(imLadenRequests[i].getEarlyDateTimeUnloadAtWarehouse() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getEarlyDateTimeUnloadAtWarehouse()));
			if(imLadenRequests[i].getLateDateTimeUnloadAtWarehouse() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getLateDateTimeUnloadAtWarehouse()));

			earliestAllowedArrivalTime.put(delivery, early);
			serviceDuration.put(delivery, (int)(input.getParams().getUnlinkLoadedContainerDuration()));
			lastestAllowedArrivalTime.put(delivery, latest);
			
			System.out.println("IL " + i + ", pick =" + pickup.getID() + ", group = " + groupId 
					+ ", ear = " + earliestAllowedArrivalTime.get(pickup) + ", late = " + lastestAllowedArrivalTime.get(pickup));
			System.out.println("IL " + i + ", del =" + delivery.getID() + ", group = " + groupId 
					+ ", ear = " + earliestAllowedArrivalTime.get(delivery) + ", late = " + lastestAllowedArrivalTime.get(delivery));
		}
		
		nwMooc = new NodeWeightsManager(points);
		nwContainer = new NodeWeightsManager(points);
		awm = new ArcWeightsManager(points);
		double max_time = Double.MIN_VALUE;
		for (int i = 0; i < points.size(); i++) {
			for (int j = 0; j < points.size(); j++) {
				double tmp_cost = getTravelTime(points.get(i).getLocationCode(),
						points.get(j).getLocationCode());
				awm.setWeight(points.get(i), points.get(j), tmp_cost);
				max_time = tmp_cost > max_time ? tmp_cost : max_time;
			}
			nwMooc.setWeight(points.get(i), point2moocWeight.get(points.get(i)));
			nwContainer.setWeight(points.get(i), point2containerWeight.get(points.get(i)));
		}
		MAX_TRAVELTIME = max_time;
	}
	
	public void initParamsForALNS(){
		nRemovalOperators = 8;
		nInsertionOperators = 8;
		lower_removal = (int) 1*(nRequest)/100;
		upper_removal = (int) 15*(nRequest)/100;
		
		nChosed = new HashMap<Point, Integer>();
		removeAllowed = new HashMap<Point, Boolean>();
		for(int i=0; i<pickupPoints.size(); i++){
			Point pi = pickupPoints.get(i);
			nChosed.put(pi, 0);
			removeAllowed.put(pi, true);
			
			Point pj = pickup2Delivery.get(pi);
			nChosed.put(pj, 0);
			removeAllowed.put(pj, true);
		}
	}
	
	public void insertMoocForAllRoutes(){
		for(int r = 1; r <= XR.getNbRoutes(); r++){
			Point st = XR.getStartingPointOfRoute(r);
			Point stMooc = null;
			Point enMooc = null;
			for(Point p = XR.next(st); p != XR.getTerminatingPointOfRoute(r); p = XR.next(p)){
				if(accMoocInvr.getSumWeights(XR.prev(p)) <= 0){
					stMooc = getBestMoocForRequest(r, XR.prev(p), p);
					if(stMooc == null)
						continue;
					mgr.performAddOnePoint(stMooc, XR.prev(p));
					int groupMooc= point2Group.get(stMooc);
					group2marked.put(groupMooc, 1);
					enMooc = start2stopMoocPoint.get(stMooc);
				}
			}
			if(accMoocInvr.getSumWeights(XR.getTerminatingPointOfRoute(r)) > 0 && enMooc != null){
				mgr.performAddOnePoint(enMooc, XR.prev(XR.getTerminatingPointOfRoute(r)));
			}
		}
	}
	
	public void insertMoocToRoutes(int r){
		Point st = XR.getStartingPointOfRoute(r);
		Point stMooc = null;
		Point enMooc = null;
		for(Point p = XR.next(st); p != XR.getTerminatingPointOfRoute(r); p = XR.next(p)){
			if(accMoocInvr.getSumWeights(XR.prev(p)) <= 0){
				stMooc = getBestMoocForRequest(r, XR.prev(p), p);
				if(stMooc == null)
					continue;
				mgr.performAddOnePoint(stMooc, XR.prev(p));
				int groupMooc= point2Group.get(stMooc);
				group2marked.put(groupMooc, 1);
				enMooc = start2stopMoocPoint.get(stMooc);
			}
		}
		if(accMoocInvr.getSumWeights(XR.getTerminatingPointOfRoute(r)) > 0 && enMooc != null){
			mgr.performAddOnePoint(enMooc, XR.prev(XR.getTerminatingPointOfRoute(r)));
		}
	}
	
	public void init_compare(){
		this.exEmptyRequests = input.getExEmptyRequests();
		this.exLadenRequests = input.getExLadenRequests();
		this.imEmptyRequests = input.getImEmptyRequests();
		this.imLadenRequests = input.getImLadenRequests();
		this.nRequest = exEmptyRequests.length
			+ exLadenRequests.length
			+ imEmptyRequests.length
			+ imLadenRequests.length;
		this.nVehicle = input.getTrucks().length;
		points = new ArrayList<Point>();
		earliestAllowedArrivalTime = new HashMap<Point, Integer>();
		serviceDuration = new HashMap<Point, Integer>();
		lastestAllowedArrivalTime = new HashMap<Point, Integer>();
		
		pickupPoints = new ArrayList<Point>();
		deliveryPoints = new ArrayList<Point>();
		rejectPickupPoints = new ArrayList<Point>();
		rejectDeliveryPoints = new ArrayList<Point>();
		startPoints = new ArrayList<Point>();
		stopPoints = new ArrayList<Point>();
		startMoocPoints = new ArrayList<Point>();
		stopMoocPoints = new ArrayList<Point>();
		point2Type = new HashMap<Point, Integer>();
		
		pickup2Delivery = new HashMap<Point, Point>();
		delivery2Pickup = new HashMap<Point, Point>();
		
		startPoint2Truck = new HashMap<Point, Truck>();
		startPoint2Mooc = new HashMap<Point, Mooc>();
		
		point2Group = new HashMap<Point, Integer>();
		group2marked = new HashMap<Integer, Integer>();
		
		point2moocWeight = new HashMap<Point, Integer>();
		point2containerWeight = new HashMap<Point, Integer>();
		
		int id = 0;
		int groupId = 0;
		for(int i = 0; i < nVehicle; i++){
			Truck truck = input.getTrucks()[i];
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < truck.getReturnDepotCodes().length; j++){
				id++;
				Point sp = new Point(Integer.parseInt(truck.getDepotTruckCode()), truck.
						getDepotTruckLocationCode());
				
				points.add(sp);
				startPoints.add(sp);
				point2Type.put(sp, START_TRUCK);
				startPoint2Truck.put(sp, truck);
				
				point2Group.put(sp, groupId);
				
				earliestAllowedArrivalTime.put(sp,(int)(DateTimeUtils.dateTime2Int(
						truck.getStartWorkingTime())));
				serviceDuration.put(sp, 0);
				lastestAllowedArrivalTime.put(sp,INF_TIME);
				
				id++;
				DepotTruck depotTruck = mCode2DepotTruck.get(truck.getReturnDepotCodes()[j]);
				Point tp = new Point(Integer.parseInt(depotTruck.getCode()), depotTruck.getLocationCode());
				points.add(tp);
				stopPoints.add(tp);
				point2Type.put(tp, END_TRUCK);
				
				point2Group.put(tp, groupId);
				
				earliestAllowedArrivalTime.put(tp,(int)(DateTimeUtils.dateTime2Int(
						input.getTrucks()[i].getStartWorkingTime())));
				serviceDuration.put(tp, 0);
				lastestAllowedArrivalTime.put(tp, INF_TIME);
				
				//pickup2Delivery.put(sp, tp);
				//delivery2Pickup.put(tp, sp);
				
				point2moocWeight.put(sp, 0);
				point2moocWeight.put(tp, 0);
				
				point2containerWeight.put(sp, 0);
				point2containerWeight.put(tp, 0);
			}
		}		
	
		for(int i = 0; i < input.getMoocs().length; i++){
			Mooc mooc = input.getMoocs()[i];
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < mooc.getReturnDepotCodes().length; j++){
				id++;
				Point sp = new Point(Integer.parseInt(mooc.getDepotMoocCode()), mooc
						.getDepotMoocLocationCode());
				points.add(sp);
				startMoocPoints.add(sp);
				point2Type.put(sp, START_MOOC);
				startPoint2Mooc.put(sp, mooc);
				
				point2Group.put(sp, groupId);
				
				earliestAllowedArrivalTime.put(sp, 0);
				serviceDuration.put(sp, input.getParams().getLinkMoocDuration());
				lastestAllowedArrivalTime.put(sp,INF_TIME);
				
				id++;
				String moocCode = mooc.getReturnDepotCodes()[j];
				DepotMooc depotMooc = mCode2DepotMooc.get(moocCode);
				Point tp = new Point(Integer.parseInt(depotMooc.getCode()), depotMooc.getLocationCode());
				points.add(tp);
				stopMoocPoints.add(tp);
				point2Type.put(tp, END_MOOC);
				point2Group.put(tp, groupId);
				
				earliestAllowedArrivalTime.put(tp, 0);
				serviceDuration.put(tp, 0);
				lastestAllowedArrivalTime.put(tp, INF_TIME);
				
				//pickup2Delivery.put(sp, tp);
				//delivery2Pickup.put(tp, sp);
				
				point2moocWeight.put(sp, 1);
				point2moocWeight.put(tp, -1);
				
				point2containerWeight.put(sp, 0);
				point2containerWeight.put(tp, 0);
			}
		}
		
		for(int i = 0; i < exEmptyRequests.length; i++){
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < input.getDepotContainers().length; j++){
				id++;
				DepotContainer depotCont = input.getDepotContainers()[j];
				Point pickup = new Point(Integer.parseInt(depotCont.getCode()), depotCont.getLocationCode());
				id++;
				Warehouse wh = mCode2Warehouse.get(
						exEmptyRequests[i].getWareHouseCode());
				Point delivery = new Point(Integer.parseInt(wh.getCode()), wh.getLocationCode());
	
				points.add(pickup);
				points.add(delivery);
				
				pickupPoints.add(pickup);
				deliveryPoints.add(delivery);
	
				pickup2Delivery.put(pickup, delivery);
				delivery2Pickup.put(delivery, pickup);
				
				point2moocWeight.put(pickup, 0);
				point2moocWeight.put(delivery, 0);
				
				point2containerWeight.put(pickup, 1);
				point2containerWeight.put(delivery, -1);
				
				point2Type.put(pickup, START_CONT);
				point2Type.put(delivery, WAREHOUSE);
				
				point2Group.put(pickup, groupId);
				point2Group.put(delivery, groupId);
				
				int early = 0;
				int latest = INF_TIME;
				if(exEmptyRequests[i].getEarlyDateTimePickupAtDepot() != null)
					early = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getEarlyDateTimePickupAtDepot()));
				if(exEmptyRequests[i].getLateDateTimePickupAtDepot() != null)
					latest = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getLateDateTimePickupAtDepot()));
				earliestAllowedArrivalTime.put(pickup, early);
				serviceDuration.put(pickup, input.getParams().getLinkEmptyContainerDuration());
				lastestAllowedArrivalTime.put(pickup, latest);
				
				early = 0;
				latest = INF_TIME;
				if(exEmptyRequests[i].getEarlyDateTimeLoadAtWarehouse() != null)
					early = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getEarlyDateTimeLoadAtWarehouse()));
				if(exEmptyRequests[i].getLateDateTimeLoadAtWarehouse() != null)
					latest = (int)(DateTimeUtils.dateTime2Int(
							exEmptyRequests[i].getLateDateTimeLoadAtWarehouse()));
				earliestAllowedArrivalTime.put(delivery, early);
				serviceDuration.put(delivery, (int)(input.getParams().getUnlinkEmptyContainerDuration()));
				lastestAllowedArrivalTime.put(delivery, latest);
			}
		}
		
		for(int i = 0; i < exLadenRequests.length; i++)
		{
			groupId++;
			group2marked.put(groupId, 0);
			id++;
			Warehouse wh = mCode2Warehouse.get(
					exLadenRequests[i].getWareHouseCode());
			Point pickup = new Point(Integer.parseInt(wh.getCode()), wh.getLocationCode());
			id++;
			Port port = mCode2Port.get(
					exLadenRequests[i].getPortCode());
			Point delivery = new Point(Integer.parseInt(port.getCode()), port.getLocationCode());

			points.add(pickup);
			points.add(delivery);
			
			pickupPoints.add(pickup);
			deliveryPoints.add(delivery);
			
			pickup2Delivery.put(pickup, delivery);
			delivery2Pickup.put(delivery, pickup);
			
			point2Type.put(pickup, WAREHOUSE);
			point2Type.put(delivery, PORT);
			
			point2Group.put(pickup, groupId);
			point2Group.put(delivery, groupId);
			
			point2moocWeight.put(pickup, 0);
			point2moocWeight.put(delivery, 0);
			
			point2containerWeight.put(pickup, 1);
			point2containerWeight.put(delivery, -1);
			
			int early = 0;
			int latest = INF_TIME;
			if(exLadenRequests[i].getEarlyDateTimeAttachAtWarehouse() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						exLadenRequests[i].getEarlyDateTimeAttachAtWarehouse()));
			
			earliestAllowedArrivalTime.put(pickup,  early);
			serviceDuration.put(pickup, input.getParams().getLinkLoadedContainerDuration());
			lastestAllowedArrivalTime.put(pickup, latest);
			
			early = 0;
			latest = INF_TIME;
			if(exLadenRequests[i].getLateDateTimeUnloadAtPort() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						exLadenRequests[i].getLateDateTimeUnloadAtPort()));
			earliestAllowedArrivalTime.put(delivery, early);
			serviceDuration.put(delivery, (int)(input.getParams().getUnlinkLoadedContainerDuration()));
			lastestAllowedArrivalTime.put(delivery, latest);
			
		}

		for(int i = 0; i < imEmptyRequests.length; i++){
			groupId++;
			group2marked.put(groupId, 0);
			for(int j = 0; j < input.getDepotContainers().length; j++){
				id++;
				Warehouse wh = mCode2Warehouse.get(
						imEmptyRequests[i].getWareHouseCode());
				Point pickup = new Point(Integer.parseInt(wh.getCode()), wh.getLocationCode());
				id++;
				DepotContainer depotCont = input.getDepotContainers()[j];
				Point delivery = new Point(Integer.parseInt(depotCont.getCode()), depotCont.getLocationCode());
	
				points.add(pickup);
				points.add(delivery);
				
				pickupPoints.add(pickup);
				deliveryPoints.add(delivery);
				
				pickup2Delivery.put(pickup, delivery);
				delivery2Pickup.put(delivery, pickup);
				
				point2moocWeight.put(pickup, 0);
				point2moocWeight.put(delivery, 0);
				
				point2containerWeight.put(pickup, 1);
				point2containerWeight.put(delivery, -1);
				
				point2Type.put(pickup, WAREHOUSE);
				point2Type.put(delivery, END_CONT);
				
				point2Group.put(pickup, groupId);
				point2Group.put(delivery, groupId);
				
				int early = 0;
				int latest = INF_TIME;
				if(imEmptyRequests[i].getEarlyDateTimeAttachAtWarehouse() != null)
					early = (int)(DateTimeUtils.dateTime2Int(
							imEmptyRequests[i].getEarlyDateTimeAttachAtWarehouse()));
				earliestAllowedArrivalTime.put(pickup, early);
				serviceDuration.put(pickup, input.getParams().getLinkEmptyContainerDuration());
				lastestAllowedArrivalTime.put(pickup, latest);
				
				early = 0;
				latest = INF_TIME;
	
				if(imEmptyRequests[i].getLateDateTimeReturnEmptyAtDepot() != null)
					latest = (int)(DateTimeUtils.dateTime2Int(
							imEmptyRequests[i].getLateDateTimeReturnEmptyAtDepot()));
				earliestAllowedArrivalTime.put(delivery, early);
				serviceDuration.put(delivery, (int)(input.getParams().getUnlinkEmptyContainerDuration()));
				lastestAllowedArrivalTime.put(delivery, latest);
			}
		}
		
		for(int i = 0; i < imLadenRequests.length; i++)
		{
			groupId++;
			group2marked.put(groupId, 0);
			id++;
			Port port = mCode2Port.get(
					imLadenRequests[i].getPortCode());
			Point pickup = new Point(Integer.parseInt(port.getCode()), port.getLocationCode());
			
			id++;
			Warehouse wh = mCode2Warehouse.get(
					imLadenRequests[i].getWareHouseCode());
			Point delivery = new Point(Integer.parseInt(wh.getCode()), wh.getLocationCode());

			points.add(pickup);
			points.add(delivery);
			
			pickupPoints.add(pickup);
			deliveryPoints.add(delivery);
			
			pickup2Delivery.put(pickup, delivery);
			delivery2Pickup.put(delivery, pickup);
			
			point2moocWeight.put(pickup, 0);
			point2moocWeight.put(delivery, 0);
			
			point2containerWeight.put(pickup, 1);
			point2containerWeight.put(delivery, -1);
			
			point2Type.put(pickup, PORT);
			point2Type.put(delivery, WAREHOUSE);
			
			point2Group.put(pickup, groupId);
			point2Group.put(delivery, groupId);
			
			int early = 0;
			int latest = INF_TIME;
			if(imLadenRequests[i].getEarlyDateTimePickupAtPort() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getEarlyDateTimePickupAtPort()));
			if(imLadenRequests[i].getLateDateTimePickupAtPort() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getLateDateTimePickupAtPort()));
			earliestAllowedArrivalTime.put(pickup, early);
			serviceDuration.put(pickup, input.getParams().getLinkLoadedContainerDuration());
			lastestAllowedArrivalTime.put(pickup, latest);
			
			early = 0;
			latest = INF_TIME;
			if(imLadenRequests[i].getEarlyDateTimeUnloadAtWarehouse() != null)
				early = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getEarlyDateTimeUnloadAtWarehouse()));
			if(imLadenRequests[i].getLateDateTimeUnloadAtWarehouse() != null)
				latest = (int)(DateTimeUtils.dateTime2Int(
						imLadenRequests[i].getLateDateTimeUnloadAtWarehouse()));
			earliestAllowedArrivalTime.put(delivery, early);
			serviceDuration.put(delivery, (int)(input.getParams().getUnlinkLoadedContainerDuration()));
			lastestAllowedArrivalTime.put(delivery, latest);
			
		}
		
		nwMooc = new NodeWeightsManager(points);
		nwContainer = new NodeWeightsManager(points);
		awm = new ArcWeightsManager(points);
		double max_time = Double.MIN_VALUE;
		for (int i = 0; i < points.size(); i++) {
			for (int j = 0; j < points.size(); j++) {
				double tmp_cost = getTravelTime(points.get(i).getLocationCode(),
						points.get(j).getLocationCode());
				awm.setWeight(points.get(i), points.get(j), tmp_cost);
				max_time = tmp_cost > max_time ? tmp_cost : max_time;
			}
			nwMooc.setWeight(points.get(i), point2moocWeight.get(points.get(i)));
			nwContainer.setWeight(points.get(i), point2containerWeight.get(points.get(i)));
		}
		MAX_TRAVELTIME = max_time;
	}
	
	public void readData(String fileName){
		try{
			Gson g = new Gson();
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			input = g.fromJson(in, ContainerTruckMoocInput.class);
			in.close();
			InputAnalyzer IA = new InputAnalyzer();
			IA.standardize(input);
			mapData(fileName);
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	public void mapData(String dataFileName) {

		// create artificial containers based on import request
		Random ran = new Random();
		additionalContainers = new ArrayList<Container>();
		int idxCode = -1;
//		if(input.getImRequests() != null){
//			for (int i = 0; i < input.getImRequests().length; i++) {
//				ImportContainerTruckMoocRequest R = input.getImRequests()[i];
//				for (int j = 0; j < R.getContainerRequest().length; j++) {
//					ImportContainerRequest r = R.getContainerRequest()[j];
//
//					idxCode++;
//					String code = "A-" + idxCode;
//					String depotContainerCode = null;
//					if(r.getDepotContainerCode() != null)
//						depotContainerCode = r.getDepotContainerCode()[0];
//					else{
//						int idx = ran.nextInt(input.getDepotContainers().length);
//						depotContainerCode = input.getDepotContainers()[idx].getCode();
//					}
//					Container c = new Container(code, (int) r.getWeight(),
//							r.getContainerCategory(), depotContainerCode,
//							r.getDepotContainerCode());
//					additionalContainers.add(c);
//					r.setContainerCode(code);
//				}
//			}
//		}
//		if(input.getImEmptyRequests() != null){
//			for (int i = 0; i < input.getImEmptyRequests().length; i++) {
//				ImportEmptyRequests R = input.getImEmptyRequests()[i];
//				idxCode++;
//				String code = "A-" + idxCode;
//				String depotContainerCode = null;
//				if(R.getDepotContainerCode() != null)
//					depotContainerCode = R.getDepotContainerCode();
//				else{
//					int idx = ran.nextInt(input.getDepotContainers().length);
//					depotContainerCode = input.getDepotContainers()[idx].getCode();
//				}
//				DepotContainer[] dpc = input.getDepotContainers();
//				for(int k = 0; k < dpc.length; k++){
//					if(dpc[k].getCode().equals(depotContainerCode))
//						dpc[k].setReturnedContainer(true);
//				}
//				input.setDepotContainers(dpc);
//				
//				String[] returnDepot = new String[1];
//				returnDepot[0] = new String();
//				returnDepot[0] = depotContainerCode;
//				Container c = new Container(code, (int) R.getWeight(),
//						R.getContainerCategory(), depotContainerCode,
//						returnDepot);
//				additionalContainers.add(c);
//				R.setContainerCode(code);
//			}
//		}
//		ArrayList<String> containerCodes = new ArrayList<String>();
//		
//		Container[] temp = input.getContainers();
//		ArrayList<Container> cL = new ArrayList<Container>();
//		for (int i = 0; i < temp.length; i++) {
//			if(!containerCodes.contains(temp[i].getCode())){
//				containerCodes.add(temp[i].getCode());
//				cL.add(temp[i]);
//			}
//		}
//		Container[] L = new Container[cL.size()
//				+ additionalContainers.size()];
//		for (int i = 0; i < cL.size(); i++) {
//			L[i] = cL.get(i);
//			L[i].setImportedContainer(false);
//		}
//		for (int i = 0; i < additionalContainers.size(); i++) {
//			L[i + cL.size()] = additionalContainers.get(i);
//			L[i + cL.size()].setImportedContainer(true);
//		}
//		input.setContainers(L);

		HashSet<String> s_locationCode = new HashSet<String>();
		for (int i = 0; i < input.getDistance().length; i++) {
			DistanceElement e = input.getDistance()[i];
			String src = e.getSrcCode();
			String dest = e.getDestCode();
			s_locationCode.add(src);
			s_locationCode.add(dest);
		}
		locationCodes = new String[s_locationCode.size()];
		mLocationCode2Index = new HashMap<String, Integer>();
		int idx = -1;
		for (String lc : s_locationCode) {
			idx++;
			locationCodes[idx] = lc;
			mLocationCode2Index.put(lc, idx);
		}
		distance = new double[s_locationCode.size()][s_locationCode.size()];
		travelTime = new double[s_locationCode.size()][s_locationCode.size()];
		for (int i = 0; i < input.getDistance().length; i++) {
			DistanceElement e = input.getDistance()[i];
			String src = e.getSrcCode();
			String dest = e.getDestCode();
			double d = e.getDistance();
			int is = mLocationCode2Index.get(src);
			int id = mLocationCode2Index.get(dest);
			distance[is][id] = d;

			travelTime[is][id] = e.getTravelTime();
		}
//		ArrayList<DepotContainer> dcL = new ArrayList<DepotContainer>();
//		ArrayList<String> codes = new ArrayList<String>();
//		for (int i = 0; i < input.getDepotContainers().length; i++) {
//			if(!codes.contains(input.getDepotContainers()[i].getCode())){
//				codes.add(input.getDepotContainers()[i].getCode());
//				dcL.add(input.getDepotContainers()[i]);
//			}
//		}
//		DepotContainer[] dpc = new DepotContainer[dcL.size()];
//		for(int i = 0; i < dcL.size(); i++)
//			dpc[i] = dcL.get(i);
//		input.setDepotContainers(dpc);
		
		mCode2DepotContainer = new HashMap<String, DepotContainer>();
		for (int i = 0; i < input.getDepotContainers().length; i++) {
			mCode2DepotContainer.put(input.getDepotContainers()[i].getCode(),
					input.getDepotContainers()[i]);
		}

//		ArrayList<DepotMooc> depotMoocList = new ArrayList<DepotMooc>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getDepotMoocs().length; i++) {
//			if(!codes.contains(input.getDepotMoocs()[i].getCode())){
//				codes.add(input.getDepotMoocs()[i].getCode());
//				depotMoocList.add(input.getDepotMoocs()[i]);
//			}
//		}
//		DepotMooc[] dpm = new DepotMooc[depotMoocList.size()];
//		for(int i = 0; i < depotMoocList.size(); i++)
//			dpm[i] = depotMoocList.get(i);
//		input.setDepotMoocs(dpm);
		
		mCode2DepotMooc = new HashMap<String, DepotMooc>();
		for (int i = 0; i < input.getDepotMoocs().length; i++) {
			mCode2DepotMooc.put(input.getDepotMoocs()[i].getCode(),
					input.getDepotMoocs()[i]);
		}
		
//		ArrayList<DepotTruck> depotTruckList = new ArrayList<DepotTruck>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getDepotTrucks().length; i++) {
//			if(!codes.contains(input.getDepotTrucks()[i].getCode())){
//				codes.add(input.getDepotTrucks()[i].getCode());
//				depotTruckList.add(input.getDepotTrucks()[i]);
//			}
//		}
//		DepotTruck[] dpt = new DepotTruck[depotTruckList.size()];
//		for(int i = 0; i < depotTruckList.size(); i++)
//			dpt[i] = depotTruckList.get(i);
//		input.setDepotTrucks(dpt);
		
		mCode2DepotTruck = new HashMap<String, DepotTruck>();
		for (int i = 0; i < input.getDepotTrucks().length; i++) {
			mCode2DepotTruck.put(input.getDepotTrucks()[i].getCode(),
					input.getDepotTrucks()[i]);
		}
		
//		ArrayList<Warehouse> whList = new ArrayList<Warehouse>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getWarehouses().length; i++) {
//			if(!codes.contains(input.getWarehouses()[i].getCode())){
//				codes.add(input.getWarehouses()[i].getCode());
//				whList.add(input.getWarehouses()[i]);
//			}
//		}
//		Warehouse[] whs = new Warehouse[whList.size()];
//		for(int i = 0; i < whList.size(); i++)
//			whs[i] = whList.get(i);
//		input.setWarehouses(whs);
		
		mCode2Warehouse = new HashMap<String, Warehouse>();
		for (int i = 0; i < input.getWarehouses().length; i++) {
			mCode2Warehouse.put(input.getWarehouses()[i].getCode(),
					input.getWarehouses()[i]);
		}
		
//		ArrayList<Mooc> moocList = new ArrayList<Mooc>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getMoocs().length; i++) {
//			if(!codes.contains(input.getMoocs()[i].getCode())){
//				codes.add(input.getMoocs()[i].getCode());
//				moocList.add(input.getMoocs()[i]);
//			}
//		}
//		Mooc[] ms = new Mooc[moocList.size()];
//		for(int i = 0; i < moocList.size(); i++)
//			ms[i] = moocList.get(i);
//		input.setMoocs(ms);
		
		mCode2Mooc = new HashMap<String, Mooc>();
		for (int i = 0; i < input.getMoocs().length; i++) {
			Mooc mooc = input.getMoocs()[i];
			mCode2Mooc.put(mooc.getCode(), mooc);
		}
		
//		ArrayList<Truck> truckList = new ArrayList<Truck>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getTrucks().length; i++) {
//			if(!codes.contains(input.getTrucks()[i].getCode())){
//				codes.add(input.getTrucks()[i].getCode());
//				truckList.add(input.getTrucks()[i]);
//			}
//		}
//		Truck[] ts = new Truck[truckList.size()];
//		for(int i = 0; i < truckList.size(); i++)
//			ts[i] = truckList.get(i);
//		input.setTrucks(ts);

		mCode2Truck = new HashMap<String, Truck>();
		for (int i = 0; i < input.getTrucks().length; i++) {
			Truck truck = input.getTrucks()[i];
			mCode2Truck.put(truck.getCode(),
					truck);
		}

		mCode2Container = new HashMap<String, Container>();
		for (int i = 0; i < input.getContainers().length; i++) {
			Container c = input.getContainers()[i];
			mCode2Container.put(c.getCode(), c);
		}

//		ArrayList<Port> portList = new ArrayList<Port>();
//		codes = new ArrayList<String>();
//		for (int i = 0; i < input.getPorts().length; i++) {
//			if(!codes.contains(input.getPorts()[i].getCode())){
//				codes.add(input.getPorts()[i].getCode());
//				portList.add(input.getPorts()[i]);
//			}
//		}
//		Port[] ps = new Port[portList.size()];
//		for(int i = 0; i < portList.size(); i++)
//			ps[i] = portList.get(i);
//		input.setPorts(ps);
		
		mCode2Port = new HashMap<String, Port>();
		for (int i = 0; i < input.getPorts().length; i++) {
			mCode2Port.put(input.getPorts()[i].getCode(), input.getPorts()[i]);
		}
		
//		try{
//			Gson gson = new Gson();
//			File fo = new File(dataFileName);
//			FileWriter fw = new FileWriter(fo);
//			gson.toJson(input, fw);
//			fw.close();
//		}
//		catch(Exception e){
//			System.out.println(e);
//		}
	}
	
	//roulette-wheel mechanism
 	private int get_operator(double[] p){
 		//String message = "probabilities input \n";
 		
 		int n = p.length;
		double[] s = new double[n];
		s[0] = 0+p[0];

		
		for(int i=1; i<n; i++)
			s[i] = s[i-1]+p[i]; 
		
		double r = s[n-1]*Math.random();
		
		if(r>=0 && r <= s[0])
			return 0;
		
		for(int i=1; i<n; i++){
			if(r>s[i-1] && r<=s[i])
				return i;
		}
		return -1;
	}
	
	public Point getBestMoocForRequest(int r, Point p, Point pickup){
		Point bestMooc = null;
		double min_d = Double.MAX_VALUE;
		for(int i = 0; i < startMoocPoints.size(); i++){
			Point stMooc = startMoocPoints.get(i);
			int groupMooc = point2Group.get(stMooc);
			if(group2marked.get(groupMooc) == 1
				|| XR.route(stMooc) != Constants.NULL_POINT)
				continue;
			double d = getTravelTime(p.getLocationCode(), stMooc.getLocationCode())
				+ getTravelTime(stMooc.getLocationCode(), pickup.getLocationCode());
			if(d < min_d){
				min_d = d;
				bestMooc = stMooc;
			}
		}
		return bestMooc;
	}
	
	public Point getBestDepotMoocForRoute(int r){
//		HashMap<Mooc, Integer> mooc2marked = new HashMap<Mooc, Integer>();
		Mooc[] moocs = input.getMoocs();
//		for(int i = 0; i < moocs.length; i++)
//			mooc2marked.put(moocs[i], 0);
//		for(int i = 0; i < startMoocPoints.size(); i++){
//			Point stMooc = startMoocPoints.get(i);
//			if(XR.route(stMooc) != Constants.NULL_POINT){
//				Mooc mooc = startPoint2Mooc.get(stMooc);
//				mooc2marked.put(mooc, 1);
//			}
//		}
		
		Point st = XR.getStartingPointOfRoute(r);
		Point bestMooc = null;
		double min_d = Double.MAX_VALUE;
		for(int i = 0; i < startMoocPoints.size(); i++){
			Point stMooc = startMoocPoints.get(i);

			int groupMooc = point2Group.get(stMooc);
			if(group2marked.get(groupMooc) == 1)
				continue;
			double d = objective.evaluateAddOnePoint(stMooc, st);
			if(d < min_d){
				min_d = d;
				bestMooc = stMooc;
			}
		}
		return bestMooc;
	}
	
	public int getNbRejectedRequests(){
		Set<Integer> grs = new HashSet<Integer>();
		for(int i = 0; i < rejectPickupPoints.size(); i++){
			Point pickup = rejectPickupPoints.get(i);
			int groupId = point2Group.get(pickup);
			
			if(group2marked.get(groupId) == 1)
				continue;
			grs.add(groupId);
		}
		return grs.size();
	}
	
	public int getNbUsedTrucks(){
		int nb = 0;
		for(int r = 1; r <= XR.getNbRoutes(); r++){
			int a = XR.index(XR.getTerminatingPointOfRoute(r));
			if(XR.index(XR.getTerminatingPointOfRoute(r)) > 3)
				nb++;
		}
		return nb;
	}
	
	public Truck getNearestTruck(String locationCode){
		int minTime = Integer.MAX_VALUE;
		Truck truck = null;
		for(int i = 0; i < input.getTrucks().length; i++){
			int d = getTravelTime(input.getTrucks()[i].getDepotTruckLocationCode(),
					locationCode);
			if(d < minTime){
				minTime = d;
				truck = input.getTrucks()[i];
			}
		}
		return truck;
	}
	
	public Mooc getNearestMooc(String locationCode){
		int minTime = Integer.MAX_VALUE;
		Mooc mooc = null;
		for(int i = 0; i < input.getMoocs().length; i++){
			int d = getTravelTime(input.getMoocs()[i].getDepotMoocLocationCode(),
					locationCode);
			if(d < minTime){
				minTime = d;
				mooc = input.getMoocs()[i];
			}
		}
		return mooc;
	}
	
	public int getTravelTime(String src, String dest) {
		if (mLocationCode2Index.get(src) == null
				|| mLocationCode2Index.get(dest) == null) {
			 System.out.println("::getTravelTime, src " + src +
			 " OR dest " + dest + " NOT COMPLETE, INPUT ERROR??????");
			//return 1000;

		}

		int is = mLocationCode2Index.get(src);
		int id = mLocationCode2Index.get(dest);
		return (int) travelTime[is][id];
	}

	public void greedyInitSolution(){
		double currtime = System.currentTimeMillis();

		for(int i = 0; i < pickup2Delivery.size(); i++){
			Point pickup = pickupPoints.get(i);
			int groupId = point2Group.get(pickup);
			if(XR.route(pickup) != Constants.NULL_POINT
				|| group2marked.get(groupId) == 1)
				continue;
			Point delivery = deliveryPoints.get(i);
			//add the request to route
			Point pre_pick = null;
			Point pre_delivery = null;
			Point beststMooc1 = null;
			Point bestenMooc1 = null;
			Point beststMooc2 = null;
			Point best_pre = null;
			double best_objective = Double.MAX_VALUE;
			
			for(int r = 1; r <= XR.getNbRoutes(); r++){
				Point st = XR.getStartingPointOfRoute(r);

				int groupTruck = point2Group.get(st);
				if(group2marked.get(groupTruck) == 1 && XR.index(XR.getTerminatingPointOfRoute(r)) <= 1)
					continue;
				
				for(Point p = st; p != XR.getTerminatingPointOfRoute(r); p = XR.next(p)){
					for(Point q = p; q != XR.getTerminatingPointOfRoute(r); q = XR.next(q)){
						Point pp = p;
						Point qq = q;
						Point stMooc1 = null;
						Point stMooc2 = null;
						Point enMooc1 = null;
						Point enMooc2 = null;
						Point pre = null;
						if(accMoocInvr.getSumWeights(p) == 0){
							stMooc1 = getBestMoocForRequest(r, p, pickup);
							mgr.performAddOnePoint(stMooc1, p);
							pp = stMooc1;
							if(p == q)
								qq = pp;
							enMooc1 = start2stopMoocPoint.get(stMooc1);
						}

						if(accMoocInvr.getWeights(delivery) < 0){
							enMooc1 = null;
							System.out.println("accContainerInvr.getWeights("
									+ XR.next(q).getID() + ") = "
									+ accContainerInvr.getWeights(XR.next(q)));
							if(accContainerInvr.getWeights(XR.next(q)) > 0){
								stMooc2 = getBestMoocForRequest(r, q, delivery);
								mgr.performAddOnePoint(stMooc2, q);
								enMooc1 = start2stopMoocPoint.get(stMooc2);
							}
						}
						for(Point h = XR.next(q); h != XR.getTerminatingPointOfRoute(r); h = XR.next(h)){
							if(enMooc1 != null
								&& stopMoocPoints.contains(h)){
									enMooc2 = h;
									break;
								}
						}
						if(enMooc1 != null){
							if(enMooc2 != null){
								pre = XR.prev(enMooc2);
								mgr.performAddOnePoint(enMooc1, pre);
								mgr.performRemoveOnePoint(enMooc2);
							}
							else{
								pre = XR.prev(XR.getTerminatingPointOfRoute(r));
								mgr.performAddOnePoint(enMooc1, pre);
							}
						}
						
						mgr.performAddTwoPoints(pickup, pp, delivery, qq);
						if(S.violations() == 0){
							double cost = objective.getValue();
							if( cost < best_objective){
								best_objective = cost;
								pre_pick = p;
								pre_delivery = q;
								beststMooc1 = stMooc1;
								bestenMooc1 = enMooc1;
								beststMooc2 = stMooc2;
								best_pre = pre;
							}
						}
						mgr.performRemoveTwoPoints(pickup, delivery);
						if(stMooc1 != null)
							mgr.performRemoveOnePoint(stMooc1);
						if(stMooc2 != null)
							mgr.performRemoveOnePoint(stMooc2);
						if(enMooc2 != null && enMooc1 != null)
							mgr.performAddOnePoint(enMooc2, pre);
						if(enMooc1 != null)
							mgr.performRemoveOnePoint(enMooc1);
					}
				}
			}
			if(pre_pick != null && pre_delivery != null){
				if(accMoocInvr.getSumWeights(pre_pick) == 0 && beststMooc1 != null){
					mgr.performAddOnePoint(beststMooc1, pre_pick);
					if(pre_pick == pre_delivery)
						pre_delivery = beststMooc1;
					pre_pick = beststMooc1;
					
					int groupMooc= point2Group.get(beststMooc1);
					group2marked.put(groupMooc, 1);
				}
				if(accMoocInvr.getWeights(delivery) < 0)
					if(accContainerInvr.getWeights(XR.next(pre_delivery)) > 0 && beststMooc2 != null){
						mgr.performAddOnePoint(beststMooc2, pre_delivery);
						int groupMooc= point2Group.get(beststMooc2);
						group2marked.put(groupMooc, 1);
					}
				if(bestenMooc1 != null){
					Point enM = XR.next(best_pre);
					mgr.performAddOnePoint(bestenMooc1, best_pre);
					if(enM != XR.getTerminatingPointOfRoute(XR.route(best_pre)))
						mgr.performRemoveOnePoint(enM);
				}
				
				mgr.performAddTwoPoints(pickup, pre_pick, delivery, pre_delivery);
				Point st = XR.getStartingPointOfRoute(XR.route(pre_pick));
				int groupTruck = point2Group.get(st);
				group2marked.put(groupTruck, 1);
				group2marked.put(groupId, 1);
			}
		}
		for(int i = 0; i < pickup2Delivery.size(); i++){
			Point pickup = pickupPoints.get(i);
			if(XR.route(pickup) == Constants.NULL_POINT && !rejectPickupPoints.contains(pickup)){
//				int groupId = point2Group.get(pickup);
//				if(group2marked.get(groupId) == 0)
//					testTimeWindow(pickup);
				rejectPickupPoints.add(pickup);
				rejectDeliveryPoints.add(pickup2Delivery.get(pickup));
			}
		}
		
		//insertMoocToRoutes();
		
		int nRp = getNbRejectedRequests();
		int nB = getNbUsedTrucks();
		System.out.println("nb rejected requests = " + nRp
				+ ", nb trucks = " + nB
				+ ", cost = " + objective.getValue()
				+ ", time for inserting reqs = " + (System.currentTimeMillis() - currtime)/1000);
	}
	
	//chen req roi chen mooc
	public void greedyInitSolution2(){
		double currtime = System.currentTimeMillis();
		int n = 0;
		for(int i = 0; i < pickup2Delivery.size(); i++){
			System.out.println("req " + i + "/" + pickup2Delivery.size());
			Point pickup = pickupPoints.get(i);
			int groupId = point2Group.get(pickup);
			if(XR.route(pickup) != Constants.NULL_POINT
				|| group2marked.get(groupId) == 1)
				continue;
			Point delivery = deliveryPoints.get(i);
			//add the request to route
			Point pre_pick = null;
			Point pre_delivery = null;
			double best_objective = Double.MAX_VALUE;
			
			for(int r = 1; r <= XR.getNbRoutes(); r++){
				Point st = XR.getStartingPointOfRoute(r);

				int groupTruck = point2Group.get(st);
				if(group2marked.get(groupTruck) == 1 && XR.index(XR.getTerminatingPointOfRoute(r)) <= 1)
					continue;
//				if(pickup.getID() == 985 && (r == 23 || r == 27))
//					System.out.println("ds");
				for(Point p = st; p != XR.getTerminatingPointOfRoute(r); p = XR.next(p)){
					for(Point q = p; q != XR.getTerminatingPointOfRoute(r); q = XR.next(q)){
//						if(pickup.getID() == 1271 && p.getID() == 121 && q.getID() == 1222)
//							System.out.println("f");
						mgr.performAddTwoPoints(pickup, p, delivery, q);
						insertMoocToRoutes(r);
						if(S.violations() == 0){
							double cost = objective.getValue();
							if( cost < best_objective){
								best_objective = cost;
								pre_pick = p;
								pre_delivery = q;
							}
						}
						mgr.performRemoveTwoPoints(pickup, delivery);
						removeMoocOnRoutes(r);
					}
				}
			}
			if(pre_pick != null && pre_delivery != null){
				mgr.performAddTwoPoints(pickup, pre_pick, delivery, pre_delivery);
				//insertMoocToRoutes(XR.route(pre_pick));
				Point st = XR.getStartingPointOfRoute(XR.route(pre_pick));
				int groupTruck = point2Group.get(st);
				group2marked.put(groupTruck, 1);
				group2marked.put(groupId, 1);
				n++;
			}
		}
		System.out.println("n = " + n);
		for(int i = 0; i < pickup2Delivery.size(); i++){
			Point pickup = pickupPoints.get(i);
			if(XR.route(pickup) == Constants.NULL_POINT && !rejectPickupPoints.contains(pickup)){
//				int groupId = point2Group.get(pickup);
//				if(group2marked.get(groupId) == 0)
//					testTimeWindow(pickup);
				rejectPickupPoints.add(pickup);
				rejectDeliveryPoints.add(pickup2Delivery.get(pickup));
			}
		}
		
		insertMoocForAllRoutes();
		
		int nRp = getNbRejectedRequests();
		int nB = getNbUsedTrucks();
		System.out.println("nb rejected requests = " + nRp
				+ ", nb trucks = " + nB
				+ ", cost = " + objective.getValue()
				+ ", time for inserting reqs = " + (System.currentTimeMillis() - currtime)/1000);
		//checkRejectedRequestsInfo();
	}
	
	public void removeAllMoocFromRoutes(){
		for(int i = 0; i < startMoocPoints.size(); i++){
			Point st = startMoocPoints.get(i);
			Point tp = start2stopMoocPoint.get(st);
			if(XR.route(st) != Constants.NULL_POINT){
				mgr.performRemoveOnePoint(st);
				int groupMooc = point2Group.get(st);
				group2marked.put(groupMooc, 0);
			}
			if(XR.route(tp) != Constants.NULL_POINT){
				mgr.performRemoveOnePoint(tp);
				int groupMooc = point2Group.get(tp);
				group2marked.put(groupMooc, 0);
			}
		}
	}
	
	public void removeMoocOnRoutes(int r){
		Point x = XR.getStartingPointOfRoute(r);
		Point next_x = XR.next(x);
		while(next_x != XR.getTerminatingPointOfRoute(r)){
			x = next_x;
			next_x = XR.next(x);
			if(startMoocPoints.contains(x) || stopMoocPoints.contains(x)){
				mgr.performRemoveOnePoint(x);
				int groupMooc = point2Group.get(x);
				group2marked.put(groupMooc, 0);
			}
		}
	}
	
	public void localSearchForImprovement(String outputfile)
	{
		int it = 0;
		int timeLimit = 36000000;
    	int nIter = 10000;
    	int maxStable = 10000;
    	int iS = 0;
    	
    	SearchOptimumByLocalSearch sa = new SearchOptimumByLocalSearch(this);
    	sa.explore(nIter, timeLimit, maxStable, outputfile);
	}
	
	public void stateModel(){
		mgr = new VRManager();
		XR = new VarRoutesVR(mgr);
		S = new ConstraintSystemVR(mgr);
		for(int i = 0; i < startPoints.size(); ++i)
			XR.addRoute(startPoints.get(i), stopPoints.get(i));
		
		for(int i = 0; i < pickupPoints.size(); ++i)
		{
			Point pickup = pickupPoints.get(i);
			Point delivery = deliveryPoints.get(i);
			XR.addClientPoint(pickup);
			XR.addClientPoint(delivery);
		}
		for(int i = 0; i < startMoocPoints.size(); ++i){
			XR.addClientPoint(startMoocPoints.get(i));
			XR.addClientPoint(stopMoocPoints.get(i));
		}
		
		//time windows
		eat = new EarliestArrivalTimeVR(XR,awm,earliestAllowedArrivalTime,serviceDuration);
		cEarliest = new CEarliestArrivalTimeVR(eat, lastestAllowedArrivalTime);
		
		//accumulated mooc
		accMoocInvr = new AccumulatedWeightNodesVR(XR, nwMooc);
		
		//accumulated container
		accContainerInvr = new AccumulatedWeightNodesVR(XR, nwContainer);
		
		//container capacity constraint
		capContCtr = new ContainerCapacityConstraint(XR, accContainerInvr);
		
		//mooc capacity constraint
		capMoocCtr = new MoocCapacityConstraint(XR, accMoocInvr);
		
		//container carried by trailer constraint
		contmoocCtr = new ContainerCarriedByTrailerConstraint(XR, accContainerInvr, accMoocInvr);
		
		S.post(cEarliest);
		S.post(capContCtr);
		S.post(capMoocCtr);
		S.post(contmoocCtr);
		objective = new TotalCostVR(XR,awm);
		valueSolution = new LexMultiValues();
		valueSolution.add(S.violations());
		valueSolution.add(objective.getValue());
		mgr.close();
	}
	
	public void testTimeWindow(Point pickup){
		int groupId = point2Group.get(pickup);
		if(XR.route(pickup) != Constants.NULL_POINT
			|| group2marked.get(groupId) == 1)
			System.out.println("conflict");
		Point delivery = pickup2Delivery.get(pickup);
		//add the request to route
		Point pre_pick = null;
		Point pre_delivery = null;
		mgr.performRemoveAllClientPoints();
		
		double best_objective = Double.MAX_VALUE;
		for(int r = 1; r <= XR.getNbRoutes(); r++){
			Point st = XR.getStartingPointOfRoute(r);
			Truck truck = startPoint2Truck.get(st);

			for(Point p = st; p != XR.getTerminatingPointOfRoute(r); p = XR.next(p)){
				for(Point q = p; q != XR.getTerminatingPointOfRoute(r); q = XR.next(q)){
					System.out.println("pick = " + pickup.getID()
							+ ", p = " + p.getID()
							+ ", del = " + delivery.getID()
							+ ", q = " + q.getID());
					System.out.println("arrTime p = " + DateTimeUtils.unixTimeStamp2DateTime((long)eat.getEarliestAllowedArrivalTime(p)));
					System.out.println("travelTime p ->pick = " + eat.getTravelTime(p, pickup));
					System.out.println("late = " + DateTimeUtils.unixTimeStamp2DateTime((long)lastestAllowedArrivalTime.get(pickup)));
					System.out.println("arrTime q = " + DateTimeUtils.unixTimeStamp2DateTime((long)eat.getEarliestAllowedArrivalTime(pickup)));
					System.out.println("travelTime pick ->del = " + eat.getTravelTime(pickup, delivery));
					System.out.println("arrTime del = " + DateTimeUtils.unixTimeStamp2DateTime((long)(eat.getEarliestAllowedArrivalTime(pickup)
							+ eat.getTravelTime(pickup, delivery))));
					System.out.println("late del = " + DateTimeUtils.unixTimeStamp2DateTime((long)lastestAllowedArrivalTime.get(delivery)));
					
					mgr.performAddTwoPoints(pickup, p, delivery, q);
					insertMoocToRoutes(r);
					if(S.violations() == 0){
						System.out.println("route = " + r + " is ok");
					}
					else
						System.out.println("pick = " + pickup.getID() + " not ok");
					mgr.performRemoveTwoPoints(pickup, delivery);
					removeMoocOnRoutes(r);
				}
			}
		}
		System.out.println("stop");
	}
	
	public void printSolution(String outputfile){
		String s = "";
		ArrayList<ArrayList<Point>> _route = new ArrayList<ArrayList<Point>>();
		
		int K = XR.getNbRoutes();
		
		for(int k=1; k<=K; k++){
			s += "route[" + k + "] = ";
			Point x = XR.getStartingPointOfRoute(k);
			for(; x != XR.getTerminatingPointOfRoute(k); x = XR.next(x)){
				s = s + x.getID() + " " + " -> ";
			}
			x = XR.getTerminatingPointOfRoute(k);
			s = s + x.getID() + "\n";
		}		
		System.out.println(s);
		
		int nbR = getNbRejectedRequests();
		int nB = getNbUsedTrucks();
		System.out.println("Search done. At end search number of reject points = " + nbR
				+ ", nb Trucks = " + nB
				+ ",  cost = " + objective.getValue()
				+ ", vio = " + S.violations());
		try{
			FileOutputStream write = new FileOutputStream(outputfile, true);
			PrintWriter fo = new PrintWriter(write);
			fo.println(s);
			fo.println("end time = " + DateTimeUtils.unixTimeStamp2DateTime(System.currentTimeMillis()/1000) 
					+ ", total reqs = " + nRequest
					+ ", #RejectedReqs = " + nbR
					+ ", nb Trucks = " + nB
					+ ", cost = " + objective.getValue()
					+ ", vio = " + S.violations());
			
			fo.close();
		}catch(Exception e){
			
		}
		
	}
	
	public static void main(String[] args){
		String dir = "data/truck-container/";

		String fileName = "random_big_data-8reqs.json";
		String outputfile = dir + "output/result-" + fileName + "-LCS.txt"; 
		String dataFileName = dir + fileName;
		SolverWithLocalSearch solver = new SolverWithLocalSearch();
		solver.readData(dataFileName);
		solver.init();
		solver.stateModel();
		solver.greedyInitSolution2();
		//solver.printSolution(outputfile);
		//solver.adaptiveSearchOperators(outputfile);
		solver.localSearchForImprovement(outputfile);
		solver.printSolution(outputfile);

	}
}
