/**
 * 
 */
package com.rayy.android.dialer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * @author RAY
 *
 */
public class InfoDialog extends SherlockDialogFragment{
	public String text;
	
	public InfoDialog (String str) {
        //mFragment = callback;
    	this.text = str;
    }
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	Context context = getActivity();
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);

    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final TextView diag_view = (TextView) inflater.inflate(R.layout.diag_info, null);
		diag_view.setText(Html.fromHtml(text));
		
    	builder.setView(diag_view);
    	builder.setTitle(getString(R.string.title_info));
    	builder.setPositiveButton(android.R.string.ok, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
    		
    	});

    	Dialog diag = builder.create();
    	
    	return diag;
    }
}
