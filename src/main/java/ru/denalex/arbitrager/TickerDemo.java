package ru.denalex.arbitrager;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.btcchina.BTCChinaExchange;
import com.xeiam.xchange.btcchina.service.polling.BTCChinaAccountServiceRaw;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;

import java.io.IOException;

//import com.xeiam.xchange.bitfinex.*;
//import com.xeiam.xchange.bitstamp.dto.marketdata.BitstampTicker;
//import com.xeiam.xchange.bitstamp.service.polling.BitstampMarketDataServiceRaw;

/**
 * Demonstrate requesting Ticker at Bitstamp. You can access both the raw data from Bitstamp or the XChange generic DTO data format.
 */
public class TickerDemo {

    public static void main(String[] args) throws IOException {

        // Use the factory to get Bitstamp exchange API using default settings
        Exchange btcChina = ExchangeFactory.INSTANCE.createExchange(BTCChinaExchange.class.getName());

        // Interested in the public polling market data feed (no authentication)
        PollingMarketDataService marketDataService = btcChina.getPollingMarketDataService();

        for (int i = 0; i < 21; i++) {
            generic(marketDataService);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //raw((BTCChinaAccountServiceRaw) marketDataService);
    }

    private static void generic(PollingMarketDataService marketDataService) throws IOException {

        Ticker ticker = marketDataService.getTicker(CurrencyPair.BTC_CNY);

        System.out.println(ticker.toString());
    }

    private static void raw(BTCChinaAccountServiceRaw marketDataService) throws IOException {

        Object bitstampTicker = marketDataService.getBTCChinaAccountInfo();

        System.out.println(bitstampTicker.toString());
    }

}