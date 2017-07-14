package com.onenow.hedgefund.market;

import com.onenow.hedgefund.discrete.ServiceType;
import com.onenow.hedgefund.discrete.TableName;
import com.onenow.hedgefund.event.LookupResponse;
import com.onenow.hedgefund.event.RecordActivity;
import com.onenow.hedgefund.logging.Watchr;
import com.onenow.hedgefund.nosqlclient.NoSqlClient;
import com.onenow.hedgefund.tsdbclient.TsdbClient;


import java.util.HashMap;
import java.util.HashSet;
import io.reactivex.*;
import io.reactivex.internal.operators.observable.*;
import io.reactivex.schedulers.Schedulers;

/**
 * Writes data to the Bus.  Also notifies the Investor of incoming data.
 */
public class FanOut {

	//public HashMap<String, HashSet<ObserverInterface>> investmentObservers; // public for testability

public Observable<HashMap<String,HashSet<ObserverInterface>>> investmentObservers;

       
	
	public FanOut(ServiceType serviceType) {
		this.investmentObservers = Observable.just(new HashMap<String, HashSet<ObserverInterface>>());
	}

	/**
	 * LookupSeries is used for key generation for gene invs + data timing
	 * @param observer
	 */
	public void registerObserver(ObserverInterface observer) {
		
	observer.getKeys().subscribe( key-> {
		               investmentObservers.map(investmentObservers-> {
				                           HashSet<ObserverInterface> observers= investmentObservers.get(key);
		                                   if(observers==null){
		                               		//System.out.println(key);
		                                	  observers=new HashSet<ObserverInterface>();
		                                	  investmentObservers.put(key, observers);
			                            	 }

		                                   observers.add(observer);
		                                   return investmentObservers;
		                                   
		                                   }).subscribe();
		               
		                               });
		
		
		/*for(String key:observer.getKeys()) {
			HashSet<ObserverInterface> observers = investmentObservers.get(key);
			if(observers==null) {
				observers = new HashSet<ObserverInterface>();
				investmentObservers.put(key, observers);
			}
			observers.add(observer);
		}*/
	}

	/**
	 * WRITE
	 * Call back from the broker to write events (Realtime, Streaming)
	 * 
     */
	public void writeThread(final RecordActivity record) {
		
		
		Schedulers.newThread();
		
		new Thread () {
			@Override public void run() {
				try {
					write(record);  // Bus does re-tries
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	// TODO: thread?
	private void write(RecordActivity record) {
		notifyObservers(record);
		writeToBus(record);
	}

	private void writeToBus(RecordActivity record) {
		try {
			TsdbClient.PUT(record);	// save the activity
		} catch (Exception e) {
			Watchr.finer(e.toString());
			// e.printStackTrace();
		}
	}
	public void notifyObservers(RecordActivity record) {
		
		
		try {
		investmentObservers.map(investmentObservers->{investmentObservers
				.get(record.getSerieName()).forEach(notifed->notifed.notify(record));
				
		return investmentObservers;
				
		}).subscribe();
		
		   }
	     catch (Exception e) {
	    	// e.printStackTrace();
	                
	         }
	   
		
		
		/*try {
			for(ObserverInterface notifed: investmentObservers.get(record.getSerieName())) {
                notifed.notify(record);
            }
		} catch (Exception e) {
			// e.printStackTrace();
		}*/
	}

}
