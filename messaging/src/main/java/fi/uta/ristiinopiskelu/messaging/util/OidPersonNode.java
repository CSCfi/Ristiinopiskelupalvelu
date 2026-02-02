package fi.uta.ristiinopiskelu.messaging.util;

public enum OidPersonNode {

    PERSON(24, OidAlgorithm.IBM),
    TEST_PERSON(98, OidAlgorithm.LUHN);

    private final int nodeId;
    private final OidAlgorithm algorithm;

    OidPersonNode(int nodeId, OidAlgorithm algorithm) {
        this.nodeId = nodeId;
        this.algorithm = algorithm;
    }

    public int getNodeId() {
        return nodeId;
    }

    public OidAlgorithm getAlgorithm() {
        return algorithm;
    }

    public static OidPersonNode from(int nodeId) {
        for(OidPersonNode oidPersonNode : OidPersonNode.values()) {
            if(oidPersonNode.getNodeId() == nodeId) {
                return oidPersonNode;
            }
        }
        return null;
    }
}
