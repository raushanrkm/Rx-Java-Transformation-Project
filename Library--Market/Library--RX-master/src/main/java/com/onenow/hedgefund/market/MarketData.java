package com.onenow.hedgefund.market;

import com.onenow.hedgefund.acquirer.InvestmentAcquirer;
import com.onenow.hedgefund.discrete.*;
import com.onenow.hedgefund.ibdiscrete.*;
import com.onenow.hedgefund.event.RecordActivity;
import com.onenow.hedgefund.event.RequestHistory;
import com.onenow.hedgefund.investment.Investment;
import com.onenow.hedgefund.logging.Watchr;
import com.onenow.hedgefund.time.DateTime;
import com.onenow.hedgefund.responsetsdb.LookupSeries;

import java.util.HashMap;


public class MarketData {

	public static ServiceType serviceType;
	public static FanOut fanOut;

	// ON-OFF
	public boolean doPriceHistory = false;
	public boolean doPriceSizeRealtime = false;
	public boolean doPriceStreaming = false;
	public boolean doSizeStreaming = false;
	public boolean doGreekStreaming = false;
	public boolean doGenericStreaming = false;
	public boolean doTickStreaming = false;

	public static HashMap<Investment, Long> lastTradeMilisecMap = new HashMap<Investment, Long>();

	private static InvestmentAcquirer investmentAcquirer;

	// The on-off must be set after construction, by default unset
	public MarketData(ServiceType serviceType) {
		this.serviceType = serviceType;
		this.fanOut = new FanOut(serviceType);
		investmentAcquirer = new InvestmentAcquirer();
	}

	// HISTORY
	public void writePriceHistory(RequestHistory request,
								  Long dateStamp, Double open, Double high, Double low, Double close,
								  Integer volume, Integer count, Double wap, Boolean hasGaps, InvDataSource source) {

		Investment inv = investmentAcquirer.get(request.getContractID());
		String serieName = LookupSeries.getKey(inv, request.getPriceType());

		// open priceRecord is the one that occurred at the timestamp
		Double price = open;
		RecordActivity priceRecord = new RecordActivity(price, getTimeInMsec(dateStamp), serieName,
				DataType.PRICE, DataTiming.HISTORIC, source);
		RecordActivity vwapRecord = new RecordActivity(wap, getTimeInMsec(dateStamp), serieName,
				DataType.VWAP, DataTiming.HISTORIC, source);
		RecordActivity volumeRecord = new RecordActivity(new Double(volume), getTimeInMsec(dateStamp), serieName,
				DataType.VOLUME, DataTiming.HISTORIC, source);

		RecordActivity countRecord = new RecordActivity(new Double(count), getTimeInMsec(dateStamp), serieName,
				DataType.COUNT, DataTiming.HISTORIC, source);

		// TODO: handle hasGaps

		if(doPriceHistory) {
			if(price>0) {
				fanOut.writeThread(priceRecord);
			} else {
				Watchr.log("History without PRICE: " + priceRecord);
			}
			if(wap>0) {			// TODO: can be negative/positive?
				fanOut.writeThread(vwapRecord);
			} else {
				Watchr.log("History without VWAP: " + priceRecord);
			}
			if(volume>0) {		// TODO: can be negative/positive?
				fanOut.writeThread(volumeRecord);
			} else {
				Watchr.log("History without VOLUME: " + priceRecord);
			}
			if(count>0) {
				fanOut.writeThread(countRecord);
			} else {
				Watchr.log("History without COUNT: " + priceRecord);
			}
			// Watchr.log(Level.INFO, "Received History from " + MemoryLevel.L3PARTNER + " " + history.toString());
		}
	}

	private long getTimeInMsec(Long dateStamp) {
		return dateStamp*1000;
	}


	// PRICE
	public void writePriceStreaming(Investment inv, Double lastPrice, PriceType priceType, InvDataSource source) {

		String seriesName = LookupSeries.getKey(inv, priceType);
		RecordActivity record = new RecordActivity(lastPrice, DateTime.getTimeMilisecondsNow(), seriesName,
				DataType.PRICE, DataTiming.STREAMING, source);

		if(doPriceStreaming) {
			if(lastPrice>0) {
				fanOut.writeThread(record);
			}
		}
	}

	// SIZE
	public void writeSizeStreaming(Investment inv, Integer lastSize, SizeType sizeType, InvDataSource source) {

		String seriesName = LookupSeries.getKey(inv, sizeType);
		RecordActivity record = new RecordActivity(lastSize.doubleValue(), DateTime.getTimeMilisecondsNow(), seriesName,
				DataType.SIZE, DataTiming.STREAMING, source);

		if(doSizeStreaming) {
			if(lastSize>0) {		// TODO: can be negative/positive?
				fanOut.writeThread(record);
			}
		}
	}

	// GREEKS
	public void writeGreekStreaming(Investment inv, Double greek, GreekType greekType, InvDataSource source) {

		String seriesName = LookupSeries.getKey(inv, greekType);
		RecordActivity record = new RecordActivity(greek.doubleValue(), DateTime.getTimeMilisecondsNow(), seriesName,
				greekType, DataTiming.STREAMING, source);

		if(doGreekStreaming) {
			// TODO: check if >=0?
			fanOut.writeThread(record);
		}
	}

	// GENERIC
	public void writeGenericStreaming(Investment inv, Double computation, GenericType genericType, InvDataSource source) {

		String seriesName = LookupSeries.getKey(inv, genericType);
		RecordActivity record = new RecordActivity(computation.doubleValue(), DateTime.getTimeMilisecondsNow(), seriesName,
				genericType, DataTiming.STREAMING, source);

		if(doGenericStreaming) {
			// TODO: check if >=0?
			fanOut.writeThread(record);
		}
	}

	// STRINGS
	public void parseAndWriteStrings(Investment inv, TickType tickType, String value, InvDataSource source) {

		String seriesName = LookupSeries.getKey(inv, tickType);
		RecordActivity record = new RecordActivity(value, DateTime.getTimeMilisecondsNow(), seriesName,
				tickType, DataTiming.STREAMING, source);

		if(doTickStreaming) {
			// TODO: check if >=0 ?
			fanOut.writeThread(record);
		}
	}

	/**
	 * RT VOLUME: parse, then write
	 * @param inv
	 * @param rtvolume
	 */
	public void parseAndWriteRealTimePriceSize(Investment inv, String rtvolume) {
		String lastTradedPrice="";
		String lastTradeSize="";
		String lastTradeTime="";
		String totalVolume="";
		String VWAP="";
		String splitFlag="";
		
		int i=1;
		for(String split:rtvolume.split(";")) {
			if(i==1) { //	Last trade price
				lastTradedPrice = split;
				if(lastTradedPrice.equals("")) {
					return;
				}
			}
			if(i==2) { //	Last trade size
				lastTradeSize = split;
				if(lastTradeSize.equals("")) {
					return;
				}
			}
			if(i==3) { //	Last trade time
				lastTradeTime = split;
				if(lastTradeTime.equals("")) {
					return;
				}
			}
			if(i==4) { //	Total volume
				totalVolume = split;
				if(totalVolume.equals("")) {
					return;
				}
			}
			if(i==5) { //	VWAP
				VWAP = split;
				if(VWAP.equals("")) {
					return;
				}
			}
			if(i==6) { //	Single trade flag - True indicates the trade was filled by a single market maker; False indicates multiple market-makers helped fill the trade
				splitFlag = split;
				if(splitFlag.equals("")) {
					return;
				}
			}
			i++;
		}
		Long time = Long.parseLong(lastTradeTime); 	
		
		InvDataSource source = InvDataSource.IB;

		Double price = Double.parseDouble(lastTradedPrice);
		if(price==null || price.isInfinite() || price.isNaN()) {
			return;
		}

		writePriceSizeRealtime(	time, inv, price, Integer.parseInt(lastTradeSize),
									Integer.parseInt(totalVolume), Double.parseDouble(VWAP), Boolean.parseBoolean(splitFlag),
									source);
		return;
	}

	/**
	 * Write RT VOLUME
	 * @param timeInMilisec
	 * @param inv
	 * @param lastPrice
	 * @param lastSize
	 * @param volume
	 * @param VWAP
	 * @param splitFlag
	 * @param source
	 */
	private void writePriceSizeRealtime(	Long timeInMilisec, Investment inv, Double lastPrice, Integer lastSize,
											Integer volume, Double VWAP, boolean splitFlag, InvDataSource source) {
		// price
		String priceSeries = LookupSeries.getKey(inv, PriceType.TRADED);
		RecordActivity priceRecord = new RecordActivity(lastPrice, timeInMilisec, priceSeries,
				DataType.PRICE, DataTiming.REALTIME, source);
		// size
		String sizeSeries = LookupSeries.getKey(inv, SizeType.TRADED_SIZE);
		RecordActivity sizeRecord = new RecordActivity(lastSize.doubleValue(), timeInMilisec, sizeSeries,
				DataType.SIZE, DataTiming.REALTIME, source);
		// volume
		String volumeSeries = LookupSeries.getKey(inv, DataType.VOLUME);
		RecordActivity volumeRecord = new RecordActivity(new Double(volume), timeInMilisec, volumeSeries,
				DataType.VOLUME, DataTiming.REALTIME, source);
		// vwap
		String vwapSeries = LookupSeries.getKey(inv, DataType.VWAP);
		RecordActivity vwapRecord = new RecordActivity(VWAP, timeInMilisec, vwapSeries,
				DataType.VWAP, DataTiming.REALTIME, source);

		// TODO: write splitFlag

		if(doPriceSizeRealtime) {
			if(lastPrice>0) {
				fanOut.writeThread(priceRecord);
			} else {
				Watchr.warning("RT Volume without price: " + priceRecord);
			}
			if(lastSize>0) {
				fanOut.writeThread(sizeRecord);
			} else {
				Watchr.warning("RT Volume without size: " + priceRecord);
			}
			if(volume>0) {
				fanOut.writeThread(volumeRecord);
			} else {
				Watchr.warning("RT Volume without volume: " + priceRecord);
			}
			if(VWAP>0) {
				fanOut.writeThread(vwapRecord);
			} else {
				Watchr.warning("RT Volume without VWAP: " + priceRecord);
			}
		}
	}


	// PRINT
	public String toString() {
		String s="";
		s = fanOut.toString();
		return s;
	}

}
