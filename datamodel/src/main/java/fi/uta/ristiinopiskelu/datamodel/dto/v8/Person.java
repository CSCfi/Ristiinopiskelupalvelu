package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import java.util.Objects;

public class Person {

  private String homeEppn;
  private String hostEppn;
  private String firstNames;
  private String surName;
  private String givenName;

  public String getHomeEppn() {
    return homeEppn;
  }

  public void setHomeEppn(String homeEppn) {
    this.homeEppn = homeEppn;
  }

  public String getHostEppn() {
    return hostEppn;
  }

  public void setHostEppn(String hostEppn) {
    this.hostEppn = hostEppn;
  }

  public String getFirstNames() {
    return firstNames;
  }

  public void setFirstNames(String firstNames) {
    this.firstNames = firstNames;
  }

  public String getSurName() {
    return surName;
  }

  public void setSurName(String surName) {
    this.surName = surName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Person)) return false;
    Person person = (Person) o;
    return Objects.equals(homeEppn, person.homeEppn) &&
            Objects.equals(hostEppn, person.hostEppn) &&
            Objects.equals(firstNames, person.firstNames) &&
            Objects.equals(surName, person.surName) &&
            Objects.equals(givenName, person.givenName);
  }

  @Override
  public int hashCode() {

    return Objects.hash(homeEppn, hostEppn, firstNames, surName, givenName);
  }
}
