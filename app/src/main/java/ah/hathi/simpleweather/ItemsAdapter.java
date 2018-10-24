package ah.hathi.simpleweather;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import com.haipq.android.flagkit.FlagImageView;

import org.json.JSONObject;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static ah.hathi.simpleweather.WeatherFragment.city;


public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.MyViewHolder>{
    private List<City> itemsList;
    private Realm realm;


    public ItemsAdapter(List<City> itemsList) {
        this.itemsList = itemsList;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView city_name, temperature_list;
        FlagImageView flag_image;

        public MyViewHolder(View view) {
            super(view);
            city_name = (TextView) view.findViewById(R.id.city_name);
            temperature_list = (TextView) view.findViewById(R.id.temperature_list);
            flag_image = (FlagImageView) view.findViewById(R.id.flag_image);

        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cities_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        City item = itemsList.get(position);
        holder.city_name.setText(item.getName());
        holder.flag_image.setCountryCode(item.getCountry());
        holder.temperature_list.setText(item.getTemperature());
    }

    public void removeItem(int position) {

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);

        realm.beginTransaction();
        itemsList.get(position).deleteFromRealm();
        realm.commitTransaction();

        itemsList.remove(position);
        notifyItemRemoved(position);
    }

    public void removeAll(){
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        realm = Realm.getInstance(config);
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    public List<City> getData() {
        return itemsList;
    }

}
