/**
 * 
 */
package com.rayy.android.dialer;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.widget.EditText;

/**
 * @author RAY
 *
 */
public class SimpleTextDialog extends SherlockDialogFragment{
	public Fragment mFragment;
	public String text;
	
	public interface OnTextSetListener {
		void onTextSet(String str);
	}

    public SimpleTextDialog (String str) {
        //mFragment = callback;
    	this.text = str;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	Context context = getActivity();
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);

    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final EditText diag_view = (EditText) inflater.inflate(R.layout.diag_set, null);
		diag_view.setText(text);
		
    	builder.setView(diag_view);
    	builder.setTitle(getString(R.string.title_set_number));
    	builder.setPositiveButton(android.R.string.ok, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				SimpleTextDialog.this.text = diag_view.getText().toString();
				//(PackageListing)context.saveNumber();
				((OnTextSetListener) getActivity()).onTextSet(text);
			}
    		
    	});

    	Dialog diag = builder.create();
    	
    	return diag;
    }
}
