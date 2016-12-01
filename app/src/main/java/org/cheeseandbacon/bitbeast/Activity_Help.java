package org.cheeseandbacon.bitbeast;


import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Activity_Help extends Activity{
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        Font.set_typeface((Button)findViewById(R.id.button_help_how_to_play));
        Font.set_typeface((Button)findViewById(R.id.button_help_pet));
        Font.set_typeface((Button)findViewById(R.id.button_help_store));
        Font.set_typeface((Button)findViewById(R.id.button_help_play));
        Font.set_typeface((Button)findViewById(R.id.button_help_train));
        Font.set_typeface((Button)findViewById(R.id.button_help_battle));
        Font.set_typeface((Button)findViewById(R.id.button_help_speech_recognition));
        Font.set_typeface((Button)findViewById(R.id.button_help_app));
        Font.set_typeface((Button)findViewById(R.id.button_help_legal));
    }
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	Drawable_Manager.unbind_drawables(findViewById(R.id.root_help));
    	System.gc();
    }
	@Override
    protected void onResume(){
    	super.onResume();
    	
    	setRequestedOrientation(Options.get_orientation(true));
    	Options.set_keep_screen_on(getWindow());
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    }
	@Override
    protected void onPause(){
    	super.onPause();
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    }
	@Override
    public boolean onSearchRequested(){
    	String save_location=StorageManager.save_screenshot(this,findViewById(R.id.root_help));
    	
    	if(save_location.length()>0){
    		Toast.makeText(getApplicationContext(),"Screenshot saved to "+save_location+".",Toast.LENGTH_SHORT).show();
    	}
    	
    	return false;
    }
	
	public void button_help_how_to_play(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.HOW_TO_PLAY);
    	startActivity(intent);
    }
	public void button_help_pet(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.PET);
    	startActivity(intent);
    }
	public void button_help_store(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.STORE);
    	startActivity(intent);
    }
	public void button_help_play(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.PLAY);
    	startActivity(intent);
    }
	public void button_help_train(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.TRAIN);
    	startActivity(intent);
    }
	public void button_help_battle(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.BATTLE);
    	startActivity(intent);
    }
	public void button_help_speech_recognition(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.SPEECH_RECOGNITION);
    	startActivity(intent);
    }
	public void button_help_app(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.APP);
    	startActivity(intent);
    }
	public void button_help_legal(View view){
		Intent intent=new Intent(this,Activity_Help_Page.class);
		intent.putExtra(getPackageName()+".page",Help_Page.LEGAL);
    	startActivity(intent);
    }
}