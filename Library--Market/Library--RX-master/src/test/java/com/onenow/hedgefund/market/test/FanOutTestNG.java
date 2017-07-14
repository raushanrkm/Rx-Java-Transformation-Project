package com.onenow.hedgefund.market.test;


import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.onenow.hedgefund.event.RecordActivity;
import com.onenow.hedgefund.market.FanOut;
import com.onenow.hedgefund.market.ObserverInterface;
import com.onenow.hedgefund.market.ObserverInterfaceImpl;

public class FanOutTestNG {

private FanOut classundertest;
private ObserverInterface observer;
private RecordActivity recordactivity;

	
@BeforeSuite
	public void setup(){
		classundertest= new FanOut(null);
	    observer= new ObserverInterfaceImpl();
	    recordactivity=new RecordActivity();
		
	}
	
	
	@Test
	public void testForRegisterObserver() {
		//ObserverInterface observer= new ObserverInterfaceImpl();
		classundertest.registerObserver(observer);
		
}
	
	
	
	@Test(dependsOnMethods={"testForRegisterObserver"})
	public void testForDisplayKeyFromInvestmentObservers () {
		classundertest.investmentObservers.subscribe(hashmap->hashmap.keySet().forEach(System.out::println));
		
}
	
	@Test(dependsOnMethods={"testForDisplayKeyFromInvestmentObservers"})
	public void testForNotifyObservers(){
		recordactivity.setSerieName("key1");
		recordactivity.setStoredValue("500");
		classundertest.notifyObservers(recordactivity);
		
		
	}
	
	/*@Test(dependsOnMethods={"testForNotifyObservers"})
	public void testForNotifyObserversPrintRecordActivity(){
		 System.out.println("------------From-------------testForNotifyObserversPrintRecordActivity-----------------");
		classundertest.investmentObservers.subscribe(hashmap->hashmap.keySet().stream()
				.map(key->hashmap.get(key)).flatMap(set->set.stream())
				.forEach(ObserverInterface->{ObserverInterface.notify(recordactivity);})
				
				);
		
		
	}*/
	

	
	@AfterSuite
	public void cleanup(){
		classundertest=null;
		
	}

	
	
	
	
	
	
}
