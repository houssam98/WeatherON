package ah.hathi.simpleweather;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;


public class CitiesActivity extends AppCompatActivity {
    private List<City> citiesList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ItemsAdapter mAdapter;
    private Realm realm;
    public FloatingActionButton add_button;
    Handler handler;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cities_list);

        handler = new Handler();

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        Realm.init(getApplicationContext());

        add_button = (FloatingActionButton) findViewById(R.id.add_button);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);


        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        mAdapter = new ItemsAdapter(citiesList);

        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());

        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setAdapter(mAdapter);

        enableSwipeToDeleteAndUndo();

        prepareShoppingItemData(citiesList);


        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               showInputDialog();
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                String city = citiesList.get(position).name;
                Intent goToNextActivity = new Intent(getApplicationContext(), WeatherActivity.class);
                goToNextActivity.putExtra("city", city);
                startActivity(goToNextActivity);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


    }


    private void updateWeatherData(final String city){
        new Thread(){
            public void run(){
                final JSONObject json = RemoteFetch.getJSON(getApplicationContext(), city);

                    handler.post(new Runnable(){
                        public void run(){
                            getCountry(json, city);
                            getTemperature(json, city);


                        }
                    });

            }
        }.start();
    }

    public void getCountry(JSONObject json, String city2){
        try {
            realm.beginTransaction();
            RealmResults<City> results = realm.where(City.class).equalTo("name", city2).findAll();
            for(City city : results){
                city.setCountry(json.getJSONObject("sys").getString("country").toLowerCase());
            }
            realm.commitTransaction();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void getTemperature(JSONObject json, String city2){
        try {
            JSONObject main = json.getJSONObject("main");
            realm.beginTransaction();
            RealmResults<City> results = realm.where(City.class).equalTo("name", city2).findAll();
            for(City city : results){
                city.setTemperature(String.format("%.0f", main.getDouble("temp")) + " ℃");
            }
            realm.commitTransaction();
        }catch(Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weather, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_delete:
                mAdapter.removeAll();
                prepareShoppingItemData(citiesList);
                break;

            case R.id.action_refresh:
                for(City city : citiesList){
                    updateWeatherData(city.getName());
                }
                prepareShoppingItemData(citiesList);
                Toast.makeText(getApplicationContext(), "Data refreshed!",
                        Toast.LENGTH_SHORT).show();
                break;

            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showInputDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add City");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                realm.beginTransaction();
                try{
                    City newCity = realm.createObject(City.class, input.getText().toString());
                    updateWeatherData(input.getText().toString());
                    realm.commitTransaction();
                    prepareShoppingItemData(citiesList);
                }catch(RealmPrimaryKeyConstraintException e){
                    Toast.makeText(getApplicationContext(), "City already exists!",
                            Toast.LENGTH_SHORT).show();
                    realm.commitTransaction();
                }
            }
        });
        builder.show();
    }


    private void prepareShoppingItemData(List<City> itemsList) {
        RealmResults<City> realmResult = realm.where(City.class).findAll();
        itemsList.clear();
        for(int i =0; i < realmResult.size(); i++){
            this.citiesList.add(0,realmResult.get(i));

        }
        mAdapter.notifyDataSetChanged();
        recyclerView.invalidate();
    }

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                final int position = viewHolder.getAdapterPosition();
                mAdapter.removeItem(position);

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }
}
