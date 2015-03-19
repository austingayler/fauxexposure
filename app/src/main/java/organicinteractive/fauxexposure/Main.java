package organicinteractive.fauxexposure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class Main extends ActionBarActivity {

    private Spinner exposureSpinner;
    public final static String numSecondsMsg = "com.organicinteractive.fauxexposure.numSecondsMsg";
    public final static String exposureTypeMsg = "com.organicinteractive.fauxexposure.exposureTypeMsg";
    Intent intent;
    Spinner spinner;
    EditText editText;
    Button frameBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = (Spinner) findViewById(R.id.exposureType);
        editText = (EditText) findViewById(R.id.numSeconds);
        frameBtn = (Button) findViewById(R.id.frameBtn);

        frameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Frame(v);
            }
        });

    }

    public void Frame(View v) {
        String et = editText.getText().toString();
        if(!et.equals("") && et != null) { //make sure user has entered a time
            int numSeconds = Integer.valueOf(et);
            String exposureType = spinner.getSelectedItem().toString();

            Intent intent = new Intent(this, Frame.class);
            intent.putExtra(numSecondsMsg, numSeconds);
            intent.putExtra(exposureTypeMsg, exposureType);

            startActivity(intent);
        } else {
            toast("Enter desired exposure time.");
        }



    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}