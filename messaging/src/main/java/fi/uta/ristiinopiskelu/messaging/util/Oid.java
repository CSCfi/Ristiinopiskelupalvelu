package fi.uta.ristiinopiskelu.messaging.util;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

public class Oid {

    public static final String ROOT_NODE = "1.2.246.562";

    private static final long RANDOM_MIN = 1000000000L;
    private static final long RANDOM_MAX = 9999999999L;

    private Oid() {
    }

    public static String randomOid(OidPersonNode oidPersonNode) {
        Assert.notNull(oidPersonNode, "oidPersonNode must not be null");
        String randomNumberPart = String.valueOf(ThreadLocalRandom.current().nextLong(RANDOM_MIN, RANDOM_MAX + 1));
        int ibmChecksum = oidPersonNode.getAlgorithm().checksum(randomNumberPart);
        return "%s.%s.%s%s".formatted(ROOT_NODE, oidPersonNode.getNodeId(), randomNumberPart, ibmChecksum);
    }

    public static boolean hasNode(String oid, OidPersonNode oidPersonNode) {
        Assert.notNull(oidPersonNode, "oidPersonNode must not be null");
        return StringUtils.hasText(oid) && oid.startsWith("%s.%s".formatted(ROOT_NODE, oidPersonNode.getNodeId()));
    }

    public static boolean isValid(String oid, boolean validateChecksum) {
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
        OidPersonNode oidPersonNode;

        try {
            int nodeId = Integer.parseInt(nodeIdPart);
            oidPersonNode = OidPersonNode.from(nodeId);
            if(oidPersonNode == null) {
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

        if(validateChecksum) {
            int calculatedCheckDigit = oidPersonNode.getAlgorithm().checksum(randomNumberPart);
            return String.valueOf(calculatedCheckDigit).equals(checkDigit);
        }

        return true;
    }
}
