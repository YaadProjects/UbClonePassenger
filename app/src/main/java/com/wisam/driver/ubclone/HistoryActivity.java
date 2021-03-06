package com.wisam.driver.ubclone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.wisam.driver.POJO.RequestsResponse;
import com.wisam.driver.events.LogoutRequest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    private RecyclerView historyEntriesRecyclerView;
    private HistoryEntriesAdapter historyEntriesAdapter;
    private RecyclerView.LayoutManager historyEntriesLayoutManager;
    private ArrayList<HistoryEntry> historyEntriesList;
    private PrefManager prefManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.history_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        prefManager = new PrefManager(this);
        
        historyEntriesRecyclerView = (RecyclerView) findViewById(R.id.history_rec_view);
        ArrayList<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();

        historyEntriesRecyclerView.setHasFixedSize(true);

        // Use linear layout manager
        historyEntriesLayoutManager = new LinearLayoutManager(this);
        historyEntriesRecyclerView.setLayoutManager(historyEntriesLayoutManager);

        // specify an adapter (See also next example)
        historyEntriesAdapter = new HistoryEntriesAdapter(this, historyEntries);
        historyEntriesRecyclerView.setAdapter(historyEntriesAdapter);

        String email = "";
        String password = "";
        if (prefManager.isLoggedIn()){
            email = prefManager.getUser().getEmail();
            password = prefManager.getUser().getPassword();
        } else {
            Log.i(TAG, "onCreate: User not logged in");
            prefManager.setIsLoggedIn(false);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.connecting));

        progressDialog.show();

        RestServiceConstants constants = new RestServiceConstants();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constants.getBaseUrl(HistoryActivity.this))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Server request

        RestService service = retrofit.create(RestService.class);
        Call<RequestsResponse> call = service.getRequests("Basic "+ Base64.encodeToString((email + ":" + password).getBytes(),Base64.NO_WRAP));
        Log.d(TAG, "onCreate: " + call.request().toString());
        call.enqueue(new Callback<RequestsResponse>() {
            @Override
            public void onResponse(Call<RequestsResponse> call, Response<RequestsResponse> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();

                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccessful() && response.body() != null){
                    List <HistoryEntry> rides = response.body().getRides();
                    List <HistoryEntry> history = new ArrayList<HistoryEntry>(){{}};
                    for (HistoryEntry entry : rides){
                        if (entry.getStatus().equals("completed") ||
                                entry.getStatus().equals("canceled")
//                                || entry.getStatus().equals("noDriver")
                                ) {
                            long unixTime;
                            Log.d(TAG,"Time is :" + entry.getTime());
                            unixTime = Long.valueOf(entry.getTime()) * 1000; // In this case, the server sends the date in seconds while unix date needs milliseconds

                            entry.setTime(String.valueOf(DateUtils.getRelativeTimeSpanString(unixTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
                            entry.setPrice(entry.getPrice() + " " + getString(R.string.currency));
                            history.add(0, entry);
                        }
                    }
                    HistoryActivity.this.setHistoryEntries(history);
                    if (rides.size() == 0) {
                        Toast.makeText(HistoryActivity.this, R.string.no_requests_found, Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 401){
                    Toast.makeText(HistoryActivity.this, R.string.please_login_again, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(HistoryActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    clearHistoryEntries();
                    Toast.makeText(HistoryActivity.this, R.string.unkown_error_occured, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<RequestsResponse> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(HistoryActivity.this, R.string.failed_to_connect_to_the_server, Toast.LENGTH_SHORT).show();




            }
        });

    }

    public void setHistoryEntries(List<HistoryEntry> historyEntries) {
        Log.i(TAG, "setTickets: Set");
//        if (historyEntries.isEmpty()) showNoTicketsIndicator();
//        else hideNoTicketsIndicator();
        if (historyEntries != null) {
            historyEntriesAdapter.updateDataSet((ArrayList<HistoryEntry>) historyEntries);
            historyEntriesAdapter.notifyDataSetChanged();
        }
//        swipeRefreshLayout.setRefreshing(false);
    }

    public void clearHistoryEntries(){
        Log.i(TAG, "clearTickets: Cleared first called");
        historyEntriesAdapter.clearDataSet();
//        showNoTicketsIndicator();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogoutRequest(LogoutRequest logoutRequest){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


}
