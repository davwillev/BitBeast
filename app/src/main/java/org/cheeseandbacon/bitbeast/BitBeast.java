/* Copyright (c) 2017 Cheese and Bacon Games, LLC */
/* This file is licensed under the MIT License. */
/* See the file development/LICENSE.txt for the full license text. */

package org.cheeseandbacon.bitbeast;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BitBeast extends Activity implements BitBeastDialogFragment.DialogViewCallback {
	private static final String TAG=BitBeast.class.getName();
	private static final int PERMISSION_REQUEST_RECORD_AUDIO = 0;

	static final String DIALOG_PROGRESS="dialogProgress";
	static final String DIALOG_TEMP="dialogTemp";
	static final String DIALOG_GAMES="dialogGames";
	static final String DIALOG_DIE="dialogDie";
	static final String DIALOG_BATTLE="dialogBattle";
	static final String DIALOG_STORE="dialogStore";
	
	static final int REQUEST_OPTIONS=1;
	static final int REQUEST_NAME=2;
	
	private Image image;
	GameView game_view;
	
	BitBeastDialogFragment dialog_progress;
	BitBeastDialogFragment dialog_store;
	BitBeastDialogFragment dialog_temp;
	BitBeastDialogFragment dialog_games;
	BitBeastDialogFragment dialog_battle;
	BitBeastDialogFragment dialog_die;
	
	SpeechRecognizer speech;
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
                
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        Sound_Manager.startup(this);
        Templates.startup();
        StorageManager.load_templates(this,getResources());
        Font.startup(getAssets());
        RNG.startup();
        Options.startup();

        StorageManager.checkForExternalSaveData(this);

        StorageManager.load_options(this);
        
        image=(Image)getLastNonConfigurationInstance();
        if(image==null){
        	image=new Image(this);
        }
        
        StorageManager.log_reset(this);
        StorageManager.error_log_reset(this);
        
        game_view=(GameView)findViewById(R.id.view_game);
        game_view.bitbeast=BitBeast.this;
        
        speech=null;
        
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_status));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_store));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_clean));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_bathe));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_temp));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_games));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_equipment));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_battle));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_light));
        Font.set_typeface(getAssets(), (Button)findViewById(R.id.button_options));
        
        Button b=null;
        
        b=(Button)findViewById(R.id.button_status);
        b.setNextFocusLeftId(R.id.button_options);
        
        b=(Button)findViewById(R.id.button_temp);
        b.setNextFocusRightId(R.id.button_games);
        
        b=(Button)findViewById(R.id.button_games);
        b.setNextFocusLeftId(R.id.button_temp);
        
        b=(Button)findViewById(R.id.button_options);
        b.setNextFocusRightId(R.id.button_status);
    }
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	Sound_Manager.cleanup();
    	Templates.cleanup();
    	Font.cleanup();
    	RNG.cleanup();
    	Options.cleanup();
    	
    	image=null;
    	game_view=null;
    	Drawable_Manager.unbind_drawables(findViewById(R.id.root_main));
    	System.gc();
    }
    @Override
    public Object onRetainNonConfigurationInstance(){
		return image;
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
    	super.onRestoreInstanceState(savedInstanceState);
    	
    	int game_mode=savedInstanceState.getInt("game_mode");
        set_game_mode(game_mode,savedInstanceState);
    }
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState){
    	savedInstanceState.putInt("game_mode",game_view.get_game_mode());
    	
    	if(game_view.get_game_mode()==Game_Mode.WORKOUT){
    		game_view.get_game_workout().save_game(savedInstanceState);
    	}
    	else if(game_view.get_game_mode()==Game_Mode.BRICKS){
    		game_view.get_game_bricks().save_game(savedInstanceState);
    	}
    	
    	super.onSaveInstanceState(savedInstanceState);
    }
    @Override  
    public boolean onCreateOptionsMenu(Menu menu){  
    	if(game_view.get_game_mode()==Game_Mode.PET){
    		button_options(game_view);
    	}
    	
    	return super.onCreateOptionsMenu(menu);  
    }
    @Override
    public void onBackPressed(){
    	if(game_view.get_game_mode()==Game_Mode.PET){
    		super.onBackPressed();
    	}
    	else if(game_view.get_game_mode()==Game_Mode.WORKOUT){
    		game_view.get_game_workout().end_game(-1,game_view.get_pet().get_status(),game_view);
    	}
    	else if(game_view.get_game_mode()==Game_Mode.BRICKS){
    		game_view.get_game_bricks().end_game(-1,game_view.get_pet().get_status(),game_view);
    	}
    }
    @Override
    protected void onResume(){
    	super.onResume();

		dismissDialogFragment(dialog_die);
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    	
    	//Reset the pet and records.
    	game_view.reset_pet();
    	game_view.reset_records();
    	
    	long ms_last_run=StorageManager.load_pet_status(BitBeast.this,(View)game_view,game_view.get_pet().get_status());
    	Options.set_keep_screen_on(getWindow());
    	
    	StorageManager.load_records(BitBeast.this,game_view.get_records());
    	
    	LinearLayout ll=(LinearLayout)findViewById(R.id.root_main);
    	if(game_view.get_pet().get_status().light){
    		ll.setBackgroundResource(R.drawable.background_blueprint);
    	}
    	else{
    		ll.setBackgroundResource(android.R.color.black);
    	}
    	
    	set_button_colors();
    	
    	game_view.reset_sprites(image);
    	
    	game_view.resume(handler,image,(Vibrator)getSystemService(VIBRATOR_SERVICE),ms_last_run);
        
        //If no name has been given, the pet is not started yet.
    	if(game_view.get_pet().get_status().name.length()==0){
    		Intent intent=new Intent(BitBeast.this,Activity_Name.class);
    		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        	startActivityForResult(intent,REQUEST_NAME);
    	}
    }
    @Override
    protected void onPause(){
    	super.onPause();
    	
    	close_dialogs();
    	
    	overridePendingTransition(R.anim.transition_in,R.anim.transition_out);
    	
    	game_view.set_want_speech(false);
    	stop_speech_recognition();
    	
    	game_view.pause();
    	
    	if(Options.vibrate){
    		Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
    		vibrator.cancel();
    	}
    	
    	//If a name has been given, the pet has been started.
    	if(game_view.get_pet().get_status().name.length()>0){
    		StorageManager.save_pet_status(this,game_view.get_pet().get_status());
    	}
    }
    
    @Override
    protected void onActivityResult(int get_request_code,int get_result_code,Intent get_data){
    	super.onActivityResult(get_request_code,get_result_code,get_data);
    	
    	switch(get_request_code){
    	case REQUEST_OPTIONS: case REQUEST_NAME:
    		if(get_result_code==RESULT_CANCELED){
    			finish();
    			return;
    		}
    		break;
    	}
    }
    
    public void set_button_colors(){
    	if(game_view!=null){
	        Button b=null;
	        
	        b=(Button)findViewById(R.id.button_status);
	        b.setTextColor(getResources().getColor(R.color.font));
	        
	        b=(Button)findViewById(R.id.button_store);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_clean);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_bathe);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_temp);
	        if(!Options.pause){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_games);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_equipment);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_battle);
	        if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD &&
	        		game_view.get_pet().get_status().get_energy()>=Pet_Status.ENERGY_LOSS_BATTLE){
	        	b.setTextColor(getResources().getColor(R.color.font));
	        }
	        else{
	        	b.setTextColor(getResources().getColor(R.color.font_grayed));
	        }
	        
	        b=(Button)findViewById(R.id.button_light);
	        b.setTextColor(getResources().getColor(R.color.font));
	        
	        b=(Button)findViewById(R.id.button_options);
	        b.setTextColor(getResources().getColor(R.color.font));
    	}
    }
    
    public void deny_egg(){
    	Sound_Manager.playSound(this, Sound.NO_STAT_POINTS);
    	Toast.makeText(getApplicationContext(),game_view.get_pet().get_status().name+" is still just an egg!",Toast.LENGTH_SHORT).show();
    }
    public void deny_dead(){
    	Sound_Manager.playSound(this, Sound.NO_STAT_POINTS);
    	Toast.makeText(getApplicationContext(),game_view.get_pet().get_status().name+" is, unfortunately, dead!",Toast.LENGTH_SHORT).show();
    }
    public void deny_paused(){
    	Sound_Manager.playSound(this, Sound.NO_STAT_POINTS);
		Toast.makeText(getApplicationContext(),"The game is paused!",Toast.LENGTH_SHORT).show();
    }
    
    public void button_status(View view){
    	StorageManager.save_pet_status(this,game_view.get_pet().get_status());
    	
    	Intent intent=new Intent(this,Activity_Status.class);
    	startActivity(intent);
    }
    public void button_store(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
			dialog_store = showDialogFragment(DIALOG_STORE, BitBeastDialogFragment.DIALOG_TYPE_ALERT, R.layout.dialog_main_store, "");
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
    		deny_dead();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_clean(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG){
			if(game_view.get_pet().overlays.size()<Overlay.OVERLAY_LIMIT){
				if(image!=null){
					game_view.get_pet().add_overlay(image,(View)game_view,(Vibrator)getSystemService(VIBRATOR_SERVICE),Overlay_Type.CLEAN_YARD,game_view.getWidth(),0,Direction.LEFT);
				}
			}
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_bathe(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
	    	if(game_view.get_pet().overlays.size()<Overlay.OVERLAY_LIMIT){
	    		if(image!=null){
	    			game_view.get_pet().add_overlay(image,(View)game_view,(Vibrator)getSystemService(VIBRATOR_SERVICE),Overlay_Type.CLEAN_PET,0,0-image.overlay_clean_pet.bitmap.getHeight(),Direction.DOWN);
	    		}
    		}
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
    		deny_dead();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_temp(View view){
    	if(!Options.pause){
			dialog_temp = showDialogFragment(DIALOG_TEMP, BitBeastDialogFragment.DIALOG_TYPE_ALERT, R.layout.dialog_main_temp, "");
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_games(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
			dialog_games = showDialogFragment(DIALOG_GAMES, BitBeastDialogFragment.DIALOG_TYPE_ALERT, R.layout.dialog_main_games, "");
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
    		deny_dead();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_equipment(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
    		StorageManager.save_pet_status(this,game_view.get_pet().get_status());
	    	
	    	Intent intent=new Intent(this,Activity_Equip.class);

	    	startActivity(intent);
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
    		deny_dead();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_battle(View view){
    	if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
    		if(game_view.get_pet().get_status().get_energy()>=Pet_Status.ENERGY_LOSS_BATTLE){
				dialog_battle = showDialogFragment(DIALOG_BATTLE, BitBeastDialogFragment.DIALOG_TYPE_ALERT, R.layout.dialog_main_battle, "");
    		}
    		else{
    			Sound_Manager.playSound(this, Sound.NO_STAT_POINTS);
    			int energy_short=Pet_Status.ENERGY_LOSS_BATTLE-game_view.get_pet().get_status().get_energy();
    			Toast.makeText(getApplicationContext(),game_view.get_pet().get_status().name+" needs "+energy_short+" more energy to battle!",Toast.LENGTH_SHORT).show();
    		}
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
    		deny_egg();
    	}
    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
    		deny_dead();
    	}
    	else if(Options.pause){
    		deny_paused();
    	}
    }
    public void button_light(View view){
    	Sound_Manager.playSound(this, Sound.TOGGLE_LIGHT);
    	
    	//Toggle the light.
    	game_view.get_pet().get_status().light=!game_view.get_pet().get_status().light;
    	
    	LinearLayout ll=(LinearLayout)findViewById(R.id.root_main);
    	if(game_view.get_pet().get_status().light){
    		ll.setBackgroundResource(R.drawable.background_blueprint);
    	}
    	else{
    		ll.setBackgroundResource(android.R.color.black);
    	}
    }
    public void button_options(View view){
    	StorageManager.save_pet_status(this,game_view.get_pet().get_status());
    	
    	Intent intent=new Intent(this,Activity_Options.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	startActivityForResult(intent,REQUEST_OPTIONS);
    }

	public void start_battle_menu_wifi(){
		game_view.get_pet().get_status().sleeping_wake_up();

		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Battle_Menu_Wifi.class);
		startActivity(intent);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_RECORD_AUDIO:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					start_speech_recognition();
				}
				else{
					Log.d(TAG, "RECORD_AUDIO permission not granted");
				}
				break;
		}
	}
    
    public void start_speech_recognition(){
    	if(speech==null){
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
				speech = SpeechRecognizer.createSpeechRecognizer(this);
				speech.setRecognitionListener(listener);

				Intent intent = new Intent();
				intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

				speech.startListening(intent);

				game_view.set_speech_listening(true);
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
			}
    	}
    }
    
    public void stop_speech_recognition(){
    	if(speech!=null){
    		speech.stopListening();
    		speech.destroy();
    		speech=null;
    		
    		game_view.set_speech_listening(false);
    	}
    }
    
    private RecognitionListener listener=new RecognitionListener(){
    	@Override
    	public void onBeginningOfSpeech(){
    		
    	}
    	
    	@Override
    	public void onBufferReceived(byte[] buffer){
    		
    	}
    	
    	@Override
    	public void onEndOfSpeech(){
    		
    	}
    	
    	@Override
    	public void onError(int error){
    		//If there is a problem, force speech recognition to stop.
    		//This is to prevent speech recognition from turning on and off over and over.
    		if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
    			Toast.makeText(getApplicationContext(),"Speech recognition failed due to insufficient permissions.",Toast.LENGTH_SHORT).show();
    			
    			game_view.set_want_speech(false);
    		}
    		else if(error==SpeechRecognizer.ERROR_NETWORK){
    			Toast.makeText(getApplicationContext(),"Speech recognition failed due to network error. Does your device have a network connection?",Toast.LENGTH_SHORT).show();
    			
    			game_view.set_want_speech(false);
    		}
    		else if(error==SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
    			Toast.makeText(getApplicationContext(),"Speech recognition failed due to network timeout.",Toast.LENGTH_SHORT).show();
    			
    			game_view.set_want_speech(false);
    		}
    		else if(error==SpeechRecognizer.ERROR_SERVER){
    			Toast.makeText(getApplicationContext(),"Speech recognition failed due to server error.",Toast.LENGTH_SHORT).show();
    			
    			game_view.set_want_speech(false);
    		}
    		
    		stop_speech_recognition();
    	}
    	
    	@Override
    	public void onEvent(int event_type,Bundle params){
    		
    	}
    	
    	@Override
    	public void onPartialResults(Bundle partial_results){
    		
    	}
    	
    	@Override
    	public void onReadyForSpeech(Bundle params){
    		
    	}
    	
    	@Override
    	public void onResults(Bundle results){
    		process_speech(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
    		
    		stop_speech_recognition();
    	}
    	
    	@Override
    	public void onRmsChanged(float rmsdB){
    		
    	}
    };
    
    public void process_speech(ArrayList<String> speech_list){
    	//The index into the speech list of the "correct" speech.
    	int correct_speech=-1;
    	int speech_command=Speech.NONE;
    	
    	//Check for a speech item matching a command.
		for(int i=0;i<speech_list.size();i++){
			String speech=speech_list.get(i).trim().toLowerCase();
		
			int get_speech_command=Speech.process(speech,game_view.get_pet().get_status().name);
			
			if(get_speech_command!=Speech.NONE){
				correct_speech=i;
				speech_command=get_speech_command;
				break;
			}
		}
		
		//If none of the speech items match a command.
		if(correct_speech==-1 && speech_list.size()>0){
			correct_speech=0;
		}
		
		if(correct_speech>=0){
			String speech=speech_list.get(correct_speech).trim().toLowerCase();
			speech=Strings.first_letter_capital(speech);
			
			Toast.makeText(getApplicationContext(),"\""+speech+"\"",Toast.LENGTH_SHORT).show();
			
			handle_speech_command(speech_command);
		}
    }
    
    public void handle_speech_command(int speech_command){
    	if(speech_command==Speech.HAPPY){
    		game_view.get_pet().get_status().sleeping_wake_up();
    		
    		game_view.get_pet().get_status().queue_thought(Thought_Type.HAPPY);
		}
		else if(speech_command==Speech.STATUS){
			Button b=(Button)findViewById(R.id.button_status);
			button_status(b);
		}
		else if(speech_command==Speech.STORE){
			Button b=(Button)findViewById(R.id.button_store);
			button_store(b);
		}
		else if(speech_command==Speech.CLEAN){
			Button b=(Button)findViewById(R.id.button_clean);
			button_clean(b);
		}
		else if(speech_command==Speech.BATHE){
			Button b=(Button)findViewById(R.id.button_bathe);
			button_bathe(b);
		}
		else if(speech_command==Speech.TEMPERATURE){
			Button b=(Button)findViewById(R.id.button_temp);
			button_temp(b);
		}
		else if(speech_command==Speech.GAMES){
			Button b=(Button)findViewById(R.id.button_games);
			button_games(b);
		}
		else if(speech_command==Speech.EQUIPMENT){
			Button b=(Button)findViewById(R.id.button_equipment);
			button_equipment(b);
		}
		else if(speech_command==Speech.BATTLE_WIFI){
			dialogButtonBattleWifiDirect();
		}
		else if(speech_command==Speech.LIGHTS){
			Button b=(Button)findViewById(R.id.button_light);
			button_light(b);
		}
		else if(speech_command==Speech.OPTIONS){
			Button b=(Button)findViewById(R.id.button_options);
			button_options(b);
		}
		else if(speech_command==Speech.STORE_FOOD){
			dialogButtonStoreFood();
		}
		else if(speech_command==Speech.STORE_DRINKS){
			dialogButtonStoreDrinks();
		}
		else if(speech_command==Speech.STORE_TREATMENTS){
			dialogButtonStoreTreatments();
		}
		else if(speech_command==Speech.STORE_PERMA){
			dialogButtonStorePerma();
		}
		else if(speech_command==Speech.AC){
			dialogButtonTempAc();
		}
		else if(speech_command==Speech.HEATER){
			dialogButtonTempHeater();
		}
		else if(speech_command==Speech.NO_TEMP){
			dialogButtonTempNone();
		}
		else if(speech_command==Speech.BRICKS){
			dialogButtonGameBricks();
		}
		else if(speech_command==Speech.RPS){
			dialogButtonGameRps();
		}
		else if(speech_command==Speech.ACCEL){
			dialogButtonGameAccel();
		}
		else if(speech_command==Speech.GPS){
			dialogButtonGameGps();
		}
		else if(speech_command==Speech.SPEED_GPS){
			dialogButtonGameSpeedGps();
		}
		else if(speech_command==Speech.TRAINING){
			dialogButtonGameTraining();
		}
		else if(speech_command==Speech.SAD){
			game_view.get_pet().get_status().sleeping_wake_up();
			
    		game_view.get_pet().get_status().queue_thought(Thought_Type.SAD);
		}
		else if(speech_command==Speech.MUSIC){
			game_view.get_pet().get_status().sleeping_wake_up();
			
    		game_view.get_pet().get_status().queue_thought(Thought_Type.MUSIC);
		}
		else if(speech_command==Speech.NEEDS){
			game_view.get_pet().get_status().sleeping_wake_up();
			
    		game_view.get_pet().clear_need_feedback_timers();
    		
    		if(!game_view.get_pet().get_status().sick && !game_view.get_pet().get_status().needs_bath() && !game_view.get_pet().get_status().is_starving() &&
    				!game_view.get_pet().get_status().is_very_thirsty() && !game_view.get_pet().get_status().temp_is_cold() && !game_view.get_pet().get_status().temp_is_hot() &&
    				game_view.get_pet().get_status().is_happy() && !game_view.get_pet().get_status().needs_poop_cleaned()){
    			game_view.get_pet().get_status().queue_thought(Thought_Type.HAPPY);
    		}
		}
		else if(speech_command==Speech.BATTLE_SHADOW){
			dialogButtonBattleShadow();
		}
    }
    
    public void set_game_mode(int get_game_mode,Bundle bundle){
    	game_view.set_want_speech(false);
    	stop_speech_recognition();
    	
    	if(bundle==null){
	    	bundle=new Bundle();
	    	bundle.putBoolean("null",true);
    	}
    	else{
    		bundle.putBoolean("null",false);
    	}
    	
		if(get_game_mode==Game_Mode.PET){
			Message msg=handler.obtainMessage();
			msg.what=BitBeast.HANDLER_SHOW_MAIN_BUTTONS;
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
		else if(get_game_mode==Game_Mode.WORKOUT){
			Message msg=handler.obtainMessage();
			msg.what=BitBeast.HANDLER_START_GAME_WORKOUT;
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
		else if(get_game_mode==Game_Mode.BRICKS){
			Message msg=handler.obtainMessage();
			msg.what=BitBeast.HANDLER_START_GAME_BRICKS;
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
    
    static final int HANDLER_SHOW_PROGRESS=0;
    static final int HANDLER_HIDE_PROGRESS=1;
    static final int HANDLER_START_GAME_WORKOUT=2;
    static final int HANDLER_SHOW_MAIN_BUTTONS=3;
    static final int HANDLER_START_GAME_BRICKS=4;
    static final int HANDLER_DIE=5;
    static final int HANDLER_SPEECH_RECOGNITION=6;
    static final int HANDLER_REWARDS=7;
    static final int HANDLER_SET_BUTTON_COLORS=8;
    
    private Handler handler=new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		LinearLayout ll=null;
    		TextView tv=null;
    		
    		switch(msg.what){
    		case HANDLER_SHOW_PROGRESS:
    			if(!isFinishing()){
					dialog_progress = showDialogFragment(DIALOG_PROGRESS, BitBeastDialogFragment.DIALOG_TYPE_PROGRESS, 0, "Updating pet...");
			    }
    			break;
    		case HANDLER_HIDE_PROGRESS:
    			if(!isFinishing()){
					dismissDialogFragment(dialog_progress);
			    }
    			break;
    		case HANDLER_REWARDS:
    			if(!isFinishing()){
    				if(game_view!=null){
    					Bundle bundle=msg.getData();
    					int sound=bundle.getInt(getPackageName()+"sound");
    					bundle.remove(getPackageName()+"sound");
    					
    					if(sound!=-1){
    						Sound_Manager.playSound(BitBeast.this, sound);
    					}
	    		    	
	    		    	Intent intent=new Intent(BitBeast.this,Activity_Rewards.class);
	    		    	intent.putExtras(bundle);
	    		    	startActivity(intent);
    				}
    			}
    			break;
    		case HANDLER_START_GAME_WORKOUT:
    			if(!isFinishing()){
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_1);
	    			if(ll!=null){
	    				ll.setVisibility(View.GONE);
	    			}
	    			
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_2);
	    			if(ll!=null){
	    				ll.setVisibility(View.GONE);
	    			}
	    			    			
	    			if(game_view!=null){
	    				game_view.setFocusable(true);
	    				game_view.requestFocus();
	    				
	    				game_view.new_game_mode=Game_Mode.WORKOUT;
	    				game_view.game_mode_bundle=msg.getData();
	    			}
    			}
    			break;
    		case HANDLER_SHOW_MAIN_BUTTONS:
    			if(!isFinishing()){
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_1);
	    			if(ll!=null){
	    				ll.setVisibility(View.VISIBLE);
	    			}
	    			
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_2);
	    			if(ll!=null){
	    				ll.setVisibility(View.VISIBLE);
	    			}
	    			
	    			if(game_view!=null){
	    				game_view.new_game_mode=Game_Mode.PET;
	    				game_view.game_mode_bundle=msg.getData();
	    			}
    			}
    			break;
    		case HANDLER_START_GAME_BRICKS:
    			if(!isFinishing()){
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_1);
	    			if(ll!=null){
	    				ll.setVisibility(View.GONE);
	    			}
	    			
	    			ll=(LinearLayout)findViewById(R.id.view_main_buttons_2);
	    			if(ll!=null){
	    				ll.setVisibility(View.GONE);
	    			}
	    			    			
	    			if(game_view!=null){
	    				game_view.setFocusable(true);
	    				game_view.requestFocus();
	    				
	    				game_view.new_game_mode=Game_Mode.BRICKS;
	    				game_view.game_mode_bundle=msg.getData();
	    			}
    			}
    			break;
    		case HANDLER_DIE:
    			if(!isFinishing()){
    				Sound_Manager.playSound(BitBeast.this, Sound.DIE);
    				
    				String name=msg.getData().getString("name");
    				
    	        	String message="";
    	        	
    	        	message+=name+" has died!";

					dialog_die = showDialogFragment(DIALOG_DIE, BitBeastDialogFragment.DIALOG_TYPE_ALERT, R.layout.dialog_main_die, message);
			    }
    			break;
    		case HANDLER_SPEECH_RECOGNITION:
    			if(!isFinishing()){
    				if(speech!=null){
    					stop_speech_recognition();
    				}
    				else{
    					if(game_view!=null){
		    				if(!Options.pause && game_view.get_pet().get_status().age_tier!=Age_Tier.EGG && game_view.get_pet().get_status().age_tier!=Age_Tier.DEAD){
		    					if(SpeechRecognizer.isRecognitionAvailable(BitBeast.this)){
		    		            	start_speech_recognition();
		    		            }
		    		            else{
		    		            	Toast.makeText(getApplicationContext(),"Your device doesn't seem to support speech recognition!",Toast.LENGTH_SHORT).show();
		    		            	game_view.set_want_speech(false);
		    		            }
		    		    	}
		    		    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.EGG){
		    		    		deny_egg();
		    		    		game_view.set_want_speech(false);
		    		    	}
		    		    	else if(game_view.get_pet().get_status().age_tier==Age_Tier.DEAD){
		    		    		deny_dead();
		    		    		game_view.set_want_speech(false);
		    		    	}
		    		    	else if(Options.pause){
		    		    		deny_paused();
		    		    		game_view.set_want_speech(false);
		    		    	}
    					}
    				}
    			}
    			break;
    		case HANDLER_SET_BUTTON_COLORS:
    			if(!isFinishing()){
	    			set_button_colors();
    			}
    			break;
    		}
 	   	}
	};

	public void dialogButtonStoreFood () {
		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Store.class);

		Bundle bundle=new Bundle();
		bundle.putInt(getPackageName()+"section",Store_Section.FOOD);
		bundle.putInt(getPackageName()+"move_direction",Direction.NONE);

		intent.putExtras(bundle);
		startActivity(intent);

		dismissDialogFragment(dialog_store);
	}

	public void dialogButtonStoreDrinks () {
		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Store.class);

		Bundle bundle=new Bundle();
		bundle.putInt(getPackageName()+"section",Store_Section.DRINKS);
		bundle.putInt(getPackageName()+"move_direction",Direction.NONE);

		intent.putExtras(bundle);
		startActivity(intent);

		dismissDialogFragment(dialog_store);
	}

	public void dialogButtonStoreTreatments () {
		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Store.class);

		Bundle bundle=new Bundle();
		bundle.putInt(getPackageName()+"section",Store_Section.TREATMENTS);
		bundle.putInt(getPackageName()+"move_direction",Direction.NONE);

		intent.putExtras(bundle);
		startActivity(intent);

		dismissDialogFragment(dialog_store);
	}

	public void dialogButtonStorePerma () {
		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Store.class);

		Bundle bundle=new Bundle();
		bundle.putInt(getPackageName()+"section",Store_Section.PERMA);
		bundle.putInt(getPackageName()+"move_direction",Direction.NONE);

		intent.putExtras(bundle);
		startActivity(intent);

		dismissDialogFragment(dialog_store);
	}

	public void dialogButtonTempAc () {
		Sound_Manager.playSound(this, Sound.AC);

		game_view.get_pet().get_status().ac=true;
		game_view.get_pet().get_status().heater=false;

		dismissDialogFragment(dialog_temp);
	}

	public void dialogButtonTempHeater () {
		Sound_Manager.playSound(this, Sound.HEATER);

		game_view.get_pet().get_status().ac=false;
		game_view.get_pet().get_status().heater=true;

		dismissDialogFragment(dialog_temp);
	}

	public void dialogButtonTempNone () {
		game_view.get_pet().get_status().ac=false;
		game_view.get_pet().get_status().heater=false;

		dismissDialogFragment(dialog_temp);
	}

	public void dialogButtonGameTraining () {
		if(game_view.get_pet().get_status().get_energy()>=Pet_Status.ENERGY_LOSS_WORKOUT){
			game_view.get_pet().get_status().sleeping_wake_up();

			set_game_mode(Game_Mode.WORKOUT,null);

			dismissDialogFragment(dialog_games);
		}
		else{
			Sound_Manager.playSound(this, Sound.NO_STAT_POINTS);
			int energy_short=Pet_Status.ENERGY_LOSS_WORKOUT-game_view.get_pet().get_status().get_energy();
			Toast.makeText(getApplicationContext(),game_view.get_pet().get_status().name+" needs "+energy_short+" more energy to train!",Toast.LENGTH_SHORT).show();
		}
	}

	public void dialogButtonGameBricks () {
		game_view.get_pet().get_status().sleeping_wake_up();

		set_game_mode(Game_Mode.BRICKS,null);

		dismissDialogFragment(dialog_games);
	}

	public void dialogButtonGameRps () {
		game_view.get_pet().get_status().sleeping_wake_up();

		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Intent intent=new Intent(BitBeast.this,Activity_Game_RPS.class);
		startActivity(intent);

		dismissDialogFragment(dialog_games);
	}

	public void dialogButtonGameAccel () {
		SensorManager sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> sensor_list=sensors.getSensorList(Sensor.TYPE_ACCELEROMETER);

		if(!sensor_list.isEmpty()){
			game_view.get_pet().get_status().sleeping_wake_up();

			StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

			Intent intent=new Intent(BitBeast.this,Activity_Game_Accel.class);
			startActivity(intent);

			dismissDialogFragment(dialog_games);
		}
		else{
			Toast.makeText(getApplicationContext(),"Your device doesn't seem to have an accelerometer!",Toast.LENGTH_SHORT).show();
		}
	}

	public void dialogButtonGameGps () {
		LocationManager locations=(LocationManager)getSystemService(LOCATION_SERVICE);
		List<String> provider_list=locations.getProviders(false);

		if(provider_list.contains(LocationManager.GPS_PROVIDER)){
			game_view.get_pet().get_status().sleeping_wake_up();

			StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

			Intent intent=new Intent(BitBeast.this,Activity_Game_GPS.class);
			startActivity(intent);

			dismissDialogFragment(dialog_games);
		}
		else{
			Toast.makeText(getApplicationContext(),"Your device doesn't seem to have a GPS receiver!",Toast.LENGTH_SHORT).show();
		}
	}

	public void dialogButtonGameSpeedGps () {
		LocationManager locations=(LocationManager)getSystemService(LOCATION_SERVICE);
		List<String> provider_list=locations.getProviders(false);

		if(provider_list.contains(LocationManager.GPS_PROVIDER)){
			game_view.get_pet().get_status().sleeping_wake_up();

			StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

			Intent intent=new Intent(BitBeast.this,Activity_Game_Speed_GPS.class);
			startActivity(intent);

			dismissDialogFragment(dialog_games);
		}
		else{
			Toast.makeText(getApplicationContext(),"Your device doesn't seem to have a GPS receiver!",Toast.LENGTH_SHORT).show();
		}
	}

	public void dialogButtonDie () {
		dismissDialogFragment(dialog_die);
	}

	public void dialogButtonBattleShadow () {
		game_view.get_pet().get_status().sleeping_wake_up();

		StorageManager.save_pet_status(BitBeast.this,game_view.get_pet().get_status());

		Pet_Status them=new Pet_Status();

		//Randomly determine which perma items the shadow has.
		for(int i=0;i<game_view.get_pet().get_status().perma_items.size();i++){
			if(RNG.random_range(0,99)<65){
				them.perma_items.add(new Perma_Item(null,null,game_view.get_pet().get_status().perma_items.get(i).name,0.0f,0.0f));
			}
		}

		//Randomly determine which food buffs the shadow has.
		if(game_view.get_pet().get_status().buff_energy_max>0 && RNG.random_range(0,99)<65){
			them.buff_energy_max=1;
		}
		if(game_view.get_pet().get_status().buff_strength_max>0 && RNG.random_range(0,99)<65){
			them.buff_strength_max=1;
		}
		if(game_view.get_pet().get_status().buff_dexterity_max>0 && RNG.random_range(0,99)<65){
			them.buff_dexterity_max=1;
		}
		if(game_view.get_pet().get_status().buff_stamina_max>0 && RNG.random_range(0,99)<65){
			them.buff_stamina_max=1;
		}
		if(game_view.get_pet().get_status().buff_magic_find>0 && RNG.random_range(0,99)<65){
			them.buff_magic_find=1;
		}

		//Set the shadow's age tier to +/- 1 from the pet's age tier.
		int age_change=RNG.random_range(0,99);
		them.age_tier=game_view.get_pet().get_status().age_tier;
		if(age_change>=0 && age_change<85){
			//Age tier stays the same.
		}
		else if(age_change>=85 && age_change<95 && them.age_tier.get_previous()!=Age_Tier.EGG){
			them.age_tier=them.age_tier.get_previous();
		}
		else if(age_change>=95 && age_change<100 && them.age_tier.get_next()!=Age_Tier.DEAD){
			them.age_tier=them.age_tier.get_next();
		}

		//Randomly determine a pet type within the shadow's age tier.
		them.type=Age_Tier.get_random_pet_type(them.age_tier);

		//Set the shadow's name and color.
		them.name=game_view.get_pet().get_status().name+"'s shadow";
		them.color=Color.rgb(0,0,0);

		//Randomly determine the shadow's hunger and thirst levels.
		them.hunger=(short)RNG.random_range((int)Age_Tier.get_hunger_max(them.age_tier)/4,(int)Age_Tier.get_hunger_max(them.age_tier));
		them.thirst=(short)RNG.random_range((int)Pet_Status.THIRST_MAX/4,(int)Pet_Status.THIRST_MAX);

		//Randomly determine if the shadow is sick.
		if(RNG.random_range(0,99)<them.get_sick_chance()){
			them.sick=true;
		}

		//Randomly determine the shadow's weight.
		them.weight=0.01*(double)RNG.random_range((int)(Pet_Status.WEIGHT_MIN*100.0),(int)((Pet_Status.OBESITY/2.0)*1.25*100.0));

		//Determine shadow's energy.
		them.energy=(short)RNG.random_range(Age_Tier.get_energy_max(them.age_tier)/2,Age_Tier.get_energy_max(them.age_tier));

		//Determine shadow's level.
		int level_min=-3;
		int level_max=1;
		if(them.age_tier.ordinal()<game_view.get_pet().get_status().age_tier.ordinal()){
			level_max=-1;
		}
		else if(them.age_tier.ordinal()>game_view.get_pet().get_status().age_tier.ordinal()){
			level_min=0;
		}
		int random_level=RNG.random_range(game_view.get_pet().get_status().level+level_min,game_view.get_pet().get_status().level+level_max);
		if(random_level<1){
			random_level=1;
		}
		else if(random_level>Pet_Status.MAX_LEVEL){
			random_level=Pet_Status.MAX_LEVEL;
		}
		for(int i=1;i<random_level;i++){
			them.gain_experience(them.experience_max-them.experience,true);
		}

		//Spend shadow's stat points.
		while(them.stat_points>0){
			short branch=Pet_Type.get_pet_branch(them.type);

			int random=RNG.random_range(0,99);

			//Spend stat point on primary stat.
			if(random>=0 && random<80){
				if(branch==Pet_Type.BRANCH_NONE || branch==Pet_Type.BRANCH_TYRANNO || branch==Pet_Type.BRANCH_APATO){
					them.strength_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.strength_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_PTERO){
					them.dexterity_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.dexterity_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_STEGO || branch==Pet_Type.BRANCH_TRICERA){
					them.stamina_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.stamina_max_bound();
					them.stat_points--;
				}
			}
			//Spend stat point on secondary stat.
			else if(random>=80 && random<95){
				if(branch==Pet_Type.BRANCH_NONE || branch==Pet_Type.BRANCH_STEGO || branch==Pet_Type.BRANCH_PTERO){
					them.strength_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.strength_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_TYRANNO || branch==Pet_Type.BRANCH_TRICERA){
					them.dexterity_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.dexterity_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_APATO){
					them.stamina_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.stamina_max_bound();
					them.stat_points--;
				}
			}
			//Spend stat point on tertiary stat.
			else{
				if(branch==Pet_Type.BRANCH_NONE || branch==Pet_Type.BRANCH_TRICERA){
					them.strength_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.strength_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_APATO || branch==Pet_Type.BRANCH_STEGO){
					them.dexterity_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.dexterity_max_bound();
					them.stat_points--;
				}
				else if(branch==Pet_Type.BRANCH_PTERO || branch==Pet_Type.BRANCH_TYRANNO){
					them.stamina_max+=Pet_Status.STAT_GAIN_SELECTION;
					them.stamina_max_bound();
					them.stat_points--;
				}
			}
		}

		them.strength=them.strength_max;
		them.dexterity=them.dexterity_max;
		them.stamina=them.stamina_max;

		///Once equipment has been added, randomly add equipment here, scaled near the player, and using the shadow's magic find.
		for(int i=Equipment.SLOT_BEGIN;i<Equipment.SLOT_END;i++){
			String equipment_gained=them.new_equipment(65,them.level,Equipment.slot_to_string((short)i));

			if(equipment_gained.length()>0){
				them.equipment_slots.set(i,them.equipment.get(0));

				them.equipment.remove(0);
			}
		}

		//Start the actual battle activity, passing it the two pets' data.
		Intent intent=new Intent(BitBeast.this,Activity_Battle.class);

		Bundle bundle=new Bundle();
		bundle.putBoolean(getPackageName()+".server",false);
		bundle.putBoolean(getPackageName()+".shadow",true);
		bundle.putInt(getPackageName()+".our_seed",RNG.random_range(0,Integer.MAX_VALUE));
		bundle.putInt(getPackageName()+".their_seed",RNG.random_range(0,Integer.MAX_VALUE));
		bundle.putAll(game_view.get_pet().get_status().write_bundle_battle_data(getPackageName() + ".us."));
		bundle.putAll(them.write_bundle_battle_data(getPackageName() + ".them."));

		intent.putExtras(bundle);
		startActivity(intent);

		dismissDialogFragment(dialog_battle);
	}

	public void dialogButtonBattleWifiDirect () {
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
			start_battle_menu_wifi();
		} else {
			Toast.makeText(BitBeast.this,"Your device doesn't seem to support WiFi Direct!",Toast.LENGTH_SHORT).show();
		}

		dismissDialogFragment(dialog_battle);
	}

	public void dialogButtonBattleCancel () {
		dismissDialogFragment(dialog_battle);
	}

	public BitBeastDialogFragment showDialogFragment (String tag, int dialogType, int dialogLayout, String dialogMessage) {
		BitBeastDialogFragment dialogFragment = BitBeastDialogFragment.newInstance(dialogType, dialogLayout, dialogMessage, this);

		if (dialogType == BitBeastDialogFragment.DIALOG_TYPE_PROGRESS) {
			dialogFragment.show(getFragmentManager(), tag);
		} else {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			Fragment previous = getFragmentManager().findFragmentByTag(tag);

			if (previous != null) {
				transaction.remove(previous);
			}

			transaction.addToBackStack(null);

			dialogFragment.show(transaction, tag);
		}

		return dialogFragment;
	}

	@Override
	public void onViewLoaded (int dialogLayout, String dialogMessage, View view) {
		if (dialogLayout == R.layout.dialog_main_store) {
			Font.set_typeface(getAssets(), (TextView) view.findViewById(R.id.dialog_title_store));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_store_food));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_store_drinks));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_store_treatments));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_store_perma));

			view.findViewById(R.id.button_dialog_main_store_food).setNextFocusUpId(R.id.button_dialog_main_store_perma);
			view.findViewById(R.id.button_dialog_main_store_perma).setNextFocusDownId(R.id.button_dialog_main_store_food);

			view.findViewById(R.id.button_dialog_main_store_food).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonStoreFood();
				}
			});
			view.findViewById(R.id.button_dialog_main_store_drinks).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonStoreDrinks();
				}
			});
			view.findViewById(R.id.button_dialog_main_store_treatments).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonStoreTreatments();
				}
			});
			view.findViewById(R.id.button_dialog_main_store_perma).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonStorePerma();
				}
			});
		} else if (dialogLayout == R.layout.dialog_main_temp) {
			Font.set_typeface(getAssets(), (TextView) view.findViewById(R.id.dialog_title_temp));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_temp_ac));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_temp_heater));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_temp_none));

			view.findViewById(R.id.button_dialog_main_temp_ac).setNextFocusUpId(R.id.button_dialog_main_temp_none);
			view.findViewById(R.id.button_dialog_main_temp_none).setNextFocusDownId(R.id.button_dialog_main_temp_ac);

			view.findViewById(R.id.button_dialog_main_temp_ac).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonTempAc();
				}
			});
			view.findViewById(R.id.button_dialog_main_temp_heater).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonTempHeater();
				}
			});
			view.findViewById(R.id.button_dialog_main_temp_none).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonTempNone();
				}
			});
		} else if (dialogLayout == R.layout.dialog_main_games) {
			Font.set_typeface(getAssets(), (TextView) view.findViewById(R.id.dialog_title_games));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_training));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_bricks));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_rps));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_accel));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_gps));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_games_speed_gps));

			view.findViewById(R.id.button_dialog_main_games_training).setNextFocusUpId(R.id.button_dialog_main_games_speed_gps);
			view.findViewById(R.id.button_dialog_main_games_speed_gps).setNextFocusDownId(R.id.button_dialog_main_games_training);

			view.findViewById(R.id.button_dialog_main_games_training).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameTraining();
				}
			});
			view.findViewById(R.id.button_dialog_main_games_bricks).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameBricks();
				}
			});
			view.findViewById(R.id.button_dialog_main_games_rps).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameRps();
				}
			});
			view.findViewById(R.id.button_dialog_main_games_accel).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameAccel();
				}
			});
			view.findViewById(R.id.button_dialog_main_games_gps).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameGps();
				}
			});
			view.findViewById(R.id.button_dialog_main_games_speed_gps).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonGameSpeedGps();
				}
			});
		} else if (dialogLayout == R.layout.dialog_main_die) {
			Font.set_typeface(getAssets(), (TextView) view.findViewById(R.id.dialog_die_message));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_die_ok));

			view.findViewById(R.id.button_dialog_die_ok).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonDie();
				}
			});

			((TextView) view.findViewById(R.id.button_dialog_die_ok)).setText(dialogMessage);
		} else if (dialogLayout == R.layout.dialog_main_battle) {
			Font.set_typeface(getAssets(), (TextView) view.findViewById(R.id.dialog_title_battle));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_battle_shadow));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_battle_wifi));
			Font.set_typeface(getAssets(), (Button) view.findViewById(R.id.button_dialog_main_battle_cancel));

			view.findViewById(R.id.button_dialog_main_battle_shadow).setNextFocusUpId(R.id.button_dialog_main_battle_cancel);
			view.findViewById(R.id.button_dialog_main_battle_cancel).setNextFocusDownId(R.id.button_dialog_main_battle_shadow);

			view.findViewById(R.id.button_dialog_main_battle_shadow).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonBattleShadow();
				}
			});
			view.findViewById(R.id.button_dialog_main_battle_wifi).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonBattleWifiDirect();
				}
			});
			view.findViewById(R.id.button_dialog_main_battle_cancel).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v){
					dialogButtonBattleCancel();
				}
			});
		}
	}

	public void dismissDialogFragment (BitBeastDialogFragment dialogFragment) {
		if (dialogFragment != null && dialogFragment.isAdded()) {
			dialogFragment.dismiss();
		}
	}

	public void close_dialogs(){
		dismissDialogFragment(dialog_progress);

		dismissDialogFragment(dialog_store);

		dismissDialogFragment(dialog_temp);

		dismissDialogFragment(dialog_games);

		dismissDialogFragment(dialog_die);

		dismissDialogFragment(dialog_battle);
	}
}