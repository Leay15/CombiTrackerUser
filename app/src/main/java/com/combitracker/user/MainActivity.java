package com.combitracker.user;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.app.AlertDialog;
import android.util.Log;
import android.view.SubMenu;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;


import com.combitracker.user.Objetos.Combi;
import com.combitracker.user.Objetos.Subrutas;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap gMap;
    boolean bus;
    private ArrayList<SubMenu> subMenus=new ArrayList<>();
    private Menu menuNavigation;
    //Referencias de firebase
    public FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    //Lista de combis
    ArrayList<Combi> listaCombis;
    private ArrayList<Marker> lstMarkers= new ArrayList<>();
    private String rutaS;
    private int rutaP;
    private Combi aux;

    //Posicion de usuario
    private double latU,lonU,latC,lonC;
    private Marker markUser,markAux=null;
    private Bitmap bmpN,bmp;


    //TRAZAR RUTA
    ArrayList<LatLng> coordenadas = new ArrayList<>();
    ArrayList<LatLng> cA = new ArrayList<>();

    ArrayList<Polyline> poli= new ArrayList<>();
    Polyline lineAux;
    CameraUpdate mPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase=FirebaseDatabase.getInstance();

        //Activar persistencia de datos
        try{
            firebaseDatabase.setPersistenceEnabled(true);
        }catch (Exception e){}


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        menuNavigation = navigationView.getMenu();



        //creando Bitmap para marker usuario
        bmpN= BitmapFactory.decodeResource(getResources(),R.drawable.user);
        bmpN=Bitmap.createScaledBitmap(bmpN, bmpN.getWidth()/30,bmpN.getHeight()/30, false);

        //Creando bitmap para marker combis
        bmp= BitmapFactory.decodeResource(getResources(),R.drawable.bus);
        bmp= Bitmap.createScaledBitmap(bmp, bmp.getWidth()/15,bmp.getHeight()/15, false);

        databaseReference=firebaseDatabase.getReference("Rutas");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int x=0;
                subMenus.clear();
                menuNavigation.clear();

                Subrutas sub;
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    Drawable icono=getResources().getDrawable(R.drawable.bus);
                    String ruta = ds.child("Nombre").getValue().toString();
                    SubMenu subMenu = menuNavigation.addSubMenu(ruta);
                    DataSnapshot subRutas= ds.child("Subrutas");
                    icono.mutate().setColorFilter( Color.parseColor(ds.child("Color").getValue().toString()), PorterDuff.Mode.SRC_IN);
                    for(DataSnapshot ds2:subRutas.getChildren()){

                        sub=ds2.getValue(Subrutas.class);
                        subMenu.add(x,subMenus.size(),0,sub.getRuta()).setIcon(icono);


                    }
                    subMenus.add(subMenu);
                    x++;


                }
                //databaseReference.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {



        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        limpiarCombis();
        rutaS=item.getTitle().toString();
        rutaP=item.getGroupId();
        obtenerRuta();
        seguirCombis();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void limpiarCombis() {

        for(int x=0;x<lstMarkers.size();x++){
            lstMarkers.get(x).remove();
        }
        lstMarkers.clear();
    }

    private void obtenerRuta() {


        //Obtener coordenadas para pintar la ruta seleccionada y dejar activo el listener en caso de actualizaciones

        DatabaseReference query=firebaseDatabase.getReference().child("Rutas").child(menuNavigation.getItem(rutaP)+"");//.child(rta);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {

                DataSnapshot color = snap.child("Color");
                String colorRuta = color.getValue().toString();
                snap = snap.child("Subrutas");

                Subrutas ruta;
                for(DataSnapshot ds:snap.getChildren()){
                    ruta=(ds.getValue(Subrutas.class));

                    if(ruta.getRuta().equalsIgnoreCase(rutaS)){
                        String camino[]=ruta.getCamino().split(",");
                        marcarRuta(camino,colorRuta);
                        break;
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }



    private void marcarRuta(String[] camino, String colorRuta) {


        limpiarRutaAnterior();

        PolylineOptions line= new PolylineOptions();

        for(int x=1;x<camino.length;x+=2){
            double lat = Double.parseDouble(camino[x-1]);
            double lng = Double.parseDouble(camino[x]);
            LatLng position = new LatLng(lat, lng);
            coordenadas.add(position);
        }
        if(!coordenadas.isEmpty()){
            coordenadas.add(coordenadas.get(0));
        }

        line.addAll(coordenadas);
        line.width(10);
        line.color(Color.parseColor(colorRuta));


        if(line!=null){
            lineAux=gMap.addPolyline(line);
        }






    }

    private void limpiarRutaAnterior() {

        if(lineAux!=null){

            lineAux.remove();
        }
        coordenadas.clear();



    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap=googleMap;

        if(gpsStatus()){
            miUbicacion();

        }else{
            AlertDialog.Builder alerta=new AlertDialog.Builder(this);
            alerta.setTitle("GPS Desactivado");
            alerta.setCancelable(false);
            alerta.setPositiveButton("Activar GPS", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                            miUbicacion();
                        }
                    });
            alerta.setNegativeButton("Cancelar", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            cerrarApp();
                        }
                    });
            alerta.create();
            alerta.show();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    private boolean gpsStatus() {
        ContentResolver content = getBaseContext().getContentResolver();
        boolean gps = Settings.Secure.isLocationProviderEnabled(content, LocationManager.NETWORK_PROVIDER);
        return gps;


    }

    private void seguirCombis() {

        //Obtener combis registradas en la ruta seleccionada

        final Query  refCombis=firebaseDatabase.getReference()
                .child("Rutas")
                .child(menuNavigation.getItem(rutaP)+"")
                .child("Combis");


        refCombis.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snap, String s) {
                String rutA = snap.child("rutaAsignada").getValue().toString();
                //aux= snap.getValue(Combi.class);
                if(snap!=null&&rutA.equalsIgnoreCase(rutaS)){
                    String lat=snap.child("lat").getValue().toString();
                    String lon=snap.child("lon").getValue().toString();
                    String num=snap.child("numero").getValue().toString();

                    agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,markAux);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snap, String s) {

                String rutA = snap.child("rutaAsignada").getValue().toString();
                String lat=snap.child("lat").getValue().toString();
                String lon=snap.child("lon").getValue().toString();
                String num=snap.child("numero").getValue().toString();

                agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,markAux);
                actualizarUbicacionC(rutA,lat,lon,num);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void actualizarUbicacionC(String rutA, String lat, String lon, String num) {
        for(int i=0;i<lstMarkers.size();i++){
            if(lstMarkers.get(i).getTitle().equalsIgnoreCase(num+"-"+rutA)){
                agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,lstMarkers.get(i));
                lstMarkers.remove(i);

                break;

            }
        }
    }

    private void miUbicacion() {

            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,1,location_listener);


            try
            {
                LatLng coordenadas = new LatLng(location.getLatitude(),location.getLongitude());
                mPosition = CameraUpdateFactory.newLatLngZoom(coordenadas, 14);
                if (markUser!= null) markUser.remove();
                markUser = gMap.addMarker(new MarkerOptions()
                        .position(coordenadas)
                        .title("Yo"));

                gMap.animateCamera(mPosition);
                Log.i("tags","dentro");

            }catch (Exception e){
            }




    }

    private LocationListener location_listener= new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            actualizarUbicacion(location);

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void actualizarUbicacion(Location location) {
        if (location != null) {
            latU = location.getLatitude();
            lonU = location.getLongitude();

                mPosition = CameraUpdateFactory.newLatLngZoom(new LatLng(latU,lonU), 14);
                gMap.moveCamera(mPosition);
                agregarMarcador(latU, lonU,"YO");



        }

    }

    private void agregarMarcador(double lat, double lon,String title) {
        LatLng coordenadas = new LatLng(lat, lon);
        if (markUser!= null) markUser.remove();
        markUser = gMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpN)));

    }

    private void agregarMarcadorC(double lat, double lon,String title, Marker mark) {
        LatLng coordenadas = new LatLng(lat, lon);

        if (mark!= null) mark.remove();
        mark = gMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp)));

        lstMarkers.add(mark);

    }

    void cerrarApp(){
        this.finish();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);

        if(requestCode==1) {
            Log.i("TAGH",requestCode+"");
            miUbicacion();
        }
    }
}
