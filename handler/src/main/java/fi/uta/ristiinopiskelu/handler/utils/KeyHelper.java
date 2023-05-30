package fi.uta.ristiinopiskelu.handler.utils;

/*
    Helper class to help verifying if study elements are same (unique keys match)
*/
public class KeyHelper {
    private String id;
    private String organisationId;

    public KeyHelper(String id, String organisationId) {
        this.id = id;
        this.organisationId = organisationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        KeyHelper keyHelper = (KeyHelper) o;

        return id.equals(keyHelper.id)
                && organisationId.equals(keyHelper.organisationId);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (organisationId != null ? organisationId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "(" + id + ", " + organisationId + ")";
    }
}