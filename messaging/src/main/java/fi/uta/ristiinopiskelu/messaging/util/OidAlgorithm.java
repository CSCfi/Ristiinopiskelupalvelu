package fi.uta.ristiinopiskelu.messaging.util;

public enum OidAlgorithm {

    IBM,
    LUHN;

    public int checksum(String randomPart) {
        switch (this) {
            case IBM -> {
                return ibmChecksum(randomPart);
            }
            case LUHN -> {
                return luhnChecksum(randomPart);
            }
            default -> throw new IllegalStateException("Unknown checksum algorithm");
        }
    }

    private int ibmChecksum(String randomPart) {
        int sum = 0;
        int[] alternate = {7, 3 , 1};

        for (int i = randomPart.length() - 1, j = 0; i >= 0; i--, j++) {
            int n = Integer.parseInt(randomPart.substring(i, i + 1));

            sum += n * alternate[j % 3];
        }

        return (10 - sum % 10) % 10;
    }

    private int luhnChecksum(String randomPart) {
        int sum = 0;
        boolean alternate = true;

        for (int i = randomPart.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(randomPart.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (10 - sum % 10) % 10;
    }
}
