package nz.co.wholemeal.christchurchmetro;

class Arrival {

  public static final String TAG = "Arrival";

  private String routeNumber;
  private String routeName;
  private String destination;
  private int eta;
  private boolean wheelchairAccess;

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
}
