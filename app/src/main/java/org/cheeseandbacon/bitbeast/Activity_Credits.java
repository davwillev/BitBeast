/* Copyright (c) 2017 Cheese and Bacon Games, LLC */
/* This file is licensed under the MIT License. */
/* See the file development/LICENSE.txt for the full license text. */

package org.cheeseandbacon.bitbeast;


import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class Activity_Credits extends AppCompatActivity {
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credits);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        Font.set_typeface(getAssets(), (TextView)findViewById(R.id.message_credits));
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
    	
    	Options.set_keep_screen_on(getWindow());
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    }
	@Override
    protected void onPause(){
    	super.onPause();
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    }
}
