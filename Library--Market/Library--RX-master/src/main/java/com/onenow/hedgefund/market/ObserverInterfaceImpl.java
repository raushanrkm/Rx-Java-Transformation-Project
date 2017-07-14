package com.onenow.hedgefund.market;

import com.onenow.hedgefund.event.RecordActivity;
import com.onenow.hedgefund.market.ObserverInterface;

import io.reactivex.Observable;

public class ObserverInterfaceImpl implements ObserverInterface {

	@Override
	public Observable<String> getKeys() {
		// TODO Auto-generated method stub
		return Observable.just("key1", "key2", "key1", "key4", "key1");
	}

	@Override
	public void notify(RecordActivity event) {
		// TODO Auto-generated method stub
	System.out.println("this messgage for test only "+event.getStoredValue());
		
	}
	
	

}
