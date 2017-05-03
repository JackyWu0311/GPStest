package ntust.jacky.gpstest;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView txtLat, txtLon, txtCampus;
    Button btnHome, btnVin;
    ShowMap mapfrag;
    
    double outCampusDist;
    int lastStatus = 1, currStatus = 1;
    Location mostRecentLocation = null;
    String[] ntustGPS = {"25.01331&&121.539263",
            "25.015941&&121.542484",
            "25.012537&&121.545178",
            "25.010272&&121.541745"};
    double[] ntustCenter = {25.013421, 121.541785};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = (TextView) findViewById(R.id.txtLat);
        txtLon = (TextView) findViewById(R.id.txtLon);
        txtCampus = (TextView) findViewById(R.id.txtCampus);
        btnHome = (Button) findViewById(R.id.btnHome);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShowMap) mapfrag).pan2Home(ntustCenter[0], ntustCenter[1]);

            }
        });
        btnVin = (Button) findViewById(R.id.btnVin);
        btnVin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShowMap) mapfrag).updatePlaces();
            }
        });
        mapfrag = ShowMap.newInstance(ntustCenter[0], ntustCenter[1]);
        getFragmentManager().beginTransaction().replace(R.id.flMap, mapfrag).commit();
    }

    public void checkLocation(Location loc) {
        mostRecentLocation = loc;
        txtLat.setText("緯度: " + mostRecentLocation.getLatitude());
        txtLon.setText("經度: " + mostRecentLocation.getLongitude());
        checkCampus();
    }

    void checkCampus() {
        int res = 1;
        String currPoint = mostRecentLocation.getLatitude() + "&&" + mostRecentLocation.getLongitude();
        for (int i = 0; i < ntustGPS.length - 1; i++) {
            if (areaCal(ntustGPS[i], ntustGPS[i + 1], currPoint) < 0) {
                res = -1;
                break;
            }
        }
        currStatus = res;
        if (res < 0) {
            String outStr = "身處台科大校外\n";
            double[] Point1 = new double[]{mostRecentLocation.getLatitude(),
                    mostRecentLocation.getLongitude()};
            double[] Point2 = new double[]{ntustCenter[0], ntustCenter[1]};
            outCampusDist = distHaversine(Point1, Point2);
            outStr += "距離: " + String.format("%.2f", outCampusDist) + "公里";
            txtCampus.setText(outStr);
            txtCampus.setTextColor(Color.RED);
        }else {
            txtCampus.setText("身處台科大校園");
            txtCampus.setTextColor(Color.GREEN);
        }
        lastStatus = currStatus;
    }

    double areaCal(String loc1,String loc2, String loc0){
        String[]point1,point2,point0;
        point1 = loc1.split("&&");
        point2 = loc2.split("&&");
        point0 = loc0.split("&&");
        double x1=Double.parseDouble(point1[1]);
        double y1=Double.parseDouble(point1[0]);
        double x2=Double.parseDouble(point2[1]);
        double y2=Double.parseDouble(point2[0]);
        double x0=Double.parseDouble(point0[1]);
        double y0=Double.parseDouble(point0[0]);
        return (x1*y0+x0*y2+x2*y1)-(x0*y1+x2*y0+x1*y2);
    }

    double distHaversine(double[]p1,double[]p2){
        double radius = (6356.752+6378.137)/2;
        if (p1[0]>-23.5&&p1[0]<23.5&&p2[0]>-23.5&&p2[0]<23.5)
            radius = 6378.137;
        else if ((p1[0]<-66.5&&p2[0]<-66.5)||(p1[0]>66.5&&p2[0]>66.5)){
            radius = 6356.752;
        }
        double distLat = rad2deg(p2[0]-p1[0]);
        double distLon=rad2deg(p2[1]-p1[1]);
        double a =Math.sin(distLat/2)*Math.sin(distLat/2)+
                Math.cos(rad2deg(p1[0]))*Math.cos(rad2deg(p2[0]))
                        *Math.sin(distLon/2)*Math.sin(distLon/2);
        double c=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
        return radius*c;
    }

    double rad2deg(double ran){
        return ran*Math.PI/180;
    }
}
