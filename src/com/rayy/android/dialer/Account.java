/**
 * 
 */
package com.rayy.android.dialer;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author wy_coordsafe
 *
 */
public class Account extends Activity {
	EditText name, password, email;
	Button btnReg, btnLogin;
	String strName, strPass, strEmail;
	Context ctx;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.account);
		
		ctx = getApplicationContext();
		
		name = (EditText) this.findViewById(R.id.name);
		password = (EditText)this.findViewById(R.id.password);
		email = (EditText)this.findViewById(R.id.email);
		
		btnReg = (Button) this.findViewById(R.id.signup);
		btnReg.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				register();
				
			}
			
		});
		
		btnLogin = (Button) this.findViewById(R.id.login);
		btnLogin.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				login();
				
			}
			
		});
		
		Parse.initialize(this, Credential.appId, Credential.clientKey);
	}

	protected void login() {
		// TODO Auto-generated method stub
		strName = name.getText().toString();
		strPass = password.getText().toString();
		
		try {
			ParseUser.logIn(strName, strPass);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	protected void register() {
		// TODO Auto-generated method stub
		strName = name.getText().toString();
		strPass = password.getText().toString();
		strEmail = email.getText().toString();
		
		ParseUser user = new ParseUser();
		
		user.setUsername(strName);
		user.setPassword(strPass);
		user.setEmail(strEmail);
		  
		// other fields can be set just like with ParseObject
		//user.put("phone", "650-555-0000");
		  
		user.signUpInBackground(new SignUpCallback() {
		  public void done(ParseException e) {
		    if (e == null) {
		      // Hooray! Let them use the app now.
		    	Toast.makeText(ctx, "Sign up successful", Toast.LENGTH_SHORT).show();
		    	
		    	// login automatically
		    	login();
		    	
		    	Account.this.finish();
		    } else {
		      // Sign up didn't succeed. Look at the ParseException
		      // to figure out what went wrong
		    	Log.e("", e.getMessage());
		    }
		  }
		});
	}

}
