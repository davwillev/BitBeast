package org.cheeseandbacon.bitbeast;


import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Credits extends Activity{
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credits);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        Font.set_typeface((TextView)findViewById(R.id.message_credits));
    }
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	Drawable_Manager.unbind_drawables(findViewById(R.id.root_credits));
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
    	String save_location=StorageManager.save_screenshot(this,findViewById(R.id.root_credits));
    	
    	if(save_location.length()>0){
    		Toast.makeText(getApplicationContext(),"Screenshot saved to "+save_location+".",Toast.LENGTH_SHORT).show();
    	}
    	
    	return false;
    }
}
