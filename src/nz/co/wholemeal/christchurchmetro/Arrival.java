package nz.co.wholemeal.christchurchmetro;

class Arrival {

  public static final String TAG = "Arrival";

  private String routeNumber;
  private String routeName;
  private String destination;
  private int eta;
  private boolean wheelchairAccess;

  public Arrival() {
  }

  public Arrival(String routeNumber, String routeName, String destination,
      int eta, boolean wheelchairAccess) {
    this.routeNumber = routeNumber;
    this.routeName = routeName;
    this.destination = destination;
    this.eta = eta;
    this.wheelchairAccess = wheelchairAccess;
  }

  public String getRouteName() {
    return routeName;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  public String getRouteNumber() {
    return routeNumber;
  }

  public void setRouteNumber(String routeNumber) {
    this.routeNumber = routeNumber;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public int getEta() {
    return eta;
  }

  public void setEta(int eta) {
    this.eta = eta;
  }

  public boolean getWheelchairAccess() {
    return wheelchairAccess;
  }

  public void setWheelchairAccess(boolean wheelchairAccess) {
    this.wheelchairAccess = wheelchairAccess;
  }
}
