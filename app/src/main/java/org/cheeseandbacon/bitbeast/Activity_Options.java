/* Copyright (c) 2017 Cheese and Bacon Games, LLC */
/* This file is licensed under the MIT License. */
/* See the file development/LICENSE.txt for the full license text. */

package org.cheeseandbacon.bitbeast;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Options extends AppCompatActivity {
	static final int DIALOG_ID_RESET=0;
	
	AlertDialog dialog_reset;

	private int dev_toggle_clicks;
	
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        setResult(RESULT_OK);
        
        showDialog(DIALOG_ID_RESET);
        dismissDialog(DIALOG_ID_RESET);
        
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_holiday));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_config));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_records));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_help));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_about));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_credits));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_reset));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_exit));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options_dev));
        Font.set_typeface(getAssets(), (TextView)dialog_reset.findViewById(R.id.dialog_options_reset_message));
        Font.set_typeface(getAssets(), (Button)dialog_reset.findViewById(R.id.button_dialog_options_reset_yes));
        Font.set_typeface(getAssets(), (Button)dialog_reset.findViewById(R.id.button_dialog_options_reset_no));
    }
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	Drawable_Manager.unbind_drawables(findViewById(R.id.root_options));
    	System.gc();
    }
	@Override
	protected Dialog onCreateDialog(int id){
		switch(id){
		case DIALOG_ID_RESET:
			LayoutInflater inflater=(LayoutInflater)getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        	View layout=inflater.inflate(R.layout.dialog_options_reset,(ViewGroup)findViewById(R.id.root_dialog_options_reset));
        	AlertDialog.Builder builder=new AlertDialog.Builder(this);
        	builder.setView(layout);
			builder.setCancelable(true);
			dialog_reset=builder.create();
			return dialog_reset;
		default:
			return null;
		}
	}
	@Override
    protected void onResume(){
    	super.onResume();

    	dev_toggle_clicks = 0;
    	
    	set_dialog_buttons();
    	
    	Options.set_keep_screen_on(getWindow());
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    	
    	Button tv=(Button)findViewById(R.id.button_options_holiday);
    	if(Holiday.get()==Holiday.NONE){
    		tv.setVisibility(TextView.GONE);
    	}
    	else{
    		tv.setVisibility(TextView.VISIBLE);
    		tv.setText(Holiday.get_string());
    		tv.setTextColor(Holiday.get_color(getResources()));
    	}

    	updateDevButton();
    }
	@Override
    protected void onPause(){
    	super.onPause();
    	
    	close_dialogs();
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    }

    private void updateDevButton () {
        Button button = findViewById(R.id.button_options_dev);
        if (Options.dev) {
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
        }
    }
	
	public void button_config(View view){
		Intent intent=new Intent(this,Activity_Config.class);
    	startActivity(intent);
    }
	public void button_records(View view){
		Intent intent=new Intent(this,Activity_Records.class);
    	startActivity(intent);
    }
	public void button_help(View view){
		Intent intent=new Intent(this,Activity_Help.class);
    	startActivity(intent);
    }
	public void button_about(View view){
		Intent intent=new Intent(this,Activity_About.class);
    	startActivity(intent);
    }
	public void button_credits(View view){
		Intent intent=new Intent(this,Activity_Credits.class);
    	startActivity(intent);
    }
	public void button_reset(View view){
    	showDialog(DIALOG_ID_RESET);
    }
	public void button_exit(View view){
    	setResult(RESULT_CANCELED);
    	this.finish();
    }
	
	public void close_dialogs(){
		if(dialog_reset.isShowing()){
			dismissDialog(DIALOG_ID_RESET);
	    }
	}
	
	public void set_dialog_buttons(){
		((Button)dialog_reset.findViewById(R.id.button_dialog_options_reset_yes)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				StorageManager.delete_pet_status(Activity_Options.this);
				
				if(dialog_reset.isShowing()){
					dismissDialog(DIALOG_ID_RESET);
			    }
				
				finish();
			}
		});
        ((Button)dialog_reset.findViewById(R.id.button_dialog_options_reset_no)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				if(dialog_reset.isShowing()){
					dismissDialog(DIALOG_ID_RESET);
			    }
			}
		});
	}

    public void button_dev_toggle (View view) {
	    if (++dev_toggle_clicks >= 5) {
	        dev_toggle_clicks = 0;

	        Options.dev = !Options.dev;

			StorageManager.save_options(this);

	        if (Options.dev) {
                Toast.makeText(this, getString(R.string.options_dev_on), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.options_dev_off), Toast.LENGTH_SHORT).show();
            }

	        updateDevButton();
        }
    }

	public void button_dev (View view) {
		startActivity(new Intent(this, Activity_Dev.class));
	}
}
