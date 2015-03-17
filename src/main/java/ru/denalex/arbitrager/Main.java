package ru.denalex.arbitrager;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.btcchina.BTCChinaExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;

/**
 * @author alexey.zakharchenko@gmail.com
 */
public class Main {
    private static final Logger error_log = LoggerFactory.getLogger(Main.class);
    private static final Logger china_log = LoggerFactory.getLogger("btcchina");
    private static final Logger china_usd_log = LoggerFactory.getLogger("btcchina_usd");
    private static final Logger bitfinex_log = LoggerFactory.getLogger("bitfinex");
    private static final Logger arbitrage_log = LoggerFactory.getLogger("arbitrage");

    private static final double PROFIT_THRESHOLD_USD_PER_BTC = 0.5;

    public static void main(String[] args) {
        Exchange btcChina = ExchangeFactory.INSTANCE.createExchange(BTCChinaExchange.class.getName());
        Exchange bitfinex = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());

        while (true) {
            try {
                Ticker chinaTicker = btcChina.getPollingMarketDataService().getTicker(CurrencyPair.BTC_CNY);
                china_log.info(s(chinaTicker));
                Ticker bitfinexTicker = bitfinex.getPollingMarketDataService().getTicker(CurrencyPair.BTC_USD);
                bitfinex_log.info(s(bitfinexTicker));

                double cny2usd = CurrencyExchange.getExchangeRate("CNY", "USD");

                checkArbitrage(bitfinexTicker, chinaTicker, cny2usd);

                Thread.sleep(1000);

            } catch (Exception e) {
                error_log.error("Error on getting ticker", e);
            }
        }
    }

    private static BigDecimal muliplty(BigDecimal what, double rate) {
        return what == null ? null : what.multiply(new BigDecimal(rate), MathContext.DECIMAL32);
    }


    // Format: timestamp, last, bid, ask, high, low, avg, volume
    private static String s(Ticker ticker) {
        return ticker.getTimestamp().getTime() + ", " +
                ticker.getLast() + ", " +
                ticker.getBid() + ", " +
                ticker.getAsk() + ", " +
                ticker.getHigh() + ", " +
                ticker.getLow() + ", " +
                ticker.getVwap() + ", " +
                ticker.getVolume();
    }

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance();
    // @todo Consider market fee!
    private static void checkArbitrage(Ticker bitfinexTicker, Ticker btcChinaTicker, double cny2usd) {
        Ticker chinaTickerUsd = new Ticker.Builder()
                .currencyPair(btcChinaTicker.getCurrencyPair())
                .last(muliplty(btcChinaTicker.getLast(), cny2usd))
                .bid(muliplty(btcChinaTicker.getBid(), cny2usd))
                .ask(muliplty(btcChinaTicker.getAsk(), cny2usd))
                .high(muliplty(btcChinaTicker.getHigh(), cny2usd))
                .low(muliplty(btcChinaTicker.getLow(), cny2usd))
                .vwap(muliplty(btcChinaTicker.getVwap(), cny2usd))
                .volume(muliplty(btcChinaTicker.getVolume(), cny2usd))
                .timestamp(btcChinaTicker.getTimestamp()
                ).build();

        china_usd_log.info(s(chinaTickerUsd));


        double diff = bitfinexTicker.getAsk().subtract(chinaTickerUsd.getBid(),MathContext.DECIMAL32).doubleValue();
        if (diff > PROFIT_THRESHOLD_USD_PER_BTC) {
            arbitrage_log.warn("\t*** Bitfinex.Ask > BTCChina.Bid");
            arbitrage_log.info("Bitfinex {}", bitfinexTicker);
            arbitrage_log.info("BTCChina {}", chinaTickerUsd);
            arbitrage_log.info("$$$ PROFIT, USD per BTC {}", PRICE_FORMAT.format(diff));
        } else {
            diff = chinaTickerUsd.getAsk().subtract(bitfinexTicker.getBid(), MathContext.DECIMAL32).doubleValue();
            if (diff > PROFIT_THRESHOLD_USD_PER_BTC) {
                arbitrage_log.warn("\t*** Bitfinex.Bid < BTCChina.Ask");
                arbitrage_log.info("Bitfinex {}", bitfinexTicker);
                arbitrage_log.info("BTCChina {}", chinaTickerUsd);
                arbitrage_log.info("$$$ PROFIT, USD per BTC {}", PRICE_FORMAT.format(diff));
            }
        }
    }
}
