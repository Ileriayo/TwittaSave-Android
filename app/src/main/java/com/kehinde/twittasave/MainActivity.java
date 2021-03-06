package com.kehinde.twittasave;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.rxdownloader.RxDownloader;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;

import io.fabric.sdk.android.Fabric;
import retrofit2.Call;
import rx.Observer;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private Button btn_download;
    private TextView txt_tweet_url;
    private TextView txt_filename;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(Constant.TWITTER_KEY, Constant.TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));
        setContentView(R.layout.activity_main);

        sharedPreferences=this.getSharedPreferences("com.kehinde.twittasave", Context.MODE_PRIVATE);

        btn_download= (Button) findViewById(R.id.btn_download);
        txt_tweet_url= (TextView) findViewById(R.id.txt_tweet_url);
        txt_filename= (TextView) findViewById(R.id.txt_filename);

        if (!storageAllowed()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE);
        }

        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Fetching tweet...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);


        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSharedText(intent); // Handle text being sent
            }
        }


        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fname;

                if (txt_tweet_url.getText().length()>0 && txt_tweet_url.getText().toString().contains("twitter.com/")) {

                    Long id = getTweetId(txt_tweet_url.getText().toString());

                    if (txt_filename.getText().length()>0) {
                        fname = txt_filename.getText().toString().trim();
                    } else {
                        fname = String.valueOf(id);
                    }

                    if (id !=null) {
                        getTweet(id, fname);
                    }

                }else {
                    alertNoUrl();
                }
            }
        });

    }

    private void handleSharedText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            try {
                if (sharedText.split("\\ ").length > 1) {
                    txt_tweet_url.setText(sharedText.split("\\ ")[4]);
                } else {
                    txt_tweet_url.setText(sharedText.split("\\ ")[0]);
                }
            }catch (Exception e){
                Log.d("TAG", "handleSharedText: "+e);
            }
        }
    }

    public void getTweet(final Long id, final String fname){
        progressDialog.show();

        TwitterApiClient twitterApiClient=TwitterCore.getInstance().getApiClient();
        StatusesService statusesService=twitterApiClient.getStatusesService();
        Call<Tweet> tweetCall=statusesService.show(id,null,null,null);
        tweetCall.enqueue(new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {

                if (result.data.extendedEtities==null && result.data.entities.media==null){
                    alertNoMedia();
                }
                //Check if gif or mp4 present in the file
                else if (!(result.data.extendedEtities.media.get(0).type).equals("video") && !(result.data.extendedEtities.media.get(0).type).equals("animated_gif")){
                    alertNoVideo();
                }
                else {
                    String filename=fname;
                    String url;

                    //Set filename to gif or mp4
                    if ((result.data.extendedEtities.media.get(0).type).equals("video")) {
                        filename = filename + ".mp4";
                    }else {
                        filename = filename + ".gif";
                    }

                    int i=0;
                    url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                    while (!url.endsWith(".mp4")){
                        if(result.data.extendedEtities.media.get(0).videoInfo.variants.get(i)!=null) {
                            url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                            i += 1;
                        }
                    }

                    downloadVideo(url,filename);
                }
            }

            @Override
            public void failure(TwitterException exception) {
                progressDialog.hide();
                Toast.makeText(MainActivity.this, "Request Failed: Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void downloadVideo(String url,String fname) {

        if (fname.endsWith(".mp4")) {
            progressDialog.setMessage("Fetching video...");
        }else{
            progressDialog.setMessage("Fetching gif...");
        }

        //Check if External Storage permission js allowed
        if (!storageAllowed()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE);
            progressDialog.hide();
            Toast.makeText(this, "Kindly grant the request and try again", Toast.LENGTH_SHORT).show();
        }else {
            RxDownloader.getInstance(this)
                    .download(url,fname, "video/*") // url, filename, and mimeType
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {
                            Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
                            loadLikeDialog();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onNext(String s) {

                        }
                    });

            progressDialog.hide();
            Toast.makeText(this, "Download Started: Check Notification", Toast.LENGTH_LONG).show();
        }
    }

    private boolean storageAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

            return permission == PackageManager.PERMISSION_GRANTED;

        }

        return true;
    }

    private void alertNoVideo() {
        progressDialog.hide();
        Toast.makeText(this, "The url entered contains no video or gif file", Toast.LENGTH_LONG).show();
    }

    private void alertNoMedia() {
        progressDialog.hide();
        Toast.makeText(this, "The url entered contains no media file", Toast.LENGTH_LONG).show();
    }

    private void alertNoUrl() {
        Toast.makeText(MainActivity.this, "Enter a correct tweet url", Toast.LENGTH_LONG).show();
    }


    private Long getTweetId(String s) {
        try {
            String[] split = s.split("\\/");
            String id = split[5].split("\\?")[0];
            return Long.parseLong(id);
        }catch (Exception e){
            Log.d("TAG", "getTweetId: "+e.getLocalizedMessage());
            alertNoUrl();
            return null;
        }
    }

    private void loadLikeDialog(){

        if (sharedPreferences.getString(Constant.FIRSTRUN,"").equals("")){

            AlertDialog.Builder builder=new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setView(R.layout.like_layout)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("Like", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Intent intent=new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.kehinde.twittasave"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });

            AlertDialog alertDialog=builder.create();
            alertDialog.show();

            sharedPreferences.edit().putString(Constant.FIRSTRUN,"no").apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId()==R.id.about){
            startActivity(new Intent(this,AboutActivity.class));
        }
        if (item.getItemId()==R.id.web){
            Intent urlIntent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://twittasave.net"));
            urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(urlIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
