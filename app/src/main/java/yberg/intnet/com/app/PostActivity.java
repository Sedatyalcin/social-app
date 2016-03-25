package yberg.intnet.com.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity implements
        PostDialog.OnFragmentInteractionListener,
        ProfileFragment.OnFragmentInteractionListener,
        AdapterView.OnItemClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayout postInfo;

    private Post receivedPost;
    private ArrayList<Post> mPost;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Post");

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        mPost = new ArrayList<>();
        receivedPost = (Post) getIntent().getSerializableExtra("post");
        mPost.add(receivedPost);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mSwipeRefreshLayout.setRefreshing(true);
                        updatePost();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
        );

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new CardAdapter(this, mPost, new CardAdapter.OnItemClickListener() {
            @Override
            public void onClick(final View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(PostActivity.this);
                DialogInterface.OnClickListener dialogClickListener;

                switch (v.getId()) {
                    case R.id.deletePostButton:
                        dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        StringRequest deletePostRequest = new StringRequest(Request.Method.POST, Database.DELETE_POST_URL, new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                System.out.println("deletePost response: " + response);
                                                try {
                                                    JSONObject jsonResponse = new JSONObject(response);
                                                    if (jsonResponse.getBoolean("success")) {
                                                        Snackbar.make(findViewById(R.id.base),
                                                                "Deleted post", Snackbar.LENGTH_LONG).show();
                                                        finish();
                                                    }
                                                    else {
                                                        Snackbar.make(findViewById(R.id.base),
                                                                jsonResponse.getString("message"), Snackbar.LENGTH_LONG).show();
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, Database.getErrorListener(findViewById(R.id.base))
                                        ) {
                                            @Override
                                            protected Map<String, String> getParams() throws AuthFailureError {
                                                Map<String, String> parameters = new HashMap<>();
                                                parameters.put("uid", "" + MainActivity.getUid());
                                                parameters.put("pid", "" + mPost.get(0).getPid());
                                                return parameters;
                                            }
                                        };
                                        deletePostRequest.setRetryPolicy(Database.getRetryPolicy());
                                        requestQueue.add(deletePostRequest);
                                        break;
                                }
                            }
                        };
                        builder.setMessage("Do you really want to delete this post?")
                                .setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("Cancel", dialogClickListener).show();
                        break;
                    case R.id.deleteCommentButton:
                        dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        StringRequest deleteCommentRequest = new StringRequest(Request.Method.POST, Database.DELETE_COMMENT_URL, new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                System.out.println("deleteComment response: " + response);
                                                try {
                                                    JSONObject jsonResponse = new JSONObject(response);
                                                    if (jsonResponse.getBoolean("success")) {
                                                        Snackbar.make(findViewById(R.id.base),
                                                                "Deleted comment", Snackbar.LENGTH_LONG).show();
                                                        updatePost();
                                                    }
                                                    else {
                                                        Snackbar.make(findViewById(R.id.base),
                                                                jsonResponse.getString("message"), Snackbar.LENGTH_LONG).show();
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, Database.getErrorListener(findViewById(R.id.base))
                                        ) {
                                            @Override
                                            protected Map<String, String> getParams() throws AuthFailureError {
                                                Map<String, String> parameters = new HashMap<>();
                                                parameters.put("uid", "" + MainActivity.getUid());
                                                parameters.put("cid", "" + mPost.get(0).getComments().get(
                                                        ((ViewGroup) mRecyclerView.findViewById(R.id.comments_section)).indexOfChild((View) v.getParent().getParent())
                                                ).getCid());

                                                System.out.println("index: " + ((ViewGroup) mRecyclerView.findViewById(R.id.comments_section)).indexOfChild((View)v.getParent().getParent()));
                                                System.out.println("cid: " + parameters.get("cid"));
                                                return parameters;
                                            }
                                        };
                                        deleteCommentRequest.setRetryPolicy(Database.getRetryPolicy());
                                        requestQueue.add(deleteCommentRequest);
                                        break;
                                }
                            }
                        };
                        builder = new AlertDialog.Builder(PostActivity.this);
                        builder.setMessage("Do you really want to delete this post?")
                                .setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("Cancel", dialogClickListener).show();
                        break;
                    case R.id.postInfo:
                        MainActivity.profileFragment = ProfileFragment.newInstance(mPost.get(0).getUser().getUid());
                        MainActivity.getMainFragmentManager().beginTransaction().replace(
                                R.id.fragment_view, MainActivity.profileFragment).commitAllowingStateLoss();
                        finish();
                        break;
                }
            }
        }, false);
        mRecyclerView.setAdapter(mAdapter);

        updatePost();
    }

    public void showCommentDialog(View v) {
        FragmentManager fm = getSupportFragmentManager();
        System.out.println("fm: " + fm);
        SharedPreferences prefs = getSharedPreferences("com.intnet.yberg", Context.MODE_PRIVATE);
        PostDialog postDialog = PostDialog.newInstance("Comment", "Commenting as",
                prefs.getString("username", ""), prefs.getString("name", ""));
        postDialog.show(fm, "fragment_post_dialog");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void updatePost() {
        StringRequest getPostRequest = new StringRequest(Request.Method.POST, Database.GET_POST_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("getPost response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        mPost.clear();
                        JSONObject post = jsonResponse.getJSONObject("post");
                        JSONObject user = post.getJSONObject("user");
                        JSONArray comments = post.getJSONArray("comments");
                        ArrayList<Comment> mComments = new ArrayList<>();
                        for (int i = 0; i < comments.length(); i++) {
                            JSONObject comment = comments.getJSONObject(i);
                            JSONObject usr = comment.getJSONObject("user");
                            mComments.add(new Comment(
                                    comment.getInt("cid"),
                                    new User(
                                            usr.getInt("uid"),
                                            usr.getString("username"),
                                            usr.getString("name"),
                                            usr.getString("image")
                                    ),
                                    comment.getString("text"),
                                    comment.getString("commented")
                            ));
                        }
                        System.out.println("user: " + user.getInt("uid"));
                        mPost.add(new Post(
                                post.getInt("pid"),
                                new User(
                                        user.getInt("uid"),
                                        user.getString("username"),
                                        user.getString("name"),
                                        user.getString("image")
                                ),
                                post.getString("text"),
                                post.getString("posted"),
                                post.getInt("numberOfComments"),
                                mComments,
                                post.getInt("upvotes"),
                                post.getInt("downvotes"),
                                post.getInt("voted"),
                                post.getString("image")
                        ));
                        mAdapter.notifyDataSetChanged();
                    }
                    else {
                        MainActivity.makeSnackbar(jsonResponse.getString("message"));
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        MainActivity.makeSnackbar(R.string.request_timeout);
                        finish();
                    }
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("pid", "" + mPost.get(0).getPid());
                parameters.put("uid", "" + MainActivity.getUid());
                return parameters;
            }
        };
        getPostRequest.setRetryPolicy(Database.getRetryPolicy());
        requestQueue.add(getPostRequest);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onDialogSubmit(final PostDialog dialog, final String text) {
        StringRequest addCommentRequest = new StringRequest(Request.Method.POST, Database.ADD_COMMENT_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("addComment response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        Snackbar.make(findViewById(R.id.base),
                                "Sent comment", Snackbar.LENGTH_LONG).show();
                        dialog.dismiss();
                        updatePost();
                    }
                    else {
                        dialog.setEnabled(true);
                        Snackbar.make(findViewById(R.id.base),
                                jsonResponse.getString("message"), Snackbar.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, Database.getErrorListener(findViewById(R.id.base))
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("pid", "" + mPost.get(0).getPid());
                parameters.put("uid", "" + MainActivity.getUid());
                parameters.put("text", text);
                return parameters;
            }
        };
        addCommentRequest.setRetryPolicy(Database.getRetryPolicy());
        requestQueue.add(addCommentRequest);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}