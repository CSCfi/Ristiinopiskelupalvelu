package fi.uta.ristiinopiskelu.messaging.util;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Oid {

    public static final int PERSON_NODE_ID = 24;
    public static final int TEST_PERSON_NODE_ID = 98;

    public static final String ROOT_NODE = "1.2.246.562";

    private static final long RANDOM_MIN = 1000000000L;
    private static final long RANDOM_MAX = 9999999999L;
    
    private static final List<Integer> VALID_NODE_IDS = List.of(
        PERSON_NODE_ID,
        TEST_PERSON_NODE_ID
    );

    private Oid() {
    }

    public static String randomOid(int nodeId) {
        Assert.isTrue(VALID_NODE_IDS.contains(nodeId), "Invalid nodeId");
        String randomNumberPart = String.valueOf(ThreadLocalRandom.current().nextLong(RANDOM_MIN, RANDOM_MAX + 1));
        int ibmChecksum = ibmChecksum(randomNumberPart);
        return "%s.%s.%s%s".formatted(ROOT_NODE, nodeId, randomNumberPart, ibmChecksum);
    }

    public static boolean hasNode(String oid, int nodeId) {
        Assert.isTrue(VALID_NODE_IDS.contains(nodeId), "Invalid nodeId");
        return StringUtils.hasText(oid) && oid.startsWith("%s.%s".formatted(ROOT_NODE, nodeId));
    }

    public static boolean isValid(String oid) {
        if(!StringUtils.hasText(oid)) {
            return false;
        }

        String[] splittedOid = oid.split("\\.");

        if(splittedOid.length < 5) {
            return false;
        }

        String rootPart = "%s.%s.%s.%s".formatted(splittedOid[0], splittedOid[1], splittedOid[2], splittedOid[3]);
        if(!rootPart.equals(ROOT_NODE)) {
            return false;
        }

        String nodeIdPart = splittedOid[4];
        try {
            int nodeId = Integer.parseInt(nodeIdPart);
            if(!VALID_NODE_IDS.contains(nodeId)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        String randomNumberPartWithCheckDigit = splittedOid[5];
        String checkDigit = randomNumberPartWithCheckDigit.substring(randomNumberPartWithCheckDigit.length() - 1);
        String randomNumberPart = randomNumberPartWithCheckDigit.substring(0, randomNumberPartWithCheckDigit.length() - 1);

        try {
            long randomNumber = Long.parseLong(randomNumberPart);
            if(randomNumber < RANDOM_MIN || randomNumber > RANDOM_MAX) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        int calculatedCheckDigit = ibmChecksum(randomNumberPart);

        return String.valueOf(calculatedCheckDigit).equals(checkDigit);
    }

    public static int ibmChecksum(String randomPart) {
        int sum = 0;
        int[] alternate = {7, 3 , 1};

        for (int i = randomPart.length() - 1, j = 0; i >= 0; i--, j++) {
            int n = Integer.parseInt(randomPart.substring(i, i + 1));

            sum += n * alternate[j % 3];
        }

        return (10 - sum % 10) % 10;
    }
}
