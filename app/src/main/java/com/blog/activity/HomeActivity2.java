package com.blog.activity;

/**
 * Created by tosin on 8/5/2016.
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.blog.R;
import com.blog.adapter.ExpandableListAdapter;
import com.blog.adapter.ListingAdapter2;
import com.blog.helpers.TextUtilities;
import com.blog.model.Categories;
import com.blog.model.Wp;
import com.blog.singleton.RestClient;
import com.blog.widget.MaterialProgressBar;
import com.blog.widget.MaterialProgressBarWhite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class HomeActivity2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    /** Expandable List **/
    private ArrayList<String> groupList;
    private ArrayList<String> childList;

    private HashMap<String, List<String>> laptopCollection;
    private HashMap<Integer, List<Integer>> categoryIdList;
    private ArrayList<Integer> catList;
    private ExpandableListView expListView;
    private int lastExpandedPosition = -1;
    private int firstExpandedPosition = 0;

    ImageView closeButton;
    private RecyclerView view;
    private ArrayList<Wp> data = new ArrayList<Wp>();
    private ArrayList<Categories> categories = new ArrayList<Categories>();
    private SwipeRefreshLayout swipeContainer;
    LinearLayoutManager linearLayoutManager;
    ListingAdapter2 listingAdapter;
    private ArrayList<String> imagesData = new ArrayList<String>();
    ArrayList<String> imageUrlList =  new ArrayList<String>();


    private MaterialProgressBar progressBar;
    private MaterialProgressBarWhite categoriesProgressBar;

    private LinearLayout lostConnect;
    private LinearLayout noResult;

    int pastVisiblesItems, visibleItemCount, totalItemCount;
    boolean loading = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        progressBar = (MaterialProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        lostConnect = (LinearLayout) findViewById(R.id.no_connection);
        noResult = (LinearLayout) findViewById(R.id.no_result);

        categoriesProgressBar = (MaterialProgressBarWhite) findViewById(R.id.categories_progress_bar);
        categoriesProgressBar.setVisibility(View.VISIBLE);


        view = (RecyclerView) findViewById(R.id.product_recycler);
        linearLayoutManager = new LinearLayoutManager(view.getContext());
        view.setLayoutManager(linearLayoutManager);
        //view.setHasFixedSize(true);
        //view.setLayoutManager(new LinearLayoutManager(this));

        view.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {
                    visibleItemCount = linearLayoutManager.getChildCount();
                    totalItemCount = linearLayoutManager.getItemCount();
                    pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();

                    if (loading)
                    {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount)
                        {
                            loading = false;
                            //Log.v("...", "Last Item Wow !");
                            Toast.makeText(HomeActivity2.this, "Last Item Wow !", Toast.LENGTH_SHORT).show();
                            //Do pagination.. i.e. fetch new data
                            fetchPaginatedContent(totalItemCount * 2);
                        }
                    }
                }
            }
        });








        RestClient.getInstance().getPosts(new Callback<List<Wp>>() {
            @Override
            public void success(List<Wp> info, Response response) {
                data = new ArrayList<Wp>(info);

                for(int i=0; i<data.size(); i++) {
                    imageUrlList.add(data.get(i).getEmbedded().getWpFeaturedmedia().get(0).getMediaDetails().getSizes().getMedium().getSourceUrl());
                }

                if(imageUrlList.size() > 0) {
                    listingAdapter = new ListingAdapter2(HomeActivity2.this, data, view, imageUrlList);
                    view.setAdapter(listingAdapter);
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    noResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                progressBar.setVisibility(View.GONE);
                lostConnect.setVisibility(View.VISIBLE);
                //Toast.makeText(HomeActivity2.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                //Log.d("RetrofitError: ", error.getLocalizedMessage());
            }
        });















        initExpandableList();
        pullToRefresh();
    }


    public void pullToRefresh(){
        // Pull to refresh
        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                fetchTimelineAsync(0);
                initExpandableList();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public void fetchTimelineAsync(int page) {
        // Send the network request to fetch the updated data
        Toast.makeText(HomeActivity2.this, "Refreshing...", Toast.LENGTH_SHORT).show();
        imageUrlList.clear();

        RestClient.getInstance().getPosts(new Callback<List<Wp>>() {
            @Override
            public void success(List<Wp> info, Response response) {
                data = new ArrayList<Wp>(info);
                System.out.println("hello1 this works");
                for(int i=0; i<data.size(); i++) {
                    imageUrlList.add(data.get(i).getEmbedded().getWpFeaturedmedia().get(0).getMediaDetails().getSizes().getMedium().getSourceUrl());
                }

                if(imageUrlList.size() > 0) {
                    listingAdapter = new ListingAdapter2(HomeActivity2.this, data, view, imageUrlList);
                    listingAdapter.notifyDataSetChanged();
                    view.setAdapter(listingAdapter);
                    progressBar.setVisibility(View.GONE);
                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);
                } else {
                    progressBar.setVisibility(View.GONE);
                    noResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                progressBar.setVisibility(View.GONE);
                lostConnect.setVisibility(View.VISIBLE);
                //Toast.makeText(HomeActivity2.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                //Log.d("RetrofitError: ", error.getLocalizedMessage());
            }
        });







    }



    public void fetchPaginatedContent(int page) {
        // Send the network request to fetch the updated data
        Toast.makeText(HomeActivity2.this, "Refreshing...", Toast.LENGTH_SHORT).show();
        imageUrlList.clear();

        RestClient.getInstance().getPostPaginate(page, new Callback<List<Wp>>() {
            @Override
            public void success(List<Wp> info, Response response) {
                data = new ArrayList<Wp>(info);

                for (int i = 0; i < data.size(); i++) {
                    imageUrlList.add(data.get(i).getEmbedded().getWpFeaturedmedia().get(0).getMediaDetails().getSizes().getMedium().getSourceUrl());
                }

                if (imageUrlList.size() > 0) {
                    listingAdapter = new ListingAdapter2(HomeActivity2.this, data, view, imageUrlList);
                    //listingAdapter.notifyDataSetChanged();
                    //listingAdapter.notifyItemRangeChanged(0, listingAdapter.getItemCount());
                    listingAdapter.notifyItemInserted(data.size() - 1);


                    //view.setAdapter(listingAdapter);
                    progressBar.setVisibility(View.GONE);
                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);
                } else {
                    progressBar.setVisibility(View.GONE);
                    noResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                progressBar.setVisibility(View.GONE);
                lostConnect.setVisibility(View.VISIBLE);
                //Toast.makeText(HomeActivity2.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                //Log.d("RetrofitError: ", error.getLocalizedMessage());
            }
        });
    }


    private void initExpandableList(){
        createGroupList();
    }

    /** Expandable List View **/
    private void createGroupList() {
        groupList = new ArrayList<String>();
        final HashMap<Integer, String> subs= new HashMap<Integer, String>();
        laptopCollection = new LinkedHashMap<String, List<String>>();
        categoryIdList = new LinkedHashMap<Integer, List<Integer>>();

        RestClient.getInstance().getCategories(new Callback<List<Categories>>() {
            @Override
            public void success(List<Categories> info, Response response) {
                categories = new ArrayList<Categories>(info);
                categoriesProgressBar.setVisibility(View.GONE);

                //put the id, and name of categories into a Map to be iterated for parent ID
                for(int i=0; i<categories.size(); i++) {
                    subs.put(categories.get(i).getId().intValue(), categories.get(i).getName().toString());
                    if(categories.get(i).getParent().intValue() == 0) {
                        groupList.add(TextUtilities.toUpperCaseFirst(categories.get(i).getName().toString()));
                    }
                }

                //Iterate Map to extract the right parent ID for subcategories
                for (Map.Entry<Integer, String> entry : subs.entrySet()) {
                    int key = entry.getKey();
                    ArrayList<String> nameArr = new ArrayList<String>();
                    int k = 0;
                    for(int i=0; i<categories.size(); i++) {
                        if(categories.get(i).getParent().intValue() == key){
                            if(k == 0) {
                                nameArr.add(entry.getValue());
                                k = 1;
                            }
                            nameArr.add(categories.get(i).getName().toString());
                        }
                    }
                    String[] subCategory = nameArr.toArray(new String[nameArr.size()]);
//                    loadChild(subCategory);
                     childList = new ArrayList<String>();
                     for (String model : subCategory) {
                         childList.add(model);
                     }
                    laptopCollection.put(TextUtilities.toUpperCaseFirst(entry.getValue().toString()), childList);
                }


                //Iterate through the Grouplist of categories
                //Iterate through the Map to get Value stored and check against the Grouplist of categories
                //If found in lowercase, add the Category ID to categoryId List.
                int j = 0;
                for(String grlist : groupList){
                    for (Map.Entry<Integer, String> entry : subs.entrySet()) {
                        int key = entry.getKey();
                        if(entry.getValue().toLowerCase().equals(grlist.toLowerCase())){
                            catList = new ArrayList<Integer>();
                            int k = 0;
                            for(int i=0; i<categories.size(); i++) {
                                if(categories.get(i).getParent().intValue() == key){
                                    if(k == 0) {
                                        catList.add(categories.get(i).getParent().intValue());
                                        k = 1;
                                    }
                                    catList.add(categories.get(i).getId().intValue());
                                }
                            }
                            categoryIdList.put(j, catList);
                            j++;
                        }
                    }
                }



                expListView = (ExpandableListView) findViewById(R.id.menu_list);
                final ExpandableListAdapter expListAdapter = new ExpandableListAdapter(
                        HomeActivity2.this, groupList, laptopCollection, categoryIdList);
                expListView.setAdapter(expListAdapter);
                expListView.setFocusable(false);
                //expListView.expandGroup(0);
                if(groupList.size()!=0){
                    //expListView.expandGroup(0);
                }

                //setGroupIndicatorToRight();
                expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    public boolean onChildClick(ExpandableListView parent, View v,
                                                int groupPosition, int childPosition, long id) {

                        int cat = categoryIdList.get(groupPosition).get(childPosition);
                        final String selected = (String) expListAdapter.getChild(
                                groupPosition, childPosition);

                        //close drawer
                        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                        drawer.closeDrawer(GravityCompat.START);

                        Context context = v.getContext();
                        Intent intent = new Intent(context, CategoryActivity.class);
                        intent.putExtra("categoryId", cat);
                        intent.putExtra("categoryTitle", selected);
                        context.startActivity(intent);

                        Toast.makeText(getBaseContext(), selected, Toast.LENGTH_LONG)
                                .show();
                        return true;
                    }
                });

                expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                    @Override
                    public void onGroupExpand(int groupPosition) {

                        if (firstExpandedPosition == 0) {
                            lastExpandedPosition = firstExpandedPosition;
                            firstExpandedPosition = 99; //any random number apart from zero
                        }

                        if (lastExpandedPosition != -1
                                && groupPosition != lastExpandedPosition) {
                            expListView.collapseGroup(lastExpandedPosition);
                        }

                        lastExpandedPosition = groupPosition;
                    }
                });


            }

            @Override
            public void failure(RetrofitError error) {
                categoriesProgressBar.setVisibility(View.GONE);
                //Toast.makeText(HomeActivity2.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                //Log.d("RetrofitError: ", error.getLocalizedMessage());
            }
        });




    }

    private void loadChild(String[] laptopModels) {
        childList = new ArrayList<String>();
        for (String model : laptopModels)
            childList.add(model);
    }

    private void setGroupIndicatorToRight() {
        /* Get the screen width */
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;

        expListView.setIndicatorBounds(width - getDipsFromPixel(35), width
                - getDipsFromPixel(5));
    }

    // Convert pixel to dip
    public int getDipsFromPixel(float pixels) {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    public StringBuilder append(String[] split){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
            if (i != split.length - 1) {
                sb.append("\n\n");
            }
        }
        return sb;
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void myFancyMethod(View v) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.home, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
