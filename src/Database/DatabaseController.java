package Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import Definitions.*;
import org.ElevatorPoint;
import org.Point;

/**
 * Created by evan on 3/25/17.
 * This Object will add, remove and edit our hospital database
 */
public class DatabaseController implements DatabaseInterface {

  ArrayList<Point> localPoints;
  ArrayList<Physician> localPhysicians;


  DatabaseDriver dbc = null;

  public DatabaseController(DatabaseDriver _dbc) {
    this.localPhysicians = new ArrayList<Physician>();
    this.localPoints = new ArrayList<Point>();
    this.dbc = _dbc;
  }

  ///////////////////////////
  /////// Physician /////////
  ///////////////////////////

  public boolean removePhysician(String first_name, String last_name, String title) {
    dbc.send_Command(
        "delete from physician (first_name, last_name, title) values ('" + first_name + "','"
            + last_name + "','" + title + "')");
    return true;
  }

  public boolean addPhysician(long PID, String first_name, String last_name, String title,
      ArrayList<FakePoint> array_points) {
    dbc.send_Command(
        "insert into physician (pid,first_name, last_name, title) values (" + PID + ",'"
            + first_name + "','"
            + last_name + "','" + title + "')");

    int i;
    for (i = 0; i < array_points.size(); i++) {
      this.addPhysicianLocation(PID, array_points.get(i).id);
    }
    return true;
  }

  public FakePhysician get_physician(int pid) {
    ResultSet res = dbc.send_Command("select * from physician where pid = " + pid).get(0);
    int c = 0;
    FakePhysician my_p = null;
    c++;
    try {
      while (res.next()) {
        if (c > 1) {
          System.out.println("db error, was not supposed to happen");
          break;
        }
        String first_name = res.getString("FIRST_NAME");
        String last_name = res.getString("LAST_NAME");
        String title = res.getString("TITLE");
        int new_pid = res.getInt("PID");

        my_p = new FakePhysician(first_name, last_name, title, pid, new ArrayList<Integer>());
        //physicians.add(p);

      }

      res.close();

      if (my_p == null) {
        System.out.println("no physician found");
        return null;
      }

      ResultSet res2 = dbc.send_Command("select * from physician_location where pid_ph = " + pid)
          .get(0);

      ArrayList<Integer> my_locs = new ArrayList<Integer>();
      while (res2.next()) {
        int new_pid2 = res2.getInt("PID_po");
        my_locs.add(get_point(new_pid2).getId());

      }
      res2.close();
      my_p.setLocations(my_locs);


    } catch (SQLException e) {
      //e.printStackTrace();
      System.out.println("error getting fake physcians from DB; Query Erro: " + e.getMessage());
    }

    return my_p;


  }


  public ArrayList<Physician> getAllPhysicians() throws SQLException {
    ArrayList<FakePhysician> fphysicians = new ArrayList<FakePhysician>();
    ResultSet res = dbc.send_Command("select pid from physician").get(0);
    while (res.next()) {
      int pid = res.getInt("PID");

      FakePhysician p = get_physician(pid);
      fphysicians.add(p);
    }
    ArrayList<Physician> physicians = new ArrayList<Physician>();
    for (int i = 0; i < fphysicians.size(); i++) {
      physicians.add(fphysicians.get(i).toRealPhysician());
    }
    for (int i = 0; i < physicians.size(); i++) {
      ArrayList<Integer> currentLocations = findFakePhysician(physicians.get(i), fphysicians)
          .getLocations();
      ArrayList<Point> locations = new ArrayList<Point>();
      for (int j = 0; j < currentLocations.size(); j++) {
        locations.add(findRealPoint(currentLocations.get(j), localPoints));
      }
      physicians.get(i).setLocations(locations);
    }
    return physicians;

  }

  private FakePhysician findFakePhysician(Physician p, ArrayList<FakePhysician> fps) {
    for (int i = 0; i < fps.size(); i++) {
      if (p.getID() == fps.get(i).getID()) {
        return fps.get(i);
      }
    }
    return null;
  }

  private Physician findRealPhysician(int p, ArrayList<Physician> pts) {
    for (int i = 0; i < pts.size(); i++) {
      if (p == pts.get(i).getID()) {
        return pts.get(i);
      }
    }
    return null;
  }


  public boolean updatePhysicians(ArrayList<Physician> ap) throws SQLException {
//    dbc.send_Command("truncate table Physician; truncate table Physician_location;");
    dbc.send_Command("DELETE from Physician where 1=1; DELETE from Physician_location where 1=1");
    int i;
    for (i = 0; i < ap.size(); i++) {
      ArrayList<Point> points = ap.get(i).getLocations();
      ArrayList<FakePoint> fakePoints = new ArrayList<FakePoint>();
      for (int j = 0; j < points.size(); j++) {
        fakePoints.add(new FakePoint(points.get(j)));
      }
      this.addPhysician(ap.get(i).getID(), ap.get(i).getFirstName(), ap.get(i).getLastName(),
          ap.get(i).getTitle(), fakePoints);
    }

    return true;
  }

  ///////////////////////////
  //// Location - Service ///
  ///////////////////////////

  public boolean addServiceLocation(String service_name, String md_related, String location_name) {
    dbc.send_Command(
        "insert into ServiceLocation (lid,sid) select lid,sid from service_location, service where location.name = '"
            + location_name + "', select sid from service where"
            + " service.name = '" + service_name + "')\n ");
    return true;
  }

  public boolean removeServiceLocation(String service_name, String location_name) {
    dbc.send_Command(
        "delete from ServiceLocation  where sid = (select sid from service where name = '"
            + service_name + "') and  lid = (select lid from location where"
            + " name = '" + location_name + "')\n ");
    return true;
  }

  ///////////////////////////
  /// Location - Physician //
  ///////////////////////////

  public boolean addPhysicianLocation(long pid_ph, int pid_po) {
    dbc.send_Command(
        "insert into Physician_Location (pid_po,pid_ph) values(" + pid_po + "," + pid_ph
            + ");\n");
    return true;
  }

  public boolean removePhysicianLocation(int pid_ph, int pid_po) {
    dbc.send_Command(
        "delete from Physician_Location where pid_ph = " + pid_ph + " and pid_po ='" + pid_po
            + ");\n");
    return true;
  }

  ////////////////////////
  /////////Point/////////
  //////////////////////

  public boolean addPoint(Point realpoint) {
    FakePoint point = new FakePoint(realpoint);
    int cost = point.getCost();
    int x = point.getXCoord();
    int y = point.getYCoord();
    int id = point.getId();
    int floor = point.getFloor();
    String name = point.getName();
    ArrayList<Integer> neighbors = point.getNeighbors();

    dbc.send_Command(
        "insert into Point (x,y,cost,pid,floor,name) values (" + x + ","
            + y + "," + cost + "," + id + "," + floor + ",'" + name + "'); \n");
    return true;
  }

  public boolean addPoint(FakePoint point) {
    int cost = point.getCost();
    int x = point.getXCoord();
    int y = point.getYCoord();
    int id = point.getId();
    int floor = point.getFloor();
    String name = point.getName();
    ArrayList<Integer> neighbors = point.getNeighbors();

    if(name == null){
      name = "";
    }

    dbc.send_Command(
        "insert into Point (x,y,cost,pid,floor,name) values (" + x + ","
            + y + "," + cost + "," + id + "," + floor + ",'" + name + "'); \n");
    return true;
  }


  public boolean removePoint(Point realpoint) {
    FakePoint point = new FakePoint(realpoint);
    int cost = point.getCost();
    int x = point.getXCoord();
    int y = point.getYCoord();
    int id = point.getId();
    int floor = point.getFloor();
    String name = point.getName();

    dbc.send_Command(
        "delete from Point where pid = " + id + ";");
    return true;
  }


  public boolean update_points(ArrayList<Point> rpal) {
    ArrayList<FakePoint> al = new ArrayList<FakePoint>();
    for (int q = 0; q < rpal.size(); q++) {
      al.add(new FakePoint(rpal.get(q)));
    }
    dbc.send_Command("DELETE from Point where 1=1;DELETE from Neighbor 1=1;");
    int i;
    for (i = 0; i < al.size(); i++) {
      this.addPoint(al.get(i));
    }
    //int i;

    int k, l;
    for (k = 0; k < al.size(); k++) {
      //this.addPoint(al.get(i));
      FakePoint point = al.get(k);
      ArrayList<Integer> neighbor_ids = point.getNeighbors();
      for (l = 0; l < neighbor_ids.size(); l++) {
        this.addNeighbor(point.getId(), neighbor_ids.get(l));
        //this.addNeighboring(pl.get(i).id,point.id);
      }
    }

    return true;
  }

  public FakePoint get_point(int my_pid) {
    FakePoint my_point = null;
    ResultSet res1 = dbc.send_Command("select * from point where pid = " + my_pid).get(0);
    int c = 0;
    try {
      while (res1.next()) {
        c++;
        if (c > 1) {
          System.out.println("that was not supposed to happen. ");
          break;
        }

        int floor = res1.getInt("floor");
        String name = res1.getString("NAME");
        if(name == null){
          name = "";
        }
        int pid = res1.getInt("PID");
        int x = res1.getInt("x");
        int y = res1.getInt("y");
        int cost = res1.getInt("cost");

        my_point = new FakePoint(x, y, name, pid, new ArrayList<Integer>(), floor);

        ArrayList<Integer> neighbor_ids = new ArrayList<Integer>();
        ResultSet res4 = dbc.send_Command(
            "select pid1,pid2 from Neighbor where pid1 = " + pid + " OR pid2 = " + pid).get(0);
        while (res4.next()) {
          int pid1 = res4.getInt("Pid1");
          int pid2 = res4.getInt("Pid2");
          if (pid1 != my_pid) {
            neighbor_ids.add(pid1);
          } else {
            neighbor_ids.add(pid2);
          }

        }
        res4.close();

        my_point.setNeighbors(neighbor_ids);


      }
    } catch (SQLException e) {
      // e.printStackTrace();
      System.out.println("error getting fake points from DB; Query Error: " + e.getMessage());

    }

    return my_point;

  }


  public ArrayList<Point> getAllPoints() throws SQLException {
    ArrayList<FakePoint> fakepoints = new ArrayList<FakePoint>();
    ResultSet res = null;
    try {
      res = dbc.send_Command("select pid from point").get(0);
    } catch (IndexOutOfBoundsException e){
      System.out.println("No Result Available");
    }

    FakePoint new_point;
    while (res!=null && res.next()) {
      int pid = res.getInt("PID");
      new_point = get_point(pid);
      fakepoints.add(new_point);
    }
    res.close();
    //Now convert to real
    ArrayList<Point> ret = new ArrayList<Point>();
    for (int i = 0; i < fakepoints.size(); i++) {
      ret.add(fakepoints.get(i).toRealPoint());
    }
    for (int i = 0; i < ret.size(); i++){
      if (ret.get(i).getName().equals("ELEVATOR")){
        Point p = ret.get(i);
        ret.remove(i);
        ret.add(i,toElevatorPoint(p));
      }
    }
    for (int i = 0; i < ret.size(); i++) {
      ArrayList<Integer> currentNeighbors = findFakePoint(ret.get(i), fakepoints).getNeighbors();
      for (int j = 0; j < currentNeighbors.size(); j++) {
        ret.get(i).neighbors.add(findRealPoint(currentNeighbors.get(j), ret));
      }
    }
    return ret;
  }


  private FakePoint findFakePoint(Point p, ArrayList<FakePoint> fps) {
    for (int i = 0; i < fps.size(); i++) {
      if (p.getId() == fps.get(i).getId()) {
        return fps.get(i);
      }
    }
    return null;
  }

  private Point findRealPoint(int p, ArrayList<Point> pts) {
    for (int i = 0; i < pts.size(); i++) {
      if (p == pts.get(i).getId()) {
        return pts.get(i);
      }
    }
    return null;
  }

  //////////////////////
///////Neighbor///////
//////////////////////

  public boolean addNeighbor(int pid1, int pid2) {

    dbc.send_Command(
        "insert into Neighbor (pid1,pid2) values (" + pid1 + "," + pid2 + "); \n");
    return true;
  }

  public boolean removeNeighbor(int pid1, int pid2) {

    dbc.send_Command(
        "delete from Neighbor where pid1 = " + pid1 + " or pid2 = " + pid2 + "); \n");
    return true;
  }

  ///////////////////////
  ////EXTRA DB METHODS///
  ///////////////////////

  private boolean compare_physicians_lists(ArrayList<Physician> l1, ArrayList<Physician> l2) {
    int i, j;
    if (l1.size() != l2.size()) {
      System.out.println("verification failed not same physician size lists");
      return false;
    } else {
      Physician p1, p2;
      for (i = 0; i < l1.size(); i++) {
        p1 = l1.get(i);
        p2 = l2.get(i);
        if (!p1.compareTo(p2)) {
          return false;
        }
      }

    }

    return true;
  }


  private boolean compare_points_lists(ArrayList<Point> l1, ArrayList<Point> l2) {
    int i, j;
    if (l1.size() != l2.size()) {
      System.out.println("verification failed, different points list size");
      return false;
    } else {
      Point p1, p2;
      for (i = 0; i < l1.size(); i++) {
        int k, l;
        p1 = l1.get(i);
        p2 = l2.get(i);
        if (!p1.compareTo(p2)) {
          return false;
        }

      }

    }

    return true;
  }


  private boolean verify_points_update() {

    boolean result;
    try {
      ArrayList<Point> db_points = getAllPoints();
      result = compare_points_lists(db_points, localPoints);
    } catch (SQLException e) {
      System.out.println("Cannot complete verification of points, querry/connection error");
      e.printStackTrace();
      return false;
    }

    return result;
  }

  private boolean verify_physicians_update() {
    boolean result;
    try {
      ArrayList<Physician> db_physicians = getAllPhysicians();
      result = compare_physicians_lists(db_physicians, localPhysicians);
    } catch (SQLException e) {
      System.out.println("Cannot complete verification of physicians, query/connection error");
      e.printStackTrace();
      return false;
    }

    return result;

  }

  private boolean save_and_verify() {
    int c = 0;
    while (c < 3) {
      c++;
      save();
      if (!(verify_physicians_update() && verify_points_update())) {
        System.out.println("ERROR: verification failed, retrying to save " + (3-c) + " more times" );
        save();
      } else {
        System.out.println("VERIFICATION SUCCEEDED");
        return true;
      }
    }
    return false;
  }


  @Override
  public void load() throws SQLException {
    System.out.println("loading physicians and points from DB to local copies ");
    localPoints = getAllPoints();
    localPhysicians = getAllPhysicians();
  }

  @Override
  public void save() {
    System.out.println("trying to transfer local copies of physicians and points to DB");
    for(Physician p : localPhysicians){
      ArrayList<Point> locations = p.getLocations();
      for (int i = 0; i < locations.size(); i ++){
        if (locations.get(i) == null)
          locations.remove(i);
      }
      p.setLocations(locations);
    }
    update_points(localPoints);
    System.out.println("transferred local points copy");
    try {
      updatePhysicians(localPhysicians);
      System.out.println("transferred local physicians copy");
    } catch (SQLException e) {
      //e.printStackTrace();
      System.out
          .println("failed to transfer local physicians copy to DB; Error: " + e.getMessage());
    }

  }

  @Override
  public ArrayList<Point> getNamedPoints() {
    System.out.println("trying to get Points with names");
    ArrayList<Point> namedPoints = new ArrayList<Point>();
    int i;
    for (i = 0; i < localPoints.size(); i++) {
      if (localPoints.get(i).getName() != null && !localPoints.get(i).getName().equals("null") && !localPoints.get(i).getName().equals("") && !(localPoints.get(i).getName().replaceAll("\\s", "") == "")) {
        namedPoints.add(localPoints.get(i));
      }
    }

    return namedPoints;
  }

  @Override
  public ArrayList<Point> getPoints() {
    try {
      System.out.println("requesting points from DB, trying to load");
      load();
    } catch (SQLException e) {
      //e.printStackTrace();
      System.out.println(
          "Error Getting Data From The Database, failed to load, will return DB local points copy \n Query/Connection Error : "
              + e.getMessage());
    }
    return localPoints;
  }

  @Override
  public void setPoints(ArrayList<Point> points) {
    System.out.println("Setting the DB local points copy");
    localPoints = points;
    //save_and_verify();
    save();
  }

  @Override
  public ArrayList<Physician> getPhysicians() {
    try {
      System.out.println("requesting physicians from DB, trying to load");
      load();
    } catch (SQLException e) {
      System.out.println(
          "Error Getting Data From The Database, failed to load, will return DB local physicians copy \n Query/Connection Error : "
              + e.getMessage());
      //e.printStackTrace();
    }
    return localPhysicians;
  }

  @Override
  public void setPhysicians(ArrayList<Physician> physicians) {
    System.out.println("Setting the DB local physicians copy");
    localPhysicians = physicians;
    //save_and_verify();
    save();
  }

  ElevatorPoint toElevatorPoint(Point p){
    ElevatorPoint ep = new ElevatorPoint(p.getXCoord(), p.getYCoord(), p.getName(), p.getId(), p.getNeighbors(), p.getFloor());
    return ep;
  }

}