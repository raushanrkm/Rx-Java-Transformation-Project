package com.onenow.hedgefund.market;

import com.onenow.hedgefund.event.*;

import io.reactivex.Observable;

import java.util.List;

/**
 * Created by pablo on 6/15/16.
 */

public interface ObserverInterface {

	Observable<String> getKeys();
    void notify(RecordActivity event);

}
