package org.multibit.hd.core.utils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by over on 18.01.15.
 */
@Ignore
public class AddressConv {

    private static Logger logger = LoggerFactory.getLogger(AddressConv.class);

    /**
     *   // m/44h/0h/0h/0/0

     // m/44h/0h/0h/0/5
     private static final String SNIFF_EXPECTED_ADDRESS_5 = "178nhbLCC4qZgb9YeJ8tkEhbzX6GBwhrBk";

     // m/44h/0h/0h/0/6
     private static final String SNIFF_EXPECTED_ADDRESS_6 = "1E798BoJxCu94m6Y2Y88WnyXGhLzjyx2yz";

     // m/44h/0h/0h/0/7
     private static final String SNIFF_EXPECTED_ADDRESS_7 = "1GRKMgdf8XcyVMZdaNmbotTWQBQ2wrAQ8A";

     // m/44h/0h/0h/0/8
     private static final String SNIFF_EXPECTED_ADDRESS_8 = "17mmU5aa1ZmXB6DXu6w1Ypfnkra3thP5VJ";

     // m/44h/0h/0h/0/9
     private static final String SNIFF_EXPECTED_ADDRESS_9 = "1LrsPGpBhzMtTM5f9sfGj3LEfCSZRcGXmd";

     * @throws AddressFormatException
     */

    @Test
    public void doConv() throws AddressFormatException {
        Address bitcoin = new Address(BitcoinNetwork.MAIN_NET.get(), "1LrsPGpBhzMtTM5f9sfGj3LEfCSZRcGXmd");
        Address litecoin = new Address(BitcoinNetwork.LITECOIN_MAIN.get(),bitcoin.getHash160());
        logger.debug("litecoin: {}", litecoin.toString());
        System.out.println("litecoin:" + litecoin.toString());
    }
}
