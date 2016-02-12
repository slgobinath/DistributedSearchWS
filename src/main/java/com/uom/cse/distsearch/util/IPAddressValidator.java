package com.uom.cse.distsearch.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gobinath
 */
public class IPAddressValidator {

    private static final Pattern pattern;

    private static final String IP_ADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    static {
        pattern = Pattern.compile(IP_ADDRESS_PATTERN);
    }

    private IPAddressValidator() {

    }

    /**
     * Validate ip address with regular expression
     *
     * @param ip ip address for validation
     * @return true valid ip address, false invalid ip address
     */
    public static boolean validate(final String ip) {
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}